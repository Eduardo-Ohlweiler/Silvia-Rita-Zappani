import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBusca } from '@/assets/icons'
import {
  TAcoesDeExportacao,
  TButton,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TModal,
  TPage,
  TPanel,
  type Coluna,
  type PaginaDaCarga,
  type ResultadoDaCarga,
  type TomResultado,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { avaliacaoUtiService } from '@/services/utiService'
import type { Page } from '@/types/comum'
import type { AvaliacaoUtiLista } from '@/types/uti'
import { formatarData, formatarNumero } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

export function AvaliacaoUtiList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<AvaliacaoUtiLista>>()
  const [carregando, setCarregando] = useState(true)
  const [aRemover, setARemover] = useState<AvaliacaoUtiLista>()
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<AvaliacaoUtiLista>>()

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    avaliacaoUtiService
      .getAll({
        pacienteNome: nomeBusca || undefined,
        de: de || undefined,
        ate: ate || undefined,
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com os dados do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, nomeBusca, de, ate, pagina])

  useEffect(carregar, [carregar])

  async function remover() {
    if (!aRemover) return
    try {
      await avaliacaoUtiService.remover(aRemover.id)
      toast.success('Avaliação removida')
      setARemover(undefined)
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  /**
   * O relatório e a planilha cobrem **o filtro inteiro**, não a página aberta.
   * Pede uma linha a mais que o teto: se ela vier, é porque há mais, e o papel
   * diz quantas ficaram de fora em vez de cortar calado.
   */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<AvaliacaoUtiLista>> => {
      const pagina = await avaliacaoUtiService.getAll({
        pacienteNome: nomeBusca || undefined,
        de: de || undefined,
        ate: ate || undefined,
        page: indice,
        size: limite,
      })
      return {
        linhas: pagina.content,
        total: pagina.totalElements,
      }
    },
    [nomeBusca, de, ate],
  )

  /**
   * O que uma linha precisa dizer no papel: quem, quando, em que estado, com
   * que meta e com qual dieta. É o resumo clínico da avaliação — as mesmas
   * colunas alimentam o CSV, então os dois não conseguem divergir.
   */
  const colunasExportadas: ColunaExportavel<AvaliacaoUtiLista>[] = [
    { titulo: 'Paciente', valor: (a) => a.pacienteNome },
    { titulo: 'Data', valor: (a) => formatarData(a.dataAvaliacao) },
    { titulo: 'Profissional', valor: (a) => a.profissionalNome ?? '—' },
    { titulo: 'Peso (kg)', numerica: true, valor: (a) => txt(a.pesoTrabalhoKg, 2) },
    { titulo: 'Origem do peso', valor: (a) => a.pesoTrabalhoOrigem ?? '—' },
    { titulo: 'IMC', numerica: true, valor: (a) => txt(a.imc, 2) },
    { titulo: 'Estado nutricional', valor: (a) => a.classificacaoImc ?? 'Sem peso ou altura' },
    { titulo: 'Meta (kcal/dia)', numerica: true, valor: (a) => txt(a.metaEnergetica, 0) },
    { titulo: 'Fórmula', valor: (a) => a.formulaNome ?? '—' },
  ]

  const filtrosAplicados = [
    { rotulo: 'Paciente', valor: nomeBusca },
    { rotulo: 'De', valor: de ? formatarData(de) : '' },
    { rotulo: 'Até', valor: ate ? formatarData(ate) : '' },
  ]

  const colunas: Coluna<AvaliacaoUtiLista>[] = [
    {
      chave: 'paciente',
      cabecalho: 'Paciente',
      render: (a) => (
        <span className="flex flex-col">
          <span>{a.pacienteNome}</span>
          {a.profissionalNome && (
            <span className="text-caption text-txt-muted">{a.profissionalNome}</span>
          )}
        </span>
      ),
    },
    {
      chave: 'data',
      cabecalho: 'Data',
      render: (a) => formatarData(a.dataAvaliacao),
    },
    {
      chave: 'peso',
      cabecalho: 'Peso considerado',
      numerica: true,
      /*
       * A origem vai junto do número. Sem ela ninguém sabe se a prescrição saiu
       * de um peso medido ou de uma estimativa por circunferências — e é isso
       * que o registro precisa dizer.
       */
      render: (a) =>
        a.pesoTrabalhoKg != null ? (
          <span className="flex flex-col items-end">
            <span>{formatarNumero(a.pesoTrabalhoKg)} kg</span>
            {a.pesoTrabalhoOrigem && (
              <span className="text-caption text-txt-muted">{a.pesoTrabalhoOrigem}</span>
            )}
          </span>
        ) : (
          <span className="text-txt-muted">Não calculado</span>
        ),
    },
    {
      chave: 'imc',
      cabecalho: 'Estado nutricional',
      /* A classificação vem GRAVADA — é o rótulo que se leu no dia. */
      render: (a) =>
        a.classificacaoImc ? (
          <span className="flex flex-col">
            <span className={`font-medium ${COR_TOM[a.tomClassificacao ?? 'NEUTRO']}`}>
              {a.classificacaoImc}
            </span>
            <span className="text-caption text-txt-muted">
              IMC {formatarNumero(a.imc)}
            </span>
          </span>
        ) : (
          <span className="text-txt-muted">Sem peso ou altura</span>
        ),
    },
    {
      chave: 'meta',
      cabecalho: 'Meta e dieta',
      numerica: true,
      render: (a) => (
        <span className="flex flex-col items-end">
          <span>
            {a.metaEnergetica != null
              ? `${formatarNumero(a.metaEnergetica)} kcal/dia`
              : '—'}
          </span>
          {a.formulaNome && (
            <span className="text-caption text-txt-muted">{a.formulaNome}</span>
          )}
        </span>
      ),
    },
  ]

  return (
    <TPage
      title="Avaliações de terapia nutricional"
      subtitle="Cada avaliação guarda os resultados do dia. Abrir uma delas não recalcula."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Avaliações de terapia nutricional"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/uti/avaliacoes/nova')}>
            <IconAdicionar className="size-4" />
            Nova avaliação
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Avaliações de terapia nutricional"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(a) => a.id}
            resumo={[
              { rotulo: 'Avaliações', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Pacientes',
                valor: String(
                  new Set(paraImprimir.linhas.map((a) => a.pacienteNome)).size,
                ),
              },
            ]}
            nota="Classificação e meta como foram gravadas em cada avaliação — nada é recalculado na emissão."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <TEntry
            label="Paciente"
            placeholder="Buscar por nome"
            value={nome}
            suffix={<IconBusca className="size-4" />}
            onChange={(e) => {
              setNome(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="De"
            type="date"
            value={de}
            onChange={(e) => {
              setDe(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="Até"
            type="date"
            value={ate}
            onChange={(e) => {
              setAte(e.target.value)
              setPagina(0)
            }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(a) => a.id}
        carregando={carregando}
        onLinhaClick={(a) => navigate(`/app/uti/avaliacoes/${a.id}`)}
        tituloCartao={(a) => a.pacienteNome}
        vazioTitulo="Nenhuma avaliação encontrada"
        vazioDescricao="Ajuste os filtros ou registre uma avaliação nova."
        acoes={(a) => (
          <TButton
            variant="secondary"
            size="sm"
            onClick={() => setARemover(a)}
            title="Remover esta avaliação"
          >
            Remover
          </TButton>
        )}
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />

      <TModal
        aberto={!!aRemover}
        titulo="Remover avaliação"
        onFechar={() => setARemover(undefined)}
        acoes={
          <>
            <TButton variant="secondary" onClick={() => setARemover(undefined)}>
              Cancelar
            </TButton>
            <TButton onClick={() => void remover()}>Remover</TButton>
          </>
        }
      >
        <p className="text-body text-txt-secondary">
          A avaliação de <strong>{aRemover?.pacienteNome}</strong> em{' '}
          {aRemover && formatarData(aRemover.dataAvaliacao)} será removida. Isso não pode ser
          desfeito.
        </p>
      </TModal>
    </TPage>
  )
}

/** Mesma escala do `TResult` — o tom vem do servidor, gravado na avaliação. */
const COR_TOM: Record<TomResultado, string> = {
  NEUTRO: 'text-txt',
  ADEQUADO: 'text-success',
  ATENCAO: 'text-warning',
  CRITICO: 'text-danger',
}

/** Número em pt-BR, ou traço. No papel o traço é legítimo: não há como perguntar. */
function txt(valor?: number | null, casas = 1): string {
  return valor == null ? '—' : formatarNumero(valor, casas, casas)
}
