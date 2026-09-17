package com.brewer.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * GraalVM native-image runtime hints for classes/resources that are looked up
 * reflectively, or read as classpath resources, outside of Spring's own bean
 * machinery, and therefore are not picked up automatically by the Spring AOT engine.
 *
 * <p><b>Flyway</b> — its {@code LogFactory} probes several logging-framework adapter
 * classes with {@code Class.forName(...)} to auto-detect which logging backend is
 * available on the classpath (SLF4J, Log4j2, Android, etc.). On the JVM, the candidates
 * that aren't present simply raise (and Flyway swallows) a {@link ClassNotFoundException}.
 * In a native image, {@code Class.forName} additionally requires the target type to be
 * explicitly registered for reflection, or it throws {@link ClassNotFoundException} even
 * for classes that ARE present in the image. This project only ships SLF4J (via Logback),
 * so only {@code Slf4jLogCreator} needs to be registered. Flyway also reads its own
 * version number from the classpath resource {@code org/flywaydb/core/internal/version.txt}
 * ({@code VersionPrinter}); classpath resources are not included in a native image unless
 * explicitly registered, otherwise the resource stream is {@code null} and Flyway fails
 * with a {@link NullPointerException} while copying it.
 *
 * <p><b>MySQL Connector/J</b> — {@code com.mysql.cj.Messages} loads a
 * {@link java.util.ResourceBundle} ({@code com.mysql.cj.LocalizedErrorMessages}) to
 * render its own error messages. Resource bundles must be explicitly registered for a
 * native image, otherwise {@code ResourceBundle.getBundle(...)} throws
 * {@link java.util.MissingResourceException} even though the bundle is present on the
 * classpath, which breaks driver initialization entirely (no JDBC connection can be
 * opened at all). Additionally, {@code com.mysql.cj.conf.ConnectionUrl$Type} picks its
 * {@code com.mysql.cj.conf.url.*ConnectionUrl} implementation class reflectively based on
 * the JDBC URL scheme (single host, failover, replication, load-balance, X DevAPI, each
 * with a plain and a DNS-SRV variant); every implementation is registered here so any of
 * these URL styles works, not just the plain single-host one used by this project today.
 * Similarly, {@code com.mysql.cj.log.LogFactory} reflectively instantiates the configured
 * {@code com.mysql.cj.log.Log} implementation (defaulting to {@code StandardLogger} when
 * no {@code logger} connection property is set), so the built-in logger implementations
 * are registered here too. Finally, {@code com.mysql.cj.exceptions.ExceptionFactory}
 * reflectively instantiates whichever specific {@code com.mysql.cj.exceptions.*}
 * subclass is requested at each call site throughout the driver (connection failures,
 * timeouts, auth errors, etc.); rather than enumerating every call site, the whole
 * {@code com.mysql.cj.exceptions} package is scanned and registered for reflection.
 * The same reflective-instantiation pattern is used for the configured
 * {@code com.mysql.cj.protocol.SocketFactory} (defaulting to
 * {@code StandardSocketFactory}) and for whichever authentication plugin
 * ({@code com.mysql.cj.protocol.a.authentication.*}, e.g. {@code CachingSha2PasswordPlugin}
 * for MySQL 8's default auth) the server requests during the handshake.
 *
 * <p><b>HikariCP</b> — {@code PoolBase}/statement-cache related code reflectively allocates a
 * {@code java.sql.Statement[]} array (via {@code Array.newInstance}), and
 * {@code ConcurrentBag} does the same for its internal
 * {@code IConcurrentBagEntry[]} thread-local array; array classes must be registered
 * individually for reflection in a native image, otherwise pool initialization fails
 * with a {@link org.graalvm.nativeimage.MissingReflectionRegistrationError} that (for the
 * {@code Statement[]} case specifically) gets silently downgraded to a bare
 * {@link NullPointerException} once wrapped by {@code HikariPool.throwPoolInitializationException}
 * (the original exception's message is only visible with
 * {@code logging.level.com.zaxxer.hikari=DEBUG}, since {@code HikariPool.createPoolEntry}
 * logs-and-swallows it at DEBUG level before HikariCP itself loses track of the real cause).
 *
 * <p><b>Flyway SQL exception classification</b> — when a database connection attempt fails,
 * {@code FlywaySqlException.throwFlywayExceptionIfPossible} reflectively invokes a static
 * {@code isFlywaySpecificVersionOf(SQLException)} method on every known vendor-specific
 * subclass under {@code org.flywaydb.core.internal.exception.sqlExceptions} to build a more
 * helpful error message; without registering these methods, the original SQL error is
 * masked by a {@link NoSuchMethodException} instead of being reported.
 *
 * <p><b>JBoss Logging (used internally by Hibernate ORM and Hibernate Validator)</b> —
 * {@code @MessageLogger}-annotated logger interfaces (e.g. {@code org.hibernate.jpa.internal.JpaLogger})
 * and {@code @MessageBundle}-annotated message-bundle interfaces (e.g.
 * {@code org.hibernate.validator.internal.util.logging.Messages}) each have an implementation
 * class generated at compile time by the JBoss Logging annotation processor, named
 * {@code <InterfaceName>_$logger} and {@code <InterfaceName>_$bundle} respectively, spread
 * across many different Hibernate/Hibernate Validator packages. {@code org.jboss.logging.Logger
 * .getMessageLogger} reflectively instantiates the {@code *_$logger} implementation, while
 * {@code org.jboss.logging.Messages.getBundle} reflectively reads the {@code *_$bundle}
 * implementation's static {@code INSTANCE} field (both fall back to runtime bytecode
 * generation on the JVM, which native-image does not support at all); every generated
 * {@code *_$logger}/{@code *_$bundle} class anywhere on the classpath is therefore discovered
 * via a classpath scan and registered for both constructor and field reflection.
 *
 * <p><b>Hibernate ORM's {@code StrategySelector}</b> — {@code StrategySelectorBuilder} registers a
 * fixed, well-known set of default strategy implementations (naming strategies, the transaction
 * coordinator builder, SQM multi-table mutation/insert strategies, the column-ordering strategy,
 * cache-keys factory, JSON/XML {@code FormatMapper}s) that {@code StrategySelectorImpl} may
 * reflectively instantiate via a no-arg constructor whenever no explicit override is configured
 * (as observed for {@code ColumnOrderingStrategyStandard}, the default column-ordering strategy).
 * Since this set is fixed by Hibernate itself (not user/application-specific), every one of these
 * default implementation classes is registered here up front, rather than reactively adding one
 * hint per strategy category as each is exercised at runtime.
 *
 * <p><b>Hibernate ORM's built-in XSD/DTD schemas</b> — {@code XmlMappingBinderAccess}/
 * {@code LocalXmlResourceResolver} resolve the legacy {@code hibernate-mapping}/
 * {@code hibernate-configuration} DTDs and the JPA {@code orm}/{@code persistence} XSDs (and
 * Hibernate's own mapping/configuration XSDs) as classpath resources when parsing
 * {@code META-INF/orm.xml}/{@code persistence.xml}-style metadata, regardless of whether this
 * project actually uses legacy XML mappings; all of {@code hibernate-core}'s bundled schema
 * files are therefore registered as resources up front.
 *
 * <p><b>Hibernate ORM's "Models" annotation descriptors</b> — every JPA/Hibernate mapping
 * annotation (e.g. {@code @Cache}, {@code @NaturalIdCache}) has a corresponding
 * {@code org.hibernate.boot.models.annotations.internal.*Annotation} "mock usage"
 * implementation, reflectively instantiated by
 * {@code OrmAnnotationDescriptor$DynamicCreator} (via a constructor taking a
 * {@code org.hibernate.models.spi.ModelsContext}) whenever Hibernate synthesizes an
 * annotation usage that wasn't explicitly present on the mapped class (e.g. the implicit
 * {@code @Cache} Hibernate binds internally while processing entity metadata). This is a
 * large (~280 classes) but fixed, framework-defined set, so the whole package is discovered
 * via a classpath scan and registered for reflection, rather than adding one hint per
 * annotation as each is exercised at runtime.
 *
 * <p><b>Hibernate ORM's {@code @DialectOverride} nested annotations</b> —
 * {@code DialectOverridesAnnotationHelper} builds a lookup table (base annotation →
 * dialect-override annotation, e.g. {@code @SQLInsert} → {@code @DialectOverride.SQLInsert})
 * by calling {@code DialectOverride.class.getNestMembers()} and reflectively reading each
 * nested annotation's own {@code @OverridesAnnotation} meta-annotation. {@code getNestMembers()}
 * requires the enclosing class to be registered with {@link MemberCategory#DECLARED_CLASSES},
 * and each of the ~30 nested {@code DialectOverride.*} annotation types must itself be
 * registered so its meta-annotations can be read reflectively; otherwise the lookup table ends
 * up empty and processing any mapped entity fails with a {@code HibernateException} ("does not
 * have an override form"), because {@code EntityBinder.bindCustomSql} unconditionally probes
 * every custom-SQL annotation type (e.g. {@code @SQLInsert}, {@code @SQLUpdate},
 * {@code @SQLDelete}) for a possible dialect override while binding *every* entity, regardless
 * of whether that particular annotation is actually present on the mapped class.
 *
 * <p><b>Hibernate ORM's event listener registry</b> — {@code EventListenerGroupImpl} keeps each
 * category of lifecycle listener (e.g. {@code AutoFlushEventListener}, {@code PreInsertEventListener},
 * {@code PostDeleteEventListener}, etc., ~34 in total under {@code org.hibernate.event.spi})
 * in a reflectively-allocated array (one array type per listener interface); this happens
 * unconditionally while bootstrapping the {@code SessionFactory}'s standard listeners,
 * regardless of whether the application registers any custom listeners itself, so every one of
 * these array types is registered here.
 *
 * <p><b>Hibernate ORM's Bean Validation integration</b> — {@code BeanValidationIntegrator} loads
 * {@code org.hibernate.boot.beanvalidation.TypeSafeActivator} via {@code Class.forName(...)} to
 * wire up Jakarta Bean Validation (this project depends on
 * {@code spring-boot-starter-validation}, which pulls in a Bean Validation provider), so that
 * class must be registered for reflection like any other {@code Class.forName} target. Once
 * activated, it registers Bean Validation's pre-insert/pre-update/pre-delete listeners against
 * Hibernate's {@code EventType} registry, resolved by name (e.g. {@code "pre-insert"}) via
 * {@code EventType.resolveEventTypeByName}; that lookup table is itself built by
 * {@code EventType.class.getDeclaredFields()} reflectively reading every one of {@code EventType}'s
 * own {@code public static final EventType<?>} constants, so {@code EventType.class} must be
 * registered for declared-field reflection too, or the table ends up empty and every event type
 * name (not just the Bean-Validation-related ones) fails to resolve.
 *
 * <p><b>Hibernate ORM's persister classes</b> — {@code StandardPersisterClassResolver} maps each
 * entity/collection mapping shape to one of a fixed set of five built-in persister
 * implementations ({@code SingleTableEntityPersister}, {@code JoinedSubclassEntityPersister},
 * {@code UnionSubclassEntityPersister} for entities; {@code OneToManyPersister},
 * {@code BasicCollectionPersister} for collections), and {@code PersisterFactoryImpl}
 * reflectively looks up and invokes that persister's constructor to build the runtime metamodel.
 * All five are registered here since which ones are used for a given entity/collection is a
 * mapping-level implementation detail, not something worth hard-coding per entity.
 *
 * <p><b>Caching</b> — {@code com.brewer.config.WebConfig.cacheManager} uses Caffeine
 * ({@code org.springframework.cache.caffeine.CaffeineCacheManager}), Spring Boot's own
 * recommended cache provider. EhCache/JSR-107 was evaluated first but dropped: its optional
 * statistics subsystem ({@code org.ehcache.shadow.org.terracotta.*}, a reflection-based
 * "context tree" that introspects cache store internals for JMX/monitoring purposes) requires
 * an effectively open-ended chain of reflection hints to work under native-image, none of which
 * are relevant to this project's actual use of the cache (a simple size/TTL-bounded lookup
 * cache with no monitoring requirement) — see docs/spring-native-migration-plan.md, section 8,
 * for the full investigation. Caffeine itself is officially GraalVM-native-image-tested
 * upstream (see the {@code com.github.ben-manes.caffeine:caffeine} entry in the
 * {@code oracle/graalvm-reachability-metadata} repository), but that metadata isn't picked up
 * automatically by plain Spring AOT processing (it's normally consumed via a dedicated Gradle/
 * Maven "native image reachability metadata" plugin, which this project does not use). Caffeine
 * specializes its internal cache-node implementation (e.g. {@code SSMS}, {@code SSMSA},
 * {@code PSWMS}, ...) via {@code LocalCacheFactory}, which builds a class name by encoding
 * which features are enabled (strong/weak keys and values, maximum-size, access/write time,
 * refresh, ...) and loads it via {@code Class.forName}/{@code MethodHandles.Lookup.findClass} —
 * an even larger combinatorial space than EhCache's statistics tree, so rather than hand-copying
 * (and keeping in sync with Caffeine version upgrades) the official metadata's specific class
 * list, the whole (self-contained, finite) {@code com.github.benmanes.caffeine.cache} package is
 * scanned and registered for both constructor and field reflection.
 *
 * <p><b>Thymeleaf's Spring MVC view class</b> — {@code ThymeleafViewResolver} produces a
 * {@code org.thymeleaf.spring6.view.ThymeleafView} per request by having Spring's
 * {@code AbstractCachingViewResolver} reflectively instantiate the configured view class via
 * its no-arg constructor (the usual {@code AbstractView} extension point for view resolvers);
 * this only surfaces once an actual page is rendered (e.g. the login page), which none of the
 * bean-creation-time checks performed earlier catch, so it must be registered explicitly.
 */
public class NativeRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerType(
				org.flywaydb.core.internal.logging.slf4j.Slf4jLogCreator.class,
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		hints.resources().registerPattern("org/flywaydb/core/internal/version.txt");
		hints.resources().registerResourceBundle("com.mysql.cj.LocalizedErrorMessages");

		for (String connectionUrlTypeName : new String[] {
				"com.mysql.cj.conf.url.SingleConnectionUrl",
				"com.mysql.cj.conf.url.FailoverConnectionUrl",
				"com.mysql.cj.conf.url.FailoverDnsSrvConnectionUrl",
				"com.mysql.cj.conf.url.LoadBalanceConnectionUrl",
				"com.mysql.cj.conf.url.LoadBalanceDnsSrvConnectionUrl",
				"com.mysql.cj.conf.url.ReplicationConnectionUrl",
				"com.mysql.cj.conf.url.ReplicationDnsSrvConnectionUrl",
				"com.mysql.cj.conf.url.XDevApiConnectionUrl",
				"com.mysql.cj.conf.url.XDevApiDnsSrvConnectionUrl",
				"com.mysql.cj.log.StandardLogger",
				"com.mysql.cj.log.Slf4JLogger",
				"com.mysql.cj.log.Jdk14Logger",
				"com.mysql.cj.log.NullLogger" }) {
			hints.reflection().registerType(
					TypeReference.of(connectionUrlTypeName),
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

		for (String sqlExceptionTypeName : new String[] {
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlUnableToConnectToDbException",
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoIntegratedAuthException",
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlServerUntrustedCertificateSqlException",
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoDriversForInteractiveAuthException" }) {
			hints.reflection().registerType(
					TypeReference.of(sqlExceptionTypeName),
					MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

		registerAllClassesInPackage(hints, classLoader, "com.mysql.cj.exceptions",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		registerAllClassesInPackage(hints, classLoader, "com.mysql.cj.protocol.a.authentication",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		for (String socketFactoryTypeName : new String[] {
				"com.mysql.cj.protocol.StandardSocketFactory",
				"com.mysql.cj.protocol.NamedPipeSocketFactory",
				"com.mysql.cj.protocol.SocksProxySocketFactory" }) {
			hints.reflection().registerType(
					TypeReference.of(socketFactoryTypeName),
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

		hints.reflection().registerType(java.sql.Statement[].class);
		hints.reflection().registerType(
				TypeReference.of("com.zaxxer.hikari.util.ConcurrentBag$IConcurrentBagEntry[]"));

		registerAllClassesMatching(hints, classLoader, "classpath*:**/*_$logger.class",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);
		registerAllClassesMatching(hints, classLoader, "classpath*:**/*_$bundle.class",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);
		hints.resources().registerResourceBundle("org.hibernate.validator.ValidationMessages");

		for (String hibernateStrategyTypeName : new String[] {
				// TransactionCoordinatorBuilder
				"org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl",
				"org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl",
				// SqmMultiTableInsertStrategy / SqmMultiTableMutationStrategy
				"org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy",
				"org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy",
				"org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy",
				"org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy",
				"org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy",
				"org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy",
				"org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy",
				"org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy",
				// ImplicitNamingStrategy
				"org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl",
				"org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl",
				"org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl",
				"org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl",
				// ImplicitDatabaseObjectNamingStrategy
				"org.hibernate.id.enhanced.StandardNamingStrategy",
				"org.hibernate.id.enhanced.SingleNamingStrategy",
				"org.hibernate.id.enhanced.LegacyNamingStrategy",
				// ColumnOrderingStrategy
				"org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard",
				"org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy",
				// CacheKeysFactory
				"org.hibernate.cache.internal.DefaultCacheKeysFactory",
				"org.hibernate.cache.internal.SimpleCacheKeysFactory",
				// FormatMapper (JSON/XML)
				"org.hibernate.type.format.jakartajson.JsonBJsonFormatMapper",
				"org.hibernate.type.format.jackson.JacksonJsonFormatMapper",
				"org.hibernate.type.format.jackson.Jackson3JsonFormatMapper",
				"org.hibernate.type.format.jackson.JacksonOsonFormatMapper",
				"org.hibernate.type.format.jackson.JacksonXmlFormatMapper",
				"org.hibernate.type.format.jackson.Jackson3XmlFormatMapper",
				"org.hibernate.type.format.jaxb.JaxbXmlFormatMapper" }) {
			hints.reflection().registerType(
					TypeReference.of(hibernateStrategyTypeName),
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

		hints.resources().registerPattern("org/hibernate/*.dtd");
		hints.resources().registerPattern("org/hibernate/*.xsd");
		hints.resources().registerPattern("org/hibernate/jpa/*.xsd");
		hints.resources().registerPattern("org/hibernate/xsd/cfg/*.xsd");
		hints.resources().registerPattern("org/hibernate/xsd/mapping/*.xsd");

		registerAllClassesInPackage(hints, classLoader, "org.hibernate.boot.models.annotations.internal",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		hints.reflection().registerType(
				org.hibernate.annotations.DialectOverride.class,
				MemberCategory.DECLARED_CLASSES);
		for (String dialectOverrideNestedTypeName : new String[] {
				"org.hibernate.annotations.DialectOverride$Check",
				"org.hibernate.annotations.DialectOverride$Checks",
				"org.hibernate.annotations.DialectOverride$ColumnDefault",
				"org.hibernate.annotations.DialectOverride$ColumnDefaults",
				"org.hibernate.annotations.DialectOverride$DiscriminatorFormula",
				"org.hibernate.annotations.DialectOverride$DiscriminatorFormulas",
				"org.hibernate.annotations.DialectOverride$FilterDefOverrides",
				"org.hibernate.annotations.DialectOverride$FilterDefs",
				"org.hibernate.annotations.DialectOverride$FilterOverrides",
				"org.hibernate.annotations.DialectOverride$Filters",
				"org.hibernate.annotations.DialectOverride$Formula",
				"org.hibernate.annotations.DialectOverride$Formulas",
				"org.hibernate.annotations.DialectOverride$GeneratedColumn",
				"org.hibernate.annotations.DialectOverride$GeneratedColumns",
				"org.hibernate.annotations.DialectOverride$JoinFormula",
				"org.hibernate.annotations.DialectOverride$JoinFormulas",
				"org.hibernate.annotations.DialectOverride$OverridesAnnotation",
				"org.hibernate.annotations.DialectOverride$SQLDelete",
				"org.hibernate.annotations.DialectOverride$SQLDeleteAll",
				"org.hibernate.annotations.DialectOverride$SQLDeleteAlls",
				"org.hibernate.annotations.DialectOverride$SQLDeletes",
				"org.hibernate.annotations.DialectOverride$SQLInsert",
				"org.hibernate.annotations.DialectOverride$SQLInserts",
				"org.hibernate.annotations.DialectOverride$SQLOrder",
				"org.hibernate.annotations.DialectOverride$SQLOrders",
				"org.hibernate.annotations.DialectOverride$SQLRestriction",
				"org.hibernate.annotations.DialectOverride$SQLRestrictions",
				"org.hibernate.annotations.DialectOverride$SQLSelect",
				"org.hibernate.annotations.DialectOverride$SQLSelects",
				"org.hibernate.annotations.DialectOverride$SQLUpdate",
				"org.hibernate.annotations.DialectOverride$SQLUpdates",
				"org.hibernate.annotations.DialectOverride$Version" }) {
			hints.reflection().registerType(
					TypeReference.of(dialectOverrideNestedTypeName),
					MemberCategory.INVOKE_DECLARED_METHODS);
		}

		for (String eventListenerTypeName : new String[] {
				"org.hibernate.event.spi.AutoFlushEventListener",
				"org.hibernate.event.spi.ClearEventListener",
				"org.hibernate.event.spi.DeleteEventListener",
				"org.hibernate.event.spi.DirtyCheckEventListener",
				"org.hibernate.event.spi.EvictEventListener",
				"org.hibernate.event.spi.FlushEntityEventListener",
				"org.hibernate.event.spi.FlushEventListener",
				"org.hibernate.event.spi.InitializeCollectionEventListener",
				"org.hibernate.event.spi.LoadEventListener",
				"org.hibernate.event.spi.LockEventListener",
				"org.hibernate.event.spi.MergeEventListener",
				"org.hibernate.event.spi.PersistEventListener",
				"org.hibernate.event.spi.PostActionEventListener",
				"org.hibernate.event.spi.PostCollectionRecreateEventListener",
				"org.hibernate.event.spi.PostCollectionRemoveEventListener",
				"org.hibernate.event.spi.PostCollectionUpdateEventListener",
				"org.hibernate.event.spi.PostCommitDeleteEventListener",
				"org.hibernate.event.spi.PostCommitInsertEventListener",
				"org.hibernate.event.spi.PostCommitUpdateEventListener",
				"org.hibernate.event.spi.PostDeleteEventListener",
				"org.hibernate.event.spi.PostInsertEventListener",
				"org.hibernate.event.spi.PostLoadEventListener",
				"org.hibernate.event.spi.PostUpdateEventListener",
				"org.hibernate.event.spi.PostUpsertEventListener",
				"org.hibernate.event.spi.PreCollectionRecreateEventListener",
				"org.hibernate.event.spi.PreCollectionRemoveEventListener",
				"org.hibernate.event.spi.PreCollectionUpdateEventListener",
				"org.hibernate.event.spi.PreDeleteEventListener",
				"org.hibernate.event.spi.PreFlushEventListener",
				"org.hibernate.event.spi.PreInsertEventListener",
				"org.hibernate.event.spi.PreLoadEventListener",
				"org.hibernate.event.spi.PreUpdateEventListener",
				"org.hibernate.event.spi.PreUpsertEventListener",
				"org.hibernate.event.spi.RefreshEventListener",
				"org.hibernate.event.spi.ReplicateEventListener" }) {
			hints.reflection().registerType(TypeReference.of(eventListenerTypeName + "[]"));
		}

		hints.reflection().registerType(
				TypeReference.of("org.hibernate.boot.beanvalidation.TypeSafeActivator"),
				MemberCategory.INVOKE_DECLARED_METHODS);
		hints.reflection().registerType(
				TypeReference.of("org.hibernate.event.spi.EventType"),
				MemberCategory.DECLARED_FIELDS);

		for (String persisterTypeName : new String[] {
				"org.hibernate.persister.entity.SingleTableEntityPersister",
				"org.hibernate.persister.entity.JoinedSubclassEntityPersister",
				"org.hibernate.persister.entity.UnionSubclassEntityPersister",
				"org.hibernate.persister.collection.OneToManyPersister",
				"org.hibernate.persister.collection.BasicCollectionPersister" }) {
			hints.reflection().registerType(
					TypeReference.of(persisterTypeName),
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

		registerAllClassesInPackage(hints, classLoader, "com.github.benmanes.caffeine.cache",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);

		hints.reflection().registerType(
				TypeReference.of("org.thymeleaf.spring6.view.ThymeleafView"),
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		// Thymeleaf's Spring integration module (org.thymeleaf.spring6.*) reflectively probes for
		// version-specific delegate classes at static-init time (e.g. the nested
		// Mvc$Spring41MvcUriComponentsBuilderDelegate class, used to detect which Spring MVC
		// URI-building API is available). Because this happens inside a static initializer, a
		// single missing nested class turns into a permanent ExceptionInInitializerError /
		// NoClassDefFoundError on every subsequent use of SpringStandardExpressionObjectFactory,
		// breaking every request that renders a Thymeleaf template (including /login). Registering
		// the whole package (declared classes/constructors) is far more robust than chasing each
		// individual nested delegate class one crash at a time.
		registerAllClassesInPackage(hints, classLoader, "org.thymeleaf.spring6",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS,
				MemberCategory.DECLARED_CLASSES);
	}

	/**
	 * Scans the given package (non-recursively, i.e. not sub-packages) on the classpath and
	 * registers every {@code .class} file found in it for reflection with the given member
	 * categories, including nested/inner classes. Used for packages where a framework
	 * reflectively instantiates an unpredictable (or simply very large) subset of the classes
	 * at runtime (e.g. an exception hierarchy, or an annotation-per-class registry), so
	 * enumerating every call site individually would be both tedious and fragile across
	 * dependency upgrades.
	 */
	private static void registerAllClassesInPackage(RuntimeHints hints, ClassLoader classLoader,
			String packageName, MemberCategory... memberCategories) {
		String packagePath = packageName.replace('.', '/');
		registerAllClassesMatching(hints, classLoader, "classpath*:" + packagePath + "/*.class",
				memberCategories);
	}

	/**
	 * Scans the classpath for every {@code .class} resource matching the given Ant-style
	 * pattern (which may span multiple/unknown packages, e.g. {@code classpath*:&#42;&#42;/*_$logger.class})
	 * and registers the corresponding type for reflection with the given member categories,
	 * skipping the synthetic {@code package-info}/{@code module-info} pseudo-classes (which
	 * have no valid, reflectable type name). Class names are resolved from the {@code .class}
	 * bytecode itself (via ASM, without loading/initializing the class), so this works
	 * regardless of which package each match lives in.
	 */
	private static void registerAllClassesMatching(RuntimeHints hints, ClassLoader classLoader,
			String antPattern, MemberCategory... memberCategories) {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
		SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(resolver);
		Resource[] resources;
		try {
			resources = resolver.getResources(antPattern);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to scan '" + antPattern + "' for native hints", ex);
		}
		for (Resource resource : resources) {
			String filename = resource.getFilename();
			if (filename != null && (filename.equals("package-info.class") || filename.equals("module-info.class"))) {
				continue;
			}
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
				String className = metadataReader.getClassMetadata().getClassName();
				hints.reflection().registerType(TypeReference.of(className), memberCategories);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to read class metadata for " + resource, ex);
			}
		}
	}
}
