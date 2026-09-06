import type { ReactNode } from 'react'

/**
 * A faixa que avisa sem impedir.
 *
 * <p>Usada quando o número na tela está certo, mas depende de algo que quem lê
 * não veria sozinho: a composição da fórmula mudou no catálogo depois da
 * avaliação, ou a adesão foi medida contra o prescrito da avaliação e não
 * contra o que se acabou de digitar. Não é erro de validação — é a frase que
 * evita a conclusão errada.
 *
 * <p>Nasceu de cinco cópias do mesmo literal de classes espalhadas pelas telas.
 * Sem variante de tom: a segunda entra quando houver a segunda, não antes.
 */
export function TAviso({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <p
      className={`text-caption rounded-md border border-warning/40 bg-warning-bg
        px-3 py-2 text-warning ${className}`}
    >
      {children}
    </p>
  )
}
