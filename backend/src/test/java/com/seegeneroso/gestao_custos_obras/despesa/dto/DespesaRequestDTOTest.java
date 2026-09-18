package com.seegeneroso.gestao_custos_obras.despesa.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// ADR-023: pagador é obrigatório e valor é sempre positivo — travas de Bean Validation puras, sem
// regra própria no Service, então testadas direto contra o Validator.
class DespesaRequestDTOTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void pagadorEhObrigatorio() {
        Set<ConstraintViolation<DespesaRequestDTO>> violacoes = VALIDATOR.validate(dto(null, new BigDecimal("100.00")));

        assertThat(violacoes).anyMatch(v -> v.getPropertyPath().toString().equals("pagadorId"));
    }

    @Test
    void valorDeveSerPositivo() {
        Set<ConstraintViolation<DespesaRequestDTO>> comZero = VALIDATOR.validate(dto(3L, BigDecimal.ZERO));
        Set<ConstraintViolation<DespesaRequestDTO>> comNegativo = VALIDATOR.validate(dto(3L, new BigDecimal("-100.00")));

        assertThat(comZero).anyMatch(v -> v.getPropertyPath().toString().equals("valor"));
        assertThat(comNegativo).anyMatch(v -> v.getPropertyPath().toString().equals("valor"));
    }

    private DespesaRequestDTO dto(Long pagadorId, BigDecimal valor) {
        return new DespesaRequestDTO(null, 2L, pagadorId, null, null, null, null,
                valor, LocalDate.of(2026, 8, 20), "Compra de material", null);
    }
}
