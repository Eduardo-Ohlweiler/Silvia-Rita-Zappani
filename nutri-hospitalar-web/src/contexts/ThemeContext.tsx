import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { configuracaoService } from '@/services/configuracaoService'
import { ehPaletaValida, paletaPorChave } from '@/styles/paletas'
import type { PaletaSistema } from '@/types/configuracao'

export type Tema = 'light' | 'dark'

const CHAVE = 'nutri-tema'
const CHAVE_PALETA = 'nutri-paleta'

/**
 * A paleta é do sistema e muda raramente, mas a aba fica aberta o plantão
 * inteiro — às vezes dias. Ao voltar o foco, consulta de novo, no máximo uma
 * vez a cada cinco minutos: "vale para todos" não pode depender de recarregar.
 */
const INTERVALO_MINIMO_CONSULTA_MS = 5 * 60_000

interface ThemeContextValor {
  /** Claro ou escuro — escolha de cada usuário, salva no navegador. */
  tema: Tema
  alternar: () => void
  definir: (tema: Tema) => void
  /** Paleta de cores — escolha do superadmin, vale para todos. */
  paleta: PaletaSistema
  definirPaleta: (paleta: PaletaSistema) => void
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

/**
 * O cache só serve para a primeira pintura não piscar — quem manda é o
 * servidor, consultado logo em seguida. O `index.html` lê a mesma chave antes
 * do React montar.
 */
function paletaInicial(): PaletaSistema {
  const salva = localStorage.getItem(CHAVE_PALETA)
  return ehPaletaValida(salva) ? salva : 'PADRAO'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(temaInicial)
  const [paleta, setPaleta] = useState<PaletaSistema>(paletaInicial)

  useEffect(() => {
    const raiz = document.documentElement
    raiz.dataset.theme = tema
    localStorage.setItem(CHAVE, tema)

    // A padrão não tem atributo: sem ele valem os blocos originais do theme.css.
    const atributo = paletaPorChave(paleta).atributo
    if (atributo) raiz.dataset.paleta = atributo
    else delete raiz.dataset.paleta
    localStorage.setItem(CHAVE_PALETA, paleta)

    // A barra do navegador no mobile acompanha o canvas — lido do próprio CSS,
    // para não haver um hex aqui que discorde do theme.css.
    const fundo = getComputedStyle(raiz).getPropertyValue('--bg').trim()
    if (fundo) document.querySelector('meta[name="theme-color"]')?.setAttribute('content', fundo)
  }, [tema, paleta])

  useEffect(() => {
    let ultimaConsulta = 0

    const consultar = () => {
      ultimaConsulta = Date.now()
      configuracaoService
        .aparencia()
        .then((a) => {
          if (ehPaletaValida(a.paleta)) setPaleta(a.paleta)
        })
        // Sem aviso: falhar em buscar a cor não é motivo para interromper
        // ninguém. Fica a paleta do cache, e a próxima volta de foco tenta de novo.
        .catch(() => {})
    }

    const aoVoltarFoco = () => {
      if (document.visibilityState === 'visible' && Date.now() - ultimaConsulta > INTERVALO_MINIMO_CONSULTA_MS) {
        consultar()
      }
    }

    consultar()
    document.addEventListener('visibilitychange', aoVoltarFoco)
    return () => document.removeEventListener('visibilitychange', aoVoltarFoco)
  }, [])

  const definir = useCallback((novo: Tema) => setTema(novo), [])
  const alternar = useCallback(() => setTema((t) => (t === 'dark' ? 'light' : 'dark')), [])
  const definirPaleta = useCallback((nova: PaletaSistema) => setPaleta(nova), [])

  const valor = useMemo(
    () => ({ tema, alternar, definir, paleta, definirPaleta }),
    [tema, alternar, definir, paleta, definirPaleta],
  )

  return <ThemeContext.Provider value={valor}>{children}</ThemeContext.Provider>
}
