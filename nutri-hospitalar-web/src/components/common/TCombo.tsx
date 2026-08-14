import {
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent,
} from 'react'
import { IconBusca, IconFechar, IconSeta } from '@/assets/icons'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import type { SelectOption } from '@/types/comum'

interface TComboProps {
  label: string
  /** Id selecionado, ou string vazia. */
  value: string
  onChange: (valor: string) => void
  /**
   * Busca no servidor, com o termo digitado. Recebe `undefined` na carga
   * inicial. Deve apontar para um endpoint `/select`.
   */
  buscar: (termo?: string) => Promise<SelectOption[]>
  /**
   * Nome do que já está selecionado, quando o chamador o conhece — carga de um
   * formulário, sessão em curso.
   *
   * **Passe sempre que tiver.** Sem isto o componente tenta adivinhar o rótulo
   * procurando o id numa busca sem termo, e todo endpoint `/select` é truncado
   * (100 itens): com 5.570 municípios, o id quase nunca está lá e o campo
   * aparece vazio, como se o dado tivesse sumido.
   */
  rotuloInicial?: string
  /** Rótulo da opção que limpa a seleção. Sem ele, a seleção é obrigatória. */
  vazio?: string
  placeholder?: string
  error?: string
  ajuda?: string
  className?: string
  disabled?: boolean
  /** Mantém o rótulo para leitor de tela, sem ocupar espaço. Para cabeçalho. */
  rotuloOculto?: boolean
  /** `sm` cabe em barra de navegação; `md` é o de formulário. */
  tamanho?: 'md' | 'sm'
}

const TAMANHOS = {
  md: 'h-[38px] text-body',
  sm: 'h-9 text-caption',
}

/**
 * Select com busca no servidor.
 *
 * Existe porque um `<select>` nativo não serve quando a lista cresce: com
 * dezenas de clientes, achar um pelo nome exige digitar. A busca é no servidor
 * (`/select?nome=`), com debounce — não carrega tudo para filtrar no navegador.
 *
 * Teclado: ↓/↑ navegam, Enter seleciona, Esc fecha. Clique fora fecha.
 */
