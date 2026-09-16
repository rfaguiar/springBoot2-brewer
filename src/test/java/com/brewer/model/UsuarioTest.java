package com.brewer.model;

import com.brewer.builder.GrupoBuilder;
import com.brewer.builder.UsuarioBuilder;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UsuarioTest {

    @Test
    public void testeMetodosTransientAoBanco() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setAtivo(true);
        LocalDate dataNascimento = LocalDate.now();
        usuario.setDataNascimento(dataNascimento);


        assertTrue(usuario.isNovo());
        assertTrue(usuario.getAtivo());
        assertEquals(dataNascimento.toString(), usuario.getDataNascimento().toString());

    }

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Usuario first = criarUsuarioBase();
        Usuario second = criarUsuarioBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnoreGruposInEqualsAndHashCodeBecauseTheyAreNotUsedByImplementation() {
        Usuario first = criarUsuarioBase();
        Usuario second = criarUsuarioBase();
        second.setGrupos(Collections.emptyList());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarUsuarioBase().toString())
                .contains("Usuario")
                .contains("nome='Usuário Teste'")
                .contains("email='usuario@teste.com'");
    }

    private Usuario criarUsuarioBase() {
        return UsuarioBuilder.get()
                .codigo(1L)
                .nome("Usuário Teste")
                .email("usuario@teste.com")
                .senha("senha-segura")
                .confirmacaoSenha("senha-segura")
                .ativo(true)
                .grupos(GrupoBuilder.criarListaGrupos())
                .dataNascimento(LocalDate.of(1990, 5, 20))
                .build();
    }
}