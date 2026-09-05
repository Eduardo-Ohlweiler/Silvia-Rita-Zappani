package com.nutri.hospitalar.uti.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BusinessException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import com.nutri.hospitalar.uti.calculo.Classificacao;
import com.nutri.hospitalar.uti.calculo.FormulaEnteralResolvida;
import com.nutri.hospitalar.uti.calculo.ModuloProteicoResolvido;
import com.nutri.hospitalar.uti.calculo.ResultadoUti;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiCreateDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiFiltrosDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiListaDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiResponseDto;
import com.nutri.hospitalar.uti.dtos.AvaliacaoUtiUpdateDto;
import com.nutri.hospitalar.uti.dtos.CalculoUtiRequestDto;
import com.nutri.hospitalar.uti.dtos.EncerramentoUtiDto;
import com.nutri.hospitalar.uti.entity.AvaliacaoUti;
import com.nutri.hospitalar.uti.entity.FormulaEnteral;
import com.nutri.hospitalar.uti.entity.ProdutoNutricional;
import com.nutri.hospitalar.uti.calculo.NecessidadeCalculator;
import com.nutri.hospitalar.uti.enums.PopulacaoReferencia;
import com.nutri.hospitalar.uti.mapper.AvaliacaoUtiMapper;
import com.nutri.hospitalar.uti.repository.AvaliacaoUtiRepository;
import com.nutri.hospitalar.uti.repository.RegistroDiarioUtiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * O CRUD das avaliações de UTI.
 *
 * <p><b>O servidor sempre recalcula ao gravar, e nunca ao ler.</b> São as duas
 * metades da mesma regra:
 *
 * <ul>
 *   <li><b>Ao gravar</b>, os resultados vêm do calculador a partir das entradas
 *       — nenhum número do cliente é aceito. Mandar {@code "imc": 999} no corpo
 *       não tem efeito.
 *   <li><b>Ao ler</b>, os resultados vêm das colunas. Se as equações mudarem
 *       amanhã, o registro de hoje continua mostrando o que se decidiu hoje.
 * </ul>
 *
 * <p>No eroERP "Calcular" e "Salvar" são o mesmo submit: clicar em Calcular
 * grava no banco. Aqui o recálculo é automático na tela e o Salvar só salva.
 *
 * <p><b>Nada de dado clínico em log</b> (regra 5 do {@code CLAUDE.md}): os
 * registros trazem o id da avaliação, e mais nada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvaliacaoUtiService {

    private final AvaliacaoUtiRepository avaliacaoUtiRepository;
    private final RegistroDiarioUtiRepository registroDiarioUtiRepository;
    private final PessoaRepository pessoaRepository;
    private final CalculoUtiService calculoUtiService;
    private final FormulaEnteralService formulaEnteralService;
    private final ProdutoNutricionalService produtoNutricionalService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<AvaliacaoUtiListaDto> getAll(Pageable pageable, AvaliacaoUtiFiltrosDto filtros) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return avaliacaoUtiRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId,
                        filtros.pacienteId(),
                        textoOuNulo(filtros.pacienteNome()),
                        filtros.de(),
                        filtros.ate())
                .map(AvaliacaoUtiService::toLista);
    }

    /**
     * Abrir uma avaliação salva <b>não recalcula</b>: todo número da resposta
     * sai das colunas gravadas.
     *
     * <p>O cálculo que roda aqui não contradiz isso — dele se aproveitam só as
     * frases que explicam um campo vazio. Sem ele, uma avaliação sem
     * circunferência do braço reabria com o bloco em branco e sem uma palavra.
     * Ver {@link AvaliacaoUtiMapper#toResultado}.
     */
    @Transactional(readOnly = true)
    public AvaliacaoUtiResponseDto findById(UUID id) {
        return paraResposta(buscar(id));
    }

    /**
     * Monta a resposta de uma avaliação já carregada — números do banco, frases
     * do recálculo.
     *
     * <p>Pública porque o painel do paciente também mostra a última avaliação
     * inteira, e montá-la lá por fora reintroduziria o traço mudo só naquela
     * tela.
     */
    @Transactional(readOnly = true)
    public AvaliacaoUtiResponseDto paraResposta(AvaliacaoUti avaliacao) {
        return AvaliacaoUtiMapper.toResponse(avaliacao, motivosDe(avaliacao));
    }

    @Transactional
    public AvaliacaoUtiResponseDto create(AvaliacaoUtiCreateDto dto) {
        AvaliacaoUti avaliacao = new AvaliacaoUti();
        avaliacao.setTenant(securityUtils.getTenantReference());
        avaliacao.setCreatedBy(securityUtils.getUsuarioLogado());

        ResultadoUti r = aplicar(avaliacao, dto.pacienteId(), dto.profissionalId(),
                dto.dataAvaliacao(), dto.calculo(), dto.observacao());

        AvaliacaoUti salva = avaliacaoUtiRepository.save(avaliacao);
        log.info("Avaliação de UTI criada id={}", salva.getId());
        return AvaliacaoUtiMapper.toResponse(salva, r);
    }

    @Transactional
    public AvaliacaoUtiResponseDto update(UUID id, AvaliacaoUtiUpdateDto dto) {
        AvaliacaoUti avaliacao = buscar(id);
        avaliacao.setUpdatedBy(securityUtils.getUsuarioLogado());

        ResultadoUti r = aplicar(avaliacao, dto.pacienteId(), dto.profissionalId(),
                dto.dataAvaliacao(), dto.calculo(), dto.observacao());

        log.info("Avaliação de UTI alterada id={}", id);
        return AvaliacaoUtiMapper.toResponse(avaliacaoUtiRepository.save(avaliacao), r);
    }

    /**
     * Encerra o acompanhamento — a saída da lista de trabalho.
     *
     * <p><b>Só isso.</b> Não trava o registro diário, não mexe no cálculo e não
     * apaga nada: corrigir um dia passado numa avaliação encerrada é legítimo, e
     * barrar isso puniria quem está consertando o registro.
     *
     * <p>A data não pode ser <b>anterior à própria avaliação</b> — um
     * acompanhamento que termina antes de começar não é dado, é erro de
     * digitação, e ele viraria dias negativos na estatística do serviço.
     */
    @Transactional
    public AvaliacaoUtiResponseDto encerrar(UUID id, EncerramentoUtiDto dto) {
        AvaliacaoUti avaliacao = buscar(id);

        if (dto.encerradoEm().isBefore(avaliacao.getDataAvaliacao()))
            throw new BusinessException(
                    ("O acompanhamento não pode terminar antes de começar: a avaliação é de %s "
                            + "e o encerramento está em %s.")
                            .formatted(avaliacao.getDataAvaliacao(), dto.encerradoEm()));

        avaliacao.setEncerradoEm(dto.encerradoEm());
        avaliacao.setMotivoEncerramento(dto.motivo());
        avaliacao.setObservacaoEncerramento(textoOuNulo(dto.observacao()));
        avaliacao.setUpdatedBy(securityUtils.getUsuarioLogado());

        // O motivo é dado clínico de desfecho: fica na coluna, não no log.
        log.info("Acompanhamento de UTI encerrado avaliacaoId={}", id);
        return paraResposta(avaliacaoUtiRepository.save(avaliacao));
    }

    /** Desfaz um encerramento feito por engano. */
    @Transactional
    public AvaliacaoUtiResponseDto reabrir(UUID id) {
        AvaliacaoUti avaliacao = buscar(id);

        avaliacao.setEncerradoEm(null);
        avaliacao.setMotivoEncerramento(null);
        avaliacao.setObservacaoEncerramento(null);
        avaliacao.setUpdatedBy(securityUtils.getUsuarioLogado());

        log.info("Acompanhamento de UTI reaberto avaliacaoId={}", id);
        return paraResposta(avaliacaoUtiRepository.save(avaliacao));
    }

    /**
     * Apagar avaliação com dias de acompanhamento vinculados é <b>recusado</b>.
     *
     * <p>A FK é {@code ON DELETE RESTRICT}, então o banco já barraria — mas
     * barraria com uma violação de constraint crua. Aqui o usuário recebe
     * quantos dias estão presos, que é o que ele precisa saber para decidir.
     */
    @Transactional
    public void remover(UUID id) {
        AvaliacaoUti avaliacao = buscar(id);

        long dias = registroDiarioUtiRepository.countByTenantIdAndAvaliacaoId(
                securityUtils.getTenantIdLogado(), id);

        if (dias > 0)
            throw new ConflictException(
                    ("Esta avaliação tem %d %s de acompanhamento vinculado%s. "
                            + "Desvincule ou remova %s antes de apagar a avaliação.")
                            .formatted(dias, dias == 1 ? "dia" : "dias", dias == 1 ? "" : "s",
                                    dias == 1 ? "o dia" : "os dias"));

        avaliacaoUtiRepository.delete(avaliacao);
        log.info("Avaliação de UTI removida id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Grava entradas e resultados.
     *
     * <p>O cálculo passa pelo <b>mesmo</b> {@link CalculoUtiService} que o
     * endpoint da calculadora usa — não há uma segunda implementação, e por isso
     * a tela nunca mostra um número diferente do que o banco guarda. Há teste
     * que compara os dois caminhos com o mesmo corpo.
     */
    /**
     * @return o cálculo recém-feito — a resposta o usa só pelo texto de
     *         ausência; os números ela lê da entidade já gravada
     */
    private ResultadoUti aplicar(AvaliacaoUti a, UUID pacienteId, UUID profissionalId,
                                 java.time.LocalDate data, CalculoUtiRequestDto calculo,
                                 String observacao) {

        a.setPaciente(buscarPessoa(pacienteId, "Paciente não encontrado"));
        a.setProfissional(profissionalId == null ? null
                : buscarPessoa(profissionalId, "Profissional não encontrado"));
        a.setDataAvaliacao(data);
        a.setObservacao(textoOuNulo(observacao));

        gravarEntradas(a, calculo);
        gravarRetratoDaFormula(a, calculo.formulaEnteralId());
        gravarRetratoDoModulo(a, calculo.moduloProteicoId());

        ResultadoUti r = calculoUtiService.calcular(calculo);
        gravarResultados(a, r);
        return r;
    }

    /**
     * Refaz o cálculo sobre as <b>entradas gravadas</b>, para colher dele só as
     * frases de ausência.
     *
     * <p>Usa o <b>retrato</b> da fórmula, não o catálogo de hoje: a fórmula pode
     * ter mudado, ou saído dele, e o motivo tem de explicar a avaliação como ela
     * foi feita.
     */
    private ResultadoUti motivosDe(AvaliacaoUti a) {
        return calculoUtiService.calcular(
                AvaliacaoUtiMapper.toEntradas(a), retratoDaFormula(a), retratoDoModulo(a));
    }

    /** O que ficou gravado do módulo proteico, na forma que o calculador consome. */
    private static ModuloProteicoResolvido retratoDoModulo(AvaliacaoUti a) {
        if (a.getModuloNome() == null) return null;
        return new ModuloProteicoResolvido(
                a.getModuloNome(), a.getModuloMedidaG(),
                a.getModuloProteinaPorMedidaG(), a.getModuloKcalPorMedida());
    }

    /** O que ficou gravado da fórmula, na forma que o calculador consome. */
    private static FormulaEnteralResolvida retratoDaFormula(AvaliacaoUti a) {
        if (a.getFormulaNome() == null) return null;
        return new FormulaEnteralResolvida(
                a.getFormulaNome(), a.getFormulaDensidadeKcalMl(), a.getFormulaProteinaGL(),
                a.getFormulaChoGL(), a.getFormulaLipGL(), a.getFormulaFibrasGL(),
                a.getFormulaPotassioMgL(), a.getFormulaAguaLivrePerc());
    }

    private void gravarEntradas(AvaliacaoUti a, CalculoUtiRequestDto c) {
        a.setSexo(c.sexo());
        a.setEtnia(c.etnia());
        a.setIdadeAnos(c.idadeAnos());
        a.setAlturaCm(c.alturaCm());
        a.setAlturaJoelhoCm(c.alturaJoelhoCm());
        a.setCircBracoCm(c.circBracoCm());
        a.setCircPanturrilhaCm(c.circPanturrilhaCm());
        a.setCircAbdominalCm(c.circAbdominalCm());
        a.setPesoAtualKg(c.pesoAtualKg());
        a.setPesoUsualKg(c.pesoUsualKg());
        a.setJanelaPerda(c.janelaPerda());

        /*
         * Grava a população EFETIVA, não a que veio no corpo.
         *
         * O cliente pode omitir o campo — e omite, sempre que o IMC está acima
         * de 18,5, porque aí a tela nem exibe o controle. Persistir o nulo faria
         * o registro perder QUAL coluna de ajuste produziu o diagnóstico de
         * depleção muscular, que é informação clínica: as duas divergem em
         * IMC < 18,5, e é justamente ali que o erro é falso-negativo
         * (docs/10 §2.9).
         *
         * O padrão é o mesmo que o calculador aplica — se um dia mudar, muda nos
         * dois, e há teste-espelho que compara os dois caminhos campo a campo.
         */
        a.setPopulacaoReferencia(c.populacaoReferencia() == null
                ? PopulacaoReferencia.POPULACAO_CLINICA : c.populacaoReferencia());

        a.setOrigemPesoPreferida(c.origemPesoPreferida());

        // Mesma razão da população acima: o padrão é gravado explicitamente. A
        // meta de 1360 kcal não se distingue da de 1190 sem saber que ponto da
        // faixa foi escolhido, e o padrão do sistema pode mudar amanhã.
        a.setPosicaoNaFaixa(NecessidadeCalculator.posicaoOuPadrao(c.posicaoNaFaixa()));

        // Substitui o conteúdo em vez de trocar a coleção: o Hibernate rastreia
        // a instância, e atribuir uma nova quebra o dirty checking da
        // @ElementCollection.
        a.getSegmentosAmputados().clear();
        if (c.segmentosAmputados() != null)
            a.getSegmentosAmputados().addAll(c.segmentosAmputados());

        a.setFase(c.fase());
        a.setTerapiaRenal(c.terapiaRenal());
        a.setKcalPorKgAlvo(c.kcalPorKgAlvo());
        a.setProteinaPorKgAlvo(c.proteinaPorKgAlvo());

        a.setModoInfusao(c.modoInfusao());
        a.setVolumePorTempo(c.volumePorTempo());
        a.setTempo(c.tempo());
        a.setVolumeDietaManualMl(c.volumeDietaManualMl());
    }

    /**
     * A composição da fórmula é copiada <b>inteira</b>, macros inclusive.
     *
     * <p>A FK é {@code ON DELETE SET NULL}: a fórmula pode sair do catálogo sem
     * levar junto o histórico clínico. Sem este retrato, a avaliação ficaria
     * ilegível — e o eroERP guarda só nome, densidade e proteína, o que a deixa
     * legível pela metade.
     */
    private void gravarRetratoDaFormula(AvaliacaoUti a, UUID formulaId) {
        if (formulaId == null) {
            a.setFormulaEnteral(null);
            a.setFormulaNome(null);
            a.setFormulaDensidadeKcalMl(null);
            a.setFormulaProteinaGL(null);
            a.setFormulaChoGL(null);
            a.setFormulaLipGL(null);
            a.setFormulaFibrasGL(null);
            a.setFormulaPotassioMgL(null);
            a.setFormulaAguaLivrePerc(null);
            return;
        }

        FormulaEnteral f = formulaEnteralService.buscarVisivel(formulaId);

        a.setFormulaEnteral(f);
        a.setFormulaNome(f.getNome());
        a.setFormulaDensidadeKcalMl(f.getDensidadeKcalMl());
        a.setFormulaProteinaGL(f.getProteinaGL());
        a.setFormulaChoGL(f.getChoGL());
        a.setFormulaLipGL(f.getLipGL());
        a.setFormulaFibrasGL(f.getFibrasGL());
        a.setFormulaPotassioMgL(f.getPotassioMgL());
        a.setFormulaAguaLivrePerc(f.getAguaLivrePerc());
    }

    /**
     * O mesmo para o módulo proteico — e só o que a conta usa.
     *
     * <p>Três números bastam porque a sugestão só depende deles: medida,
     * proteína por medida e <b>kcal por medida como o rótulo declara</b>. Não
     * há macro aqui de propósito; recompor a caloria macro a macro é o defeito
     * 10 da planilha, e sem a coluna ele não tem como voltar.
     */
    private void gravarRetratoDoModulo(AvaliacaoUti a, UUID moduloId) {
        if (moduloId == null) {
            a.setModuloProteico(null);
            a.setModuloNome(null);
            a.setModuloMedidaG(null);
            a.setModuloProteinaPorMedidaG(null);
            a.setModuloKcalPorMedida(null);
            return;
        }

        ProdutoNutricional p = produtoNutricionalService.buscarVisivel(moduloId);

        a.setModuloProteico(p);
        a.setModuloNome(p.getNome());
        a.setModuloMedidaG(p.getMedidaQtd());
        a.setModuloProteinaPorMedidaG(p.getProteinaG());
        a.setModuloKcalPorMedida(p.getKcal());
    }

    private void gravarResultados(AvaliacaoUti a, ResultadoUti r) {
        var antro = r.antropometria();
        a.setAlturaEstimadaCm(antro.alturaEstimadaCm());
        a.setPesoChumleaKg(antro.pesoChumleaKg());
        a.setPesoJungKg(antro.pesoJungKg());
        a.setPesoRabitoKg(antro.pesoRabitoKg());
        a.setPesoTrabalhoKg(antro.pesoDeTrabalhoKg());
        a.setPesoTrabalhoOrigem(antro.pesoDeTrabalhoOrigem());
        a.setAlturaUsadaCm(antro.alturaUsadaCm());
        a.setAlturaUsadaOrigem(antro.alturaUsadaOrigem());
        a.setImc(antro.imc());
        a.setClassifImcOms(rotulo(antro.classificacaoImcOms()));
        a.setClassifImcOmsTom(tom(antro.classificacaoImcOms()));
        a.setClassifImcOpas(rotulo(antro.classificacaoImcOpas()));
        a.setClassifImcOpasTom(tom(antro.classificacaoImcOpas()));
        a.setPesoIdealKg(antro.pesoIdealKg());
        a.setPesoIdealImc25Kg(antro.pesoIdealImc25Kg());
        a.setPesoAjustadoKg(antro.pesoAjustadoKg());
        a.setPesoAmputacaoKg(antro.pesoCorrigidoAmputacaoKg());
        a.setPercPerdaPeso(antro.percentualPerdaPeso());
        a.setClassifPerdaPeso(rotulo(antro.classificacaoPerdaPeso()));
        a.setClassifPerdaPesoTom(tom(antro.classificacaoPerdaPeso()));
        a.setP50CircBracoCm(antro.p50CircBracoCm());
        a.setAdequacaoCircBracoPerc(antro.adequacaoCircBracoPerc());
        a.setClassifAdequacaoCb(rotulo(antro.classificacaoAdequacaoCircBraco()));
        a.setClassifAdequacaoCbTom(tom(antro.classificacaoAdequacaoCircBraco()));
        a.setCircBracoAjustadaCm(antro.circBracoAjustadaCm());
        a.setClassifMassaBraco(rotulo(antro.classificacaoMassaMuscularBraco()));
        a.setClassifMassaBracoTom(tom(antro.classificacaoMassaMuscularBraco()));
        a.setCircPanturrilhaAjustadaCm(antro.circPanturrilhaAjustadaCm());
        a.setClassifDeplecaoCp(rotulo(antro.classificacaoDeplecaoPanturrilha()));
        a.setClassifDeplecaoCpTom(tom(antro.classificacaoDeplecaoPanturrilha()));

        var nec = r.necessidades();
        a.setEnergiaMinima(nec.energiaMinima());
        a.setEnergiaMaxima(nec.energiaMaxima());
        a.setProteinaMinima(nec.proteinaMinima());
        a.setProteinaMaxima(nec.proteinaMaxima());
        a.setMetaEnergetica(nec.metaEnergetica());
        a.setMetaEnergeticaOrigem(nec.metaEnergeticaOrigem());
        a.setMetaProteica(nec.metaProteica());
        a.setMetaProteicaOrigem(nec.metaProteicaOrigem());
        a.setProteinaTerapiaRenal(nec.proteinaTerapiaRenal());
        a.setObeso(nec.obeso());
        a.setBaseDoPeso(nec.baseDoPeso());

        var dieta = r.dieta();
        a.setVolumeTotalMl(dieta.volumeTotalMl());
        a.setCaloriasOfertadas(dieta.caloriasOfertadas());
        a.setProteinaOfertada(dieta.proteinaOfertada());
        a.setCaloriasPorQuilo(dieta.caloriasPorQuilo());
        a.setProteinaPorQuilo(dieta.proteinaPorQuilo());
        a.setPercentualDoVct(dieta.percentualDoVct());
        a.setPercentualDaProteina(dieta.percentualDaProteina());
        a.setChoOfertado(dieta.choOfertado());
        a.setLipOfertado(dieta.lipOfertado());
        a.setFibrasOfertadas(dieta.fibrasOfertadas());
        a.setPotassioOfertado(dieta.potassioOfertado());
        a.setVolumePleno(dieta.volumePleno());
        a.setProteinaNoVolumePleno(dieta.proteinaNoVolumePleno());
        a.setProteinaSuplementar(dieta.proteinaSuplementar());
        a.setModuloGramas(dieta.moduloGramas());
        a.setModuloMedidas(dieta.moduloMedidas());
        a.setModuloKcal(dieta.moduloKcal());

        var hidra = r.hidratacao();
        a.setHidratacaoNecessidadeMinima(hidra.necessidadeMinima());
        a.setHidratacaoNecessidadeIdeal(hidra.necessidadeIdeal());
        a.setHidratacaoPercentualAgua(hidra.percentualAgua());
        a.setHidratacaoPercentualAguaOrigem(hidra.percentualAguaOrigem());
        a.setHidratacaoAguaNaDieta(hidra.aguaNaDieta());
        a.setHidratacaoAguaExtraMinima(hidra.aguaExtraMinima());
        a.setHidratacaoAguaExtraIdeal(hidra.aguaExtraIdeal());
    }

    private static AvaliacaoUtiListaDto toLista(AvaliacaoUti a) {
        return new AvaliacaoUtiListaDto(
                a.getId(),
                a.getPaciente().getNome(),
                a.getProfissional() == null ? null : a.getProfissional().getNome(),
                a.getDataAvaliacao(),
                a.getPesoTrabalhoKg(),
                a.getPesoTrabalhoOrigem(),
                a.getImc(),
                a.getClassifImcOms(),
                a.getClassifImcOmsTom(),
                a.getMetaEnergetica(),
                a.getFormulaNome());
    }

    private static String rotulo(Classificacao c) {
        return c == null ? null : c.rotulo();
    }

    private static String tom(Classificacao c) {
        return c == null ? null : c.tom().name();
    }

    private AvaliacaoUti buscar(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return avaliacaoUtiRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Avaliação não encontrada"));
    }

    private Pessoa buscarPessoa(UUID id, String mensagem) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException(mensagem));
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
