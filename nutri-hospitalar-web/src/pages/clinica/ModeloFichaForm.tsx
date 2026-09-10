import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import {
  IconAdicionar,
  IconBloqueado,
  IconClonar,
  IconDescer,
  IconExcluir,
  IconSubir,
} from '@/assets/icons'
import {
  TAviso,
  TButton,
  TCheckBox,
  TEntry,
  TPage,
  TPanel,
  TSelect,
  TTextArea,
} from '@/components/common'
import { handleApiError } from '@/services/api'
import { modeloFichaService } from '@/services/clinicaService'
import {
  GRUPOS_ESCORE,
  TIPOS_CAMPO,
  exigeOpcoes,
  type CampoFicha,
  type EscoreCodigo,
  type ModeloFichaResponse,
  type TipoCampoFicha,
} from '@/types/clinica'
import { formatarNumero, paraNumero } from '@/utils/format'

/** Uma linha do construtor. `opcoesTexto` é o que se digita: uma por linha. */
interface LinhaPergunta {
  id?: string
  secao: string
  rotulo: string
  tipo: TipoCampoFicha
  opcoesTexto: string
  /** Os pontos, um por linha, **na mesma ordem das opções**. Só com escala. */
  pontosTexto: string
  grupoEscore: string
  obrigatorio: boolean
  ativo: boolean
}

const LINHA_NOVA: LinhaPergunta = {
  secao: '',
  rotulo: '',
  tipo: 'TEXTO',
  opcoesTexto: '',
  pontosTexto: '',
  grupoEscore: '',
  obrigatorio: false,
  ativo: true,
}

/**
 * O construtor de perguntas de um modelo de ficha.
 *
 * <p><b>Modelo do sistema abre em somente leitura</b>, com o aviso e o botão
 * Clonar no lugar de Salvar. Não é só conveniência: o servidor responde 404 a
 * qualquer escrita nele, e um formulário que deixa preencher para recusar no
 * fim faz o usuário perder o trabalho antes de descobrir a regra.
 *
 * <p>A ordem das perguntas é a posição na lista, e é o servidor quem a
 * reatribui — aqui só se sobe e desce.
 */
