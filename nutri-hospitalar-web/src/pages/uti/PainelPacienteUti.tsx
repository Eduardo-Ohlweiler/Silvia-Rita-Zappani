import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  TBadge,
  TBotaoImprimir,
  TButton,
  TCombo,
  TPage,
  TPanel,
  TSelect,
  TTabPanel,
  TTabs,
  type Aba,
} from '@/components/common'
import { Indicador } from '@/components/graficos/chrome'
import {
  AdequacaoCbNoTempo,
  ImcComFaixas,
  MetasNoTempo,
  OfertaPorQuilo,
  PesoNoTempo,
} from '@/components/uti/graficos/GraficosPaciente'
import { DocumentoPainelPaciente } from '@/components/uti/impressao/DocumentoPainelPaciente'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { utiPainelService } from '@/services/utiService'
import type { ClassificacaoUti, PainelPacienteUti as Painel, TomResultado } from '@/types/uti'
import { formatarData, formatarDocumento, formatarNumero } from '@/utils/format'

const TOM_BADGE: Record<TomResultado, 'neutro' | 'sucesso' | 'alerta' | 'erro'> = {
  NEUTRO: 'neutro',
  ADEQUADO: 'sucesso',
  ATENCAO: 'alerta',
  CRITICO: 'erro',
}

const PERIODOS = [
  { valor: '0', rotulo: 'Todo o histórico' },
  { valor: '30', rotulo: 'Últimos 30 dias' },
  { valor: '90', rotulo: 'Últimos 90 dias' },
  { valor: '365', rotulo: 'Último ano' },
]

/**
 * O paciente de UTI no tempo.
 *
 * Em abas, como a tela de cálculo: seis blocos empilhados obrigariam a rolar
 * para comparar o topo com o rodapé. Os filtros ficam **acima das abas**, numa
 * linha só — todo o painel responde ao mesmo recorte, e filtro dentro de card
 * faz o leitor achar que vale só para aquele gráfico (`docs/05 §10.6`).
 *
 * Nada nesta tela recalcula: cada ponto é o que a avaliação daquele dia gravou.
 */
