package com.brewer.service.event.venda;

import com.brewer.builder.CervejaBuilder;
import com.brewer.builder.VendaBuilder;
import com.brewer.model.Cerveja;
import com.brewer.repository.Cervejas;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

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
    public void testeVendaVendaEmitida() {
        VendaEvent vendaEvent = new VendaEvent(VendaBuilder.criarVenda());
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        Mockito.when(mockCervejasRepo.getOne(ArgumentMatchers.any())).thenReturn(cerveja);
        listener.vendaEmitida(vendaEvent);
    }
}