export function ModeloFichaForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [ativo, setAtivo] = useState(true)
  const [perguntas, setPerguntas] = useState<LinhaPergunta[]>([{ ...LINHA_NOVA }])
  const [doSistema, setDoSistema] = useState(false)

  /*
   * A escala do modelo (docs/13). Não é editável em lugar nenhum: ela vem do
   * seed e o clone a herda. Um modelo de três perguntas que se declarasse MNA
   * receberia as faixas da MNA sobre um total de cinco pontos, e a classificação
   * sairia errada com cara de certa — por isso o servidor nem aceita o campo.
   */
  const [escoreCodigo, setEscoreCodigo] = useState<EscoreCodigo | null>(null)
  const [escalaNome, setEscalaNome] = useState<string | null>(null)

  const [carregando, setCarregando] = useState(editando)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string>()

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    modeloFichaService
      .findById(id)
      .then(popular)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id])

  function popular(modelo: ModeloFichaResponse) {
    setNome(modelo.nome)
    setDescricao(modelo.descricao ?? '')
    setAtivo(modelo.ativo)
    setDoSistema(modelo.doSistema)
    setEscoreCodigo(modelo.escoreCodigo)
    setEscalaNome(modelo.escalaNome)
    setPerguntas(
      modelo.campos.map((c) => ({
        id: c.id,
        secao: c.secao ?? '',
        rotulo: c.rotulo,
        tipo: c.tipo,
        opcoesTexto: c.opcoes.join('\n'),
        pontosTexto: c.pontos.map((n) => formatarNumero(n, 1)).join('\n'),
        grupoEscore: c.grupoEscore ?? '',
        obrigatorio: c.obrigatorio,
        ativo: c.ativo,
      })),
    )
  }

  function alterar(indice: number, mudanca: Partial<LinhaPergunta>) {
    setPerguntas((atuais) =>
      atuais.map((linha, i) => (i === indice ? { ...linha, ...mudanca } : linha)),
    )
  }

  function mover(indice: number, direcao: -1 | 1) {
    setPerguntas((atuais) => {
      const destino = indice + direcao
      if (destino < 0 || destino >= atuais.length) return atuais
      const copia = [...atuais]
      const [movida] = copia.splice(indice, 1)
      copia.splice(destino, 0, movida)
      return copia
    })
  }

  async function salvar(e: React.FormEvent) {
    e.preventDefault()
    setErro(undefined)

    const campos: CampoFicha[] = perguntas.map((linha) => ({
      id: linha.id,
      secao: linha.secao.trim() || undefined,
      rotulo: linha.rotulo.trim(),
      tipo: linha.tipo,
      opcoes: exigeOpcoes(linha.tipo) ? linhasDe(linha.opcoesTexto) : undefined,
      /*
       * Os pontos VOLTAM inteiros, e isso não é detalhe: sem enviá-los, salvar
       * um modelo clonado apagaria a pontuação em silêncio, e a validação do
       * servidor recusaria com "somam no máximo 0, e a escala exige 14" — uma
       * frase certa sobre um erro que a própria tela teria acabado de cometer.
       */
      pontos: escoreCodigo ? numerosDe(linha.pontosTexto) : undefined,
      grupoEscore: escoreCodigo ? linha.grupoEscore.trim() || undefined : undefined,
      obrigatorio: linha.obrigatorio,
      ativo: linha.ativo,
    }))

    /* Espelha a Bean Validation do servidor — que valida de novo, sempre. */
    if (!nome.trim()) return setErro('Informe o nome do modelo')
    if (campos.length === 0) return setErro('O modelo precisa de ao menos uma pergunta')

    const semRotulo = campos.findIndex((c) => !c.rotulo)
    if (semRotulo >= 0) return setErro(`A pergunta ${semRotulo + 1} está sem texto`)

    const semOpcoes = campos.find((c) => exigeOpcoes(c.tipo) && (c.opcoes?.length ?? 0) < 2)
    if (semOpcoes) return setErro(`A pergunta "${semOpcoes.rotulo}" precisa de ao menos duas opções`)

    /* Espelha a guarda de cardinalidade do servidor: casar ponto com opção por
       índice só é honesto quando as duas listas têm o mesmo comprimento. */
    const desalinhada = campos.find(
      (c) => (c.pontos?.length ?? 0) > 0 && c.pontos?.length !== (c.opcoes?.length ?? 0),
    )
    if (desalinhada)
      return setErro(
        `A pergunta "${desalinhada.rotulo}" tem ${desalinhada.opcoes?.length ?? 0} opções e ` +
          `${desalinhada.pontos?.length ?? 0} pontos: cada opção precisa do seu`,
      )

    const carga = { nome: nome.trim(), descricao: descricao.trim() || undefined, campos, ativo }

    setSalvando(true)
    try {
      if (id) await modeloFichaService.update(id, carga)
      else await modeloFichaService.create(carga)
      toast.success('Modelo salvo')
      navigate('/app/clinica/modelos-ficha')
    } catch (e2) {
      handleApiError(e2)
    } finally {
      setSalvando(false)
    }
  }

  async function clonar() {
    if (!id) return
    setSalvando(true)
    try {
      const copia = await modeloFichaService.clonar(id)
      toast.success(`Cópia criada: ${copia.nome}`)
      navigate(`/app/clinica/modelos-ficha/${copia.id}`)
    } catch (e) {
      handleApiError(e)
    } finally {
      setSalvando(false)
    }
  }

  async function excluir() {
    if (!id) return
    if (!confirm('Excluir este modelo? As fichas já preenchidas continuam inteiras.')) return
    setSalvando(true)
    try {
      await modeloFichaService.delete(id)
      toast.success('Modelo excluído')
      navigate('/app/clinica/modelos-ficha')
    } catch (e) {
      handleApiError(e)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) {
    return (
      <TPage title="Modelo de ficha">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  return (
    <TPage
      title={editando ? 'Modelo de ficha' : 'Novo modelo de ficha'}
      subtitle={
        doSistema
          ? 'Modelo do sistema — igual para todos os clientes.'
          : 'As perguntas que a ficha vai fazer, na ordem em que se responde.'
      }
      actions={
        doSistema && (
          <TButton onClick={clonar} loading={salvando}>
            <IconClonar className="size-4" />
            Clonar para editar
          </TButton>
        )
      }
    >
      <form onSubmit={salvar} noValidate className="flex flex-col gap-5">
        {doSistema && (
          <TAviso>
            <IconBloqueado className="mr-1 inline size-3.5 align-[-2px]" />
            Este é um modelo do sistema: ele vem pronto, é igual para todos os clientes e não pode
            ser alterado nem excluído. Para adaptá-lo à sua prática, clique em{' '}
            <strong>Clonar para editar</strong> — a cópia nasce sua, com estas mesmas perguntas.
          </TAviso>
        )}

        {/*
          Um modelo com escala não é um questionário: a pontuação é o que a
          classificação lê. O servidor confere, a cada gravação, se cada bloco
          ainda soma o máximo que a publicação declara — sem isso, apagar dez
          perguntas da MNA daria um total máximo de 8 lido pelas faixas de 30, e
          "desnutrido" sairia para quem respondeu tudo.
        */}
        {escoreCodigo && !doSistema && (
          <TAviso>
            Este modelo aplica a escala <strong>{escalaNome ?? escoreCodigo}</strong>. Reescrever o
            texto de uma pergunta ou de uma opção é livre — isso não muda a conta. Já{' '}
            <strong>alterar a pontuação, remover perguntas ou trocar o bloco</strong> faz a
            classificação sair errada, e a gravação será recusada enquanto cada bloco não somar o
            máximo que a publicação define.
          </TAviso>
        )}

        <TPanel title="Identificação">
          <div className="grid gap-4 sm:grid-cols-2">
            <TEntry
              label="Nome do modelo"
              autoFocus={!doSistema}
              disabled={doSistema}
              value={nome}
              onChange={(e) => {
                setNome(e.target.value)
                if (erro) setErro(undefined)
              }}
            />
            <TTextArea
              label="Descrição"
              rows={2}
              disabled={doSistema}
              ajuda="Para que serve, e quando usar."
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
            />
          </div>

          {!doSistema && (
            <div className="mt-4">
              <TCheckBox
                label="Modelo ativo"
                ajuda="Só os ativos aparecem na hora de abrir uma ficha."
                checked={ativo}
                onChange={(e) => setAtivo(e.target.checked)}
              />
            </div>
          )}
        </TPanel>

        <TPanel
          title="Perguntas"
          subtitle="A ordem é a desta lista. Uma pergunta inativa para de ser feita em fichas novas, mas continua nas fichas já preenchidas."
          actions={
            !doSistema && (
              <TButton
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => setPerguntas((p) => [...p, { ...LINHA_NOVA }])}
              >
                <IconAdicionar className="size-4" />
                Adicionar pergunta
              </TButton>
            )
          }
        >
          <div className="flex flex-col gap-4">
            {perguntas.map((linha, i) => (
              <div
                key={linha.id ?? `nova-${i}`}
                className="rounded-md border border-line p-4"
              >
                <div className="mb-3 flex items-center justify-between gap-2">
                  <span className="text-caption font-medium text-txt-secondary">
                    Pergunta {i + 1}
                  </span>

                  {!doSistema && (
                    <div className="flex items-center gap-1">
                      <TButton
                        type="button"
                        variant="ghost"
                        size="sm"
                        title="Subir"
                        disabled={i === 0}
                        onClick={() => mover(i, -1)}
                      >
                        <IconSubir className="size-4" />
                      </TButton>
                      <TButton
                        type="button"
                        variant="ghost"
                        size="sm"
                        title="Descer"
                        disabled={i === perguntas.length - 1}
                        onClick={() => mover(i, 1)}
                      >
                        <IconDescer className="size-4" />
                      </TButton>
                      <TButton
                        type="button"
                        variant="ghost"
                        size="sm"
                        title="Remover"
                        onClick={() => setPerguntas((p) => p.filter((_, j) => j !== i))}
                      >
                        <IconExcluir className="size-4 text-danger" />
                      </TButton>
                    </div>
                  )}
                </div>

                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <TEntry
                    label="Seção"
                    placeholder="Saúde, Hábitos…"
                    disabled={doSistema}
                    ajuda="Agrupa as perguntas na tela e no papel."
                    value={linha.secao}
                    onChange={(e) => alterar(i, { secao: e.target.value })}
                  />
                  <TEntry
                    label="Pergunta"
                    className="lg:col-span-2"
                    disabled={doSistema}
                    value={linha.rotulo}
                    onChange={(e) => alterar(i, { rotulo: e.target.value })}
                  />
                  <TSelect
                    label="Tipo de resposta"
                    opcoes={TIPOS_CAMPO.map((t) => ({ valor: t.valor, rotulo: t.rotulo }))}
                    disabled={doSistema}
                    ajuda={TIPOS_CAMPO.find((t) => t.valor === linha.tipo)?.ajuda}
                    value={linha.tipo}
                    onChange={(e) => alterar(i, { tipo: e.target.value as TipoCampoFicha })}
                  />
                </div>

                {/* Só aparece para os dois tipos que a exigem — e o servidor
                    recusa opção pendurada em campo de texto. */}
                {exigeOpcoes(linha.tipo) && (
                  <div className="mt-4 grid gap-4 sm:grid-cols-2">
                    <TTextArea
                      label="Opções"
                      rows={4}
                      disabled={doSistema}
                      ajuda="Uma por linha. São necessárias ao menos duas."
                      value={linha.opcoesTexto}
                      onChange={(e) => alterar(i, { opcoesTexto: e.target.value })}
                    />

                    {/* Só em modelo com escala — e a pontuação é o que a
                        classificação lê, então mexer nela muda o resultado. */}
                    {escoreCodigo && (
                      <TTextArea
                        label="Pontos"
                        rows={4}
                        disabled={doSistema}
                        ajuda="Um por linha, na mesma ordem das opções."
                        value={linha.pontosTexto}
                        onChange={(e) => alterar(i, { pontosTexto: e.target.value })}
                      />
                    )}
                  </div>
                )}

                {escoreCodigo && (
                  <div className="mt-4 sm:max-w-xs">
                    <TSelect
                      label="Bloco do escore"
                      vazio="Fora do escore"
                      disabled={doSistema}
                      ajuda="Em qual etapa da escala esta pergunta soma."
                      value={linha.grupoEscore}
                      onChange={(e) => alterar(i, { grupoEscore: e.target.value })}
                      opcoes={GRUPOS_ESCORE[escoreCodigo] ?? []}
                    />
                  </div>
                )}

                <div className="mt-3 flex flex-wrap gap-x-6">
                  <TCheckBox
                    label="Resposta obrigatória"
                    disabled={doSistema}
                    checked={linha.obrigatorio}
                    onChange={(e) => alterar(i, { obrigatorio: e.target.checked })}
                  />
                  <TCheckBox
                    label="Pergunta ativa"
                    disabled={doSistema}
                    checked={linha.ativo}
                    onChange={(e) => alterar(i, { ativo: e.target.checked })}
                  />
                </div>
              </div>
            ))}
          </div>
        </TPanel>

        {erro && <TAviso>{erro}</TAviso>}

        {/* Em mobile o botão principal fica embaixo: column-reverse resolve. */}
        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <TButton
            type="button"
            variant="secondary"
            onClick={() => navigate('/app/clinica/modelos-ficha')}
            className="sm:w-auto"
          >
            Voltar
          </TButton>

          {editando && !doSistema && (
            <TButton type="button" variant="danger" onClick={excluir} className="sm:w-auto">
              <IconExcluir className="size-4" />
              Excluir
            </TButton>
          )}

          {!doSistema && (
            <TButton type="submit" loading={salvando}>
              Salvar
            </TButton>
          )}
        </div>
      </form>
    </TPage>
  )
}

/** Uma opção por linha, sem as vazias e sem espaço sobrando. */
function linhasDe(texto: string): string[] {
  return texto
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean)
}

/**
 * Os pontos digitados, uma linha por opção.
 *
 * <p>Passa por `paraNumero`, que é a única porta por onde texto vira número
 * neste front: digitar `0,5` tem de valer meio ponto, e não cinco. Linha que não
 * é número vira `NaN` e é descartada — o desalinhamento resultante é recusado
 * pela guarda de cardinalidade, com frase, em vez de virar uma soma errada.
 */
function numerosDe(texto: string): number[] {
  return linhasDe(texto)
    .map((l) => paraNumero(l))
    .filter((n): n is number => typeof n === 'number' && !Number.isNaN(n))
}
