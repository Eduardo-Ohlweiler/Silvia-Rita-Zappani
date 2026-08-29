/**
 * As faixas de referência do laboratório e da hemodinâmica de UTI.
 *
 * **Nenhuma delas está na planilha.** Todas vêm de literatura, e cada uma
 * carrega a fonte no próprio objeto — é o mesmo rigor de `docs/10`: constante
 * clínica sem procedência não entra no sistema.
 *
 * ## Por que não são todas do mesmo tipo
 *
 * Um componente que só soubesse desenhar banda mentiria em três dos nove:
 *
 * - **banda** — há valor de menos e valor de mais (potássio, sódio, magnésio,
 *   pH, pCO₂, HCO₃);
 * - **limiar superior** — só o excesso importa. Não existe "lactato baixo
 *   demais" nem "PCR baixa demais";
 * - **alvo** — não é faixa de normalidade, é onde se *quer* manter o paciente.
 *
 * ## O caso da glicemia
 *
 * A faixa de adulto saudável é 70–99 mg/dL em jejum, e usá-la num painel de UTI
 * marcaria quase todo paciente crítico como alterado. O alvo em terapia
 * intensiva é **140–180**: o NICE-SUGAR (NEJM 2009) mostrou que controle
 * estrito (< 110) aumenta hipoglicemia grave **sem** ganho de mortalidade. Por
 * isso o rótulo diz "alvo", e não "normal".
 */

export type FormaDaFaixa = 'banda' | 'limiarSuperior' | 'alvo'

export interface Referencia {
  /** Rótulo curto, para o eixo e a legenda. */
  rotulo: string
  unidade: string
  forma: FormaDaFaixa
  /** Presente em `banda` e `alvo`. */
  min?: number
  /** Em `limiarSuperior` é o ponto a partir do qual alerta. */
  max: number
  /** Segundo limiar, quando existe — lactato > 4 é outra conversa. */
  grave?: number
  /** De onde veio o número. Vai para a nota sob o título do gráfico. */
  fonte: string
}

export const REFERENCIAS = {
  k: {
    rotulo: 'Potássio',
    unidade: 'mEq/L',
    forma: 'banda',
    min: 3.5,
    max: 5.0,
    fonte: 'Faixa de referência laboratorial para adultos',
  },
  na: {
    rotulo: 'Sódio',
    unidade: 'mEq/L',
    forma: 'banda',
    min: 135,
    max: 145,
    fonte: 'Faixa de referência laboratorial para adultos',
  },
  mg: {
    rotulo: 'Magnésio',
    unidade: 'mg/dL',
    forma: 'banda',
    min: 1.7,
    max: 2.2,
    fonte: 'Faixa de referência laboratorial para adultos',
  },
  ph: {
    rotulo: 'pH',
    unidade: '',
    forma: 'banda',
    min: 7.35,
    max: 7.45,
    fonte: 'Gasometria arterial — faixa fisiológica',
  },
  pco2: {
    rotulo: 'pCO₂',
    unidade: 'mmHg',
    forma: 'banda',
    min: 35,
    max: 45,
    fonte: 'Gasometria arterial — faixa fisiológica',
  },
  hco3: {
    rotulo: 'HCO₃',
    unidade: 'mEq/L',
    forma: 'banda',
    min: 21,
    max: 28,
    fonte: 'Gasometria arterial — faixa fisiológica',
  },
  lactato: {
    rotulo: 'Lactato',
    unidade: 'mmol/L',
    forma: 'limiarSuperior',
    max: 2,
    grave: 4,
    fonte: 'Surviving Sepsis Campaign — > 2 alerta, > 4 hipoperfusão grave',
  },
  pcr: {
    rotulo: 'PCR',
    unidade: 'mg/dL',
    forma: 'limiarSuperior',
    max: 0.5,
    fonte: 'Proteína C reativa — limite superior de referência',
  },
  hgt: {
    rotulo: 'Glicemia capilar',
    unidade: 'mg/dL',
    forma: 'alvo',
    min: 140,
    max: 180,
    fonte: 'Alvo glicêmico em terapia intensiva (NICE-SUGAR, NEJM 2009) — não é faixa de normalidade',
  },
} as const satisfies Record<string, Referencia>

export type ChaveReferencia = keyof typeof REFERENCIAS

/**
 * Diurese: **0,5 ml/kg/h** é o corte de oligúria do KDIGO (2012), critério de
 * lesão renal aguda por débito urinário. É o que justifica o sistema ter
 * calculado ml/kg/h em vez de deixar o volume bruto.
 */
export const OLIGURIA_ML_KG_H = 0.5

/**
 * PAM alvo de **65 mmHg** na sepse (Surviving Sepsis Campaign). Só é
 * desenhável porque este sistema grava sistólica e diastólica separadas — no
 * eroERP a PA é texto livre `"120/80"` e não plota nada.
 */
export const PAM_ALVO_MMHG = 65

/** PAM = diastólica + (sistólica − diastólica) / 3. */
export function pam(sistolica?: number | null, diastolica?: number | null): number | null {
  if (sistolica == null || diastolica == null) return null
  return diastolica + (sistolica - diastolica) / 3
}
