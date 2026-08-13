package com.nutri.hospitalar.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErroResponseDto> negocio(ApplicationException e, HttpServletRequest req) {
        return build(e.getStatus(), e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDto> validacao(MethodArgumentNotValidException e,
                                                     HttpServletRequest req) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, mensagem, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponseDto> parametroAusente(MissingServletRequestParameterException e,
                                                            HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + e.getParameterName(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponseDto> tipoInvalido(MethodArgumentTypeMismatchException e,
                                                        HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro: " + e.getName(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponseDto> corpoIlegivel(HttpMessageNotReadableException e,
                                                         HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", req);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErroResponseDto> rotaInexistente(Exception e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponseDto> metodoNaoSuportado(HttpRequestMethodNotSupportedException e,
                                                              HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Método " + req.getMethod() + " não permitido neste recurso", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponseDto> tipoNaoSuportado(HttpMediaTypeNotSupportedException e,
                                                            HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponseDto> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        log.warn("Acesso negado em {} {}", req.getMethod(), req.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponseDto> naoAutenticado(AuthenticationException e, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Não autenticado", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponseDto> integridade(DataIntegrityViolationException e,
                                                       HttpServletRequest req) {
        String ref = UUID.randomUUID().toString();
        log.warn("Violação de integridade [{}] em {}", ref, req.getRequestURI(), e);
        return build(HttpStatus.CONFLICT,
                "A operação viola uma regra de integridade dos dados. Referência: " + ref, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDto> inesperado(Exception e, HttpServletRequest req) {
        String ref = UUID.randomUUID().toString();
        log.error("Erro inesperado [{}] em {} {}", ref, req.getMethod(), req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno. Referência: " + ref, req);
    }

    private ResponseEntity<ErroResponseDto> build(HttpStatus status, String mensagem,
                                                  HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErroResponseDto.de(mensagem, status.value(), req.getRequestURI()));
    }
}
