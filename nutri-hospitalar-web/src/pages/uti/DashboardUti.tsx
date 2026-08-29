import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TBotaoImprimir, TPage, TPanel, TSelect } from '@/components/common'
import { Indicador, TituloGrafico } from '@/components/graficos/chrome'
import {
  BarrasNominaisUti,
  DistribuicaoPorTom,
  MovimentoPorPeriodo,
} from '@/components/uti/graficos/GraficosGerenciaisUti'
import { DocumentoDashboardUti } from '@/components/uti/impressao/DocumentoDashboardUti'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { utiPainelService } from '@/services/utiService'
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
  const [dados, setDados] = useState<Dashboard>()
  const [carregando, setCarregando] = useState(false)

  const carregar = useCallback(() => {
    setCarregando(true)
    utiPainelService
      .dashboard({ dias: Number(dias) })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, dias])

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
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TSelect
              label="Período"
              opcoes={PERIODOS}
              value={dias}
              onChange={(e) => setDias(e.target.value)}
            />
          </div>
        </TPanel>

        {!dados ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">Carregando…</p>
          </TPanel>
        ) : (
          <div className={`flex flex-col gap-4 ${carregando ? 'opacity-50 transition-opacity' : ''}`}>
            <TPanel>
              <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                <Indicador rotulo="Avaliações" valor={String(dados.totalAvaliacoes)} />
                <Indicador rotulo="Pacientes" valor={String(dados.totalPacientes)} />
                <Indicador rotulo="No mês corrente" valor={String(dados.avaliacoesMes)} />
                <Indicador rotulo="Dias registrados" valor={String(dados.totalDiasRegistrados)} />
              </div>

              <hr className="my-5 border-line" />

              <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                <Indicador rotulo="Idade média" valor={num(dados.idadeMediaAnos, 'anos', 1)} />
                <Indicador rotulo="Peso médio" valor={num(dados.pesoTrabalhoMedio, 'kg', 2)} />
                <Indicador rotulo="IMC médio" valor={num(dados.imcMedio, '', 2)} />
                <Indicador rotulo="Em eutrofia" valor={num(dados.percEutrofia, '%', 1)} />
                <Indicador
                  rotulo="Meta energética média"
                  valor={num(dados.metaEnergeticaMedia, 'kcal/dia', 0)}
                />
                <Indicador
                  rotulo="Meta proteica média"
                  valor={num(dados.metaProteicaMedia, 'g/dia', 1)}
                />
                <Indicador rotulo="Energia ofertada" valor={num(dados.kcalPorQuiloMedio, 'kcal/kg', 1)} />
                <Indicador rotulo="Adesão média" valor={num(dados.adesaoMedia, '%', 1)} />
              </div>

              <p className="mt-5 text-caption text-txt-muted">
                As médias ignoram a avaliação em que o valor não existe — quem não tinha altura
                não entra na média de IMC. O percentual em eutrofia é sobre as avaliações{' '}
                <b>classificadas</b>: não classificado não é sinônimo de inadequado.
                {dados.avaliacoesObesidade > 0 && (
                  <>
                    {' '}
                    {dados.avaliacoesObesidade === 1
                      ? 'Uma avaliação aplicou'
                      : `${dados.avaliacoesObesidade} avaliações aplicaram`}{' '}
                    a correção de obesidade da ASPEN/SCCM 2016, que muda a base do peso.
                  </>
                )}
              </p>
            </TPanel>

            <TPanel>
              <TituloGrafico referencia="Mês sem movimento aparece com zero — encurtar o intervalo insinuaria atividade que não houve.">
                Movimento por mês
              </TituloGrafico>
              <MovimentoPorPeriodo dados={dados.porPeriodo} />
            </TPanel>

            <div className="grid gap-4 lg:grid-cols-2">
              <TPanel>
                <TituloGrafico referencia="Classificação da OMS 1997, como foi gravada em cada avaliação.">
                  Estado nutricional por IMC
                </TituloGrafico>
                <DistribuicaoPorTom dados={dados.classifImcOms} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="Adequação da circunferência do braço, em seis faixas (docs/10 §2.8).">
                  Adequação da circunferência do braço
                </TituloGrafico>
                <DistribuicaoPorTom dados={dados.classifAdequacaoCb} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="Perda de peso no intervalo declarado, contra o corte de significância.">
                  Perda de peso
                </TituloGrafico>
                <DistribuicaoPorTom dados={dados.classifPerdaPeso} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="Retrato gravado na avaliação — a fórmula pode ter saído do catálogo depois.">
                  Fórmulas prescritas
                </TituloGrafico>
                <BarrasNominaisUti dados={dados.porFormula} />
              </TPanel>

              <TPanel>
                <TituloGrafico>Fase da terapia</TituloGrafico>
                <BarrasNominaisUti dados={dados.porFase} />
              </TPanel>

              <TPanel>
                <TituloGrafico referencia="A terapia renal substitui a meta proteica por 1,8 ou 2,0 g/kg.">
                  Terapia renal substitutiva
                </TituloGrafico>
                <BarrasNominaisUti dados={dados.porTerapiaRenal} />
              </TPanel>

              <TPanel>
                <TituloGrafico>Modo de infusão</TituloGrafico>
                <BarrasNominaisUti dados={dados.porModoInfusao} />
              </TPanel>
            </div>

            <TPanel title="Pacientes mais avaliados">
              {dados.pacientesMaisAvaliados.length === 0 ? (
                <p className="py-4 text-body text-txt-secondary">
                  Nenhuma avaliação no período escolhido.
                </p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[420px] text-body">
                    <thead>
                      <tr className="border-b border-line text-caption text-txt-secondary">
                        <th className="py-2 text-left font-normal">Paciente</th>
                        <th className="py-2 text-right font-normal">Avaliações</th>
                        <th className="py-2 text-right font-normal">Dias</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.pacientesMaisAvaliados.map((p) => (
                        <tr key={p.pacienteId} className="border-b border-line last:border-0">
                          <td className="py-2">
                            <button
                              type="button"
                              className="text-primary hover:underline"
                              onClick={() =>
                                navigate(`/app/uti/painel-paciente?pacienteId=${p.pacienteId}`)
                              }
                            >
                              {p.pacienteNome}
                            </button>
                          </td>
                          <td className="numeric py-2 text-right">{p.avaliacoes}</td>
                          <td className="numeric py-2 text-right">{p.dias}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </TPanel>
          </div>
        )}
      </div>
    </TPage>
  )
}

function num(valor: number | null | undefined, unidade: string, casas: number): string {
  if (valor == null) return '—'
  return `${formatarNumero(valor, casas)}${unidade ? ` ${unidade}` : ''}`
}
