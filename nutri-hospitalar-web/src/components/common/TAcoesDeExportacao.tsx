import { useCallback, useEffect, useRef, useState } from 'react'
import { toast } from 'react-toastify'
import { TButton } from './TButton'
import { handleApiError } from '@/services/api'
import { baixarCsv, nomeDeArquivo, type ColunaExportavel } from '@/utils/planilha'

/**
 * Teto de linhas de uma exportação.
 *
 * <p>Existe para o navegador não tentar montar uma folha de 40 mil linhas, e é
 * <b>anunciado</b> quando corta: o relatório diz quantos registros ficaram de
 * fora e pede para estreitar o período. Truncar em silêncio é o que faz uma
 * exportação parecer completa sem ser — e num prontuário isso é pior do que
 * não exportar.
 */
export const TETO_DE_EXPORTACAO = 2000

/**
 * Quantas linhas cabem numa requisição.
 *
 * <p>É o {@code spring.data.web.pageable.max-page-size} da API, e ele não é
 * negociável a partir do cliente: pedir {@code size=2000} devolve <b>100</b>,
 * calado. A exportação pedia 2.000 numa chamada só e recebia 100 — o teto real
 * era vinte vezes menor que o anunciado aqui e no {@code CLAUDE.md}, e a
 * planilha do log de acesso saía com 100 de 175. O aviso de corte disparava,
 * então ninguém perdia dado sem saber; mas quem exportasse um mês de
 * acompanhamento receberia "estreite o período" todo dia, sem entender por quê.
 *
 * <p>Subir o limite do servidor resolveria pelo lado errado — ele existe para
 * que nenhuma listagem devolva página gigante. Quem precisa de mais páginas
 * as pede, e é o que a exportação faz agora.
 */
const LINHAS_POR_REQUISICAO = 100

/** Uma página de resultados, como a listagem a devolve. */
export interface PaginaDaCarga<T> {
  linhas: T[]
  /** Quantas linhas atendem ao filtro no total, não só nesta página. */
  total: number
}

export interface ResultadoDaCarga<T> {
  linhas: T[]
  /** Quantas linhas atendem ao filtro além das que vieram. */
  restantes: number
}

/**
 * Os dois botões de uma listagem: **relatório** e **planilha**, ambos no
 * recorte que está na tela.
 *
 * <p>O ponto que faz isto valer a pena: nenhum dos dois olha a página visível.
 * Os dois chamam {@code carregar}, que refaz a consulta com os mesmos filtros e
 * sem paginação — exportar "os 20 da página 1" seria a armadilha óbvia, e o
 * usuário só descobriria ao conferir o total.
 *
 * <p>As colunas são as mesmas nos dois caminhos, e por isso o papel e o CSV não
 * conseguem discordar sobre o que é relevante naquela lista.
 *
 * <p>Imprimir é em dois tempos: os dados chegam, o React pinta o documento, e
 * só então {@code window.print()}. Chamar antes imprimiria a folha anterior —
 * ou uma em branco.
 */
export function TAcoesDeExportacao<T>({
  nome,
  colunas,
  carregar,
  aoCarregarParaImprimir,
}: {
  /** Vira o nome do arquivo e o título do relatório. */
  nome: string
  colunas: ColunaExportavel<T>[]
  carregar: (limite: number, indice: number) => Promise<PaginaDaCarga<T>>
  /** Entrega as linhas à tela, que monta o `documento` da `TPage`. */
  aoCarregarParaImprimir: (resultado: ResultadoDaCarga<T> | undefined) => void
}) {
  const [ocupado, setOcupado] = useState(false)
  const vaiImprimir = useRef(false)

  /**
   * Junta páginas até cobrir o filtro ou bater no teto.
   *
   * <p>Para no primeiro dos três: alcançou o total que o servidor informou,
   * chegou ao teto, ou veio página vazia — esta última é a guarda contra laço
   * infinito se o total mentir.
   */
  const carregarTudo = useCallback(async (): Promise<ResultadoDaCarga<T>> => {
    const linhas: T[] = []
    let total = 0
    for (let indice = 0; linhas.length < TETO_DE_EXPORTACAO; indice++) {
      const pagina = await carregar(LINHAS_POR_REQUISICAO, indice)
      total = pagina.total
      if (pagina.linhas.length === 0) break
      linhas.push(...pagina.linhas)
      if (linhas.length >= total) break
    }
    return { linhas, restantes: Math.max(0, total - linhas.length) }
  }, [carregar])

  const planilha = useCallback(async () => {
    setOcupado(true)
    try {
      const { linhas, restantes } = await carregarTudo()
      if (linhas.length === 0) {
        toast.info('Nenhum registro para o filtro aplicado.')
        return
      }
      baixarCsv(nomeDeArquivo(nome), colunas, linhas)
      if (restantes > 0) {
        toast.warn(
          `A planilha traz ${linhas.length} registros; outros ${restantes} atendem ao filtro e ficaram de fora. Estreite o período.`,
        )
      }
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setOcupado(false)
    }
  }, [carregarTudo, colunas, nome])

  const relatorio = useCallback(async () => {
    setOcupado(true)
    try {
      const resultado = await carregarTudo()
      if (resultado.linhas.length === 0) {
        toast.info('Nenhum registro para o filtro aplicado.')
        return
      }
      aoCarregarParaImprimir(resultado)
      vaiImprimir.current = true
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setOcupado(false)
    }
  }, [carregarTudo, aoCarregarParaImprimir])

  // O documento só existe depois que o React pintou as linhas que acabaram de
  // chegar. Dois quadros: um para o commit, outro para o layout assentar.
  useEffect(() => {
    if (!vaiImprimir.current) return
    vaiImprimir.current = false

    const id = requestAnimationFrame(() => requestAnimationFrame(() => window.print()))
    return () => cancelAnimationFrame(id)
  })

  return (
    <>
      <TButton variant="secondary" onClick={relatorio} loading={ocupado}>
        Relatório
      </TButton>
      <TButton variant="secondary" onClick={planilha} loading={ocupado}>
        Planilha
      </TButton>
    </>
  )
}
