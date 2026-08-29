import { useState } from 'react'
import { TBotaoImprimir, TButton, TPage } from '@/components/common'
import { CalculoUti } from '@/components/uti/CalculoUti'
import { DocumentoAvaliacaoUti } from '@/components/uti/impressao/DocumentoAvaliacaoUti'
import { paraRequisicao } from '@/components/uti/entradas'
import type { ResultadoUti } from '@/types/uti'
import {
  ENTRADAS_UTI_VAZIAS,
  entradasVazias,
  type EntradasUti,
} from '@/components/uti/entradas'

/**
 * Cálculo avulso: sem paciente e sem gravar nada.
 *
 * É a mesma tela da avaliação sem o que a avaliação tem em volta — o miolo é o
 * mesmo componente, então as duas nunca divergem. Serve para conferir uma
 * prescrição à beira do leito sem abrir prontuário.
 */
export function UtiCalculadora() {
  const [entradas, setEntradas] = useState<EntradasUti>(ENTRADAS_UTI_VAZIAS)
  const [resultado, setResultado] = useState<ResultadoUti | null>(null)

  return (
    <TPage
      title="Calculadora de terapia nutricional"
      subtitle="Cálculo rápido, sem paciente. Nada aqui é gravado."
      actions={
        <>
          {resultado && <TBotaoImprimir />}
          <TButton
            variant="secondary"
            disabled={entradasVazias(entradas)}
            onClick={() => setEntradas(ENTRADAS_UTI_VAZIAS)}
          >
            Limpar
          </TButton>
        </>
      }
      documento={
        resultado && (
          <DocumentoAvaliacaoUti entradas={paraRequisicao(entradas)} resultado={resultado} />
        )
      }
    >
      <CalculoUti entradas={entradas} onChange={setEntradas} onResultado={setResultado} />
    </TPage>
  )
}
