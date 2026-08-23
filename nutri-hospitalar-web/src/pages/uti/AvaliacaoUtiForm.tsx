import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar } from '@/assets/icons'
import { TButton, TCombo, TEntry, TPage, TPanel, TTextArea } from '@/components/common'
import { CalculoUti } from '@/components/uti/CalculoUti'
import {
  ENTRADAS_UTI_VAZIAS,
  paraEntradas,
  paraRequisicao,
  type EntradasUti,
} from '@/components/uti/entradas'
import { PessoaRapidaModal } from '@/components/pessoa/PessoaRapidaModal'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { avaliacaoUtiService } from '@/services/utiService'
import type { ResultadoUti } from '@/types/uti'
import { formatarDocumento } from '@/utils/format'
import { idadeEmAnos } from '@/utils/idade'

const HOJE = () => new Date().toISOString().slice(0, 10)

/**
 * Registra uma avaliação de terapia nutricional num paciente.
 *
 * O miolo é o **mesmo** `CalculoUti` da calculadora — a diferença é o que existe
 * em volta: identificação, observações e gravação. Duas cópias divergiriam.
 *
 * **Só Salvar.** No eroERP "Calcular" e "Salvar" são o mesmo submit, e clicar em
 * Calcular grava no banco. Aqui o recálculo é automático na tela, e o Salvar só
 * salva.
 *
 * **Abrir uma avaliação salva não recalcula:** o `resultadoInicial` vem do banco
 * e fica na tela até o primeiro toque num campo. Prontuário não se reescreve
 * sozinho.
 */
