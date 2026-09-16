package com.brewer.repository.helper.cerveja;

import com.brewer.Constantes;
import com.brewer.builder.CervejaBuilder;
import com.brewer.dto.CervejaDTO;
import com.brewer.dto.ValorItensEstoque;
import com.brewer.helper.JPAHibernateTest;
import com.brewer.model.Cerveja;
import com.brewer.model.Estilo;
import com.brewer.model.Origem;
import com.brewer.model.Sabor;
import com.brewer.repository.filter.CervejaFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import com.brewer.storage.FotoStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CervejasImplTest {

    private CervejasImpl cervejasImpl;
    private Cerveja cerveja1;
    private Cerveja cerveja2;

    @Mock
    private FotoStorage fotoStorage;
    @Mock
    private PaginacaoUtil paginacaoUtil;
    @Mock
    private Pageable pageable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(paginacaoUtil.ordenar(Mockito.any(), Mockito.anyString())).thenReturn("");
        Mockito.when(fotoStorage.getUrl(Mockito.anyString()))
                .thenAnswer(invocation -> "https://cdn.test/" + invocation.getArgument(0));

        EntityManager entityManager = JPAHibernateTest.getEntityManager();
        entityManager.getTransaction().begin();

        cerveja1 = CervejaBuilder.criarCerveja();
        cerveja1.setNome("Hop Lager");
        cerveja1.setSku("CE1001");
        cerveja1.setValor(new BigDecimal("123.00"));
        cerveja1.setQuantidadeEstoque(500);
        cerveja1.setSabor(Sabor.ADOCICADA);
        cerveja1.setOrigem(Origem.NACIONAL);
        cerveja1.setFoto("hop-lager.png");
        cerveja1.getEstilo().setNome("Amber Lager");
        entityManager.persist(cerveja1.getEstilo());
        entityManager.persist(cerveja1);

        cerveja2 = CervejaBuilder.criarCerveja();
        cerveja2.setNome("Bitter Ale");
        cerveja2.setSku("CE2002");
        cerveja2.setValor(new BigDecimal("80.00"));
        cerveja2.setQuantidadeEstoque(200);
        cerveja2.setSabor(Sabor.AMARGA);
        cerveja2.setOrigem(Origem.INTERNACIONAL);
        cerveja2.setFoto("bitter-ale.png");
        cerveja2.getEstilo().setNome("Pale Ale");
        entityManager.persist(cerveja2.getEstilo());
        entityManager.persist(cerveja2);

        cervejasImpl = new CervejasImpl(entityManager, paginacaoUtil, fotoStorage);
    }

    @After
    public void tearDown() {
        JPAHibernateTest.roolbackEcloseEntityManager();
    }

    @Test
    public void testeMetodoFiltrarQuandoSemFiltrosDeveRetornarTodasCervejasComConteudoCorreto() {
        Page<Cerveja> result = cervejasImpl.filtrar(new CervejaFilter(), pageable);

        assertEquals(2, result.getContent().size());
        assertCerveja(result.getContent().get(0), cerveja1);
        assertCerveja(result.getContent().get(1), cerveja2);
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroNuloDeveRetornarTodasCervejas() {
        Page<Cerveja> result = cervejasImpl.filtrar(null, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Hop Lager", result.getContent().get(0).getNome());
        assertEquals("Bitter Ale", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoTodosOsFiltrosSaoInformadosDeveRetornarApenasACervejaCorrespondente() {
        CervejaFilter filtro = new CervejaFilter();
        filtro.setSku(cerveja1.getSku());
        filtro.setNome("Hop");
        filtro.setEstilo(cerveja1.getEstilo());
        filtro.setSabor(cerveja1.getSabor());
        filtro.setOrigem(cerveja1.getOrigem());
        filtro.setValorDe(new BigDecimal("123.00"));
        filtro.setValorAte(new BigDecimal("123.00"));

        Page<Cerveja> result = cervejasImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertCerveja(result.getContent().get(0), cerveja1);
    }

    @Test
    public void testeMetodoFiltrarQuandoEstiloNaoPossuiCodigoNaoDeveAplicarFiltroDeEstilo() {
        CervejaFilter filtro = new CervejaFilter();
        Estilo estiloSemCodigo = new Estilo();
        estiloSemCodigo.setNome(cerveja1.getEstilo().getNome());
        filtro.setEstilo(estiloSemCodigo);
        filtro.setNome(" ");
        filtro.setSku(null);

        Page<Cerveja> result = cervejasImpl.filtrar(filtro, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Hop Lager", result.getContent().get(0).getNome());
        assertEquals("Bitter Ale", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroPorNomeParcialDeveRetornarApenasItemCompativel() {
        CervejaFilter filtro = new CervejaFilter();
        filtro.setNome("lager");

        Page<Cerveja> result = cervejasImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertCerveja(result.getContent().get(0), cerveja1);
    }

    @Test
    public void testeMetodoPorSkuOuNomeQuandoArgumentoNomeDeveRetornarDtoComUrlDoThumbnail() {
        List<CervejaDTO> result = cervejasImpl.porSkuOuNome("hop");

        assertEquals(1, result.size());
        CervejaDTO dto = result.get(0);
        assertEquals(cerveja1.getCodigo(), dto.getCodigo());
        assertEquals(cerveja1.getSku(), dto.getSku());
        assertEquals(cerveja1.getNome(), dto.getNome());
        assertEquals(cerveja1.getOrigem().getDescricao(), dto.getOrigem());
        assertEquals(cerveja1.getValor(), dto.getValor());
        assertEquals(cerveja1.getFoto(), dto.getFoto());
        assertEquals("https://cdn.test/" + Constantes.THUMBNAIL_PREFIX + cerveja1.getFoto(), dto.getUrlThumbnailFoto());
        Mockito.verify(fotoStorage).getUrl(Constantes.THUMBNAIL_PREFIX + cerveja1.getFoto());
    }

    @Test
    public void testeMetodoPorSkuOuNomeQuandoNenhumItemCombinaDeveRetornarListaVazia() {
        List<CervejaDTO> result = cervejasImpl.porSkuOuNome("nao-existe");

        assertEquals(0, result.size());
    }

    @Test
    public void testeMetodoValorItensEstoqueDeveRetornarValorETotalCorretos() {
        ValorItensEstoque result = cervejasImpl.valorItensEstoque();

        assertEquals(Long.valueOf(700), result.getTotalItens());
        assertEquals("77500.00", result.getValor().toString());
    }

    private void assertCerveja(Cerveja atual, Cerveja esperada) {
        assertEquals(esperada.getCodigo(), atual.getCodigo());
        assertEquals(esperada.getSku(), atual.getSku());
        assertEquals(esperada.getNome(), atual.getNome());
        assertEquals(esperada.getValor(), atual.getValor());
        assertEquals(esperada.getQuantidadeEstoque(), atual.getQuantidadeEstoque());
        assertEquals(esperada.getSabor(), atual.getSabor());
        assertEquals(esperada.getOrigem(), atual.getOrigem());
        assertEquals(esperada.getEstilo().getCodigo(), atual.getEstilo().getCodigo());
        assertEquals(esperada.getEstilo().getNome(), atual.getEstilo().getNome());
        assertEquals(esperada.getFoto(), atual.getFoto());
    }
}
