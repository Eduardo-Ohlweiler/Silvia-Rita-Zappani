import { Folha, Secao, TabelaDoc, TiraIndicadores } from '@/components/impressao/Folha'
import type { ColunaExportavel } from '@/utils/planilha'

/** Um filtro que estava aplicado, para o papel dizer o recorte que mostra. */
export interface FiltroAplicado {
  rotulo: string
  valor: string
}

/**
 * Relatório de uma listagem, **no recorte que estava na tela**.
 *
 * <p>Duas coisas fazem deste papel um relatório e não um print de grid:
 *
 * <ol>
 *   <li><b>Os filtros aplicados vêm escritos no cabeçalho.</b> Uma folha com 40
 *       linhas e sem o recorte que as produziu é indefensável — quem a recebe
 *       não sabe se são todos os pacientes ou os de um mês. Esta é a diferença
 *       entre relatório e captura de tela.</li>
 *   <li><b>O relatório cobre o filtro inteiro, não a página visível.</b> Quem
 *       carrega os dados é {@code TAcoesDeExportacao}, que refaz a consulta sem
 *       paginação antes de imprimir.</li>
 * </ol>
 *
 * As colunas são as mesmas da planilha — um só lugar define o que é relevante
 * naquela lista, e é impossível o CSV e o papel discordarem.
 */
export function DocumentoLista<T>({
  titulo,
  colunas,
  linhas,
  filtros,
  chaveDe,
  resumo,
  nota,
  truncado,
}: {
  titulo: string
  colunas: ColunaExportavel<T>[]
  linhas: T[]
  filtros: FiltroAplicado[]
  chaveDe: (linha: T, indice: number) => string | number
  /** Números do topo — total, médias, o que resume a lista. */
  resumo?: { rotulo: string; valor: string }[]
  nota?: string
  /** Quantas linhas ficaram de fora do teto de exportação, se ficaram. */
  truncado?: number
}) {
  const aplicados = filtros.filter((f) => f.valor)

  return (
    <Folha
      titulo={titulo}
      referencia={
        <>
          {linhas.length} {linhas.length === 1 ? 'registro' : 'registros'}
          {aplicados.length > 0
            ? ` · ${aplicados.map((f) => `${f.rotulo}: ${f.valor}`).join(' · ')}`
            : ' · sem filtro aplicado'}
        </>
      }
    >
      {resumo && resumo.length > 0 && <TiraIndicadores itens={resumo} />}

      <Secao titulo={titulo} nota={nota}>
        <TabelaDoc
          linhas={linhas}
          chaveDe={chaveDe}
          vazio="Nenhum registro para o filtro aplicado."
          colunas={colunas.map((c) => ({
            titulo: c.titulo,
            numerica: c.numerica,
            celula: (l: T) => c.valor(l),
          }))}
        />
        {truncado != null && truncado > 0 && (
          <p className="folha-vazio">
            Mais {truncado} {truncado === 1 ? 'registro atende' : 'registros atendem'} ao filtro e
            não {truncado === 1 ? 'coube' : 'couberam'} neste relatório. Estreite o período para
            que a folha cubra tudo — um relatório que corta em silêncio não serve de registro.
          </p>
        )}
      </Secao>
    </Folha>
  )
}
