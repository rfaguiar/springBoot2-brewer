package com.brewer.storage.local;

import com.brewer.Constantes;
import com.brewer.storage.FotoStorage;
import com.brewer.storage.exception.FotoStorageException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.FileSystems.getDefault;

@Profile("!prod")
@Component
public class FotoStorageLocal implements FotoStorage {

    private static final Logger logger = LoggerFactory.getLogger(FotoStorageLocal.class);

    private Path local;

    public FotoStorageLocal(@Value("${brewer.foto-storage-local.local}") Path path) {
        this.local = path;
        criarPastas();
    }

    private void criarPastas() {
        try {
            Files.createDirectories(this.local);
            logger.debug("Pastas criadas para salvar fotos.");
        } catch (IOException e) {
            throw new FotoStorageException("Erro criando pasta para salvar foto", e);
        }
    }

    @Override
    public String salvar(MultipartFile[] files) {
        String novoNome = null;
        if (files != null && files.length > 0) {
            MultipartFile arquivo = files[0];
            novoNome = renomearArquivo(arquivo.getOriginalFilename());
            try {
                arquivo.transferTo(new File(this.local.toAbsolutePath().toString() + getDefault().getSeparator() + novoNome));
            } catch (IOException e) {
                throw new FotoStorageException("Erro salvando a foto", e);
            }
        }

        try {
            Thumbnails.of(this.local.resolve(novoNome).toString()).size(40, 68).toFiles(Rename.PREFIX_DOT_THUMBNAIL);
        } catch (IOException e) {
            throw new FotoStorageException("Erro gerando thumbnail", e);
        }

        return novoNome;
    }

    @Override
    public byte[] recuperar(String nome) {
        try {
            return Files.readAllBytes(this.local.resolve(nome));
        } catch (IOException e) {
            throw new FotoStorageException("Erro lendo a foto", e);
        }
    }

    @Override
    public byte[] recuperarThumbnail(String fotoCerveja) {
        return recuperar(Constantes.THUMBNAIL_PREFIX + fotoCerveja);
    }

    @Override
    public boolean excluir(String foto) {
        try {
            Files.deleteIfExists(this.local.resolve(foto));
            Files.deleteIfExists(this.local.resolve(Constantes.THUMBNAIL_PREFIX + foto));
            return true;
        } catch (IOException e) {
            throw new FotoStorageException(String.format("Erro apagando foto '%s'.", foto), e);
        }
    }

    @Override
    public String getUrl(String foto) {
        return "http://localhost:8080/fotos/" + foto;
    }

}
