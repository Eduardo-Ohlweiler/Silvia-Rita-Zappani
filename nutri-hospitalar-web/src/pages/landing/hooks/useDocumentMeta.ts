import { useEffect } from 'react'

interface DocumentMeta {
  titulo: string
  descricao: string
}

function definirMeta(nome: string, conteudo: string) {
  let tag = document.querySelector<HTMLMetaElement>(
    `meta[property="${nome}"], meta[name="${nome}"]`,
  )
  const criada = !tag
  if (!tag) {
    tag = document.createElement('meta')
    tag.setAttribute(nome.startsWith('og:') ? 'property' : 'name', nome)
    document.head.appendChild(tag)
  }
  const anterior = tag.getAttribute('content')
  tag.setAttribute('content', conteudo)
  return { tag, anterior, criada }
}

export function useDocumentMeta({ titulo, descricao }: DocumentMeta) {
  useEffect(() => {
    const tituloAnterior = document.title
    document.title = titulo

    const alvos = [
      definirMeta('description', descricao),
      definirMeta('og:title', titulo),
      definirMeta('og:description', descricao),
    ]

    return () => {
      document.title = tituloAnterior
      for (const { tag, anterior, criada } of alvos) {
        if (criada) {
          tag.remove()
        } else if (anterior !== null) {
          tag.setAttribute('content', anterior)
        }
      }
    }
  }, [titulo, descricao])
}
