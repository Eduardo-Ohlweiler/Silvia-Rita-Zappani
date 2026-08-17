import { useEffect, useId } from 'react'

const API_SCRIPT_SRC = 'https://player.pandavideo.com.br/api.v2.js'
const EXTERNAL_SCRIPT_SRC = 'https://player.pandavideo.com.br/player.external.js'

declare global {
  interface Window {
    pandascripttag?: Array<() => void>
    PandaPlayer?: new (
      id: string,
      opcoes: Record<string, unknown>,
    ) => { destroy?: () => void }
  }
}

function carregarScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existente = document.querySelector<HTMLScriptElement>(`script[src="${src}"]`)
    if (existente) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Falha ao carregar ${src}`))
    document.body.appendChild(script)
  })
}

interface PandaVideoPlayerProps {
  videoId: string
}

export function PandaVideoPlayer({ videoId }: PandaVideoPlayerProps) {
  const reactId = useId()
  const containerId = `panda-video-${reactId.replace(/[:]/g, '')}`

  useEffect(() => {
    let player: { destroy?: () => void } | undefined
    let cancelado = false

    Promise.all([carregarScript(API_SCRIPT_SRC), carregarScript(EXTERNAL_SCRIPT_SRC)]).then(() => {
      if (cancelado) return
      window.pandascripttag = window.pandascripttag ?? []
      window.pandascripttag.push(() => {
        if (cancelado || !window.PandaPlayer) return
        player = new window.PandaPlayer(containerId, {
          url: `https://player-vz-0767af7a-2a2.tv.pandavideo.com.br/embed/?v=${videoId}`,
          defaultStyle: true,
          fetchPriority: 'high',
          autoplay: true,
          smartAutoplay: false,
          pandaBranding: false,
          controls: ['play-large'],
          mutedIndicatorIcon: true,
          mutedIndicatorTextTop: 'Clique aqui',
          mutedIndicatorTextBottom: 'para ativar o som',
          bigPlayButtonSize: 150,
        })
      })
    })

    return () => {
      cancelado = true
      player?.destroy?.()
    }
  }, [containerId, videoId])

  return (
    <div className="w-full overflow-hidden rounded-2xl shadow-xl">
      <div style={{ position: 'relative', paddingTop: '56.25%' }}>
        <div id={containerId} style={{ position: 'absolute', inset: 0 }} />
      </div>
    </div>
  )
}
