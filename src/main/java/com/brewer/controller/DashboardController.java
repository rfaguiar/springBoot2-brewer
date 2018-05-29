package com.brewer.controller;

import com.brewer.repository.Cervejas;
import com.brewer.repository.Clientes;
import com.brewer.repository.Vendas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DashboardController {

	private Vendas vendasRepo;
	private Cervejas cervejasRepo;
	private Clientes clientesRepo;

	@Autowired
	public DashboardController(Vendas vendasRepo, Cervejas cervejasRepo, Clientes clientesRepo) {
		this.vendasRepo = vendasRepo;
		this.cervejasRepo = cervejasRepo;
		this.clientesRepo = clientesRepo;
	}
	
	@GetMapping("/")
	public ModelAndView dashboard() {
		ModelAndView mv = new ModelAndView("Dashboard");
		
		mv.addObject("vendasNoAno", vendasRepo.valorTotalNoAno());
		mv.addObject("vendasNoMes", vendasRepo.valorTotalNoMes());
		mv.addObject("ticketMedio", vendasRepo.valorTicketMedioNoAno());
		
		mv.addObject("valorItensEstoque", cervejasRepo.valorItensEstoque());
		mv.addObject("totalClientes", clientesRepo.count());
		
		return mv;
}
}
