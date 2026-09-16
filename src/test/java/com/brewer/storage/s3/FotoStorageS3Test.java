package com.brewer.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.brewer.Constantes;
import com.brewer.storage.exception.FotoStorageException;
import net.coobird.thumbnailator.Thumbnails;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FotoStorageS3Test {

    private static final byte[] FOTO_BYTES = new byte[]{1, 2, 3, 4, 5};
    private static final byte[] THUMBNAIL_BYTES = new byte[]{9, 8, 7};

    private FotoStorageS3 storage;

    @Mock
    private AmazonS3 amazonS3;
    @Mock
    @SuppressWarnings("rawtypes")
    private Thumbnails.Builder thumbnailBuilder;
    @Mock
    private S3Object s3Object;
    @Mock
    private S3ObjectInputStream s3ObjectInputStream;

    private MockedStatic<Thumbnails> thumbnailsMock;
    private MockedStatic<IOUtils> ioUtilsMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        thumbnailsMock = Mockito.mockStatic(Thumbnails.class);
        ioUtilsMock = Mockito.mockStatic(IOUtils.class);
        storage = new FotoStorageS3("bucketTeste", amazonS3);
    }

    @After
    public void tearDown() {
        if (thumbnailsMock != null) {
            thumbnailsMock.close();
        }
        if (ioUtilsMock != null) {
            ioUtilsMock.close();
        }
    }

    @Test
    public void testeMetodoSalvarDeveSalvarFotoEThumbnailComMetadataEPermissaoCorretas() throws IOException {
        MultipartFile arquivo = new MockMultipartFile("file", "cerveja.png", "image/png", FOTO_BYTES);
        thumbnailsMock.when(() -> Thumbnails.of(ArgumentMatchers.any(InputStream.class))).thenReturn(thumbnailBuilder);
        Mockito.when(thumbnailBuilder.size(40, 68)).thenReturn(thumbnailBuilder);
        Mockito.doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(THUMBNAIL_BYTES);
            return null;
        }).when(thumbnailBuilder).toOutputStream(ArgumentMatchers.any(OutputStream.class));

        try (MockedConstruction<ObjectMetadata> metadataConstruction = Mockito.mockConstruction(ObjectMetadata.class);
             MockedConstruction<AccessControlList> aclConstruction = Mockito.mockConstruction(AccessControlList.class)) {
            String nomeSalvo = storage.salvar(new MultipartFile[]{arquivo});

            assertNotNull(nomeSalvo);
            assertTrue(nomeSalvo.matches("^[0-9a-f\\-]{36}_cerveja\\.png$"));

            List<ObjectMetadata> metadados = metadataConstruction.constructed();
            assertEquals(2, metadados.size());
            Mockito.verify(metadados.get(0)).setContentType("image/png");
            Mockito.verify(metadados.get(0)).setContentLength((long) FOTO_BYTES.length);
            Mockito.verify(metadados.get(1)).setContentType("image/png");
            Mockito.verify(metadados.get(1)).setContentLength((long) THUMBNAIL_BYTES.length);

            List<AccessControlList> acls = aclConstruction.constructed();
            assertEquals(1, acls.size());
            Mockito.verify(acls.get(0)).grantPermission(GroupGrantee.AllUsers, Permission.Read);

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            Mockito.verify(amazonS3, Mockito.times(2)).putObject(requestCaptor.capture());

            List<PutObjectRequest> requests = requestCaptor.getAllValues();
            assertEquals(nomeSalvo, requests.get(0).getKey());
            assertEquals(Constantes.THUMBNAIL_PREFIX + nomeSalvo, requests.get(1).getKey());
            assertSame(acls.get(0), requests.get(0).getAccessControlList());
            assertSame(acls.get(0), requests.get(1).getAccessControlList());
            Mockito.verify(thumbnailBuilder).size(40, 68);
        }
    }

    @Test(expected = FotoStorageException.class)
    public void testeMetodoSalvarQuandoOcorreExcecaoDeveLancarMsgAdequada() throws IOException {
        MultipartFile arquivo = new MockMultipartFile("file", "cerveja.png", "image/png", FOTO_BYTES);
        thumbnailsMock.when(() -> Thumbnails.of(ArgumentMatchers.any(InputStream.class))).thenReturn(thumbnailBuilder);
        Mockito.when(thumbnailBuilder.size(40, 68)).thenReturn(thumbnailBuilder);
        Mockito.doThrow(new IOException("falha-thumbnail")).when(thumbnailBuilder)
                .toOutputStream(ArgumentMatchers.any(OutputStream.class));

        try {
            storage.salvar(new MultipartFile[]{arquivo});
        } catch (FotoStorageException e) {
            assertEquals("Erro salvando arquivo no S3", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testeMetodoRecuperarDeveRetornarBytesDaFotoArmazenadaNaAmazonS3() throws IOException {
        byte[] bytesEsperados = new byte[]{'t', 'e', 's', 't', 'e'};
        Mockito.when(amazonS3.getObject("bucketTeste", "teste.jpg")).thenReturn(s3Object);
        Mockito.when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        ioUtilsMock.when(() -> IOUtils.toByteArray(s3ObjectInputStream)).thenReturn(bytesEsperados);

        byte[] resultado = storage.recuperar("teste.jpg");

        assertArrayEquals(bytesEsperados, resultado);
    }

    @Test(expected = FotoStorageException.class)
    public void testeMetodoRecuperarQuandoOcorrerErroDeveLancarExcecaoComMensagemAdequada() throws IOException {
        Mockito.when(amazonS3.getObject("bucketTeste", "teste")).thenReturn(s3Object);
        Mockito.when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        ioUtilsMock.when(() -> IOUtils.toByteArray(s3ObjectInputStream)).thenThrow(new IOException("falha"));

        try {
            storage.recuperar("teste");
        } catch (FotoStorageException e) {
            assertEquals("Não conseguiu recuperar foto do S3", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testeMetodoRecuperarThumbnailDeveBuscarArquivoComPrefixoDeThumbnail() throws IOException {
        byte[] bytesEsperados = new byte[]{5, 4, 3};
        Mockito.when(amazonS3.getObject("bucketTeste", Constantes.THUMBNAIL_PREFIX + "foto.png")).thenReturn(s3Object);
        Mockito.when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        ioUtilsMock.when(() -> IOUtils.toByteArray(s3ObjectInputStream)).thenReturn(bytesEsperados);

        byte[] resultado = storage.recuperarThumbnail("foto.png");

        assertArrayEquals(bytesEsperados, resultado);
        Mockito.verify(amazonS3).getObject("bucketTeste", Constantes.THUMBNAIL_PREFIX + "foto.png");
    }

    @Test
    public void testeMetodoExcluirQuandoFotoInformadaDeveRetornarTrueEEnviarOsDoisArquivosParaExclusao() {
        String foto = "cerveja.png";
        Mockito.when(amazonS3.deleteObjects(ArgumentMatchers.any(DeleteObjectsRequest.class))).thenReturn(null);

        boolean excluiu = storage.excluir(foto);

        assertTrue(excluiu);
        ArgumentCaptor<DeleteObjectsRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        Mockito.verify(amazonS3).deleteObjects(requestCaptor.capture());
        DeleteObjectsRequest request = requestCaptor.getValue();
        assertEquals("bucketTeste", request.getBucketName());
        assertEquals(2, request.getKeys().size());
        assertEquals(foto, request.getKeys().get(0).getKey());
        assertEquals(Constantes.THUMBNAIL_PREFIX + foto, request.getKeys().get(1).getKey());
    }

    @Test
    public void testeMetodoExcluirQuandoFotoNaoInformadaDeveRetornarFalseESemChamarAmazonS3() {
        boolean excluiu = storage.excluir("");

        assertFalse(excluiu);
        Mockito.verifyNoInteractions(amazonS3);
    }

    @Test
    public void testeMetodoGetUrlDeveRetornarAmazonUrlDaFoto() {
        String resultado = storage.getUrl("fotoTeste");

        assertEquals("https://s3.amazonaws.com/bucketTeste/fotoTeste", resultado);
    }

    @Test
    public void testeMetodoGetUrlVaziaDeveRetornarNull() {
        String resultado = storage.getUrl("");

        assertNull(resultado);
    }
}
