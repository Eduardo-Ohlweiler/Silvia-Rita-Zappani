import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  TBotaoImprimir,
  TPage,
  TPanel,
  TSelect,
  type OpcaoSelect,
} from '@/components/common'
import { TituloGrafico } from '@/components/graficos/chrome'
import {
  AdesaoPorPeriodo,
  AvaliacoesPorPeriodo,
  BarrasNominaisUti,
  ClassificacoesEmpilhadas,
  ColunasFaixaEtaria,
  LinhaDeConduta,
} from '@/components/uti/graficos/GraficosGerenciaisUti'
import { DocumentoDashboardUti } from '@/components/uti/impressao/DocumentoDashboardUti'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { formulaEnteralService, utiPainelService } from '@/services/utiService'
import type { DashboardUti as Dashboard } from '@/types/uti'
import { formatarNumero } from '@/utils/format'

const PERIODOS = [
  { valor: '30', rotulo: 'Últimos 30 dias' },
  { valor: '90', rotulo: 'Últimos 90 dias' },
  { valor: '365', rotulo: 'Último ano' },
  { valor: '0', rotulo: 'Todo o histórico' },
]

/**
 * Terapia nutricional de UTI em números.
 *
 * Uma página rolável, e não abas: aqui o leitor **varre** procurando o que
 * chama atenção, e aba esconderia metade. É o inverso do painel do paciente,
 * onde se compara um bloco com outro.
 *
 * Toda média ignora a avaliação em que o valor não existe — o servidor faz
 * isso, e a nota de cada indicador diz quando importa.
 */
