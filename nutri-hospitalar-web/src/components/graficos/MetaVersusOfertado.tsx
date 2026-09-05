import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
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
 * O que foi prescrito contra o que foi recebido — **um eixo só**.
 *
 * O eroERP põe ml e percentual na mesma moldura, com dois eixos Y. Eixo duplo é
 * anti-padrão explícito em `docs/05 §10.5`: a leitura de "a linha cruzou a
 * barra" passa a depender de qual escala o leitor está olhando, e não do dado.
 * Aqui as duas séries têm a mesma unidade, e a meta é tracejada por ser alvo.
 */
export function MetaVersusOfertado<T extends object>({
  dados,
  chaveX,
  chaveMeta,
  chaveOfertado,
  titulo,
  referencia,
  unidade,
  casas = 0,
  rotuloMeta = 'Prescrito',
  rotuloOfertado = 'Recebido',
  rotuloX,
  formatarX,
  altura = 260,
  vazio,
}: {
  dados: T[]
  chaveX: keyof T & string
  chaveMeta: keyof T & string
  chaveOfertado: keyof T & string
  titulo: string
  referencia?: string
  unidade: string
  casas?: number
  rotuloMeta?: string
  rotuloOfertado?: string
  rotuloX?: (ponto: T) => string
  /**
   * Como escrever o x no **eixo**. Sem isto ele mostra o valor cru — e uma data
   * ISO (`2026-08-22`) é ilegível e ocupa o dobro do espaço.
   */
  formatarX?: (valor: string) => string
  altura?: number
  vazio?: string
}) {
  const comValor = dados.filter((p) => p[chaveMeta] != null || p[chaveOfertado] != null)

  if (comValor.length === 0) {
    return (
      <div>
        <TituloGrafico>{titulo}</TituloGrafico>
        <GraficoVazio>{vazio ?? 'Nenhum dia com prescrição ou oferta informada.'}</GraficoVazio>
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

          <Bar
            dataKey={chaveDoPonto<T>(chaveOfertado)}
            name={rotuloOfertado}
            fill="var(--viz-serie-1)"
            radius={[3, 3, 0, 0]}
            isAnimationActive={false}
          />
          <Line
            type="stepAfter"
            dataKey={chaveDoPonto<T>(chaveMeta)}
            name={rotuloMeta}
            stroke="var(--txt-secondary)"
            strokeWidth={2}
            strokeDasharray="5 4"
            connectNulls
            isAnimationActive={false}
            dot={false}
          />

          <Tooltip
            cursor={{ fill: 'var(--line)', fillOpacity: 0.4 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as T
              const meta = p[chaveMeta] as number | null | undefined
              const ofertado = p[chaveOfertado] as number | null | undefined
              return (
                <TooltipCartao
                  titulo={rotuloX ? rotuloX(p) : String(p[chaveX])}
                  linhas={[
                    {
                      rotulo: rotuloMeta,
                      cor: 'var(--txt-secondary)',
                      valor: meta == null ? '—' : `${formatarNumero(meta, casas)} ${unidade}`,
                    },
                    {
                      rotulo: rotuloOfertado,
                      cor: 'var(--viz-serie-1)',
                      valor: ofertado == null ? '—' : `${formatarNumero(ofertado, casas)} ${unidade}`,
                    },
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

/**
 * A adesão em percentual, dia a dia.
 *
 * **O desvio não é colorido, e isso é decisão clínica, não estética.** A ESPEN
 * recomenda oferta hipocalórica — abaixo de 70 % — nos três primeiros dias, com
 * progressão do 3º ao 7º; a própria planilha diz o mesmo na escada de
 * 25 · 50 · 75 · 100 % que o sistema já implementa. Mas adequação sustentada
 * abaixo de 70 % associa-se a 1,4× mais óbito, e *overfeeding* também piora
 * desfecho.
 *
 * Ou seja: **o mesmo 60 % é conduta no dia 2 e alerta no dia 10.** Um gráfico
 * que pintasse o desvio de vermelho — como o `corPerc` do eroERP, verde ≥ 90 ·
 * âmbar ≥ 70 · vermelho < 70 — estaria errado metade do tempo. Aqui há a linha
 * tracejada nos 100 % e a nota; o julgamento fica com quem prescreve.
 *
 * Também não se replica o `domain={[0, 120]}` de lá, que **corta fora** a barra
 * de quem recebeu mais que o prescrito — justamente o caso que se quer ver.
 */
export function AdesaoNoTempo<T extends object>({
  dados,
  chaveX,
  chaveAdesao,
  rotuloX,
  formatarX,
  altura = 240,
}: {
  dados: T[]
  chaveX: keyof T & string
  chaveAdesao: keyof T & string
  rotuloX?: (ponto: T) => string
  /**
   * Como escrever o x no **eixo**. Sem isto ele mostra o valor cru — e uma data
   * ISO (`2026-08-22`) é ilegível e ocupa o dobro do espaço.
   */
  formatarX?: (valor: string) => string
  altura?: number
}) {
  const comValor = dados.filter((p) => p[chaveAdesao] != null)

  if (comValor.length === 0) {
    return (
      <div>
        <TituloGrafico>Adesão à dieta</TituloGrafico>
        <GraficoVazio>
          Nenhum dia com volume recebido e prescrito informados — a adesão precisa dos dois.
        </GraficoVazio>
      </div>
    )
  }

  return (
    <div>
      <TituloGrafico referencia="A meta é progressiva na primeira semana (ESPEN): oferta abaixo de 70 % nos primeiros dias é conduta, não falha.">
        Adesão à dieta
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={altura}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis
            dataKey={chaveDoPonto<T>(chaveX)}
            {...EIXO}
            tickFormatter={(v: string) => (formatarX ? formatarX(v) : v)}
          />
          {/* Sem domain fixo: adesão acima de 100 % é dado, não ruído a cortar. */}
          <YAxis {...EIXO} width={48} tickFormatter={(v: number) => `${formatarNumero(v, 0)}%`} />

          <ReferenceLine
            y={100}
            stroke="var(--txt-muted)"
            strokeDasharray="4 4"
            label={{
              value: 'prescrito',
              position: 'right',
              fill: 'var(--txt-muted)',
              fontSize: 11,
            }}
          />

          <Bar
            dataKey={chaveDoPonto<T>(chaveAdesao)}
            name="Adesão"
            fill="var(--viz-serie-1)"
            radius={[3, 3, 0, 0]}
            isAnimationActive={false}
          />

          <Tooltip
            cursor={{ fill: 'var(--line)', fillOpacity: 0.4 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as T
              const valor = p[chaveAdesao] as number | null | undefined
              return (
                <TooltipCartao
                  titulo={rotuloX ? rotuloX(p) : String(p[chaveX])}
                  linhas={[
                    {
                      rotulo: 'Do prescrito',
                      cor: 'var(--viz-serie-1)',
                      valor: valor == null ? '—' : `${formatarNumero(valor, 1)}%`,
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
