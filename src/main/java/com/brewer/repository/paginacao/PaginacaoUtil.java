package com.brewer.repository.paginacao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import jakarta.persistence.TypedQuery;

@Component
public class PaginacaoUtil {

	public void preparar(TypedQuery<?> query, Pageable pageable) {
		int totalRegistrosPorPagina = pageable.getPageSize();
		int paginaAtual = pageable.getPageNumber();
		int primeiroRegistro = paginaAtual * totalRegistrosPorPagina;

		query.setFirstResult(primeiroRegistro);
		query.setMaxResults(totalRegistrosPorPagina);
	}

	public String ordenar(Pageable pageable, String aliasPadrao) {
		Sort sort = pageable != null ? pageable.getSort() : Sort.unsorted();
		if (sort != null && sort.isSorted()) {
			Sort.Order order = sort.iterator().next();
			String propriedade = order.getProperty();
			if (!propriedade.contains(".")) {
				propriedade = aliasPadrao + "." + propriedade;
			}
			return " order by " + propriedade + (order.isAscending() ? " asc" : " desc");
		}
		return "";
	}
}
