package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import com.nutri.hospitalar.uti.dtos.AvaliacaoSugeridaDto;
import com.nutri.hospitalar.uti.dtos.MedidasDoDia;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiCreateDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiListaDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiResponseDto;
import com.nutri.hospitalar.uti.dtos.RegistroDiarioUtiUpdateDto;
import com.nutri.hospitalar.uti.entity.AvaliacaoUti;
import com.nutri.hospitalar.uti.entity.RegistroDiarioUti;
import com.nutri.hospitalar.uti.mapper.RegistroDiarioUtiMapper;
import com.nutri.hospitalar.uti.repository.AvaliacaoUtiRepository;
import com.nutri.hospitalar.uti.repository.RegistroDiarioUtiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * O acompanhamento diário na UTI.
 *
 * <p><b>Um registro por paciente por dia.</b> A {@code UNIQUE} do banco garante,
 * e este service a antecipa com uma mensagem que diz qual dia — o eroERP não tem
 * a restrição, então dois registros do mesmo dia coexistem e o painel plota os
 * dois.
 *
 * <p><b>O vínculo com a avaliação nunca acontece em silêncio.</b> A tela pede a
 * sugestão em {@link #avaliacaoSugerida} e manda o que o usuário confirmar.
 * Ligar sozinho faria o kcal/kg mudar sem que ninguém tivesse escolhido a
 * referência.
 *
 * <p><b>Nada de dado clínico em log</b> (regra 5): só o id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistroDiarioUtiService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String SEM_AVALIACAO_ATE_A_DATA =
            "Este paciente ainda não tem avaliação até esta data. O dia pode ser registrado "
                    + "assim mesmo — kcal/kg e diurese por quilo ficam de fora.";

    private final RegistroDiarioUtiRepository registroDiarioUtiRepository;
    private final AvaliacaoUtiRepository avaliacaoUtiRepository;
    private final PessoaRepository pessoaRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<RegistroDiarioUtiListaDto> getAll(Pageable pageable, UUID pessoaId,
                                                  String pessoaNome, LocalDate de, LocalDate ate) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return registroDiarioUtiRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, pessoaId, textoOuNulo(pessoaNome), de, ate)
                .map(RegistroDiarioUtiMapper::toLista);
    }

    @Transactional(readOnly = true)
    public RegistroDiarioUtiResponseDto findById(UUID id) {
        return RegistroDiarioUtiMapper.toResponse(buscar(id));
    }

    /**
     * Qual avaliação o dia deveria referenciar — <b>sugestão</b>, não vínculo.
     *
     * <p>A mais recente daquele paciente <b>até</b> aquela data: um dia de três
     * meses atrás não deve ser comparado com a prescrição de ontem.
     */
    @Transactional(readOnly = true)
    public AvaliacaoSugeridaDto avaliacaoSugerida(UUID pessoaId, LocalDate data) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return registroDiarioUtiRepository.findIdDaAvaliacaoVigente(tenantId, pessoaId, data)
                .flatMap(id -> avaliacaoUtiRepository.findByIdAndTenantId(id, tenantId))
                .map(a -> new AvaliacaoSugeridaDto(
                        a.getId(), a.getDataAvaliacao(), a.getPesoTrabalhoKg(),
                        a.getMetaEnergetica(), a.getVolumeTotalMl(), a.getFormulaNome(), null))
                .orElseGet(() -> new AvaliacaoSugeridaDto(
                        null, null, null, null, null, null, SEM_AVALIACAO_ATE_A_DATA));
    }

    @Transactional
    public RegistroDiarioUtiResponseDto create(RegistroDiarioUtiCreateDto dto) {
        recusarDiaRepetido(dto.pessoaId(), dto.data(), null);

        RegistroDiarioUti registro = new RegistroDiarioUti();
        registro.setTenant(securityUtils.getTenantReference());
        registro.setCreatedBy(securityUtils.getUsuarioLogado());
        registro.setPessoa(buscarPessoa(dto.pessoaId()));
        registro.setData(dto.data());
        registro.setAvaliacao(resolverAvaliacao(dto.avaliacaoId()));

        aplicarMedidas(registro, dto);

        RegistroDiarioUti salvo = registroDiarioUtiRepository.save(registro);
        log.info("Registro diário de UTI criado id={}", salvo.getId());
        return RegistroDiarioUtiMapper.toResponse(salvo);
    }

    @Transactional
    public RegistroDiarioUtiResponseDto update(UUID id, RegistroDiarioUtiUpdateDto dto) {
        RegistroDiarioUti registro = buscar(id);
        recusarDiaRepetido(dto.pessoaId(), dto.data(), id);

        registro.setUpdatedBy(securityUtils.getUsuarioLogado());
        registro.setPessoa(buscarPessoa(dto.pessoaId()));
        registro.setData(dto.data());
        registro.setAvaliacao(resolverAvaliacao(dto.avaliacaoId()));

        aplicarMedidas(registro, dto);

        log.info("Registro diário de UTI alterado id={}", id);
        return RegistroDiarioUtiMapper.toResponse(registroDiarioUtiRepository.save(registro));
    }

    @Transactional
    public void remover(UUID id) {
        registroDiarioUtiRepository.delete(buscar(id));
        log.info("Registro diário de UTI removido id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Dois registros do mesmo paciente no mesmo dia não podem coexistir.
     *
     * <p>A {@code UNIQUE} do banco é a garantia real; isto existe para o usuário
     * receber "já existe um registro em 15/03/2026" em vez de uma violação de
     * constraint crua.
     */
    private void recusarDiaRepetido(UUID pessoaId, LocalDate data, UUID idAtual) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        registroDiarioUtiRepository
                .findByTenantIdAndPessoaIdAndData(tenantId, pessoaId, data)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new ConflictException(
                            "Já existe um acompanhamento deste paciente em %s. Abra o registro "
                                    + "existente em vez de criar outro.".formatted(data.format(DIA)));
                });
    }

    /** A avaliação, sempre dentro do tenant. Nulo é resposta válida. */
    private AvaliacaoUti resolverAvaliacao(UUID avaliacaoId) {
        if (avaliacaoId == null) return null;

        UUID tenantId = securityUtils.getTenantIdLogado();
        return avaliacaoUtiRepository.findByIdAndTenantId(avaliacaoId, tenantId)
                .orElseThrow(() -> new NotFoundException("Avaliação não encontrada"));
    }

    /** Uma implementação só: os dois DTOs expõem as mesmas medidas. */
    private void aplicarMedidas(RegistroDiarioUti r, MedidasDoDia d) {
        r.setDieta(textoOuNulo(d.dieta()));
        r.setVolPrescrito24h(d.volPrescrito24h());
        r.setVolRecebido24h(d.volRecebido24h());

        r.setMg(d.mg());
        r.setK(d.k());
        r.setNa(d.na());
        r.setLactato(d.lactato());
        r.setPcr(d.pcr());
        r.setPh(d.ph());
        r.setPco2(d.pco2());
        r.setHco3(d.hco3());
        r.setHgt(d.hgt());

        r.setSuporteVentilatorio(d.suporteVentilatorio());
        r.setFio2Perc(d.fio2Perc());
        r.setPaSistolica(d.paSistolica());
        r.setPaDiastolica(d.paDiastolica());
        r.setBalancoHidricoMl(d.balancoHidricoMl());
        r.setDiureseMl(d.diureseMl());
        r.setEvacuacao(textoOuNulo(d.evacuacao()));

        r.setCafeManha(d.cafeManha());
        r.setLancheManha(d.lancheManha());
        r.setAlmoco(d.almoco());
        r.setLancheTarde(d.lancheTarde());
        r.setJantar(d.jantar());
        r.setCeia(d.ceia());

        r.setObservacao(textoOuNulo(d.observacao()));
    }

    private RegistroDiarioUti buscar(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return registroDiarioUtiRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Acompanhamento não encontrado"));
    }

    private Pessoa buscarPessoa(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado"));
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
