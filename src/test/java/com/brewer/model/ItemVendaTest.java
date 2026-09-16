package com.brewer.model;

import com.brewer.builder.CervejaBuilder;
import com.brewer.builder.ItemVendaBuilder;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemVendaTest {

    @Test
    public void shouldImplementEqualsAndHashCodeWithCurrentProductionBehavior() {
        ItemVenda first = criarItemVendaBase();
        ItemVenda second = criarItemVendaBase();

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldIgnoreCervejaAndVendaInEqualsAndHashCodeBecauseTheyAreNotUsedByImplementation() {
        ItemVenda first = criarItemVendaBase();
        ItemVenda second = criarItemVendaBase();
        second.setCerveja(CervejaBuilder.get().codigo(2L).sku("BB2222").nome("Outra").build());
        second.setVenda(new Venda());

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldRenderToStringWithKeyFields() {
        assertThat(criarItemVendaBase().toString())
                .contains("ItemVenda")
                .contains("quantidade=2")
                .contains("valorUnitario=9.90");
    }

    private ItemVenda criarItemVendaBase() {
        return ItemVendaBuilder.get()
                .codigo(1L)
                .quantidade(2)
                .valorUnitario(new BigDecimal("9.90"))
                .cerveja(CervejaBuilder.criarCerveja())
                .venda(new Venda())
                .build();
    }
}
