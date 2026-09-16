package com.brewer.model;

import com.brewer.builder.GrupoBuilder;
import com.brewer.builder.UsuarioBuilder;
import com.brewer.builder.UsuarioGrupoBuilder;
import com.brewer.builder.UsuarioGrupoIdBuilder;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class UsuarioGrupoTest {

    @Test
    public void testeMetodoBuilder() {
        UsuarioGrupo usuarioGrupo = UsuarioGrupoBuilder.criarUsuarioGrupoId();
        UsuarioGrupo usuarioGrupo1 = UsuarioGrupoBuilder.criarUsuarioGrupoId();

        assertNotNull(usuarioGrupo.getId());
        assertNotNull(usuarioGrupo.getId().getGrupo());
        assertNotNull(usuarioGrupo.getId().getUsuario());
        assertThat(usuarioGrupo).isEqualTo(usuarioGrupo1);
    }

    @Test
    public void shouldHonorEqualsAndHashCodeContract() {
        UsuarioGrupoId firstId = UsuarioGrupoIdBuilder.get()
                .usuario(UsuarioBuilder.get().codigo(1L).nome("usuario-1").email("usuario-1@teste.com").build())
                .grupo(GrupoBuilder.get().codigo(1L).nome("grupo-1").build())
                .build();
        UsuarioGrupoId secondId = UsuarioGrupoIdBuilder.get()
                .usuario(UsuarioBuilder.get().codigo(2L).nome("usuario-2").email("usuario-2@teste.com").build())
                .grupo(GrupoBuilder.get().codigo(2L).nome("grupo-2").build())
                .build();

        EqualsVerifier.forClass(UsuarioGrupo.class)
                .withPrefabValues(UsuarioGrupoId.class, firstId, secondId)
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE, Warning.SURROGATE_KEY)
                .verify();
    }

    @Test
    public void shouldRenderToStringWithEmbeddedId() {
        UsuarioGrupo usuarioGrupo = UsuarioGrupoBuilder.criarUsuarioGrupoId();

        assertThat(usuarioGrupo.toString())
                .contains("UsuarioGrupo")
                .contains("id=");
    }

}