import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { TButton, TEntry, TModal, TSelect, type OpcaoSelect } from '@/components/common'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import {
  OPCOES_SEXO,
  OPCOES_TIPO_PESSOA,
  type PessoaResponse,
  type Sexo,
  type TipoPessoa,
} from '@/types/pessoa'
import { mascararCnpj, mascararCpf, mascararTelefone, somenteDigitos } from '@/utils/format'

interface Props {
  aberto: boolean
  onFechar: () => void
  onCriada: (pessoa: PessoaResponse) => void
  /**
   * Tipo de cadastro já marcado ao abrir. Quem chama sabe o que está
   * cadastrando: o vínculo abre um "Responsável", a avaliação pediátrica abre
   * um "Paciente".
   */
  tipoPadrao?: string
}

/**
 * Cadastro rápido, para criar a pessoa do vínculo sem sair do formulário.
 *
 * <p>Usa o mesmo `POST /pessoas` do cadastro completo — herda as validações de
 * documento e de tipo, e não abre um caminho paralelo de escrita. O que ficou
 * de fora (endereço, redes sociais, mais de um contato) entra depois, abrindo
 * o cadastro dela.
 */
export function PessoaRapidaModal({
  aberto,
  onFechar,
  onCriada,
  tipoPadrao = 'Responsável',
}: Props) {
  const [nome, setNome] = useState('')
  const [tipoPessoa, setTipoPessoa] = useState<TipoPessoa>('PESSOA_FISICA')
  const [documento, setDocumento] = useState('')
  const [telefone, setTelefone] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [sexo, setSexo] = useState('')
  const [tipoCadastroId, setTipoCadastroId] = useState('')
  const [tiposCadastro, setTiposCadastro] = useState<OpcaoSelect[]>([])
  const [tipoTelefoneId, setTipoTelefoneId] = useState('')
  const [salvando, setSalvando] = useState(false)

  const ehFisica = tipoPessoa === 'PESSOA_FISICA'

  useEffect(() => {
    if (!aberto) return

    Promise.all([catalogoService.tiposCadastro(), catalogoService.tiposTelefone()])
      .then(([cadastro, telefones]) => {
        setTiposCadastro(cadastro.map((t) => ({ valor: t.id, rotulo: t.nome })))
        const padrao = cadastro.find((t) => t.nome === tipoPadrao) ?? cadastro[0]
        setTipoCadastroId((atual) => atual || padrao?.id || '')
        setTipoTelefoneId((atual) => atual || telefones[0]?.id || '')
      })
      .catch(handleApiError)
  }, [aberto, tipoPadrao])

  function limpar() {
    setNome('')
    setTipoPessoa('PESSOA_FISICA')
    setDocumento('')
    setTelefone('')
    setDataNascimento('')
    setSexo('')
  }

  async function salvar() {
    if (!nome.trim()) {
      toast.error('Informe o nome')
      return
    }
    if (!tipoCadastroId) {
      toast.error('Selecione o tipo de cadastro')
      return
    }

    setSalvando(true)
    try {
      const criada = await pessoaService.create({
        nome: nome.trim(),
        tipoPessoa,
        cpf: ehFisica ? somenteDigitos(documento) : undefined,
        cnpj: ehFisica ? undefined : somenteDigitos(documento),
        // Data de nascimento e sexo poupam digitação na tela de cálculo: é de
        // onde saem a idade em meses e a curva da OMS.
        dataNascimento: ehFisica ? dataNascimento || undefined : undefined,
        sexo: ehFisica ? (sexo as Sexo) || undefined : undefined,
        tiposCadastroIds: [tipoCadastroId],
        telefones: somenteDigitos(telefone)
          ? [{ tipoTelefoneId, numero: somenteDigitos(telefone)!, principal: true }]
          : [],
      })

      toast.success(`${criada.nome} cadastrada`)
      limpar()
      onCriada(criada)
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setSalvando(false)
    }
  }

  return (
    <TModal
      aberto={aberto}
      titulo="Cadastro rápido"
      largura="sm"
      onFechar={onFechar}
      acoes={
        <>
          <TButton type="button" variant="secondary" onClick={onFechar} className="sm:w-auto">
            Cancelar
          </TButton>
          <TButton
            type="button"
            loading={salvando}
            onClick={() => void salvar()}
            className="sm:w-auto"
          >
            Cadastrar
          </TButton>
        </>
      }
    >
      <div className="grid gap-4">
        <TEntry
          label="Nome"
          autoFocus
          value={nome}
          onChange={(e) => setNome(e.target.value)}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <TSelect
            label="Tipo de pessoa"
            opcoes={OPCOES_TIPO_PESSOA}
            value={tipoPessoa}
            onChange={(e) => {
              setTipoPessoa(e.target.value as TipoPessoa)
              setDocumento('')
            }}
          />
          <TEntry
            label={ehFisica ? 'CPF' : 'CNPJ'}
            inputMode="numeric"
            placeholder={ehFisica ? '000.000.000-00' : '00.000.000/0000-00'}
            value={documento}
            onChange={(e) =>
              setDocumento(ehFisica ? mascararCpf(e.target.value) : mascararCnpj(e.target.value))
            }
          />
        </div>

        {ehFisica && (
          <div className="grid gap-4 sm:grid-cols-2">
            <TEntry
              label="Data de nascimento"
              type="date"
              ajuda="Opcional — dá a idade no cálculo"
              value={dataNascimento}
              onChange={(e) => setDataNascimento(e.target.value)}
            />
            <TSelect
              label="Sexo"
              vazio="Não informado"
              opcoes={OPCOES_SEXO}
              value={sexo}
              onChange={(e) => setSexo(e.target.value)}
            />
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <TSelect
            label="Tipo de cadastro"
            opcoes={tiposCadastro}
            value={tipoCadastroId}
            onChange={(e) => setTipoCadastroId(e.target.value)}
          />
          <TEntry
            label="Telefone"
            inputMode="numeric"
            placeholder="(51) 99999-0000"
            ajuda="Opcional"
            value={telefone}
            onChange={(e) => setTelefone(mascararTelefone(e.target.value))}
          />
        </div>

        <p className="rounded-md bg-info-bg px-3 py-2 text-caption text-info">
          Endereço, redes sociais e mais contatos entram depois, abrindo o
          cadastro completo dela.
        </p>
      </div>
    </TModal>
  )
}
