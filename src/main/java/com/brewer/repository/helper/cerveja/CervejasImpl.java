package com.brewer.repository.helper.cerveja;

import com.brewer.Constantes;
import com.brewer.dto.CervejaDTO;
import com.brewer.dto.ValorItensEstoque;
import com.brewer.model.Cerveja;
import com.brewer.repository.filter.CervejaFilter;
import com.brewer.repository.paginacao.PaginacaoUtil;
import com.brewer.storage.FotoStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CervejasImpl implements CervejasQueries{

	@PersistenceContext
	private EntityManager manager;
	@Autowired
	private PaginacaoUtil paginacaoUtil;
    @Autowired
    private FotoStorage fotoStorage;

	public CervejasImpl() {}

	public CervejasImpl(EntityManager manager, PaginacaoUtil paginacaoUtil, FotoStorage fotoStorage) {
		this.manager = manager;
		this.paginacaoUtil = paginacaoUtil;
		this.fotoStorage = fotoStorage;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public Page<Cerveja> filtrar(CervejaFilter filtro, Pageable pageable){
		StringBuilder jpql = new StringBuilder("select c from Cerveja c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		jpql.append(paginacaoUtil.ordenar(pageable, "c"));

		TypedQuery<Cerveja> query = manager.createQuery(jpql.toString(), Cerveja.class);
		params.forEach(query::setParameter);
		paginacaoUtil.preparar(query, pageable);
		List<Cerveja> list = query.getResultList();
		return new PageImpl<>(list, pageable, total(filtro));
	}

	private Long total(CervejaFilter filtro) {
		StringBuilder jpql = new StringBuilder("select count(c) from Cerveja c where 1=1");
		Map<String, Object> params = new HashMap<>();
		adicionarFiltro(filtro, jpql, params);
		TypedQuery<Long> query = manager.createQuery(jpql.toString(), Long.class);
		params.forEach(query::setParameter);
		return query.getSingleResult();
	}

	private void adicionarFiltro(CervejaFilter filtro, StringBuilder jpql, Map<String, Object> params) {
		if(filtro != null){
			if(StringUtils.hasText(filtro.getSku())){
				jpql.append(" and c.sku = :sku");
				params.put("sku", filtro.getSku());
			}
			
			if(StringUtils.hasText(filtro.getNome())){
				jpql.append(" and lower(c.nome) like :nome");
				params.put("nome", "%" + filtro.getNome().toLowerCase() + "%");
			}
			
			if(isEstiloPresente(filtro)){
				jpql.append(" and c.estilo = :estilo");
				params.put("estilo", filtro.getEstilo());
			}
			
			if(filtro.getSabor() != null){
				jpql.append(" and c.sabor = :sabor");
				params.put("sabor", filtro.getSabor());
			}
			
			if(filtro.getOrigem() != null){
				jpql.append(" and c.origem = :origem");
				params.put("origem", filtro.getOrigem());
			}
			
			if(filtro.getValorDe() != null){
				jpql.append(" and c.valor >= :valorDe");
				params.put("valorDe", filtro.getValorDe());
			}
			
			if(filtro.getValorAte() != null){
				jpql.append(" and c.valor <= :valorAte");
				params.put("valorAte", filtro.getValorAte());
			}
		}
	}

	private boolean isEstiloPresente(CervejaFilter filtro) {
		return filtro.getEstilo() != null && filtro.getEstilo().getCodigo() != null;
	}

	@Override
	public List<CervejaDTO> porSkuOuNome(String skuOuNome) {
		String jpql = "select new com.brewer.dto.CervejaDTO(c.codigo, c.sku, c.nome, c.origem, c.valor, c.foto) "
				+ "from Cerveja c where lower(c.sku) like :skuOuNome or lower(c.nome) like :skuOuNome";
        List<CervejaDTO> cervejasFiltradas = manager.createQuery(jpql, CervejaDTO.class)
				.setParameter("skuOuNome", skuOuNome.toLowerCase() + "%")
				.getResultList();
        cervejasFiltradas.forEach(c -> c.setUrlThumbnailFoto(fotoStorage.getUrl(Constantes.THUMBNAIL_PREFIX + c.getFoto())));
        return cervejasFiltradas;

    }
	
	@Override
	public ValorItensEstoque valorItensEstoque() {
		String query = "select new com.brewer.dto.ValorItensEstoque(coalesce(sum(c.valor * c.quantidadeEstoque), 0), coalesce(sum(c.quantidadeEstoque), 0)) from Cerveja c";
		return manager.createQuery(query, ValorItensEstoque.class).getSingleResult();
	}
}
