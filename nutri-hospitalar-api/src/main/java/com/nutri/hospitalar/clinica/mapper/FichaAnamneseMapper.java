package com.nutri.hospitalar.clinica.mapper;

import com.nutri.hospitalar.clinica.dtos.FichaAnamneseListaDto;
import com.nutri.hospitalar.clinica.dtos.FichaAnamneseResponseDto;
import com.nutri.hospitalar.clinica.dtos.RespostaFichaResponseDto;
import com.nutri.hospitalar.clinica.entity.FichaAnamnese;
import com.nutri.hospitalar.clinica.entity.RespostaFicha;

import java.util.Comparator;
import java.util.List;

public final class FichaAnamneseMapper {

    private FichaAnamneseMapper() {}

    /**
     * @param modeloAlterado calculado pelo service comparando o retrato com o
     *                       modelo vivo. Vem de fora porque é uma decisão, não
     *                       uma leitura de campo — e decisão publicada é decisão
     *                       que a tela não precisa reimplementar.
     *
     * <p>O escore é <b>desserializado</b> de {@code escore_json}, e nunca
     * recalculado: ele é o número do dia, e recalcular ao abrir faria uma data de
     * nascimento corrigida mexer, calada, no escore NRS de toda ficha antiga
     * daquele paciente. Ver docs/13 §5.
     */
    public static FichaAnamneseResponseDto toResponse(FichaAnamnese ficha,
                                                      boolean modeloRemovido,
                                                      boolean modeloAlterado) {
        return new FichaAnamneseResponseDto(
                ficha.getId(),
                ficha.getPaciente().getId(),
                ficha.getPaciente().getNome(),
                ficha.getProfissional() == null ? null : ficha.getProfissional().getId(),
                ficha.getProfissional() == null ? null : ficha.getProfissional().getNome(),
                ficha.getDataPreenchimento(),
                ficha.getModelo() == null ? null : ficha.getModelo().getId(),
                ficha.getModeloNome(),
                modeloRemovido,
                modeloAlterado,
                EscoreJson.paraDto(ficha.getEscoreJson()),
                respostasOrdenadas(ficha),
                ficha.getObservacao(),
                ficha.getCreatedAt(),
                ficha.getUpdatedAt());
    }

    public static FichaAnamneseListaDto toLista(FichaAnamnese ficha, int respondidas, int total) {
        return new FichaAnamneseListaDto(
                ficha.getId(),
                ficha.getPaciente().getId(),
                ficha.getPaciente().getNome(),
                ficha.getProfissional() == null ? null : ficha.getProfissional().getNome(),
                ficha.getDataPreenchimento(),
                /* Do retrato, nunca do modelo: a lista tem de continuar dizendo
                   de qual modelo a ficha veio depois de ele ser apagado. */
                ficha.getModeloNome(),
                respondidas,
                total,
                ficha.getEscoreTotal(),
                ficha.getEscoreClassificacao(),
                ficha.getEscoreTom());
    }

    public static RespostaFichaResponseDto toResponse(RespostaFicha resposta) {
        return new RespostaFichaResponseDto(
                resposta.getId(),
                resposta.getCampo() == null ? null : resposta.getCampo().getId(),
                resposta.getSecao(),
                resposta.getRotulo(),
                resposta.getTipo(),
                OpcoesJson.paraLista(resposta.getOpcoes()),
                resposta.getOrdem(),
                resposta.getObrigatorio(),
                resposta.getPontos(),
                resposta.getGrupoEscore(),
                resposta.getValor());
    }

    private static List<RespostaFichaResponseDto> respostasOrdenadas(FichaAnamnese ficha) {
        return ficha.getRespostas().stream()
                .sorted(Comparator.comparing(RespostaFicha::getOrdem))
                .map(FichaAnamneseMapper::toResponse)
                .toList();
    }
}
