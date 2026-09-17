package com.seegeneroso.gestao_custos_obras.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Toda resposta de erro carrega o motivo em `mensagem`, porque é ela que o
 * frontend mostra ao usuário (ver shared/erro/erro.util.ts). Erro sem motivo
 * legível obriga a abrir o log do servidor para descobrir que um campo passou
 * do tamanho da coluna — foi exatamente o que motivou estes handlers.
 */
@Slf4j
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpoErro(ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(corpoErro(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        // LinkedHashMap para a mensagem sair sempre na mesma ordem dos campos do DTO.
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }

        // A mensagem repete os campos em texto corrido: "Erro de validação" sozinho não diz
        // qual campo recusou o valor, que é a informação de que o usuário precisa.
        String detalhe = campos.entrySet().stream()
                .map(entrada -> entrada.getKey() + ": " + entrada.getValue())
                .collect(Collectors.joining("; "));
        Map<String, Object> corpo = corpoErro(detalhe.isBlank() ? "Erro de validação" : detalhe);
        corpo.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    /**
     * Rede de segurança para o que escapa do bean validation: valor maior que a coluna,
     * unique key violada, FK inexistente. A causa mais específica é a mensagem do driver,
     * feia mas concreta — melhor que "não foi possível salvar".
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade", ex);
        String causa = ex.getMostSpecificCause().getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(corpoErro("O banco recusou a operação: " + causa));
    }

    /**
     * Sem este handler, exceder spring.servlet.multipart.max-file-size cai no catch-all genérico
     * como 500. O frontend já checa o tamanho antes de enviar, mas essa é a rede de segurança do
     * lado do servidor.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleArquivoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(corpoErro("Arquivo maior que o limite permitido de 20MB."));
    }

    /** JSON malformado ou valor de enum inexistente — o corpo nem chega a virar DTO. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição ilegível", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(corpoErro("Não foi possível interpretar os dados enviados: " + ex.getMostSpecificCause().getMessage()));
    }

    /**
     * Sem este handler, exceção não mapeada vira 500 sem corpo e o usuário só vê a mensagem
     * genérica do frontend. Expor o tipo e a mensagem é decisão consciente: é uma aplicação
     * interna de um usuário só, e o motivo do erro é justamente o que ele precisa ver. A
     * stack completa fica no log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInesperado(Exception ex) {
        // Exceções do próprio Spring MVC (rota inexistente, método HTTP errado, parâmetro
        // faltando) já trazem o status correto. Sem esta guarda, o catch-all as rebaixaria
        // todas a 500 — um 404 de rota viraria "erro inesperado".
        if (ex instanceof ErrorResponse resposta) {
            log.warn("Requisição recusada pelo Spring MVC: {}", ex.getMessage());
            String detalhe = resposta.getBody().getDetail();
            return ResponseEntity.status(resposta.getStatusCode())
                    .body(corpoErro(detalhe != null ? detalhe : ex.getMessage()));
        }

        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(corpoErro("Erro inesperado (" + ex.getClass().getSimpleName() + "): " + ex.getMessage()));
    }

    private Map<String, Object> corpoErro(String mensagem) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("timestamp", Instant.now());
        corpo.put("mensagem", mensagem);
        return corpo;
    }
}
