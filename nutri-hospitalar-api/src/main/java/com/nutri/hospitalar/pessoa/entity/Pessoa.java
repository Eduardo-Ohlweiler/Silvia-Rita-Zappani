package com.nutri.hospitalar.pessoa.entity;

import com.nutri.hospitalar.baseentity.TenantEntity;
import com.nutri.hospitalar.catalogo.entity.TipoCadastro;
import com.nutri.hospitalar.contato.entity.Email;
import com.nutri.hospitalar.contato.entity.RedeSocial;
import com.nutri.hospitalar.contato.entity.Telefone;
import com.nutri.hospitalar.pessoa.enums.TipoPessoa;
import com.nutri.hospitalar.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pessoa física ou jurídica do cliente.
 *
 * <p>Uma tabela para os dois tipos, com os campos de cada um anuláveis e a
 * coerência garantida no service — desenho herdado do eroERP. Duas tabelas
 * quase iguais exigiriam pendurar a mesma agenda de contatos em ambas.
 */
@Entity
@Table(name = "pessoa")
@Getter
@Setter
@NoArgsConstructor
public class Pessoa extends TenantEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 20)
    private TipoPessoa tipoPessoa;

    // ─── Pessoa física ──────────────────────────────────────────────────
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "rg", length = 20)
    private String rg;

    // ─── Pessoa jurídica ────────────────────────────────────────────────
    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "inscricao_estadual", length = 50)
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = 50)
    private String inscricaoMunicipal;

    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(name = "razao_social", length = 255)
    private String razaoSocial;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pessoa_tipo_cadastro",
            joinColumns = @JoinColumn(name = "pessoa_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_cadastro_id")
    )
    private Set<TipoCadastro> tiposCadastro = new LinkedHashSet<>();

    /*
     * Coleções de leitura: quem escreve são os serviços de contato, pelo
     * `sincronizar…`. Sem cascade de propósito — a remoção do filho é decidida
     * lá, comparando o que veio da tela com o que existe no banco.
     */
    @OneToMany(mappedBy = "pessoa", fetch = FetchType.LAZY)
    private List<Telefone> telefones = new ArrayList<>();

    @OneToMany(mappedBy = "pessoa", fetch = FetchType.LAZY)
    private List<Email> emails = new ArrayList<>();

    @OneToMany(mappedBy = "pessoa", fetch = FetchType.LAZY)
    private List<RedeSocial> redesSociais = new ArrayList<>();

    public boolean ehPessoaFisica() {
        return TipoPessoa.PESSOA_FISICA.equals(this.tipoPessoa);
    }
}
