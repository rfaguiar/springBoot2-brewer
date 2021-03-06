package com.brewer.service;

import com.brewer.builder.CidadeBuilder;
import com.brewer.model.Cidade;
import com.brewer.repository.Cidades;
import com.brewer.service.exception.NomeCidadeJaCadastradaException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CadastroCidadeServiceTest {

    private CadastroCidadeService service;
    @Mock
    private Cidades mockCidadesRepo;

    @Before
    public void iniciarCenarioDeTeste() {
        MockitoAnnotations.initMocks(this);
        this.service = new CadastroCidadeService(mockCidadesRepo);
    }

    @Test(expected = NomeCidadeJaCadastradaException.class)
    public void testeMetodoSalvarQuandoCidadeComMesmoNomeEEstadoDeveLancarExcepcaoComMsgAdequada() {
        try {
            Cidade cidade = CidadeBuilder.criarCidade();
            Optional<Cidade> cidadeOpt = Optional.of(cidade);
            Mockito.when(mockCidadesRepo.findByNomeAndEstado(cidade.getNome(), cidade.getEstado())).thenReturn(cidadeOpt);
            service.salvar(cidade);
        }catch (Exception e) {
            assertEquals("Nome de cidade já cadastrado", e.getMessage());
            throw e;
        }
        fail();
    }

    @Test
    public void testeMetodoSalvarDeveSalvarCidadeNova() {
        Cidade cidade = CidadeBuilder.criarCidade();
        Optional<Cidade> cidadeOpt = Optional.empty();
        Mockito.when(mockCidadesRepo.findByNomeAndEstado(cidade.getNome(), cidade.getEstado())).thenReturn(cidadeOpt);
        service.salvar(cidade);
    }
}