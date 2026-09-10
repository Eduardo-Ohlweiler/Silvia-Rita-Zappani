package com.nutri.hospitalar.clinica.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.usuario.entity.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma anamnese preenchida — o que o paciente contou, numa data.
 *
 * <p>Paciente e profissional são {@code pessoa}, como nas duas avaliações: não
 * há tabela de paciente neste sistema, e o profissional é opcional porque nem
 * todo nutricionista citado tem login.
 *
 * <p><b>A ficha não depende do modelo para ser lida.</b> O {@code modelo} é
 * anulável e serve de rastro; o que a explica é o {@code modeloNome} e o retrato
 * de cada pergunta, gravado em {@link RespostaFicha}. Por isso apagar um modelo
 * do cliente não esvazia prontuário nenhum.
 */
@Entity
@Table(name = "ficha_anamnese")
@Getter
@Setter
@NoArgsConstructor
public class FichaAnamnese extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Pessoa paciente;

    /** Quem conduziu a anamnese. Pode não ter login — por isso é pessoa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id")
    private Pessoa profissional;

    @Column(name = "data_preenchimento", nullable = false)
    private LocalDate dataPreenchimento;

    /** Rastro. Pode virar nulo se o modelo for apagado — e a ficha sobrevive. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_id")
    private ModeloFicha modelo;

    /** Retrato do nome do modelo. É ele que a tela e o papel mostram. */
    @Column(name = "modelo_nome", nullable = false, length = 200)
    private String modeloNome;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    // ─── O ESCORE CONGELADO (fatia 13, docs/13 §5) ──────────────────────────

    /**
     * O escore como ficou no dia — e não como seria recalculado hoje.
     *
     * <p>O retrato de cada resposta já congela as <b>entradas</b>, mas duas
     * coisas ainda moveriam o número de uma ficha salva: {@code
     * pessoa.data_nascimento} é editável fora da ficha, e corrigi-la faria o
     * ponto por idade da NRS-2002 entrar ou sair, calado, em toda ficha antiga
     * daquele paciente; e uma faixa corrigida em Java reclassificaria prontuário
     * retroativamente.
     *
     * <p>Por isso o {@code EscoreDto} inteiro é serializado em
     * {@code escoreJson}, e é ele que a ficha salva devolve. As colunas planas
     * existem porque JSON não se ordena nem se filtra na native query da
     * listagem.
     */
    @Column(name = "escore_codigo", length = 30)
    private String escoreCodigo;

    @Column(name = "escore_total", precision = 4, scale = 1)
    private BigDecimal escoreTotal;

    @Column(name = "escore_classificacao", length = 120)
    private String escoreClassificacao;

    @Column(name = "escore_tom", length = 20)
    private String escoreTom;

    @Column(name = "escore_json", columnDefinition = "TEXT")
    private String escoreJson;

    @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<RespostaFicha> respostas = new ArrayList<>();

    /**
     * Quem digitou — diferente do {@code profissional}, que é quem conduziu.
     * Enquanto o {@code audit_log} não existe, é o que responde "quem preencheu".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;

    public void adicionarResposta(RespostaFicha resposta) {
        resposta.setFicha(this);
        this.respostas.add(resposta);
    }
}
