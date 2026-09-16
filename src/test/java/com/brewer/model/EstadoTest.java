package com.brewer.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EstadoTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Estado first = new Estado();
        first.setCodigo(1L);
        first.setNome("São Paulo");
        first.setSigla("SP");

        Estado second = new Estado();
        second.setCodigo(1L);
        second.setNome("São Paulo");
        second.setSigla("SP");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRequireNomeAndSiglaInAdditionToCodigoInEqualsAndHashCode() {
        Estado first = new Estado();
        first.setCodigo(1L);
        first.setNome("São Paulo");
        first.setSigla("SP");

        Estado second = new Estado();
        second.setCodigo(1L);
        second.setNome("Rio de Janeiro");
        second.setSigla("RJ");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        Estado estado = new Estado();
        estado.setNome("São Paulo");
        estado.setSigla("SP");

        assertThat(estado.toString())
                .contains("Estado")
                .contains("nome='São Paulo'")
                .contains("sigla='SP'");
    }
}