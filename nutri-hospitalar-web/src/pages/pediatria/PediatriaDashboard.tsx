import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TPage, TPanel, TSelect, type OpcaoSelect } from '@/components/common'
import {
  AvaliacoesPorPeriodo,
  BarrasFaixaEtaria,
  BarrasNominais,
  DistribuicaoClassificacoes,
  ProporcaoSexo,
} from '@/components/pediatria/graficos/GraficosGerenciais'
import { TituloGrafico } from '@/components/pediatria/graficos/chrome'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { formulaLacteaService, pediatriaService } from '@/services/pediatriaService'
import type { DashboardGeral } from '@/types/pediatria'
import { OPCOES_SEXO } from '@/types/pessoa'
import { formatarNumero } from '@/utils/format'

const PERIODOS = [
  { valor: '30', rotulo: 'Últimos 30 dias' },
  { valor: '90', rotulo: 'Últimos 90 dias' },
  { valor: '365', rotulo: 'Último ano' },
  { valor: '0', rotulo: 'Todo o histórico' },
]

/**
 * Visão gerencial da pediatria.
 *
 * <p>Os indicadores vêm em cartões, não em gráfico: um número solto não vira
 * barra. Os gráficos abaixo respondem todos à <b>mesma linha de filtros</b>, no
 * topo — filtro dentro de card faz parecer que vale só para aquele gráfico.
 */
export function PediatriaDashboard() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [dias, setDias] = useState('365')
  const [formulaLacteaId, setFormulaLacteaId] = useState('')
  const [sexo, setSexo] = useState('')
  const [formulas, setFormulas] = useState<OpcaoSelect[]>([])
  const [dados, setDados] = useState<DashboardGeral>()
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    formulaLacteaService
      .select()
      .then((fs) => setFormulas(fs.map((f) => ({ valor: f.id, rotulo: f.nome }))))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    setCarregando(true)
    pediatriaService
      .dashboard({
        dias: Number(dias),
        formulaLacteaId: formulaLacteaId || undefined,
        sexo: sexo || undefined,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, dias, formulaLacteaId, sexo])

  useEffect(carregar, [carregar])

  return (
    <TPage
      title="Pediatria em números"
      subtitle="Como está o acompanhamento nutricional das crianças no período."
    >
      <div className="flex flex-col gap-4">
        <TPanel>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <TSelect
              label="Período"
              opcoes={PERIODOS}
              value={dias}
              onChange={(e) => setDias(e.target.value)}
            />
            <TSelect
              label="Fórmula láctea"
              vazio="Todas"
              opcoes={formulas}
              value={formulaLacteaId}
              onChange={(e) => setFormulaLacteaId(e.target.value)}
            />
            <TSelect
              label="Sexo"
              vazio="Todos"
              opcoes={OPCOES_SEXO}
              value={sexo}
              onChange={(e) => setSexo(e.target.value)}
            />
          </div>
        </TPanel>

        {!dados ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">Carregando…</p>
          </TPanel>
        ) : (
          <div className={carregando ? 'flex flex-col gap-4 opacity-50 transition-opacity' : 'flex flex-col gap-4'}>
            {/* ─── Indicadores ─────────────────────────────────────── */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Kpi rotulo="Avaliações" valor={String(dados.totalAvaliacoes)} nota="no período" />
              <Kpi rotulo="Crianças" valor={String(dados.totalPacientes)} nota="acompanhadas" />
              <Kpi rotulo="Avaliações no mês" valor={String(dados.avaliacoesMes)} nota="mês atual" />
              <Kpi
                rotulo="Idade média"
                valor={valor(dados.idadeMediaMeses, 1)}
                nota="meses de vida"
              />
              <Kpi rotulo="Peso médio" valor={valor(dados.pesoMedio, 2, 'kg')} nota="" />
              <Kpi rotulo="IMC médio" valor={valor(dados.imcMedio, 2)} nota="entre quem tem estatura" />
              <Kpi
                rotulo="IMC adequado"
                valor={valor(dados.percImcAdequado, 1, '%')}
                nota="das avaliações classificadas"
              />
              <Kpi
                rotulo="Cobertura calórica"
                valor={valor(dados.coberturaCaloricaMedia, 1, '%')}
                nota="média da dieta prescrita"
              />
            </div>

            {/* ─── Evolução ────────────────────────────────────────── */}
            <TPanel>
              <TituloGrafico>Avaliações por mês</TituloGrafico>
              <AvaliacoesPorPeriodo dados={dados.porPeriodo} />
            </TPanel>

            {/* ─── Estado nutricional ──────────────────────────────── */}
            <TPanel>
              <TituloGrafico referencia="Faixas da OMS · azul abaixo, verde adequado, laranja acima">
                Estado nutricional
              </TituloGrafico>
              <DistribuicaoClassificacoes
                peso={dados.classifPesoIdade}
                estatura={dados.classifEstaturaIdade}
                imc={dados.classifImcIdade}
              />
            </TPanel>

            <div className="grid gap-4 lg:grid-cols-2">
              <TPanel>
                <TituloGrafico>Fórmulas mais prescritas</TituloGrafico>
                <BarrasNominais dados={dados.porFormula} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="Acima de 60 meses fica fora das curvas da OMS">
                  Faixa etária
                </TituloGrafico>
                <BarrasFaixaEtaria dados={dados.porFaixaEtaria} />
              </TPanel>

              <TPanel>
                <TituloGrafico>Sexo</TituloGrafico>
                <ProporcaoSexo dados={dados.porSexo} />
              </TPanel>

              {/* Ranking é lista, não gráfico: dez nomes viram dez cores à toa. */}
              <TPanel>
                <TituloGrafico>Crianças mais avaliadas</TituloGrafico>
                {dados.pacientesMaisAvaliados.length === 0 ? (
                  <p className="text-body text-txt-secondary">Sem avaliações no período.</p>
                ) : (
                  <ol className="flex flex-col divide-y divide-line">
                    {dados.pacientesMaisAvaliados.map((p, i) => (
                      <li key={p.pacienteId} className="flex items-baseline gap-3 py-2">
                        <span className="numeric w-5 text-caption text-txt-muted">{i + 1}</span>
                        <button
                          type="button"
                          onClick={() =>
                            navigate(
                              `/app/pediatria/painel-paciente?pacienteId=${p.pacienteId}`,
                            )
                          }
                          className="truncate text-left text-body text-primary hover:underline"
                        >
                          {p.pacienteNome}
                        </button>
                        <span className="numeric ml-auto text-caption text-txt-secondary">
                          {p.avaliacoes}
                        </span>
                      </li>
                    ))}
                  </ol>
                )}
              </TPanel>
            </div>
          </div>
        )}
      </div>
    </TPage>
  )
}

// ─────────────────────────────────────────────────────────────────────

function Kpi({ rotulo, valor, nota }: { rotulo: string; valor: string; nota: string }) {
  return (
    <TPanel>
      <div className="flex flex-col gap-1">
        <span className="text-caption text-txt-secondary">{rotulo}</span>
        {/* Figuras proporcionais no número grande — tabular afrouxa o traço */}
        <span className="text-display font-semibold text-txt">{valor}</span>
        {nota && <span className="text-caption text-txt-muted">{nota}</span>}
      </div>
    </TPanel>
  )
}

function valor(v: number | null | undefined, casas: number, unidade = ''): string {
  if (v == null) return '—'
  return `${formatarNumero(v, casas)}${unidade}`
}
