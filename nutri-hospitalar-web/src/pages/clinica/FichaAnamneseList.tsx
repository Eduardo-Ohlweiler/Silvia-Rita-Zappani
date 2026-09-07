import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { IconAdicionar } from '@/assets/icons'
import {
  TAcoesDeExportacao,
  TBadge,
  TButton,
  TCombo,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TPage,
  TPanel,
  type Coluna,
  type PaginaDaCarga,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { fichaAnamneseService, modeloFichaService } from '@/services/clinicaService'
import { pessoaService } from '@/services/pessoaService'
import type { Page } from '@/types/comum'
import type { FichaAnamneseLista } from '@/types/clinica'
import { formatarData, formatarDocumento } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

/**
 * Fichas de anamnese preenchidas.
 *
 * <p>O `modeloNome` de cada linha vem do <b>retrato</b> gravado na ficha: a
 * lista continua dizendo de qual modelo cada ficha veio mesmo depois de ele ser
 * renomeado ou excluído.
 */
export function FichaAnamneseList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [pacienteId, setPacienteId] = useState('')
  const [modeloId, setModeloId] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<FichaAnamneseLista>>()
  const [carregando, setCarregando] = useState(true)
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<FichaAnamneseLista>>()
  const [tipoPacienteId, setTipoPacienteId] = useState<string>()

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id))
      .catch(handleApiError)
  }, [])

  const filtros = useCallback(
    (page: number, size: number) => ({
      pacienteId: pacienteId || undefined,
      modeloId: modeloId || undefined,
      de: de || undefined,
      ate: ate || undefined,
      page,
      size,
    }),
    [pacienteId, modeloId, de, ate],
  )

  const carregar = useCallback(() => {
    setCarregando(true)
    fichaAnamneseService
      .getAll(filtros(pagina, 20))
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, filtros, pagina])

  useEffect(carregar, [carregar])

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<FichaAnamneseLista>> => {
      const p = await fichaAnamneseService.getAll(filtros(indice, limite))
      return { linhas: p.content, total: p.totalElements }
    },
    [filtros],
  )

  const colunasExportadas: ColunaExportavel<FichaAnamneseLista>[] = [
    { titulo: 'Data', valor: (f) => formatarData(f.dataPreenchimento) },
    { titulo: 'Paciente', valor: (f) => f.pacienteNome },
    { titulo: 'Profissional', valor: (f) => f.profissionalNome ?? 'Não informado' },
    { titulo: 'Modelo', valor: (f) => f.modeloNome },
    { titulo: 'Respondidas', numerica: true, valor: (f) => String(f.respondidas) },
    { titulo: 'Perguntas', numerica: true, valor: (f) => String(f.totalPerguntas) },
  ]

  const filtrosAplicados = [
    { rotulo: 'Período', valor: periodo(de, ate) },
    { rotulo: 'Paciente', valor: pacienteId ? 'Um paciente' : '' },
    { rotulo: 'Modelo', valor: modeloId ? 'Um modelo' : '' },
  ]

  const colunas: Coluna<FichaAnamneseLista>[] = [
    {
      chave: 'data',
      cabecalho: 'Data',
      render: (f) => formatarData(f.dataPreenchimento),
    },
    { chave: 'paciente', cabecalho: 'Paciente', render: (f) => f.pacienteNome },
    {
      chave: 'modelo',
      cabecalho: 'Modelo',
      secundaria: true,
      render: (f) => f.modeloNome,
    },
    {
      chave: 'profissional',
      cabecalho: 'Profissional',
      secundaria: true,
      render: (f) =>
        f.profissionalNome ?? <span className="text-txt-muted">Não informado</span>,
    },
    {
      chave: 'preenchimento',
      cabecalho: 'Preenchimento',
      numerica: true,
      /* Distingue a ficha completa da que ficou pela metade sem abrir. */
      render: (f) =>
        f.respondidas === f.totalPerguntas ? (
          <TBadge tom="sucesso">{`${f.respondidas} de ${f.totalPerguntas}`}</TBadge>
        ) : (
          <TBadge tom="alerta">{`${f.respondidas} de ${f.totalPerguntas}`}</TBadge>
        ),
    },
  ]

  return (
    <TPage
      title="Fichas de anamnese"
      subtitle="O que o paciente contou — alergia, intolerância, hábito, objetivo. Cada ficha guarda as perguntas como foram feitas no dia."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Fichas de anamnese"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/clinica/fichas/nova')}>
            <IconAdicionar className="size-4" />
            Nova ficha
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Fichas de anamnese"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(f) => f.id}
            resumo={[
              { rotulo: 'Fichas', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Completas',
                valor: String(
                  paraImprimir.linhas.filter((f) => f.respondidas === f.totalPerguntas).length,
                ),
              },
              {
                rotulo: 'Pacientes',
                valor: String(new Set(paraImprimir.linhas.map((f) => f.pacienteId)).size),
              },
            ]}
            nota="O modelo indicado é o que foi usado no dia do preenchimento, guardado com a ficha — ele não muda se o modelo for renomeado ou excluído depois."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <TCombo
            label="Paciente"
            vazio="Todos"
            placeholder="Buscar por nome ou documento"
            value={pacienteId}
            onChange={(v) => {
              setPacienteId(v)
              setPagina(0)
            }}
            buscar={(termo) =>
              pessoaService.select(termo, tipoPacienteId).then((ps) =>
                ps.map((p) => ({
                  id: p.id,
                  nome: p.documento ? `${p.nome} (${formatarDocumento(p.documento)})` : p.nome,
                })),
              )
            }
          />
          <TCombo
            label="Modelo"
            vazio="Todos"
            placeholder="Buscar por nome"
            value={modeloId}
            onChange={(v) => {
              setModeloId(v)
              setPagina(0)
            }}
            buscar={modeloFichaService.selectParaCombo}
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
        chaveDe={(f) => f.id}
        carregando={carregando}
        onLinhaClick={(f) => navigate(`/app/clinica/fichas/${f.id}`)}
        tituloCartao={(f) => f.pacienteNome}
        vazioTitulo="Nenhuma ficha encontrada"
        vazioDescricao="Ajuste os filtros ou preencha a primeira."
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}

function periodo(de: string, ate: string): string {
  if (de && ate) return `${formatarData(de)} a ${formatarData(ate)}`
  if (de) return `a partir de ${formatarData(de)}`
  if (ate) return `até ${formatarData(ate)}`
  return ''
}
