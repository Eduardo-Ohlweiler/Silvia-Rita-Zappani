import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { Contagem, ContagemFaixa, FaixaOms, PontoPeriodo } from '@/types/pediatria'
import { formatarNumero } from '@/utils/format'
import { EIXO, GRADE, GraficoVazio, TooltipCartao } from '@/components/graficos/chrome'

/** Uma série só: o título nomeia, não precisa de legenda. */
export function AvaliacoesPorPeriodo({ dados }: { dados: PontoPeriodo[] }) {
  if (dados.length === 0) return <GraficoVazio>Sem avaliações no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={260}>
      <AreaChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="periodo" {...EIXO} />
        <YAxis {...EIXO} width={40} allowDecimals={false} />
        <Area
          type="monotone"
          dataKey="avaliacoes"
          name="Avaliações"
          stroke="var(--viz-serie-1)"
          strokeWidth={2}
          fill="var(--viz-faixa-externa)"
          isAnimationActive={false}
          dot={{ r: 3, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
        />
        <Tooltip
          cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as PontoPeriodo
            return (
              <TooltipCartao
                titulo={p.periodo}
                linhas={[
                  { rotulo: 'Avaliações', cor: 'var(--viz-serie-1)', valor: p.avaliacoes },
                ]}
              />
            )
          }}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}

const COR_FAIXA: Record<FaixaOms, string> = {
  BAIXA: 'var(--viz-serie-1)',
  ADEQUADA: 'var(--viz-serie-3)',
  ALTA: 'var(--viz-serie-2)',
}

/**
 * As três classificações da OMS numa barra empilhada por índice.
 *
 * <p>Substitui as três pizzas do eroERP de propósito: pizza serve para
 * parte-do-todo num olhar, não para comparar valores próximos — e aqui a
 * pergunta é justamente comparativa ("qual índice tem mais criança fora da
 * faixa?"). Empilhadas lado a lado, os três se comparam de uma vez.
 */
