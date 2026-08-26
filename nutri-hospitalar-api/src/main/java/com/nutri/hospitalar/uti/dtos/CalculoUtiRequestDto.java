package com.nutri.hospitalar.uti.dtos;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.enums.EtniaChumlea;
import com.nutri.hospitalar.uti.enums.FaseTerapia;
import com.nutri.hospitalar.uti.enums.JanelaPerdaPeso;
import com.nutri.hospitalar.uti.enums.ModoInfusao;
import com.nutri.hospitalar.uti.enums.OrigemValor;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.enums.PosicaoNaFaixa;
import com.nutri.hospitalar.uti.enums.SegmentoAmputado;
import com.nutri.hospitalar.uti.enums.TerapiaRenal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * As entradas das quatro abas da tela de cálculo.
 *
 * <p><b>Só entradas.</b> Nenhum resultado é aceito do cliente: o servidor
 * recalcula tudo e devolve os seus próprios números. Mandar {@code "imc": 999}
 * no corpo não tem efeito nenhum — há teste que fixa isso.
 *
 * <p>Todo campo é opcional. A tela recalcula a cada 500 ms e na maior parte do
 * tempo o formulário está pela metade; o que não dá para calcular volta como
 * ausência <b>com o motivo</b>.
 *
 * @param populacaoReferencia coluna de ajuste de CB e CP. Ausente assume
 *                            população clínica, que é o padrão de UTI e o lado
 *                            conservador. Só tem efeito em IMC &lt; 18,5.
 * @param origemPesoPreferida força a fonte do peso de trabalho. Ausente deixa o
 *                            servidor escolher, e o resultado diz o que ele
 *                            escolheu.
 * @param posicaoNaFaixa      onde na faixa recomendada fixar a meta — mínimo,
 *                            médio ou máximo. Ausente assume o máximo. O campo
 *                            {@code kcalPorKgAlvo} continua vencendo os três.
 */
public record CalculoUtiRequestDto(

        // ─── Antropometria ──────────────────────────────────────────────
        Sexo sexo,
        EtniaChumlea etnia,

        @Min(value = 0, message = "A idade não pode ser negativa")
        @Max(value = 130, message = "Idade acima de 130 anos não é plausível")
        Integer idadeAnos,

        @DecimalMin(value = "0.0", message = "A altura não pode ser negativa")
        @DecimalMax(value = "260.0", message = "Altura acima de 260 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal alturaCm,

        @DecimalMin(value = "0.0", message = "A altura do joelho não pode ser negativa")
        @DecimalMax(value = "100.0", message = "Altura do joelho acima de 100 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal alturaJoelhoCm,

        @DecimalMin(value = "0.0", message = "A circunferência do braço não pode ser negativa")
        @DecimalMax(value = "100.0", message = "Circunferência do braço acima de 100 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal circBracoCm,

        @DecimalMin(value = "0.0", message = "A circunferência da panturrilha não pode ser negativa")
        @DecimalMax(value = "100.0", message = "Circunferência da panturrilha acima de 100 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal circPanturrilhaCm,

        @DecimalMin(value = "0.0", message = "A circunferência abdominal não pode ser negativa")
        @DecimalMax(value = "250.0", message = "Circunferência abdominal acima de 250 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal circAbdominalCm,

        @DecimalMin(value = "0.0", message = "O peso não pode ser negativo")
        @DecimalMax(value = "500.0", message = "Peso acima de 500 kg não é plausível")
        @Digits(integer = 4, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal pesoAtualKg,

        @DecimalMin(value = "0.0", message = "O peso habitual não pode ser negativo")
        @DecimalMax(value = "500.0", message = "Peso acima de 500 kg não é plausível")
        @Digits(integer = 4, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal pesoUsualKg,

        JanelaPerdaPeso janelaPerda,
        Set<SegmentoAmputado> segmentosAmputados,
        PopulacaoReferencia populacaoReferencia,
        OrigemValor origemPesoPreferida,

        // ─── Necessidades ───────────────────────────────────────────────
        FaseTerapia fase,
        TerapiaRenal terapiaRenal,

        @DecimalMin(value = "0.0", message = "O alvo calórico não pode ser negativo")
        @DecimalMax(value = "100.0", message = "Alvo acima de 100 kcal/kg não é plausível")
        @Digits(integer = 3, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal kcalPorKgAlvo,

        @DecimalMin(value = "0.0", message = "O alvo proteico não pode ser negativo")
        @DecimalMax(value = "10.0", message = "Alvo acima de 10 g/kg não é plausível")
        @Digits(integer = 2, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal proteinaPorKgAlvo,

        PosicaoNaFaixa posicaoNaFaixa,

        // ─── Dieta enteral ──────────────────────────────────────────────
        UUID formulaEnteralId,
        ModoInfusao modoInfusao,

        @DecimalMin(value = "0.0", message = "O volume não pode ser negativo")
        @Digits(integer = 6, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volumePorTempo,

        @DecimalMin(value = "0.0", message = "O tempo não pode ser negativo")
        @DecimalMax(value = "24.0", message = "A infusão contínua vai até 24 h por dia")
        @Digits(integer = 3, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal tempo,

        // ─── Hidratação ─────────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "O volume da dieta não pode ser negativo")
        @Digits(integer = 6, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volumeDietaManualMl
) {}
