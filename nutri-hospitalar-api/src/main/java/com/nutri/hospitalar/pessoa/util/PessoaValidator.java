package com.nutri.hospitalar.pessoa.util;

/**
 * Validação de documento, portada do eroERP.
 *
 * <p>CPF e CNPJ têm dígito verificador e são validados de verdade — errar o CPF
 * de um paciente é errar a identidade dele. RG, Inscrição Estadual e Municipal
 * não têm regra nacional: variam por estado e por município, então aqui só se
 * confere a faixa de tamanho, que pega o dedo escorregado sem recusar dado
 * legítimo.
 */
public final class PessoaValidator {

    private PessoaValidator() {}

    public static boolean validarCpf(String cpf) {
        if (cpf == null) return false;

        String digitos = somenteDigitos(cpf);

        if (digitos.length() != 11 || digitos.matches("(\\d)\\1{10}")) return false;

        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += Character.getNumericValue(digitos.charAt(i)) * ((t + 1) - i);
            }
            int resto = (soma * 10) % 11;
            if (resto == 10) resto = 0;
            if (Character.getNumericValue(digitos.charAt(t)) != resto) return false;
        }

        return true;
    }

    public static boolean validarCnpj(String cnpj) {
        if (cnpj == null) return false;

        String digitos = somenteDigitos(cnpj);

        if (digitos.length() != 14 || digitos.matches("(\\d)\\1{13}")) return false;

        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        for (int t = 12; t < 14; t++) {
            int soma = 0;
            int[] pesos = (t == 12) ? pesos1 : pesos2;
            for (int i = 0; i < t; i++) {
                soma += Character.getNumericValue(digitos.charAt(i)) * pesos[i];
            }
            int resto = soma % 11;
            int digito = (resto < 2) ? 0 : 11 - resto;
            if (Character.getNumericValue(digitos.charAt(t)) != digito) return false;
        }

        return true;
    }

    public static boolean validarRg(String rg) {
        if (rg == null) return false;
        String limpo = rg.replaceAll("[^0-9Xx]", "");
        return limpo.length() >= 6 && limpo.length() <= 10;
    }

    public static boolean validarInscricao(String inscricao) {
        if (inscricao == null) return false;
        String digitos = somenteDigitos(inscricao);
        return digitos.length() >= 5 && digitos.length() <= 12;
    }

    /** Devolve {@code null} para vazio — é o que o service grava na coluna. */
    public static String somenteDigitosOuNulo(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String digitos = somenteDigitos(valor);
        return digitos.isEmpty() ? null : digitos;
    }

    private static String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }
}
