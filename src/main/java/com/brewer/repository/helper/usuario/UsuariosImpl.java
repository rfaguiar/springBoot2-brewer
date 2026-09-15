package com.brewer.repository.helper.usuario;

import com.brewer.model.Grupo;
import com.brewer.model.Usuario;
import com.brewer.model.UsuarioGrupo;
import com.brewer.repository.filter.UsuarioFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UsuariosImpl implements UsuariosQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;

	public UsuariosImpl() {}

	public UsuariosImpl(EntityManager manager, PaginacaoUtil paginacaoUtil) {
		this.manager = manager;
		this.paginacaoUtil = paginacaoUtil;
	}

	@Override
	public Optional<Usuario> porEmailEAtivo(String email) {
		return manager
				.createQuery("from Usuario where lower(email) = lower(:email) and ativo = true", Usuario.class)
				.setParameter("email", email).getResultList().stream().findFirst();
	}

	@Override
	public List<String> permissoes(Usuario usuario) {
		return manager.createQuery(
				"select distinct p.nome from Usuario u inner join u.grupos g inner join g.permissoes p where u = :usuario", String.class)
				.setParameter("usuario", usuario)
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	@Override
	public Page<Usuario> filtrar(UsuarioFilter filtro, Pageable pageable) {
		StringBuilder jpql = new StringBuilder("select distinct u from Usuario u where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		jpql.append(paginacaoUtil.ordenar(pageable, "u"));

		TypedQuery<Usuario> query = manager.createQuery(jpql.toString(), Usuario.class);
		params.forEach(query::setParameter);
		paginacaoUtil.preparar(query, pageable);

		List<Usuario> filtrados = query.getResultList();
		filtrados.forEach(u -> Hibernate.initialize(u.getGrupos()));
		
		return new PageImpl<>(filtrados, pageable, total(filtro));
	}

	@Transactional(readOnly = true)
	@Override
	public Usuario buscarComGrupos(Long codigo) {
		return manager.createQuery("select distinct u from Usuario u left join fetch u.grupos where u.codigo = :codigo", Usuario.class)
				.setParameter("codigo", codigo)
				.getSingleResult();
	}
	
	private Long total(UsuarioFilter filtro) {
		StringBuilder jpql = new StringBuilder("select count(distinct u) from Usuario u where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		TypedQuery<Long> query = manager.createQuery(jpql.toString(), Long.class);
		params.forEach(query::setParameter);
		return query.getSingleResult();
	}

	private void adicionarFiltro(UsuarioFilter filtro, StringBuilder jpql, Map<String, Object> params) {
		if (filtro != null) {
			if (StringUtils.hasText(filtro.getNome())) {
				jpql.append(" and lower(u.nome) like :nome");
				params.put("nome", "%" + filtro.getNome().toLowerCase() + "%");
			}
			
			if (StringUtils.hasText(filtro.getEmail())) {
				jpql.append(" and lower(u.email) like :email");
				params.put("email", filtro.getEmail().toLowerCase() + "%");
			}

			if (filtro.getGrupos() != null && !filtro.getGrupos().isEmpty()) {
				int index = 0;
				for (Grupo grupo : filtro.getGrupos()) {
					String param = "grupoCodigo" + index++;
					jpql.append(" and exists (")
						.append("select ug from UsuarioGrupo ug where ug.id.usuario = u and ug.id.grupo.codigo = :")
						.append(param)
						.append(")");
					params.put(param, grupo.getCodigo());
				}
			}
		}
	}

}
