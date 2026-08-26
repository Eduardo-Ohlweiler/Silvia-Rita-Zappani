package com.nutri.hospitalar.uti.enums;

/**
 * Como o paciente está respirando no dia.
 *
 * <p><b>Este vocabulário é convenção nossa, declarada.</b> A planilha não tem o
 * campo, e o eroERP guarda um {@code VARCHAR(255)} livre rotulado "VM / O₂ (%)"
 * que mistura <i>modo</i> e <i>percentual</i> num campo só — com máscara
 * numérica no formulário e texto no banco, funcionando apenas porque o Jackson
 * converte número em texto na saída.
 *
 * <p>Separar em enum mais {@code fio2Perc} numérico resolve os dois problemas:
 * o modo passa a ser comparável entre dias (o painel consegue plotar), e a FiO₂
 * passa a ser número de verdade.
 *
 * <p>A lista é a escada usual de suporte respiratório em terapia intensiva. Se a
 * Silvia usar outro vocabulário, é aqui que se muda — e a migration 024 tem o
 * {@code CHECK} correspondente.
 */
public enum SuporteVentilatorio {

    AR_AMBIENTE("Ar ambiente"),
    CATETER_NASAL("Cateter nasal"),
    MASCARA("Máscara de oxigênio"),
    ALTO_FLUXO("Cateter nasal de alto fluxo"),
    VNI("Ventilação não invasiva"),
    VENTILACAO_MECANICA("Ventilação mecânica invasiva"),
    TRAQUEOSTOMIA("Traqueostomia");

    private final String descricao;

    SuporteVentilatorio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
