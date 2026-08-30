import type { Sexo } from '@/types/pessoa'
import type { CalculoPediatricoRequest } from '@/types/pediatria'
import { paraNumero } from '@/utils/format'

/**
 * As entradas do cálculo pediátrico, como a tela as guarda: **texto cru**, do
 * jeito que foi digitado.
 *
 * Guardar como número obrigaria a converter a cada tecla, e "1," viraria 1 no
 * meio da digitação de "1,5". A conversão acontece uma vez, ao montar a
 * requisição.
 *
 * Mora fora do componente porque a calculadora e a avaliação a compartilham —
 * e porque exportar constante junto com componente quebra o fast refresh.
 */
export interface EntradasCalculo {
  sexo: string
  idadeMeses: string
  peso: string
  estatura: string
  formulaLacteaId: string
  volumeMl: string
  frequenciaHoras: string
}

export const ENTRADAS_VAZIAS: EntradasCalculo = {
  sexo: '',
  idadeMeses: '',
  peso: '',
  estatura: '',
  formulaLacteaId: '',
  volumeMl: '',
  frequenciaHoras: '',
}

/**
 * Texto digitado vira o corpo da requisição — **uma vez, num lugar só**.
 *
 * Este mapeamento vivia inline no formulário. A calculadora precisa do mesmo,
 * e duplicá-lo deixaria as duas telas livres para divergir sem ninguém notar:
 * um `paraNumero` a menos num lado e a mesma medida passaria a calcular
 * diferente conforme a porta de entrada. É o gêmeo de `paraRequisicao` da UTI.
 */
export function paraRequisicao(e: EntradasCalculo): CalculoPediatricoRequest {
  return {
    sexo: (e.sexo || null) as Sexo | null,
    idadeMeses: paraNumero(e.idadeMeses) ?? null,
    peso: paraNumero(e.peso) ?? null,
    estatura: paraNumero(e.estatura) ?? null,
    formulaLacteaId: e.formulaLacteaId || null,
    volumeMl: paraNumero(e.volumeMl) ?? null,
    frequenciaHoras: paraNumero(e.frequenciaHoras) ?? null,
  }
}
