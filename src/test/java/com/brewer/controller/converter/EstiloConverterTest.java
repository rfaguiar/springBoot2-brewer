package com.brewer.controller.converter;

import com.brewer.model.Estilo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EstiloConverterTest {

    private EstiloConverter converter;

    @Before
    public void metodoInicializaCenarioDeTeste() {
        this.converter = new EstiloConverter();
    }

    @Test
    public void testeMetodoConvertDeveRetornarEstiloComIdConvertido() {
        Estilo result = converter.convert("1");
        assertEquals(new Long(1), result.getCodigo());
    }

    @Test
    public void testeMetodoConvertQuandoCodigoVazioDeveRetornarNull() {
        Estilo result = converter.convert("");
        assertNull(result);
    }
}