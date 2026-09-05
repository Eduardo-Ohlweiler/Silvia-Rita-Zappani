import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  TBotaoImprimir,
  TCombo,
  TEntry,
  TPage,
  TPanel,
  TTabPanel,
  TTabs,
  type Aba,
} from '@/components/common'
import { Indicador } from '@/components/graficos/chrome'
import { AdesaoNoTempo } from '@/components/graficos/MetaVersusOfertado'
import {
  BalancoNoTempo,
  DiureseNoTempo,
  LaboratorioNoTempo,
  PressaoNoTempo,
  VolumeNoTempo,
} from '@/components/uti/graficos/GraficosAcompanhamento'
import { useDiasPlotados } from '@/components/uti/graficos/dias'
import { DocumentoAcompanhamento } from '@/components/uti/impressao/DocumentoAcompanhamento'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { utiPainelService } from '@/services/utiService'
import type { PainelAcompanhamentoUti as Painel } from '@/types/uti'
import { formatarData, formatarDocumento, formatarNumero } from '@/utils/format'

/**
 * Os dias de acompanhamento de um paciente.
 *
 * Quatro abas, porque o conjunto é grande: adesão, laboratório, hemodinâmica e
 * a tabela. Os filtros ficam acima das abas — o recorte vale para tudo.
 *
 * **Nada aqui é nota de desempenho.** A adesão à dieta tem leitura clínica que
 * depende do dia da internação, e a nota sob o gráfico diz isso. Ver
 * `AdesaoNoTempo`.
 */