export function DashboardUti() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [dias, setDias] = useState('365')
  const [formulaEnteralId, setFormulaEnteralId] = useState('')
  const [formulas, setFormulas] = useState<OpcaoSelect[]>([])
  const [dados, setDados] = useState<Dashboard>()
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    formulaEnteralService
      .select()
      .then((fs) => setFormulas(fs.map((f) => ({ valor: f.id, rotulo: f.nome }))))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    setCarregando(true)
    utiPainelService
      .dashboard({ dias: Number(dias), formulaEnteralId: formulaEnteralId || undefined })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, dias, formulaEnteralId])

  useEffect(carregar, [carregar])

  return (
    <TPage
      title="Terapia nutricional em números"
      subtitle="Volume de atendimento, perfil dos pacientes e adesão à dieta no período."
      actions={dados && <TBotaoImprimir rotulo="Imprimir demonstrativo" />}
      documento={
        dados && (
          <DocumentoDashboardUti
            dados={dados}
            periodo={PERIODOS.find((p) => p.valor === dias)?.rotulo ?? ''}
          />
        )
      }
    >
      <div className="flex flex-col gap-4">
        {/* Filtro é interface: some no papel (docs/05 §8). */}
        <TPanel className="nao-imprime">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <TSelect
              label="Período"
              opcoes={PERIODOS}
              value={dias}
              onChange={(e) => setDias(e.target.value)}
            />
            <TSelect
              label="Fórmula enteral"
              vazio="Todas"
              opcoes={formulas}
              value={formulaEnteralId}
              onChange={(e) => setFormulaEnteralId(e.target.value)}
            />
          </div>
        </TPanel>

        {!dados ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">Carregando…</p>
          </TPanel>
        ) : (
          <div className={`flex flex-col gap-4 ${carregando ? 'opacity-50 transition-opacity' : ''}`}>
            {/* Um cartão por indicador, com a nota do que ele significa —
                o padrão de `PediatriaDashboard`. A nota carrega a ressalva que
                de outra forma viraria rodapé em letra miúda que ninguém lê. */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Kpi rotulo="Avaliações" valor={String(dados.totalAvaliacoes)} nota="no período" />
              <Kpi rotulo="Pacientes" valor={String(dados.totalPacientes)} nota="acompanhados" />
              <Kpi
                rotulo="Avaliações no mês"
                valor={String(dados.avaliacoesMes)}
                nota="mês corrente"
              />
              <Kpi
                rotulo="Dias de acompanhamento"
                valor={String(dados.totalDiasRegistrados)}
                nota="registros diários"
              />
              <Kpi rotulo="Idade média" valor={num(dados.idadeMediaAnos, 'anos', 1)} />
              <Kpi
                rotulo="Peso médio"
                valor={num(dados.pesoTrabalhoMedio, 'kg', 2)}
                nota="peso de trabalho"
              />
              <Kpi
                rotulo="IMC médio"
                valor={num(dados.imcMedio, '', 2)}
                nota="entre quem tem altura"
              />
              <Kpi
                rotulo="Em eutrofia"
                valor={num(dados.percEutrofia, '%', 1)}
                nota="das avaliações classificadas"
              />
              <Kpi
                rotulo="Meta energética"
                valor={num(dados.metaEnergeticaMedia, 'kcal', 0)}
                nota="média por dia"
              />
              <Kpi
                rotulo="Meta proteica"
                valor={num(dados.metaProteicaMedia, 'g', 1)}
                nota="média por dia"
              />
              <Kpi
                rotulo="Energia prescrita"
                valor={num(dados.kcalPorQuiloMedio, 'kcal/kg', 1)}
                nota="o que a dieta entrega"
              />
              <Kpi
                rotulo="Adesão média"
                valor={num(dados.adesaoMedia, '%', 1)}
                nota="não é nota — a meta é progressiva na 1ª semana"
              />
            </div>

            {dados.avaliacoesObesidade > 0 && (
              <TPanel>
                <p className="text-caption text-txt-secondary">
                  <b>
                    {dados.avaliacoesObesidade === 1
                      ? 'Uma avaliação aplicou'
                      : `${dados.avaliacoesObesidade} avaliações aplicaram`}
                  </b>{' '}
                  a correção de obesidade da ASPEN/SCCM 2016 — nelas a energia sai de 11 a 14
                  kcal/kg de peso <b>atual</b> (IMC 30 a 50) ou de 22 a 25 kcal/kg de peso{' '}
                  <b>ideal</b> (IMC acima de 50), e a fase da terapia não se aplica. Isso puxa a
                  média de energia por quilo para baixo, e é conduta, não desvio.
                </p>
              </TPanel>
            )}

            {/* Largura inteira, como na pediatria: o gráfico do tempo é o
                que dá escala à página, e meia largura o espreme. */}
            <TPanel>
              <TituloGrafico referencia="Mês sem avaliação aparece com zero — encurtar o intervalo insinuaria atividade que não houve.">
                Avaliações por mês
              </TituloGrafico>
              <AvaliacoesPorPeriodo dados={dados.porPeriodo} />
            </TPanel>

            {/* As três réguas sobre a mesma casuística, num quadro só. Três
                cartões estreitos com duas barrinhas cada não deixam comparar. */}
            <TPanel>
              <TituloGrafico referencia="A mesma casuística lida por três réguas: IMC pela OMS 1997, circunferência do braço por Blackburn e Thornton (1979), perda de peso por Blackburn (1977).">
                Estado nutricional
              </TituloGrafico>
              <ClassificacoesEmpilhadas
                imc={dados.classifImcOms}
                circBraco={dados.classifAdequacaoCb}
                perdaPeso={dados.classifPerdaPeso}
              />
            </TPanel>

            <div className="grid gap-4 xl:grid-cols-2">
              <TPanel>
                <TituloGrafico referencia="A linha salta o mês sem dia medido em vez de emendar por cima — emendar inventaria uma adesão que ninguém mediu.">
                  Adesão média por mês
                </TituloGrafico>
                <AdesaoPorPeriodo dados={dados.porPeriodo} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="A partir de 60 anos vale a classificação de IMC da OPAS 2002, com outros cortes que a da OMS.">
                  Faixa etária
                </TituloGrafico>
                <ColunasFaixaEtaria dados={dados.porFaixaEtaria} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="Retrato gravado na avaliação — a fórmula pode ter saído do catálogo depois.">
                  Fórmulas prescritas
                </TituloGrafico>
                <BarrasNominaisUti dados={dados.porFormula} />
              </TPanel>

              {/* Três distribuições curtas num painel só. Cada uma vira frase
                  quando tem uma categoria só — barra de 100 % não informa. */}
              <TPanel>
                <TituloGrafico>Conduta prescrita</TituloGrafico>
                <div className="divide-y divide-line">
                  <LinhaDeConduta titulo="Fase da terapia" dados={dados.porFase} />
                  <LinhaDeConduta
                    titulo="Terapia renal substitutiva"
                    dados={dados.porTerapiaRenal}
                    nota="Substitui a meta proteica por 1,8 g/kg na hemodiálise intermitente e 2,0 na contínua."
                  />
                  <LinhaDeConduta
                    titulo="Modo de infusão"
                    dados={dados.porModoInfusao}
                    nota="Contínua em ml/h por um número de horas; intermitente em ml por horário."
                  />
                </div>
              </TPanel>
            </div>

            {/* Ranking é lista, não gráfico: dez nomes viram dez cores à toa. */}
            <TPanel title="Pacientes mais avaliados">
              {dados.pacientesMaisAvaliados.length === 0 ? (
                <p className="py-4 text-body text-txt-secondary">
                  Nenhuma avaliação no período escolhido.
                </p>
              ) : (
                <ol className="flex flex-col divide-y divide-line">
                  {dados.pacientesMaisAvaliados.map((p, i) => (
                    <li key={p.pacienteId} className="flex items-baseline gap-3 py-2">
                      <span className="numeric w-5 text-caption text-txt-muted">{i + 1}</span>
                      <button
                        type="button"
                        onClick={() =>
                          navigate(`/app/uti/painel-paciente?pacienteId=${p.pacienteId}`)
                        }
                        className="truncate text-left text-body text-primary hover:underline"
                      >
                        {p.pacienteNome}
                      </button>
                      <span className="ml-auto shrink-0 text-caption text-txt-secondary">
                        <span className="numeric">{p.avaliacoes}</span>{' '}
                        {p.avaliacoes === 1 ? 'avaliação' : 'avaliações'}
                        {p.dias > 0 && (
                          <>
                            {' · '}
                            <span className="numeric">{p.dias}</span>{' '}
                            {p.dias === 1 ? 'dia' : 'dias'}
                          </>
                        )}
                      </span>
                    </li>
                  ))}
                </ol>
              )}
            </TPanel>
          </div>
        )}
      </div>
    </TPage>
  )
}

/** Indicador em cartão, com a nota que o torna legível sem consultar o doc. */
function Kpi({ rotulo, valor, nota }: { rotulo: string; valor: string; nota?: string }) {
  return (
    <TPanel>
      <div className="flex flex-col gap-1">
        <span className="text-caption text-txt-secondary">{rotulo}</span>
        <span className="text-display font-semibold text-txt">{valor}</span>
        {nota && <span className="text-caption text-txt-muted">{nota}</span>}
      </div>
    </TPanel>
  )
}

function num(valor: number | null | undefined, unidade: string, casas: number): string {
  if (valor == null) return '—'
  return `${formatarNumero(valor, casas)}${unidade ? ` ${unidade}` : ''}`
}
