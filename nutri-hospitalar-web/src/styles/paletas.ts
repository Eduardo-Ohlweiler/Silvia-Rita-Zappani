import type { PaletaSistema } from '@/types/configuracao'

/**
 * O catálogo das paletas, para a tela de Configurações gerais — **sem um hex
 * sequer**. As cores vivem só no `theme.css`; a prévia de cada cartão é uma
 * miniatura com `data-paleta` no próprio elemento, e as variáveis CSS valem na
 * subárvore. Uma segunda cópia das cores aqui envelheceria calada.
 *
 * `atributo` é o valor de `data-paleta` no `<html>`. A padrão não tem: sem o
 * atributo, valem os blocos originais do `theme.css`, intocados.
 *
 * `menuEscuro` são as variantes de docs/05 §11.6: as mesmas cores, com o menu
 * lateral e o cabeçalho nos tons escuros da paleta. Elas não têm CSS próprio —
 * a moldura recebe `data-theme="dark"` e usa o escuro da paleta base.
 */
export interface Paleta {
  chave: PaletaSistema
  atributo: string | null
  rotulo: string
  descricao: string
  menuEscuro: boolean
}

/** As bases, na ordem do círculo cromático, a partir da padrão. */
const BASES: Omit<Paleta, 'menuEscuro'>[] = [
  { chave: 'PADRAO', atributo: null, rotulo: 'Padrão', descricao: 'Azul marinho da marca' },
  { chave: 'MENTA', atributo: 'menta', rotulo: 'Menta', descricao: 'Verde claro, fresco' },
  { chave: 'FLORESTA', atributo: 'floresta', rotulo: 'Floresta', descricao: 'Verde profundo' },
  { chave: 'JADE', atributo: 'jade', rotulo: 'Jade', descricao: 'Verde-azulado sereno' },
  { chave: 'OCEANO', atributo: 'oceano', rotulo: 'Oceano', descricao: 'Azul petróleo' },
  { chave: 'INDIGO', atributo: 'indigo', rotulo: 'Índigo', descricao: 'Azul-violeta sobre fundo creme' },
  { chave: 'LAVANDA', atributo: 'lavanda', rotulo: 'Lavanda', descricao: 'Roxo suave' },
  { chave: 'AMEIXA', atributo: 'ameixa', rotulo: 'Ameixa', descricao: 'Roxo intenso' },
  { chave: 'MALVA', atributo: 'malva', rotulo: 'Malva', descricao: 'Rosa antigo' },
  { chave: 'CAFE', atributo: 'cafe', rotulo: 'Café', descricao: 'Marrom acolhedor' },
]

export const PALETAS: Paleta[] = [
  ...BASES.map((b) => ({ ...b, menuEscuro: false })),
  ...BASES.map((b) => ({
    ...b,
    chave: `${b.chave}_MENU_ESCURO` as PaletaSistema,
    descricao: `${b.descricao}, com menu e cabeçalho escuros`,
    menuEscuro: true,
  })),
]

export function paletaPorChave(chave: PaletaSistema): Paleta {
  return PALETAS.find((p) => p.chave === chave) ?? PALETAS[0]
}

/** Valor desconhecido (cache antigo, paleta removida) cai na padrão. */
export function ehPaletaValida(valor: unknown): valor is PaletaSistema {
  return PALETAS.some((p) => p.chave === valor)
}
