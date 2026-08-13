package com.nutri.hospitalar.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageableUtils {

    private static final int TAMANHO_MAXIMO = 100;

    private PageableUtils() {}

    public static Pageable semOrdenacao(Pageable pageable) {
        int tamanho = Math.min(Math.max(pageable.getPageSize(), 1), TAMANHO_MAXIMO);
        return PageRequest.of(pageable.getPageNumber(), tamanho);
    }
}
