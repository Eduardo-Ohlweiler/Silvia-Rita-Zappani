package com.nutri.hospitalar.uti.enums;

/**
 * Onde, dentro da faixa recomendada, a meta é fixada.
 *
 * <p>A recomendação nutricional é sempre um intervalo — 15 a 20 kcal/kg na fase
 * aguda, 1,2 a 1,5 g/kg de proteína — e a meta que desce para a dieta é <b>um
 * número</b>. Alguém precisa escolher, e na planilha esse alguém é invisível: a
 * faixa é tabulada em {@code Necessidades} e a meta é <b>redigitada à mão</b> na
 * aba da dieta (defeito 14 de {@code docs/10} §11).
 *
 * <p>O padrão é {@link #MAXIMO}, e a razão é documental: o exemplo em cache da
 * própria planilha usa VCT <b>1360</b> para um paciente de 68 kg em fase aguda,
 * e 1360 é exatamente 20 × 68 — o topo. O eroERP usa o ponto médio, sem dizer.
 *
 * <p><b>Isto não substitui a progressão.</b> A ESPEN recomenda oferta
 * hipocalórica nos primeiros dias, e isso já está na tabela de 25/50/75/100 %
 * dos dias 1 a 4 ({@code Contínuo!P15:P18}). Baixar a meta <i>e</i> progredir
 * sobre ela descontaria duas vezes.
 *
 * <p>O campo {@code kcal/kg alvo} continua vencendo os três: alvo digitado é
 * {@link OrigemValor#META_PERSONALIZADA} e não passa por aqui.
 */
public enum PosicaoNaFaixa {

    MINIMO("mínimo"),
    MEDIO("médio"),
    MAXIMO("máximo");

    private final String descricao;

    PosicaoNaFaixa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
