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
  TSelect,
  type Coluna,
  type OpcaoSelect,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { formulaLacteaService, pediatriaService } from '@/services/pediatriaService'
import { pessoaService } from '@/services/pessoaService'
import type { Page } from '@/types/comum'
import type { AvaliacaoPediatricaLista, FaixaOms } from '@/types/pediatria'
import {
  formatarData,
  formatarDocumento,
  formatarNumero,
  paraNumero,
} from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

/** Faixa vira tom de badge. O texto vem do backend, junto da faixa. */
const TOM_FAIXA: Record<FaixaOms, 'info' | 'sucesso' | 'alerta'> = {
  BAIXA: 'info',
  ADEQUADA: 'sucesso',
  ALTA: 'alerta',
}

export function AvaliacaoPediatricaList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [pacienteId, setPacienteId] = useState('')
  const [formulaLacteaId, setFormulaLacteaId] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [mesesMin, setMesesMin] = useState('')
  const [mesesMax, setMesesMax] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<AvaliacaoPediatricaLista>>()
  const [formulas, setFormulas] = useState<OpcaoSelect[]>([])
  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [carregando, setCarregando] = useState(true)
  const [paraImprimir, setParaImprimir] =
    useState<ResultadoDaCarga<AvaliacaoPediatricaLista>>()

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id))
      .catch(handleApiError)

    formulaLacteaService
      .select()
      .then((fs) => setFormulas(fs.map((f) => ({ valor: f.id, rotulo: f.nome }))))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    setCarregando(true)
    pediatriaService
      .getAll({
        pacienteId: pacienteId || undefined,
        formulaLacteaId: formulaLacteaId || undefined,
        de: de || undefined,
        ate: ate || undefined,
        mesesMin: paraNumero(mesesMin),
        mesesMax: paraNumero(mesesMax),
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com os dados do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, pacienteId, formulaLacteaId, de, ate, mesesMin, mesesMax, pagina])

  useEffect(carregar, [carregar])

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number): Promise<ResultadoDaCarga<AvaliacaoPediatricaLista>> => {
      const pagina = await pediatriaService.getAll({
        pacienteId: pacienteId || undefined,
        formulaLacteaId: formulaLacteaId || undefined,
        de: de || undefined,
        ate: ate || undefined,
        mesesMin: paraNumero(mesesMin),
        mesesMax: paraNumero(mesesMax),
        page: 0,
        size: limite,
      })
      return {
        linhas: pagina.content,
        restantes: Math.max(0, pagina.totalElements - pagina.content.length),
      }
    },
    [pacienteId, formulaLacteaId, de, ate, mesesMin, mesesMax],
  )

  /**
   * O resumo de uma criança no papel: quem, quando, com que idade, em que
   * estado nutricional e com qual fórmula. As mesmas colunas alimentam o CSV.
   */
  const colunasExportadas: ColunaExportavel<AvaliacaoPediatricaLista>[] = [
    { titulo: 'Paciente', valor: (a) => a.pacienteNome },
    { titulo: 'Data', valor: (a) => formatarData(a.dataAvaliacao) },
    {
      titulo: 'Idade (meses)',
      numerica: true,
      valor: (a) => String(a.idadeMeses),
    },
    { titulo: 'Peso (kg)', numerica: true, valor: (a) => txt(a.peso, 2) },
    { titulo: 'IMC', numerica: true, valor: (a) => txt(a.imc, 2) },
    {
      titulo: 'IMC para a idade',
      valor: (a) => a.classifImcIdadeRotulo ?? 'Sem estatura',
    },
    { titulo: 'Fórmula láctea', valor: (a) => a.formulaNome ?? '—' },
  ]

  const filtrosAplicados = [
    {
      rotulo: 'Paciente',
      // O combo devolve só o id. Quando há filtro de paciente, toda linha do
      // relatório é dele — então o nome sai do próprio recorte. Deixar em
      // branco faria a folha dizer "sem filtro aplicado" mostrando um paciente
      // só, que é justamente a mentira que este cabeçalho existe para evitar.
      valor: pacienteId ? (paraImprimir?.linhas[0]?.pacienteNome ?? 'selecionado') : '',
    },
    {
      rotulo: 'Fórmula',
      valor: formulas.find((f) => f.valor === formulaLacteaId)?.rotulo ?? '',
    },
    { rotulo: 'De', valor: de ? formatarData(de) : '' },
    { rotulo: 'Até', valor: ate ? formatarData(ate) : '' },
    { rotulo: 'Idade mínima', valor: mesesMin ? `${mesesMin} meses` : '' },
    { rotulo: 'Idade máxima', valor: mesesMax ? `${mesesMax} meses` : '' },
  ]

  const colunas: Coluna<AvaliacaoPediatricaLista>[] = [
    {
      chave: 'data',
      cabecalho: 'Data',
      render: (a) => formatarData(a.dataAvaliacao),
    },
    {
      chave: 'paciente',
      cabecalho: 'Paciente',
      render: (a) => a.pacienteNome,
    },
    {
      chave: 'idade',
      cabecalho: 'Idade',
      numerica: true,
      render: (a) => `${a.idadeMeses} m`,
    },
    {
      chave: 'peso',
      cabecalho: 'Peso',
      numerica: true,
      render: (a) => `${formatarNumero(a.peso, 3)} kg`,
    },
    {
      chave: 'imc',
      cabecalho: 'IMC',
      numerica: true,
      secundaria: true,
      render: (a) => formatarNumero(a.imc),
    },
    {
      chave: 'classifImc',
      cabecalho: 'IMC / idade',
      render: (a) =>
        a.classifImcIdade ? (
          <TBadge tom={TOM_FAIXA[a.classifImcIdade]}>{a.classifImcIdadeRotulo}</TBadge>
        ) : (
          '—'
        ),
    },
    {
      chave: 'formula',
      cabecalho: 'Fórmula',
      secundaria: true,
      render: (a) => a.formulaNome ?? '—',
    },
  ]

  return (
    <TPage
      title="Avaliações pediátricas"
      subtitle="Estado nutricional pela OMS, necessidades pelas DRIs e adequação da dieta."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Avaliações pediátricas"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/pediatria/avaliacoes/nova')}>
            <IconAdicionar className="size-4" />
            Nova avaliação
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Avaliações pediátricas"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(a) => a.id}
            resumo={[
              { rotulo: 'Avaliações', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Crianças',
                valor: String(new Set(paraImprimir.linhas.map((a) => a.pacienteNome)).size),
              },
            ]}
            nota="Classificação da OMS como foi gravada em cada avaliação — nada é recalculado na emissão."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <TCombo
            label="Paciente"
            vazio="Todos"
            placeholder="Buscar por nome ou documento"
            value={pacienteId}
            onChange={(valor) => {
              setPacienteId(valor)
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
          <TSelect
            label="Fórmula láctea"
            vazio="Todas"
            opcoes={formulas}
            value={formulaLacteaId}
            onChange={(e) => {
              setFormulaLacteaId(e.target.value)
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
          {/* Faixa etária: é assim que se procura "os lactentes" ou "os
              pré-escolares" sem saber o nome de cada um. */}
          <TEntry
            label="Idade de"
            suffix="meses"
            mascara="inteiro"
            value={mesesMin}
            onChange={(e) => {
              setMesesMin(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="Idade até"
            suffix="meses"
            mascara="inteiro"
            value={mesesMax}
            onChange={(e) => {
              setMesesMax(e.target.value)
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
        onLinhaClick={(a) => navigate(`/app/pediatria/avaliacoes/${a.id}`)}
        tituloCartao={(a) => `${a.pacienteNome} — ${formatarData(a.dataAvaliacao)}`}
        vazioTitulo="Nenhuma avaliação encontrada"
        vazioDescricao="Ajuste os filtros ou registre a primeira avaliação."
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}

/** Número em pt-BR, ou traço. */
function txt(valor?: number | null, casas = 1): string {
  return valor == null ? '—' : formatarNumero(valor, casas, casas)
}
