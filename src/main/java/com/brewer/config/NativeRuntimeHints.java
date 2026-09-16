package com.brewer.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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
 * <p><b>Flyway SQL exception classification</b> — when a database connection attempt fails,
 * {@code FlywaySqlException.throwFlywayExceptionIfPossible} reflectively invokes a static
 * {@code isFlywaySpecificVersionOf(SQLException)} method on every known vendor-specific
 * subclass under {@code org.flywaydb.core.internal.exception.sqlExceptions} to build a more
 * helpful error message; without registering these methods, the original SQL error is
 * masked by a {@link NoSuchMethodException} instead of being reported.
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
	}

	/**
	 * Scans the given package on the classpath and registers every top-level class found
	 * in it (skipping nested/anonymous classes) for reflection with the given member
	 * categories. Used for packages where a framework reflectively instantiates an
	 * unpredictable subset of the classes at runtime (e.g. an exception hierarchy), so
	 * enumerating every call site individually would be both tedious and fragile across
	 * dependency upgrades.
	 */
	private static void registerAllClassesInPackage(RuntimeHints hints, ClassLoader classLoader,
			String packageName, MemberCategory... memberCategories) {
		String packagePath = packageName.replace('.', '/');
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
		Resource[] resources;
		try {
			resources = resolver.getResources("classpath*:" + packagePath + "/*.class");
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to scan package " + packageName + " for native hints", ex);
		}
		for (Resource resource : resources) {
			String filename = resource.getFilename();
			if (filename == null || filename.contains("$")) {
				continue;
			}
			String className = packageName + "." + filename.substring(0, filename.length() - ".class".length());
			hints.reflection().registerType(TypeReference.of(className), memberCategories);
		}
	}
}
