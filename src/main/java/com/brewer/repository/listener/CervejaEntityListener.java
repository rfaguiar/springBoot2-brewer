package com.brewer.repository.listener;

import com.brewer.BrewerApplication;
import com.brewer.Constantes;
import com.brewer.model.Cerveja;
import com.brewer.storage.FotoStorage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.PostLoad;

public class CervejaEntityListener {

    @PostLoad
    public void postLoad(final Cerveja cerveja) {
        FotoStorage fotoStorage = BrewerApplication.getBean(FotoStorage.class);
        cerveja.setUrlFoto(fotoStorage.getUrl(cerveja.getFotoOuMock()));
        cerveja.setUrlThumbnailFoto(fotoStorage.getUrl(Constantes.THUMBNAIL_PREFIX + cerveja.getFotoOuMock()));
    }
}
