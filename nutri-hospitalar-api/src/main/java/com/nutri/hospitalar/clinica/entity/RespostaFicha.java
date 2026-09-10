package com.nutri.hospitalar.clinica.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.clinica.enums.TipoCampoFicha;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Uma resposta — <b>com o retrato da pergunta que a produziu</b>.
 *
 * <p>Este é o ponto que sustenta a fatia. No eroERP a resposta guarda só o
 * {@code campo_id}, e a ficha é desenhada lendo os campos <b>vivos</b> do
 * template. Lá, trocar <i>"Consome álcool?"</i> por <i>"Consome álcool
 * diariamente?"</i> faz um "Sim" respondido há um ano passar a responder outra
 * pergunta, e desativar um campo faz a resposta sumir da tela sem sumir do
 * banco.
 *
 * <p>Aqui é pior que nos módulos de cálculo, e por isso a regra é mais dura: uma
 * resposta de anamnese é <b>só texto</b>. Se o rótulo muda, nada denuncia — não
 * há número ao lado para não fechar. É a armadilha do retrato gravado, que este
 * projeto já pagou duas vezes.
 *
 * <p>Por isso {@code secao}, {@code rotulo}, {@code tipo}, {@code opcoes},
 * {@code ordem} e {@code obrigatorio} são <b>colunas desta tabela</b>, copiadas
 * no momento da gravação. A ficha salva é desenhada, impressa e exportada a
 * partir das suas próprias linhas — nunca do modelo. O {@code campo} continua
 * gravado, mas só como rastro de procedência, e é anulável: o campo pode ser
 * apagado do modelo e a resposta continua válida e legível.
 */
@Entity
@Table(name = "resposta_ficha")
@Getter
@Setter
@NoArgsConstructor
public class RespostaFicha extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false)
    private FichaAnamnese ficha;

    /** Rastro de procedência. Nulo quando o campo saiu do modelo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campo_id")
    private CampoFicha campo;

    // ─── O RETRATO DA PERGUNTA ──────────────────────────────────────────────

    @Column(name = "secao", length = 200)
    private String secao;

    @Column(name = "rotulo", nullable = false, length = 300)
    private String rotulo;

    /** O que torna o {@code valor} legível para sempre. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoCampoFicha tipo;

    @Column(name = "opcoes", columnDefinition = "TEXT")
    private String opcoes;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    @Column(name = "obrigatorio", nullable = false)
    private Boolean obrigatorio = false;

    /**
     * Quanto esta resposta valeu — o retrato do <b>ponto</b>.
     *
     * <p>Irmão de {@code rotulo} e {@code tipo}, e pela mesma razão: editar os
     * pontos de um modelo não pode mudar o escore de uma ficha já gravada. Sem
     * esta coluna, o retrato explicaria a pergunta e não explicaria o número.
     *
     * <p>{@code NUMERIC}, nunca ponto flutuante: quatro itens da MNA valem 0,5, e
     * a faixa intermediária dela vai de 17 a <b>23,5</b>.
     */
    @Column(name = "pontos", precision = 3, scale = 1)
    private BigDecimal pontos;

    /** O retrato do grupo. É por ele que o subtotal é refeito. */
    @Column(name = "grupo_escore", length = 30)
    private String grupoEscore;

    // ─── A RESPOSTA ─────────────────────────────────────────────────────────

    /**
     * Sempre texto, qualquer que seja o tipo. Nulo é "não informado", e é
     * diferente de string vazia só na intenção — as duas são tratadas como
     * ausência pelo service.
     */
    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;
}
