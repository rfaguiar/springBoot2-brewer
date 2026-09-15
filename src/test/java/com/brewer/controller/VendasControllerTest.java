package com.brewer.controller;

import com.brewer.Constantes;
import com.brewer.builder.*;
import com.brewer.controller.validator.VendaValidator;
import com.brewer.dto.VendaMes;
import com.brewer.dto.VendaOrigem;
import com.brewer.mail.Mailer;
import com.brewer.model.*;
import com.brewer.repository.Cervejas;
import com.brewer.repository.Vendas;
import com.brewer.repository.filter.VendaFilter;
import com.brewer.security.UsuarioSistema;
import com.brewer.service.CadastroVendaService;
import com.brewer.session.TabelasItensSession;
import org.junit.Before;
import org.junit.Test;
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
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
        Mockito.when(mockTabelaItens.getValorTotal(ArgumentMatchers.anyString())).thenReturn(new BigDecimal(456));

        ModelAndView result = controller.nova(venda);

        List<ItemVenda> itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensBenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertFalse(StringUtils.isEmpty(venda.getUuid()));
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal(456), valorItensBenda);
    }

    @Test
    public void testeMetodoSalvarQuandoContemErrosDeveVoltarAViewComOsDadosInformados() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(venda.getUsuario());
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(true);
        Mockito.when(mockVendaService.salvar(venda)).thenReturn(venda);
        Mockito.when(mockRedirectAttributes.addFlashAttribute(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(mockRedirectAttributes);
        Mockito.when(mockTabelaItens.getValorTotal(ArgumentMatchers.anyString())).thenReturn(new BigDecimal(456));

        ModelAndView result = controller.salvar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        List<ItemVenda> itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensBenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        Mockito.verifyNoInteractions(mockRedirectAttributes);
        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal(456), valorItensBenda);
    }

    @Test
    public void testeMetodoSalvarQuandoNaoContemErrosDeveSalvarEMostrarMsgAdequada() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(venda.getUsuario());
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(false);
        Mockito.when(mockVendaService.salvar(venda)).thenReturn(venda);
        Mockito.when(mockRedirectAttributes.addFlashAttribute(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(mockRedirectAttributes);

        ModelAndView result = controller.salvar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verify(mockRedirectAttributes).addFlashAttribute(Constantes.MENSAGEM_VIEW, "Venda salva com sucesso");
        assertEquals(Constantes.REDIRECT_VENDAS_NOVA_VIEW, result.getViewName());
    }

    @Test
    public void  testeMetodoEmitirQuandoContemErrosDeveRetornarParaViewDeVendasComAsValidacoes() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doNothing().when(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(true);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(venda.getUsuario());
        Mockito.doNothing().when(mockVendaService).emitir(venda);
        Mockito.when(mockTabelaItens.getValorTotal(ArgumentMatchers.anyString())).thenReturn(new BigDecimal(456));

        ModelAndView result = controller.emitir(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        List<ItemVenda>  itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensBenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        Mockito.verifyNoInteractions(mockRedirectAttributes);
        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal(456), valorItensBenda);

    }

    @Test
    public void testeMetodoEmitirQuandoNaoComtemErrosNaViewDeveRetornarMsgAdequadaERedirecionarParaViewDeVendas() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doNothing().when(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(false);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(venda.getUsuario());
        Mockito.doNothing().when(mockVendaService).emitir(venda);

        ModelAndView result = controller.emitir(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verify(mockRedirectAttributes).addFlashAttribute(Constantes.MENSAGEM_VIEW, "Venda emitida com sucesso");
        assertEquals(Constantes.REDIRECT_VENDAS_NOVA_VIEW, result.getViewName());

    }

    @Test
    public void testeMetodoEnviarEmailQuandoSalvarUmaVendaDeveEnviarEmailESalvarEMostrarMsgAdequadaERedirecionarParaVendasView() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doNothing().when(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(false);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(venda.getUsuario());
        Mockito.when(mockVendaService.salvar(venda)).thenReturn(venda);
        Mockito.doNothing().when(mockMailer).enviar(venda);

        ModelAndView result = controller.enviarEmail(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verify(mockRedirectAttributes).addFlashAttribute(Constantes.MENSAGEM_VIEW, String.format("Venda n° %d salva e e-mail enviado", venda.getCodigo()));
        assertEquals(Constantes.REDIRECT_VENDAS_NOVA_VIEW, result.getViewName());
    }

    @Test
    public void tesMetodoEnviarEmailQuandoContemErrosDeveRetornarParaVendasView() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doNothing().when(mockVendaValidador).validate(venda, mockBindingResult);
        Mockito.when(mockBindingResult.hasErrors()).thenReturn(true);
        Mockito.when(mockUsuarioSistema.getUsuario()).thenReturn(venda.getUsuario());
        Mockito.when(mockVendaService.salvar(venda)).thenReturn(venda);
        Mockito.doNothing().when(mockMailer).enviar(venda);
        Mockito.when(mockTabelaItens.getValorTotal(ArgumentMatchers.anyString())).thenReturn(new BigDecimal(456));

        ModelAndView result = controller.enviarEmail(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verifyNoInteractions(mockRedirectAttributes);
        Mockito.verifyNoInteractions(mockVendaService);
        Mockito.verifyNoInteractions(mockMailer);

        List<ItemVenda>  itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensBenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal(456), valorItensBenda);
    }

    @Test
    public void testMetodoAdicionarItemDeveAdicionarUmaCervejaERetornarAVendaView() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        List<ItemVenda> itens = ItemVendaBuilder.criarListaItenVenda();
        Mockito.when(mockCervejaRepo.getOne(ArgumentMatchers.anyLong())).thenReturn(cerveja);
        Mockito.doNothing().when(mockTabelaItens).adicionarItem("123", cerveja, 1);
        Mockito.when(mockTabelaItens.getItens("123")).thenReturn(itens);
        Mockito.when(mockTabelaItens.getValorTotal("123")).thenReturn(new BigDecimal(1234));

        ModelAndView result = controller.adicionarItem(new Long(1), "123");

        List<ItemVenda> itensResult = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal totalResult = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL);

        assertEquals(Constantes.TABELA_ITENS_VENDA_VIEW, result.getViewName());
        assertTrue(ItemVendaBuilder.validarListaItensVenda(itens, itensResult));
        assertEquals(new BigDecimal(1234), totalResult);

    }

    @Test
    public void testeMetodoAlterarQuantidadeItemDeveAlterarAQuantidadeERetornarParaVendaView() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        List<ItemVenda> itens = ItemVendaBuilder.criarListaItenVenda();
        Mockito.doNothing().when(mockTabelaItens).alterarQuantidadeItens("123", cerveja, 1);
        Mockito.when(mockTabelaItens.getItens("123")).thenReturn(itens);
        Mockito.when(mockTabelaItens.getValorTotal("123")).thenReturn(new BigDecimal(1234));

        ModelAndView result = controller.alterarQuantidadeItem(cerveja, 1, "123");

        List<ItemVenda> itensResult = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal totalResult = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL);

        assertEquals(Constantes.TABELA_ITENS_VENDA_VIEW, result.getViewName());
        assertTrue(ItemVendaBuilder.validarListaItensVenda(itens, itensResult));
        assertEquals(new BigDecimal(1234), totalResult);

    }

    @Test
    public void testeMetodoExcluirItemDeveRemoverItemERetornarParaVendaView() {
        Cerveja cerveja = CervejaBuilder.criarCerveja();
        List<ItemVenda> itens = ItemVendaBuilder.criarListaItenVenda();
        Mockito.doNothing().when(mockTabelaItens).excluirItem("123", cerveja);
        Mockito.when(mockTabelaItens.getItens("123")).thenReturn(itens);
        Mockito.when(mockTabelaItens.getValorTotal("123")).thenReturn(new BigDecimal(1234));

        ModelAndView result = controller.excluirItem(cerveja, "123");

        List<ItemVenda> itensResult = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal totalResult = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL);

        assertEquals(Constantes.TABELA_ITENS_VENDA_VIEW, result.getViewName());
        assertTrue(ItemVendaBuilder.validarListaItensVenda(itens, itensResult));
        assertEquals(new BigDecimal(1234), totalResult);
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
    }

    @Test
    public void testeMetodoEditarDeveRetornarParaVendaViewComOsDadosDaVendaInformadaPeloId() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.when(mockVendasRepo.buscarComItens(new Long(1))).thenReturn(venda);
        Mockito.doNothing().when(mockTabelaItens).adicionarItem(ArgumentMatchers.anyString(), ArgumentMatchers.any(Cerveja.class), ArgumentMatchers.anyInt());
        Mockito.when(mockTabelaItens.getValorTotal(ArgumentMatchers.anyString())).thenReturn(new BigDecimal(456));

        ModelAndView result = controller.editar(new Long(1));

        List<ItemVenda> itensVenda = (List<ItemVenda>) result.getModel().get(Constantes.ITENS);
        BigDecimal valorFrete = (BigDecimal) result.getModel().get(Constantes.VALOR_FRETE);
        BigDecimal valorDesconto = (BigDecimal) result.getModel().get(Constantes.VALOR_DESCONTO);
        BigDecimal valorItensBenda = (BigDecimal) result.getModel().get(Constantes.VALOR_TOTAL_ITENS);

        assertEquals(Constantes.CADASTRO_VENDA_VIEW, result.getViewName());
        assertFalse(StringUtils.isEmpty(venda.getUuid()));
        assertEquals(venda.getItens(), itensVenda);
        assertEquals(venda.getValorFrete(), valorFrete);
        assertEquals(venda.getValorDesconto(), valorDesconto);
        assertEquals(new BigDecimal(456), valorItensBenda);
    }

    @Test
    public void TesteMetodoCancelarDeveCancelarVendaInformadaERedirecionarParaVendasView() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doNothing().when(mockVendaService).cancelar(venda);

        ModelAndView result = controller.cancelar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verify(mockRedirectAttributes).addFlashAttribute("mensgaem", "Venda cancelada com sucesso");
        assertEquals(Constantes.REDIRECT_VENDAS_VIEW + venda.getCodigo(), result.getViewName());
    }

    @Test
    public void TesteMetodoCancelarNaoTemPermissaoNaoDeveCancelarVendaInformadaERedirecionarParaVendasViewComMsgAdequada() {
        Venda venda = VendaBuilder.criarVenda();
        Mockito.doThrow(new AccessDeniedException("nao Autorizado")).when(mockVendaService).cancelar(venda);

        ModelAndView result = controller.cancelar(venda, mockBindingResult, mockRedirectAttributes, mockUsuarioSistema);

        Mockito.verifyNoInteractions(mockRedirectAttributes);
        assertEquals("/403", result.getViewName());
    }

    @Test
    public void testeMetodoListarTotalTotalVendaPorMesDeveRetornarListaComTotalDeVendasPorMes() {
        List<VendaMes> listaVendas = VendaMesBuilder.criarListaVendaMes();
        Mockito.when(mockVendasRepo.totalPorMes()).thenReturn(listaVendas);

        List<VendaMes> result = controller.listarTotalTotalVendaPorMes();

        assertEquals(listaVendas, result);
    }

    @Test
    public void testeMetodoVendasPorNacionalidadeDeveRetornarListaComTotalDeVendasPorOrigem() {
        List<VendaOrigem> listaOrigem = VendaOrigemBuilder.criarListaVendaOrigem();
        Mockito.when(mockVendasRepo.totalPorOrigem()).thenReturn(listaOrigem);

        List<VendaOrigem> result = controller.vendasPorNacionalidade();

        assertEquals(listaOrigem, result);
    }
}