export function DistribuicaoClassificacoes({
  peso,
  estatura,
  imc,
}: {
  peso: ContagemFaixa[]
  estatura: ContagemFaixa[]
  imc: ContagemFaixa[]
}) {
  const linhas = [
    { indice: 'Peso / idade', faixas: peso },
    { indice: 'Estatura / idade', faixas: estatura },
    { indice: 'IMC / idade', faixas: imc },
  ]

  const total = linhas.reduce((s, l) => s + l.faixas.reduce((a, f) => a + f.quantidade, 0), 0)
  if (total === 0) return <GraficoVazio>Sem avaliações classificadas no período.</GraficoVazio>

  const dados = linhas.map((l) => ({
    indice: l.indice,
    BAIXA: quantidade(l.faixas, 'BAIXA'),
    ADEQUADA: quantidade(l.faixas, 'ADEQUADA'),
    ALTA: quantidade(l.faixas, 'ALTA'),
    naoClassificado: l.faixas.find((f) => !f.faixa)?.quantidade ?? 0,
    rotulos: Object.fromEntries(l.faixas.map((f) => [f.faixa ?? 'NULO', f.rotulo])),
  }))

  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 12, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="indice" {...EIXO} width={110} />

        {(['BAIXA', 'ADEQUADA', 'ALTA'] as FaixaOms[]).map((faixa) => (
          <Bar
            key={faixa}
            dataKey={faixa}
            stackId="f"
            fill={COR_FAIXA[faixa]}
            /* 2px da superfície entre segmentos, no lugar de contorno */
            stroke="var(--surface)"
            strokeWidth={2}
            isAnimationActive={false}
            maxBarSize={26}
          >
            {/* Rótulo dentro do segmento só quando cabe — número espremido
                cortado pela borda é pior que número nenhum. */}
            <LabelList
              dataKey={faixa}
              position="center"
              fill="var(--txt-inverse)"
              fontSize={11}
              formatter={(v) => (Number(v) > 0 ? String(v) : '')}
            />
          </Bar>
        ))}
        <Bar
          dataKey="naoClassificado"
          stackId="f"
          fill="var(--line-strong)"
          stroke="var(--surface)"
          strokeWidth={2}
          isAnimationActive={false}
          maxBarSize={26}
        />

        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as (typeof dados)[number]
            return (
              <TooltipCartao
                titulo={p.indice}
                linhas={[
                  { rotulo: p.rotulos.BAIXA ?? 'Baixa', cor: COR_FAIXA.BAIXA, valor: p.BAIXA },
                  { rotulo: p.rotulos.ADEQUADA ?? 'Adequada', cor: COR_FAIXA.ADEQUADA, valor: p.ADEQUADA },
                  { rotulo: p.rotulos.ALTA ?? 'Alta', cor: COR_FAIXA.ALTA, valor: p.ALTA },
                  ...(p.naoClassificado > 0
                    ? [{ rotulo: 'Não classificado', valor: p.naoClassificado }]
                    : []),
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
 * Categoria sem ordem natural (nome de fórmula) → **uma cor só** para todas as
 * barras. Colorir cada uma por quantidade duplicaria o que o comprimento já diz.
 */
export function BarrasNominais({ dados }: { dados: Contagem[] }) {
  if (dados.length === 0) return <GraficoVazio>Sem dados no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={Math.max(180, dados.length * 34 + 40)}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 32, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="rotulo" {...EIXO} width={130} />
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
            const p = payload[0].payload as Contagem
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[{ rotulo: 'Avaliações', cor: 'var(--viz-serie-1)', valor: p.quantidade }]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * Faixa etária tem ordem, então a cor a acompanha: rampa de uma cor só, cinco
 * degraus. A faixa acima de 60 meses fica **fora da rampa**, em cinza — está
 * fora do alcance das curvas da OMS, e a cor precisa dizer isso.
 */
export function BarrasFaixaEtaria({ dados }: { dados: Contagem[] }) {
  if (dados.every((d) => d.quantidade === 0))
    return <GraficoVazio>Sem avaliações no período.</GraficoVazio>

  const RAMPA = [
    'var(--viz-ordinal-1)',
    'var(--viz-ordinal-2)',
    'var(--viz-ordinal-3)',
    'var(--viz-ordinal-4)',
    'var(--viz-ordinal-5)',
  ]

  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={dados} margin={{ top: 16, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="rotulo" {...EIXO} interval={0} fontSize={11} />
        <YAxis {...EIXO} width={40} allowDecimals={false} />
        <Bar dataKey="quantidade" isAnimationActive={false} maxBarSize={48} radius={[4, 4, 0, 0]}>
          {dados.map((d, i) => (
            <Cell
              key={d.rotulo}
              fill={i < RAMPA.length ? RAMPA[i] : 'var(--txt-muted)'}
              stroke="var(--surface)"
              strokeWidth={2}
            />
          ))}
          <LabelList
            dataKey="quantidade"
            position="top"
            fill="var(--txt-secondary)"
            fontSize={12}
            formatter={(v) => (Number(v) > 0 ? String(v) : '')}
          />
        </Bar>
        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as Contagem
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[{ rotulo: 'Avaliações', valor: p.quantidade }]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/** Duas categorias não são gráfico — é indicador. */
export function ProporcaoSexo({ dados }: { dados: Contagem[] }) {
  const total = dados.reduce((s, d) => s + d.quantidade, 0)
  if (total === 0) return <GraficoVazio>Sem avaliações no período.</GraficoVazio>

  return (
    <ul className="flex flex-col gap-3">
      {dados.map((d, i) => {
        const parte = (d.quantidade / total) * 100
        return (
          <li key={d.rotulo} className="flex flex-col gap-1">
            <div className="flex items-baseline justify-between gap-2 text-caption">
              <span className="text-txt-secondary">{d.rotulo}</span>
              <span className="numeric text-txt">
                {d.quantidade} · {formatarNumero(parte, 1)}%
              </span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-surface-alt">
              <div
                className="h-full rounded-full"
                style={{
                  width: `${parte}%`,
                  background: i === 0 ? 'var(--viz-serie-1)' : 'var(--viz-serie-2)',
                }}
              />
            </div>
          </li>
        )
      })}
    </ul>
  )
}

function quantidade(faixas: ContagemFaixa[], faixa: FaixaOms): number {
  return faixas.find((f) => f.faixa === faixa)?.quantidade ?? 0
}