export function PainelPacienteUti() {
  const navigate = useNavigate()
  const { sessao } = useAuth()
  // O ranking do painel gerencial linka para cá com o paciente na URL.
  const [parametros] = useSearchParams()

  const [pacienteId, setPacienteId] = useState(parametros.get('pacienteId') ?? '')
  const [pacienteRotulo, setPacienteRotulo] = useState('')
  const [dias, setDias] = useState('0')
  const [aba, setAba] = useState('resumo')

  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [dados, setDados] = useState<Painel>()
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    if (!pacienteId) {
      setDados(undefined)
      return
    }
    setCarregando(true)
    utiPainelService
      .painelPaciente(pacienteId, Number(dias))
      .then((painel) => {
        setDados(painel)
        setPacienteRotulo(painel.pacienteNome)
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, pacienteId, dias])

  useEffect(carregar, [carregar])

  const abas: Aba[] = [
    { id: 'resumo', rotulo: 'Resumo' },
    { id: 'antropometria', rotulo: 'Antropometria' },
    { id: 'metas', rotulo: 'Metas e oferta' },
    { id: 'historico', rotulo: 'Histórico' },
  ]

  const ultima = dados?.ultima
  const antropometria = ultima?.resultado.antropometria
  const necessidades = ultima?.resultado.necessidades

  return (
    <TPage
      title="Painel do paciente"
      subtitle="Onde o paciente está hoje e por onde a terapia passou."
      actions={dados && <TBotaoImprimir rotulo="Imprimir histórico" />}
      documento={dados && <DocumentoPainelPaciente dados={dados} />}
    >
      <div className="flex flex-col gap-4">
        {/* Filtro é interface: some no papel (docs/05 §8). */}
        <TPanel className="nao-imprime">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <TCombo
              label="Paciente"
              className="lg:col-span-2"
              placeholder="Buscar por nome ou documento"
              value={pacienteId}
              rotuloInicial={pacienteRotulo}
              onChange={setPacienteId}
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
              label="Período"
              opcoes={PERIODOS}
              value={dias}
              onChange={(e) => setDias(e.target.value)}
            />
          </div>
        </TPanel>

        {!pacienteId ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">
              Escolha um paciente para ver o acompanhamento.
            </p>
          </TPanel>
        ) : !dados ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">Carregando…</p>
          </TPanel>
        ) : (
          <>
            <TTabs abas={abas} ativa={aba} onChange={setAba} />

            {/* Enquanto refaz a consulta, o desenho anterior fica esmaecido —
                pular para um esqueleto faria a página saltar. */}
            <div className={carregando ? 'opacity-50 transition-opacity' : undefined}>
              {/* ─── Resumo ───────────────────────────────────────── */}
              <TTabPanel id="resumo" ativa={aba}>
                {/* Cartão por indicador, com a nota do que ele significa — é o
                    padrão de `PediatriaDashboard`. Oito números apertados numa
                    linha viram tabela; em cartão cada um respira e ganha a
                    ressalva que o torna legível sem consultar o documento. */}
                <div className="mb-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <Kpi rotulo="Idade" valor={anos(dados.idadeAnosAtual)} nota="hoje" />
                  <Kpi
                    rotulo="Avaliações"
                    valor={String(dados.totalAvaliacoes)}
                    nota={
                      dados.primeiraAvaliacao
                        ? `desde ${formatarData(dados.primeiraAvaliacao)}`
                        : 'no período'
                    }
                  />
                  <Kpi
                    rotulo="Peso de trabalho"
                    valor={num(antropometria?.pesoDeTrabalhoKg, 'kg', 2)}
                    nota={antropometria?.pesoDeTrabalhoOrigem ?? 'sem peso definido'}
                  />
                  <Kpi
                    rotulo="IMC"
                    valor={num(antropometria?.imc, '', 2)}
                    nota={antropometria?.classificacaoImcOms?.rotulo ?? 'sem peso ou altura'}
                  />
                  <Kpi
                    rotulo="Meta energética"
                    valor={num(necessidades?.metaEnergetica, 'kcal/dia', 0)}
                    nota={necessidades?.metaEnergeticaOrigem ?? 'não calculada'}
                  />
                  <Kpi
                    rotulo="Meta proteica"
                    valor={num(necessidades?.metaProteica, 'g/dia', 1)}
                    nota={necessidades?.metaProteicaOrigem ?? 'não calculada'}
                  />
                  <Kpi
                    rotulo="Energia por quilo"
                    valor={num(ultima?.resultado.dieta.caloriasPorQuilo, 'kcal/kg', 1)}
                    nota="o que a dieta prescrita entrega"
                  />
                  <Kpi
                    rotulo="Dias registrados"
                    valor={String(dados.totalDiasRegistrados)}
                    nota={
                      dados.ultimoDia
                        ? `último em ${formatarData(dados.ultimoDia)}`
                        : 'nenhum acompanhamento'
                    }
                  />
                </div>

                <TPanel>
                  {dados.totalDiasRegistrados > 0 && (
                    <div className="mb-4">
                      <TButton
                        variant="secondary"
                        onClick={() =>
                          navigate(`/app/uti/painel-acompanhamento?pessoaId=${dados.pacienteId}`)
                        }
                      >
                        Ver os {dados.totalDiasRegistrados} dias de acompanhamento
                      </TButton>
                    </div>
                  )}

                  {ultima ? (
                    <>
                      <hr className="my-5 border-line" />
                      <h3 className="mb-4 text-h3 font-medium text-txt">
                        Última avaliação{' '}
                        <span className="text-caption font-normal text-txt-muted">
                          {formatarData(ultima.dataAvaliacao)}
                        </span>
                      </h3>

                      <div className="mb-5 flex flex-wrap gap-x-5 gap-y-2">
                        <Classificacao rotulo="IMC (OMS)" c={antropometria?.classificacaoImcOms} />
                        <Classificacao
                          rotulo="IMC (OPAS, idoso)"
                          c={antropometria?.classificacaoImcOpas}
                        />
                        <Classificacao rotulo="Perda de peso" c={antropometria?.classificacaoPerdaPeso} />
                        <Classificacao
                          rotulo="Circ. do braço"
                          c={antropometria?.classificacaoAdequacaoCircBraco}
                        />
                      </div>

                      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                        <Indicador
                          rotulo="Peso de trabalho"
                          valor={num(antropometria?.pesoDeTrabalhoKg, 'kg', 2)}
                        />
                        <Indicador rotulo="IMC" valor={num(antropometria?.imc, '', 2)} />
                        <Indicador
                          rotulo="Meta energética"
                          valor={num(necessidades?.metaEnergetica, 'kcal/dia', 0)}
                        />
                        <Indicador
                          rotulo="Meta proteica"
                          valor={num(necessidades?.metaProteica, 'g/dia', 1)}
                        />
                      </div>

                      <div className="mt-4">
                        <TButton
                          variant="secondary"
                          onClick={() => navigate(`/app/uti/avaliacoes/${ultima.id}`)}
                        >
                          Abrir a avaliação
                        </TButton>
                      </div>
                    </>
                  ) : (
                    <p className="mt-6 text-body text-txt-secondary">
                      Este paciente ainda não tem avaliação de UTI no período escolhido.
                    </p>
                  )}
                </TPanel>
              </TTabPanel>

              {/* ─── Antropometria ────────────────────────────────── */}
              <TTabPanel id="antropometria" ativa={aba}>
                <div className="flex flex-col gap-4">
                  {/* As três estimativas ao lado do peso adotado. Não é gráfico:
                      são quatro números de um instante só, e barra de quatro
                      categorias sem série no tempo é decoração. */}
                  {antropometria && (
                    <TPanel title="De onde saiu o peso e a altura">
                      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                        <Indicador
                          rotulo="Adotado"
                          valor={num(antropometria.pesoDeTrabalhoKg, 'kg', 2)}
                        />
                        <Indicador
                          rotulo="Chumlea"
                          valor={num(antropometria.pesoChumleaKg, 'kg', 2)}
                        />
                        <Indicador rotulo="Jung" valor={num(antropometria.pesoJungKg, 'kg', 2)} />
                        <Indicador
                          rotulo="Rabito"
                          valor={num(antropometria.pesoRabitoKg, 'kg', 2)}
                        />
                      </div>
                      <p className="mt-3 text-caption text-txt-muted">
                        {antropometria.pesoDeTrabalhoOrigem
                          ? `O cálculo usou: ${antropometria.pesoDeTrabalhoOrigem}.`
                          : (antropometria.motivoPesoDeTrabalho ?? '')}
                        {antropometria.motivoEstimativas
                          ? ` ${antropometria.motivoEstimativas}.`
                          : ''}
                      </p>
                      <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                        <Indicador
                          rotulo="Altura usada"
                          valor={num(antropometria.alturaUsadaCm, 'cm', 1)}
                        />
                        <Indicador
                          rotulo="Peso ideal"
                          valor={num(antropometria.pesoIdealKg, 'kg', 2)}
                        />
                        <Indicador
                          rotulo="Peso ajustado"
                          valor={num(antropometria.pesoAjustadoKg, 'kg', 2)}
                        />
                        <Indicador
                          rotulo="Corrigido por amputação"
                          valor={num(antropometria.pesoCorrigidoAmputacaoKg, 'kg', 2)}
                        />
                      </div>
                    </TPanel>
                  )}

                  <TPanel>
                    <PesoNoTempo evolucao={dados.evolucao} />
                  </TPanel>
                  <TPanel>
                    <ImcComFaixas evolucao={dados.evolucao} />
                  </TPanel>
                  <TPanel>
                    <AdequacaoCbNoTempo evolucao={dados.evolucao} />
                  </TPanel>
                </div>
              </TTabPanel>

              {/* ─── Metas e oferta ───────────────────────────────── */}
              <TTabPanel id="metas" ativa={aba}>
                <div className="flex flex-col gap-4">
                  <TPanel>
                    <OfertaPorQuilo evolucao={dados.evolucao} />
                  </TPanel>
                  <TPanel>
                    <MetasNoTempo evolucao={dados.evolucao} />
                  </TPanel>
                </div>
              </TTabPanel>

              {/* ─── Histórico ────────────────────────────────────── */}
              <TTabPanel id="historico" ativa={aba}>
                <div className="flex flex-col gap-4">
                  <TPanel title="Fórmulas usadas">
                    {dados.historicoFormulas.length === 0 ? (
                      <p className="py-4 text-body text-txt-secondary">
                        Nenhuma avaliação com fórmula enteral escolhida no período.
                      </p>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[420px] text-body">
                          <thead>
                            <tr className="border-b border-line text-caption text-txt-secondary">
                              <th className="px-2 py-2 text-left font-normal">Fórmula</th>
                              <th className="px-2 py-2 text-right font-normal">Avaliações</th>
                              <th className="px-2 py-2 text-right font-normal">1º uso</th>
                              <th className="px-2 py-2 text-right font-normal">Último uso</th>
                            </tr>
                          </thead>
                          <tbody>
                            {dados.historicoFormulas.map((f) => (
                              <tr key={f.formulaNome} className="border-b border-line last:border-0">
                                <td className="px-2 py-2">{f.formulaNome}</td>
                                <td className="numeric px-2 py-2 text-right">{f.avaliacoes}</td>
                                <td className="px-2 py-2 text-right">{formatarData(f.primeiroUso)}</td>
                                <td className="px-2 py-2 text-right">{formatarData(f.ultimoUso)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </TPanel>

                  {/* A tabela equivalente ao gráfico, que `docs/05 §10.6` exige:
                      quem não lê o desenho tem de ter o número. */}
                  <TPanel title="Avaliações do período">
                    {dados.evolucao.length === 0 ? (
                      <p className="py-4 text-body text-txt-secondary">
                        Nenhuma avaliação no período escolhido.
                      </p>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[640px] text-body">
                          <thead>
                            <tr className="border-b border-line text-caption text-txt-secondary">
                              <th className="px-2 py-2 text-left font-normal">Data</th>
                              <th className="px-2 py-2 text-right font-normal">Peso</th>
                              <th className="px-2 py-2 text-right font-normal">IMC</th>
                              <th className="px-2 py-2 text-left font-normal">Classificação</th>
                              <th className="px-2 py-2 text-right font-normal">Meta kcal</th>
                              <th className="px-2 py-2 text-right font-normal">kcal/kg</th>
                              <th className="px-2 py-2 text-right font-normal">g PTN/kg</th>
                            </tr>
                          </thead>
                          <tbody>
                            {[...dados.evolucao].reverse().map((p) => (
                              <tr key={p.dataAvaliacao} className="border-b border-line last:border-0">
                                <td className="px-2 py-2">{formatarData(p.dataAvaliacao)}</td>
                                <td className="numeric px-2 py-2 text-right">
                                  {num(p.pesoTrabalhoKg, '', 2)}
                                </td>
                                <td className="numeric px-2 py-2 text-right">{num(p.imc, '', 2)}</td>
                                <td className="px-2 py-2">{p.classifImcOms ?? '—'}</td>
                                <td className="numeric px-2 py-2 text-right">
                                  {num(p.metaEnergetica, '', 0)}
                                </td>
                                <td className="numeric px-2 py-2 text-right">
                                  {num(p.caloriasPorQuilo, '', 1)}
                                </td>
                                <td className="numeric px-2 py-2 text-right">
                                  {num(p.proteinaPorQuilo, '', 2)}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </TPanel>
                </div>
              </TTabPanel>
            </div>
          </>
        )}
      </div>
    </TPage>
  )
}

// ─────────────────────────────────────────────────────────────────────

/**
 * Um indicador em cartão, com a nota que o torna legível.
 *
 * A nota não é enfeite: "68 kg" sozinho não diz se veio da balança ou de uma
 * equação de estimativa, e essa diferença muda a confiança na prescrição.
 */
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

function Classificacao({ rotulo, c }: { rotulo: string; c?: ClassificacaoUti | null }) {
  return (
    <span className="flex items-center gap-2">
      <span className="text-caption text-txt-secondary">{rotulo}:</span>
      {c ? (
        <TBadge tom={TOM_BADGE[c.tom]}>{c.rotulo}</TBadge>
      ) : (
        <span className="text-caption text-txt-muted">—</span>
      )}
    </span>
  )
}

function anos(valor?: number | null): string {
  if (valor == null) return '—'
  return `${valor} ${valor === 1 ? 'ano' : 'anos'}`
}

function num(valor: number | null | undefined, unidade: string, casas: number): string {
  if (valor == null) return '—'
  return `${formatarNumero(valor, casas)}${unidade ? ` ${unidade}` : ''}`
}
