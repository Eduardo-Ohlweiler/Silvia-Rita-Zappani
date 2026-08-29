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
  carregar: (limite: number) => Promise<ResultadoDaCarga<T>>
  /** Entrega as linhas à tela, que monta o `documento` da `TPage`. */
  aoCarregarParaImprimir: (resultado: ResultadoDaCarga<T> | undefined) => void
}) {
  const [ocupado, setOcupado] = useState(false)
  const vaiImprimir = useRef(false)

  const planilha = useCallback(async () => {
    setOcupado(true)
    try {
      const { linhas, restantes } = await carregar(TETO_DE_EXPORTACAO)
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
  }, [carregar, colunas, nome])

  const relatorio = useCallback(async () => {
    setOcupado(true)
    try {
      const resultado = await carregar(TETO_DE_EXPORTACAO)
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
  }, [carregar, aoCarregarParaImprimir])

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
