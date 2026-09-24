package com.brewer.controller;

import com.brewer.dto.FotoDTO;
import com.brewer.storage.FotoStorage;
import com.brewer.storage.FotoStorageRunnable;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/fotos")
public class FotosController {

	private FotoStorage fotoStorage;

	@Autowired
	public FotosController(FotoStorage fotoStorage) {
		this.fotoStorage = fotoStorage;
	}

	@WithSpan("fotos.upload")
	@PostMapping
	public DeferredResult<FotoDTO> upload(@RequestParam("files[]") MultipartFile[] files) {
		Span.current().setAttribute("fotos.quantidade", files.length);

		DeferredResult<FotoDTO> resultado = new DeferredResult<>();

		Thread thread = new Thread(new FotoStorageRunnable(files, resultado, fotoStorage));
		thread.start();

		return resultado;
	}

    @WithSpan("fotos.recuperar")
    @GetMapping("/{nome:.*}")
	public byte[] recuperar(@PathVariable String nome) {
		Span.current().setAttribute("fotos.nome", nome);
		return fotoStorage.recuperar(nome);
	}
}
