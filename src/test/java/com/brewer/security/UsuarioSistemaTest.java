package com.brewer.security;

import com.brewer.builder.UsuarioBuilder;
import com.brewer.model.Usuario;
import org.junit.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class UsuarioSistemaTest {

    @Test
    public void deveRetornarMesmaInstanciaDeUsuarioInformadaNoConstrutor() {
        Usuario usuario = criarUsuarioPadrao();

        UsuarioSistema usuarioSistema = criarUsuarioSistema(usuario);

        assertSame(usuario, usuarioSistema.getUsuario());
    }

    @Test
    public void deveConsiderarIgualQuandoComparadoComAMesmaInstancia() {
        UsuarioSistema usuarioSistema = criarUsuarioSistema(criarUsuarioPadrao());

        assertTrue(usuarioSistema.equals(usuarioSistema));
    }

    @Test
    public void naoDeveConsiderarIgualQuandoComparadoComNull() {
        UsuarioSistema usuarioSistema = criarUsuarioSistema(criarUsuarioPadrao());

        assertFalse(usuarioSistema.equals(null));
    }

    @Test
    public void naoDeveConsiderarIgualQuandoComparadoComObjetoDeOutraClasse() {
        UsuarioSistema usuarioSistema = criarUsuarioSistema(criarUsuarioPadrao());

        assertFalse(usuarioSistema.equals("outro-tipo"));
    }

    @Test
    public void deveConsiderarIgualQuandoPossuemMesmoUsuarioEDadosDoUserPai() {
        Usuario usuario = criarUsuarioPadrao();

        UsuarioSistema primeiro = criarUsuarioSistema(usuario);
        UsuarioSistema segundo = criarUsuarioSistema(usuario);

        assertTrue(primeiro.equals(segundo));
        assertTrue(segundo.equals(primeiro));
    }

    @Test
    public void naoDeveConsiderarIgualQuandoUsuarioForDiferenteMesmoComMesmoUsernameESenha() {
        Usuario primeiroUsuario = criarUsuarioPadrao();
        Usuario segundoUsuario = UsuarioBuilder.get()
                .nome("outroNome")
                .senha(primeiroUsuario.getSenha())
                .confirmacaoSenha(primeiroUsuario.getConfirmacaoSenha())
                .email(primeiroUsuario.getEmail())
                .ativo(primeiroUsuario.getAtivo())
                .grupos(primeiroUsuario.getGrupos())
                .dataNascimento(primeiroUsuario.getDataNascimento())
                .build();

        UsuarioSistema primeiro = criarUsuarioSistema(primeiroUsuario);
        UsuarioSistema segundo = criarUsuarioSistema(segundoUsuario);

        assertFalse(primeiro.equals(segundo));
    }

    @Test
    public void naoDeveConsiderarIgualQuandoDadosDoUserPaiForemDiferentes() {
        Usuario primeiroUsuario = criarUsuarioPadrao();
        Usuario segundoUsuario = UsuarioBuilder.get()
                .nome(primeiroUsuario.getNome())
                .senha(primeiroUsuario.getSenha())
                .confirmacaoSenha(primeiroUsuario.getConfirmacaoSenha())
                .email("outro-email@teste.com")
                .ativo(primeiroUsuario.getAtivo())
                .grupos(primeiroUsuario.getGrupos())
                .dataNascimento(primeiroUsuario.getDataNascimento())
                .build();

        UsuarioSistema primeiro = criarUsuarioSistema(primeiroUsuario);
        UsuarioSistema segundo = criarUsuarioSistema(segundoUsuario);

        assertFalse(primeiro.equals(segundo));
    }

    @Test
    public void deveGerarMesmoHashCodeParaObjetosIguais() {
        Usuario primeiroUsuario = criarUsuarioPadrao();
        Usuario segundoUsuario = UsuarioBuilder.get()
                .nome(primeiroUsuario.getNome())
                .senha(primeiroUsuario.getSenha())
                .confirmacaoSenha(primeiroUsuario.getConfirmacaoSenha())
                .email(primeiroUsuario.getEmail())
                .ativo(primeiroUsuario.getAtivo())
                .grupos(primeiroUsuario.getGrupos())
                .dataNascimento(primeiroUsuario.getDataNascimento())
                .build();

        UsuarioSistema primeiro = criarUsuarioSistema(primeiroUsuario);
        UsuarioSistema segundo = criarUsuarioSistema(segundoUsuario);

        assertEquals(primeiro.hashCode(), segundo.hashCode());
    }

    @Test
    public void deveGerarHashCodeDiferenteQuandoUsuarioForDiferente() {
        Usuario primeiroUsuario = criarUsuarioPadrao();
        Usuario segundoUsuario = UsuarioBuilder.get()
                .nome("nome-diferente")
                .senha(primeiroUsuario.getSenha())
                .confirmacaoSenha(primeiroUsuario.getConfirmacaoSenha())
                .email(primeiroUsuario.getEmail())
                .ativo(primeiroUsuario.getAtivo())
                .grupos(primeiroUsuario.getGrupos())
                .dataNascimento(primeiroUsuario.getDataNascimento())
                .build();

        UsuarioSistema primeiro = criarUsuarioSistema(primeiroUsuario);
        UsuarioSistema segundo = criarUsuarioSistema(segundoUsuario);

        assertNotEquals(primeiro.hashCode(), segundo.hashCode());
    }

    @Test
    public void deveRetornarToStringComInformacoesDoUsuario() {
        UsuarioSistema usuarioSistema = criarUsuarioSistema(criarUsuarioPadrao());

        assertThat(usuarioSistema.toString())
                .isNotBlank()
                .contains("usuario=");
    }

    private UsuarioSistema criarUsuarioSistema(Usuario usuario) {
        return new UsuarioSistema(usuario, criarAuthoritiesPadrao());
    }

    private Usuario criarUsuarioPadrao() {
        return UsuarioBuilder.criarUsuario();
    }

    private Collection<SimpleGrantedAuthority> criarAuthoritiesPadrao() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
