package com.nutri.hospitalar.clinica.mapper;

import com.nutri.hospitalar.clinica.dtos.CampoFichaResponseDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaResponseDto;
import com.nutri.hospitalar.clinica.dtos.ModeloFichaSelectDto;
import com.nutri.hospitalar.clinica.entity.CampoFicha;
import com.nutri.hospitalar.clinica.entity.ModeloFicha;
import com.nutri.hospitalar.clinica.escore.EscalaNutricional;
import com.nutri.hospitalar.clinica.escore.EscalaRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ModeloFichaMapper {

    private ModeloFichaMapper() {}

    public static ModeloFichaResponseDto toResponse(ModeloFicha modelo) {
        return new ModeloFichaResponseDto(
                modelo.getId(),
                modelo.getNome(),
                modelo.getDescricao(),
                modelo.getAtivo(),
                modelo.ehGlobal(),
                modelo.getEscoreCodigo(),
                /* A tela nunca monta citação bibliográfica à mão: o nome e a
                   procedência do instrumento vêm de quem os conhece. Foi assim
                   que a legenda da panturrilha passou a mentir — literal cravado
                   na tela ao lado de um dado que o servidor já mandava. */
                escala(modelo).map(EscalaNutricional::nome).orElse(null),
                escala(modelo).map(EscalaNutricional::referencia).orElse(null),
                camposOrdenados(modelo),
                modelo.getCreatedAt(),
                modelo.getUpdatedAt());
    }

    public static ModeloFichaSelectDto toSelect(ModeloFicha modelo) {
        return new ModeloFichaSelectDto(
                modelo.getId(),
                modelo.getNome(),
                modelo.getDescricao(),
                modelo.ehGlobal(),
                modelo.getEscoreCodigo(),
                (int) modelo.getCampos().stream().filter(CampoFicha::getAtivo).count());
    }

    public static CampoFichaResponseDto toResponse(CampoFicha campo) {
        return new CampoFichaResponseDto(
                campo.getId(),
                campo.getSecao(),
                campo.getRotulo(),
                campo.getTipo(),
                OpcoesJson.paraLista(campo.getOpcoes()),
                PontosJson.paraLista(campo.getPontos()),
                campo.getGrupoEscore(),
                campo.getOrdem(),
                campo.getObrigatorio(),
                campo.getAtivo());
    }

    /**
     * O {@code @OrderBy} da entidade só vale no carregamento da coleção; um
     * modelo recém-montado na memória traz os campos na ordem em que foram
     * adicionados. Ordenar aqui faz a resposta ser a mesma nos dois caminhos.
     */
    private static Optional<EscalaNutricional> escala(ModeloFicha modelo) {
        return EscalaRegistry.de(modelo.getEscoreCodigo());
    }

    private static List<CampoFichaResponseDto> camposOrdenados(ModeloFicha modelo) {
        return modelo.getCampos().stream()
                .sorted(Comparator.comparing(CampoFicha::getOrdem))
                .map(ModeloFichaMapper::toResponse)
                .toList();
    }
}