export function TCombo({
  label,
  value,
  onChange,
  buscar,
  rotuloInicial,
  vazio,
  placeholder = 'Selecione…',
  error,
  ajuda,
  className = '',
  disabled = false,
  rotuloOculto = false,
  tamanho = 'md',
}: TComboProps) {
  const idBase = useId()
  const idInput = `${idBase}-input`
  const idLista = `${idBase}-lista`
  const idErro = `${idBase}-erro`

  const raiz = useRef<HTMLDivElement>(null)
  const [aberto, setAberto] = useState(false)
  const [termo, setTermo] = useState('')
  const [opcoes, setOpcoes] = useState<SelectOption[]>([])
  const [destacado, setDestacado] = useState(0)
  const [carregando, setCarregando] = useState(false)
  /** Guardado à parte: o rótulo do selecionado precisa sobreviver ao filtro. */
  const [rotuloSelecionado, setRotuloSelecionado] = useState(rotuloInicial ?? '')

  const termoBusca = useDebounce(termo, 300)

  /** Declarado antes dos efeitos que o usam — `const` não sobe por hoisting. */
  const fechar = useCallback(() => {
    setAberto(false)
    setTermo('')
  }, [])

  // Carrega as opções: na abertura e a cada termo novo
  useEffect(() => {
    if (!aberto) return
    setCarregando(true)
    buscar(termoBusca || undefined)
      .then((r) => {
        setOpcoes(r)
        setDestacado(0)
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `buscar` é recriada a cada render nos usos comuns; depender dela causaria
    // laço. O termo e a abertura são o que de fato dispara a consulta.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aberto, termoBusca])

  // Resolve o rótulo do valor que veio de fora (ex.: carga de um formulário)
  useEffect(() => {
    if (!value) {
      setRotuloSelecionado('')
      return
    }

    const naLista = opcoes.find((o) => o.id === value)
    if (naLista) {
      setRotuloSelecionado(naLista.nome)
      return
    }

    // Só quando ainda não se sabe o rótulo: um já conhecido não pode ser
    // sobrescrito, senão digitar uma busca nova trocaria o nome do que está
    // selecionado pelo nome antigo.
    if (rotuloSelecionado) return

    if (rotuloInicial) {
      setRotuloSelecionado(rotuloInicial)
      return
    }

    // Último recurso, e falho de propósito: procurar o id numa busca sem termo
    // só acha o que estiver na primeira página do endpoint. É exatamente para
    // não depender disto que existe `rotuloInicial`.
    buscar()
      .then((r) => {
        const achado = r.find((o) => o.id === value)
        if (achado) setRotuloSelecionado(achado.nome)
      })
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, opcoes, rotuloInicial])

  // Clique fora fecha
  useEffect(() => {
    if (!aberto) return
    const aoClicar = (e: MouseEvent) => {
      if (!raiz.current?.contains(e.target as Node)) fechar()
    }
    document.addEventListener('mousedown', aoClicar)
    return () => document.removeEventListener('mousedown', aoClicar)
  }, [aberto, fechar])

  function selecionar(opcao: SelectOption | null) {
    onChange(opcao?.id ?? '')
    setRotuloSelecionado(opcao?.nome ?? '')
    fechar()
  }

  /** Opção de limpar, quando permitida, ocupa a posição 0 da navegação. */
  const itens: (SelectOption | null)[] = vazio ? [null, ...opcoes] : opcoes

  function aoTeclar(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (!aberto) return setAberto(true)
      setDestacado((i) => Math.min(i + 1, itens.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setDestacado((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter') {
      if (!aberto) return
      e.preventDefault()
      const escolhido = itens[destacado]
      if (escolhido !== undefined) selecionar(escolhido)
    } else if (e.key === 'Escape') {
      fechar()
    }
  }

  return (
    <div className={`flex flex-col gap-1.5 ${className}`} ref={raiz}>
      <label
        htmlFor={idInput}
        className={rotuloOculto ? 'sr-only' : 'text-caption text-txt-secondary'}
      >
        {label}
      </label>

      <div className="relative">
        <IconBusca className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-txt-muted" />

        <input
          id={idInput}
          role="combobox"
          aria-expanded={aberto}
          aria-controls={idLista}
          aria-autocomplete="list"
          aria-invalid={!!error}
          aria-describedby={error ? idErro : undefined}
          autoComplete="off"
          disabled={disabled}
          placeholder={rotuloSelecionado || placeholder}
          value={aberto ? termo : rotuloSelecionado}
          onChange={(e) => {
            setTermo(e.target.value)
            if (!aberto) setAberto(true)
          }}
          onFocus={() => setAberto(true)}
          onKeyDown={aoTeclar}
          className={`w-full truncate rounded-md border bg-surface pl-9 pr-16 text-txt
            placeholder:text-txt-muted transition-shadow duration-150
            focus:outline-none focus:ring-[3px]
            disabled:cursor-not-allowed disabled:opacity-60
            ${TAMANHOS[tamanho]}
            ${
              error
                ? 'border-danger focus:border-danger focus:ring-danger/15'
                : 'border-line-strong focus:border-primary focus:ring-primary/10'
            }`}
        />

        <div className="absolute right-2 top-1/2 flex -translate-y-1/2 items-center gap-0.5">
          {/*
            Limpar só faz sentido onde não escolher é uma opção — que é
            exatamente o que `vazio` declara. Sem ele, o campo é obrigatório e
            oferecer o × contradiz isso; trocar continua sendo escolher outro.
          */}
          {vazio && value && !disabled && (
            <button
              type="button"
              aria-label="Limpar seleção"
              onClick={() => selecionar(null)}
              className="grid size-6 place-items-center rounded text-txt-muted hover:bg-surface-alt hover:text-txt"
            >
              <IconFechar className="size-3.5" />
            </button>
          )}
          <IconSeta
            className={`size-4 text-txt-muted transition-transform ${aberto ? 'rotate-180' : ''}`}
          />
        </div>

        {aberto && (
          <ul
            id={idLista}
            role="listbox"
            className="absolute z-30 mt-1 max-h-64 w-full overflow-y-auto rounded-md
              border border-line-strong bg-surface py-1 shadow-hover"
          >
            {carregando && (
              <li className="px-3 py-2 text-caption text-txt-muted">Buscando…</li>
            )}

            {!carregando &&
              itens.map((opcao, i) => {
                const selecionado = (opcao?.id ?? '') === value
                return (
                  <li
                    key={opcao?.id ?? '__vazio'}
                    role="option"
                    aria-selected={selecionado}
                    onMouseEnter={() => setDestacado(i)}
                    onClick={() => selecionar(opcao)}
                    className={`cursor-pointer px-3 py-2 text-body
                      ${i === destacado ? 'bg-surface-alt' : ''}
                      ${selecionado ? 'font-medium text-primary' : 'text-txt'}
                      ${opcao === null ? 'text-txt-secondary' : ''}`}
                  >
                    {opcao?.nome ?? vazio}
                  </li>
                )
              })}

            {!carregando && opcoes.length === 0 && (
              <li className="px-3 py-2 text-caption text-txt-muted">
                Nenhum resultado para &ldquo;{termo}&rdquo;
              </li>
            )}
          </ul>
        )}
      </div>

      {error ? (
        <span id={idErro} role="alert" className="text-caption text-danger">
          {error}
        </span>
      ) : ajuda ? (
        <span className="text-caption text-txt-muted">{ajuda}</span>
      ) : null}
    </div>
  )
}
