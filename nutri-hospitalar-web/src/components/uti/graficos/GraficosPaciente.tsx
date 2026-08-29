import {
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

  return (
    <div>
      <TituloGrafico referencia="O peso que a prescrição usou — atual, estimado ou ajustado, conforme a avaliação">
        Peso de trabalho
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={260}>
        <ComposedChart data={evolucao} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          <YAxis {...EIXO} width={52} domain={['auto', 'auto']} unit=" kg" />

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
                      rotulo: 'Perda de peso',
                      valor:
                        p.percPerdaPeso != null ? `${formatarNumero(p.percPerdaPeso, 2)}%` : '—',
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

  return (
    <div>
      <TituloGrafico referencia="Percentual do P50 de referência. Cortes em 90 % e 110 % delimitam a eutrofia; a avaliação traz a classificação nas seis faixas.">
        Adequação da circunferência do braço
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={240}>
        <ComposedChart data={evolucao} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="dataAvaliacao" {...EIXO} tickFormatter={(v: string) => formatarData(v)} />
          <YAxis {...EIXO} width={48} tickFormatter={(v: number) => `${formatarNumero(v, 0)}%`} />

          <ReferenceArea y1={0} y2={90} fill="var(--viz-ordinal-3)" fillOpacity={0.18} stroke="none" />
          <ReferenceArea y1={90} y2={110} fill="var(--viz-ordinal-1)" fillOpacity={0.18} stroke="none" />
          <ReferenceArea y1={110} y2={200} fill="var(--viz-ordinal-2)" fillOpacity={0.18} stroke="none" />

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
