import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { IconAdicionar } from '@/assets/icons'
import {
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
} from '@/components/common'
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
        <TButton onClick={() => navigate('/app/pediatria/avaliacoes/nova')}>
          <IconAdicionar className="size-4" />
          Nova avaliação
        </TButton>
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
            inputMode="numeric"
            value={mesesMin}
            onChange={(e) => {
              setMesesMin(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="Idade até"
            suffix="meses"
            inputMode="numeric"
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
