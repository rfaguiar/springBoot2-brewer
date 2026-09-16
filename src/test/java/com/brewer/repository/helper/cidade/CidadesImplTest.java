package com.brewer.repository.helper.cidade;

import com.brewer.builder.CidadeBuilder;
import com.brewer.helper.JPAHibernateTest;
import com.brewer.model.Cidade;
import com.brewer.repository.filter.CidadeFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;

import static org.junit.Assert.assertEquals;

public class CidadesImplTest {

    private CidadesImpl cidadesImpl;
    private Cidade cidade1;
    private Cidade cidade2;

    @Mock
    private PaginacaoUtil paginacaoUtil;
    @Mock
    private Pageable pageable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(paginacaoUtil.ordenar(Mockito.any(), Mockito.anyString())).thenReturn("");
        EntityManager entityManager = JPAHibernateTest.getEntityManager();

        entityManager.getTransaction().begin();
        cidade1 = CidadeBuilder.criarCidade();
        cidade1.setNome("Campinas");
        cidade1.getEstado().setNome("São Paulo");
        cidade1.getEstado().setSigla("SP");
        entityManager.persist(cidade1.getEstado());
        entityManager.persist(cidade1);

        cidade2 = CidadeBuilder.criarCidade();
        cidade2.setNome("Niteroi");
        cidade2.getEstado().setNome("Rio de Janeiro");
        cidade2.getEstado().setSigla("RJ");
        entityManager.persist(cidade2.getEstado());
        entityManager.persist(cidade2);

        cidadesImpl = new CidadesImpl(entityManager, paginacaoUtil);
    }

    @After
    public void tearDown() {
        JPAHibernateTest.roolbackEcloseEntityManager();
    }

    @Test
    public void testeMetodoFiltrarSemFiltrosDeveRetornarTodosRegistrosComConteudoCorreto() {
        Page<Cidade> result = cidadesImpl.filtrar(new CidadeFilter(), pageable);

        assertEquals(2, result.getContent().size());
        assertCidade(result.getContent().get(0), cidade1);
        assertCidade(result.getContent().get(1), cidade2);
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroNuloDeveRetornarTodosOsRegistros() {
        Page<Cidade> result = cidadesImpl.filtrar(null, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Campinas", result.getContent().get(0).getNome());
        assertEquals("Niteroi", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoFiltrarComNomeParcialEEstadoDeveRetornarSomenteACidadeCompativel() {
        CidadeFilter filtro = new CidadeFilter();
        filtro.setEstado(cidade1.getEstado());
        filtro.setNome("ampi");

        Page<Cidade> result = cidadesImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertCidade(result.getContent().get(0), cidade1);
    }

    @Test
    public void testeMetodoFiltrarQuandoNomeEmBrancoEEstadoNuloNaoDeveAplicarFiltros() {
        CidadeFilter filtro = new CidadeFilter();
        filtro.setNome("   ");
        filtro.setEstado(null);

        Page<Cidade> result = cidadesImpl.filtrar(filtro, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("SP", result.getContent().get(0).getEstado().getSigla());
        assertEquals("RJ", result.getContent().get(1).getEstado().getSigla());
    }

    private void assertCidade(Cidade atual, Cidade esperada) {
        assertEquals(esperada.getCodigo(), atual.getCodigo());
        assertEquals(esperada.getNome(), atual.getNome());
        assertEquals(esperada.getEstado().getCodigo(), atual.getEstado().getCodigo());
        assertEquals(esperada.getEstado().getNome(), atual.getEstado().getNome());
        assertEquals(esperada.getEstado().getSigla(), atual.getEstado().getSigla());
    }
}
