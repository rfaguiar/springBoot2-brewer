package com.brewer.model;

import com.brewer.builder.CidadeBuilder;
import com.brewer.builder.EnderecoBuilder;
import com.brewer.builder.EstadoBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnderecoTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        Endereco first = criarEnderecoBase();
        Endereco second = criarEnderecoBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnoreCidadeAndEstadoInEqualsAndHashCodeBecauseTheyAreNotUsedByImplementation() {
        Endereco first = criarEnderecoBase();
        Endereco second = criarEnderecoBase();
        second.setCidade(CidadeBuilder.get().codigo(99L).nome("outra cidade").build());
        second.setEstado(EstadoBuilder.get().codigo(98L).nome("Outro Estado").sigla("OE").build());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarEnderecoBase().toString())
                .contains("Endereco")
                .contains("logradouro='Rua 1'")
                .contains("numero='100'")
                .contains("complemento='Casa'")
                .contains("cep='13179180'");
    }

    private Endereco criarEnderecoBase() {
        return EnderecoBuilder.get()
                .logradouro("Rua 1")
                .numero("100")
                .complemento("Casa")
                .cep("13179180")
                .cidade(CidadeBuilder.criarCidade())
                .estado(EstadoBuilder.criarEstado())
                .build();
    }
}
