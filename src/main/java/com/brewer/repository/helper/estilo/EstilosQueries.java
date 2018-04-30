package com.brewer.repository.helper.estilo;

import com.brewer.repository.Estilos;
import com.brewer.repository.filter.EstiloFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EstilosQueries {

	public Page<Estilos> filtrar(EstiloFilter filtro, Pageable pageable);
}
