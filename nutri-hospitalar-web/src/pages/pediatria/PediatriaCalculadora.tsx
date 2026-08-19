import { useState } from 'react'
import { TButton, TPage } from '@/components/common'
import { CalculoPediatrico } from '@/components/pediatria/CalculoPediatrico'
import {
  ENTRADAS_VAZIAS,
  type EntradasCalculo,
} from '@/components/pediatria/entradas'

/**
 * Cálculo rápido: sem paciente e sem gravar nada.
 *
 * É a mesma tela da avaliação sem o que a avaliação tem em volta — o miolo é o
 * mesmo componente, então as duas nunca divergem.
 */
export function PediatriaCalculadora() {
  const [entradas, setEntradas] = useState<EntradasCalculo>(ENTRADAS_VAZIAS)

  const preenchido = Object.values(entradas).some((v) => v !== '')

  return (
    <TPage
      title="Calculadora pediátrica"
      subtitle="Cálculo rápido, sem paciente. Nada aqui é gravado."
      actions={
        <TButton
          variant="secondary"
          disabled={!preenchido}
          onClick={() => setEntradas(ENTRADAS_VAZIAS)}
        >
          Limpar
        </TButton>
      }
    >
      <CalculoPediatrico entradas={entradas} onChange={setEntradas} />
    </TPage>
  )
}
