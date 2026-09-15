package com.brewer.repository.filter;

import com.brewer.Constantes;
import com.brewer.model.StatusVenda;
import com.brewer.model.TipoPessoa;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class VendaFilter {

	private Long codigo;
	private StatusVenda status;

	private LocalDate desde;
	private LocalDate ate;
	private BigDecimal valorMinimo;
	private BigDecimal valorMaximo;

	private String nomeCliente;
	private String cpfOuCnpjCliente;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public StatusVenda getStatus() {
		return status;
	}

	public void setStatus(StatusVenda status) {
		this.status = status;
	}

	public LocalDate getDesde() {
		return desde;
	}

	public void setDesde(LocalDate desde) {
		this.desde = desde;
	}

	public LocalDate getAte() {
		return ate;
	}

	public void setAte(LocalDate ate) {
		this.ate = ate;
	}

	public BigDecimal getValorMinimo() {
		return valorMinimo;
	}

	public void setValorMinimo(BigDecimal valorMinimo) {
		this.valorMinimo = valorMinimo;
	}

	public BigDecimal getValorMaximo() {
		return valorMaximo;
	}

	public void setValorMaximo(BigDecimal valorMaximo) {
		this.valorMaximo = valorMaximo;
	}

	public String getNomeCliente() {
		return nomeCliente;
	}

	public void setNomeCliente(String nomeCliente) {
		this.nomeCliente = nomeCliente;
	}

	public String getCpfOuCnpjCliente() {
		return cpfOuCnpjCliente;
	}

	public void setCpfOuCnpjCliente(String cpfOuCnpjCliente) {
		this.cpfOuCnpjCliente = cpfOuCnpjCliente;
	}

    public void adicionarFiltros(String aliasVenda, String aliasCliente, StringBuilder jpql, Map<String, Object> params) {
		if (codigo != null) {
			jpql.append(" and ").append(aliasVenda).append(".codigo = :codigo");
			params.put("codigo", codigo);
		}

		if (status != null) {
			jpql.append(" and ").append(aliasVenda).append(".").append(Constantes.STATUS).append(" = :status");
			params.put("status", status);
		}

		if (desde != null) {
			LocalDateTime desdeDate = LocalDateTime.of(this.desde, LocalTime.of(0, 0));
			jpql.append(" and ").append(aliasVenda).append(".dataCriacao >= :desde");
			params.put("desde", desdeDate);
		}

		if (ate != null) {
			LocalDateTime ateDate = LocalDateTime.of(this.ate, LocalTime.of(23, 59));
			jpql.append(" and ").append(aliasVenda).append(".dataCriacao <= :ate");
			params.put("ate", ateDate);
		}

		if (valorMinimo != null) {
			jpql.append(" and ").append(aliasVenda).append(".valorTotal >= :valorMinimo");
			params.put("valorMinimo", valorMinimo);
		}

		if (valorMaximo != null) {
			jpql.append(" and ").append(aliasVenda).append(".valorTotal <= :valorMaximo");
			params.put("valorMaximo", valorMaximo);
		}

		if (!StringUtils.isEmpty(nomeCliente)) {
			jpql.append(" and lower(").append(aliasCliente).append(".nome) like :nomeCliente");
			params.put("nomeCliente", "%" + nomeCliente.toLowerCase() + "%");
		}

		if (!StringUtils.isEmpty(cpfOuCnpjCliente)) {
			jpql.append(" and ").append(aliasCliente).append(".cpfOuCnpj = :cpfOuCnpjCliente");
			params.put("cpfOuCnpjCliente", TipoPessoa.removerFormatacao(cpfOuCnpjCliente));
		}
    }
}