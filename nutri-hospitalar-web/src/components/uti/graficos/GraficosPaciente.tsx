import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ReferenceArea,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EIXO, GRADE, GraficoVazio, TituloGrafico, TooltipCartao } from '@/components/graficos/chrome'
import type { PontoAvaliacaoUti } from '@/types/uti'
import { formatarData, formatarNumero } from '@/utils/format'

/**
 * As faixas de IMC da OMS 1997, para desenhar ao fundo do gráfico.
 *
 * São as mesmas seis do `AntropometriaCalculator`, colapsadas em **quatro
 * zonas** de fundo: os três graus de obesidade viram uma só. `docs/05 §10.4`
 * diz que seis degraus não mantêm o intervalo mínimo de luminosidade entre
 * vizinhos — quem precisa do grau exato lê o rótulo classificado ao lado, que
 * vem do servidor com os seis nomes.
 *
 * A rampa é **ordinal**, não semáforo: sair da eutrofia para qualquer lado é o
 * mesmo tipo de afastamento, e pintar de vermelho só um lado sugeriria que
 * emagrecer é sempre melhor.
 */
const FAIXAS_IMC = [
  { de: 10, ate: 18.5, cor: 'var(--viz-ordinal-3)', rotulo: 'desnutrição' },
  { de: 18.5, ate: 25, cor: 'var(--viz-ordinal-1)', rotulo: 'eutrofia' },
  { de: 25, ate: 30, cor: 'var(--viz-ordinal-2)', rotulo: 'sobrepeso' },
  { de: 30, ate: 60, cor: 'var(--viz-ordinal-3)', rotulo: 'obesidade' },
]

const dataCurta = (p: PontoAvaliacaoUti) => formatarData(p.dataAvaliacao)

/**
 * Peso de trabalho no tempo.
 *
 * Separado do IMC de propósito. O eroERP põe os dois na mesma moldura com dois
 * eixos Y (`...PacienteDashboard.tsx:275-282`) e, ao fazê-lo, **perde
 * justamente o que o IMC tem de útil**: não dá para desenhar as faixas de
 * classificação atrás de um eixo que divide espaço com quilos.
 */
