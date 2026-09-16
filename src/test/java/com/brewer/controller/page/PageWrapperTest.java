package com.brewer.controller.page;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageWrapperTest {

    private static final String URL_BASE = "http://localhost/cervejas";
    private static final String QUERY_STRING = "nome=ipa";
    private static final String URL_COM_QUERY = URL_BASE + "?" + QUERY_STRING;

    @Mock
    private HttpServletRequest mockHttpRequest;
    @Mock
    private Page<Object> mockPage;
    @Mock
    private Sort mockSort;
    @Mock
    private Sort.Order mockOrder;
    @Mock
    private UriComponentsBuilder mockUriBuilder;
    @Mock
    private UriComponentsBuilder mockUriBuilderOrdenacao;
    @Mock
    private UriComponents mockUriComponentsBase;
    @Mock
    private UriComponents mockUriComponentsPaginacao;
    @Mock
    private UriComponents mockUriComponentsOrdenacao;

    private MockedStatic<UriComponentsBuilder> mockedUriComponentsBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(mockHttpRequest.getRequestURL()).thenReturn(new StringBuffer(URL_BASE));
        Mockito.when(mockHttpRequest.getQueryString()).thenReturn(QUERY_STRING);
        Mockito.when(mockPage.getSort()).thenReturn(mockSort);
        Mockito.when(mockUriBuilder.build(true)).thenReturn(mockUriComponentsBase);
        Mockito.when(mockUriComponentsBase.encode()).thenReturn(mockUriComponentsBase);
        Mockito.when(mockUriComponentsBase.toUriString()).thenReturn(URL_COM_QUERY);

        mockedUriComponentsBuilder = Mockito.mockStatic(UriComponentsBuilder.class, Mockito.CALLS_REAL_METHODS);
        mockedUriComponentsBuilder.when(() -> UriComponentsBuilder.fromHttpUrl(URL_COM_QUERY)).thenReturn(mockUriBuilder);
        mockedUriComponentsBuilder.when(() -> UriComponentsBuilder.fromHttpUrl(URL_BASE)).thenReturn(mockUriBuilder);
        mockedUriComponentsBuilder.when(() -> UriComponentsBuilder.fromUriString(URL_COM_QUERY)).thenReturn(mockUriBuilderOrdenacao);
        mockedUriComponentsBuilder.clearInvocations();
    }

    @After
    public void tearDown() {
        mockedUriComponentsBuilder.close();
    }

    @Test
    public void deveConstruirUrlComQueryStringQuandoElaExistir() {
        new PageWrapper<>(mockPage, mockHttpRequest);

        mockedUriComponentsBuilder.verify(() -> UriComponentsBuilder.fromHttpUrl(URL_COM_QUERY));
    }

    @Test
    public void deveConstruirUrlSemQueryStringQuandoElaForNula() {
        Mockito.when(mockHttpRequest.getQueryString()).thenReturn(null);

        new PageWrapper<>(mockPage, mockHttpRequest);

        mockedUriComponentsBuilder.verify(() -> UriComponentsBuilder.fromHttpUrl(URL_BASE));
    }

    @Test
    public void deveRetornarPaginaAtual() {
        Mockito.when(mockPage.getNumber()).thenReturn(3);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals(3, pageWrapper.getAtual());
        Mockito.verify(mockPage).getNumber();
    }

    @Test
    public void deveRetornarTotalDePaginas() {
        Mockito.when(mockPage.getTotalPages()).thenReturn(7);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals(7, pageWrapper.getTotal());
        Mockito.verify(mockPage).getTotalPages();
    }

    @Test
    public void deveRetornarTrueQuandoForPrimeiraPagina() {
        Mockito.when(mockPage.isFirst()).thenReturn(true);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertTrue(pageWrapper.isPrimeira());
        Mockito.verify(mockPage).isFirst();
    }

    @Test
    public void deveRetornarFalseQuandoNaoForPrimeiraPagina() {
        Mockito.when(mockPage.isFirst()).thenReturn(false);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertFalse(pageWrapper.isPrimeira());
        Mockito.verify(mockPage).isFirst();
    }

    @Test
    public void deveRetornarTrueQuandoForUltimaPagina() {
        Mockito.when(mockPage.isLast()).thenReturn(true);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertTrue(pageWrapper.isUltima());
        Mockito.verify(mockPage).isLast();
    }

    @Test
    public void deveRetornarFalseQuandoNaoForUltimaPagina() {
        Mockito.when(mockPage.isLast()).thenReturn(false);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertFalse(pageWrapper.isUltima());
        Mockito.verify(mockPage).isLast();
    }

    @Test
    public void deveRetornarTrueQuandoConteudoEstiverVazio() {
        Mockito.when(mockPage.getContent()).thenReturn(Collections.emptyList());

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertTrue(pageWrapper.isVazia());
        Mockito.verify(mockPage).getContent();
    }

    @Test
    public void deveRetornarFalseQuandoConteudoNaoEstiverVazio() {
        Mockito.when(mockPage.getContent()).thenReturn(Arrays.asList(new Object()));

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertFalse(pageWrapper.isVazia());
        Mockito.verify(mockPage).getContent();
    }

    @Test
    public void deveRetornarTrueQuandoPropriedadeEstiverOrdenada() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(mockOrder);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertTrue(pageWrapper.ordenada("codigo"));
        Mockito.verify(mockSort, Mockito.times(2)).getOrderFor("codigo");
    }

    @Test
    public void deveRetornarFalseQuandoOrdemDaPropriedadeNaoExistir() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(null);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertFalse(pageWrapper.ordenada("codigo"));
        Mockito.verify(mockSort).getOrderFor("codigo");
    }

    @Test
    public void deveRetornarFalseQuandoPaginaNaoPossuirOrdenacao() {
        Mockito.when(mockPage.getSort()).thenReturn(null);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertFalse(pageWrapper.ordenada("codigo"));
        Mockito.verify(mockPage).getSort();
    }

    @Test
    public void deveInverterDirecaoParaAscQuandoNaoExistirOrder() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(null);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals("asc", pageWrapper.inverterDirecao("codigo"));
        Mockito.verify(mockSort).getOrderFor("codigo");
    }

    @Test
    public void deveInverterDirecaoParaDescQuandoOrderAtualForAsc() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(mockOrder);
        Mockito.when(mockOrder.getDirection()).thenReturn(Sort.Direction.ASC);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals("desc", pageWrapper.inverterDirecao("codigo"));
        Mockito.verify(mockOrder).getDirection();
    }

    @Test
    public void deveManterDirecaoAscQuandoOrderAtualNaoForAsc() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(mockOrder);
        Mockito.when(mockOrder.getDirection()).thenReturn(Sort.Direction.DESC);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals("asc", pageWrapper.inverterDirecao("codigo"));
        Mockito.verify(mockOrder).getDirection();
    }

    @Test
    public void deveRetornarTrueQuandoOrdenacaoResultarEmDescendente() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(mockOrder);
        Mockito.when(mockOrder.getDirection()).thenReturn(Sort.Direction.DESC);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertTrue(pageWrapper.descendente("codigo"));
    }

    @Test
    public void deveRetornarFalseQuandoOrdenacaoResultarEmAscendente() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(mockOrder);
        Mockito.when(mockOrder.getDirection()).thenReturn(Sort.Direction.ASC);

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertFalse(pageWrapper.descendente("codigo"));
    }

    @Test
    public void deveMontarUrlParaPaginaInformada() {
        Mockito.when(mockUriBuilder.replaceQueryParam("page", 2)).thenReturn(mockUriBuilder);
        Mockito.when(mockUriBuilder.build(true)).thenReturn(mockUriComponentsPaginacao);
        Mockito.when(mockUriComponentsPaginacao.encode()).thenReturn(mockUriComponentsPaginacao);
        Mockito.when(mockUriComponentsPaginacao.toUriString()).thenReturn(URL_COM_QUERY + "&page=2");

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals(URL_COM_QUERY + "&page=2", pageWrapper.urlParaPagina(2));
        Mockito.verify(mockUriBuilder).replaceQueryParam("page", 2);
    }

    @Test
    public void deveMontarUrlOrdenadaComDirecaoInvertida() {
        Mockito.when(mockSort.getOrderFor("codigo")).thenReturn(mockOrder);
        Mockito.when(mockOrder.getDirection()).thenReturn(Sort.Direction.ASC);
        Mockito.when(mockUriBuilderOrdenacao.replaceQueryParam("sort", "codigo,desc"))
                .thenReturn(mockUriBuilderOrdenacao);
        Mockito.when(mockUriBuilderOrdenacao.build(true)).thenReturn(mockUriComponentsOrdenacao);
        Mockito.when(mockUriComponentsOrdenacao.encode()).thenReturn(mockUriComponentsOrdenacao);
        Mockito.when(mockUriComponentsOrdenacao.toUriString()).thenReturn(URL_COM_QUERY + "&sort=codigo,desc");

        PageWrapper<Object> pageWrapper = criarPageWrapper();

        assertEquals(URL_COM_QUERY + "&sort=codigo,desc", pageWrapper.urlOrdenada("codigo"));
        Mockito.verify(mockUriBuilderOrdenacao).replaceQueryParam("sort", "codigo,desc");
    }

    private PageWrapper<Object> criarPageWrapper() {
        return new PageWrapper<>(mockPage, mockHttpRequest);
    }
}
