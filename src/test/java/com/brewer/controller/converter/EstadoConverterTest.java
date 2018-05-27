package com.brewer.controller.converter;

import com.brewer.model.Estado;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EstadoConverterTest {

    private EstadoConverter converter;

    @Before
    public void metodoInicializaCenarioDeTeste() {
        this.converter = new EstadoConverter();
    }

    @Test
    public void testeMetodoConvertDeveRetornarEstadoComIdConvertido() {
        Estado result = converter.convert("1");
        assertEquals(new Long(1), result.getCodigo());
    }

    @Test
    public void testeMetodoConvertQuandoCodigoVazioDeveRetornarNull() {
        Estado result = converter.convert("");
        assertNull(result);
    }
}