export function PesoNoTempo({ evolucao }: { evolucao: PontoAvaliacaoUti[] }) {
  const dados = evolucao.filter((p) => p.pesoTrabalhoKg != null)

  if (dados.length === 0) {
    return (
      <div>
        <TituloGrafico>Peso de trabalho</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com peso definido no período — sem peso atual nem
          medidas para estimá-lo, não há o que plotar.
        </GraficoVazio>
      </div>
    )
  }

  const ultimo = dados[dados.length - 1]

  // O domínio precisa abraçar as metas, senão a linha de referência sai do
  // quadro e o gráfico volta a ser um ponto solto.
  const relevantes = [
    ...dados.map((p) => p.pesoTrabalhoKg as number),
    ultimo?.pesoIdealKg,
    ultimo?.pesoIdealImc25Kg,
  ].filter((v): v is number => v != null)
  const piso = Math.floor(Math.min(...relevantes) - 2)
  const teto = Math.ceil(Math.max(...relevantes) + 2)

  return (
    <div>
      <TituloGrafico referencia="O peso que a prescrição usou — atual, estimado ou ajustado — contra as metas de peso da própria avaliação.">
        Peso de trabalho
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={280}>
        <ComposedChart data={evolucao} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          <YAxis {...EIXO} width={52} domain={[piso, teto]} unit=" kg" />

          {/* Peso ideal e peso para IMC 25 delimitam onde o peso deveria estar
              — é o que salva o gráfico do paciente com UMA avaliação: um ponto
              solto num eixo automático não diz se 68 kg é muito ou pouco para
              aquela altura.

              São `ReferenceLine` horizontais, e não séries. Linha tracejada
              ligando um ponto só não desenha linha: desenha dois tracinhos
              soltos no meio do quadro, que foi o que apareceu na primeira
              versão. As metas derivam da altura, que não muda na internação,
              então a da última avaliação vale para o gráfico inteiro. */}
          {ultimo?.pesoIdealKg != null && (
            <ReferenceLine
              y={ultimo.pesoIdealKg}
              stroke="var(--viz-serie-3)"
              strokeWidth={1.5}
              strokeDasharray="5 4"
              label={{
                value: `ideal ${formatarNumero(ultimo.pesoIdealKg, 1)} kg`,
                position: 'insideTopLeft',
                fill: 'var(--txt-muted)',
                fontSize: 11,
              }}
            />
          )}
          {ultimo?.pesoIdealImc25Kg != null && (
            <ReferenceLine
              y={ultimo.pesoIdealImc25Kg}
              stroke="var(--viz-serie-2)"
              strokeWidth={1.5}
              strokeDasharray="2 4"
              label={{
                value: `IMC 25 · ${formatarNumero(ultimo.pesoIdealImc25Kg, 1)} kg`,
                position: 'insideBottomLeft',
                fill: 'var(--txt-muted)',
                fontSize: 11,
              }}
            />
          )}

          <Line
            type="monotone"
            dataKey="pesoTrabalhoKg"
            name="Peso"
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoAvaliacaoUti
              return (
                <TooltipCartao
                  titulo={dataCurta(p)}
                  linhas={[
                    {
                      rotulo: 'Peso de trabalho',
                      cor: 'var(--viz-serie-1)',
                      valor:
                        p.pesoTrabalhoKg != null
                          ? `${formatarNumero(p.pesoTrabalhoKg, 2)} kg`
                          : '—',
                    },
                    {
                      rotulo: 'Peso ideal',
                      cor: 'var(--viz-serie-3)',
                      valor: p.pesoIdealKg != null ? `${formatarNumero(p.pesoIdealKg, 2)} kg` : '—',
                    },
                    {
                      rotulo: 'Peso para IMC 25',
                      cor: 'var(--viz-serie-2)',
                      valor:
                        p.pesoIdealImc25Kg != null
                          ? `${formatarNumero(p.pesoIdealImc25Kg, 2)} kg`
                          : '—',
                    },
                    {
                      rotulo: 'Perda de peso',
                      valor:
                        p.percPerdaPeso != null
                          ? `${formatarNumero(p.percPerdaPeso, 2)}% · ${p.classifPerdaPeso ?? ''}`
                          : '—',
                    },
                  ]}
                />
              )
            }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}

/**
 * IMC no tempo, **com as faixas de classificação ao fundo**.
 *
 * É o ganho de separar do peso: a linha do IMC só significa alguma coisa contra
 * a faixa em que cai, e é isso que o eixo duplo do eroERP escondia.
 */
export function ImcComFaixas({ evolucao }: { evolucao: PontoAvaliacaoUti[] }) {
  const dados = evolucao.filter((p) => p.imc != null)

  if (dados.length === 0) {
    return (
      <div>
        <TituloGrafico>IMC e faixas de classificação</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com IMC no período — ele precisa de peso e altura.
        </GraficoVazio>
      </div>
    )
  }

  const valores = dados.map((p) => p.imc as number)
  const min = Math.min(...valores, 18)
  const max = Math.max(...valores, 27)

  return (
    <div>
      <TituloGrafico referencia="Faixas da OMS 1997. Para paciente com 60 anos ou mais, a avaliação também traz a classificação da OPAS 2002, que usa outros cortes.">
        IMC e faixas de classificação
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={260}>
        <ComposedChart data={evolucao} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          <YAxis {...EIXO} width={44} domain={[Math.floor(min - 2), Math.ceil(max + 2)]} />

          {FAIXAS_IMC.map((f) => (
            <ReferenceArea
              key={f.rotulo}
              y1={f.de}
              y2={f.ate}
              fill={f.cor}
              fillOpacity={0.18}
              stroke="none"
            />
          ))}
          {/* Os cortes explícitos, porque a mancha sozinha não dá o número */}
          {[18.5, 25, 30].map((corte) => (
            <ReferenceLine
              key={corte}
              y={corte}
              stroke="var(--line-strong)"
              label={{
                value: String(corte).replace('.', ','),
                position: 'left',
                fill: 'var(--txt-muted)',
                fontSize: 11,
              }}
            />
          ))}

          <Line
            type="monotone"
            dataKey="imc"
            name="IMC"
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoAvaliacaoUti
              return (
                <TooltipCartao
                  titulo={dataCurta(p)}
                  linhas={[
                    {
                      rotulo: 'IMC',
                      cor: 'var(--viz-serie-1)',
                      valor: p.imc != null ? formatarNumero(p.imc, 2) : '—',
                    },
                    // O rótulo dos seis graus vem do servidor: aqui não se
                    // reclassifica nada, só se mostra o que foi gravado.
                    { rotulo: 'Classificação', valor: p.classifImcOms ?? '—' },
                  ]}
                />
              )
            }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}

