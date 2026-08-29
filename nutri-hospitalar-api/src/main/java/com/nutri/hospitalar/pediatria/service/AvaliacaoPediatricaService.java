package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pediatria.calculo.ResultadoPediatrico;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaCreateDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaListaDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaResponseDto;
import com.nutri.hospitalar.pediatria.dtos.AvaliacaoPediatricaUpdateDto;
import com.nutri.hospitalar.pediatria.dtos.CalculoPediatricoRequestDto;
import com.nutri.hospitalar.pediatria.dtos.ResultadoPediatricoDto;
import com.nutri.hospitalar.pediatria.entity.AvaliacaoPediatrica;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import com.nutri.hospitalar.pediatria.mapper.AvaliacaoPediatricaMapper;
import com.nutri.hospitalar.pediatria.mapper.ResultadoPediatricoMapper;
import com.nutri.hospitalar.pediatria.repository.AvaliacaoPediatricaRepository;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Avaliação pediátrica.
 *
 * <p><b>Nenhum resultado vem do cliente.</b> Os DTOs de escrita só carregam
 * entradas, e o service recalcula antes de gravar. É a diferença central para o
 * eroERP, onde o front calculava e o backend gravava o que recebesse.
 *
 * <p>Nada de dado clínico em log — peso, IMC, classificação e prescrição são
 * dados de saúde sob a LGPD. Os logs levam id e nada mais.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvaliacaoPediatricaService {

    private final AvaliacaoPediatricaRepository avaliacaoRepository;
    private final PessoaRepository pessoaRepository;
    private final FormulaLacteaService formulaLacteaService;
    private final CalculoPediatricoService calculoService;
    private final SecurityUtils securityUtils;

    /** Cálculo avulso: não persiste nada, e é o que a tela chama a cada alteração. */
    @Transactional(readOnly = true)
    public ResultadoPediatricoDto calcular(CalculoPediatricoRequestDto dto) {
        FormulaLactea formula = buscarFormula(dto.formulaLacteaId());

        ResultadoPediatrico resultado = calculoService.calcular(
                dto.sexo(), dto.idadeMeses(), dto.peso(), dto.estatura(),
                formula, dto.volumeMl(), dto.frequenciaHoras());

        return ResultadoPediatricoMapper.toResponse(resultado);
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoPediatricaListaDto> getAll(Pageable pageable, UUID pacienteId,
                                                    UUID formulaLacteaId, LocalDate de,
                                                    LocalDate ate, Integer mesesMin,
                                                    Integer mesesMax) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return avaliacaoRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, pacienteId, formulaLacteaId, de, ate, mesesMin, mesesMax)
                .map(AvaliacaoPediatricaMapper::toLista);
    }

    /**
     * Abre uma avaliação salva — <b>sem recalcular</b>. Mostra o que foi
     * gravado, ainda que as curvas ou as DRIs tenham mudado desde então.
     *
     * <p>O cálculo que roda aqui não contradiz isso: dele se aproveitam
     * <b>só os motivos de ausência</b>, e nenhum número. Sem isso a avaliação de
     * uma criança fora da faixa das DRIs reabre com quatro traços mudos.
     */
    @Transactional(readOnly = true)
    public AvaliacaoPediatricaResponseDto findById(UUID id) {
        AvaliacaoPediatrica avaliacao = buscar(id);
        return AvaliacaoPediatricaMapper.toResponse(avaliacao, calculoService.motivosDe(avaliacao));
    }

    @Transactional
    public AvaliacaoPediatricaResponseDto create(AvaliacaoPediatricaCreateDto dto) {
        AvaliacaoPediatrica avaliacao = new AvaliacaoPediatrica();
        avaliacao.setTenant(securityUtils.getTenantReference());
        avaliacao.setCreatedBy(securityUtils.getUsuarioLogado());

        ResultadoPediatrico r = aplicar(avaliacao,
                dto.pacienteId(), dto.profissionalId(), dto.dataAvaliacao(),
                dto.sexo(), dto.idadeMeses(), dto.peso(), dto.estatura(),
                dto.formulaLacteaId(), dto.volumeMl(), dto.frequenciaHoras(), dto.observacao());

        AvaliacaoPediatrica salva = avaliacaoRepository.save(avaliacao);
        log.info("Avaliação pediátrica criada id={}", salva.getId());
        return AvaliacaoPediatricaMapper.toResponse(salva, r);
    }

    @Transactional
    public AvaliacaoPediatricaResponseDto update(UUID id, AvaliacaoPediatricaUpdateDto dto) {
        AvaliacaoPediatrica avaliacao = buscar(id);
        avaliacao.setUpdatedBy(securityUtils.getUsuarioLogado());

        ResultadoPediatrico r = aplicar(avaliacao,
                dto.pacienteId(), dto.profissionalId(), dto.dataAvaliacao(),
                dto.sexo(), dto.idadeMeses(), dto.peso(), dto.estatura(),
                dto.formulaLacteaId(), dto.volumeMl(), dto.frequenciaHoras(), dto.observacao());

        log.info("Avaliação pediátrica alterada id={}", id);
        return AvaliacaoPediatricaMapper.toResponse(avaliacaoRepository.save(avaliacao), r);
    }

    @Transactional
    public void delete(UUID id) {
        AvaliacaoPediatrica avaliacao = buscar(id);
        avaliacaoRepository.delete(avaliacao);
        log.info("Avaliação pediátrica excluída id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Grava as entradas, <b>recalcula</b> e grava o resultado do próprio
     * cálculo. Serve create e update: o caminho é o mesmo, e ter um só evita
     * que a alteração deixe de recalcular por esquecimento.
     *
     * @return o cálculo recém-feito — a resposta o usa só pelos motivos de
     *         ausência; os números ela lê da entidade já gravada
     */
    private ResultadoPediatrico aplicar(AvaliacaoPediatrica avaliacao,
                         UUID pacienteId, UUID profissionalId, LocalDate dataAvaliacao,
                         Sexo sexo, Integer idadeMeses,
                         BigDecimal peso, BigDecimal estatura,
                         UUID formulaLacteaId, BigDecimal volumeMl,
                         BigDecimal frequenciaHoras, String observacao) {

        avaliacao.setPaciente(buscarPessoa(pacienteId, "Paciente não encontrado"));
        avaliacao.setProfissional(profissionalId == null ? null
                : buscarPessoa(profissionalId, "Profissional não encontrado"));

        avaliacao.setDataAvaliacao(dataAvaliacao);
        avaliacao.setSexo(sexo);
        avaliacao.setIdadeMeses(idadeMeses);
        avaliacao.setPeso(peso);
        avaliacao.setEstatura(estatura);
        avaliacao.setVolumeMl(volumeMl);
        avaliacao.setFrequenciaHoras(frequenciaHoras);
        avaliacao.setObservacao(observacao);

        // Retrato da fórmula: a avaliação não pode depender de o catálogo
        // continuar igual amanhã.
        FormulaLactea formula = buscarFormula(formulaLacteaId);
        avaliacao.setFormulaLactea(formula);
        avaliacao.setFormulaNome(formula == null ? null : formula.getNome());
        avaliacao.setFormulaKcalPor100ml(formula == null ? null : formula.getKcalPor100ml());
        avaliacao.setFormulaProteinaPor100ml(formula == null ? null : formula.getProteinaPor100ml());

        ResultadoPediatrico r = calculoService.calcular(
                sexo, idadeMeses, peso, estatura, formula, volumeMl, frequenciaHoras);
        aplicarResultado(avaliacao, r);
        return r;
    }


    private void aplicarResultado(AvaliacaoPediatrica avaliacao, ResultadoPediatrico r) {
        avaliacao.setImc(r.imc());
        avaliacao.setClassifPesoIdade(r.pesoIdade());
        avaliacao.setClassifEstaturaIdade(r.estaturaIdade());
        avaliacao.setClassifImcIdade(r.imcIdade());

        avaliacao.setVet(r.vet());
        avaliacao.setProteinaNecessidade(r.proteinaNecessidade());

        avaliacao.setVezesDia(r.vezesDia());
        avaliacao.setVolumeTotal(r.volumeTotal());
        avaliacao.setCaloriasTotais(r.caloriasTotais());
        avaliacao.setProteinaTotal(r.proteinaTotal());
        avaliacao.setPercCalorico(r.percCalorico());
        avaliacao.setPercProteico(r.percProteico());
    }

    private AvaliacaoPediatrica buscar(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return avaliacaoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Avaliação pediátrica não encontrada"));
    }

    /** Paciente e profissional são pessoas do próprio tenant — nunca de outro. */
    private Pessoa buscarPessoa(UUID id, String mensagem) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException(mensagem));
    }

    private FormulaLactea buscarFormula(UUID formulaLacteaId) {
        return formulaLacteaId == null ? null : formulaLacteaService.buscarVisivel(formulaLacteaId);
    }
}
