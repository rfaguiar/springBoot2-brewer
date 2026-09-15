package com.brewer.repository.helper.cidade;

import com.brewer.model.Cidade;
import com.brewer.repository.filter.CidadeFilter;
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

public class CidadesImpl implements CidadesQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;

	public CidadesImpl() {}

	public CidadesImpl(EntityManager manager, PaginacaoUtil paginacaoUtil) {
		this.manager = manager;
		this.paginacaoUtil = paginacaoUtil;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public Page<Cidade> filtrar(CidadeFilter filtro, Pageable pageable) {
		StringBuilder jpql = new StringBuilder("select c from Cidade c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		jpql.append(paginacaoUtil.ordenar(pageable, "c"));

		TypedQuery<Cidade> query = manager.createQuery(jpql.toString(), Cidade.class);
		params.forEach(query::setParameter);
		paginacaoUtil.preparar(query, pageable);

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}
	
	private Long total(CidadeFilter filtro) {
		StringBuilder jpql = new StringBuilder("select count(c) from Cidade c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		TypedQuery<Long> query = manager.createQuery(jpql.toString(), Long.class);
		params.forEach(query::setParameter);
		return query.getSingleResult();
	}

	private void adicionarFiltro(CidadeFilter filtro, StringBuilder jpql, Map<String, Object> params) {
		if (filtro != null) {
			if (filtro.getEstado() != null) {
				jpql.append(" and c.estado = :estado");
				params.put("estado", filtro.getEstado());
			}
			
			if (StringUtils.hasText(filtro.getNome())) {
				jpql.append(" and lower(c.nome) like :nome");
				params.put("nome", "%" + filtro.getNome().toLowerCase() + "%");
			}
		}
	}

}
