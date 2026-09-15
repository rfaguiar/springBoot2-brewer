package com.brewer.repository.listener;

import com.brewer.BrewerApplication;
import com.brewer.Constantes;
import com.brewer.builder.CervejaBuilder;
import com.brewer.model.Cerveja;
import com.brewer.storage.FotoStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

public class CervejaEntityListenerTest {

    private CervejaEntityListener listener;
    @Mock
    private FotoStorage mockFotoStorage;
    private MockedStatic<BrewerApplication> mockBrewerApplication;

    @Before
    public void iniciarCenarioDeTeste() {
        MockitoAnnotations.initMocks(this);
        mockBrewerApplication = Mockito.mockStatic(BrewerApplication.class);
        this.listener = new CervejaEntityListener();
        mockBrewerApplication.when(() -> BrewerApplication.getBean(FotoStorage.class)).thenReturn(mockFotoStorage);
    }

    @org.junit.After
    public void finalizarCenarioDeTeste() {
        if (mockBrewerApplication != null) {
            mockBrewerApplication.close();
        }
    }

    @Test
    public void testeMetodoPostLoadDeveBuscarUrlFoto() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        Mockito.when(mockFotoStorage.getUrl(cerveja.getFotoOuMock())).thenReturn("foto");
        Mockito.when(mockFotoStorage.getUrl(Constantes.THUMBNAIL_PREFIX + cerveja.getFotoOuMock())).thenReturn("thumbnailFoto");
        listener.postLoad(cerveja);
        assertEquals("foto", cerveja.getUrlFoto());
        assertEquals("thumbnailFoto", cerveja.getUrlThumbnailFoto());
    }
}