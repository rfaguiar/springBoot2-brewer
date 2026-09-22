# Modelo C4 · brewer-springboot

[⬅ Voltar ao README](../../Readme.md) · [⬅ Voltar ao 02 · Arquitetura](../02-arquitetura.md)

> Este é o **modelo C4** (Context, Containers, Components, Code) completo do sistema, complementando
> a visão de camadas descrita em [02 · Arquitetura](../02-arquitetura.md). Os diagramas foram
> extraídos por varredura direta do código-fonte (`src/main/java/com/brewer`), do `pom.xml`, do
> `Dockerfile`/`docker-compose.yml` e das configurações (`SecurityConfig`, `MailConfig`, `S3Config`).

## O que é o C4 Model

O [C4 Model](https://c4model.com/) descreve a arquitetura de um sistema em **4 níveis de zoom**,
do mais abstrato ao mais detalhado:

| Nível | Pergunta que responde | Documento |
|---|---|---|
| 1. **Contexto** | Quem usa o sistema e com quais sistemas externos ele conversa? | [01 · Diagrama de Contexto](01-contexto.md) |
| 2. **Contêineres** | Quais são as "unidades de deploy" (aplicação, banco, storage) e como se comunicam? | [02 · Diagrama de Contêineres](02-containers.md) |
| 3. **Componentes** | Dentro da aplicação, quais os principais módulos/pacotes e suas responsabilidades? | [03 · Diagrama de Componentes](03-componentes.md) |
| 4. **Dinâmico** | Como um caso de uso concreto flui entre os componentes, passo a passo? | [04 · Diagrama Dinâmico (emissão de venda)](04-dinamico.md) |

> Os diagramas usam sintaxe Mermaid (`flowchart`/`sequenceDiagram`) com convenções visuais de C4
> (pessoa, sistema/contêiner, sistema externo, componente) para garantir renderização nativa no
> GitHub, sem depender de plugins externos.

## Resumo executivo

- **1 pessoa**: colaborador da distribuidora (vendedor/administrador) que acessa via navegador.
- **1 sistema de software**: Brewer (este projeto).
- **2 sistemas externos**: SendGrid (SMTP, e-mail de confirmação) e Amazon S3 (armazenamento de
  fotos em produção).
- **3 contêineres** dentro do sistema Brewer: Aplicação Web (Spring Boot/Thymeleaf), Banco de Dados
  (MySQL 8) e Armazenamento Local (disco, usado por padrão fora do perfil `prod`).
- **~6 grupos de componentes** dentro da Aplicação Web: Web/Apresentação, Segurança, Aplicação
  (services + eventos), Persistência (repositories), Domínio (model) e Infraestrutura Transversal
  (storage, e-mail, relatórios).

## Próxima leitura

- [01 · Diagrama de Contexto](01-contexto.md)
- [02 · Arquitetura (camadas e padrões)](../02-arquitetura.md)
