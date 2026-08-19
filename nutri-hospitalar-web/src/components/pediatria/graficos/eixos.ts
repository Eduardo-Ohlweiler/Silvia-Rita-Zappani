/**
 * Chrome de eixo e grade, comum a todos os gráficos da pediatria.
 *
 * Mora fora do arquivo de componentes porque exportar constante junto com
 * componente quebra o fast refresh do Vite.
 */
export const EIXO = {
  stroke: 'var(--line-strong)',
  tick: { fill: 'var(--txt-muted)', fontSize: 12 },
  tickLine: false,
} as const

export const GRADE = {
  stroke: 'var(--line)',
  strokeDasharray: undefined,
  vertical: false,
} as const
