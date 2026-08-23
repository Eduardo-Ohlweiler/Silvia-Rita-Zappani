package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.calculo.ResultadoFerramentas;
import com.nutri.hospitalar.uti.dtos.FerramentasClinicasRequestDto;
import com.nutri.hospitalar.uti.service.FerramentasClinicasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * As quatro ferramentas clínicas, sem persistir nada.
 *
 * <p>Mesma razão de {@link CalculoUtiController} para devolver o record do
 * módulo de cálculo em vez de um DTO espelho: {@link ResultadoFerramentas} é
 * record puro, sem JPA, já com rótulo e tom prontos para exibição.
 */
@RestController
@RequestMapping("/uti/ferramentas-clinicas")
@RequiredArgsConstructor
@Tag(name = "Ferramentas clínicas")
@PreAuthorize("isAuthenticated()")
public class FerramentasClinicasController {

    private final FerramentasClinicasService ferramentasClinicasService;

    @PostMapping
    @Operation(summary = "Calcula as quatro ferramentas, sem gravar",
            description = """
                    Dose de noradrenalina, balanço nitrogenado, calorias do
                    propofol e receita da dieta artesanal.

                    As quatro são independentes — não há cascata entre elas — e
                    vêm num corpo só porque a tela recalcula tudo com um debounce
                    só.

                    **Noradrenalina:** os presets "32" e "64" da planilha são
                    2 e 4 ampolas em 250 ml da mesma fórmula, e a resposta traz a
                    **concentração resultante** para conferência à beira do leito.
                    Preparo diferente é `AMPOLAS_E_SORO`, com os números do
                    serviço.

                    **Propofol:** as horas de infusão são campo, com 24 como
                    padrão. Na planilha são constante escondida na fórmula.

                    **Dieta artesanal:** os insumos vêm do catálogo
                    (`GET /produtos-nutricionais/insumos-artesanais?papel=...`),
                    não de constante no código. Os quatro papéis são fixos; quem
                    os ocupa é o cadastro.
                    """)
    public ResponseEntity<ResultadoFerramentas> calcular(
            @Valid @RequestBody FerramentasClinicasRequestDto dto) {
        return ResponseEntity.ok(ferramentasClinicasService.calcular(dto));
    }
}
