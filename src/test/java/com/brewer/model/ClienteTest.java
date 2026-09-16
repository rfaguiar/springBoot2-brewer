package com.brewer.model;

import com.brewer.builder.ClienteBuilder;
import com.brewer.builder.EnderecoBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClienteTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Cliente first = criarClienteBase();
        Cliente second = criarClienteBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnoreEnderecoInEqualsAndHashCodeBecauseItIsNotUsedByImplementation() {
        Cliente first = criarClienteBase();
        Cliente second = criarClienteBase();
        second.setEndereco(EnderecoBuilder.get().logradouro("rua diferente").numero("999").build());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarClienteBase().toString())
                .contains("Cliente")
                .contains("nome='Cliente Teste'")
                .contains("cpfOuCnpj='12345678901'")
                .contains("telefone='19999999999'")
                .contains("email='cliente@teste.com'");
    }

    private Cliente criarClienteBase() {
        return ClienteBuilder.get()
                .codigo(1L)
                .nome("Cliente Teste")
                .tipoPessoa(TipoPessoa.FISICA)
                .cpfOuCnpj("12345678901")
                .telefone("19999999999")
                .email("cliente@teste.com")
                .endereco(EnderecoBuilder.criarEndereco())
                .build();
    }
}
