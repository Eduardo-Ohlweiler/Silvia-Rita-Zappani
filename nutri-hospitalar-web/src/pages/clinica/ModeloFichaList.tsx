import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBloqueado, IconBusca } from '@/assets/icons'
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
  type PaginaDaCarga,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { modeloFichaService } from '@/services/clinicaService'
import type { Page } from '@/types/comum'
import type { ModeloFichaResponse } from '@/types/clinica'
import type { ColunaExportavel } from '@/utils/planilha'

const ORIGENS = [
  { valor: 'sistema', rotulo: 'Do sistema' },
  { valor: 'proprios', rotulo: 'Meus modelos' },
]

const SITUACOES = [
  { valor: 'true', rotulo: 'Ativo' },
  { valor: 'false', rotulo: 'Inativo' },
]

/**
 * Modelos de ficha — o que cada ficha de anamnese pergunta.
 *
 * <p>A coluna **Origem** não é decoração: modelo do sistema é imutável, e sem o
 * selo o usuário só descobriria isso ao tentar salvar. Um botão que existe e
 * recusa é pior que um botão que não existe.
 */
export function ModeloFichaList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [origem, setOrigem] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<ModeloFichaResponse>>()
  const [carregando, setCarregando] = useState(true)
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<ModeloFichaResponse>>()

  const nomeBusca = useDebounce(nome)

  const filtros = useCallback(
    (page: number, size: number) => ({
      nome: nomeBusca || undefined,
      ativo: ativo === '' ? undefined : ativo === 'true',
      doSistema: origem === '' ? undefined : origem === 'sistema',
      page,
      size,
    }),
    [nomeBusca, ativo, origem],
  )

  const carregar = useCallback(() => {
    setCarregando(true)
    modeloFichaService
      .getAll(filtros(pagina, 20))
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com os dados do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, filtros, pagina])

  useEffect(carregar, [carregar])

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<ModeloFichaResponse>> => {
      const p = await modeloFichaService.getAll(filtros(indice, limite))
      return { linhas: p.content, total: p.totalElements }
    },
    [filtros],
  )

  const colunasExportadas: ColunaExportavel<ModeloFichaResponse>[] = [
    { titulo: 'Modelo', valor: (m) => m.nome },
    { titulo: 'Origem', valor: (m) => (m.doSistema ? 'Do sistema' : 'Meu') },
    { titulo: 'Descrição', valor: (m) => m.descricao ?? '' },
    { titulo: 'Perguntas', numerica: true, valor: (m) => String(ativas(m)) },
    { titulo: 'Seções', valor: (m) => secoesDe(m).join(' · ') },
    { titulo: 'Situação', valor: (m) => (m.ativo ? 'Ativo' : 'Inativo') },
  ]

  const filtrosAplicados = [
    { rotulo: 'Nome', valor: nomeBusca },
    {
      rotulo: 'Origem',
      valor: origem === 'sistema' ? 'Do sistema' : origem === 'proprios' ? 'Meus modelos' : '',
    },
    { rotulo: 'Situação', valor: ativo === 'true' ? 'Ativo' : ativo === 'false' ? 'Inativo' : '' },
  ]

  async function alternarAtivo(modelo: ModeloFichaResponse) {
    try {
      await modeloFichaService.alterarAtivo(modelo.id, !modelo.ativo)
      toast.success(
        modelo.ativo
          ? `${modelo.nome} saiu da lista de escolha`
          : `${modelo.nome} voltou para a lista`,
      )
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<ModeloFichaResponse>[] = [
    {
      chave: 'nome',
      cabecalho: 'Modelo',
      render: (m) => (
        <span className="flex flex-wrap items-center gap-2">
          <span>{m.nome}</span>
          {/* O usuário precisa saber antes de clicar que não vai poder editar. */}
          {m.doSistema && (
            <TBadge tom="info" icone={<IconBloqueado className="size-3" />}>
              Do sistema
            </TBadge>
          )}
        </span>
      ),
    },
    {
      chave: 'descricao',
      cabecalho: 'Descrição',
      secundaria: true,
      render: (m) => m.descricao ?? <span className="text-txt-muted">Sem descrição</span>,
    },
    {
      chave: 'perguntas',
      cabecalho: 'Perguntas',
      numerica: true,
      render: (m) => ativas(m),
    },
    {
      chave: 'secoes',
      cabecalho: 'Seções',
      secundaria: true,
      render: (m) => {
        const secoes = secoesDe(m)
        return secoes.length > 0 ? (
          secoes.join(' · ')
        ) : (
          <span className="text-txt-muted">Sem seção</span>
        )
      },
    },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (m) =>
        m.ativo ? <TBadge tom="sucesso">Ativo</TBadge> : <TBadge tom="neutro">Inativo</TBadge>,
    },
  ]

  return (
    <TPage
      title="Modelos de ficha"
      subtitle="As perguntas que cada ficha de anamnese faz. Os três do sistema vêm prontos e valem para todos os clientes — para adaptar um deles, clone."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Modelos de ficha"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/clinica/modelos-ficha/novo')}>
            <IconAdicionar className="size-4" />
            Novo modelo
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Modelos de ficha de anamnese"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(m) => m.id}
            resumo={[
              { rotulo: 'Modelos', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Do sistema',
                valor: String(paraImprimir.linhas.filter((m) => m.doSistema).length),
              },
              {
                rotulo: 'Perguntas ao todo',
                valor: String(paraImprimir.linhas.reduce((s, m) => s + ativas(m), 0)),
              },
            ]}
            nota="Modelo do sistema é igual para todos os clientes e não pode ser alterado. A adaptação se faz clonando: a cópia nasce do cliente e é editável."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
          <TSelect
            label="Origem"
            vazio="Todos"
            opcoes={ORIGENS}
            value={origem}
            onChange={(e) => {
              setOrigem(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Situação"
            vazio="Todas"
            opcoes={SITUACOES}
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
        chaveDe={(m) => m.id}
        carregando={carregando}
        onLinhaClick={(m) => navigate(`/app/clinica/modelos-ficha/${m.id}`)}
        tituloCartao={(m) => m.nome}
        vazioTitulo="Nenhum modelo encontrado"
        vazioDescricao="Ajuste os filtros ou clone um dos modelos do sistema."
        acoes={(m) =>
          /* Do sistema não se inativa: o servidor responde 404, e oferecer o
             botão só para ele falhar é pior do que não oferecer. */
          m.doSistema ? null : (
            <TButton
              variant="ghost"
              size="sm"
              onClick={(e) => {
                e.stopPropagation()
                void alternarAtivo(m)
              }}
            >
              {m.ativo ? 'Inativar' : 'Reativar'}
            </TButton>
          )
        }
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}

/** Perguntas que a ficha realmente faz — a desativada continua no cadastro. */
function ativas(modelo: ModeloFichaResponse): number {
  return modelo.campos.filter((c) => c.ativo).length
}

function secoesDe(modelo: ModeloFichaResponse): string[] {
  const vistas: string[] = []
  modelo.campos
    .filter((c) => c.ativo && c.secao)
    .forEach((c) => {
      if (c.secao && !vistas.includes(c.secao)) vistas.push(c.secao)
    })
  return vistas
}
