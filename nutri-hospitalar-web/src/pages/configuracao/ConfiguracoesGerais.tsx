import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { IconCheck } from '@/assets/icons'
import { TBadge, TButton, TPage, TPanel } from '@/components/common'
import { useTheme } from '@/hooks/useTheme'
import { handleApiError } from '@/services/api'
import { configuracaoService } from '@/services/configuracaoService'
import { PALETAS, paletaPorChave, type Paleta } from '@/styles/paletas'
import type { PaletaSistema } from '@/types/configuracao'

/** Os dois grupos de cartões: as mesmas dez paletas, com a moldura clara ou escura. */
const GRUPOS = [
  {
    titulo: 'Paletas',
    nota: 'Menu e cabeçalho claros, como o conteúdo.',
    menuEscuro: false,
  },
  {
    titulo: 'Com menu e cabeçalho escuros',
    nota: 'As mesmas cores, com o menu lateral e o cabeçalho nos tons escuros da paleta. O conteúdo continua claro.',
    menuEscuro: true,
  },
]

/**
 * Configurações do sistema — exclusiva do superadmin. Por ora uma opção só: a
 * paleta de cores, que vale para todos os usuários de todos os clientes.
 *
 * Escolher um cartão não aplica nada: só o botão aplica. Trocar a cor de todo
 * mundo é ação com efeito imediato, e não pode acontecer por um clique de
 * quem só estava olhando as opções.
 */
export function ConfiguracoesGerais() {
  const { paleta: atual, definirPaleta } = useTheme()
  const [escolhida, setEscolhida] = useState<PaletaSistema>(atual)
  const [salvando, setSalvando] = useState(false)

  // A paleta do contexto pode ser a do cache: a tela confere com o servidor,
  // para não oferecer como "em uso" uma escolha que outro superadmin já trocou.
  useEffect(() => {
    configuracaoService
      .aparencia()
      .then((a) => {
        definirPaleta(a.paleta)
        setEscolhida(a.paleta)
      })
      .catch(handleApiError)
  }, [definirPaleta])

  async function aplicar() {
    setSalvando(true)
    try {
      const { paleta } = await configuracaoService.atualizarAparencia(escolhida)
      definirPaleta(paleta)
      toast.success(`Paleta ${paletaPorChave(paleta).rotulo} aplicada para todos os usuários`)
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setSalvando(false)
    }
  }

  return (
    <TPage
      title="Configurações gerais"
      subtitle="Valem para o sistema inteiro: todos os usuários, de todos os clientes."
    >
      <TPanel
        title="Tema do sistema"
        subtitle="A paleta de cores da interface e dos documentos impressos. Cada paleta tem a versão clara e a escura."
      >
        {GRUPOS.map((grupo) => (
          <fieldset key={grupo.titulo} className="mt-6 first:mt-0">
            <legend className="mb-1 text-h3 font-medium text-txt">{grupo.titulo}</legend>
            <p className="mb-3 text-caption text-txt-secondary">{grupo.nota}</p>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {PALETAS.filter((p) => p.menuEscuro === grupo.menuEscuro).map((p) => (
                <CartaoPaleta
                  key={p.chave}
                  paleta={p}
                  emUso={p.chave === atual}
                  selecionada={p.chave === escolhida}
                  onEscolher={() => setEscolhida(p.chave)}
                />
              ))}
            </div>
          </fieldset>
        ))}

        <p className="mt-5 rounded-md bg-info-bg px-3 py-2 text-caption text-info">
          A troca chega a quem já está com o sistema aberto quando a pessoa volta à
          aba, ou na próxima vez em que entrar. O claro ou escuro continua sendo
          escolha de cada usuário, no botão do topo da tela.
        </p>

        <div className="mt-5 flex flex-wrap items-center gap-3">
          <TButton
            type="button"
            loading={salvando}
            disabled={escolhida === atual}
            onClick={() => void aplicar()}
            className="w-full sm:w-auto"
          >
            Aplicar para todos
          </TButton>
          {escolhida !== atual && (
            <span className="text-caption text-txt-secondary">
              Em uso: {paletaPorChave(atual).rotulo} · escolhida: {paletaPorChave(escolhida).rotulo}
            </span>
          )}
        </div>
      </TPanel>
    </TPage>
  )
}

