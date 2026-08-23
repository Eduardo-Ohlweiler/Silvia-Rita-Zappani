package com.nutri.hospitalar.uti.controller;

import com.nutri.hospitalar.uti.calculo.ResultadoUti;
import com.nutri.hospitalar.uti.dtos.CalculoUtiRequestDto;
import com.nutri.hospitalar.uti.service.CalculoUtiService;
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
 * O cálculo da terapia nutricional de UTI adulto, sem persistir.
 *
 * <p><b>Sobre o tipo de retorno:</b> devolvemos {@link ResultadoUti}, que é o
 * próprio record do módulo de cálculo, e não um DTO espelho. A regra do
 * {@code docs/03} §3 proíbe expor <i>Entity</i> — e {@code ResultadoUti} não é
 * entidade: é record puro, sem JPA, sem lazy loading e já com os rótulos de
 * classificação prontos para exibição. Na pediatria o DTO existe porque o
 * mapper <b>acrescenta</b> o rótulo ao enum {@code FaixaOms}; aqui
 * {@link com.nutri.hospitalar.uti.calculo.Classificacao} já carrega rótulo e
 * tom, então um espelho seria duplicação sem ganho — e mais uma superfície onde
 * um campo pode ficar para trás.
 */
@RestController
@RequestMapping("/uti/calculo")
@RequiredArgsConstructor
@Tag(name = "Cálculo da UTI adulto")
@PreAuthorize("isAuthenticated()")
public class CalculoUtiController {

    private final CalculoUtiService calculoUtiService;

    @PostMapping
    @Operation(summary = "Calcula sem gravar",
            description = """
                    Recebe as entradas das quatro abas e devolve os resultados
                    das quatro. É o endpoint que a tela chama a cada alteração de
                    campo, com 500 ms de debounce.

                    **Só entradas são aceitas.** Mandar um resultado no corpo não
                    tem efeito: o servidor recalcula tudo.

                    Todo campo é opcional, e o que não dá para calcular volta como
                    ausência **com o motivo** — nunca um traço mudo.

                    O peso, a altura e as metas vêm com a **origem por escrito**
                    ("peso estimado · Rabito 2008", "protocolo de obesidade"):
                    prescrição sem procedência não é auditável.

                    Segmentos amputados que se contêm — "Membro superior" e "Mão"
                    juntos — devolvem **422** nomeando os dois, em vez de um peso
                    silenciosamente menor que o real.
                    """)
    public ResponseEntity<ResultadoUti> calcular(@Valid @RequestBody CalculoUtiRequestDto dto) {
        return ResponseEntity.ok(calculoUtiService.calcular(dto));
    }
}
