package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Flyway pausado (ADR-013): seed via runner, não migration. Roda uma vez, se a tabela estiver vazia.
@Component
@RequiredArgsConstructor
public class CategoriaDespesaSeedRunner implements CommandLineRunner {

    private static final List<String> CATEGORIAS_PADRAO = List.of(
            "Aquisição",
            "ITBI/Escritura",
            "Documentação",
            "IPTU",
            "Material",
            "Mão de obra",
            "Custos de financiamento",
            "Corretagem",
            "Impostos sobre a venda"
    );

    private final CategoriaDespesaRepository categoriaDespesaRepository;

    @Override
    public void run(String... args) {
        if (categoriaDespesaRepository.count() > 0) {
            return;
        }
        CATEGORIAS_PADRAO.forEach(nome ->
                categoriaDespesaRepository.save(CategoriaDespesaModel.builder().nome(nome).build()));
    }
}
