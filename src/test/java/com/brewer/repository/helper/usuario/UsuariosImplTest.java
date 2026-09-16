package com.brewer.repository.helper.usuario;

import com.brewer.builder.GrupoBuilder;
import com.brewer.builder.PermissaoBuilder;
import com.brewer.builder.UsuarioBuilder;
import com.brewer.helper.JPAHibernateTest;
import com.brewer.model.Grupo;
import com.brewer.model.Permissao;
import com.brewer.model.Usuario;
import com.brewer.repository.filter.UsuarioFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UsuariosImplTest {

    private UsuariosImpl usuariosImpl;
    private Usuario usuario1;
    private Usuario usuario2;
    private Grupo grupoAdministrador;
    private Grupo grupoVendedor;

    @Mock
    private Pageable pageable;
    @Mock
    private Sort sort;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        EntityManager entityManager = JPAHibernateTest.getEntityManager();
        entityManager.getTransaction().begin();

        grupoAdministrador = criarGrupo(entityManager, "Administradores", "PERMISSAO_ADMIN");
        grupoVendedor = criarGrupo(entityManager, "Vendedores", "PERMISSAO_VENDA");

        usuario1 = UsuarioBuilder.criarUsuario();
        usuario1.setNome("Carlos Admin");
        usuario1.setEmail("carlos@brewer.com");
        usuario1.setGrupos(Arrays.asList(grupoAdministrador, grupoVendedor));
        entityManager.persist(usuario1);

        usuario2 = UsuarioBuilder.criarUsuario();
        usuario2.setNome("Maria Vendas");
        usuario2.setEmail("maria@brewer.com");
        usuario2.setGrupos(Collections.singletonList(grupoVendedor));
        usuario2.setConfirmacaoSenha(usuario2.getSenha());
        entityManager.persist(usuario2);

        usuariosImpl = new UsuariosImpl(entityManager, new PaginacaoUtil());
        Mockito.when(pageable.getPageSize()).thenReturn(10);
        Mockito.when(pageable.getPageNumber()).thenReturn(0);
        Mockito.when(pageable.getSort()).thenReturn(sort);
        List<Sort.Order> order = Collections.singletonList(new Sort.Order(Sort.Direction.ASC, "codigo"));
        Mockito.when(sort.iterator()).thenReturn(order.iterator());
        Mockito.when(sort.isSorted()).thenReturn(true);
    }

    @After
    public void tearDown() {
        JPAHibernateTest.roolbackEcloseEntityManager();
    }

    @Test
    public void testeMetodoFiltrarQuandoSemFiltrosDeveRetornarTodosUsuariosComConteudoCorreto() {
        Page<Usuario> result = usuariosImpl.filtrar(new UsuarioFilter(), pageable);

        assertEquals(2, result.getContent().size());
        assertUsuario(result.getContent().get(0), usuario1);
        assertUsuario(result.getContent().get(1), usuario2);
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroNuloDeveRetornarTodosUsuarios() {
        Page<Usuario> result = usuariosImpl.filtrar(null, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Carlos Admin", result.getContent().get(0).getNome());
        assertEquals("Maria Vendas", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltrosCombinadosSaoInformadosDeveRetornarApenasUsuarioCorrespondente() {
        UsuarioFilter filtro = new UsuarioFilter();
        filtro.setNome("Carlos");
        filtro.setEmail("carlos@");
        filtro.setGrupos(Arrays.asList(grupoAdministrador, grupoVendedor));

        Page<Usuario> result = usuariosImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertUsuario(result.getContent().get(0), usuario1);
        assertEquals(2, result.getContent().get(0).getGrupos().size());
        assertTrue(result.getContent().get(0).getGrupos().stream()
                .anyMatch(grupo -> "Administradores".equals(grupo.getNome())));
        assertTrue(result.getContent().get(0).getGrupos().stream()
                .anyMatch(grupo -> "Vendedores".equals(grupo.getNome())));
    }

    @Test
    public void testeMetodoFiltrarQuandoGruposVazioENomeEmailEmBrancoNaoDeveAplicarFiltros() {
        UsuarioFilter filtro = new UsuarioFilter();
        filtro.setNome("   ");
        filtro.setEmail("");
        filtro.setGrupos(new ArrayList<>());

        Page<Usuario> result = usuariosImpl.filtrar(filtro, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("Carlos Admin", result.getContent().get(0).getNome());
        assertEquals("Maria Vendas", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoPorEmailEAtivoDeveRetornarUsuarioAtivo() {
        Optional<Usuario> result = usuariosImpl.porEmailEAtivo("CARLOS@BREWER.COM");

        assertTrue(result.isPresent());
        assertEquals(usuario1.getCodigo(), result.get().getCodigo());
    }

    @Test
    public void testeMetodoPorEmailEAtivoQuandoEmailNaoExisteDeveRetornarVazio() {
        Optional<Usuario> result = usuariosImpl.porEmailEAtivo("inexistente@brewer.com");

        assertFalse(result.isPresent());
    }

    @Test
    public void testeMetodoPermissoesDeveRetornarPermissoesDistintasDoUsuarioInformado() {
        List<String> result = usuariosImpl.permissoes(usuario1);

        assertEquals(2, result.size());
        assertTrue(result.contains("PERMISSAO_ADMIN"));
        assertTrue(result.contains("PERMISSAO_VENDA"));
    }

    @Test
    public void testeMetodoBuscarComGruposDeveRetornarUsuarioPeloIdComListaDeGrupos() {
        Usuario result = usuariosImpl.buscarComGrupos(usuario1.getCodigo());

        assertUsuario(result, usuario1);
        assertEquals(2, result.getGrupos().size());
        assertTrue(result.getGrupos().stream()
                .flatMap(grupo -> grupo.getPermissoes().stream())
                .anyMatch(permissao -> "PERMISSAO_ADMIN".equals(permissao.getNome())));
    }

    private Grupo criarGrupo(EntityManager entityManager, String nomeGrupo, String nomePermissao) {
        Permissao permissao = PermissaoBuilder.get().nome(nomePermissao).build();
        entityManager.persist(permissao);

        Grupo grupo = GrupoBuilder.get().nome(nomeGrupo).permissoes(Collections.singletonList(permissao)).build();
        entityManager.persist(grupo);
        return grupo;
    }

    private void assertUsuario(Usuario atual, Usuario esperado) {
        assertEquals(esperado.getCodigo(), atual.getCodigo());
        assertEquals(esperado.getNome(), atual.getNome());
        assertEquals(esperado.getEmail(), atual.getEmail());
        assertEquals(esperado.getSenha(), atual.getSenha());
        assertEquals(esperado.getAtivo(), atual.getAtivo());
        assertEquals(esperado.getDataNascimento(), atual.getDataNascimento());
    }
}
