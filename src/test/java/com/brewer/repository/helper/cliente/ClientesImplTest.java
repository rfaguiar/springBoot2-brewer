package com.brewer.repository.helper.cliente;

import com.brewer.builder.ClienteBuilder;
import com.brewer.helper.JPAHibernateTest;
import com.brewer.model.Cliente;
import com.brewer.repository.filter.ClienteFilter;
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

public class ClientesImplTest {

    private ClientesImpl clientesImpl;
    private Cliente cliente1;
    private Cliente cliente2;

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
        cliente1 = ClienteBuilder.criarCliente();
        cliente1.setNome("Carlos Silva");
        cliente1.setCpfOuCnpj("39053344705");
        cliente1.getEndereco().getCidade().setNome("Campinas");
        entityManager.persist(cliente1.getEndereco().getCidade().getEstado());
        entityManager.persist(cliente1.getEndereco().getCidade());
        entityManager.persist(cliente1.getEndereco().getEstado());
        entityManager.persist(cliente1);

        cliente2 = ClienteBuilder.criarCliente();
        cliente2.setNome("Maria Souza");
        cliente2.setCpfOuCnpj("11144477735");
        cliente2.getEndereco().getCidade().setNome("Valinhos");
        entityManager.persist(cliente2.getEndereco().getCidade().getEstado());
        entityManager.persist(cliente2.getEndereco().getCidade());
        entityManager.persist(cliente2.getEndereco().getEstado());
        entityManager.persist(cliente2);

        clientesImpl = new ClientesImpl(entityManager, paginacaoUtil);
    }

    @After
    public void tearDown() {
        JPAHibernateTest.roolbackEcloseEntityManager();
    }

    @Test
    public void testeMetodoFiltrarSemFiltrosDeveRetornarTodosClientesComConteudoCorreto() {
        Page<Cliente> result = clientesImpl.filtrar(new ClienteFilter(), pageable);

        assertEquals(2, result.getContent().size());
        assertCliente(result.getContent().get(0), cliente1);
        assertCliente(result.getContent().get(1), cliente2);
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroNuloDeveRetornarTodosClientes() {
        Page<Cliente> result = clientesImpl.filtrar(null, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Carlos Silva", result.getContent().get(0).getNome());
        assertEquals("Maria Souza", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoFiltrarComNomeParcialECpfDeveRetornarSomenteClienteCompativel() {
        ClienteFilter filtro = new ClienteFilter();
        filtro.setNome("sil");
        filtro.setCpfOuCnpj("39053344705");

        Page<Cliente> result = clientesImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertCliente(result.getContent().get(0), cliente1);
    }

    @Test
    public void testeMetodoFiltrarQuandoCamposTextoEstaoVaziosNaoDeveAplicarFiltros() {
        ClienteFilter filtro = new ClienteFilter();
        filtro.setNome(" ");
        filtro.setCpfOuCnpj("");

        Page<Cliente> result = clientesImpl.filtrar(filtro, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("39053344705", result.getContent().get(0).getCpfOuCnpjSemFormatacao());
        assertEquals("11144477735", result.getContent().get(1).getCpfOuCnpjSemFormatacao());
    }

    private void assertCliente(Cliente atual, Cliente esperado) {
        assertEquals(esperado.getCodigo(), atual.getCodigo());
        assertEquals(esperado.getNome(), atual.getNome());
        assertEquals(esperado.getCpfOuCnpjSemFormatacao(), atual.getCpfOuCnpjSemFormatacao());
        assertEquals(esperado.getEmail(), atual.getEmail());
        assertEquals(esperado.getEndereco().getCidade().getNome(), atual.getEndereco().getCidade().getNome());
        assertEquals(esperado.getEndereco().getCidade().getEstado().getSigla(),
                atual.getEndereco().getCidade().getEstado().getSigla());
    }
}
