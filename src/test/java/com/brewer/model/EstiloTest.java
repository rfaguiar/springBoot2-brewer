package com.brewer.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EstiloTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Estilo first = new Estilo();
        first.setCodigo(1L);
        first.setNome("IPA");

        Estilo second = new Estilo();
        second.setCodigo(1L);
        second.setNome("IPA");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRequireNomeInAdditionToCodigoInEqualsAndHashCode() {
        Estilo first = new Estilo();
        first.setCodigo(1L);
        first.setNome("IPA");

        Estilo second = new Estilo();
        second.setCodigo(1L);
        second.setNome("Stout");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        Estilo estilo = new Estilo();
        estilo.setNome("IPA");

        assertThat(estilo.toString())
                .contains("Estilo")
                .contains("nome='IPA'");
    }
}
