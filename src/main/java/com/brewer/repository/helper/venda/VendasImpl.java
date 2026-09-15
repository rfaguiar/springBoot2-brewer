package com.brewer.repository.helper.venda;

import com.brewer.Constantes;
import com.brewer.dto.VendaMes;
import com.brewer.dto.VendaOrigem;
import com.brewer.model.StatusVenda;
import com.brewer.model.Venda;
import com.brewer.repository.filter.VendaFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VendasImpl implements VendasQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;

	public VendasImpl() {}

	public VendasImpl(EntityManager entityManager, PaginacaoUtil paginacaoUtil) {
		this.manager = entityManager;
		this.paginacaoUtil = paginacaoUtil;
	}

    @SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	@Override
	public Page<Venda> filtrar(VendaFilter filtro, Pageable pageable) {
		StringBuilder jpql = new StringBuilder("select v from Venda v join v.cliente c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		jpql.append(paginacaoUtil.ordenar(pageable, "v"));

		TypedQuery<Venda> query = manager.createQuery(jpql.toString(), Venda.class);
		params.forEach(query::setParameter);
		paginacaoUtil.preparar(query, pageable);

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}
	

	@Transactional(readOnly = true)
	@Override
	public Venda buscarComItens(Long codigo) {
		return manager.createQuery("select distinct v from Venda v left join fetch v.itens where v.codigo = :codigo", Venda.class)
				.setParameter("codigo", codigo)
				.getSingleResult();
	}

	@Override
	public BigDecimal valorTotalNoAno() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select coalesce(sum(v.valorTotal), 0) from Venda v where year(v.dataCriacao) = :ano and v.status = :status", BigDecimal.class)
					.setParameter("ano", Year.now().getValue())
					.setParameter(Constantes.STATUS, StatusVenda.EMITIDA)
					.getSingleResult());
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@Override
	public BigDecimal valorTotalNoMes() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select coalesce(sum(v.valorTotal), 0) from Venda v where month(v.dataCriacao) = :mes and v.status = :status", BigDecimal.class)
					.setParameter("mes", MonthDay.now().getMonthValue())
					.setParameter(Constantes.STATUS, StatusVenda.EMITIDA)
					.getSingleResult());
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@Override
	public BigDecimal valorTicketMedioNoAno() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select coalesce(sum(v.valorTotal) / count(v), 0) from Venda v where year(v.dataCriacao) = :ano and v.status = :status", BigDecimal.class)
					.setParameter("ano", Year.now().getValue())
					.setParameter(Constantes.STATUS, StatusVenda.EMITIDA)
					.getSingleResult());
		return optional.orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<VendaMes> totalPorMes() {
		List<VendaMes> vendasMes = manager.createNamedQuery("Vendas.totalPorMes").getResultList();
		LocalDate hoje = LocalDate.now();
		
		for(int i = 1; i <= 6; i++){
			String mesIdeal = String.format("%d/%02d", hoje.getYear(), hoje.getMonthValue());
			Optional<VendaMes> findOpt = vendasMes.stream()
                                                .filter(v -> mesIdeal.equals(v.getMes()))
                                                .findAny();
			if(!findOpt.isPresent()){
				vendasMes.add(i -1, new VendaMes(mesIdeal, 0));
			}
			hoje = hoje.minusMonths(1);
		}
		
		return vendasMes;
	}
	
	@Override
	public List<VendaOrigem> totalPorOrigem() {
		List<VendaOrigem> vendasNacionalidade = manager.createNamedQuery("Vendas.porOrigem", VendaOrigem.class).getResultList();
		
		LocalDate now = LocalDate.now();
		for (int i = 1; i <= 6; i++) {
			String mesIdeal = String.format("%d/%02d", now.getYear(), now.getMonth().getValue());
            Optional<VendaOrigem> findOpt = vendasNacionalidade.stream()
                                            .filter(v -> v.getMes().equals(mesIdeal))
                                            .findAny();
			if (!findOpt.isPresent()) {
				vendasNacionalidade.add(i - 1, new VendaOrigem(mesIdeal, 0, 0));
			}
			
			now = now.minusMonths(1);
		}
		
		return vendasNacionalidade;
	}
	
	private Long total(VendaFilter filtro) {
		StringBuilder jpql = new StringBuilder("select count(v) from Venda v join v.cliente c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		TypedQuery<Long> query = manager.createQuery(jpql.toString(), Long.class);
		params.forEach(query::setParameter);
		return query.getSingleResult();
	}
	
	private void adicionarFiltro(VendaFilter filtro, StringBuilder jpql, Map<String, Object> params) {
		if (filtro != null) {
			filtro.adicionarFiltros("v", "c", jpql, params);
		}
	}



}
