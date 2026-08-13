package com.nutri.hospitalar.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {

    public ConflictException(String mensagem) {
        super(mensagem, HttpStatus.CONFLICT);
    }
}
