package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// O cadastro pede só o que é do lote (ADR-033); construcao e casa vêm junto apenas na edição de
// um imóvel que já alcançou aquela fase, e o service ignora os grupos de fases futuras.
public record ImovelRequestDTO(

        @NotBlank(message = "Identificador é obrigatório")
        String identificador,

        String endereco,
        String numero,
        String bairro,
        String cidade,

        @Size(max = 2, message = "UF deve ter 2 letras")
        String uf,

        @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP deve ter 8 dígitos")
        String cep,

        String observacaoEndereco,

        DadosLoteDTO lote,
        DadosConstrucaoDTO construcao,
        DadosCasaDTO casa,

        BigDecimal compraValor,

        @NotNull(message = "Data da compra é obrigatória")
        LocalDate compraData,

        Long compraVendedorId,
        Boolean compraParcelada,

        BigDecimal vendaValorPretendido,

        String descricao
) {}
