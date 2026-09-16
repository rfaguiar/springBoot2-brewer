package com.brewer.model;

import com.brewer.builder.GrupoBuilder;
import com.brewer.builder.UsuarioBuilder;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UsuarioGrupoIdTest {

    @Test
    public void shouldHonorEqualsAndHashCodeContract() {
        Usuario firstUser = UsuarioBuilder.get().codigo(1L).nome("usuario-1").email("usuario-1@teste.com").build();
        Usuario secondUser = UsuarioBuilder.get().codigo(2L).nome("usuario-2").email("usuario-2@teste.com").build();
        Grupo firstGroup = GrupoBuilder.get().codigo(1L).nome("grupo-1").build();
        Grupo secondGroup = GrupoBuilder.get().codigo(2L).nome("grupo-2").build();

        EqualsVerifier.forClass(UsuarioGrupoId.class)
                .withPrefabValues(Usuario.class, firstUser, secondUser)
                .withPrefabValues(Grupo.class, firstGroup, secondGroup)
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE, Warning.JPA_GETTER)
                .verify();
    }

    @Test
    public void shouldExposeNonEmptyInheritedToString() {
        assertThat(new UsuarioGrupoId().toString()).contains("UsuarioGrupoId");
    }
}
