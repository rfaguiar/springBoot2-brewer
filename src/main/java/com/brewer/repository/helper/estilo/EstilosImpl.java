package com.brewer.repository.helper.estilo;

import com.brewer.model.Estilo;
import com.brewer.repository.filter.EstiloFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.Map;

public class EstilosImpl implements EstilosQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;

	public EstilosImpl() {}

	public EstilosImpl(EntityManager manager, PaginacaoUtil paginacaoUtil) {
		this.manager = manager;
		this.paginacaoUtil = paginacaoUtil;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public Page<Estilo> filtrar(EstiloFilter filtro, Pageable pageable){
		StringBuilder jpql = new StringBuilder("select e from Estilo e where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarfiltro(filtro, jpql, params);
		jpql.append(paginacaoUtil.ordenar(pageable, "e"));

		TypedQuery<Estilo> query = manager.createQuery(jpql.toString(), Estilo.class);
		params.forEach(query::setParameter);
		paginacaoUtil.preparar(query, pageable);
		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}
	
	private Long total(EstiloFilter filtro) {
		StringBuilder jpql = new StringBuilder("select count(e) from Estilo e where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarfiltro(filtro, jpql, params);
		TypedQuery<Long> query = manager.createQuery(jpql.toString(), Long.class);
		params.forEach(query::setParameter);
		return query.getSingleResult();
	}
	
	private void adicionarfiltro(EstiloFilter filtro, StringBuilder jpql, Map<String, Object> params) {
		if(filtro != null && StringUtils.hasText(filtro.getNome())){
			jpql.append(" and lower(e.nome) like :nome");
			params.put("nome", "%" + filtro.getNome().toLowerCase() + "%");
		}
	}

}
