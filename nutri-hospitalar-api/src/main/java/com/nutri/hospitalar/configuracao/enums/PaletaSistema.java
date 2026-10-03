package com.nutri.hospitalar.configuracao.enums;

/**
 * Paleta de cores do sistema, escolhida pelo superadmin e aplicada a todos os
 * usuários de todos os tenants.
 *
 * <p>{@code PADRAO} é a paleta original da marca (azul marinho) e o default:
 * subir a versão não muda a cor de ninguém. As demais seguem a mesma régua de
 * claridade da padrão, degrau a degrau, com outro matiz — docs/05 §11.
 *
 * <p>Cada paleta tem uma variante {@code _MENU_ESCURO}: as mesmas cores, com o
 * menu lateral e o cabeçalho nos tons escuros da própria paleta e o conteúdo
 * claro (docs/05 §11.6).
 *
 * <p>Cada paleta tem um bloco {@code [data-paleta="…"]} no {@code theme.css}
 * (nome da base em minúsculas) e uma entrada em {@code styles/paletas.ts}. O
 * {@code npm run paletas} confere que os três lugares listam as mesmas chaves,
 * e o {@code CHECK} das migrations 033/034 é travado em teste.
 */
public enum PaletaSistema {
    PADRAO,
    MENTA,
    FLORESTA,
    LAVANDA,
    AMEIXA,
    OCEANO,
    CAFE,
    INDIGO,
    MALVA,
    JADE,
    PADRAO_MENU_ESCURO,
    MENTA_MENU_ESCURO,
    FLORESTA_MENU_ESCURO,
    LAVANDA_MENU_ESCURO,
    AMEIXA_MENU_ESCURO,
    OCEANO_MENU_ESCURO,
    CAFE_MENU_ESCURO,
    INDIGO_MENU_ESCURO,
    MALVA_MENU_ESCURO,
    JADE_MENU_ESCURO
}
