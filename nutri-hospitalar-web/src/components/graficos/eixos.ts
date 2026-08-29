/**
 * Chrome de eixo e grade, comum a todos os gráficos do sistema.
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

/**
 * A chave de um ponto, no formato que o `dataKey` do recharts aceita.
 *
 * Recharts 3 tipa `dataKey` como um condicional sobre o tipo do ponto
 * (`TypedDataKey`). Dentro de um componente **genérico** o TypeScript não
 * consegue resolver esse condicional — o tipo do ponto ainda é um parâmetro —
 * e recusa até `keyof T & string`, que é exatamente o que ele pediria se o
 * tipo fosse concreto.
 *
 * Esta função concentra a conversão num lugar só, em vez de espalhar `as` por
 * cada eixo e cada série. A chave já vem tipada como `keyof T` na assinatura de
 * quem chama, então a segurança está garantida antes de chegar aqui.
 */
export function chaveDoPonto<T>(chave: keyof T & string): never {
  return chave as never
}
