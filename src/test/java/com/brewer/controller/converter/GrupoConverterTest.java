package com.brewer.controller.converter;

import com.brewer.model.Grupo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GrupoConverterTest {

    private GrupoConverter converter;

    @Before
    public void metodoInicializaCenarioDeTeste() {
        this.converter = new GrupoConverter();
    }

    @Test
    public void testeMetodoConvertDeveRetornarGrupoComIdConvertido() {
        Grupo result = converter.convert("1");
        assertEquals(new Long(1), result.getCodigo());
    }

    @Test
    public void testeMetodoConvertQuandoCodigoVazioDeveRetornarNull() {
        Grupo result = converter.convert("");
        assertNull(result);
    }
}