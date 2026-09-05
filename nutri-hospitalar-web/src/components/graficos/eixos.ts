import { formatarNumero } from '@/utils/format'

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

/**
 * O rótulo do eixo Y, na notação do país.
 *
 * Sem isto o recharts imprime o número cru, com o `toString()` do JavaScript —
 * e aí `9,263 kg` sai como **`9.263`**, que em português se lê "nove mil
 * duzentos e sessenta e três". O peso de uma criança de 14 meses aparecia
 * assim, no eixo do gráfico de crescimento. Os eixos da UTI escaparam por
 * acaso: as escalas de lá caíam em números inteiros, e inteiro não tem
 * separador decimal para errar.
 *
 * É a mesma família de `paraNumero`, e a mesma lição: **o ponto não é
 * separador de milhar aqui**. O `casas` já existia em todo gráfico, para o
 * tooltip; era só o eixo que não o usava.
 */
export const tickNumerico = (casas: number) => (v: number) => formatarNumero(v, casas)

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
