package com.brewer.controller;

import com.brewer.Constantes;
import com.brewer.builder.CervejaBuilder;
import com.brewer.builder.ItemVendaBuilder;
import com.brewer.builder.UsuarioBuilder;
import com.brewer.builder.VendaBuilder;
import com.brewer.builder.VendaMesBuilder;
import com.brewer.builder.VendaOrigemBuilder;
import com.brewer.controller.validator.VendaValidator;
import com.brewer.dto.VendaMes;
import com.brewer.dto.VendaOrigem;
import com.brewer.mail.Mailer;
import com.brewer.model.Cerveja;
import com.brewer.model.ItemVenda;
import com.brewer.model.StatusVenda;
import com.brewer.model.TipoPessoa;
import com.brewer.model.Usuario;
import com.brewer.model.Venda;
import com.brewer.repository.Cervejas;
import com.brewer.repository.Vendas;
import com.brewer.repository.filter.VendaFilter;
import com.brewer.security.UsuarioSistema;
import com.brewer.service.CadastroVendaService;
import com.brewer.session.TabelasItensSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VendasControllerTest {

    private VendasController controller;

    @Mock
    private Vendas mockVendasRepo;
    @Mock
    private VendaValidator mockVendaValidador;
    @Mock
    private CadastroVendaService mockVendaService;
    @Mock
    private TabelasItensSession mockTabelaItens;
    @Mock
    private Cervejas mockCervejaRepo;
    @Mock
    private Mailer mockMailer;
    @Mock
    private UsuarioSistema mockUsuarioSistema;
    @Mock
    private BindingResult mockBindingResult;
    @Mock
    private RedirectAttributes mockRedirectAttributes;
    @Mock
    private HttpServletRequest mockHttpRequest;
    @Mock
    private Pageable mockPegeable;
    @Mock
    private VendaFilter mockVendFilter;

    @Before
    public void iniciarCenarioDeTeste() {
        MockitoAnnotations.initMocks(this);
        this.controller = new VendasController(mockCervejaRepo, mockTabelaItens, mockVendaService, mockVendaValidador, mockVendasRepo, mockMailer);
    }

    @Test
    public void testeMetodoNovaDeveRetornarCadastroVendaViewComOsValoresInformadosNaViewEUuidNovoCasoVazio() {
        Venda venda = VendaBuilder.criarVenda();
        venda.setUuid("");
        Mockito.when(mockTabelaItens.getValorTotal(ArgumentMatchers.anyString())).thenReturn(new BigDecimal("456"));

        ModelAndView result = controller.nova(venda);

        List<ItemVenda> itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensVenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertFalse(StringUtils.isEmpty(venda.getUuid()));
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal("456"), valorItensVenda);
        Mockito.verify(mockTabelaItens).getValorTotal(venda.getUuid());
    }

    @Test
    public void testeMetodoSalvarQuandoContemErrosDeveVoltarAViewComOsDadosInformadosEValidarVenda() {
        Venda venda = criarVendaSpy("uuid-salvar-erro");
        List<ItemVenda> itensSessao = criarItensSessao();
        Mockito.when(mockTabelaItens.getItens(venda.getUuid())).thenReturn(itensSessao);
        Mockito.when(mockTabelaItens.getValorTotal(venda.getUuid())).thenReturn(new BigDecimal("456"));
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(true);

        ModelAndView result = controller.salvar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        assertModelAndViewNova(result, venda, itensSessao, "456");
        Mockito.verify(mockTabelaItens).getItens(venda.getUuid());
        Mockito.verify(mockTabelaItens).getValorTotal(venda.getUuid());
        Mockito.verify(venda).adicionarItens(itensSessao);
        Mockito.verify(venda).calcularValorTotal();
        Mockito.verify(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.verify(mockBindingResult).hasErrors();
        Mockito.verify(venda, Mockito.never()).setUsuario(ArgumentMatchers.any(Usuario.class));
        Mockito.verifyNoInteractions(mockRedirectAttributes, mockVendaService, mockMailer, mockUsuarioSistema);
    }

    @Test
    public void testeMetodoSalvarQuandoNaoContemErrosDeveSalvarEMostrarMsgAdequadaComInteracoesEsperadas() {
        Venda venda = criarVendaSpy("uuid-salvar-ok");
        List<ItemVenda> itensSessao = criarItensSessao();
        Usuario usuarioEsperado = UsuarioBuilder.criarUsuario();
        Mockito.when(mockTabelaItens.getItens(venda.getUuid())).thenReturn(itensSessao);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(false);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(usuarioEsperado);
        Mockito.when(mockVendaService.salvar(venda)).thenReturn(venda);

        ModelAndView result = controller.salvar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(mockTabelaItens).getItens(venda.getUuid());
        Mockito.verify(venda).adicionarItens(itensSessao);
        Mockito.verify(venda).calcularValorTotal();
        Mockito.verify(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.verify(mockBindingResult).hasErrors();
        Mockito.verify(mockUsuarioSistema).getUsuario();
        Mockito.verify(venda).setUsuario(usuarioCaptor.capture());
        Mockito.verify(mockVendaService).salvar(venda);
        Mockito.verify(mockRedirectAttributes).addFlashAttribute(Constantes.MENSAGEM_VIEW, "Venda salva com sucesso");
        Mockito.verifyNoInteractions(mockMailer);
        assertSame(usuarioEsperado, usuarioCaptor.getValue());
        assertEquals(Constantes.REDIRECT_VENDAS_NOVA_VIEW, result.getViewName());
    }

    @Test
    public void testeMetodoEmitirQuandoContemErrosDeveRetornarParaViewDeVendasComAsValidacoes() {
        Venda venda = criarVendaSpy("uuid-emitir-erro");
        List<ItemVenda> itensSessao = criarItensSessao();
        Mockito.when(mockTabelaItens.getItens(venda.getUuid())).thenReturn(itensSessao);
        Mockito.when(mockTabelaItens.getValorTotal(venda.getUuid())).thenReturn(new BigDecimal("456"));
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(true);

        ModelAndView result = controller.emitir(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        assertModelAndViewNova(result, venda, itensSessao, "456");
        Mockito.verify(mockTabelaItens).getItens(venda.getUuid());
        Mockito.verify(mockTabelaItens).getValorTotal(venda.getUuid());
        Mockito.verify(venda).adicionarItens(itensSessao);
        Mockito.verify(venda).calcularValorTotal();
        Mockito.verify(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.verify(mockBindingResult).hasErrors();
        Mockito.verify(venda, Mockito.never()).setUsuario(ArgumentMatchers.any(Usuario.class));
        Mockito.verifyNoInteractions(mockRedirectAttributes, mockVendaService, mockMailer, mockUsuarioSistema);
    }

    @Test
    public void testeMetodoEmitirQuandoNaoComtemErrosNaViewDeveRetornarMsgAdequadaERedirecionarParaViewDeVendas() {
        Venda venda = criarVendaSpy("uuid-emitir-ok");
        List<ItemVenda> itensSessao = criarItensSessao();
        Usuario usuarioEsperado = UsuarioBuilder.criarUsuario();
        Mockito.when(mockTabelaItens.getItens(venda.getUuid())).thenReturn(itensSessao);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(false);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(usuarioEsperado);

        ModelAndView result = controller.emitir(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(mockTabelaItens).getItens(venda.getUuid());
        Mockito.verify(venda).adicionarItens(itensSessao);
        Mockito.verify(venda).calcularValorTotal();
        Mockito.verify(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.verify(mockBindingResult).hasErrors();
        Mockito.verify(mockUsuarioSistema).getUsuario();
        Mockito.verify(venda).setUsuario(usuarioCaptor.capture());
        Mockito.verify(mockVendaService).emitir(venda);
        Mockito.verify(mockRedirectAttributes).addFlashAttribute(Constantes.MENSAGEM_VIEW, "Venda emitida com sucesso");
        Mockito.verifyNoInteractions(mockMailer);
        assertSame(usuarioEsperado, usuarioCaptor.getValue());
        assertEquals(Constantes.REDIRECT_VENDAS_NOVA_VIEW, result.getViewName());
    }

    @Test
    public void testeMetodoEnviarEmailQuandoSalvarUmaVendaDeveEnviarEmailESalvarEMostrarMsgAdequadaERedirecionarParaVendasView() {
        Venda venda = criarVendaSpy("uuid-email-ok");
        List<ItemVenda> itensSessao = criarItensSessao();
        Usuario usuarioEsperado = UsuarioBuilder.criarUsuario();
        Mockito.when(mockTabelaItens.getItens(venda.getUuid())).thenReturn(itensSessao);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(false);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(usuarioEsperado);
        Mockito.when(mockVendaService.salvar(venda)).thenReturn(venda);

        ModelAndView result = controller.enviarEmail(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        Mockito.verify(mockTabelaItens).getItens(venda.getUuid());
        Mockito.verify(venda).adicionarItens(itensSessao);
        Mockito.verify(venda).calcularValorTotal();
        Mockito.verify(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.verify(mockBindingResult).hasErrors();
        Mockito.verify(mockUsuarioSistema).getUsuario();
        Mockito.verify(venda).setUsuario(usuarioCaptor.capture());
        Mockito.verify(mockVendaService).salvar(venda);
        Mockito.verify(mockMailer).enviar(venda);
        Mockito.verify(mockRedirectAttributes)
                .addFlashAttribute(Constantes.MENSAGEM_VIEW, String.format("Venda n° %d salva e e-mail enviado", venda.getCodigo()));
        assertSame(usuarioEsperado, usuarioCaptor.getValue());
        assertEquals(Constantes.REDIRECT_VENDAS_NOVA_VIEW, result.getViewName());
    }

    @Test
    public void tesMetodoEnviarEmailQuandoContemErrosDeveRetornarParaVendasView() {
        Venda venda = criarVendaSpy("uuid-email-erro");
        List<ItemVenda> itensSessao = criarItensSessao();
        Mockito.when(mockTabelaItens.getItens(venda.getUuid())).thenReturn(itensSessao);
        Mockito.when(mockTabelaItens.getValorTotal(venda.getUuid())).thenReturn(new BigDecimal("456"));
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(true);

        ModelAndView result = controller.enviarEmail(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        assertModelAndViewNova(result, venda, itensSessao, "456");
        Mockito.verify(mockTabelaItens).getItens(venda.getUuid());
        Mockito.verify(mockTabelaItens).getValorTotal(venda.getUuid());
        Mockito.verify(venda).adicionarItens(itensSessao);
        Mockito.verify(venda).calcularValorTotal();
        Mockito.verify(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.verify(mockBindingResult).hasErrors();
        Mockito.verify(venda, Mockito.never()).setUsuario(ArgumentMatchers.any(Usuario.class));
        Mockito.verifyNoInteractions(mockRedirectAttributes, mockVendaService, mockMailer, mockUsuarioSistema);
    }

    @Test
    public void testMetodoAdicionarItemDeveAdicionarUmaCervejaERetornarAVendaView() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        List<ItemVenda> itens = ItemVendaBuilder.criarListaItenVenda();
        Mockito.when(mockCervejaRepo.getOne(1L)).thenReturn(cerveja);
        Mockito.when(mockTabelaItens.getItens("123")).thenReturn(itens);
        Mockito.when(mockTabelaItens.getValorTotal("123")).thenReturn(new BigDecimal("1234"));

        ModelAndView result = controller.adicionarItem(1L, "123");

        List<ItemVenda> itensResult = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal totalResult = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL);

        assertEquals(Constantes.TABELA_ITENS_VENDA_VIEW, result.getViewName());
        assertTrue(ItemVendaBuilder.validarListaItensVenda(itens, itensResult));
        assertEquals(new BigDecimal("1234"), totalResult);
        Mockito.verify(mockCervejaRepo).getOne(1L);
        Mockito.verify(mockTabelaItens).adicionarItem("123", cerveja, 1);
        Mockito.verify(mockTabelaItens).getItens("123");
        Mockito.verify(mockTabelaItens).getValorTotal("123");
    }

    @Test
    public void testeMetodoAlterarQuantidadeItemDeveAlterarAQuantidadeERetornarParaVendaView() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        List<ItemVenda> itens = ItemVendaBuilder.criarListaItenVenda();
        Mockito.when(mockTabelaItens.getItens("123")).thenReturn(itens);
        Mockito.when(mockTabelaItens.getValorTotal("123")).thenReturn(new BigDecimal("1234"));

        ModelAndView result = controller.alterarQuantidadeItem(cerveja, 1, "123");

        List<ItemVenda> itensResult = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal totalResult = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL);

        assertEquals(Constantes.TABELA_ITENS_VENDA_VIEW, result.getViewName());
        assertTrue(ItemVendaBuilder.validarListaItensVenda(itens, itensResult));
        assertEquals(new BigDecimal("1234"), totalResult);
        Mockito.verify(mockTabelaItens).alterarQuantidadeItens("123", cerveja, 1);
        Mockito.verify(mockTabelaItens).getItens("123");
        Mockito.verify(mockTabelaItens).getValorTotal("123");
    }

    @Test
    public void testeMetodoExcluirItemDeveRemoverItemERetornarParaVendaView() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        List<ItemVenda> itens = ItemVendaBuilder.criarListaItenVenda();
        Mockito.when(mockTabelaItens.getItens("123")).thenReturn(itens);
        Mockito.when(mockTabelaItens.getValorTotal("123")).thenReturn(new BigDecimal("1234"));

        ModelAndView result = controller.excluirItem(cerveja, "123");

        List<ItemVenda> itensResult = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal totalResult = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL);

        assertEquals(Constantes.TABELA_ITENS_VENDA_VIEW, result.getViewName());
        assertTrue(ItemVendaBuilder.validarListaItensVenda(itens, itensResult));
        assertEquals(new BigDecimal("1234"), totalResult);
        Mockito.verify(mockTabelaItens).excluirItem("123", cerveja);
        Mockito.verify(mockTabelaItens).getItens("123");
        Mockito.verify(mockTabelaItens).getValorTotal("123");
    }

    @Test
    public void testeMetodoPesquisarDeveRetornarVendasViewComValoresPadraoETodasAsVendasRealizadas() {
        List<Venda> listaVendas = VendaBuilder.criarListaVenda();
        PageImpl<Venda> vendasPage = new PageImpl<>(listaVendas, mockPegeable, 1);
        Mockito.when(mockVendasRepo.filtrar(mockVendFilter, mockPegeable)).thenReturn(vendasPage);
        Mockito.when(mockHttpRequest.getRequestURL()).thenReturn(new StringBuffer("url"));
        Mockito.when(mockHttpRequest.getQueryString()).thenReturn("?");

        ModelAndView result = controller.pesquisar(mockVendFilter, mockPegeable, mockHttpRequest);

        StatusVenda[] statusVendasResult = (StatusVenda[]) result.getModel().get(Constantes.TODOS_STATUS);
        TipoPessoa[] tipoPessoasResult = (TipoPessoa[]) result.getModel().get(Constantes.TIPOS_PESSOA);

        assertEquals(Constantes.PESQUISA_VENDAS_VIEW, result.getViewName());
        assertArrayEquals(StatusVenda.values(), statusVendasResult);
        assertArrayEquals(TipoPessoa.values(), tipoPessoasResult);
        Mockito.verify(mockVendasRepo).filtrar(mockVendFilter, mockPegeable);
    }

    @Test
    public void testeMetodoEditarDeveRetornarParaVendaViewComOsDadosDaVendaInformadaPeloIdEAdicionarItensNaSessao() {
        Venda venda = VendaBuilder.criarVenda();
        venda.setUuid("uuid-editar");
        ItemVenda primeiroItem = criarItemVenda(2, "10.00");
        ItemVenda segundoItem = criarItemVenda(3, "7.50");
        venda.setItens(Arrays.asList(primeiroItem, segundoItem));
        Mockito.when(mockVendasRepo.buscarComItens(1L)).thenReturn(venda);
        Mockito.when(mockTabelaItens.getValorTotal(venda.getUuid())).thenReturn(new BigDecimal("456"));

        ModelAndView result = controller.editar(1L);

        List<ItemVenda> itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensVenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertEquals("uuid-editar", venda.getUuid());
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal("456"), valorItensVenda);
        Mockito.verify(mockVendasRepo).buscarComItens(1L);
        Mockito.verify(mockTabelaItens).adicionarItem("uuid-editar", primeiroItem.getCerveja(), primeiroItem.getQuantidade());
        Mockito.verify(mockTabelaItens).adicionarItem("uuid-editar", segundoItem.getCerveja(), segundoItem.getQuantidade());
        Mockito.verify(mockTabelaItens).getValorTotal("uuid-editar");
    }

    @Test
    public void testeMetodoCancelarDeveCancelarVendaInformadaERedirecionarParaVendasView() {
        Venda venda = VendaBuilder.criarVenda();

        ModelAndView result = controller.cancelar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verify(mockVendaService).cancelar(venda);
        Mockito.verify(mockRedirectAttributes).addFlashAttribute("mensgaem", "Venda cancelada com sucesso");
        assertEquals(Constantes.REDIRECT_VENDAS_VIEW + venda.getCodigo(), result.getViewName());
    }

    @Test
    public void testeMetodoCancelarNaoTemPermissaoNaoDeveCancelarVendaInformadaERedirecionarParaVendasViewComMsgAdequada() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doThrow(new AccessDeniedException("nao Autorizado")).when(mockVendaService).cancelar(venda);

        ModelAndView result = controller.cancelar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verify(mockVendaService).cancelar(venda);
        Mockito.verifyNoInteractions(mockRedirectAttributes);
        assertEquals("/403", result.getViewName());
    }

    @Test
    public void testeMetodoListarTotalTotalVendaPorMesDeveRetornarListaComTotalDeVendasPorMes() {
        List<VendaMes> listaVendas = VendaMesBuilder.criarListaVendaMes();
        Mockito.when(mockVendasRepo.totalPorMes()).thenReturn(listaVendas);

        List<VendaMes> result = controller.listarTotalTotalVendaPorMes();

        assertEquals(listaVendas, result);
        Mockito.verify(mockVendasRepo).totalPorMes();
    }

    @Test
    public void testeMetodoVendasPorNacionalidadeDeveRetornarListaComTotalDeVendasPorOrigem() {
        List<VendaOrigem> listaOrigem = VendaOrigemBuilder.criarListaVendaOrigem();
        Mockito.when(mockVendasRepo.totalPorOrigem()).thenReturn(listaOrigem);

        List<VendaOrigem> result = controller.vendasPorNacionalidade();

        assertEquals(listaOrigem, result);
        Mockito.verify(mockVendasRepo).totalPorOrigem();
    }

    private Venda criarVendaSpy(String uuid) {
        Venda venda = Mockito.spy(VendaBuilder.criarVenda());
        venda.setUuid(uuid);
        return venda;
    }

    private List<ItemVenda> criarItensSessao() {
        return Arrays.asList(
                criarItemVenda(2, "10.00"),
                criarItemVenda(1, "7.50"));
    }

    private ItemVenda criarItemVenda(int quantidade, String valorUnitario) {
        ItemVenda itemVenda = new ItemVenda();
        itemVenda.setQuantidade(quantidade);
        itemVenda.setValorUnitario(new BigDecimal(valorUnitario));
        itemVenda.setCerveja(CervejaBuilder.criarCerveja());
        return itemVenda;
    }

    private void assertModelAndViewNova(ModelAndView result, Venda venda, List<ItemVenda> itensEsperados, String valorTotalItensEsperado) {
        List<ItemVenda> itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensVenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertSame(itensEsperados, itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal(valorTotalItensEsperado), valorItensVenda);
    }
}
