package com.seegeneroso.gestao_custos_obras.shared.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseCleanupRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE despesa DROP COLUMN IF EXISTS etapa_id");
            jdbcTemplate.execute("ALTER TABLE despesa_pagamento DROP COLUMN IF EXISTS envolvido_id");
            log.info("Limpeza de colunas antigas efetuada com sucesso no banco de dados!");
        } catch (Exception e) {
            log.warn("Aviso ao tentar remover colunas antigas: {}", e.getMessage());
        }
    }
}
