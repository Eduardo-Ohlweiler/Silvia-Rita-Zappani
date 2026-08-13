package com.nutri.hospitalar.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApplicationException {

    public BusinessException(String mensagem) {
        super(mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
