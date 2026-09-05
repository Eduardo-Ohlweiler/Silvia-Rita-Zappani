import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBusca } from '@/assets/icons'
import {
  TAcoesDeExportacao,
  TBadge,
  TButton,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TPage,
  TPanel,
  TSelect,
  type Coluna,
  type OpcaoSelect,
  type PaginaDaCarga,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import type { Page } from '@/types/comum'
import { OPCOES_TIPO_PESSOA, type PessoaResponse, type TipoPessoa } from '@/types/pessoa'
import { formatarData, formatarDocumento, formatarTelefone, rotuloDe } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

export function PessoaList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [documento, setDocumento] = useState('')
  const [tipoPessoa, setTipoPessoa] = useState('')
  const [tipoCadastroId, setTipoCadastroId] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<PessoaResponse>>()
  const [tiposCadastro, setTiposCadastro] = useState<OpcaoSelect[]>([])
  const [carregando, setCarregando] = useState(true)
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<PessoaResponse>>()

  const nomeBusca = useDebounce(nome)
  const documentoBusca = useDebounce(documento)

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTiposCadastro(tipos.map((t) => ({ valor: t.id, rotulo: t.nome }))))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    setCarregando(true)
    pessoaService
      .getAll({
        nome: nomeBusca || undefined,
        documento: documentoBusca || undefined,
        tipoPessoa: (tipoPessoa as TipoPessoa) || undefined,
        tipoCadastroId: tipoCadastroId || undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com os dados do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, nomeBusca, documentoBusca, tipoPessoa, tipoCadastroId, ativo, pagina])

  useEffect(carregar, [carregar])

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<PessoaResponse>> => {
      const pagina = await pessoaService.getAll({
        nome: nomeBusca || undefined,
        documento: documentoBusca || undefined,
        tipoPessoa: (tipoPessoa as TipoPessoa) || undefined,
        tipoCadastroId: tipoCadastroId || undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        page: indice,
        size: limite,
      })
      return {
        linhas: pagina.content,
        total: pagina.totalElements,
      }
    },
    [nomeBusca, documentoBusca, tipoPessoa, tipoCadastroId, ativo],
  )

  /**
   * O cadastro tem listas filhas, e uma célula não comporta lista.
   *
   * <p>Telefone, e-mail e endereço saem pelo <b>principal</b> — que é o que a
   * secretaria usa para ligar —, e o cadastro completo continua na tela do
   * registro. Exportar todos os telefones de cada pessoa numa célula produziria
   * planilha que ninguém filtra.
   *
   * <p>Os tipos de cadastro, ao contrário, saem <b>todos</b>: uma pessoa que é
   * paciente <i>e</i> responsável deixaria de aparecer no filtro de responsável
   * se a exportação escolhesse um só.
   */
  const colunasExportadas: ColunaExportavel<PessoaResponse>[] = [
    { titulo: 'Nome', valor: (p) => p.nome },
    { titulo: 'Nome fantasia', valor: (p) => p.nomeFantasia ?? '' },
    { titulo: 'Tipo de pessoa', valor: (p) => rotuloDe(OPCOES_TIPO_PESSOA, p.tipoPessoa) ?? '' },
    { titulo: 'CPF / CNPJ', valor: (p) => formatarDocumento(p.cpf ?? p.cnpj) },
    {
      titulo: 'Nascimento',
      valor: (p) => (p.dataNascimento ? formatarData(p.dataNascimento) : ''),
    },
    { titulo: 'Tipos de cadastro', valor: (p) => p.tiposCadastro.map((t) => t.nome).join(', ') },
    { titulo: 'Telefone principal', valor: (p) => telefonePrincipal(p) },
    { titulo: 'E-mail principal', valor: (p) => emailPrincipal(p) },
    { titulo: 'Cidade', valor: (p) => cidadePrincipal(p) },
    { titulo: 'Vínculos', numerica: true, valor: (p) => String(p.vinculos.length) },
    { titulo: 'Situação', valor: (p) => (p.ativo ? 'Ativa' : 'Inativa') },
  ]

  const filtrosAplicados = [
    { rotulo: 'Nome', valor: nomeBusca },
    { rotulo: 'CPF/CNPJ', valor: documentoBusca },
    { rotulo: 'Tipo de pessoa', valor: rotuloDe(OPCOES_TIPO_PESSOA, tipoPessoa) ?? '' },
    {
      rotulo: 'Tipo de cadastro',
      valor: tiposCadastro.find((t) => t.valor === tipoCadastroId)?.rotulo ?? '',
    },
    { rotulo: 'Situação', valor: ativo === 'true' ? 'Ativa' : ativo === 'false' ? 'Inativa' : '' },
  ]

  async function alternarAtivo(pessoa: PessoaResponse) {
    try {
      await pessoaService.alterarAtivo(pessoa.id, !pessoa.ativo)
      toast.success(pessoa.ativo ? `${pessoa.nome} foi inativada` : `${pessoa.nome} foi reativada`)
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<PessoaResponse>[] = [
    {
      chave: 'nome',
      cabecalho: 'Nome',
      render: (p) => (
        <span className="flex flex-col">
          <span>{p.nome}</span>
          {p.nomeFantasia && (
            <span className="text-caption text-txt-secondary">{p.nomeFantasia}</span>
          )}
        </span>
      ),
    },
    {
      chave: 'documento',
      cabecalho: 'CPF / CNPJ',
      render: (p) => formatarDocumento(p.cpf ?? p.cnpj),
    },
    {
      chave: 'tipoCadastro',
      cabecalho: 'Tipo de cadastro',
      secundaria: true,
      render: (p) =>
        p.tiposCadastro.length === 0 ? (
          '—'
        ) : (
          <span className="flex flex-wrap gap-1">
            {p.tiposCadastro.map((t) => (
              <TBadge key={t.id} tom="sage">
                {t.nome}
              </TBadge>
            ))}
          </span>
        ),
    },
    {
      chave: 'telefone',
      cabecalho: 'Telefone',
      secundaria: true,
      render: (p) => {
        const principal = p.telefones.find((t) => t.principal) ?? p.telefones[0]
        return principal
          ? formatarTelefone(principal.codigoPais, principal.numero)
          : '—'
      },
    },
    {
      chave: 'cidade',
      cabecalho: 'Cidade',
      secundaria: true,
      render: (p) => {
        const principal = p.enderecos.find((e) => e.principal) ?? p.enderecos[0]
        return principal ? `${principal.cidadeNome}/${principal.estadoSigla}` : '—'
      },
    },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (p) =>
        p.ativo ? <TBadge tom="sucesso">Ativa</TBadge> : <TBadge tom="neutro">Inativa</TBadge>,
    },
  ]

  return (
    <TPage
      title="Pessoas"
      subtitle="Pacientes, responsáveis, profissionais e fornecedores do seu cliente."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Pessoas"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/pessoas/nova')}>
            <IconAdicionar className="size-4" />
            Nova pessoa
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Pessoas"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(p) => p.id}
            resumo={[
              { rotulo: 'Pessoas', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Físicas',
                valor: String(paraImprimir.linhas.filter((p) => p.tipoPessoa === 'PESSOA_FISICA').length),
              },
              {
                rotulo: 'Ativas',
                valor: String(paraImprimir.linhas.filter((p) => p.ativo).length),
              },
            ]}
            nota="Telefone, e-mail e cidade saem pelo registro principal de cada pessoa; o cadastro completo fica na tela do registro. Esta folha contém dado pessoal — trate-a como documento do prontuário."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <TEntry
            label="Nome"
            placeholder="Buscar por nome"
            value={nome}
            suffix={<IconBusca className="size-4" />}
            onChange={(e) => {
              setNome(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="CPF / CNPJ"
            placeholder="Com ou sem pontuação"
            value={documento}
            onChange={(e) => {
              setDocumento(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Tipo de pessoa"
            vazio="Todos"
            opcoes={OPCOES_TIPO_PESSOA}
            value={tipoPessoa}
            onChange={(e) => {
              setTipoPessoa(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Tipo de cadastro"
            vazio="Todos"
            opcoes={tiposCadastro}
            value={tipoCadastroId}
            onChange={(e) => {
              setTipoCadastroId(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Situação"
            vazio="Todas"
            opcoes={[
              { valor: 'true', rotulo: 'Ativa' },
              { valor: 'false', rotulo: 'Inativa' },
            ]}
            value={ativo}
            onChange={(e) => {
              setAtivo(e.target.value)
              setPagina(0)
            }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(p) => p.id}
        carregando={carregando}
        onLinhaClick={(p) => navigate(`/app/pessoas/${p.id}`)}
        tituloCartao={(p) => p.nome}
        vazioTitulo="Nenhuma pessoa encontrada"
        vazioDescricao="Ajuste os filtros ou cadastre a primeira pessoa."
        acoes={(p) => (
          <TButton
            variant="secondary"
            size="sm"
            onClick={() => void alternarAtivo(p)}
            title={
              p.ativo
                ? 'Inativar — o cadastro continua no histórico'
                : 'Reativar o cadastro'
            }
          >
            {p.ativo ? 'Inativar' : 'Reativar'}
          </TButton>
        )}
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}

/** O contato que a secretaria usa: o marcado como principal, ou o primeiro. */
function telefonePrincipal(p: PessoaResponse): string {
  const t = p.telefones.find((x) => x.principal) ?? p.telefones[0]
  return t ? formatarTelefone(t.codigoPais, t.numero) : ''
}

function emailPrincipal(p: PessoaResponse): string {
  const e = p.emails.find((x) => x.principal) ?? p.emails[0]
  return e?.email ?? ''
}

function cidadePrincipal(p: PessoaResponse): string {
  const e = p.enderecos.find((x) => x.principal) ?? p.enderecos[0]
  return e ? `${e.cidadeNome}/${e.estadoSigla}` : ''
}
