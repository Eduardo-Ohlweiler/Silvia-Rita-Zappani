import type { ReactNode } from 'react'

/** Faixa de classificação da OMS — espelha o enum `FaixaOms` do backend. */
export type FaixaOms = 'BAIXA' | 'ADEQUADA' | 'ALTA'

export interface Classificacao {
  faixa: FaixaOms
  rotulo: string
}

interface TResultProps {
  label: string
  /** Já formatado para exibição. Ausente vira "—" com o motivo. */
  valor?: ReactNode
  unidade?: string
  classificacao?: Classificacao | null
  /** De qual fórmula veio o número. "DRIs 2002", "OMS 0–60 meses". */
  referencia?: string
  /** Por que o valor está ausente. Vem do backend. */
  motivoAusencia?: string | null
  /** Enquanto recalcula, o valor anterior fica visível e esmaecido. */
  recalculando?: boolean
  className?: string
}

/**
 * Um resultado de cálculo: valor, unidade, classificação e referência.
 *
 * A referência não é enfeite. Quem prescreve precisa saber de qual fórmula veio
 * o número — um valor solto na tela não é auditável.
 *
 * **Ausência é explicada, nunca muda.** Uma criança de 40 meses não tem VET, e
 * o traço sozinho faz o profissional achar que o sistema falhou. O motivo vem
 * do backend, que é quem conhece as faixas de validade.
 *
 * **Enquanto recalcula, o valor anterior permanece esmaecido** — sumir ou
 * piscar a cada tecla torna a tela impossível de ler.
 */
export function TResult({
  label,
  valor,
  unidade,
  classificacao,
  referencia,
  motivoAusencia,
  recalculando = false,
  className = '',
}: TResultProps) {
  const vazio = valor === undefined || valor === null || valor === ''
  const temClassificacao = !!classificacao

  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <span className="text-caption text-txt-secondary">{label}</span>

      <div
        className={`flex min-h-[38px] items-baseline gap-1.5 transition-opacity duration-150
          ${recalculando ? 'opacity-50' : 'opacity-100'}`}
      >
        {temClassificacao ? (
          <span className={`text-body font-semibold ${COR_FAIXA[classificacao.faixa]}`}>
            {classificacao.rotulo}
          </span>
        ) : vazio ? (
          <span className="text-body text-txt-muted">—</span>
        ) : (
          <>
            <span className="numeric text-h3 font-semibold text-txt">{valor}</span>
            {unidade && <span className="text-caption text-txt-secondary">{unidade}</span>}
          </>
        )}
      </div>

      {referencia && !motivoAusencia && (
        <span className="text-caption text-txt-muted">{referencia}</span>
      )}

      {motivoAusencia && (
        <span className="text-caption text-txt-muted">{motivoAusencia}</span>
      )}
    </div>
  )
}

/**
 * Cor por faixa, não por texto.
 *
 * O eroERP coloria com `texto.includes('adequado')` — quebra em "Adequada", que
 * é justamente o rótulo da estatura. Aqui a faixa vem do backend como enum.
 *
 * A cor nunca é o único portador: o rótulo diz o mesmo por escrito.
 */
const COR_FAIXA: Record<FaixaOms, string> = {
  BAIXA: 'text-info',
  ADEQUADA: 'text-success',
  ALTA: 'text-warning',
}
