package com.brewer.controller.converter;

import com.brewer.model.Cidade;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CidadeConverterTest {

    private CidadeConverter converter;

    @Before
    public void metodoInicializaCenarioDeTeste() {
        this.converter = new CidadeConverter();
    }

    @Test
    public void testeMetodoConvertDeveRetornarCidadeComIdConvertido() {
        Cidade result = converter.convert("1");
        assertEquals(new Long(1), result.getCodigo());
    }

    @Test
    public void testeMetodoConvertQuandoCodigoVazioDeveRetornarNull() {
        Cidade result = converter.convert("");
        assertNull(result);
    }
}