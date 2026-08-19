package com.nutri.hospitalar.pediatria.mapper;

import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaResponseDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaSelectDto;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;

public final class FormulaLacteaMapper {

    private FormulaLacteaMapper() {}

    public static FormulaLacteaResponseDto toResponse(FormulaLactea formula) {
        return new FormulaLacteaResponseDto(
                formula.getId(),
                formula.getNome(),
                formula.getKcalPor100ml(),
                formula.getProteinaPor100ml(),
                formula.getAtivo(),
                formula.ehGlobal(),
                formula.getCreatedAt(),
                formula.getUpdatedAt());
    }

    public static FormulaLacteaSelectDto toSelect(FormulaLactea formula) {
        return new FormulaLacteaSelectDto(
                formula.getId(),
                formula.getNome(),
                formula.getKcalPor100ml(),
                formula.getProteinaPor100ml(),
                formula.ehGlobal());
    }
}
