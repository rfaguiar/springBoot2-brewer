package com.brewer.model;

import com.brewer.builder.CidadeBuilder;
import com.brewer.builder.EstadoBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CidadeTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Cidade first = criarCidadeBase();
        Cidade second = criarCidadeBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnoreEstadoInEqualsAndHashCodeBecauseItIsNotUsedByImplementation() {
        Cidade first = criarCidadeBase();
        Cidade second = criarCidadeBase();
        second.setEstado(EstadoBuilder.get().codigo(2L).nome("Outro Estado").sigla("OE").build());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarCidadeBase().toString())
                .contains("Cidade")
                .contains("nome='Campinas'");
    }

    private Cidade criarCidadeBase() {
        return CidadeBuilder.get()
                .codigo(1L)
                .nome("Campinas")
                .estado(EstadoBuilder.criarEstado())
                .build();
    }
}
