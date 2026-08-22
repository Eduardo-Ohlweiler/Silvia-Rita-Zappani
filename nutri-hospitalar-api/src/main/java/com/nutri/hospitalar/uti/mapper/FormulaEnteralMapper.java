package com.nutri.hospitalar.uti.mapper;

import com.nutri.hospitalar.uti.dtos.FormulaEnteralResponseDto;
import com.nutri.hospitalar.uti.dtos.FormulaEnteralSelectDto;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import com.nutri.hospitalar.uti.enums.CategoriaFormulaEnteral;

public final class FormulaEnteralMapper {

    private FormulaEnteralMapper() {}

    public static FormulaEnteralResponseDto toResponse(FormulaEnteral formula) {
        return new FormulaEnteralResponseDto(
                formula.getId(),
                formula.getNome(),
                formula.getCategoria(),
                descricao(formula.getCategoria()),
                formula.getDensidadeKcalMl(),
                formula.getProteinaGL(),
                formula.getChoGL(),
                formula.getLipGL(),
                formula.getFibrasGL(),
                formula.getPotassioMgL(),
                formula.getOsmolaridadeMosmL(),
                formula.getAguaLivrePerc(),
                formula.getAtivo(),
                formula.ehGlobal(),
                formula.getCreatedAt(),
                formula.getUpdatedAt());
    }

    public static FormulaEnteralSelectDto toSelect(FormulaEnteral formula) {
        return new FormulaEnteralSelectDto(
                formula.getId(),
                formula.getNome(),
                descricao(formula.getCategoria()),
                formula.getDensidadeKcalMl(),
                formula.getProteinaGL(),
                formula.getChoGL(),
                formula.getLipGL(),
                formula.getFibrasGL(),
                formula.getPotassioMgL(),
                formula.getAguaLivrePerc(),
                formula.ehGlobal());
    }

    /** Categoria é anulável: produto sem categoria declarada é utilizável. */
    private static String descricao(CategoriaFormulaEnteral categoria) {
        return categoria == null ? null : categoria.getDescricao();
    }
}
