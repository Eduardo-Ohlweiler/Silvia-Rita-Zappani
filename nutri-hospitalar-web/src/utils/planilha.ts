/**
 * Exportação para planilha.
 *
 * **CSV, e não XLSX.** XLSX exigiria uma biblioteca de ~1 MB para escrever um
 * formato que, aqui, ninguém precisa: as listas não têm fórmula, aba nem
 * formatação condicional. CSV com BOM e ponto-e-vírgula abre no Excel em
 * português com um duplo clique, e o arquivo é legível por qualquer coisa.
 *
 * Três detalhes que decidem se o arquivo abre certo no Brasil:
 *
 * 1. **BOM UTF-8.** Sem ele o Excel lê o arquivo como Latin-1 e "Avaliação"
 *    vira "AvaliaÃ§Ã£o". É o defeito mais comum de exportação de CSV.
 * 2. **Ponto-e-vírgula como separador**, porque a vírgula é o nosso decimal —
 *    e o Excel em pt-BR já espera `;`.
 * 3. **Decimal com vírgula**, senão a coluna chega como texto e não soma.
 */

/** O separador que o Excel em português espera. */
const SEPARADOR = ';'

/**
 * Escapa uma célula.
 *
 * <p>Além das aspas e do separador, trata a <b>injeção de fórmula</b>: uma
 * célula que começa com <code>=</code>, <code>+</code>, <code>-</code>,
 * <code>@</code>, tab ou CR é executada como fórmula ao abrir a planilha. Nome
 * de paciente é texto que o usuário digitou, e um paciente chamado
 * <code>=cmd|…</code> não pode virar comando na máquina de quem abre o arquivo.
 * O prefixo com apóstrofo é o que o OWASP recomenda, e o Excel o esconde.
 */
function celula(valor: string): string {
  const perigoso = /^[=+\-@\t\r]/.test(valor)
  const texto = perigoso ? `'${valor}` : valor

  return /["\n\r;]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto
}

export interface ColunaExportavel<T> {
  titulo: string
  /** O texto da célula, já formatado em pt-BR. */
  valor: (linha: T) => string
  /** Alinha à direita no relatório impresso. Não muda o CSV. */
  numerica?: boolean
}

/**
 * Monta o CSV e entrega ao navegador como download.
 *
 * <p>O arquivo é construído <b>no cliente</b>, dos dados que já vieram pela
 * API com o token na mão. Não há endpoint de exportação, e por isso também não
 * há o risco clássico de link de download que escapa do JWT.
 */
export function baixarCsv<T>(
  nomeArquivo: string,
  colunas: ColunaExportavel<T>[],
  linhas: T[],
): void {
  const conteudo = [
    colunas.map((c) => celula(c.titulo)).join(SEPARADOR),
    ...linhas.map((l) => colunas.map((c) => celula(c.valor(l))).join(SEPARADOR)),
  ].join('\r\n')

  // ﻿ é o BOM. Sem ele o Excel destrói todo acento do arquivo.
  const blob = new Blob([`﻿${conteudo}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `${nomeArquivo}.csv`
  link.click()

  // Sem revogar, o blob fica na memória da aba até a página ser descarregada.
  URL.revokeObjectURL(url)
}

/** Nome de arquivo com a data de hoje, sem acento e sem espaço. */
export function nomeDeArquivo(base: string): string {
  const hoje = new Date().toISOString().slice(0, 10)
  const limpo = base
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase()
  return `${limpo}-${hoje}`
}
