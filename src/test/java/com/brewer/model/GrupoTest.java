package com.brewer.model;

import com.brewer.builder.GrupoBuilder;
import com.brewer.builder.PermissaoBuilder;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GrupoTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Grupo first = criarGrupoBase();
        Grupo second = criarGrupoBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnorePermissoesInEqualsAndHashCodeBecauseTheyAreNotUsedByImplementation() {
        Grupo first = criarGrupoBase();
        Grupo second = criarGrupoBase();
        second.setPermissoes(Collections.emptyList());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarGrupoBase().toString())
                .contains("Grupo")
                .contains("nome='Administradores'");
    }

    private Grupo criarGrupoBase() {
        return GrupoBuilder.get()
                .codigo(1L)
                .nome("Administradores")
                .permissoes(PermissaoBuilder.criarListaPermissao())
                .build();
    }
}
