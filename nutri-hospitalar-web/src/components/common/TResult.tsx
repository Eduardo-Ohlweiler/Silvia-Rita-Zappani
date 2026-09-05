import { AUSENTE } from '@/utils/format'
import type { ReactNode } from 'react'

/** Faixa de classificação da OMS — espelha o enum `FaixaOms` do backend. */
export type FaixaOms = 'BAIXA' | 'ADEQUADA' | 'ALTA'

export interface Classificacao {
  faixa: FaixaOms
  rotulo: string
}

/**
 * Gravidade de um resultado — espelha o enum `TomResultado` do backend.
 *
 * A pediatria usa `FaixaOms`, que tem três valores ordenados (baixa, adequada,
 * alta) e nasceu das curvas da OMS. A UTI adulto precisa de escalas de tamanhos
 * diferentes — 6 níveis na adequação de CB, 4 no IMC da OMS, 3 na perda de peso,
 * 2 na depleção — e nenhuma delas cabe em "baixa/adequada/alta": não há onde pôr
 * *grave*.
 *
 * Por isso o segundo formato. Os dois convivem: `TResult` aceita qualquer um, e
 * a pediatria continua como está.
 */
export type TomResultado = 'NEUTRO' | 'ADEQUADO' | 'ATENCAO' | 'CRITICO'

export interface ClassificacaoTom {
  tom: TomResultado
  rotulo: string
}

interface TResultProps {
  label: string
  /** Já formatado para exibição. Ausente vira "—" com o motivo. */
  valor?: ReactNode
  unidade?: string
  classificacao?: Classificacao | ClassificacaoTom | null
  /** De qual fórmula veio o número. "DRIs 2002", "OMS 0–60 meses". */
  referencia?: string
  /** Por que o valor está ausente. Vem do backend. */
  motivoAusencia?: string | null
  /** Enquanto recalcula, o valor anterior fica visível e esmaecido. */
  recalculando?: boolean
  /**
   * Versão de uma linha, para a faixa de dependência no topo de uma aba —
   * "peso 68 kg · peso estimado · Rabito 2008". Não é resultado da aba: é o que
   * ela recebeu de outra, e por isso não compete visualmente com os números
   * dali.
   */
  compacto?: boolean
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
  compacto = false,
  className = '',
}: TResultProps) {
  /*
   * O traço TAMBÉM é ausência, e esta linha é o conserto de um defeito que
   * calou 23 motivos.
   *
   * `formatarNumero(null)` devolve "—" — uma string, e portanto um valor
   * "presente" para quem só testasse vazio contra null e "". A calculadora de
   * UTI passava `formatarNumero(...)` direto em 53 lugares e por isso exibia 26
   * traços SEM UMA PALAVRA, enquanto o servidor calculava cada motivo
   * fielmente. A pediatria escapou por acaso: lá as chamadas são guardadas com
   * `x != null ? formatarNumero(x) : undefined`.
   *
   * Reconhecer o traço aqui vale mais do que consertar os 53 pontos: um ponto
   * novo não tem como regredir.
   */
  const vazio = valor === undefined || valor === null || valor === '' || valor === AUSENTE
  const cor = classificacao ? corDe(classificacao) : ''

  if (compacto) {
    return (
      <div
        className={`flex flex-wrap items-baseline gap-x-1.5 transition-opacity duration-150
          ${recalculando ? 'opacity-50' : 'opacity-100'} ${className}`}
      >
        <span className="text-caption text-txt-secondary">{label}</span>
        {vazio ? (
          <span className="text-caption text-txt-muted">{motivoAusencia ?? '—'}</span>
        ) : (
          <>
            <span className="numeric text-body font-semibold text-txt">{valor}</span>
            {unidade && <span className="text-caption text-txt-secondary">{unidade}</span>}
            {referencia && <span className="text-caption text-txt-muted">· {referencia}</span>}
          </>
        )}
      </div>
    )
  }

  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <span className="text-caption text-txt-secondary">{label}</span>

      <div
        className={`flex min-h-[38px] flex-wrap items-baseline gap-x-1.5 transition-opacity duration-150
          ${recalculando ? 'opacity-50' : 'opacity-100'}`}
      >
        {classificacao ? (
          <>
            <span className={`text-body font-semibold ${cor}`}>{classificacao.rotulo}</span>
            {/* Na UTI o número e a classificação convivem: 92,88 % de adequação
                E "Eutrofia". Um sem o outro obriga a consultar a tabela.

                O `valor !== rotulo` é guarda, não otimização: três chamadas do
                acompanhamento pediátrico passavam `valor={d.pesoIdade?.rotulo}`
                JUNTO de `classificacao={d.pesoIdade}`, e a tela imprimia
                "Peso adequado  Peso adequado". Consertar só os call sites deixa
                o quarto livre para nascer — é a mesma lição do traço mudo, em
                que o conserto foi aqui e não nos 53 pontos. */}
            {!vazio && valor !== classificacao.rotulo && (
              <span className="numeric text-caption text-txt-secondary">
                {valor}
                {unidade ? ` ${unidade}` : ''}
              </span>
            )}
          </>
        ) : vazio ? (
          <span className="text-body text-txt-muted">—</span>
        ) : (
          <>
            <span className="numeric text-h3 font-semibold text-txt">{valor}</span>
            {unidade && <span className="text-caption text-txt-secondary">{unidade}</span>}
          </>
        )}
      </div>

      {referencia && !(vazio && motivoAusencia) && (
        <span className="text-caption text-txt-muted">{referencia}</span>
      )}

      {/* O motivo explica uma ausência: ao lado de um número ele mentiria.
          Passou a importar quando a avaliação salva começou a trazer motivo. */}
      {vazio && motivoAusencia && (
        <span className="text-caption text-txt-muted">{motivoAusencia}</span>
      )}
    </div>
  )
}

/** Aceita os dois formatos sem que nenhum deles precise conhecer o outro. */
function corDe(classificacao: Classificacao | ClassificacaoTom): string {
  return 'tom' in classificacao ? COR_TOM[classificacao.tom] : COR_FAIXA[classificacao.faixa]
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

/**
 * Quatro tons, atribuídos pelo backend.
 *
 * `NEUTRO` não é ausência de classificação: é classificação sem juízo clínico,
 * como o preparo escolhido da noradrenalina. Por isso usa a cor do texto comum
 * em vez de cinza de "vazio".
 */
const COR_TOM: Record<TomResultado, string> = {
  NEUTRO: 'text-txt',
  ADEQUADO: 'text-success',
  ATENCAO: 'text-warning',
  CRITICO: 'text-danger',
}
