import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import LogoFull from '@/assets/brand/logo-full.svg?react'

/**
 * 404 público, fora do ERP.
 *
 * Não redireciona para "/": a landing monta o Meta Pixel, e mandar todo erro de
 * URL para lá viraria `PageView` de tráfego pago. Como o SPA responde 200 em
 * qualquer caminho, também marca `noindex` — senão o Google trata cada URL
 * inventada como duplicata da home.
 */
export function NaoEncontradoPublico() {
  useEffect(() => {
    const tag = document.createElement('meta')
    tag.name = 'robots'
    tag.content = 'noindex'
    document.head.appendChild(tag)
    return () => tag.remove()
  }, [])

  return (
    <main className="grid min-h-dvh place-items-center bg-bg p-6">
      <div className="flex max-w-120 flex-col items-center text-center">
        <LogoFull className="h-12 text-txt" />

        <h1 className="mt-8 text-h1 font-medium text-txt">Página não encontrada</h1>
        <p className="mt-2 text-body text-txt-secondary">
          O endereço que você abriu não existe ou foi movido.
        </p>

        <div className="mt-7 flex flex-wrap justify-center gap-x-5 gap-y-2 text-body">
          <Link to="/" className="text-primary underline-offset-4 hover:underline">
            Voltar ao início
          </Link>
          <Link to="/app/login" className="text-primary underline-offset-4 hover:underline">
            Acessar o sistema
          </Link>
        </div>
      </div>
    </main>
  )
}
