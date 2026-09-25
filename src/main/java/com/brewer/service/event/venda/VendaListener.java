package com.brewer.service.event.venda;

import com.brewer.model.Cerveja;
import com.brewer.model.ItemVenda;
import com.brewer.repository.Cervejas;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class VendaListener {

	private Cervejas cervejasRepo;

	@Autowired
	public VendaListener(Cervejas cervejasRepo) {
		this.cervejasRepo = cervejasRepo;
	}
	
	@WithSpan("venda.baixar-estoque")
	@EventListener	
	public void vendaEmitida(VendaEvent vendaEvent){
		Span.current().setAttribute("venda.codigo", String.valueOf(vendaEvent.getVenda().getCodigo()));

		for(ItemVenda item :vendaEvent.getVenda().getItens()){
			Cerveja cerveja = cervejasRepo.getOne(item.getCerveja().getCodigo());
			// getQuantidadeEstoque() pode ser null (coluna sem NOT NULL/default na migration V02).
			int estoqueAtual = cerveja.getQuantidadeEstoque() != null ? cerveja.getQuantidadeEstoque() : 0;
			cerveja.setQuantidadeEstoque(estoqueAtual - item.getQuantidade());
			cervejasRepo.save(cerveja);
			
		}
	}
}
