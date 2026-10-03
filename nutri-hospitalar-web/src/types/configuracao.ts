/**
 * Configurações gerais do sistema. Espelha o enum `PaletaSistema` do backend —
 * o `npm run paletas` confere que as listas (e o `theme.css`) batem.
 */
type PaletaBase =
  | 'PADRAO'
  | 'MENTA'
  | 'FLORESTA'
  | 'JADE'
  | 'OCEANO'
  | 'INDIGO'
  | 'LAVANDA'
  | 'AMEIXA'
  | 'MALVA'
  | 'CAFE'

/** A base, ou a mesma base com menu e cabeçalho escuros (docs/05 §11.6). */
export type PaletaSistema = PaletaBase | `${PaletaBase}_MENU_ESCURO`

export interface Aparencia {
  paleta: PaletaSistema
}
