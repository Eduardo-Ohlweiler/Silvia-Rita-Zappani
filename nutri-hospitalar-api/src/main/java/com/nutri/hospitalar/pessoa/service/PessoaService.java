package com.nutri.hospitalar.pessoa.service;

import com.nutri.hospitalar.catalogo.entity.TipoCadastro;
import com.nutri.hospitalar.catalogo.service.CatalogoService;
import com.nutri.hospitalar.config.PageableUtils;
import com.nutri.hospitalar.config.SecurityUtils;
import com.nutri.hospitalar.contato.dtos.EmailItemDto;
import com.nutri.hospitalar.contato.dtos.EnderecoItemDto;
import com.nutri.hospitalar.contato.dtos.RedeSocialItemDto;
import com.nutri.hospitalar.contato.dtos.TelefoneItemDto;
import com.nutri.hospitalar.contato.service.ContatoService;
import com.nutri.hospitalar.exceptions.BadRequestException;
import com.nutri.hospitalar.exceptions.ConflictException;
import com.nutri.hospitalar.exceptions.NotFoundException;
import com.nutri.hospitalar.pessoa.dtos.PessoaCreateDto;
import com.nutri.hospitalar.pessoa.dtos.PessoaResponseDto;
import com.nutri.hospitalar.pessoa.dtos.PessoaSelectDto;
import com.nutri.hospitalar.pessoa.dtos.PessoaUpdateDto;
import com.nutri.hospitalar.pessoa.entity.Pessoa;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.pessoa.mapper.PessoaMapper;
import com.nutri.hospitalar.pessoa.repository.PessoaRepository;
import com.nutri.hospitalar.pessoa.util.PessoaValidator;
import com.nutri.hospitalar.vinculo.dtos.VinculoItemDto;
import com.nutri.hospitalar.vinculo.entity.PessoaVinculo;
import com.nutri.hospitalar.vinculo.service.VinculoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cadastro de pessoas — porte do {@code PessoaService} do eroERP.
 *
 * <p><b>Nada de dado pessoal em log (regra 5 do CLAUDE.md).</b> Nome, CPF,
 * telefone e e-mail são dados de saúde sob a LGPD quando ligados a um paciente:
 * os logs deste módulo levam só o id e a operação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final CatalogoService catalogoService;
    private final ContatoService contatoService;
    private final VinculoService vinculoService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<PessoaResponseDto> getAll(Pageable pageable, String nome, String documento,
                                          TipoPessoa tipoPessoa, Boolean ativo,
                                          UUID tipoCadastroId) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findAllWithFilters(
                        PageableUtils.semOrdenacao(pageable),
                        tenantId,
                        nomeOuNulo(nome),
                        PessoaValidator.somenteDigitosOuNulo(documento),
                        tipoPessoa != null ? tipoPessoa.name() : null,
                        ativo,
                        tipoCadastroId)
                .map(PessoaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PessoaSelectDto> select(String termo, UUID tipoCadastroId, UUID ignorarId) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository
                .findForSelect(tenantId, nomeOuNulo(termo), tipoCadastroId, ignorarId).stream()
                .map(PessoaMapper::toSelect)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponseDto findByIdResponse(UUID id) {
        Pessoa pessoa = findById(id);
        return PessoaMapper.toResponse(pessoa, vinculoService.daPessoa(pessoa));
    }

    @Transactional(readOnly = true)
    public Pessoa findById(UUID id) {
        UUID tenantId = securityUtils.getTenantIdLogado();
        return pessoaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Pessoa não encontrada"));
    }

    @Transactional
    public PessoaResponseDto create(PessoaCreateDto dto) {
        Documentos documentos = Documentos.de(dto.cpf(), dto.rg(), dto.cnpj(),
                dto.inscricaoEstadual(), dto.inscricaoMunicipal());

        validarCamposPorTipo(dto.tipoPessoa(), documentos,
                dto.nomeFantasia(), dto.razaoSocial(), dto.dataNascimento());

        UUID tenantId = securityUtils.getTenantIdLogado();
        validarDocumentos(documentos, tenantId, null);

        Set<TipoCadastro> tipos = catalogoService.exigirTiposCadastro(dto.tiposCadastroIds());

        Pessoa pessoa = new Pessoa();
        pessoa.setTenant(securityUtils.getTenantReference());
        pessoa.setNome(dto.nome().trim());
        pessoa.setTipoPessoa(dto.tipoPessoa());
        pessoa.setObservacao(dto.observacao());
        pessoa.setCreatedBy(securityUtils.getUsuarioLogado());
        aplicarDadosDoTipo(pessoa, dto.tipoPessoa(), documentos, dto.dataNascimento(),
                dto.nomeFantasia(), dto.razaoSocial());
        pessoa.getTiposCadastro().addAll(tipos);

        Pessoa salva = pessoaRepository.save(pessoa);
        List<PessoaVinculo> vinculos = sincronizarFilhos(salva,
                dto.telefones(), dto.emails(), dto.redesSociais(),
                dto.enderecos(), dto.vinculos());

        log.info("Pessoa criada id={} tipo={}", salva.getId(), salva.getTipoPessoa());
        return PessoaMapper.toResponse(salva, vinculos);
    }

    @Transactional
    public PessoaResponseDto update(UUID id, PessoaUpdateDto dto) {
        Pessoa pessoa = findById(id);

        Documentos documentos = Documentos.de(dto.cpf(), dto.rg(), dto.cnpj(),
                dto.inscricaoEstadual(), dto.inscricaoMunicipal());

        validarCamposPorTipo(dto.tipoPessoa(), documentos,
                dto.nomeFantasia(), dto.razaoSocial(), dto.dataNascimento());

        UUID tenantId = pessoa.getTenant().getId();
        validarDocumentos(documentos, tenantId, id);

        Set<TipoCadastro> tipos = catalogoService.exigirTiposCadastro(dto.tiposCadastroIds());

        pessoa.setNome(dto.nome().trim());
        pessoa.setTipoPessoa(dto.tipoPessoa());
        pessoa.setObservacao(dto.observacao());
        pessoa.setUpdatedBy(securityUtils.getUsuarioLogado());
        aplicarDadosDoTipo(pessoa, dto.tipoPessoa(), documentos, dto.dataNascimento(),
                dto.nomeFantasia(), dto.razaoSocial());

        pessoa.getTiposCadastro().clear();
        pessoa.getTiposCadastro().addAll(tipos);

        Pessoa salva = pessoaRepository.save(pessoa);
        List<PessoaVinculo> vinculos = sincronizarFilhos(salva,
                dto.telefones(), dto.emails(), dto.redesSociais(),
                dto.enderecos(), dto.vinculos());

        log.info("Pessoa alterada id={}", id);
        return PessoaMapper.toResponse(salva, vinculos);
    }

    @Transactional
    public PessoaResponseDto alterarAtivo(UUID id, boolean ativo) {
        Pessoa pessoa = findById(id);
        pessoa.setAtivo(ativo);
        pessoa.setUpdatedBy(securityUtils.getUsuarioLogado());

        log.info("Pessoa {} alterada para ativo={}", id, ativo);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
    }

    // ─────────────────────────────────────────────────────────────────────

    /**
     * @return os vínculos resultantes — não são coleção da pessoa, e o mapper
     *         precisa deles para montar a resposta.
     */
    private List<PessoaVinculo> sincronizarFilhos(Pessoa pessoa,
                                                  List<TelefoneItemDto> telefones,
                                                  List<EmailItemDto> emails,
                                                  List<RedeSocialItemDto> redes,
                                                  List<EnderecoItemDto> enderecos,
                                                  List<VinculoItemDto> vinculos) {
        // A lista devolvida substitui a da entidade em memória: sem isso a
        // resposta sairia com os contatos anteriores.
        pessoa.setTelefones(contatoService.sincronizarTelefones(pessoa, telefones));
        pessoa.setEmails(contatoService.sincronizarEmails(pessoa, emails));
        pessoa.setRedesSociais(contatoService.sincronizarRedesSociais(pessoa, redes));
        pessoa.setEnderecos(contatoService.sincronizarEnderecos(pessoa, enderecos));

        return vinculoService.sincronizar(pessoa, vinculos);
    }

    /**
     * Campo do tipo errado é recusado, não ignorado: CNPJ digitado numa pessoa
     * física quase sempre significa que quem preencheu escolheu o tipo errado,
     * e salvar em silêncio esconderia o engano.
     */
    private void validarCamposPorTipo(TipoPessoa tipoPessoa, Documentos doc,
                                      String nomeFantasia, String razaoSocial,
                                      java.time.LocalDate dataNascimento) {

        if (TipoPessoa.PESSOA_FISICA.equals(tipoPessoa)) {
            if (doc.cnpj() != null)
                throw new BadRequestException("Pessoa física não tem CNPJ");
            if (doc.inscricaoEstadual() != null)
                throw new BadRequestException("Pessoa física não tem Inscrição Estadual");
            if (doc.inscricaoMunicipal() != null)
                throw new BadRequestException("Pessoa física não tem Inscrição Municipal");
            if (preenchido(nomeFantasia))
                throw new BadRequestException("Pessoa física não tem nome fantasia");
            if (preenchido(razaoSocial))
                throw new BadRequestException("Pessoa física não tem razão social");
            return;
        }

        if (doc.cpf() != null)
            throw new BadRequestException("Pessoa jurídica não tem CPF");
        if (doc.rg() != null)
            throw new BadRequestException("Pessoa jurídica não tem RG");
        if (dataNascimento != null)
            throw new BadRequestException("Pessoa jurídica não tem data de nascimento");
    }

    private void validarDocumentos(Documentos doc, UUID tenantId, UUID idAtual) {
        if (doc.cpf() != null) {
            if (!PessoaValidator.validarCpf(doc.cpf()))
                throw new BadRequestException("CPF inválido");
            if (idAtual == null
                    ? pessoaRepository.existsByCpfAndTenantId(doc.cpf(), tenantId)
                    : pessoaRepository.existsByCpfAndTenantIdAndIdNot(doc.cpf(), tenantId, idAtual))
                throw new ConflictException("Já existe uma pessoa com esse CPF");
        }

        if (doc.cnpj() != null) {
            if (!PessoaValidator.validarCnpj(doc.cnpj()))
                throw new BadRequestException("CNPJ inválido");
            if (idAtual == null
                    ? pessoaRepository.existsByCnpjAndTenantId(doc.cnpj(), tenantId)
                    : pessoaRepository.existsByCnpjAndTenantIdAndIdNot(doc.cnpj(), tenantId, idAtual))
                throw new ConflictException("Já existe uma pessoa com esse CNPJ");
        }

        if (doc.rg() != null) {
            if (!PessoaValidator.validarRg(doc.rg()))
                throw new BadRequestException("RG inválido");
            if (idAtual == null
                    ? pessoaRepository.existsByRgAndTenantId(doc.rg(), tenantId)
                    : pessoaRepository.existsByRgAndTenantIdAndIdNot(doc.rg(), tenantId, idAtual))
                throw new ConflictException("Já existe uma pessoa com esse RG");
        }

        if (doc.inscricaoEstadual() != null
                && !PessoaValidator.validarInscricao(doc.inscricaoEstadual()))
            throw new BadRequestException("Inscrição Estadual inválida");

        if (doc.inscricaoMunicipal() != null
                && !PessoaValidator.validarInscricao(doc.inscricaoMunicipal()))
            throw new BadRequestException("Inscrição Municipal inválida");
    }

    /**
     * Grava os campos do tipo escolhido e <b>limpa os do outro</b>. Trocar de
     * tipo sem limpar deixaria um CPF pendurado numa pessoa jurídica, e o
     * índice único ainda o consideraria ocupado.
     */
    private void aplicarDadosDoTipo(Pessoa pessoa, TipoPessoa tipoPessoa, Documentos doc,
                                    java.time.LocalDate dataNascimento,
                                    String nomeFantasia, String razaoSocial) {

        if (TipoPessoa.PESSOA_FISICA.equals(tipoPessoa)) {
            pessoa.setDataNascimento(dataNascimento);
            pessoa.setCpf(doc.cpf());
            pessoa.setRg(doc.rg());

            pessoa.setCnpj(null);
            pessoa.setInscricaoEstadual(null);
            pessoa.setInscricaoMunicipal(null);
            pessoa.setNomeFantasia(null);
            pessoa.setRazaoSocial(null);
            return;
        }

        pessoa.setCnpj(doc.cnpj());
        pessoa.setInscricaoEstadual(doc.inscricaoEstadual());
        pessoa.setInscricaoMunicipal(doc.inscricaoMunicipal());
        pessoa.setNomeFantasia(textoOuNulo(nomeFantasia));
        pessoa.setRazaoSocial(textoOuNulo(razaoSocial));

        pessoa.setDataNascimento(null);
        pessoa.setCpf(null);
        pessoa.setRg(null);
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String textoOuNulo(String valor) {
        return preenchido(valor) ? valor.trim() : null;
    }

    private String nomeOuNulo(String valor) {
        return preenchido(valor) ? valor.trim() : null;
    }

    /**
     * Documentos já reduzidos a dígitos. A tela manda com máscara e o banco
     * guarda sem — normalizar num ponto só evita "111.111.111-11" e
     * "11111111111" convivendo como se fossem pessoas diferentes.
     */
    private record Documentos(String cpf, String rg, String cnpj,
                              String inscricaoEstadual, String inscricaoMunicipal) {

        static Documentos de(String cpf, String rg, String cnpj,
                             String inscricaoEstadual, String inscricaoMunicipal) {
            return new Documentos(
                    PessoaValidator.somenteDigitosOuNulo(cpf),
                    rgOuNulo(rg),
                    PessoaValidator.somenteDigitosOuNulo(cnpj),
                    PessoaValidator.somenteDigitosOuNulo(inscricaoEstadual),
                    PessoaValidator.somenteDigitosOuNulo(inscricaoMunicipal));
        }

        /** RG mantém o X do dígito verificador — só a pontuação sai. */
        private static String rgOuNulo(String rg) {
            if (rg == null || rg.isBlank()) return null;
            String limpo = rg.replaceAll("[^0-9Xx]", "").toUpperCase();
            return limpo.isEmpty() ? null : limpo;
        }
    }
}
