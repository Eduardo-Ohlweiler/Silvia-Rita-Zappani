import { useMemo } from 'react'
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { CurvaOmsPonto, FaixaOms, PontoEvolutivo } from '@/types/pediatria'
import { formatarNumero } from '@/utils/format'
import { EIXO, GRADE, GraficoVazio, TituloGrafico, TooltipCartao } from '@/components/graficos/chrome'

/** Qual medida a curva desenha. */
export type Medida = 'peso' | 'estatura' | 'imc'

const CONFIG: Record<Medida, { titulo: string; unidade: string; casas: number }> = {
  peso: { titulo: 'Peso para a idade', unidade: 'kg', casas: 2 },
  estatura: { titulo: 'Estatura para a idade', unidade: 'cm', casas: 1 },
  imc: { titulo: 'IMC para a idade', unidade: 'kg/m²', casas: 2 },
}

const ROTULO_FAIXA: Record<FaixaOms, string> = {
  BAIXA: 'abaixo do P15',
  ADEQUADA: 'dentro da faixa',
  ALTA: 'acima do P85',
}

interface Props {
  medida: Medida
  curva: CurvaOmsPonto[]
  evolucao: PontoEvolutivo[]
  pacienteNome: string
}

/**
 * A criança plotada dentro das faixas de crescimento da OMS.
 *
 * Duas faixas concêntricas, como na curva impressa: a externa P3–P97 situa o
 * extremo, a interna P15–P85 é a que classifica. Elas são **contexto** — daí
 * serem claras e sem contorno; quem carrega a informação é a linha do paciente.
 *
 * <p><b>O ponto não é colorido por classificação.</b> Onde ele cai em relação à
 * faixa já diz se está adequado — colorir também gastaria o canal de cor com o
 * que o gráfico mostra sozinho. A classificação vem escrita no tooltip.
 */
