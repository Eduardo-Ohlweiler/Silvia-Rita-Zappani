import { useState } from 'react'
import { TButton, TPage } from '@/components/common'
import { CalculoUti } from '@/components/uti/CalculoUti'
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

  return (
    <TPage
      title="Calculadora de terapia nutricional"
      subtitle="Cálculo rápido, sem paciente. Nada aqui é gravado."
      actions={
        <TButton
          variant="secondary"
          disabled={entradasVazias(entradas)}
          onClick={() => setEntradas(ENTRADAS_UTI_VAZIAS)}
        >
          Limpar
        </TButton>
      }
    >
      <CalculoUti entradas={entradas} onChange={setEntradas} />
    </TPage>
  )
}
