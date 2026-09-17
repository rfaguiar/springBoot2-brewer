# 01 · Visão Geral e Propósito

[⬅ Voltar ao README](../Readme.md)

## O que é o projeto

**brewer-springboot** é um sistema web de gestão para uma distribuidora/loja de cervejas artesanais.
Ele cobre o ciclo básico de um pequeno ERP/CRM de vendas:

- Cadastro de produtos (**cervejas**, com estilo, sabor, valor, foto e SKU);
- Cadastro de **clientes** (pessoa física/jurídica) e **cidades/estados**;
- Cadastro de **usuários**, organizados em **grupos** com **permissões** (RBAC);
- Processo de **vendas** (carrinho de itens em sessão, emissão, cancelamento, baixa de estoque);
- **Dashboard** com indicadores de vendas por mês e por origem (nacional/estrangeira);
- Envio de **e-mail** de confirmação de venda com PDF anexado (JasperReports);
- **Upload de fotos** de produto, armazenadas localmente ou na nuvem (Amazon S3);
- Internacionalização (mensagens em `pt_BR`).

## Origem e propósito

O projeto nasceu como um **estudo de caso da Algaworks** (curso de Spring Framework) e evoluiu para um
**laboratório vivo de modernização Java**: a base de código original (Spring MVC) foi
progressivamente migrada e atualizada até a stack atual **Spring Boot 4 + Java 25**, servindo como
material de referência para:

1. **Aprendizado de arquitetura Spring MVC em camadas** (Controller → Service → Repository → Model)
   aplicada a um domínio de negócio real, porém simples de entender (venda de produtos).
2. **Catálogo prático de bibliotecas e padrões do ecossistema Java/Spring** — cada dependência do
   projeto tem um propósito didático específico (ver [Catálogo Tecnológico](04-catalogo-tecnologico.md)).
3. **Exercício de modernização contínua**: upgrades de versão de Java/Spring Boot, correção de CVEs,
   containerização (Docker), avaliação de portabilidade para nuvem (Azure/AppCAT) e melhoria de
   qualidade de testes (mutation testing com PIT).

## Público-alvo desta documentação

Esta documentação foi escrita para ser usada como **material de consulta rápida** por qualquer
engenheiro(a) de software que precise:

- Relembrar como determinada tecnologia/biblioteca Java é usada na prática (Spring Security, Flyway,
  JasperReports, AWS SDK, Thymeleaf, EhCache, etc.);
- Entender rapidamente a arquitetura e estrutura de um projeto Spring Boot em camadas;
- Usar este repositório como **exemplo de referência** ao montar um novo projeto ou ao explicar um
  conceito em uma entrevista, mentoria ou aula.

## Próximos passos de leitura

| Documento | Conteúdo |
|---|---|
| [02 · Arquitetura](02-arquitetura.md) | Camadas, padrões de projeto e fluxo de uma requisição |
| [03 · Estrutura de Pastas](03-estrutura-de-pastas.md) | Mapa de pacotes Java do projeto |
| [04 · Catálogo Tecnológico](04-catalogo-tecnologico.md) | Todas as dependências, versões e para que servem |
| [05 · Features](05-features.md) | Funcionalidades de negócio implementadas |
