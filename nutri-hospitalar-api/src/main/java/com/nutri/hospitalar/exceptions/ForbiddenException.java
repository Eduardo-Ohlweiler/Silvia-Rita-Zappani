package com.nutri.hospitalar.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String mensagem) {
        super(mensagem, HttpStatus.FORBIDDEN);
    }
}