export function PainelAcompanhamentoUti() {
  const { sessao } = useAuth()
  // O painel do paciente linka para cá com a pessoa na URL.
  const [parametros] = useSearchParams()

  const [pessoaId, setPessoaId] = useState(parametros.get('pessoaId') ?? '')
  const [pessoaRotulo, setPessoaRotulo] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
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
    if (!pessoaId) {
      setDados(undefined)
      return
    }
    setCarregando(true)
    utiPainelService
      .painelAcompanhamento(pessoaId, de || undefined, ate || undefined)
      .then((painel) => {
        setDados(painel)
        setPessoaRotulo(painel.pessoaNome)
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, pessoaId, de, ate])

  useEffect(carregar, [carregar])

  const dias = useDiasPlotados(dados?.dias ?? [])

  const abas: Aba[] = [
    { id: 'resumo', rotulo: 'Resumo' },
    { id: 'dieta', rotulo: 'Dieta' },
    { id: 'laboratorio', rotulo: 'Laboratório' },
    { id: 'hemodinamica', rotulo: 'Balanço e hemodinâmica' },
    { id: 'tabela', rotulo: 'Tabela' },
  ]

  return (
    <TPage
      title="Painel de acompanhamento"
      subtitle="Adesão à dieta, laboratório, balanço e hemodinâmica, dia a dia."
      actions={dados && <TBotaoImprimir rotulo="Imprimir relatório" />}
      // O papel é um relatório do período, não este formulário. Ver `Folha`.
      documento={dados && <DocumentoAcompanhamento dados={dados} />}
    >
      <div className="flex flex-col gap-4">
        {/* Filtro é interface: some no papel (docs/05 §8). */}
        <TPanel className="nao-imprime">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TCombo
              label="Paciente"
              className="lg:col-span-2"
              placeholder="Buscar por nome ou documento"
              value={pessoaId}
              rotuloInicial={pessoaRotulo}
              onChange={setPessoaId}
              buscar={(termo) =>
                pessoaService.select(termo, tipoPacienteId).then((ps) =>
                  ps.map((p) => ({
                    id: p.id,
                    nome: p.documento ? `${p.nome} (${formatarDocumento(p.documento)})` : p.nome,
                  })),
                )
              }
            />
            <TEntry
              label="De"
              type="date"
              value={de}
              onChange={(e) => setDe(e.target.value)}
              ajuda="Em branco: últimos 30 dias"
            />
            <TEntry label="Até" type="date" value={ate} onChange={(e) => setAte(e.target.value)} />
          </div>
        </TPanel>

        {!pessoaId ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">
              Escolha um paciente para ver os dias de acompanhamento.
            </p>
          </TPanel>
        ) : !dados ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">Carregando…</p>
          </TPanel>
        ) : (
          <>
            <TTabs abas={abas} ativa={aba} onChange={setAba} />

            <div className={carregando ? 'opacity-50 transition-opacity' : undefined}>
              {/* ─── Resumo ───────────────────────────────────────── */}
              <TTabPanel id="resumo" ativa={aba}>
                {/* Cartão por indicador, com a nota do que ele significa. A
                    nota é onde mora a ressalva clínica — sem ela, "adesão 62 %"
                    lido no dia 2 vira um julgamento que a ESPEN desautoriza. */}
                <div className="mb-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <Kpi
                    rotulo="Dias registrados"
                    valor={String(dados.totalDias)}
                    nota={
                      dados.diasSemAvaliacao > 0
                        ? `${dados.diasSemAvaliacao} sem avaliação vinculada`
                        : 'todos com avaliação vinculada'
                    }
                  />
                  <Kpi
                    rotulo="Adesão média"
                    valor={num(dados.adesaoMedia, '%', 1)}
                    nota="não é nota — a meta é progressiva na 1ª semana"
                  />
                  <Kpi
                    rotulo="Energia recebida"
                    valor={num(dados.caloriasPorQuiloMedia, 'kcal/kg', 1)}
                    nota="média dos dias com peso definido"
                  />
                  <Kpi
                    rotulo="Proteína recebida"
                    valor={num(dados.proteinaPorQuiloMedia, 'g/kg', 2)}
                    nota="média dos dias com peso definido"
                  />
                  <Kpi
                    rotulo="Balanço acumulado"
                    valor={num(dados.balancoAcumuladoMl, 'ml', 0)}
                    nota="soma dos saldos de 24 h"
                  />
                  <Kpi
                    rotulo="Diurese média"
                    valor={num(dados.diureseMediaMlKgHora, 'ml/kg/h', 2)}
                    nota="oligúria abaixo de 0,5 (KDIGO)"
                  />
                  <Kpi
                    rotulo="Ingestão oral"
                    valor={num(dados.ingestaoOralMedia, '%', 1)}
                    nota="aceitação média das refeições"
                  />
                  <Kpi
                    rotulo="Volume prescrito"
                    valor={num(dados.volumePrescritoNaAvaliacao, 'ml/dia', 0)}
                    nota={
                      dados.ultimaAvaliacao
                        ? `avaliação de ${formatarData(dados.ultimaAvaliacao)}`
                        : 'sem avaliação de referência'
                    }
                  />
                </div>

                <TPanel>
                  <h3 className="mb-3 text-h3 font-medium text-txt">
                    Prescrição de referência{' '}
                    <span className="text-caption font-normal text-txt-muted">
                      avaliação de {formatarData(dados.ultimaAvaliacao)}
                    </span>
                  </h3>
                  <div className="grid gap-5 sm:grid-cols-3">
                    <Indicador
                      rotulo="Volume prescrito"
                      valor={num(dados.volumePrescritoNaAvaliacao, 'ml', 0)}
                    />
                    <Indicador
                      rotulo="Meta energética"
                      valor={num(dados.metaEnergetica, 'kcal/dia', 0)}
                    />
                    <Indicador rotulo="Meta proteica" valor={num(dados.metaProteica, 'g/dia', 1)} />
                  </div>

                  {dados.diasSemAvaliacao > 0 && (
                    <p className="mt-5 text-caption text-txt-muted">
                      {dados.diasSemAvaliacao === 1
                        ? 'Um dia não tem avaliação vinculada'
                        : `${dados.diasSemAvaliacao} dias não têm avaliação vinculada`}
                      : neles kcal/kg, proteína por quilo e diurese por quilo não existem — dependem
                      do peso e da fórmula prescritos, e ficam fora das médias acima.
                    </p>
                  )}
                </TPanel>
              </TTabPanel>

              {/* ─── Dieta ────────────────────────────────────────── */}
              <TTabPanel id="dieta" ativa={aba}>
                <div className="flex flex-col gap-4">
                  <TPanel>
                    <AdesaoNoTempo
                      dados={dias}
                      chaveX="data"
                      chaveAdesao="percentualRecebido"
                      rotuloX={(d) => formatarData(d.data)}
                    />
                  </TPanel>
                  <TPanel>
                    <VolumeNoTempo dias={dias} />
                  </TPanel>
                </div>
              </TTabPanel>

              {/* ─── Laboratório ──────────────────────────────────── */}
              <TTabPanel id="laboratorio" ativa={aba}>
                <TPanel>
                  <LaboratorioNoTempo dias={dias} />
                </TPanel>
              </TTabPanel>

              {/* ─── Balanço e hemodinâmica ───────────────────────── */}
              <TTabPanel id="hemodinamica" ativa={aba}>
                <div className="flex flex-col gap-4">
                  <TPanel>
                    <BalancoNoTempo dias={dias} />
                  </TPanel>
                  <div className="grid gap-4 lg:grid-cols-2">
                    <TPanel>
                      <DiureseNoTempo dias={dias} />
                    </TPanel>
                    <TPanel>
                      <PressaoNoTempo dias={dias} />
                    </TPanel>
                  </div>
                </div>
              </TTabPanel>

              {/* ─── Tabela ───────────────────────────────────────── */}
              <TTabPanel id="tabela" ativa={aba}>
                <TPanel title="Os dias do período">
                  {dias.length === 0 ? (
                    <p className="py-4 text-body text-txt-secondary">
                      Nenhum dia registrado no período escolhido.
                    </p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[760px] text-body">
                        <thead>
                          <tr className="border-b border-line text-caption text-txt-secondary">
                            <th className="px-2 py-2 text-left font-normal">Data</th>
                            <th className="px-2 py-2 text-right font-normal">Prescrito</th>
                            <th className="px-2 py-2 text-right font-normal">Recebido</th>
                            <th className="px-2 py-2 text-right font-normal">Adesão</th>
                            <th className="px-2 py-2 text-right font-normal">kcal/kg</th>
                            <th className="px-2 py-2 text-right font-normal">g PTN/kg</th>
                            <th className="px-2 py-2 text-right font-normal">Balanço</th>
                            <th className="px-2 py-2 text-right font-normal">Diurese</th>
                            <th className="px-2 py-2 text-right font-normal">PAM</th>
                          </tr>
                        </thead>
                        <tbody>
                          {[...dias].reverse().map((d) => (
                            <tr key={d.id} className="border-b border-line last:border-0">
                              <td className="px-2 py-2">{formatarData(d.data)}</td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.volPrescrito24h, '', 0)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.volRecebido24h, '', 0)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.percentualRecebido, '%', 1)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.caloriasPorQuilo, '', 1)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.proteinaPorQuilo, '', 2)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.balancoHidricoMl, '', 0)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">
                                {num(d.diuresePorQuiloHora, '', 2)}
                              </td>
                              <td className="numeric px-2 py-2 text-right">{num(d.pam, '', 0)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </TPanel>
              </TTabPanel>
            </div>
          </>
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
