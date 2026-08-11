CREATE TABLE orcamento_etapa (
    id BIGSERIAL PRIMARY KEY,
    imovel_id BIGINT NOT NULL REFERENCES imovel(id),
    etapa_projeto_id BIGINT NOT NULL REFERENCES etapa_projeto(id),
    valor_orcado NUMERIC(14,2) NOT NULL,
    data_inicio_prevista DATE,
    data_fim_prevista DATE,
    CONSTRAINT uk_orcamento_imovel_etapa UNIQUE (imovel_id, etapa_projeto_id)
);
