package com.nutri.hospitalar.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApplicationException {

    public NotFoundException(String mensagem) {
        super(mensagem, HttpStatus.NOT_FOUND);
    }
}
