import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  LabelList,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EIXO, GRADE, GraficoVazio, TooltipCartao } from '@/components/graficos/chrome'
import type { ContagemRotuladaUti, ContagemUti, PontoPeriodoUti, TomResultado } from '@/types/uti'

/**
 * Cor por **tom gravado**, e nunca por texto.
 *
 * O tom vem do servidor junto com o rótulo, decidido pelo mesmo classificador
 * que decidiu o nome. O eroERP colore com `texto.includes('adequado')`, o que
 * quebra em "Adequada" e em qualquer rótulo que use a palavra por acaso.
 *
 * `NEUTRO` é também a cor de "não classificado": a ausência de classificação
 * não é uma categoria clínica e não pode ganhar cor de uma.
 */
const COR_TOM: Record<TomResultado, string> = {
  NEUTRO: 'var(--txt-muted)',
  ADEQUADO: 'var(--viz-ordinal-1)',
  ATENCAO: 'var(--viz-ordinal-3)',
  CRITICO: 'var(--viz-ordinal-5)',
}

/**
 * Avaliações e dias de acompanhamento por mês.
 *
 * As duas séries são **contagens** — mesma unidade, mesmo eixo. As barras são
 * as avaliações, e os dias vêm como linha porque são muito mais numerosos e
 * duas séries de barras da mesma largura disputariam a leitura.
 */
export function MovimentoPorPeriodo({ dados }: { dados: PontoPeriodoUti[] }) {
  if (dados.length === 0) return <GraficoVazio>Sem movimento no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={260}>
      <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="periodo" {...EIXO} />
        <YAxis {...EIXO} width={44} allowDecimals={false} />

        <Bar
          dataKey="avaliacoes"
          name="Avaliações"
          fill="var(--viz-serie-1)"
          radius={[3, 3, 0, 0]}
          maxBarSize={40}
          isAnimationActive={false}
        />
        <Line
          type="monotone"
          dataKey="dias"
          name="Dias de acompanhamento"
          stroke="var(--viz-serie-2)"
          strokeWidth={2}
          isAnimationActive={false}
          dot={false}
        />

        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as PontoPeriodoUti
            return (
              <TooltipCartao
                titulo={p.periodo}
                linhas={[
                  { rotulo: 'Avaliações', cor: 'var(--viz-serie-1)', valor: p.avaliacoes },
                  { rotulo: 'Dias', cor: 'var(--viz-serie-2)', valor: p.dias },
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
  )
}

/**
 * Distribuição de uma classificação clínica, em barras horizontais.
 *
 * Horizontal porque os rótulos são longos — "Obesidade grau III", "Desnutrição
 * grave" — e na vertical eles se inclinam ou se cortam.
 */
export function DistribuicaoPorTom({ dados }: { dados: ContagemRotuladaUti[] }) {
  if (dados.length === 0) return <GraficoVazio>Nenhuma avaliação classificada no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={Math.max(180, dados.length * 34 + 40)}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 32, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="rotulo" {...EIXO} width={150} />
        <Bar dataKey="quantidade" isAnimationActive={false} maxBarSize={22} radius={[0, 4, 4, 0]}>
          {dados.map((d) => (
            <Cell key={d.rotulo} fill={COR_TOM[d.tom ?? 'NEUTRO']} />
          ))}
          <LabelList
            dataKey="quantidade"
            position="right"
            fill="var(--txt-secondary)"
            fontSize={12}
          />
        </Bar>
        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as ContagemRotuladaUti
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[
                  {
                    rotulo: 'Avaliações',
                    cor: COR_TOM[p.tom ?? 'NEUTRO'],
                    valor: p.quantidade,
                  },
                ]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * Categoria sem ordem natural — nome de fórmula, modo de infusão — e por isso
 * **uma cor só**. Colorir cada barra duplicaria o que o comprimento já diz.
 */
export function BarrasNominaisUti({
  dados,
  rotuloValor = 'Avaliações',
}: {
  dados: ContagemUti[]
  rotuloValor?: string
}) {
  if (dados.length === 0) return <GraficoVazio>Sem dados no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={Math.max(180, dados.length * 34 + 40)}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 32, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="rotulo" {...EIXO} width={150} />
        <Bar
          dataKey="quantidade"
          fill="var(--viz-serie-1)"
          isAnimationActive={false}
          maxBarSize={22}
          radius={[0, 4, 4, 0]}
        >
          <LabelList
            dataKey="quantidade"
            position="right"
            fill="var(--txt-secondary)"
            fontSize={12}
          />
        </Bar>
        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as ContagemUti
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[{ rotulo: rotuloValor, cor: 'var(--viz-serie-1)', valor: p.quantidade }]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}
