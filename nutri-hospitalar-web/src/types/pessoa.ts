import type { Paginacao, SelectOption } from './comum'

/** Espelha o enum `TipoPessoa` do backend. */
export type TipoPessoa = 'PESSOA_FISICA' | 'PESSOA_JURIDICA'

export const TIPO_PESSOA_LABEL: Record<TipoPessoa, string> = {
  PESSOA_FISICA: 'Pessoa física',
  PESSOA_JURIDICA: 'Pessoa jurídica',
}

export const OPCOES_TIPO_PESSOA = (Object.keys(TIPO_PESSOA_LABEL) as TipoPessoa[]).map((t) => ({
  valor: t,
  rotulo: TIPO_PESSOA_LABEL[t],
}))

// ─── Contatos ─────────────────────────────────────────────────────────

export interface TelefoneResponse {
  id: string
  tipoTelefoneId: string
  tipoTelefoneNome: string
  codigoPais: string
  numero: string
  observacao: string | null
  principal: boolean
}

export interface EmailResponse {
  id: string
  tipoEmailId: string
  tipoEmailNome: string
  email: string
  observacao: string | null
  principal: boolean
}

export interface RedeSocialResponse {
  id: string
  tipoRedeSocialId: string
  tipoRedeSocialNome: string
  usuario: string | null
  url: string | null
  observacao: string | null
}

/**
 * Item enviado no create/update. `id` ausente é linha nova; o que não for
 * enviado é **removido** no backend — a lista é o estado completo.
 */
export interface TelefoneItem {
  id?: string
  tipoTelefoneId: string
  numero: string
  codigoPais?: string
  observacao?: string
  principal?: boolean
}

export interface EmailItem {
  id?: string
  tipoEmailId: string
  email: string
  observacao?: string
  principal?: boolean
}

export interface RedeSocialItem {
  id?: string
  tipoRedeSocialId: string
  usuario?: string
  url?: string
  observacao?: string
}

// ─── Endereço ─────────────────────────────────────────────────────────

export interface EnderecoResponse {
  id: string
  tipoEnderecoId: string
  tipoEnderecoNome: string
  cidadeId: string
  cidadeNome: string
  estadoSigla: string
  estadoNome: string
  cep: string | null
  rua: string | null
  numero: string | null
  bairro: string | null
  complemento: string | null
  principal: boolean
}

export interface EnderecoItem {
  id?: string
  tipoEnderecoId: string
  cidadeId: string
  cep?: string
  rua?: string
  numero?: string
  bairro?: string
  complemento?: string
  principal?: boolean
}

// ─── Vínculo ──────────────────────────────────────────────────────────

/** Espelha o enum `TipoVinculo`. Responsável e dependente são inversos. */
export type TipoVinculo = 'RESPONSAVEL' | 'DEPENDENTE' | 'CONJUGE' | 'FAMILIAR'

export const TIPO_VINCULO_LABEL: Record<TipoVinculo, string> = {
  RESPONSAVEL: 'Responsável',
  DEPENDENTE: 'Dependente',
  CONJUGE: 'Cônjuge',
  FAMILIAR: 'Familiar',
}

export const OPCOES_TIPO_VINCULO = (Object.keys(TIPO_VINCULO_LABEL) as TipoVinculo[]).map(
  (t) => ({ valor: t, rotulo: TIPO_VINCULO_LABEL[t] }),
)

/** Os dados são sempre da **outra** pessoa; o tipo é o papel dela. */
export interface VinculoResponse {
  id: string
  pessoaId: string
  pessoaNome: string
  documento: string | null
  tipo: TipoVinculo
  tipoDescricao: string
  observacao: string | null
}

export interface VinculoItem {
  id?: string
  pessoaId: string
  tipo: TipoVinculo
  observacao?: string
}

// ─── Pessoa ───────────────────────────────────────────────────────────

export interface PessoaResponse {
  id: string
  nome: string
  tipoPessoa: TipoPessoa
  dataNascimento: string | null
  cpf: string | null
  rg: string | null
  cnpj: string | null
  inscricaoEstadual: string | null
  inscricaoMunicipal: string | null
  nomeFantasia: string | null
  razaoSocial: string | null
  observacao: string | null
  ativo: boolean
  tiposCadastro: SelectOption[]
  telefones: TelefoneResponse[]
  emails: EmailResponse[]
  redesSociais: RedeSocialResponse[]
  enderecos: EnderecoResponse[]
  vinculos: VinculoResponse[]
  createdAt: string
  updatedAt: string | null
}

export interface PessoaCreate {
  nome: string
  tipoPessoa: TipoPessoa
  dataNascimento?: string
  cpf?: string
  rg?: string
  cnpj?: string
  inscricaoEstadual?: string
  inscricaoMunicipal?: string
  nomeFantasia?: string
  razaoSocial?: string
  observacao?: string
  tiposCadastroIds: string[]
  telefones?: TelefoneItem[]
  emails?: EmailItem[]
  redesSociais?: RedeSocialItem[]
  enderecos?: EnderecoItem[]
  vinculos?: VinculoItem[]
}

export type PessoaUpdate = PessoaCreate

export interface PessoaSelect {
  id: string
  nome: string
  documento: string | null
}

export interface PessoaFiltros extends Paginacao {
  nome?: string
  documento?: string
  tipoPessoa?: TipoPessoa
  ativo?: boolean
  tipoCadastroId?: string
}
