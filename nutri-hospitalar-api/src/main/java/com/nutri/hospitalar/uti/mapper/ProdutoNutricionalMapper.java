package com.nutri.hospitalar.uti.mapper;

import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalResponseDto;
import com.nutri.hospitalar.uti.dtos.ProdutoNutricionalSelectDto;
import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.enums.PapelArtesanal;

public final class ProdutoNutricionalMapper {

    private ProdutoNutricionalMapper() {}

    public static ProdutoNutricionalResponseDto toResponse(ProdutoNutricional produto) {
        return new ProdutoNutricionalResponseDto(
                produto.getId(),
                produto.getNome(),
                produto.getTipo(),
                produto.getTipo().getDescricao(),
                produto.getMedidaNome(),
                produto.getMedidaQtd(),
                produto.getEmbalagemQtd(),
                produto.getKcal(),
                produto.getProteinaG(),
                produto.getChoG(),
                produto.getAcucarG(),
                produto.getLipG(),
                produto.getSodioMg(),
                produto.getPotassioMg(),
                produto.getFosforoMg(),
                produto.getFerroMg(),
                produto.getFibrasG(),
                produto.getOsmolaridadeMosmL(),
                produto.getModuloProteico(),
                produto.getPapelArtesanal(),
                descricao(produto.getPapelArtesanal()),
                produto.getObservacao(),
                produto.getAtivo(),
                produto.ehGlobal(),
                produto.getCreatedAt(),
                produto.getUpdatedAt());
    }

    public static ProdutoNutricionalSelectDto toSelect(ProdutoNutricional produto) {
        return new ProdutoNutricionalSelectDto(
                produto.getId(),
                produto.getNome(),
                produto.getTipo().getDescricao(),
                produto.getMedidaNome(),
                produto.getMedidaQtd(),
                produto.getEmbalagemQtd(),
                produto.getKcal(),
                produto.getProteinaG(),
                produto.getChoG(),
                produto.getLipG(),
                produto.getPapelArtesanal(),
                produto.ehGlobal());
    }

    /** Papel só existe em insumo artesanal — nos outros dois é ausência legítima. */
    private static String descricao(PapelArtesanal papel) {
        return papel == null ? null : papel.getDescricao();
    }
}
