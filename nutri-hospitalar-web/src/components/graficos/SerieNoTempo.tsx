import {
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceArea,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EIXO, GRADE, GraficoVazio, TituloGrafico, TooltipCartao } from '@/components/graficos/chrome'
import { chaveDoPonto } from '@/components/graficos/eixos'
import type { Referencia } from '@/components/graficos/referencias'
import { formatarNumero } from '@/utils/format'

/**
 * Uma grandeza no tempo, com a sua faixa de referência ao fundo.
 *
 * É o **small multiple** dos painéis da UTI: em vez dos dois eixos Y do eroERP
 * — que punham potássio e sódio na mesma moldura e escondiam onde cada um cai —
 * cada analito ganha o seu quadro, com a sua escala e a sua faixa.
 *
 * **A faixa tem três formas, e o componente desenha as três** (ver
 * `referencias.ts`): banda com piso e teto, limiar de um lado só, e alvo
 * terapêutico. Desenhar tudo como banda mentiria em três dos nove analitos.
 *
 * A faixa é plana — não varia com o x, como varia a da OMS na pediatria. Por
 * isso aqui basta `ReferenceArea`/`ReferenceLine`, e não o empilhamento de
 * `Area` que a `CurvaCrescimento` precisa.
 */
export function SerieNoTempo<T extends object>({
  dados,
  chaveX,
  chaveY,
  titulo,
  referencia,
  casas = 1,
  rotuloX,
  altura = 200,
  vazio,
}: {
  dados: T[]
  chaveX: keyof T & string
  chaveY: keyof T & string
  titulo: string
  referencia?: Referencia
  casas?: number
  /** Como escrever o x no tooltip. Sem isto, mostra o valor cru. */
  rotuloX?: (ponto: T) => string
  altura?: number
  vazio?: string
}) {
  const comValor = dados.filter((p) => p[chaveY] != null)

  if (comValor.length === 0) {
    return (
      <div>
        <TituloGrafico>{titulo}</TituloGrafico>
        <GraficoVazio>{vazio ?? 'Nenhum dia com este valor informado no período.'}</GraficoVazio>
      </div>
    )
  }

  const unidade = referencia?.unidade ? ` ${referencia.unidade}` : ''

  return (
    <div>
      <TituloGrafico referencia={referencia?.fonte}>{titulo}</TituloGrafico>

      <ResponsiveContainer width="100%" height={altura}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey={chaveDoPonto<T>(chaveX)} {...EIXO} />
          <YAxis {...EIXO} width={48} domain={['auto', 'auto']} />

          {referencia && <Faixa referencia={referencia} />}

          <Line
            type="monotone"
            dataKey={chaveDoPonto<T>(chaveY)}
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 3, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as T
              const valor = p[chaveY] as number | null | undefined
              return (
                <TooltipCartao
                  titulo={rotuloX ? rotuloX(p) : String(p[chaveX])}
                  linhas={[
                    {
                      rotulo: titulo,
                      cor: 'var(--viz-serie-1)',
                      valor: valor == null ? '—' : `${formatarNumero(valor, casas)}${unidade}`,
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
 * A faixa ao fundo, na forma que a referência declarar.
 *
 * `alvo` é desenhado como banda mas com rótulo próprio: quem lê precisa saber
 * que ali não está a normalidade do adulto saudável, e sim onde se quer manter
 * o paciente crítico.
 */
function Faixa({ referencia }: { referencia: Referencia }) {
  if (referencia.forma === 'limiarSuperior') {
    return (
      <>
        <ReferenceLine
          y={referencia.max}
          stroke="var(--txt-muted)"
          strokeDasharray="4 4"
          label={{
            value: `alerta ${formatarNumero(referencia.max, 1)}`,
            position: 'right',
            fill: 'var(--txt-muted)',
            fontSize: 11,
          }}
        />
        {referencia.grave != null && (
          <ReferenceLine
            y={referencia.grave}
            stroke="var(--txt-muted)"
            strokeDasharray="2 4"
            label={{
              value: `grave ${formatarNumero(referencia.grave, 1)}`,
              position: 'right',
              fill: 'var(--txt-muted)',
              fontSize: 11,
            }}
          />
        )}
      </>
    )
  }

  if (referencia.min == null) return null

  return (
    <ReferenceArea
      y1={referencia.min}
      y2={referencia.max}
      fill="var(--viz-faixa-interna)"
      fillOpacity={0.35}
      stroke="none"
      label={{
        value: referencia.forma === 'alvo' ? 'alvo' : 'referência',
        position: 'insideTopLeft',
        fill: 'var(--txt-muted)',
        fontSize: 11,
      }}
    />
  )
}