/**
 * Meta contra oferta, energética e proteica.
 *
 * As duas séries de cada gráfico têm a **mesma unidade** — kcal com kcal, grama
 * com grama — e por isso dividem um eixo. A meta é tracejada por ser alvo; a
 * oferta é a série cheia, porque é ela que se mexe.
 */
export function MetasNoTempo({ evolucao }: { evolucao: PontoAvaliacaoUti[] }) {
  const temEnergia = evolucao.some((p) => p.metaEnergetica != null || p.caloriasOfertadas != null)
  const temProteina = evolucao.some((p) => p.metaProteica != null || p.proteinaOfertada != null)

  if (!temEnergia && !temProteina) {
    return (
      <div>
        <TituloGrafico>Meta e oferta no tempo</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com meta calculada — ela depende do peso e da fase da terapia.
        </GraficoVazio>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      {temEnergia && (
        <ParMetaOferta
          evolucao={evolucao}
          titulo="Energia: meta e oferta"
          chaveMeta="metaEnergetica"
          chaveOferta="caloriasOfertadas"
          unidade="kcal/dia"
          casas={0}
        />
      )}
      {temProteina && (
        <ParMetaOferta
          evolucao={evolucao}
          titulo="Proteína: meta e oferta"
          chaveMeta="metaProteica"
          chaveOferta="proteinaOfertada"
          unidade="g/dia"
          casas={1}
        />
      )}
    </div>
  )
}

function ParMetaOferta({
  evolucao,
  titulo,
  chaveMeta,
  chaveOferta,
  unidade,
  casas,
}: {
  evolucao: PontoAvaliacaoUti[]
  titulo: string
  chaveMeta: 'metaEnergetica' | 'metaProteica'
  chaveOferta: 'caloriasOfertadas' | 'proteinaOfertada'
  unidade: string
  casas: number
}) {
  return (
    <div>
      <TituloGrafico>{titulo}</TituloGrafico>

      <ResponsiveContainer width="100%" height={240}>
        <ComposedChart data={evolucao} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          <YAxis {...EIXO} width={56} />

          <Line
            type="stepAfter"
            dataKey={chaveMeta}
            name="Meta"
            stroke="var(--txt-secondary)"
            strokeWidth={2}
            strokeDasharray="5 4"
            connectNulls
            isAnimationActive={false}
            dot={false}
          />
          <Line
            type="monotone"
            dataKey={chaveOferta}
            name="Ofertado"
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoAvaliacaoUti
              return (
                <TooltipCartao
                  titulo={dataCurta(p)}
                  linhas={[
                    {
                      rotulo: 'Meta',
                      cor: 'var(--txt-secondary)',
                      valor:
                        p[chaveMeta] != null
                          ? `${formatarNumero(p[chaveMeta], casas)} ${unidade}`
                          : '—',
                    },
                    {
                      rotulo: 'Ofertado',
                      cor: 'var(--viz-serie-1)',
                      valor:
                        p[chaveOferta] != null
                          ? `${formatarNumero(p[chaveOferta], casas)} ${unidade}`
                          : '—',
                    },
                  ]}
                />
              )
            }}
          />
          <Legend
            verticalAlign="top"
            align="right"
            iconType="plainline"
            wrapperStyle={{ fontSize: 12, color: 'var(--txt-secondary)', paddingBottom: 8 }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}

/**
 * Adequação da circunferência do braço, em percentual do P50.
 *
 * Os cortes vêm de `Estimativas!N12:O17` (docs/10 §2.8): 70 · 80 · 90 · 110 ·
 * 120 %, em **seis** classificações. Aqui as seis viram três zonas de fundo —
 * desnutrição, eutrofia, excesso — porque `docs/05 §10.4` recusa seis degraus
 * de luminosidade. As seis continuam por extenso no rótulo de cada ponto.
 */
export function AdequacaoCbNoTempo({ evolucao }: { evolucao: PontoAvaliacaoUti[] }) {
  const dados = evolucao.filter((p) => p.adequacaoCircBracoPerc != null)

  if (dados.length === 0) {
    return (
      <div>
        <TituloGrafico>Adequação da circunferência do braço</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com circunferência do braço medida no período.
        </GraficoVazio>
      </div>
    )
  }

  const perc = dados.map((p) => p.adequacaoCircBracoPerc as number)
  const pisoCb = Math.min(60, Math.floor(Math.min(...perc) - 5))
  const tetoCb = Math.max(130, Math.ceil(Math.max(...perc) + 5))

  return (
    <div>
      <TituloGrafico referencia="Percentual do P50 de referência. Cortes em 90 % e 110 % delimitam a eutrofia; a avaliação traz a classificação nas seis faixas.">
        Adequação da circunferência do braço
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={240}>
        <ComposedChart data={evolucao} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          {/* O domínio TEM de conter 90 e 110: com escala automática de 0 a 80,
              as faixas que dão sentido ao número ficavam fora do quadro e o
              gráfico voltava a ser um ponto solto. */}
          <YAxis
            {...EIXO}
            width={48}
            domain={[pisoCb, tetoCb]}
            tickFormatter={(v: number) => `${formatarNumero(v, 0)}%`}
          />

          <ReferenceArea y1={pisoCb} y2={90} fill="var(--viz-ordinal-3)" fillOpacity={0.18} stroke="none" />
          <ReferenceArea y1={90} y2={110} fill="var(--viz-ordinal-1)" fillOpacity={0.18} stroke="none" />
          <ReferenceArea y1={110} y2={tetoCb} fill="var(--viz-ordinal-2)" fillOpacity={0.18} stroke="none" />
          {[90, 110].map((corte) => (
            <ReferenceLine
              key={corte}
              y={corte}
              stroke="var(--line-strong)"
              label={{
                value: `${corte} %`,
                position: 'left',
                fill: 'var(--txt-muted)',
                fontSize: 11,
              }}
            />
          ))}

          <Line
            type="monotone"
            dataKey="adequacaoCircBracoPerc"
            name="Adequação"
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoAvaliacaoUti
              return (
                <TooltipCartao
                  titulo={dataCurta(p)}
                  linhas={[
                    {
                      rotulo: 'Adequação',
                      cor: 'var(--viz-serie-1)',
                      valor:
                        p.adequacaoCircBracoPerc != null
                          ? `${formatarNumero(p.adequacaoCircBracoPerc, 2)}%`
                          : '—',
                    },
                    { rotulo: 'Classificação', valor: p.classifAdequacaoCb ?? '—' },
                  ]}
                />
              )
            }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}

/**
 * O que a dieta entrega **por quilo**, contra a faixa recomendada do dia.
 *
 * É a leitura que o intensivista faz de cabeça — "está em 20 kcal/kg" — e a
 * única em que a faixa de referência muda a cada avaliação, porque ela é
 * calculada sobre o peso e a fase daquele dia. Por isso a faixa é desenhada
 * como área que acompanha o eixo X, e não como banda plana.
 */
export function OfertaPorQuilo({ evolucao }: { evolucao: PontoAvaliacaoUti[] }) {
  const dados = evolucao
    .filter((p) => p.pesoTrabalhoKg)
    .map((p) => ({
      ...p,
      // A faixa vem em kcal/dia; por quilo é ela dividida pelo peso do dia.
      kcalMinPorKg: porQuilo(p.energiaMinima, p.pesoTrabalhoKg),
      kcalMaxPorKg: porQuilo(p.energiaMaxima, p.pesoTrabalhoKg),
      // `Area` empilha: a segunda série desenha a ALTURA da faixa, não o topo.
      kcalFaixa: diferenca(p.energiaMaxima, p.energiaMinima, p.pesoTrabalhoKg),
    }))

  if (dados.every((p) => p.caloriasPorQuilo == null)) {
    return (
      <div>
        <TituloGrafico>Energia ofertada por quilo</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com dieta prescrita e peso definido no período.
        </GraficoVazio>
      </div>
    )
  }

  return (
    <div>
      <TituloGrafico referencia="A faixa ao fundo é a recomendação daquela avaliação, dividida pelo peso do dia — ela se move quando o peso ou a fase mudam.">
        Energia ofertada por quilo
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={260}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          <YAxis {...EIXO} width={52} unit=" kcal" />

          {/* Duas áreas empilhadas: a de baixo é transparente e serve de base;
              a de cima é a faixa. É o mesmo truque da curva da OMS, e existe
              porque a faixa VARIA com o x — banda plana não serviria. */}
          <Area
            type="monotone"
            dataKey="kcalMinPorKg"
            stackId="faixa"
            stroke="none"
            fill="none"
            isAnimationActive={false}
            legendType="none"
          />
          <Area
            type="monotone"
            dataKey="kcalFaixa"
            stackId="faixa"
            name="Faixa recomendada"
            stroke="none"
            fill="var(--viz-faixa-interna)"
            fillOpacity={0.45}
            isAnimationActive={false}
          />

          <Line
            type="monotone"
            dataKey="caloriasPorQuilo"
            name="Ofertado"
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoAvaliacaoUti & {
                kcalMinPorKg?: number
                kcalMaxPorKg?: number
              }
              return (
                <TooltipCartao
                  titulo={dataCurta(p)}
                  linhas={[
                    {
                      rotulo: 'Ofertado',
                      cor: 'var(--viz-serie-1)',
                      valor:
                        p.caloriasPorQuilo != null
                          ? `${formatarNumero(p.caloriasPorQuilo, 1)} kcal/kg`
                          : '—',
                    },
                    {
                      rotulo: 'Recomendado',
                      cor: 'var(--viz-faixa-interna)',
                      valor:
                        p.kcalMinPorKg != null && p.kcalMaxPorKg != null
                          ? `${formatarNumero(p.kcalMinPorKg, 1)} a ${formatarNumero(p.kcalMaxPorKg, 1)} kcal/kg`
                          : '—',
                    },
                    { rotulo: 'Fase', valor: p.fase ?? '—' },
                    ...(p.obeso
                      ? [{ rotulo: 'Obesidade', valor: 'correção da ASPEN aplicada' }]
                      : []),
                  ]}
                />
              )
            }}
          />
          <Legend
            verticalAlign="top"
            align="right"
            wrapperStyle={{ fontSize: 12, color: 'var(--txt-secondary)', paddingBottom: 8 }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}

function porQuilo(valor?: number | null, peso?: number | null): number | undefined {
  return valor != null && peso ? valor / peso : undefined
}

function diferenca(
  maximo?: number | null,
  minimo?: number | null,
  peso?: number | null,
): number | undefined {
  if (maximo == null || minimo == null || !peso) return undefined
  return (maximo - minimo) / peso
}
