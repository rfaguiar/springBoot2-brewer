package com.brewer.repository.helper.estilo;

import com.brewer.helper.JPAHibernateTest;
import com.brewer.model.Estilo;
import com.brewer.repository.filter.EstiloFilter;
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

public class EstilosImplTest {

    private EstilosImpl estilosImpl;

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
        persistirEstilo(entityManager, "Amber Lager");
        persistirEstilo(entityManager, "Dark Lager");
        persistirEstilo(entityManager, "Pale Lager");
        persistirEstilo(entityManager, "Pilsner");

        estilosImpl = new EstilosImpl(entityManager, paginacaoUtil);
    }

    @After
    public void tearDown() {
        JPAHibernateTest.roolbackEcloseEntityManager();
    }

    @Test
    public void testeMetodoFiltrarQuandoNaoContemFiltrosDeveRetornarTodosRegistrosComConteudoCorreto() {
        Page<Estilo> result = estilosImpl.filtrar(new EstiloFilter(), pageable);

        assertEquals(4, result.getContent().size());
        assertEquals("Amber Lager", result.getContent().get(0).getNome());
        assertEquals("Dark Lager", result.getContent().get(1).getNome());
        assertEquals("Pale Lager", result.getContent().get(2).getNome());
        assertEquals("Pilsner", result.getContent().get(3).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroNuloDeveRetornarTodosRegistros() {
        Page<Estilo> result = estilosImpl.filtrar(null, pageable);

        assertEquals(4, result.getContent().size());
        assertEquals("Amber Lager", result.getContent().get(0).getNome());
        assertEquals("Pilsner", result.getContent().get(3).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroPorNomeDeveRetornarCompativeisComFiltro() {
        EstiloFilter filtro = new EstiloFilter();
        filtro.setNome("Amber Lager");

        Page<Estilo> result = estilosImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Amber Lager", result.getContent().get(0).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoNomeEmBrancoNaoDeveAplicarFiltro() {
        EstiloFilter filtro = new EstiloFilter();
        filtro.setNome("   ");

        Page<Estilo> result = estilosImpl.filtrar(filtro, pageable);

        assertEquals(4, result.getContent().size());
        assertEquals("Dark Lager", result.getContent().get(1).getNome());
    }

    @Test
    public void testeMetodoFiltrarQuandoFiltroPorNomeComParteDoTextoDeveRetornarCompativeisComFiltro() {
        EstiloFilter filtro = new EstiloFilter();
        filtro.setNome("Amber");

        Page<Estilo> result = estilosImpl.filtrar(filtro, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Amber Lager", result.getContent().get(0).getNome());
    }

    private void persistirEstilo(EntityManager entityManager, String nome) {
        Estilo estilo = new Estilo();
        estilo.setNome(nome);
        entityManager.persist(estilo);
    }
}
