package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// O cadastro pede só o que é do lote (ADR-033); construcao e casa vêm junto apenas na edição de
// um imóvel que já alcançou aquela fase, e o service ignora os grupos de fases futuras.
public record ImovelRequestDTO(

        @NotBlank(message = "Identificador é obrigatório")
        @Size(max = 50, message = "Identificador deve ter no máximo 50 caracteres")
        String identificador,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
        String endereco,

        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
        String numero,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
        String bairro,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String cidade,

        @Size(max = 2, message = "UF deve ter 2 letras")
        String uf,

        @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP deve ter 8 dígitos")
        String cep,

        String observacaoEndereco,

        @Valid DadosLoteDTO lote,
        @Valid DadosConstrucaoDTO construcao,
        @Valid DadosCasaDTO casa,

        @Positive(message = "Valor da compra deve ser positivo")
        BigDecimal compraValor,

        @NotNull(message = "Data da compra é obrigatória")
        LocalDate compraData,

        @NotNull(message = "Vendedor é obrigatório")
        Long compraVendedorId,
        Boolean compraParcelada,

        BigDecimal vendaValorPretendido,

        String descricao
) {

    /**
     * Compra parcelada não pede o valor do lote no cadastro (ADR-037): ele viria de um cronograma
     * que ainda não existe nessa tela, e digitar às cegas geraria juros falsos. Quem preenche é
     * ContratoFinanceiroService.aplicarValorDoLote quando o PARCELAMENTO_COMPRA é criado.
     */
    @AssertTrue(message = "Valor da compra é obrigatório quando a compra não é parcelada")
    public boolean isCompraValorValido() {
        return Boolean.TRUE.equals(compraParcelada) || compraValor != null;
    }
}
