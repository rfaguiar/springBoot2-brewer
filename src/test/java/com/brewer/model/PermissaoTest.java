package com.brewer.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissaoTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Permissao first = new Permissao();
        first.setCodigo(1L);
        first.setNome("CADASTRAR_USUARIO");

        Permissao second = new Permissao();
        second.setCodigo(1L);
        second.setNome("CADASTRAR_USUARIO");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRequireNomeInAdditionToCodigoInEqualsAndHashCode() {
        Permissao first = new Permissao();
        first.setCodigo(1L);
        first.setNome("CADASTRAR_USUARIO");

        Permissao second = new Permissao();
        second.setCodigo(1L);
        second.setNome("EDITAR_USUARIO");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        Permissao permissao = new Permissao();
        permissao.setNome("CADASTRAR_USUARIO");

        assertThat(permissao.toString())
                .contains("Permissao")
                .contains("nome='CADASTRAR_USUARIO'");
    }
}
