package com.nutri.hospitalar.pediatria.service;

import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaCreateDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaResponseDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaSelectDto;
import com.nutri.hospitalar.pediatria.dtos.FormulaLacteaUpdateDto;
import com.nutri.hospitalar.pediatria.entity.FormulaLactea;
import com.nutri.hospitalar.pediatria.mapper.FormulaLacteaMapper;
import com.nutri.hospitalar.pediatria.repository.FormulaLacteaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.UUID;

/**
 * Catálogo de fórmulas lácteas.
 *
 * <p><b>Ler alcança as globais; escrever, não.</b> As 10 fórmulas do sistema
 * vêm da planilha e são produtos de mercado — a composição do NAN 2 é a mesma
 * em qualquer hospital. Todo tenant as enxerga; nenhum as altera.
 *
 * <p>A recusa é explícita ({@link BadRequestException}) e não um 404 seco: o
 * usuário precisa entender que a fórmula existe e é do sistema, não que ela
 * sumiu.
 *
 * <p><b>O cadastro recusa composição implausível</b>, como o de fórmula enteral
 * já fazia. Aqui a conta é outra: a fórmula enteral declara os três macros e
 * fecha por Atwater; a láctea declara só energia e proteína, e não há o que
 * fechar. As duas guardas de {@link #validarPlausibilidade} substituem esse
 * fechamento — ver {@code docs/09 §3.1}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormulaLacteaService {

    private static final String GLOBAL_NAO_EDITAVEL =
            "Esta é uma fórmula do sistema e não pode ser alterada. "
                    + "Cadastre uma fórmula própria se precisar de outra composição.";

    /**
     * Teto de densidade energética, em kcal por 100 ml.
     *
     * <p>O produto enteral líquido mais denso descrito na literatura chega a
     * <b>2,4 kcal/ml</b> — acima de 1,5 kcal/ml a densidade só sobe aumentando
     * muito a fração lipídica, e passado isso não é mais líquido. 250 deixa
     * folga larga sobre o mais denso do catálogo, o FORTINI a 150.
     *
     * <p>O que este teto pega é o erro que importa: a composição da <b>lata de
     * pó</b> (≈ 500 kcal/100 g) lançada como se fosse a do produto
     * reconstituído (≈ 67 kcal/100 ml). Hoje isso passava calado e multiplicava
     * a mamadeira por sete.
     */
    private static final BigDecimal DENSIDADE_MAX_KCAL_100ML = new BigDecimal("250");

    /**
     * Piso de densidade energética, em kcal por 100 ml.
     *
     * <p>Abaixo disso não alimenta. Pega o fator 10 para baixo (6,7 no lugar de
     * 67) e a troca de campos — energia e proteína digitadas ao contrário.
     */
    private static final BigDecimal DENSIDADE_MIN_KCAL_100ML = new BigDecimal("20");

    /**
     * Faixa da razão proteína/energia, em gramas por 100 kcal.
     *
     * <p>A razão é <b>invariante de escala</b>: vale igual para a fórmula de
     * partida a 67 kcal/100 ml e para o suplemento hipercalórico a 150. É por
     * isso que ela é a régua certa aqui, e não uma faixa de energia — o
     * catálogo não é de fórmula infantil de partida, é de tudo que a criança
     * recebe por via oral ou sonda.
     *
     * <p>O <b>Codex Alimentarius CXS 72-1981</b> fixa, para fórmula infantil,
     * <b>1,8 a 3,0 g/100 kcal</b>. As dez fórmulas semeadas pela migration
     * {@code 016} caem todas dentro dessa faixa — de 1,86 (NAN 1) a 2,85
     * (NEOCATE LCD), incluindo os três hipercalóricos.
     *
     * <p>A guarda adota <b>1,0 a 6,0</b>: a faixa do Codex alargada para caber
     * o produto pediátrico especializado, que existe fora do escopo da norma.
     * Ainda assim pega o fator 10 — uma proteína dez vezes maior cai perto de
     * 21 g/100 kcal, muito fora. Esta não é uma checagem de conformidade
     * regulatória; é guarda de erro de digitação, e reprovar produto real seria
     * pior do que não ter guarda nenhuma.
     */
    private static final BigDecimal RAZAO_MIN_G_100KCAL = new BigDecimal("1.0");
    private static final BigDecimal RAZAO_MAX_G_100KCAL = new BigDecimal("6.0");

    private static final BigDecimal CEM = new BigDecimal("100");

    private final FormulaLacteaRepository formulaLacteaRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<FormulaLacteaResponseDto> getAll(Pageable pageable, String nome,
                                                 Boolean ativo, Boolean global) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaLacteaRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId, textoOuNulo(nome), ativo, global)
                .map(FormulaLacteaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<FormulaLacteaSelectDto> select(String termo) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaLacteaRepository.findForSelect(tenantId, textoOuNulo(termo))
                .stream()
                .map(FormulaLacteaMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public FormulaLacteaResponseDto findById(UUID id) {
        return FormulaLacteaMapper.toResponse(buscarVisivel(id));
    }

    /** Leitura: enxerga a do tenant e a global. Usado também pelo cálculo. */
    @Transactional(readOnly = true)
    public FormulaLactea buscarVisivel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return formulaLacteaRepository.findByIdVisivelPara(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fórmula láctea não encontrada"));
    }

    @Transactional
    public FormulaLacteaResponseDto create(FormulaLacteaCreateDto dto) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        String nome = dto.nome().trim();

        if (formulaLacteaRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, nome))
            throw new ConflictException("Já existe uma fórmula com esse nome");

        validarPlausibilidade(dto.kcalPor100ml(), dto.proteinaPor100ml());

        FormulaLactea formula = new FormulaLactea();
        formula.setTenant(securityUtils.getTenantReference());
        formula.setNome(nome);
        formula.setKcalPor100ml(dto.kcalPor100ml());
        formula.setProteinaPor100ml(dto.proteinaPor100ml());
        formula.setAtivo(dto.ativo() == null || dto.ativo());

        FormulaLactea salva = formulaLacteaRepository.save(formula);
        log.info("Fórmula láctea criada id={}", salva.getId());
        return FormulaLacteaMapper.toResponse(salva);
    }

    @Transactional
    public FormulaLacteaResponseDto update(UUID id, FormulaLacteaUpdateDto dto) {
        FormulaLactea formula = buscarEditavel(id);
        UUID tenantId = formula.getTenant().getId();
        String nome = dto.nome().trim();

        if (formulaLacteaRepository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(tenantId, nome, id))
            throw new ConflictException("Já existe uma fórmula com esse nome");

        validarPlausibilidade(dto.kcalPor100ml(), dto.proteinaPor100ml());

        formula.setNome(nome);
        formula.setKcalPor100ml(dto.kcalPor100ml());
        formula.setProteinaPor100ml(dto.proteinaPor100ml());

        log.info("Fórmula láctea alterada id={}", id);
        return FormulaLacteaMapper.toResponse(formulaLacteaRepository.save(formula));
    }

    @Transactional
    public FormulaLacteaResponseDto alterarAtivo(UUID id, boolean ativo) {
        FormulaLactea formula = buscarEditavel(id);
        formula.setAtivo(ativo);

        log.info("Fórmula láctea {} alterada para ativo={}", id, ativo);
        return FormulaLacteaMapper.toResponse(formulaLacteaRepository.save(formula));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Recusa composição que não pode ser de um produto real.
     *
     * <p>Duas checagens, cada uma pegando um erro diferente: a faixa de
     * densidade denuncia a composição do <b>pó</b> lançada como reconstituída,
     * e a razão proteína/energia denuncia o <b>ponto decimal deslocado</b> na
     * proteína. Nenhuma das duas sozinha pega as duas coisas — o pó tem razão
     * proteica correta, e o fator 10 na proteína não muda a densidade.
     *
     * <p>Vale para o cadastro do tenant. O seed global da migration {@code 016}
     * passa pelas duas, e {@code CatalogoPediatriaTest} garante que continue
     * passando: se alguém apertar a faixa, o próprio catálogo do sistema
     * reprova.
     */
    private void validarPlausibilidade(BigDecimal kcalPor100ml, BigDecimal proteinaPor100ml) {
        if (kcalPor100ml == null || proteinaPor100ml == null) return;

        if (kcalPor100ml.compareTo(DENSIDADE_MAX_KCAL_100ML) > 0)
            throw new BadRequestException(
                    ("%s kcal por 100 ml é densidade de pó, não de fórmula pronta — o produto "
                            + "líquido mais denso que existe chega a 240. Confira se o rótulo lido "
                            + "é o da lata: aqui a composição é sempre por 100 ml já preparados.")
                            .formatted(semZeroAtoa(kcalPor100ml)));

        if (kcalPor100ml.compareTo(DENSIDADE_MIN_KCAL_100ML) < 0)
            throw new BadRequestException(
                    ("%s kcal por 100 ml é pouco demais para alimentar — fórmula láctea fica "
                            + "entre 60 e 150. Confira se o ponto decimal não deslocou, e se "
                            + "energia e proteína não trocaram de campo.")
                            .formatted(semZeroAtoa(kcalPor100ml)));

        // Proteína zero é módulo puro de carboidrato ou lipídio, que existe em
        // dieta metabólica pediátrica. Sem proteína não há razão a conferir, e
        // exigi-la transformaria um cadastro legítimo em cadastro impossível —
        // o mesmo critério que a fórmula enteral usa com macro ausente.
        if (proteinaPor100ml.signum() == 0) return;

        BigDecimal razao = proteinaPor100ml
                .multiply(CEM, MathContext.DECIMAL64)
                .divide(kcalPor100ml, MathContext.DECIMAL64);

        if (razao.compareTo(RAZAO_MAX_G_100KCAL) > 0 || razao.compareTo(RAZAO_MIN_G_100KCAL) < 0)
            throw new BadRequestException(
                    ("%s g de proteína para %s kcal dá %.1f g por 100 kcal, fora do que uma "
                            + "fórmula láctea pode ser (o Codex CXS 72-1981 fixa 1,8 a 3,0 para "
                            + "fórmula infantil, e produto especializado não passa de 6). Confira "
                            + "o ponto decimal da proteína.")
                            .formatted(semZeroAtoa(proteinaPor100ml), semZeroAtoa(kcalPor100ml), razao));
    }

    /** {@code 74.20} vira {@code 74,2} — a mensagem é lida por gente. */
    private String semZeroAtoa(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /**
     * A fórmula que este tenant pode alterar.
     *
     * <p>Busca primeiro sem o {@code OR IS NULL}: se não achar, verifica se ela
     * existe como global só para poder recusar com a mensagem certa. Devolver
     * 404 aqui faria o usuário procurar um registro que está bem à sua frente
     * na lista.
     */
    private FormulaLactea buscarEditavel(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();

        return formulaLacteaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> formulaLacteaRepository.findByIdVisivelPara(id, tenantId)
                        .map(global -> (RuntimeException) new BadRequestException(GLOBAL_NAO_EDITAVEL))
                        .orElseGet(() -> new NotFoundException("Fórmula láctea não encontrada")));
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
