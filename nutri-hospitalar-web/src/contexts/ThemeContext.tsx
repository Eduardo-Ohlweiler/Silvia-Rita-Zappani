import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'

export type Tema = 'light' | 'dark'

const CHAVE = 'nutri-tema'

interface ThemeContextValor {
  tema: Tema
  alternar: () => void
  definir: (tema: Tema) => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const ThemeContext = createContext<ThemeContextValor | null>(null)

function temaInicial(): Tema {
  const salvo = localStorage.getItem(CHAVE)
  if (salvo === 'light' || salvo === 'dark') return salvo
  // Sem escolha registrada, abre no CLARO: é o tema da marca.
  // Não seguimos `prefers-color-scheme` — quem quiser o escuro alterna, e a
  // escolha fica salva.
  return 'light'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(temaInicial)

  useEffect(() => {
    document.documentElement.dataset.theme = tema
    localStorage.setItem(CHAVE, tema)

    // Mantém a barra do navegador no mobile em sintonia com a tela
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', tema === 'dark' ? '#000F27' : '#FAF6FE')
  }, [tema])

  const definir = useCallback((novo: Tema) => setTema(novo), [])
  const alternar = useCallback(() => setTema((t) => (t === 'dark' ? 'light' : 'dark')), [])

  const valor = useMemo(() => ({ tema, alternar, definir }), [tema, alternar, definir])

  return <ThemeContext.Provider value={valor}>{children}</ThemeContext.Provider>
}
