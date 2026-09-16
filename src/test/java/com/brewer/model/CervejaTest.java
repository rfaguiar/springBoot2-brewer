package com.brewer.model;

import com.brewer.Constantes;
import com.brewer.builder.CervejaBuilder;
import com.brewer.builder.EstiloBuilder;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CervejaTest {

    @Test
    public void testeMetodoGetFotoOuMockQuandoFotoCervejaEstiverNuloOuVazioDeveRetornarImagenFotoMock() {
        Cerveja cerveja = new Cerveja();
        assertEquals(Constantes.IMAGEN_CERVEJA_MOCK, cerveja.getFotoOuMock());
    }

    @Test
    public void testeMetodosTransientAPersistencia() {
        Cerveja cerveja = new Cerveja();
        cerveja.setCodigo(new Long(1));
        cerveja.setFoto("fotoTeste");
        cerveja.setDescricao("descTeste");
        cerveja.setTeorAlcoolico(new BigDecimal(10));
        cerveja.setComissao(new BigDecimal(20));
        cerveja.setQuantidadeEstoque(new Integer(10));
        cerveja.setContentType("png");
        cerveja.setUrlFoto("urlFotoTeste");
        cerveja.setUrlThumbnailFoto("urlThumbNailTeste");
        cerveja.setNovaFoto(true);

        assertFalse(cerveja.isNova());
        assertTrue(cerveja.temFoto());
        assertEquals("fotoTeste", cerveja.getFoto());
        assertEquals("descTeste", cerveja.getDescricao());
        assertEquals("10", cerveja.getTeorAlcoolico().toString());
        assertEquals("20", cerveja.getComissao().toString());
        assertEquals("10", cerveja.getQuantidadeEstoque().toString());
        assertEquals("png", cerveja.getContentType());
        assertEquals("urlFotoTeste", cerveja.getUrlFoto());
        assertEquals("urlThumbNailTeste", cerveja.getUrlThumbnailFoto());
        assertTrue(cerveja.isNovaFoto());
    }

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Cerveja first = criarCervejaBase();
        Cerveja second = criarCervejaBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnoreOrigemSaborAndEstiloInEqualsAndHashCodeBecauseTheyAreNotUsedByImplementation() {
        Cerveja first = criarCervejaBase();
        Cerveja second = criarCervejaBase();
        second.setOrigem(Origem.INTERNACIONAL);
        second.setSabor(Sabor.FORTE);
        second.setEstilo(EstiloBuilder.get().codigo(2L).nome("Stout").build());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarCervejaBase().toString())
                .contains("Cerveja")
                .contains("sku='AA1111'")
                .contains("nome='Pilsen Teste'")
                .contains("descricao='Descricao teste'")
                .contains("valor=12.50");
    }

    private Cerveja criarCervejaBase() {
        return CervejaBuilder.get()
                .codigo(1L)
                .sku("AA1111")
                .nome("Pilsen Teste")
                .descricao("Descricao teste")
                .valor(new BigDecimal("12.50"))
                .teorAlcoolico(new BigDecimal("5.0"))
                .comissao(new BigDecimal("1.5"))
                .quantidadeEstoque(8)
                .origem(Origem.NACIONAL)
                .sabor(Sabor.SUAVE)
                .estilo(EstiloBuilder.get().codigo(1L).nome("Pilsen").build())
                .foto("foto.png")
                .contentType("image/png")
                .novaFoto(true)
                .urlFoto("https://teste/foto.png")
                .urlThumbnailFoto("https://teste/thumb.png")
                .build();
    }
}