package com.nutri.hospitalar.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(String mensagem) {
        super(mensagem, HttpStatus.UNAUTHORIZED);
    }
}
