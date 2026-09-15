package com.brewer.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.brewer.storage.exception.FotoStorageException;
import net.coobird.thumbnailator.Thumbnails;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

public class FotoStorageS3Test {

    private FotoStorageS3 storage;

    @Mock
    private AmazonS3 mockAmazonS3;
    @Mock
    private Thumbnails.Builder mockThumbBuild;
    @Mock
    private S3Object mock3Object;
    @Mock
    private S3ObjectInputStream s3ObjStream;
    @Mock
    private DeleteObjectsResult mockDeleteObj;
    private MockedStatic<Thumbnails> mockThumbnails;
    private MockedStatic<IOUtils> mockIoUtils;

    @Before
    public void metodoIniciandoCenariosDeTeste() {
        MockitoAnnotations.initMocks(this);
        mockThumbnails = Mockito.mockStatic(Thumbnails.class);
        mockIoUtils = Mockito.mockStatic(IOUtils.class);
        this.storage = new FotoStorageS3("bucketTeste", mockAmazonS3);

    }

    @org.junit.After
    public void finalizarCenariosDeTeste() {
        if (mockThumbnails != null) {
            mockThumbnails.close();
        }
        if (mockIoUtils != null) {
            mockIoUtils.close();
        }
    }

    @Test
    public void testeMetodoSalvarDeveSalvarCorretamenteAFotoNaAmazonS3() throws IOException {
        MultipartFile[] multPart = {new MockMultipartFile("teste", new byte[]{'t', 'e', 's', 't', 'e'})};
        mockThumbnails.when(() -> Thumbnails.of(ArgumentMatchers.any(InputStream.class))).thenReturn(mockThumbBuild);
        Mockito.when(mockThumbBuild.size(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(mockThumbBuild);
        Mockito.doNothing().when(mockThumbBuild).toOutputStream(ArgumentMatchers.any(OutputStream.class));
        String result = storage.salvar(multPart);
        assertNotNull(result);
    }

    @Test(expected = FotoStorageException.class)
    public void testeMetodoSalvarQuandoOcorreExcecaoDeveLancarMsgAdequada() throws IOException {
        try {
            MultipartFile[] multPart = {new MockMultipartFile("teste", new byte[]{'t', 'e', 's', 't', 'e'})};
            mockThumbnails.when(() -> Thumbnails.of(ArgumentMatchers.any(InputStream.class))).thenReturn(mockThumbBuild);
            Mockito.when(mockThumbBuild.size(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(mockThumbBuild);
            Mockito.doThrow(new IOException()).when(mockThumbBuild).toOutputStream(ArgumentMatchers.any(OutputStream.class));
            storage.salvar(multPart);
        } catch (IOException e) {
            assertEquals("Erro salvando arquivo no S3", e.getMessage());
            throw e;
        }
        fail();
    }

    @Test
    public void testeMetodoRecuperarAFotoArmazenadaNaAmazonS3() throws IOException {

        Mockito.when(mockAmazonS3.getObject(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(mock3Object);
        Mockito.when(mock3Object.getObjectContent()).thenReturn(s3ObjStream);
        mockIoUtils.when(() -> IOUtils.toByteArray(s3ObjStream)).thenReturn(new byte[]{'t', 'e', 's', 't', 'e'});
        byte[] result = storage.recuperar("teste");
        assertNotNull(result);

    }

    @Test(expected = FotoStorageException.class)
    public void testeMetodoRecuperarQuandoOcorrerErroDeveLancarExcecaoComMensagenAdequada() throws IOException {
        try {
            Mockito.when(mockAmazonS3.getObject(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(mock3Object);
            Mockito.when(mock3Object.getObjectContent()).thenReturn(s3ObjStream);
            mockIoUtils.when(() -> IOUtils.toByteArray(s3ObjStream)).thenThrow(new IOException());
            storage.recuperar("teste");
        }catch (FotoStorageException e) {
            assertEquals("Não conseguiu recuperar foto do S3", e.getMessage());
            throw e;
        }
        fail();

    }

    @Test
    public void testeMetodoRecuperarThumbnailDeveRetornarAFotoPequena() throws IOException {

        Mockito.when(mockAmazonS3.getObject(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(mock3Object);
        Mockito.when(mock3Object.getObjectContent()).thenReturn(s3ObjStream);
        mockIoUtils.when(() -> IOUtils.toByteArray(s3ObjStream)).thenReturn(new byte[]{'t', 'e', 's', 't', 'e'});
        byte[] result = storage.recuperarThumbnail("teste");
        assertNotNull(result);
    }

    @Test
    public void testeMetodoExcluirDeveDeletarArquivoNaAmazon() {
        Mockito.when(mockAmazonS3.deleteObjects(ArgumentMatchers.any(DeleteObjectsRequest.class))).thenReturn(mockDeleteObj);
        storage.excluir("teste");
        Mockito.verify(mockAmazonS3).deleteObjects(ArgumentMatchers.any(DeleteObjectsRequest.class));
    }

    @Test
    public void testeMetodoGetUrlDeveRetornarAmazonUrlDaFoto() {

        String result = storage.getUrl("fotoTeste");
        assertEquals("https://s3.amazonaws.com/bucketTeste/fotoTeste", result);
    }

    @Test
    public void testeMetodoGetUrlVaziaDeveRetornarNull() {

        String result = storage.getUrl("");
        assertNull(result);
    }
}