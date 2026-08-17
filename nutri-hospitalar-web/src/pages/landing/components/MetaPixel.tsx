import { useEffect } from 'react'

const PIXEL_ID = '1857026921901673'

type FacebookPixel = ((...args: unknown[]) => void) & {
  queue?: unknown[]
  loaded?: boolean
  callMethod?: (...args: unknown[]) => void
}

declare global {
  interface Window {
    fbq?: FacebookPixel
    _fbq?: FacebookPixel
  }
}

/**
 * Carrega o Meta Pixel só enquanto a landing está montada — o ERP autenticado
 * não deve ser rastreado por esse pixel de marketing.
 */
export function MetaPixel() {
  useEffect(() => {
    if (!window.fbq) {
      const fbq: FacebookPixel = function (...args: unknown[]) {
        if (fbq.callMethod) {
          fbq.callMethod(...args)
        } else {
          fbq.queue = fbq.queue ?? []
          fbq.queue.push(args)
        }
      }
      window.fbq = fbq
      window._fbq = fbq
      fbq.loaded = true

      const script = document.createElement('script')
      script.async = true
      script.src = 'https://connect.facebook.net/en_US/fbevents.js'
      document.head.appendChild(script)
    }

    window.fbq?.('init', PIXEL_ID)
    window.fbq?.('track', 'PageView')
  }, [])

  return null
}
