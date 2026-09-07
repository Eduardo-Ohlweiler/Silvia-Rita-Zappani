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
 * <p><b>Toda medida tem piso e teto, e os dois são de plausibilidade — não são
 * corte clínico.</b> Por muito tempo só houve teto ("acima de 260 cm não é
 * plausível") e o piso era zero, então valor grande demais voltava 400 com
 * frase legível e valor <b>pequeno demais</b> entrava na conta. É uma
 * assimetria cara aqui, porque a máscara de centavos torna o erro pequeno o
 * mais provável dos dois: quem digita "156" num campo de duas casas obtém
 * <b>1,56 cm</b>, e as equações de estimativa de peso são lineares com uma
 * constante grande subtraída — 0,32 cm de braço devolve peso negativo. Os
 * pisos ficam bem abaixo de qualquer adulto (o P50 de braço da própria
 * {@code percentil_cb} vai de 26,7 a 33,0 cm) e a mensagem ensina a vírgula,
 * que é a causa humana.
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

        @DecimalMin(value = "50.0", message = "Altura de menos de 50 cm não é plausível em adulto. Confira a vírgula: para 156 cm, digite 15600")
        @DecimalMax(value = "260.0", message = "Altura acima de 260 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal alturaCm,

        @DecimalMin(value = "20.0", message = "Altura do joelho de menos de 20 cm não é plausível. Confira a vírgula: para 53 cm, digite 5300")
        @DecimalMax(value = "100.0", message = "Altura do joelho acima de 100 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal alturaJoelhoCm,

        @DecimalMin(value = "10.0", message = "Circunferência do braço de menos de 10 cm não é plausível. Confira a vírgula: para 32 cm, digite 3200")
        @DecimalMax(value = "100.0", message = "Circunferência do braço acima de 100 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal circBracoCm,

        @DecimalMin(value = "10.0", message = "Circunferência da panturrilha de menos de 10 cm não é plausível. Confira a vírgula: para 34 cm, digite 3400")
        @DecimalMax(value = "100.0", message = "Circunferência da panturrilha acima de 100 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal circPanturrilhaCm,

        @DecimalMin(value = "20.0", message = "Circunferência abdominal de menos de 20 cm não é plausível. Confira a vírgula: para 90 cm, digite 9000")
        @DecimalMax(value = "250.0", message = "Circunferência abdominal acima de 250 cm não é plausível")
        @Digits(integer = 4, fraction = 2, message = "No máximo 2 casas decimais")
        BigDecimal circAbdominalCm,

        @DecimalMin(value = "1.0", message = "Peso de menos de 1 kg não é plausível. Confira a vírgula: para 56 kg, digite 56000")
        @DecimalMax(value = "500.0", message = "Peso acima de 500 kg não é plausível")
        @Digits(integer = 4, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal pesoAtualKg,

        @DecimalMin(value = "1.0", message = "Peso de menos de 1 kg não é plausível. Confira a vírgula: para 72,5 kg, digite 72500")
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

        /**
         * Módulo proteico para cobrir a lacuna, quando há lacuna.
         *
         * <p>Não influencia nenhum outro número do cálculo — a sugestão é
         * leitura da lacuna que a dieta já produziu. Só produto do tipo
         * {@code MODULO_PROTEICO} é aceito; o service recusa os outros.
         */
        UUID moduloProteicoId,

        // ─── Hidratação ─────────────────────────────────────────────────
        @DecimalMin(value = "0.0", message = "O volume da dieta não pode ser negativo")
        @Digits(integer = 6, fraction = 3, message = "No máximo 3 casas decimais")
        BigDecimal volumeDietaManualMl
) {}
