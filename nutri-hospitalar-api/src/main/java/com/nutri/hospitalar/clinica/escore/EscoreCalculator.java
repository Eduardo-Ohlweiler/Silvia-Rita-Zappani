package com.nutri.hospitalar.clinica.escore;

import com.nutri.hospitalar.clinica.dtos.EscoreDto;
import com.nutri.hospitalar.clinica.entity.CampoFicha;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <b>única</b> porta pelo qual um escore é calculado neste sistema.
 *
 * <p>Dois chamadores, uma decisão: o endpoint que recalcula enquanto a ficha é
 * preenchida, e a gravação, que congela o resultado. Não há uma terceira soma no
 * front — e isso não é economia de código, é a lição mais cara já registrada no
 * {@code CLAUDE.md}: a regra do denominador da adesão chegou a existir em quatro
 * linguagens (calculator, ternário do mapper, {@code ??} do front e um
 * {@code COALESCE} em SQL), e <b>sete telas erraram</b>. Um somador em Java e
 * outro em TypeScript seriam a quinta e a sexta.
 */
public final class EscoreCalculator {

    private EscoreCalculator() {}

    /**
     * Devolve {@code null} quando o modelo não aplica escala — que é o caso dos
     * três questionários descritivos da fatia 12. Nulo aqui significa "não há
     * escore a mostrar", e não "o escore falhou": a tela simplesmente não desenha
     * o painel.
     */
    public static EscoreDto avaliar(String escoreCodigo, List<CampoFicha> perguntas,
                                    Map<UUID, String> valores, Integer idadeAnos) {

        return EscalaRegistry.de(escoreCodigo)
                .map(escala -> escala.avaliar(itensDe(perguntas, valores), idadeAnos))
                .orElse(null);
    }

    private static List<ItemRespondido> itensDe(List<CampoFicha> perguntas,
                                                Map<UUID, String> valores) {
        return perguntas.stream()
                .map(campo -> ItemRespondido.de(campo, valores.get(campo.getId())))
                .toList();
    }

    /**
     * Anos completos <b>na data de preenchimento</b>, e não hoje.
     *
     * <p>A NRS-2002 soma um ponto a partir dos 70 anos, e a ficha é um registro
     * de um dia: uma ficha de dois anos atrás reaberta hoje não pode ganhar o
     * ponto que o paciente só fez depois. {@code Period.between} compara mês e
     * dia — subtrair anos erraria em quem faz aniversário amanhã.
     *
     * <p>Nulo quando o paciente não tem data de nascimento no cadastro. Quem
     * decide o que fazer com isso é a escala: a NRS recusa concluir e escreve o
     * porquê, a MNA nem pergunta.
     */
    public static Integer idadeEm(LocalDate nascimento, LocalDate referencia) {
        if (nascimento == null || referencia == null) return null;
        if (nascimento.isAfter(referencia)) return null;
        return Period.between(nascimento, referencia).getYears();
    }
}
