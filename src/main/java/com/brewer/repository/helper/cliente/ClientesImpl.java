package com.brewer.repository.helper.cliente;

import com.brewer.model.Cliente;
import com.brewer.repository.filter.ClienteFilter;
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

public class ClientesImpl implements ClientesQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;

	public ClientesImpl() {}

	public ClientesImpl(EntityManager manager, PaginacaoUtil paginacaoUtil) {
		this.manager = manager;
		this.paginacaoUtil = paginacaoUtil;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public Page<Cliente> filtrar(ClienteFilter filtro, Pageable pageable){
		StringBuilder jpql = new StringBuilder("select c from Cliente c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarfiltro(filtro, jpql, params);
		jpql.append(paginacaoUtil.ordenar(pageable, "c"));

		TypedQuery<Cliente> query = manager.createQuery(jpql.toString(), Cliente.class);
		params.forEach(query::setParameter);
		paginacaoUtil.preparar(query, pageable);

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}
	
	private Long total(ClienteFilter filtro) {
		StringBuilder jpql = new StringBuilder("select count(c) from Cliente c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarfiltro(filtro, jpql, params);
		TypedQuery<Long> query = manager.createQuery(jpql.toString(), Long.class);
		params.forEach(query::setParameter);
		return query.getSingleResult();
	}
	
	private void adicionarfiltro(ClienteFilter filtro, StringBuilder jpql, Map<String, Object> params) {
		if(filtro != null){
			if(StringUtils.hasText(filtro.getNome())){
				jpql.append(" and lower(c.nome) like :nome");
				params.put("nome", "%" + filtro.getNome().toLowerCase() + "%");
			}
			if(StringUtils.hasText(filtro.getCpfOuCnpj())){
				jpql.append(" and c.cpfOuCnpj = :cpfOuCnpj");
				params.put("cpfOuCnpj", filtro.getCpfOuCnpj());
			}
		}
	}

}
