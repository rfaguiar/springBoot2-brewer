# 01 · Diagrama de Contexto (C4 Nível 1)

[⬅ Voltar ao Modelo C4](README.md)

Mostra o sistema Brewer como uma "caixa preta", quem o usa e com quais sistemas externos ele troca
informação.

```mermaid
flowchart TB
    colaborador["👤 Colaborador da Distribuidora<br/><i>[Pessoa]</i><br/>Vendedor(a)/administrador(a) que cadastra<br/>produtos, clientes e emite vendas via navegador"]

    brewer["🍺 Brewer<br/><i>[Sistema de Software]</i><br/>Sistema web de gestão de vendas de cervejas<br/>artesanais: cadastro, RBAC, emissão de vendas,<br/>dashboard e e-mail de confirmação com PDF"]

    sendgrid["📧 SendGrid<br/><i>[Sistema Externo]</i><br/>Serviço SMTP de terceiros usado para enviar o<br/>e-mail de confirmação de venda com PDF anexo"]

    s3["☁️ Amazon S3<br/><i>[Sistema Externo]</i><br/>Armazenamento de objetos na nuvem usado<br/>(perfil prod) para hospedar as fotos das cervejas"]

    colaborador -->|"Usa via HTTPS<br/>(navegador / formulários HTML)"| brewer
    brewer -->|"Envia e-mail via SMTP<br/>(porta 587)"| sendgrid
    brewer -->|"Upload/download de fotos<br/>via AWS SDK (perfil prod)"| s3

    classDef person fill:#08427b,color:#fff,stroke:#052e56;
    classDef system fill:#1168bd,color:#fff,stroke:#0b4884;
    classDef external fill:#999999,color:#fff,stroke:#6b6b6b;

    class colaborador person
    class brewer system
    class sendgrid,s3 external
```

## Notas

- Não há APIs públicas ou integrações de sistema-a-sistema além das duas listadas: o Brewer é
  consumido diretamente por um humano via navegador (aplicação server-side rendered, sem SPA/API
  REST separada).
- O uso do Amazon S3 é condicional ao perfil Spring `prod` (`S3Config` é `@Profile("prod")`); fora
  desse perfil, as fotos ficam em armazenamento local (ver [02 · Diagrama de Contêineres](02-containers.md)).
- O envio de e-mail (`MailConfig`) está sempre configurado para usar o host SMTP do SendGrid
  (`smtp.sendgrid.net:587`), com credenciais vindas de variável de ambiente
  (`BREWER_EMAIL_USERNAME`/`BREWER_EMAIL_PASSWORD`).

## Próxima leitura

- [02 · Diagrama de Contêineres](02-containers.md)
