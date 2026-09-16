package com.brewer.service.event.venda;

import com.brewer.builder.CervejaBuilder;
import com.brewer.builder.ItemVendaBuilder;
import com.brewer.builder.VendaBuilder;
import com.brewer.model.Cerveja;
import com.brewer.model.ItemVenda;
import com.brewer.model.Venda;
import com.brewer.repository.Cervejas;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class VendaListenerTest {

    private VendaListener listener;
    @Mock
    private Cervejas mockCervejasRepo;

    @Before
    public void iniciarCenarioDeTeste() {
        MockitoAnnotations.initMocks(this);
        this.listener = new VendaListener(mockCervejasRepo);
    }

    @Test
    public void testeVendaVendaEmitidaDeveSubtrairQuantidadeVendidaDoEstoque() {
        Cerveja cervejaBase = CervejaBuilder.criarCerveja();
        Cerveja cerveja = Mockito.spy(CervejaBuilder.get()
                .codigo(1L)
                .sku(cervejaBase.getSku())
                .nome(cervejaBase.getNome())
                .descricao(cervejaBase.getDescricao())
                .valor(cervejaBase.getValor())
                .teorAlcoolico(cervejaBase.getTeorAlcoolico())
                .comissao(cervejaBase.getComissao())
                .quantidadeEstoque(100)
                .origem(cervejaBase.getOrigem())
                .sabor(cervejaBase.getSabor())
                .estilo(cervejaBase.getEstilo())
                .foto(cervejaBase.getFoto())
                .contentType(cervejaBase.getContentType())
                .novaFoto(cervejaBase.isNovaFoto())
                .urlFoto(cervejaBase.getUrlFoto())
                .urlThumbnailFoto(cervejaBase.getUrlThumbnailFoto())
                .build());
        ItemVenda itemVenda = ItemVendaBuilder.get()
                .cerveja(CervejaBuilder.get().codigo(1L).build())
                .quantidade(10)
                .valorUnitario(cerveja.getValor())
                .build();
        Venda venda = VendaBuilder.get().itens(Collections.singletonList(itemVenda)).build();
        VendaEvent vendaEvent = new VendaEvent(venda);

        Mockito.when(mockCervejasRepo.getOne(1L)).thenReturn(cerveja);

        listener.vendaEmitida(vendaEvent);

        assertEquals(Integer.valueOf(90), cerveja.getQuantidadeEstoque());
        Mockito.verify(cerveja).setQuantidadeEstoque(90);
        Mockito.verify(mockCervejasRepo).save(cerveja);
    }
}