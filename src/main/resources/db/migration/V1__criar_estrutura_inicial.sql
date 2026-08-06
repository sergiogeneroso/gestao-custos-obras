CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ADMIN'
);

CREATE TABLE aportante (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    documento VARCHAR(20),
    email VARCHAR(150),
    telefone VARCHAR(20),
    tipo_participacao VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE imovel (
    id BIGSERIAL PRIMARY KEY,
    identificador VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('LOTE','IMOVEL')),
    endereco VARCHAR(255),
    area NUMERIC(10,2),
    valor_aquisicao_inicial NUMERIC(14,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANEJAMENTO',
    descricao TEXT,
    ativo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE etapa_projeto (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT
);

CREATE TABLE despesa (
    id BIGSERIAL PRIMARY KEY,
    imovel_id BIGINT NOT NULL REFERENCES imovel(id),
    etapa_projeto_id BIGINT NOT NULL REFERENCES etapa_projeto(id),
    valor NUMERIC(14,2) NOT NULL,
    data_pagamento DATE NOT NULL,
    descricao VARCHAR(255),
    comprovante_url VARCHAR(500)
);

CREATE TABLE despesa_pagamento (
    id BIGSERIAL PRIMARY KEY,
    despesa_id BIGINT NOT NULL REFERENCES despesa(id),
    aportante_id BIGINT NOT NULL REFERENCES aportante(id) ON DELETE RESTRICT,
    valor_pago NUMERIC(14,2) NOT NULL
);