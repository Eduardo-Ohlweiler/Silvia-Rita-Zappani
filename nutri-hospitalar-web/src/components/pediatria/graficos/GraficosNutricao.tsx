import {
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
import type { PontoEvolutivo } from '@/types/pediatria'
import { formatarNumero } from '@/utils/format'
import { EIXO, GRADE, GraficoVazio, TituloGrafico, TooltipCartao } from '@/components/graficos/chrome'

/**
 * Quanto da necessidade a dieta cobriu, ao longo do tempo.
 *
 * As duas séries são percentuais — mesma unidade, mesmo eixo. A linha dos 100 %
 * é tracejada porque é **limiar**, não grade: é a única coisa tracejada aqui.
 */
export function CoberturaNoTempo({ evolucao }: { evolucao: PontoEvolutivo[] }) {
  const dados = evolucao.filter((p) => p.percCalorico != null || p.percProteico != null)

  if (dados.length === 0) {
    return (
      <div>
        <TituloGrafico>Cobertura nutricional no tempo</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com dieta prescrita no período — a cobertura depende
          de fórmula, volume e frequência.
        </GraficoVazio>
      </div>
    )
  }

  return (
    <div>
      <TituloGrafico referencia="100 % = a necessidade calculada pelas DRIs">
        Cobertura nutricional no tempo
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={280}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="idadeMeses" {...EIXO} />
          <YAxis {...EIXO} width={48} tickFormatter={(v: number) => `${formatarNumero(v, 0)}%`} />

          <ReferenceLine
            y={100}
            stroke="var(--txt-muted)"
            strokeDasharray="4 4"
            label={{ value: 'necessidade', position: 'right', fill: 'var(--txt-muted)', fontSize: 11 }}
          />

          <Line
            type="monotone"
            dataKey="percCalorico"
            name="Calórica"
            stroke="var(--viz-serie-1)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />
          <Line
            type="monotone"
            dataKey="percProteico"
            name="Proteica"
            stroke="var(--viz-serie-2)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            dot={{ r: 4, fill: 'var(--viz-serie-2)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoEvolutivo
              return (
                <TooltipCartao
                  titulo={`${p.idadeMeses} meses`}
                  linhas={[
                    {
                      rotulo: 'Cobertura calórica',
                      cor: 'var(--viz-serie-1)',
                      valor: p.percCalorico != null ? `${formatarNumero(p.percCalorico, 1)}%` : '—',
                    },
                    {
                      rotulo: 'Cobertura proteica',
                      cor: 'var(--viz-serie-2)',
                      valor: p.percProteico != null ? `${formatarNumero(p.percProteico, 1)}%` : '—',
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
 * Necessidade energética contra o que a dieta de fato oferta.
 *
 * As duas em kcal/dia — **um eixo só**. A necessidade é tracejada por ser alvo,
 * e a oferta é a série cheia: é ela que se mexe.
 */
export function IngestaoEnergetica({ evolucao }: { evolucao: PontoEvolutivo[] }) {
  const dados = evolucao.filter((p) => p.vet != null || p.caloriasTotais != null)

  if (dados.length === 0) {
    return (
      <div>
        <TituloGrafico>Ingestão energética</TituloGrafico>
        <GraficoVazio>
          Nenhuma avaliação com VET ou dieta no período. O VET das DRIs é definido
          até 35 meses.
        </GraficoVazio>
      </div>
    )
  }

  return (
    <div>
      <TituloGrafico referencia="Necessidade pelas DRIs 2002 × oferta da dieta prescrita">
        Ingestão energética
      </TituloGrafico>

      <ResponsiveContainer width="100%" height={280}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />
          <XAxis dataKey="idadeMeses" {...EIXO} />
          <YAxis {...EIXO} width={56} tickFormatter={(v: number) => formatarNumero(v, 0)} />

          <Line
            type="monotone"
            dataKey="vet"
            name="Necessidade (VET)"
            stroke="var(--txt-muted)"
            strokeWidth={2}
            strokeDasharray="5 4"
            connectNulls
            isAnimationActive={false}
            dot={false}
          />
          <Line
            type="monotone"
            dataKey="caloriasTotais"
            name="Oferta da dieta"
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
              const p = payload[0].payload as PontoEvolutivo
              return (
                <TooltipCartao
                  titulo={`${p.idadeMeses} meses`}
                  linhas={[
                    {
                      rotulo: 'Necessidade',
                      valor: p.vet != null ? `${formatarNumero(p.vet, 0)} kcal/dia` : '—',
                    },
                    {
                      rotulo: 'Oferta',
                      cor: 'var(--viz-serie-1)',
                      valor:
                        p.caloriasTotais != null
                          ? `${formatarNumero(p.caloriasTotais, 0)} kcal/dia`
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
