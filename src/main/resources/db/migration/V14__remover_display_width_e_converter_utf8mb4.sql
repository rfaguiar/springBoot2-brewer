-- Corrige warnings do MySQL sem alterar migrations ja aplicadas (V01, V04, V05, V07, V12):
--   1) "Integer display width is deprecated" -> remove o display width de colunas BIGINT(20)
--   2) "'utf8' is currently an alias for UTF8MB3..." -> converte tabelas para utf8mb4

-- estilo / cerveja (V01)
ALTER TABLE estilo CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE cerveja CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE estilo MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE cerveja MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE cerveja MODIFY COLUMN codigo_estilo BIGINT NOT NULL;

-- estado / cidade (V04)
ALTER TABLE estado CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE cidade CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE estado MODIFY COLUMN codigo BIGINT;
ALTER TABLE cidade MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE cidade MODIFY COLUMN codigo_estado BIGINT NOT NULL;

-- cliente (V05)
ALTER TABLE cliente CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE cliente MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE cliente MODIFY COLUMN codigo_cidade BIGINT;

-- usuario / grupo / permissao / usuario_grupo / grupo_permissao (V07)
ALTER TABLE usuario CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE grupo CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE permissao CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE usuario_grupo CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE grupo_permissao CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE usuario MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE grupo MODIFY COLUMN codigo BIGINT;
ALTER TABLE permissao MODIFY COLUMN codigo BIGINT;
ALTER TABLE usuario_grupo MODIFY COLUMN codigo_usuario BIGINT NOT NULL;
ALTER TABLE usuario_grupo MODIFY COLUMN codigo_grupo BIGINT NOT NULL;
ALTER TABLE grupo_permissao MODIFY COLUMN codigo_grupo BIGINT NOT NULL;
ALTER TABLE grupo_permissao MODIFY COLUMN codigo_permissao BIGINT NOT NULL;

-- venda / item_venda (V12)
ALTER TABLE venda CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE item_venda CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE venda MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE venda MODIFY COLUMN codigo_cliente BIGINT NOT NULL;
ALTER TABLE venda MODIFY COLUMN codigo_usuario BIGINT NOT NULL;
ALTER TABLE item_venda MODIFY COLUMN codigo BIGINT AUTO_INCREMENT;
ALTER TABLE item_venda MODIFY COLUMN codigo_cerveja BIGINT NOT NULL;
ALTER TABLE item_venda MODIFY COLUMN codigo_venda BIGINT NOT NULL;
