import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar } from '@/assets/icons'
import { TButton, TCombo, TEntry, TPage, TPanel } from '@/components/common'
import { CalculoPediatrico } from '@/components/pediatria/CalculoPediatrico'
import {
  ENTRADAS_VAZIAS,
  type EntradasCalculo,
} from '@/components/pediatria/entradas'
import { PessoaRapidaModal } from '@/components/pessoa/PessoaRapidaModal'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pediatriaService } from '@/services/pediatriaService'
import { pessoaService } from '@/services/pessoaService'
import type { ResultadoPediatrico } from '@/types/pediatria'
import type { Sexo } from '@/types/pessoa'
import { formatarDocumento, paraNumero } from '@/utils/format'

/** Idade em meses completos entre o nascimento e a data da avaliação. */
function idadeEmMeses(nascimento: string, referencia: string): number | undefined {
  const nasc = new Date(`${nascimento}T00:00:00`)
  const ref = new Date(`${referencia}T00:00:00`)
  if (Number.isNaN(nasc.getTime()) || Number.isNaN(ref.getTime())) return undefined

  let meses =
    (ref.getFullYear() - nasc.getFullYear()) * 12 + (ref.getMonth() - nasc.getMonth())
  if (ref.getDate() < nasc.getDate()) meses -= 1
  return meses < 0 ? undefined : meses
}

const HOJE = () => new Date().toISOString().slice(0, 10)

export function AvaliacaoPediatricaForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [pacienteId, setPacienteId] = useState('')
  const [pacienteRotulo, setPacienteRotulo] = useState('')
  const [profissionalId, setProfissionalId] = useState('')
  const [profissionalRotulo, setProfissionalRotulo] = useState('')
  const [dataAvaliacao, setDataAvaliacao] = useState(HOJE)
  const [observacao, setObservacao] = useState('')
  const [entradas, setEntradas] = useState<EntradasCalculo>(ENTRADAS_VAZIAS)

  const [nascimento, setNascimento] = useState<string>()
  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [tipoProfissionalId, setTipoProfissionalId] = useState<string>()
  const [modalAberto, setModalAberto] = useState(false)

  const [resultadoSalvo, setResultadoSalvo] = useState<ResultadoPediatrico | null>(null)
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
    pediatriaService
      .findById(id)
      .then((a) => {
        setPacienteId(a.pacienteId)
        setPacienteRotulo(a.pacienteNome)
        setProfissionalId(a.profissionalId ?? '')
        setProfissionalRotulo(a.profissionalNome ?? '')
        setDataAvaliacao(a.dataAvaliacao)
        setObservacao(a.observacao ?? '')
        setEntradas({
          sexo: a.sexo,
          idadeMeses: String(a.idadeMeses),
          peso: String(a.peso).replace('.', ','),
          estatura: a.estatura != null ? String(a.estatura).replace('.', ',') : '',
          formulaLacteaId: a.formulaLacteaId ?? '',
          volumeMl: a.volumeMl != null ? String(a.volumeMl).replace('.', ',') : '',
          frequenciaHoras:
            a.frequenciaHoras != null ? String(a.frequenciaHoras).replace('.', ',') : '',
        })
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
          idadeMeses: escolhido.dataNascimento
            ? String(idadeEmMeses(escolhido.dataNascimento, dataAvaliacao) ?? '')
            : atual.idadeMeses,
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
      idadeMeses: String(idadeEmMeses(nascimento, nova) ?? ''),
    }))
  }

  async function salvar() {
    setErro(undefined)

    if (!pacienteId) return setErro('Selecione o paciente')
    if (!dataAvaliacao) return setErro('Informe a data da avaliação')
    if (!entradas.sexo) return setErro('Informe o sexo — a curva da OMS depende dele')

    const idadeMeses = paraNumero(entradas.idadeMeses)
    const peso = paraNumero(entradas.peso)
    if (idadeMeses === undefined) return setErro('Informe a idade em meses')
    if (peso === undefined || peso <= 0) return setErro('Informe o peso')

    const payload = {
      pacienteId,
      profissionalId: profissionalId || null,
      dataAvaliacao,
      sexo: entradas.sexo as Sexo,
      idadeMeses,
      peso,
      estatura: paraNumero(entradas.estatura) ?? null,
      formulaLacteaId: entradas.formulaLacteaId || null,
      volumeMl: paraNumero(entradas.volumeMl) ?? null,
      frequenciaHoras: paraNumero(entradas.frequenciaHoras) ?? null,
      observacao: observacao || null,
    }

    setSalvando(true)
    try {
      if (id) {
        const alterada = await pediatriaService.update(id, payload)
        setResultadoSalvo(alterada.resultado)
        toast.success('Avaliação alterada')
      } else {
        const criada = await pediatriaService.create(payload)
        toast.success('Avaliação registrada')
        navigate(`/app/pediatria/avaliacoes/${criada.id}`, { replace: true })
      }
    } catch (e) {
      handleApiError(e)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) {
    return (
      <TPage title="Avaliação pediátrica">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  return (
    <TPage
      title={editando ? 'Editar avaliação pediátrica' : 'Nova avaliação pediátrica'}
      subtitle="Os resultados são calculados no servidor e gravados junto com as entradas."
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
                pessoaService.select(termo, tipoProfissionalId).then((ps) =>
                  ps.map((p) => ({ id: p.id, nome: p.nome })),
                )
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

        <CalculoPediatrico
          entradas={entradas}
          onChange={setEntradas}
          resultadoInicial={resultadoSalvo}
          abaExtra={{
            id: 'observacoes',
            rotulo: 'Observações',
            conteudo: (
              <label className="flex flex-col gap-1.5">
                <span className="text-caption text-txt-secondary">Observações</span>
                <textarea
                  rows={5}
                  value={observacao}
                  onChange={(e) => setObservacao(e.target.value)}
                  placeholder="Conduta, intercorrências, o que mais precisar ficar registrado."
                  className="w-full rounded-md border border-line-strong bg-surface px-3 py-2
                    text-body text-txt placeholder:text-txt-muted transition-shadow duration-150
                    focus:border-primary focus:outline-none focus:ring-[3px] focus:ring-primary/10"
                />
              </label>
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
            onClick={() => navigate('/app/pediatria/avaliacoes')}
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
            idadeMeses: pessoa.dataNascimento
              ? String(idadeEmMeses(pessoa.dataNascimento, dataAvaliacao) ?? '')
              : atual.idadeMeses,
          }))
          setModalAberto(false)
        }}
      />
    </TPage>
  )
}
