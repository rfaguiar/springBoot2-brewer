package com.brewer.repository.helper.venda;

import com.brewer.builder.ItemVendaBuilder;
import com.brewer.builder.VendaBuilder;
import com.brewer.dto.VendaMes;
import com.brewer.dto.VendaOrigem;
import com.brewer.helper.JPAHibernateTest;
import com.brewer.model.Cerveja;
import com.brewer.model.Estilo;
import com.brewer.model.ItemVenda;
import com.brewer.model.StatusVenda;
import com.brewer.model.Venda;
import com.brewer.repository.filter.VendaFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VendasImplTest {

    private VendasImpl vendasImpl;
    private Venda venda1;
    private List<ItemVenda> itemVendas;
    @Mock
    private PaginacaoUtil mockPaginacaoUtil;
    @Mock
    private Pageable mockPageable;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        org.mockito.Mockito.when(mockPaginacaoUtil.ordenar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn("");
        EntityManager entityManager = JPAHibernateTest.getEntityManager();

        venda1 = VendaBuilder.criarVenda();
        venda1.setCodigo(null);
        venda1.setItens(null);
        venda1.setUsuario(null);
        Venda venda2 = VendaBuilder.criarVenda();
        venda2.setCodigo(null);
        venda2.setItens(null);
        venda2.setUsuario(null);

        //persistencia Cliente
        entityManager.getTransaction().begin();

        //Persistencia ItemVenda, Cerveja e Estilo
        itemVendas = ItemVendaBuilder
                .criarListaItenVenda();
                itemVendas.forEach(itemVenda -> {
                    Cerveja cerveja = itemVenda.getCerveja();
                    Estilo estiloPersistido = entityManager.merge(cerveja.getEstilo());
                    cerveja.setEstilo(estiloPersistido);
                    Cerveja cervejaPersistida = entityManager.merge(itemVenda.getCerveja());
                    itemVenda.setCerveja(cervejaPersistida);
                    itemVenda.setVenda(venda1);
                    ItemVenda itemPersistido = entityManager.merge(itemVenda);
                    itemVenda.setCodigo(itemPersistido.getCodigo());
                });
        venda1.setItens(itemVendas);

        //Persistencia Vendas
        entityManager.persist(venda1.getCliente().getEndereco().getCidade().getEstado());
        entityManager.persist(venda1.getCliente().getEndereco().getCidade());
        entityManager.persist(venda1.getCliente().getEndereco().getEstado());
        entityManager.persist(venda1.getCliente());
        Venda persistido = entityManager.merge(venda1);
        venda1.setCodigo(persistido.getCodigo());

        entityManager.persist(venda2.getCliente().getEndereco().getCidade().getEstado());
        entityManager.persist(venda2.getCliente().getEndereco().getCidade());
        entityManager.persist(venda2.getCliente().getEndereco().getEstado());
        entityManager.persist(venda2.getCliente());
        entityManager.persist(venda2);

        vendasImpl = new VendasImpl(entityManager, mockPaginacaoUtil);
    }

    @After
    public void end() {
        JPAHibernateTest.roolbackEcloseEntityManager();
    }

    @Test
    public void testeMetodoFiltrarSemFiltrosDeveRetornarTodosRegistros() {
        Page<Venda> result = vendasImpl.filtrar(new VendaFilter(), mockPageable);
        assertEquals(2, result.getContent().size());
    }

    @Test
    public void testeMetodoFiltrarComFiltrosDeveRetornarUmRegistro() {
        VendaFilter filtro = new VendaFilter();
        filtro.setCodigo(venda1.getCodigo());
        filtro.setStatus(venda1.getStatus());
        filtro.setDesde(LocalDate.now().minusDays(1));
        filtro.setAte(LocalDate.now().plusDays(1));
        filtro.setValorMinimo(new BigDecimal(400));
        filtro.setValorMaximo(new BigDecimal(800));
        filtro.setNomeCliente(venda1.getCliente().getNome());
        filtro.setCpfOuCnpjCliente(venda1.getCliente().getCpfOuCnpjSemFormatacao());

        Page<Venda> result = vendasImpl.filtrar(filtro, mockPageable);

        assertEquals(1, result.getContent().size());
        assertEquals(venda1, result.getContent().get(0));
    }

    @Test
    public void buscarComItens() {
        Venda result = vendasImpl.buscarComItens(venda1.getCodigo());
        assertEquals(itemVendas.size(), result.getItens().size());
        assertEquals(itemVendas.get(0), result.getItens().get(0));
    }

    @Test
    public void valorTotalNoAno() {
        BigDecimal result = vendasImpl.valorTotalNoAno();
        assertEquals("912.00", result.toString());
    }

    @Test
    public void valorTotalNoMes() {
        BigDecimal result = vendasImpl.valorTotalNoMes();
        assertEquals("912.00", result.toString());
    }

    @Test
    public void valorTicketMedioNoAno() {
        BigDecimal result = vendasImpl.valorTicketMedioNoAno();
        assertEquals("456.00", result.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testeMetodoFiltrarDeveAplicarPaginacaoNaQuery() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<Venda> queryVendas = mock(TypedQuery.class);
        TypedQuery<Long> queryTotal = mock(TypedQuery.class);
        Pageable pageable = PageRequest.of(2, 4, Sort.unsorted());

        when(manager.createQuery(anyString(), eq(Venda.class))).thenReturn(queryVendas);
        when(manager.createQuery(anyString(), eq(Long.class))).thenReturn(queryTotal);
        when(queryVendas.getResultList()).thenReturn(Collections.emptyList());
        when(queryTotal.getSingleResult()).thenReturn(5L);

        VendasImpl repository = new VendasImpl(manager, new PaginacaoUtil());

        Page<Venda> result = repository.filtrar(null, pageable);

        assertEquals(5L, result.getTotalElements());
        verify(queryVendas).setFirstResult(8);
        verify(queryVendas).setMaxResults(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testeMetodoFiltrarDeveRetornarTotalExatoEAdicionarFiltroNaCountQuery() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<Venda> queryVendas = mock(TypedQuery.class);
        TypedQuery<Long> queryTotal = mock(TypedQuery.class);
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
        VendaFilter filtro = new VendaFilter();
        filtro.setCodigo(99L);
        filtro.setStatus(StatusVenda.EMITIDA);

        when(manager.createQuery(anyString(), eq(Venda.class))).thenReturn(queryVendas);
        when(manager.createQuery(anyString(), eq(Long.class))).thenReturn(queryTotal);
        when(queryVendas.getResultList()).thenReturn(Collections.emptyList());
        when(queryTotal.getSingleResult()).thenReturn(7L);

        VendasImpl repository = new VendasImpl(manager, new PaginacaoUtil());

        Page<Venda> result = repository.filtrar(filtro, pageable);
        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);

        assertEquals(7L, result.getTotalElements());
        verify(manager).createQuery(jpqlCaptor.capture(), eq(Long.class));
        assertEquals(
                "select count(v) from Venda v join v.cliente c where 1=1 and v.codigo = :codigo and v.status = :status",
                jpqlCaptor.getValue());
        verify(queryTotal).setParameter("codigo", 99L);
        verify(queryTotal).setParameter("status", StatusVenda.EMITIDA);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testeMetodoFiltrarComFiltroNuloNaoDeveAdicionarParametrosNaCountQuery() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<Venda> queryVendas = mock(TypedQuery.class);
        TypedQuery<Long> queryTotal = mock(TypedQuery.class);
        Pageable pageable = PageRequest.of(0, 5, Sort.unsorted());

        when(manager.createQuery(anyString(), eq(Venda.class))).thenReturn(queryVendas);
        when(manager.createQuery(anyString(), eq(Long.class))).thenReturn(queryTotal);
        when(queryVendas.getResultList()).thenReturn(Collections.emptyList());
        when(queryTotal.getSingleResult()).thenReturn(3L);

        VendasImpl repository = new VendasImpl(manager, new PaginacaoUtil());

        Page<Venda> result = repository.filtrar(null, pageable);
        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);

        assertEquals(3L, result.getTotalElements());
        verify(manager).createQuery(jpqlCaptor.capture(), eq(Long.class));
        assertEquals("select count(v) from Venda v join v.cliente c where 1=1", jpqlCaptor.getValue());
        verify(queryTotal, never()).setParameter(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testeMetodoFiltrarComFiltroVazioNaoDeveAdicionarParametrosNaCountQuery() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<Venda> queryVendas = mock(TypedQuery.class);
        TypedQuery<Long> queryTotal = mock(TypedQuery.class);
        Pageable pageable = PageRequest.of(0, 5, Sort.unsorted());

        when(manager.createQuery(anyString(), eq(Venda.class))).thenReturn(queryVendas);
        when(manager.createQuery(anyString(), eq(Long.class))).thenReturn(queryTotal);
        when(queryVendas.getResultList()).thenReturn(Collections.emptyList());
        when(queryTotal.getSingleResult()).thenReturn(4L);

        VendasImpl repository = new VendasImpl(manager, new PaginacaoUtil());

        Page<Venda> result = repository.filtrar(new VendaFilter(), pageable);
        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);

        assertEquals(4L, result.getTotalElements());
        verify(manager).createQuery(jpqlCaptor.capture(), eq(Long.class));
        assertEquals("select count(v) from Venda v join v.cliente c where 1=1", jpqlCaptor.getValue());
        verify(queryTotal, never()).setParameter(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void totalPorMesSemResultadosDevePreencherOsUltimosSeisMesesComZero() {
        EntityManager manager = mock(EntityManager.class);
        Query namedQuery = mock(Query.class);
        List<VendaMes> vendasMes = new ArrayList<>();

        when(manager.createNamedQuery("Vendas.totalPorMes")).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(vendasMes);

        VendasImpl repository = new VendasImpl(manager, mock(PaginacaoUtil.class));

        List<VendaMes> result = repository.totalPorMes();

        assertVendaMes(result, ultimosSeisMeses(), new int[]{0, 0, 0, 0, 0, 0});
    }

    @Test
    @SuppressWarnings("unchecked")
    public void totalPorMesComApenasUmMesDevePreencherOsMesesFaltantesComZero() {
        EntityManager manager = mock(EntityManager.class);
        Query namedQuery = mock(Query.class);
        List<String> meses = ultimosSeisMeses();
        List<VendaMes> vendasMes = new ArrayList<>();
        vendasMes.add(new VendaMes(meses.get(2), 9));

        when(manager.createNamedQuery("Vendas.totalPorMes")).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(vendasMes);

        VendasImpl repository = new VendasImpl(manager, mock(PaginacaoUtil.class));

        List<VendaMes> result = repository.totalPorMes();

        assertVendaMes(result, meses, new int[]{0, 0, 9, 0, 0, 0});
    }

    @Test
    @SuppressWarnings("unchecked")
    public void totalPorMesComMaisDeUmMesDeveManterTotaisNasPosicoesCorretas() {
        EntityManager manager = mock(EntityManager.class);
        Query namedQuery = mock(Query.class);
        List<String> meses = ultimosSeisMeses();
        List<VendaMes> vendasMes = new ArrayList<>();
        vendasMes.add(new VendaMes(meses.get(0), 10));
        vendasMes.add(new VendaMes(meses.get(3), 7));
        vendasMes.add(new VendaMes(meses.get(5), 2));

        when(manager.createNamedQuery("Vendas.totalPorMes")).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(vendasMes);

        VendasImpl repository = new VendasImpl(manager, mock(PaginacaoUtil.class));

        List<VendaMes> result = repository.totalPorMes();

        assertVendaMes(result, meses, new int[]{10, 0, 0, 7, 0, 2});
    }

    @Test
    @SuppressWarnings("unchecked")
    public void totalPorOrigemSemResultadosDevePreencherOsUltimosSeisMesesComZero() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<VendaOrigem> namedQuery = mock(TypedQuery.class);
        List<VendaOrigem> vendasOrigem = new ArrayList<>();

        when(manager.createNamedQuery("Vendas.porOrigem", VendaOrigem.class)).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(vendasOrigem);

        VendasImpl repository = new VendasImpl(manager, mock(PaginacaoUtil.class));

        List<VendaOrigem> result = repository.totalPorOrigem();

        assertVendaOrigem(result, ultimosSeisMeses(), new int[][]{
                {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void totalPorOrigemComApenasUmMesDevePreencherOsMesesFaltantesComZero() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<VendaOrigem> namedQuery = mock(TypedQuery.class);
        List<String> meses = ultimosSeisMeses();
        List<VendaOrigem> vendasOrigem = new ArrayList<>();
        vendasOrigem.add(new VendaOrigem(meses.get(1), 3, 8));

        when(manager.createNamedQuery("Vendas.porOrigem", VendaOrigem.class)).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(vendasOrigem);

        VendasImpl repository = new VendasImpl(manager, mock(PaginacaoUtil.class));

        List<VendaOrigem> result = repository.totalPorOrigem();

        assertVendaOrigem(result, meses, new int[][]{
                {0, 0}, {3, 8}, {0, 0}, {0, 0}, {0, 0}, {0, 0}
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void totalPorOrigemComMaisDeUmMesDeveManterTotaisNasPosicoesCorretas() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<VendaOrigem> namedQuery = mock(TypedQuery.class);
        List<String> meses = ultimosSeisMeses();
        List<VendaOrigem> vendasOrigem = new ArrayList<>();
        vendasOrigem.add(new VendaOrigem(meses.get(0), 4, 6));
        vendasOrigem.add(new VendaOrigem(meses.get(2), 1, 9));
        vendasOrigem.add(new VendaOrigem(meses.get(4), 7, 2));

        when(manager.createNamedQuery("Vendas.porOrigem", VendaOrigem.class)).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(vendasOrigem);

        VendasImpl repository = new VendasImpl(manager, mock(PaginacaoUtil.class));

        List<VendaOrigem> result = repository.totalPorOrigem();

        assertVendaOrigem(result, meses, new int[][]{
                {4, 6}, {0, 0}, {1, 9}, {0, 0}, {7, 2}, {0, 0}
        });
    }

    private List<String> ultimosSeisMeses() {
        List<String> meses = new ArrayList<>();
        LocalDate data = LocalDate.now();

        for (int i = 0; i < 6; i++) {
            meses.add(String.format("%d/%02d", data.getYear(), data.getMonthValue()));
            data = data.minusMonths(1);
        }

        return meses;
    }

    private void assertVendaMes(List<VendaMes> result, List<String> mesesEsperados, int[] totaisEsperados) {
        assertEquals(6, result.size());

        for (int i = 0; i < mesesEsperados.size(); i++) {
            assertEquals(mesesEsperados.get(i), result.get(i).getMes());
            assertEquals(Integer.valueOf(totaisEsperados[i]), result.get(i).getTotal());
        }
    }

    private void assertVendaOrigem(List<VendaOrigem> result, List<String> mesesEsperados, int[][] totaisEsperados) {
        assertEquals(6, result.size());

        for (int i = 0; i < mesesEsperados.size(); i++) {
            assertEquals(mesesEsperados.get(i), result.get(i).getMes());
            assertEquals(Integer.valueOf(totaisEsperados[i][0]), result.get(i).getTotalNacional());
            assertEquals(Integer.valueOf(totaisEsperados[i][1]), result.get(i).getTotalInternacional());
        }
    }
}