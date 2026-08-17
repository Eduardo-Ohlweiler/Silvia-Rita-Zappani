const NUMERO_WHATSAPP = '555198662425'
const MENSAGEM_PADRAO = 'Olá, venho da bio e quero saber mais sobre a mentoria!'

interface WhatsAppButtonProps {
  rotulo: string
  className?: string
}

export function WhatsAppButton({ rotulo, className = '' }: WhatsAppButtonProps) {
  const href = `https://wa.me/${NUMERO_WHATSAPP}?text=${encodeURIComponent(MENSAGEM_PADRAO)}`

  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className={`inline-flex items-center justify-center rounded-full bg-[var(--landing-primary)] px-8 py-4 text-center text-base font-semibold text-white shadow-lg transition hover:bg-[var(--landing-primary-dark)] ${className}`}
    >
      {rotulo}
    </a>
  )
}
