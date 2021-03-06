package com.brewer.service.event.venda;

import com.brewer.model.Cerveja;
import com.brewer.model.ItemVenda;
import com.brewer.repository.Cervejas;
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
	
	@EventListener	
	public void vendaEmitida(VendaEvent vendaEvent){
		for(ItemVenda item :vendaEvent.getVenda().getItens()){
			Cerveja cerveja = cervejasRepo.getOne(item.getCerveja().getCodigo());
			cerveja.setQuantidadeEstoque(cerveja.getQuantidadeEstoque() - item.getQuantidade());
			cervejasRepo.save(cerveja);
			
		}
	}
}
