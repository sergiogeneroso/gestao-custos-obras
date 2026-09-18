package com.seegeneroso.gestao_custos_obras.shared.exclusao;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// ADR-040: motivo é obrigatório em toda exclusão lógica — trava de Bean Validation pura.
class ExclusaoRequestDTOTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void motivoEhObrigatorio() {
        Set<ConstraintViolation<ExclusaoRequestDTO>> semMotivo = VALIDATOR.validate(new ExclusaoRequestDTO(null));
        Set<ConstraintViolation<ExclusaoRequestDTO>> emBranco = VALIDATOR.validate(new ExclusaoRequestDTO("   "));

        assertThat(semMotivo).anyMatch(v -> v.getPropertyPath().toString().equals("motivo"));
        assertThat(emBranco).anyMatch(v -> v.getPropertyPath().toString().equals("motivo"));
    }
}
