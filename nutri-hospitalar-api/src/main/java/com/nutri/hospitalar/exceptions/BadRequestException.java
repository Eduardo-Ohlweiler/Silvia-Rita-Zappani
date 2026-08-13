package com.nutri.hospitalar.exceptions;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApplicationException {

    public BadRequestException(String mensagem) {
        super(mensagem, HttpStatus.BAD_REQUEST);
    }
}
