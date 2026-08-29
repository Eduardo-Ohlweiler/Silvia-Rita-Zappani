import { useMemo } from 'react'
import { OLIGURIA_ML_KG_H, PAM_ALVO_MMHG, pam } from '@/components/graficos/referencias'
import type { RegistroDiarioUtiResponse } from '@/types/uti'

/**
 * Um dia do acompanhamento, já com o que só o painel precisa calcular.
 *
 * A **PAM** não é derivado gravado porque não é prescrição — é leitura de tela,
 * e o registro não muda por causa dela. Só é possível calculá-la porque este
 * sistema grava sistólica e diastólica **separadas**; no eroERP a PA é texto
 * livre `"120/80"` e não plota nada.
 */
export interface DiaPlotado extends RegistroDiarioUtiResponse {
  pam: number | null
  /** Repetido em toda linha para virar a reta de corte no gráfico. */
  oliguria: number
  pamAlvo: number
}

/**
 * Mora fora do arquivo de gráficos porque exportar hook junto com componente
 * quebra o fast refresh do Vite — o mesmo motivo de `eixos.ts`.
 */
export function useDiasPlotados(dias: RegistroDiarioUtiResponse[]): DiaPlotado[] {
  return useMemo(
    () =>
      dias.map((d) => ({
        ...d,
        pam: pam(d.paSistolica, d.paDiastolica),
        oliguria: OLIGURIA_ML_KG_H,
        pamAlvo: PAM_ALVO_MMHG,
      })),
    [dias],
  )
}
