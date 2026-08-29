import { TButton } from './TButton'

/**
 * Manda a página para o papel.
 *
 * **Não existe geração de PDF no servidor, e é decisão.** Imprimir o DOM que já
 * está na tela garante que a folha e o monitor mostrem o mesmo número — um PDF
 * montado no backend seria uma segunda montagem do mesmo documento, e duas
 * montagens divergem com o tempo, com a do papel envelhecendo calada. Num
 * sistema que prescreve dieta, essa divergência é o problema, não a estética do
 * arquivo.
 *
 * O que sai na folha é regido por `@media print` em `styles/theme.css`: some a
 * interface — sidebar, header, botões, filtros — e fica o conteúdo com a sua
 * procedência. O próprio botão some, porque é `<button>`.
 *
 * A numeração de página vem do diálogo do navegador: `counter(page)` dentro de
 * `@page` não é suportado por nenhum deles.
 */
export function TBotaoImprimir({ rotulo = 'Imprimir' }: { rotulo?: string }) {
  return (
    <TButton variant="secondary" onClick={() => window.print()}>
      {rotulo}
    </TButton>
  )
}
