import { useTheme } from '@/hooks/useTheme'

/** Alterna claro/escuro. A escolha fica salva em `localStorage`. */
export function TThemeToggle() {
  const { tema, alternar } = useTheme()
  const escuro = tema === 'dark'

  return (
    <button
      type="button"
      onClick={alternar}
      title={escuro ? 'Mudar para o tema claro' : 'Mudar para o tema escuro'}
      aria-label={escuro ? 'Mudar para o tema claro' : 'Mudar para o tema escuro'}
      aria-pressed={escuro}
      /* `surface-alt`, não `brand-50`: a escala brand-* é fixa e não vira com
         o tema — no escuro o hover ficaria branco. */
      className="grid size-9 place-items-center rounded-md text-txt-secondary
        transition-colors duration-150 hover:bg-surface-alt hover:text-txt"
    >
      {escuro ? (
        // Sol
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
             strokeLinecap="round" className="size-5" aria-hidden>
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
        </svg>
      ) : (
        // Lua
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
             strokeLinecap="round" strokeLinejoin="round" className="size-5" aria-hidden>
          <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
        </svg>
      )}
    </button>
  )
}
