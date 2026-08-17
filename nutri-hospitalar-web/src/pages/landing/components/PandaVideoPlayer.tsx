interface PandaVideoPlayerProps {
  videoId: string
}

/**
 * Mesma URL/parâmetros do embed usado no site atual da Silvia — um iframe
 * direto é mais confiável que o SDK JS do Panda, que nem sempre repassa as
 * opções de controles para o player interno (a barra de progresso/volume
 * completa aparecia em vez de só o botão de play grande).
 */
export function PandaVideoPlayer({ videoId }: PandaVideoPlayerProps) {
  const params = new URLSearchParams({
    v: videoId,
    controls: 'play-large',
    autoplay: 'true',
    smartAutoplay: 'false',
    pandaBranding: 'false',
    saveProgress: 'false',
    mutedIndicatorIcon: 'true',
    mutedIndicatorTextTop: 'Clique aqui',
    mutedIndicatorTextBottom: 'para ativar o som',
    disableForward: 'false',
    bigPlayButtonSize: '150',
  })

  return (
    <div className="w-full overflow-hidden rounded-2xl shadow-xl">
      <div style={{ position: 'relative', paddingTop: '56.25%' }}>
        <iframe
          src={`https://player-vz-0767af7a-2a2.tv.pandavideo.com.br/embed/?${params.toString()}`}
          title="Vídeo de apresentação"
          allow="autoplay; fullscreen; encrypted-media"
          allowFullScreen
          style={{ position: 'absolute', inset: 0, border: 'none', width: '100%', height: '100%' }}
        />
      </div>
    </div>
  )
}
