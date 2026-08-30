import { useState } from 'react'
import { TBotaoImprimir, TButton, TPage } from '@/components/common'
import { CalculoPediatrico } from '@/components/pediatria/CalculoPediatrico'
import {
  ENTRADAS_VAZIAS,
  paraRequisicao,
  type EntradasCalculo,
} from '@/components/pediatria/entradas'
import { DocumentoAvaliacaoPediatrica } from '@/components/pediatria/impressao/DocumentoAvaliacaoPediatrica'
import type { FormulaLacteaSelect, ResultadoPediatrico } from '@/types/pediatria'

/**
 * Cálculo rápido: sem paciente e sem gravar nada.
 *
 * É a mesma tela da avaliação sem o que a avaliação tem em volta — o miolo é o
 * mesmo componente, então as duas nunca divergem. Imprime pelo mesmo documento
 * da avaliação: `paciente`, `data` e `observacao` são opcionais lá, e a folha
 * sai sem eles.
 */
export function PediatriaCalculadora() {
  const [entradas, setEntradas] = useState<EntradasCalculo>(ENTRADAS_VAZIAS)
  const [resultado, setResultado] = useState<ResultadoPediatrico | null>(null)
  const [formula, setFormula] = useState<FormulaLacteaSelect>()

  const preenchido = Object.values(entradas).some((v) => v !== '')

  return (
    <TPage
      title="Calculadora pediátrica"
      subtitle="Cálculo rápido, sem paciente. Nada aqui é gravado."
      actions={
        <>
          {resultado && <TBotaoImprimir />}
          <TButton
            variant="secondary"
            disabled={!preenchido}
            onClick={() => setEntradas(ENTRADAS_VAZIAS)}
          >
            Limpar
          </TButton>
        </>
      }
      documento={
        resultado && (
          <DocumentoAvaliacaoPediatrica
            entradas={paraRequisicao(entradas)}
            resultado={resultado}
            formula={formula}
          />
        )
      }
    >
      <CalculoPediatrico
        entradas={entradas}
        onChange={setEntradas}
        onResultado={(r, f) => {
          setResultado(r)
          setFormula(f)
        }}
      />
    </TPage>
  )
}