interface CartaoPaletaProps {
  paleta: Paleta
  emUso: boolean
  selecionada: boolean
  onEscolher: () => void
}

/**
 * Rádio nativo escondido dentro de um rótulo: o teclado (setas, Tab, espaço)
 * vem do navegador, e o cartão inteiro é a área de clique.
 */
function CartaoPaleta({ paleta, emUso, selecionada, onEscolher }: CartaoPaletaProps) {
  return (
    <label
      className={`flex cursor-pointer flex-col gap-3 rounded-lg border bg-surface p-3 transition-colors
        has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary
        ${selecionada ? 'border-primary ring-1 ring-primary' : 'border-line hover:border-line-strong'}`}
    >
      <input
        type="radio"
        name="paleta"
        value={paleta.chave}
        checked={selecionada}
        onChange={onEscolher}
        className="sr-only"
      />

      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <span className="block text-h3 font-medium text-txt">{paleta.rotulo}</span>
          <span className="block text-caption text-txt-secondary">{paleta.descricao}</span>
        </div>
        {emUso && (
          <TBadge tom="sucesso" icone={<IconCheck className="size-3.5" />}>
            Em uso
          </TBadge>
        )}
      </div>

      <div className="grid grid-cols-2 gap-2">
        <Miniatura atributo={paleta.atributo} tema="light" menuEscuro={paleta.menuEscuro} />
        <Miniatura atributo={paleta.atributo} tema="dark" menuEscuro={paleta.menuEscuro} />
      </div>
    </label>
  )
}

/**
 * A tela em miniatura, pintada pelo **CSS de verdade**: `data-theme` e
 * `data-paleta` no próprio elemento, e as variáveis do `theme.css` valem na
 * subárvore. Nenhuma cor é repetida aqui.
 *
 * Todo elemento com tema leva também o seu `data-paleta` — a Padrão leva
 * "padrao", que não casa com bloco nenhum. Sem isso a moldura escura de uma
 * miniatura herdaria a paleta do `<html>` que a contém: o seletor da moldura
 * (`[data-paleta] [data-theme=dark]`) casa com qualquer ancestral, e é o
 * `:not([data-paleta])` dele que deixa a miniatura de fora.
 */
function Miniatura({
  atributo,
  tema,
  menuEscuro,
}: {
  atributo: string | null
  tema: 'light' | 'dark'
  menuEscuro: boolean
}) {
  const paleta = atributo ?? 'padrao'
  const moldura = menuEscuro ? { 'data-theme': 'dark', 'data-paleta': paleta } : {}

  return (
    <div className="flex flex-col gap-1">
      <div
        data-theme={tema}
        data-paleta={paleta}
        aria-hidden
        className="flex h-24 overflow-hidden rounded-md border border-line bg-bg text-txt"
      >
        <div {...moldura} className="flex w-[38%] flex-col gap-1 border-r border-line bg-sidebar p-1.5">
          <span className="h-1.5 w-3/4 rounded-full bg-txt" />
          <span className="mt-1 truncate rounded bg-sidebar-active-bg px-1 text-[10px] leading-4 text-sidebar-active-text">
            Início
          </span>
          <span className="truncate px-1 text-[10px] leading-4 text-sidebar-text">UTI</span>
        </div>
        <div className="flex min-w-0 flex-1 flex-col">
          <div
            {...moldura}
            className={`flex h-3.5 shrink-0 items-center justify-end gap-1 border-b border-line px-1.5 ${menuEscuro ? 'bg-sidebar' : 'bg-surface'}`}
          >
            <span className="h-1 w-5 rounded-full bg-txt-secondary" />
            <span className="size-1.5 rounded-full bg-txt-secondary" />
          </div>
          <div className="p-1.5">
            <div className="rounded border border-line bg-surface p-1.5">
              <span className="block truncate text-[11px] font-medium leading-4 text-txt">Paciente</span>
              <span className="block truncate text-[10px] leading-4 text-txt-secondary">1.850 kcal</span>
              <span className="mt-1 inline-block rounded bg-primary px-1.5 text-[10px] leading-4 text-txt-inverse">
                Salvar
              </span>
            </div>
          </div>
        </div>
      </div>
      <span className="text-center text-caption text-txt-secondary">
        {tema === 'light' ? 'Claro' : 'Escuro'}
      </span>
    </div>
  )
}