export function AvaliacaoUtiForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [pacienteId, setPacienteId] = useState('')
  const [pacienteRotulo, setPacienteRotulo] = useState('')
  const [profissionalId, setProfissionalId] = useState('')
  const [profissionalRotulo, setProfissionalRotulo] = useState('')
  const [dataAvaliacao, setDataAvaliacao] = useState(HOJE)
  const [observacao, setObservacao] = useState('')
  const [entradas, setEntradas] = useState<EntradasUti>(ENTRADAS_UTI_VAZIAS)

  const [nascimento, setNascimento] = useState<string>()
  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [tipoProfissionalId, setTipoProfissionalId] = useState<string>()
  const [modalAberto, setModalAberto] = useState(false)

  const [resultadoSalvo, setResultadoSalvo] = useState<ResultadoUti | null>(null)
  const [formulaRemovida, setFormulaRemovida] = useState(false)
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

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    avaliacaoUtiService
      .findById(id)
      .then((a) => {
        setPacienteId(a.pacienteId)
        setPacienteRotulo(a.pacienteNome)
        setProfissionalId(a.profissionalId ?? '')
        setProfissionalRotulo(a.profissionalNome ?? '')
        setDataAvaliacao(a.dataAvaliacao)
        setObservacao(a.observacao ?? '')
        setEntradas(paraEntradas(a.calculo))
        setFormulaRemovida(a.formulaRemovida)
        // Enquanto nada for tocado, mostra o que foi gravado — sem recalcular.
        setResultadoSalvo(a.resultado)
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id])

  /**
   * Escolher o paciente traz idade e sexo do cadastro — e ambos continuam
   * editáveis: nem todo paciente terá data de nascimento ou sexo gravados, e o
   * cálculo não pode ficar refém disso.
   */
  function aoEscolherPaciente(novoId: string) {
    setPacienteId(novoId)
    if (!novoId) {
      setNascimento(undefined)
      return
    }

    pessoaService
      .select(undefined, tipoPacienteId)
      .then((ps) => {
        const escolhido = ps.find((p) => p.id === novoId)
        if (!escolhido) return

        setNascimento(escolhido.dataNascimento ?? undefined)
        setEntradas((atual) => ({
          ...atual,
          sexo: escolhido.sexo ?? atual.sexo,
          idadeAnos: escolhido.dataNascimento
            ? String(idadeEmAnos(escolhido.dataNascimento, dataAvaliacao) ?? '')
            : atual.idadeAnos,
        }))
      })
      .catch(handleApiError)
  }

  function aoTrocarData(nova: string) {
    setDataAvaliacao(nova)
    if (!nascimento) return
    // A idade é sempre relativa à data da avaliação, não a hoje.
    setEntradas((atual) => ({
      ...atual,
      idadeAnos: String(idadeEmAnos(nascimento, nova) ?? ''),
    }))
  }

  async function salvar() {
    setErro(undefined)

    if (!pacienteId) return setErro('Selecione o paciente')
    if (!dataAvaliacao) return setErro('Informe a data da avaliação')

    const payload = {
      pacienteId,
      profissionalId: profissionalId || null,
      dataAvaliacao,
      calculo: paraRequisicao(entradas),
      observacao: observacao || null,
    }

    setSalvando(true)
    try {
      if (id) {
        const alterada = await avaliacaoUtiService.update(id, payload)
        setResultadoSalvo(alterada.resultado)
        toast.success('Avaliação alterada')
      } else {
        const criada = await avaliacaoUtiService.create(payload)
        toast.success('Avaliação registrada')
        navigate(`/app/uti/avaliacoes/${criada.id}`, { replace: true })
      }
    } catch (e) {
      handleApiError(e)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) {
    return (
      <TPage title="Avaliação de terapia nutricional">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  return (
    <TPage
      title={editando ? 'Editar avaliação' : 'Nova avaliação de terapia nutricional'}
      subtitle="Os resultados são calculados no servidor e gravados junto com as entradas — inclusive a origem de cada valor."
    >
      <div className="flex flex-col gap-5">
        {/* Identificação fica FORA das abas: não é entrada de cálculo. */}
        <TPanel>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div className="flex items-end gap-2">
              <TCombo
                label="Paciente"
                className="flex-1"
                placeholder="Buscar por nome ou documento"
                value={pacienteId}
                rotuloInicial={pacienteRotulo}
                onChange={aoEscolherPaciente}
                buscar={(termo) =>
                  pessoaService.select(termo, tipoPacienteId).then((ps) =>
                    ps.map((p) => ({
                      id: p.id,
                      nome: p.documento
                        ? `${p.nome} (${formatarDocumento(p.documento)})`
                        : p.nome,
                    })),
                  )
                }
              />
              <TButton
                type="button"
                variant="secondary"
                onClick={() => setModalAberto(true)}
                title="Cadastrar um paciente novo sem sair daqui"
                aria-label="Cadastrar paciente novo"
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
              label="Data da avaliação"
              type="date"
              value={dataAvaliacao}
              onChange={(e) => aoTrocarData(e.target.value)}
            />
          </div>
        </TPanel>

        {/*
          A FK saiu, mas o retrato ficou. Vale dizer por escrito: quem reabre
          precisa saber que a composição na tela não existe mais no catálogo.
        */}
        {formulaRemovida && (
          <p className="text-caption rounded-md border border-warning/40 bg-warning-bg px-3 py-2 text-warning">
            A fórmula usada nesta avaliação saiu do catálogo. Os números continuam válidos —
            a composição foi copiada para o registro no dia da avaliação.
          </p>
        )}

        <CalculoUti
          entradas={entradas}
          onChange={setEntradas}
          resultadoInicial={resultadoSalvo}
          abaExtra={{
            id: 'observacoes',
            rotulo: 'Observações',
            conteudo: (
              <TTextArea
                label="Observações"
                value={observacao}
                onChange={(e) => setObservacao(e.target.value)}
                placeholder="Conduta, intercorrências, o que mais precisar ficar registrado."
              />
            ),
          }}
        />

        {erro && (
          <p role="alert" className="text-caption text-danger">
            {erro}
          </p>
        )}

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <TButton
            type="button"
            variant="secondary"
            className="sm:w-auto"
            onClick={() => navigate('/app/uti/avaliacoes')}
          >
            Voltar
          </TButton>
          <TButton type="button" loading={salvando} onClick={() => void salvar()}>
            Salvar
          </TButton>
        </div>
      </div>

      <PessoaRapidaModal
        aberto={modalAberto}
        tipoPadrao="Paciente"
        onFechar={() => setModalAberto(false)}
        onCriada={(pessoa) => {
          setPacienteId(pessoa.id)
          setPacienteRotulo(pessoa.nome)
          setNascimento(pessoa.dataNascimento ?? undefined)
          setEntradas((atual) => ({
            ...atual,
            sexo: pessoa.sexo ?? atual.sexo,
            idadeAnos: pessoa.dataNascimento
              ? String(idadeEmAnos(pessoa.dataNascimento, dataAvaliacao) ?? '')
              : atual.idadeAnos,
          }))
          setModalAberto(false)
        }}
      />
    </TPage>
  )
}
