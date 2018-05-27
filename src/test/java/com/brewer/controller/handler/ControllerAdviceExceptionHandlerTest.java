package com.brewer.controller.handler;

import com.brewer.service.exception.NomeEstiloJaCadastradoException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.*;

public class ControllerAdviceExceptionHandlerTest {

    private ControllerAdviceExceptionHandler controllerAdvice;

    @Before
    public void metodoInicializaCenarioDeTeste() {
        this.controllerAdvice = new ControllerAdviceExceptionHandler();
    }

    @Test
    public void testeMetodoHandleNomeEstiloJaCadastradoExceptionDeveEncapsularErroNaRequisicaoComCodigo400EMsgDaExcecao() {
        NomeEstiloJaCadastradoException exception = new NomeEstiloJaCadastradoException("excessao");
        ResponseEntity<String> result = controllerAdvice.handleNomeEstiloJaCadastradoException(exception);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("excessao", result.getBody());
    }

}