package com.brewer.session;

import com.brewer.model.Cerveja;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TabelaItensVendaTest {

	private TabelaItensVenda tabelaItensVenda;

	@Before
	public void setUp() {
		this.tabelaItensVenda = new TabelaItensVenda("1");
	}

	@Test
	public void deveCalcularValorTotalSemItens() {
		assertEquals(BigDecimal.ZERO, tabelaItensVenda.getValorTotal());
	}

	@Test
	public void deveCalcularValorTotalComUmItem() {
		Cerveja cerveja = cerveja(1L, "8.90");

		tabelaItensVenda.adicionarIten(cerveja, 1);

		assertEquals(new BigDecimal("8.90"), tabelaItensVenda.getValorTotal());
	}

	@Test
	public void deveCalcularValorTotalComVariosItens() {
		Cerveja c1 = cerveja(1L, "8.90");
		Cerveja c2 = cerveja(2L, "4.99");

		tabelaItensVenda.adicionarIten(c1, 1);
		tabelaItensVenda.adicionarIten(c2, 2);

		assertEquals(new BigDecimal("18.88"), tabelaItensVenda.getValorTotal());
	}

	@Test
	public void deveManterTamanhoDaListaParaMesmasCervejas() {
		Cerveja c1 = cerveja(1L, "4.50");

		tabelaItensVenda.adicionarIten(c1, 1);
		tabelaItensVenda.adicionarIten(c1, 1);

		assertEquals(1, tabelaItensVenda.total());
		assertEquals(new BigDecimal("9.00"), tabelaItensVenda.getValorTotal());
	}

	@Test
	public void deveAlterarQuantidadeDoItem() {
		Cerveja c1 = cerveja(1L, "4.50");

		tabelaItensVenda.adicionarIten(c1, 1);
		tabelaItensVenda.alterarQuantidadeItens(c1, 3);

		assertEquals(new BigDecimal("13.50"), tabelaItensVenda.getValorTotal());
	}

	@Test
	public void deveExluirItem() {
		Cerveja c1 = cerveja(1L, "8.90");
		Cerveja c2 = cerveja(2L, "4.99");
		Cerveja c3 = cerveja(3L, "2.00");

		tabelaItensVenda.adicionarIten(c1, 1);
		tabelaItensVenda.adicionarIten(c2, 2);
		tabelaItensVenda.adicionarIten(c3, 1);

		tabelaItensVenda.excluir(c2);

		assertEquals(2, tabelaItensVenda.total());
		assertEquals(new BigDecimal("10.90"), tabelaItensVenda.getValorTotal());
	}

	@Test
	public void deveSerIgualQuandoCompararMesmaInstancia() {
		assertTrue(tabelaItensVenda.equals(tabelaItensVenda));
	}

	@Test
	public void deveRetornarFalseQuandoCompararComNull() {
		assertFalse(tabelaItensVenda.equals(null));
	}

	@Test
	public void deveRetornarFalseQuandoCompararComOutraClasse() {
		assertFalse(tabelaItensVenda.equals("1"));
	}

	@Test
	public void deveSerIgualEManterMesmoHashCodeQuandoUuidForIgual() {
		TabelaItensVenda outraTabela = new TabelaItensVenda("1");

		assertTrue(tabelaItensVenda.equals(outraTabela));
		assertEquals(tabelaItensVenda.hashCode(), outraTabela.hashCode());
	}

	@Test
	public void deveSerDiferenteQuandoUuidForDiferente() {
		TabelaItensVenda outraTabela = new TabelaItensVenda("2");

		assertFalse(tabelaItensVenda.equals(outraTabela));
	}

	@Test
	public void deveSerIgualQuandoAmbosUuidForemNulos() {
		TabelaItensVenda tabela1 = new TabelaItensVenda(null);
		TabelaItensVenda tabela2 = new TabelaItensVenda(null);

		assertTrue(tabela1.equals(tabela2));
		assertEquals(tabela1.hashCode(), tabela2.hashCode());
	}

	@Test
	public void deveSerDiferenteQuandoApenasUmUuidForNulo() {
		TabelaItensVenda tabelaComUuidNulo = new TabelaItensVenda(null);

		assertFalse(tabelaComUuidNulo.equals(tabelaItensVenda));
		assertFalse(tabelaItensVenda.equals(tabelaComUuidNulo));
	}

	private Cerveja cerveja(Long codigo, String valor) {
		Cerveja cerveja = new Cerveja();
		cerveja.setCodigo(codigo);
		cerveja.setValor(new BigDecimal(valor));
		return cerveja;
	}
}
