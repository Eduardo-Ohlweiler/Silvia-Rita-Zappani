import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconExcluir } from '@/assets/icons'
import {
  TAviso,
  TBotaoImprimir,
  TButton,
  TCheckBox,
  TCombo,
  TEntry,
  TPage,
  TPanel,
  TRadio,
  TSelect,
  TTextArea,
} from '@/components/common'
import { DocumentoFichaAnamnese } from '@/components/clinica/impressao/DocumentoFichaAnamnese'
import { PessoaRapidaModal } from '@/components/pessoa/PessoaRapidaModal'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { fichaAnamneseService, modeloFichaService } from '@/services/clinicaService'
import { pessoaService } from '@/services/pessoaService'
import type {
  FichaAnamneseResponse,
  ModeloFichaResponse,
  RespostaFicha,
  TipoCampoFicha,
} from '@/types/clinica'
import { formatarDocumento, hojeIso } from '@/utils/format'

/**
 * Uma pergunta como a tela a desenha.
 *
 * <p>Vem de duas fontes, e é aí que está o ponto: do **retrato** gravado na
 * ficha (o que foi perguntado naquele dia) ou do **modelo** vivo (o que se
 * pergunta hoje). Nunca das duas ao mesmo tempo.
 */
interface Pergunta {
  /** Nulo quando a pergunta foi apagada do modelo: dá para ler, não para responder. */
  campoId: string | null
  chave: string
  secao: string | null
  rotulo: string
  tipo: TipoCampoFicha
  opcoes: string[]
  obrigatorio: boolean
}

const SIM_NAO = [
  { valor: 'true', rotulo: 'Sim' },
  { valor: 'false', rotulo: 'Não' },
]

/**
 * Preenche uma ficha de anamnese.
 *
 * <p><b>Ficha salva é desenhada pelo retrato</b>, e não pelo modelo de hoje: se
 * uma pergunta foi reescrita depois, o que a tela mostra é o que foi de fato
 * perguntado. O aviso diz que o modelo mudou e oferece trocar para as perguntas
 * atuais — o que só acontece se alguém pedir, e nunca sozinho.
 */
