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
