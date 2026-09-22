# 04 · Diagrama Dinâmico — Emissão de Venda (C4 Nível "Dynamic")

[⬅ Voltar ao Modelo C4](README.md)

O diagrama dinâmico do C4 model documenta como os componentes colaboram para realizar **um caso de
uso específico**, em ordem de execução — complementando o diagrama de componentes (que é estático).
Aqui detalhamos o fluxo de **emissão de uma venda**, já resumido em
[02 · Arquitetura](../02-arquitetura.md#fluxo-típico-de-uma-requisição-exemplo-emitir-uma-venda).

```mermaid
sequenceDiagram
    actor Colaborador
    participant Controller as VendasController
    participant Validator as VendaValidator
    participant Service as CadastroVendaService
    participant VendaRepo as Vendas (Repository)
    participant Publisher as ApplicationEventPublisher
    participant Listener as VendaListener
    participant CervejaRepo as Cervejas (Repository)
    participant DB as MySQL
    participant Mailer as Mailer
    participant Jasper as JasperReports
    participant SendGrid as SendGrid (SMTP)

    Colaborador->>Controller: Submete formulário de venda
    Controller->>Validator: valida(venda, bindingResult)
    Validator-->>Controller: OK (ou erros de negócio, ex.: venda sem itens)

    alt Formulário inválido
        Controller-->>Colaborador: Retorna a view com mensagens de erro
    else Formulário válido
        Controller->>Service: emitir(venda)
        Service->>VendaRepo: save(venda)
        VendaRepo->>DB: INSERT venda + itens
        Service->>Publisher: publishEvent(new VendaEvent(venda))
        Publisher->>Listener: onApplicationEvent(evento)
        Listener->>CervejaRepo: darBaixaNoEstoque(itens da venda)
        CervejaRepo->>DB: UPDATE estoque de cada cerveja
        Service-->>Controller: venda emitida com sucesso

        opt Envio de e-mail de confirmação habilitado
            Service->>Mailer: enviarEmailVenda(venda)
            Mailer->>Jasper: gerarPdfVenda(venda)
            Jasper-->>Mailer: PDF em bytes
            Mailer->>SendGrid: send(email + PDF anexo) via SMTP:587
        end

        Controller-->>Colaborador: Redirect + mensagem flash de sucesso
    end
```

## Passo a passo

1. O colaborador submete o formulário de venda (itens já estavam no carrinho em sessão —
   `TabelasItensSession` — antes deste passo).
2. `VendasController#emitir` delega a validação de negócio ao `VendaValidator` (ex.: venda sem
   itens é rejeitada aqui, além das validações Bean Validation já aplicadas no binding).
3. Se válida, o controller delega a `CadastroVendaService#emitir`.
4. O service persiste a venda (e seus itens) via o repository `Vendas`.
5. O service publica um `VendaEvent` via `ApplicationEventPublisher` — desacoplando a baixa de
   estoque da própria emissão da venda (padrão Observer/Domain Event).
6. `VendaListener`, escutando o evento (`@EventListener`), dá baixa no estoque de cada `Cerveja`
   vendida através do repository `Cervejas`.
7. Opcionalmente, o `Mailer` gera o PDF da venda via JasperReports e envia o e-mail de confirmação
   através do SMTP do SendGrid.
8. O controller redireciona o colaborador com uma mensagem flash de sucesso
   (`RedirectAttributes`).

## Próxima leitura

- [03 · Diagrama de Componentes](03-componentes.md)
- [README do Modelo C4](README.md)