export function FichaAnamneseForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [pacienteId, setPacienteId] = useState('')
  const [pacienteRotulo, setPacienteRotulo] = useState<string>()
  const [profissionalId, setProfissionalId] = useState('')
  const [profissionalRotulo, setProfissionalRotulo] = useState<string>()
  /* `hojeIso()`, nunca `toISOString()`: às 22h aquele manda amanhã, e o
     `@PastOrPresent` do servidor recusa com 400 o plantão noturno inteiro. */
  const [data, setData] = useState(hojeIso())
  const [modeloId, setModeloId] = useState('')
  const [modeloRotulo, setModeloRotulo] = useState<string>()
  const [observacao, setObservacao] = useState('')

  const [perguntas, setPerguntas] = useState<Pergunta[]>([])
  const [valores, setValores] = useState<Record<string, string>>({})

  const [ficha, setFicha] = useState<FichaAnamneseResponse>()
  const [fonte, setFonte] = useState<'retrato' | 'modelo'>('modelo')

  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [tipoProfissionalId, setTipoProfissionalId] = useState<string>()
  const [modalAberto, setModalAberto] = useState(false)

  const [carregando, setCarregando] = useState(editando)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string>()

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => {
        setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id)
        setTipoProfissionalId(tipos.find((t) => t.nome === 'Profissional de saúde')?.id)
      })
      .catch(handleApiError)
  }, [])

  // ── Carga de uma ficha existente: desenha pelo retrato ────────────────────

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    fichaAnamneseService
      .findById(id)
      .then((f) => {
        setFicha(f)
        setPacienteId(f.pacienteId)
        setPacienteRotulo(f.pacienteNome)
        setProfissionalId(f.profissionalId ?? '')
        setProfissionalRotulo(f.profissionalNome ?? undefined)
        setData(f.dataPreenchimento)
        setModeloId(f.modeloId ?? '')
        setModeloRotulo(f.modeloNome)
        setObservacao(f.observacao ?? '')

        setFonte('retrato')
        setPerguntas(
          f.respostas.map((r) => ({
            campoId: r.campoId,
            chave: r.campoId ?? r.id,
            secao: r.secao,
            rotulo: r.rotulo,
            tipo: r.tipo,
            opcoes: r.opcoes,
            obrigatorio: r.obrigatorio,
          })),
        )
        setValores(
          Object.fromEntries(
            f.respostas
              .filter((r) => r.valor != null)
              .map((r) => [r.campoId ?? r.id, r.valor as string]),
          ),
        )
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id])

  // ── Perguntas vindas do modelo vivo ───────────────────────────────────────

  const carregarDoModelo = useCallback(
    (escolhido: string, limparValores: boolean) => {
      modeloFichaService
        .findById(escolhido)
        .then((m: ModeloFichaResponse) => {
          setFonte('modelo')
          setModeloRotulo(m.nome)
          setPerguntas(
            m.campos
              .filter((c) => c.ativo)
              .map((c) => ({
                campoId: c.id,
                chave: c.id,
                secao: c.secao,
                rotulo: c.rotulo,
                tipo: c.tipo,
                opcoes: c.opcoes,
                obrigatorio: c.obrigatorio,
              })),
          )
          if (limparValores) setValores({})
        })
        .catch(handleApiError)
    },
    [],
  )

  /**
   * Trocar o modelo **descarta as respostas**: meio caminho seria uma ficha com
   * metade das perguntas de um modelo e metade de outro. Quem troca é avisado.
   */
  function aoEscolherModelo(novo: string) {
    if (!novo) {
      setModeloId('')
      setPerguntas([])
      setValores({})
      return
    }
    const trocando = !!modeloId && novo !== modeloId
    if (trocando && Object.keys(valores).length > 0) {
      if (!confirm('Trocar o modelo descarta as respostas já preenchidas. Continuar?')) return
    }
    setModeloId(novo)
    carregarDoModelo(novo, true)
  }

  const valor = (chave: string) => valores[chave] ?? ''
  const definir = (chave: string, v: string) =>
    setValores((atuais) => {
      const copia = { ...atuais }
      if (v === '') delete copia[chave]
      else copia[chave] = v
      return copia
    })

  // ── Gravação ──────────────────────────────────────────────────────────────

  async function salvar(e: React.FormEvent) {
    e.preventDefault()
    setErro(undefined)

    if (!pacienteId) return setErro('Informe o paciente')
    if (!modeloId) return setErro('Escolha o modelo da ficha')

    const semResposta = perguntas.find((p) => p.obrigatorio && !valores[p.chave])
    if (semResposta) return setErro(`Responda a pergunta "${semResposta.rotulo}"`)

    /* Só o campoId e o valor: o retrato é do servidor. */
    const respostas: RespostaFicha[] = perguntas
      .filter((p) => p.campoId)
      .map((p) => ({ campoId: p.campoId as string, valor: valores[p.chave] }))

    const carga = {
      pacienteId,
      profissionalId: profissionalId || undefined,
      dataPreenchimento: data,
      modeloId,
      respostas,
      observacao: observacao.trim() || undefined,
    }

    setSalvando(true)
    try {
      if (id) await fichaAnamneseService.update(id, carga)
      else await fichaAnamneseService.create(carga)
      toast.success('Ficha salva')
      navigate('/app/clinica/fichas')
    } catch (e2) {
      handleApiError(e2)
    } finally {
      setSalvando(false)
    }
  }

  async function excluir() {
    if (!id) return
    if (!confirm('Excluir esta ficha de anamnese?')) return
    setSalvando(true)
    try {
      await fichaAnamneseService.delete(id)
      toast.success('Ficha excluída')
      navigate('/app/clinica/fichas')
    } catch (e) {
      handleApiError(e)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) {
    return (
      <TPage title="Ficha de anamnese">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  const secoes = agruparPorSecao(perguntas)

  return (
    <TPage
      title={editando ? 'Ficha de anamnese' : 'Nova ficha de anamnese'}
      subtitle={editando ? `Modelo: ${ficha?.modeloNome ?? modeloRotulo}` : undefined}
      actions={editando && ficha && <TBotaoImprimir />}
      documento={
        editando && ficha ? <DocumentoFichaAnamnese ficha={ficha} /> : undefined
      }
    >
      <form onSubmit={salvar} noValidate className="flex flex-col gap-5">
        {/* Identificação fica FORA das perguntas — não é resposta de anamnese. */}
        <TPanel title="Identificação">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-end gap-2">
              <TCombo
                label="Paciente"
                className="flex-1"
                placeholder="Buscar por nome ou documento"
                value={pacienteId}
                rotuloInicial={pacienteRotulo}
                onChange={setPacienteId}
                buscar={(termo) =>
                  pessoaService.select(termo, tipoPacienteId).then((ps) =>
                    ps.map((p) => ({
                      id: p.id,
                      nome: p.documento ? `${p.nome} (${formatarDocumento(p.documento)})` : p.nome,
                    })),
                  )
                }
              />
              <TButton
                type="button"
                variant="secondary"
                onClick={() => setModalAberto(true)}
                title="Cadastrar um paciente novo sem sair daqui"
              >
                <IconAdicionar className="size-4" />
              </TButton>
            </div>

            <TCombo
              label="Profissional"
              vazio="Não informado"
              placeholder="Buscar por nome"
              value={profissionalId}
              rotuloInicial={profissionalRotulo}
              onChange={setProfissionalId}
              buscar={(termo) =>
                pessoaService
                  .select(termo, tipoProfissionalId)
                  .then((ps) => ps.map((p) => ({ id: p.id, nome: p.nome })))
              }
            />

            <TEntry
              label="Data"
              type="date"
              max={hojeIso()}
              value={data}
              onChange={(e) => setData(e.target.value)}
            />

            <TCombo
              label="Modelo da ficha"
              placeholder="Escolher o modelo"
              value={modeloId}
              rotuloInicial={modeloRotulo}
              onChange={aoEscolherModelo}
              buscar={modeloFichaService.selectParaCombo}
            />
          </div>
        </TPanel>

        {/* A conversa sobre o modelo — duas, e são diferentes. */}
        {ficha?.modeloRemovido && (
          <TAviso>
            O modelo <strong>{ficha.modeloNome}</strong> foi excluído do catálogo. Esta ficha
            continua inteira: as perguntas abaixo são as que foram realmente feitas, guardadas com
            ela. Para gravar alterações, escolha um modelo no campo acima.
          </TAviso>
        )}

        {ficha?.modeloAlterado && fonte === 'retrato' && (
          <TAviso>
            O modelo <strong>{ficha.modeloNome}</strong> mudou desde que esta ficha foi preenchida.
            Abaixo estão as perguntas <strong>como foram feitas naquele dia</strong>.{' '}
            <button
              type="button"
              className="underline underline-offset-2"
              onClick={() => modeloId && carregarDoModelo(modeloId, false)}
            >
              Ver as perguntas de hoje
            </button>{' '}
            — e salvar passa a gravar por elas.
          </TAviso>
        )}

        {ficha && fonte === 'modelo' && ficha.modeloAlterado && (
          <TAviso>
            Estas são as perguntas <strong>do modelo de hoje</strong>. Salvar substitui as que
            estavam gravadas nesta ficha.
          </TAviso>
        )}

        {perguntas.length === 0 ? (
          <TPanel>
            <p className="text-body text-txt-secondary">
              Escolha o modelo da ficha para ver as perguntas.
            </p>
          </TPanel>
        ) : (
          secoes.map((secao) => (
            <TPanel key={secao.titulo ?? '__sem-secao'} title={secao.titulo ?? 'Perguntas'}>
              <div className="grid gap-5 sm:grid-cols-2">
                {secao.perguntas.map((p) => (
                  <CampoDaPergunta
                    key={p.chave}
                    pergunta={p}
                    valor={valor(p.chave)}
                    onChange={(v) => definir(p.chave, v)}
                  />
                ))}
              </div>
            </TPanel>
          ))
        )}

        <TPanel title="Observações">
          <TTextArea
            label="Observações da ficha"
            rows={4}
            ajuda="O que não coube em nenhuma pergunta."
            value={observacao}
            onChange={(e) => setObservacao(e.target.value)}
          />
        </TPanel>

        {erro && <TAviso>{erro}</TAviso>}

        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <TButton
            type="button"
            variant="secondary"
            onClick={() => navigate('/app/clinica/fichas')}
            className="sm:w-auto"
          >
            Voltar
          </TButton>

          {editando && (
            <TButton type="button" variant="danger" onClick={excluir} className="sm:w-auto">
              <IconExcluir className="size-4" />
              Excluir
            </TButton>
          )}

          <TButton type="submit" loading={salvando}>
            Salvar
          </TButton>
        </div>
      </form>

      <PessoaRapidaModal
        aberto={modalAberto}
        onFechar={() => setModalAberto(false)}
        tipoPadrao="Paciente"
        onCriada={(p) => {
          setPacienteId(p.id)
          setPacienteRotulo(p.nome)
          setModalAberto(false)
        }}
      />
    </TPage>
  )
}

// ── O renderizador de um campo ─────────────────────────────────────────────

/**
 * Cada tipo cai num componente que já existe na biblioteca. O único que ganha
 * tratamento próprio é o sim/não: ele precisa de **três estados**, porque uma
 * caixa desmarcada não distingue "o paciente disse que não" de "ninguém
 * perguntou" — e num prontuário essa é a diferença que importa.
 */
function CampoDaPergunta({
  pergunta,
  valor,
  onChange,
}: {
  pergunta: Pergunta
  valor: string
  onChange: (valor: string) => void
}) {
  const rotulo = pergunta.obrigatorio ? `${pergunta.rotulo} *` : pergunta.rotulo

  /* Pergunta apagada do modelo: dá para ler o que foi respondido, não para
     responder de novo — não há a que campo enviar a resposta. */
  const orfa = pergunta.campoId == null

  switch (pergunta.tipo) {
    case 'TEXTO_LONGO':
      return (
        <TTextArea
          label={rotulo}
          className="sm:col-span-2"
          rows={3}
          disabled={orfa}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
        />
      )

    case 'CHECKBOX':
      return (
        <TRadio
          label={rotulo}
          opcoes={SIM_NAO}
          vazio="Não informado"
          disabled={orfa}
          value={valor}
          onChange={onChange}
        />
      )

    case 'DATA':
      return (
        <TEntry
          label={rotulo}
          type="date"
          disabled={orfa}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
        />
      )

    case 'NUMERO':
      return (
        <TEntry
          label={rotulo}
          inputMode="decimal"
          disabled={orfa}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
        />
      )

    case 'OPCOES':
      return (
        <TSelect
          label={rotulo}
          vazio="Não informado"
          opcoes={pergunta.opcoes.map((o) => ({ valor: o, rotulo: o }))}
          disabled={orfa}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
        />
      )

    case 'MULTIPLAS_OPCOES': {
      const escolhidas = listaDe(valor)
      return (
        <fieldset className="flex flex-col gap-1.5 sm:col-span-2">
          <legend className="text-caption text-txt-secondary">{rotulo}</legend>
          <div className="flex flex-wrap gap-x-6">
            {pergunta.opcoes.map((o) => (
              <TCheckBox
                key={o}
                label={o}
                disabled={orfa}
                checked={escolhidas.includes(o)}
                onChange={(e) => {
                  const novas = e.target.checked
                    ? [...escolhidas, o]
                    : escolhidas.filter((x) => x !== o)
                  onChange(novas.length ? JSON.stringify(novas) : '')
                }}
              />
            ))}
          </div>
        </fieldset>
      )
    }

    default:
      return (
        <TEntry
          label={rotulo}
          disabled={orfa}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
        />
      )
  }
}

// ── Auxiliares ─────────────────────────────────────────────────────────────

interface Secao {
  titulo: string | null
  perguntas: Pergunta[]
}

/** Mantém a ordem de aparição: a seção nasce quando a primeira pergunta dela chega. */
function agruparPorSecao(perguntas: Pergunta[]): Secao[] {
  const secoes: Secao[] = []
  for (const p of perguntas) {
    const titulo = p.secao ?? null
    const atual = secoes.find((s) => s.titulo === titulo)
    if (atual) atual.perguntas.push(p)
    else secoes.push({ titulo, perguntas: [p] })
  }
  return secoes
}

/** O valor de opções múltiplas é um JSON array; texto inválido vira lista vazia. */
function listaDe(valor: string): string[] {
  if (!valor) return []
  try {
    const lido: unknown = JSON.parse(valor)
    return Array.isArray(lido) ? (lido as string[]) : []
  } catch {
    return []
  }
}
