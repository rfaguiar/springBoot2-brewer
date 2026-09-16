package com.brewer.model;

import com.brewer.builder.UsuarioBuilder;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VendaTest {

    @Test
    public void testeMetodosTransientAoBd() {
        Venda venda = new Venda();
        LocalDateTime dataCriacao = LocalDateTime.now();
        LocalDateTime dataHoraEntrega = LocalDateTime.now();
        LocalDate dataEntrega = LocalDate.now();
        LocalTime horaEntrega = LocalTime.now();

        venda.setDataCriacao(dataCriacao);
        venda.setValorTotal(new BigDecimal("10"));
        venda.setObservacao("obsTeste");
        venda.setDataHoraEntrega(dataHoraEntrega);
        venda.setDataEntrega(dataEntrega);
        venda.setHorarioEntrega(horaEntrega);
        venda.setStatus(StatusVenda.EMITIDA);

        assertEquals(dataCriacao, venda.getDataCriacao());
        assertBigDecimalEquals("10", venda.getValorTotal());
        assertEquals("obsTeste", venda.getObservacao());
        assertEquals(dataHoraEntrega, venda.getDataHoraEntrega());
        assertEquals(dataEntrega, venda.getDataEntrega());
        assertEquals(horaEntrega, venda.getHorarioEntrega());
        assertTrue(venda.isNova());
        assertEquals(Long.valueOf(0), venda.getDiasCriacao());
        assertTrue(venda.isSalvarPermitido());
        assertFalse(venda.isSalvarProibido());
    }

    @Test
    public void calcularValorTotalDeveSomarSubtotalQuandoFreteEDescontoForemZero() {
        Venda venda = criarVendaComTotais("20.00", "0.00", "0.00");

        venda.calcularValorTotal();

        assertBigDecimalEquals("20.00", venda.getValorTotal());
    }

    @Test
    public void calcularValorTotalDeveSubtrairDescontoQuandoFreteForZero() {
        Venda venda = criarVendaComTotais("20.00", "0.00", "3.50");

        venda.calcularValorTotal();

        assertBigDecimalEquals("16.50", venda.getValorTotal());
    }

    @Test
    public void calcularValorTotalDeveSomarFreteQuandoDescontoForZero() {
        Venda venda = criarVendaComTotais("20.00", "4.25", "0.00");

        venda.calcularValorTotal();

        assertBigDecimalEquals("24.25", venda.getValorTotal());
    }

    @Test
    public void calcularValorTotalDeveSomarFreteESubtrairDesconto() {
        Venda venda = criarVendaComTotais("20.00", "4.25", "3.50");

        venda.calcularValorTotal();

        assertBigDecimalEquals("20.75", venda.getValorTotal());
    }

    @Test
    public void adicionarItensDeveAssociarCadaItemComAVenda() {
        Venda venda = new Venda();
        ItemVenda primeiroItem = Mockito.mock(ItemVenda.class);
        ItemVenda segundoItem = Mockito.mock(ItemVenda.class);
        List<ItemVenda> itens = Arrays.asList(primeiroItem, segundoItem);

        venda.adicionarItens(itens);

        assertSame(itens, venda.getItens());
        Mockito.verify(primeiroItem).setVenda(venda);
        Mockito.verify(segundoItem).setVenda(venda);
    }

    @Test
    public void getDiasCriacaoDeveRetornarQuantidadeExataDeDias() {
        Venda venda = new Venda();
        venda.setDataCriacao(LocalDateTime.now().minusDays(3));

        assertEquals(Long.valueOf(3), venda.getDiasCriacao());
    }

    @Test
    public void getUsuarioDeveRetornarUsuarioExatoInformado() {
        Venda venda = new Venda();
        Usuario usuario = UsuarioBuilder.criarUsuario();
        venda.setUsuario(usuario);

        assertSame(usuario, venda.getUsuario());
    }

    @Test
    public void getValorDescontoDeveRetornarValorExatoInformado() {
        Venda venda = new Venda();
        venda.setValorDesconto(new BigDecimal("7.35"));

        assertBigDecimalEquals("7.35", venda.getValorDesconto());
    }

    @Test
    public void getValorFreteDeveRetornarValorExatoInformado() {
        Venda venda = new Venda();
        venda.setValorFrete(new BigDecimal("12.40"));

        assertBigDecimalEquals("12.40", venda.getValorFrete());
    }

    @Test
    public void equalsDeveRetornarFalseQuandoComparadoComNull() {
        assertFalse(criarVendaCompleta(1L).equals(null));
    }

    @Test
    public void equalsDeveRetornarFalseQuandoComparadoComOutraClasse() {
        assertFalse(criarVendaCompleta(1L).equals("outra classe"));
    }

    @Test
    public void equalsDeveRetornarTrueParaMesmaInstancia() {
        Venda venda = criarVendaCompleta(1L);

        assertTrue(venda.equals(venda));
    }

    @Test
    public void equalsEHashCodeDevemSerIguaisQuandoObjetosPossuemMesmoEstado() {
        Venda primeiraVenda = criarVendaCompleta(1L);
        Venda segundaVenda = criarVendaCompleta(1L);

        assertEquals(primeiraVenda, segundaVenda);
        assertEquals(primeiraVenda.hashCode(), segundaVenda.hashCode());
    }

    @Test
    public void equalsDeveRetornarFalseQuandoCodigoForDiferente() {
        Venda primeiraVenda = criarVendaCompleta(1L);
        Venda segundaVenda = criarVendaCompleta(2L);

        assertNotEquals(primeiraVenda, segundaVenda);
    }

    @Test
    public void toStringDeveRetornarRepresentacaoNaoVazia() {
        Venda venda = criarVendaCompleta(1L);

        assertFalse(venda.toString().isEmpty());
        assertTrue(venda.toString().contains("valorTotal=20.00"));
    }

    private Venda criarVendaComTotais(String subtotal, String frete, String desconto) {
        Venda venda = new Venda();
        venda.adicionarItens(Arrays.asList(
                criarItemVenda(2, "5.00"),
                criarItemVenda(2, "5.00")));
        venda.setValorFrete(new BigDecimal(frete));
        venda.setValorDesconto(new BigDecimal(desconto));
        venda.setValorTotal(new BigDecimal(subtotal));
        return venda;
    }

    private ItemVenda criarItemVenda(int quantidade, String valorUnitario) {
        ItemVenda itemVenda = new ItemVenda();
        itemVenda.setQuantidade(quantidade);
        itemVenda.setValorUnitario(new BigDecimal(valorUnitario));
        return itemVenda;
    }

    private Venda criarVendaCompleta(Long codigo) {
        Venda venda = new Venda();
        venda.setCodigo(codigo);
        venda.setDataCriacao(LocalDateTime.of(2024, 1, 10, 8, 30));
        venda.setValorFrete(new BigDecimal("4.25"));
        venda.setValorDesconto(new BigDecimal("3.50"));
        venda.setValorTotal(new BigDecimal("20.00"));
        venda.setObservacao("observacao");
        venda.setDataHoraEntrega(LocalDateTime.of(2024, 1, 11, 18, 0));
        venda.setUsuario(UsuarioBuilder.criarUsuario());
        venda.setStatus(StatusVenda.EMITIDA);
        venda.setUuid("uuid-teste");
        venda.setDataEntrega(LocalDate.of(2024, 1, 11));
        venda.setHorarioEntrega(LocalTime.of(18, 0));
        return venda;
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
