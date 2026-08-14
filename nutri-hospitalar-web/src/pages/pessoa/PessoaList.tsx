import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBusca } from '@/assets/icons'
import {
  TBadge,
  TButton,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TPage,
  TPanel,
  TSelect,
  type Coluna,
  type OpcaoSelect,
} from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import type { Page } from '@/types/comum'
import { OPCOES_TIPO_PESSOA, type PessoaResponse, type TipoPessoa } from '@/types/pessoa'
import { formatarDocumento, formatarTelefone } from '@/utils/format'

export function PessoaList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [documento, setDocumento] = useState('')
  const [tipoPessoa, setTipoPessoa] = useState('')
  const [tipoCadastroId, setTipoCadastroId] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<PessoaResponse>>()
  const [tiposCadastro, setTiposCadastro] = useState<OpcaoSelect[]>([])
  const [carregando, setCarregando] = useState(true)

  const nomeBusca = useDebounce(nome)
  const documentoBusca = useDebounce(documento)

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTiposCadastro(tipos.map((t) => ({ valor: t.id, rotulo: t.nome }))))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    setCarregando(true)
    pessoaService
      .getAll({
        nome: nomeBusca || undefined,
        documento: documentoBusca || undefined,
        tipoPessoa: (tipoPessoa as TipoPessoa) || undefined,
        tipoCadastroId: tipoCadastroId || undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com os dados do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, nomeBusca, documentoBusca, tipoPessoa, tipoCadastroId, ativo, pagina])

  useEffect(carregar, [carregar])

  async function alternarAtivo(pessoa: PessoaResponse) {
    try {
      await pessoaService.alterarAtivo(pessoa.id, !pessoa.ativo)
      toast.success(pessoa.ativo ? `${pessoa.nome} foi inativada` : `${pessoa.nome} foi reativada`)
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<PessoaResponse>[] = [
    {
      chave: 'nome',
      cabecalho: 'Nome',
      render: (p) => (
        <span className="flex flex-col">
          <span>{p.nome}</span>
          {p.nomeFantasia && (
            <span className="text-caption text-txt-secondary">{p.nomeFantasia}</span>
          )}
        </span>
      ),
    },
    {
      chave: 'documento',
      cabecalho: 'CPF / CNPJ',
      render: (p) => formatarDocumento(p.cpf ?? p.cnpj),
    },
    {
      chave: 'tipoCadastro',
      cabecalho: 'Tipo de cadastro',
      secundaria: true,
      render: (p) =>
        p.tiposCadastro.length === 0 ? (
          '—'
        ) : (
          <span className="flex flex-wrap gap-1">
            {p.tiposCadastro.map((t) => (
              <TBadge key={t.id} tom="sage">
                {t.nome}
              </TBadge>
            ))}
          </span>
        ),
    },
    {
      chave: 'telefone',
      cabecalho: 'Telefone',
      secundaria: true,
      render: (p) => {
        const principal = p.telefones.find((t) => t.principal) ?? p.telefones[0]
        return principal
          ? formatarTelefone(principal.codigoPais, principal.numero)
          : '—'
      },
    },
    {
      chave: 'cidade',
      cabecalho: 'Cidade',
      secundaria: true,
      render: (p) => {
        const principal = p.enderecos.find((e) => e.principal) ?? p.enderecos[0]
        return principal ? `${principal.cidadeNome}/${principal.estadoSigla}` : '—'
      },
    },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (p) =>
        p.ativo ? <TBadge tom="sucesso">Ativa</TBadge> : <TBadge tom="neutro">Inativa</TBadge>,
    },
  ]

  return (
    <TPage
      title="Pessoas"
      subtitle="Pacientes, responsáveis, profissionais e fornecedores do seu cliente."
      actions={
        <TButton onClick={() => navigate('/pessoas/nova')}>
          <IconAdicionar className="size-4" />
          Nova pessoa
        </TButton>
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <TEntry
            label="Nome"
            placeholder="Buscar por nome"
            value={nome}
            suffix={<IconBusca className="size-4" />}
            onChange={(e) => {
              setNome(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="CPF / CNPJ"
            placeholder="Com ou sem pontuação"
            value={documento}
            onChange={(e) => {
              setDocumento(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Tipo de pessoa"
            vazio="Todos"
            opcoes={OPCOES_TIPO_PESSOA}
            value={tipoPessoa}
            onChange={(e) => {
              setTipoPessoa(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Tipo de cadastro"
            vazio="Todos"
            opcoes={tiposCadastro}
            value={tipoCadastroId}
            onChange={(e) => {
              setTipoCadastroId(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Situação"
            vazio="Todas"
            opcoes={[
              { valor: 'true', rotulo: 'Ativa' },
              { valor: 'false', rotulo: 'Inativa' },
            ]}
            value={ativo}
            onChange={(e) => {
              setAtivo(e.target.value)
              setPagina(0)
            }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(p) => p.id}
        carregando={carregando}
        onLinhaClick={(p) => navigate(`/pessoas/${p.id}`)}
        tituloCartao={(p) => p.nome}
        vazioTitulo="Nenhuma pessoa encontrada"
        vazioDescricao="Ajuste os filtros ou cadastre a primeira pessoa."
        acoes={(p) => (
          <TButton
            variant="secondary"
            size="sm"
            onClick={() => void alternarAtivo(p)}
            title={
              p.ativo
                ? 'Inativar — o cadastro continua no histórico'
                : 'Reativar o cadastro'
            }
          >
            {p.ativo ? 'Inativar' : 'Reativar'}
          </TButton>
        )}
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}
