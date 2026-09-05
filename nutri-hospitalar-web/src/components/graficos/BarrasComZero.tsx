import {
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EIXO, GRADE, GraficoVazio, TituloGrafico, TooltipCartao } from '@/components/graficos/chrome'
import { chaveDoPonto, tickNumerico } from '@/components/graficos/eixos'
import { formatarNumero } from '@/utils/format'

/**
 * Grandeza que cruza o zero — o balanço hídrico do dia.
 *
 * **Barra, e não linha.** O balanço é o saldo fechado de 24 h: uma grandeza
 * discreta por dia, sem valor intermediário. Uma linha atravessando o zero
 * desenha uma travessia contínua que não existe — sugere que houve um instante
 * de equilíbrio entre a terça positiva e a quarta negativa, e não houve.
 *
 * A linha de zero é sólida e mais forte que a grade: aqui ela não é grade, é a
 * fronteira entre reter e perder líquido.
 *
 * Cor por sinal, e só por sinal — positivo e negativo não são "bom" e "ruim"
 * num paciente de UTI, e por isso os dois tons são da mesma família ordinal, e
 * não semáforo.
 */
export function BarrasComZero<T extends object>({
  dados,
  chaveX,
  chaveY,
  titulo,
  referencia,
  unidade,
  casas = 0,
  rotuloX,
  formatarX,
  altura = 240,
  vazio,
}: {
  dados: T[]
  chaveX: keyof T & string
  chaveY: keyof T & string
  titulo: string
  referencia?: string
  unidade: string
  casas?: number
  rotuloX?: (ponto: T) => string
  /**
   * Como escrever o x no **eixo**. Sem isto ele mostra o valor cru — e uma data
   * ISO (`2026-08-22`) é ilegível e ocupa o dobro do espaço.
   */
  formatarX?: (valor: string) => string
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

  return (
    <div>
      <TituloGrafico referencia={referencia}>{titulo}</TituloGrafico>

      <ResponsiveContainer width="100%" height={altura}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis
            dataKey={chaveDoPonto<T>(chaveX)}
            {...EIXO}
            tickFormatter={(v: string) => (formatarX ? formatarX(v) : v)}
          />
          <YAxis {...EIXO} width={56} tickFormatter={tickNumerico(casas)} />

          <ReferenceLine y={0} stroke="var(--line-strong)" strokeWidth={1.5} />

          <Bar dataKey={chaveDoPonto<T>(chaveY)} isAnimationActive={false} radius={[2, 2, 0, 0]}>
            {dados.map((p, i) => (
              <Cell
                key={i}
                fill={
                  ((p[chaveY] as number | null) ?? 0) < 0
                    ? 'var(--viz-ordinal-2)'
                    : 'var(--viz-ordinal-4)'
                }
              />
            ))}
          </Bar>

          <Tooltip
            cursor={{ fill: 'var(--line)', fillOpacity: 0.4 }}
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
                      valor: valor == null ? '—' : `${formatarNumero(valor, casas)} ${unidade}`,
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