export function CurvaCrescimento({ medida, curva, evolucao, pacienteNome }: Props) {
  const { titulo, unidade, casas } = CONFIG[medida]

  const dados = useMemo(() => montar(medida, curva, evolucao), [medida, curva, evolucao])

  const temMedida = evolucao.some((p) => valorDe(p, medida) != null)

  if (!temMedida) {
    return (
      <div>
        <TituloGrafico referencia="OMS · 0 a 60 meses">{titulo}</TituloGrafico>
        <GraficoVazio>
          {medida === 'peso'
            ? 'Nenhuma avaliação com peso no período.'
            : `Nenhuma avaliação com ${medida === 'estatura' ? 'estatura' : 'IMC'} no período. ` +
              'O IMC depende da estatura, que é opcional na avaliação.'}
        </GraficoVazio>
      </div>
    )
  }

  return (
    <div>
      <TituloGrafico referencia="Faixas da OMS · 0 a 60 meses">{titulo}</TituloGrafico>

      {/* A altura inclui a faixa do eixo X — container curto demais empurra os
          rótulos para uma rolagem interna dentro do card. */}
      <ResponsiveContainer width="100%" height={300}>
        <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
          <CartesianGrid {...GRADE} />

          <XAxis
            dataKey="idadeMeses"
            type="number"
            domain={['dataMin', 'dataMax']}
            {...EIXO}
            label={{
              value: 'idade (meses)',
              position: 'insideBottom',
              offset: -2,
              fill: 'var(--txt-muted)',
              fontSize: 12,
            }}
          />
          <YAxis
            {...EIXO}
            width={48}
            domain={['auto', 'auto']}
            tickFormatter={(v: number) => formatarNumero(v, 0)}
          />

          {/* Empilhadas: a externa é a base, a interna soma por cima. É como se
              desenha faixa em recharts sem inventar um segundo eixo. */}
          <Area
            type="monotone"
            dataKey="baseExterna"
            stackId="externa"
            stroke="none"
            fill="none"
            isAnimationActive={false}
            legendType="none"
            tooltipType="none"
          />
          <Area
            type="monotone"
            dataKey="alturaExterna"
            stackId="externa"
            name="Faixa P3–P97"
            stroke="none"
            fill="var(--viz-faixa-externa)"
            isAnimationActive={false}
            tooltipType="none"
          />

          <Area
            type="monotone"
            dataKey="baseInterna"
            stackId="interna"
            stroke="none"
            fill="none"
            isAnimationActive={false}
            legendType="none"
            tooltipType="none"
          />
          <Area
            type="monotone"
            dataKey="alturaInterna"
            stackId="interna"
            name="Faixa P15–P85"
            stroke="none"
            fill="var(--viz-faixa-interna)"
            isAnimationActive={false}
            tooltipType="none"
          />

          <Line
            type="monotone"
            dataKey="paciente"
            name={pacienteNome}
            stroke="var(--viz-serie-2)"
            strokeWidth={2}
            connectNulls
            isAnimationActive={false}
            /* Marcador com anel da superfície: separa o ponto da faixa sem
               desenhar borda em volta da marca. */
            dot={{ r: 4, fill: 'var(--viz-serie-2)', stroke: 'var(--surface)', strokeWidth: 2 }}
            activeDot={{ r: 6, fill: 'var(--viz-serie-2)', stroke: 'var(--surface)', strokeWidth: 2 }}
          />

          <Tooltip
            /* Área de acerto maior que a marca — um ponto de 4px não se acerta
               no centro com o dedo. */
            cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as PontoGrafico
              if (p.paciente == null) return null

              return (
                <TooltipCartao
                  titulo={`${p.idadeMeses} meses`}
                  linhas={[
                    {
                      rotulo: pacienteNome,
                      cor: 'var(--viz-serie-2)',
                      valor: `${formatarNumero(p.paciente, casas)} ${unidade}`,
                    },
                    {
                      rotulo: 'Classificação',
                      valor: p.faixa ? ROTULO_FAIXA[p.faixa] : '—',
                    },
                    { rotulo: 'P15 – P85', valor: `${formatarNumero(p.p15, casas)} – ${formatarNumero(p.p85, casas)}` },
                    { rotulo: 'P3 – P97', valor: `${formatarNumero(p.p3, casas)} – ${formatarNumero(p.p97, casas)}` },
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

// ─────────────────────────────────────────────────────────────────────

interface PontoGrafico {
  idadeMeses: number
  baseExterna: number
  alturaExterna: number
  baseInterna: number
  alturaInterna: number
  p3: number
  p15: number
  p85: number
  p97: number
  paciente: number | null
  faixa: FaixaOms | null
}

function valorDe(p: PontoEvolutivo, medida: Medida): number | null | undefined {
  return medida === 'peso' ? p.peso : medida === 'estatura' ? p.estatura : p.imc
}

function faixaDe(p: PontoEvolutivo, medida: Medida): FaixaOms | null | undefined {
  return medida === 'peso'
    ? p.classifPesoIdade
    : medida === 'estatura'
      ? p.classifEstaturaIdade
      : p.classifImcIdade
}

/**
 * Junta a curva de referência com os pontos do paciente, um registro por mês.
 *
 * As faixas viram base + altura porque a área empilhada do recharts desenha a
 * partir de zero: a base é invisível e a altura é a faixa de fato.
 */
function montar(
  medida: Medida,
  curva: CurvaOmsPonto[],
  evolucao: PontoEvolutivo[],
): PontoGrafico[] {
  const medidoPorMes = new Map<number, PontoEvolutivo>()
  // Havendo mais de uma avaliação no mesmo mês de vida, a mais recente vence.
  for (const p of evolucao) medidoPorMes.set(p.idadeMeses, p)

  return curva.map((c) => {
    const p3 = percentil(c, medida, 'P3')
    const p15 = percentil(c, medida, 'P15')
    const p85 = percentil(c, medida, 'P85')
    const p97 = percentil(c, medida, 'P97')

    const medido = medidoPorMes.get(c.idadeMeses)
    const valor = medido ? valorDe(medido, medida) : null

    return {
      idadeMeses: c.idadeMeses,
      baseExterna: p3,
      alturaExterna: p97 - p3,
      baseInterna: p15,
      alturaInterna: p85 - p15,
      p3,
      p15,
      p85,
      p97,
      paciente: valor ?? null,
      faixa: (medido ? faixaDe(medido, medida) : null) ?? null,
    }
  })
}

function percentil(c: CurvaOmsPonto, medida: Medida, p: 'P3' | 'P15' | 'P85' | 'P97'): number {
  return c[`${medida}${p}` as keyof CurvaOmsPonto] as number
}
