import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAlerta, IconBusca } from '@/assets/icons'
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
} from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { tenantService } from '@/services/tenantService'
import type { Page } from '@/types/comum'
import { DIAS_ALERTA, PERIODO_ACESSO_LABEL, type TenantResponse } from '@/types/tenant'
import { formatarData } from '@/utils/format'

/**
 * "Expirando" não é um valor de `ativo` — é uma janela de vencimento. Vira
 * `ativo=true` mais `expirandoEmDias` na consulta.
 */
type Situacao = '' | 'true' | 'false' | 'expirando'

export function TenantList() {
  const { sessao, switchTenant } = useAuth()
  const navigate = useNavigate()

  const [nome, setNome] = useState('')
  const [situacao, setSituacao] = useState<Situacao>('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<TenantResponse>>()
  const [carregando, setCarregando] = useState(true)

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    tenantService
      .getAll({
        nome: nomeBusca || undefined,
        ativo: situacao === '' ? undefined : situacao !== 'false',
        expirandoEmDias: situacao === 'expirando' ? DIAS_ALERTA : undefined,
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [nomeBusca, situacao, pagina])

  useEffect(carregar, [carregar])

  /**
   * Reativar quem venceu não acontece aqui: o backend devolve 409 porque
   * reativar sem renovar seria desfeito pela rotina na madrugada seguinte. O
   * caminho é o formulário, onde se escolhe o período novo.
   */
  async function alternarAtivo(tenant: TenantResponse) {
    if (!tenant.ativo && tenant.acessoExpirado) {
      navigate(`/tenants/${tenant.id}`)
      return
    }
    try {
      await tenantService.alterarAtivo(tenant.id, !tenant.ativo)
      toast.success(
        tenant.ativo
          ? `"${tenant.nome}" desativado — os usuários dele perdem o acesso`
          : `"${tenant.nome}" reativado`,
      )
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  /** Sem recarregar a página — o reload perderia o token do tenant de destino. */
  async function entrar(tenant: TenantResponse) {
    try {
      await switchTenant(tenant.id)
      toast.success(`Você entrou em "${tenant.nome}"`)
      navigate('/usuarios')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<TenantResponse>[] = [
    { chave: 'nome', cabecalho: 'Nome', render: (t) => t.nome },
    {
      chave: 'criado',
      cabecalho: 'Criado em',
      secundaria: true,
      render: (t) => formatarData(t.createdAt),
    },
    {
      chave: 'acesso',
      cabecalho: 'Acesso até',
      secundaria: true,
      render: (t) =>
        t.acessoExpiraEm ? (
          <span title={PERIODO_ACESSO_LABEL[t.periodoAcesso]}>
            {formatarData(t.acessoExpiraEm)}
          </span>
        ) : (
          'Sem prazo'
        ),
    },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (t) => <SituacaoBadge tenant={t} />,
    },
  ]

  return (
    <TPage
      title="Tenants"
      subtitle="Um cliente nasce ao cadastrar o seu primeiro usuário — não há cadastro de tenant."
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
          <TSelect
            label="Situação"
            vazio="Todas"
            opcoes={[
              { valor: 'true', rotulo: 'Ativo' },
              { valor: 'false', rotulo: 'Inativo' },
              { valor: 'expirando', rotulo: `Expirando (${DIAS_ALERTA} dias)` },
            ]}
            value={situacao}
            onChange={(e) => {
              setSituacao(e.target.value as Situacao)
              setPagina(0)
            }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(t) => t.id}
        carregando={carregando}
        onLinhaClick={(t) => navigate(`/tenants/${t.id}`)}
        tituloCartao={(t) => t.nome}
        vazioTitulo="Nenhum tenant encontrado"
        vazioDescricao="Cadastre um usuário sem indicar tenant para abrir um cliente novo."
        acoes={(t) => (
          <>
            {t.ativo && t.id !== sessao?.tenantId && (
              <TButton variant="secondary" size="sm" onClick={() => void entrar(t)}>
                Entrar
              </TButton>
            )}
            <TButton
              variant="secondary"
              size="sm"
              onClick={() => navigate(`/tenants/${t.id}`)}
              title="Definir ou renovar o período de acesso"
            >
              {t.acessoExpiraEm ? 'Renovar acesso' : 'Definir prazo'}
            </TButton>
            <TButton
              variant={t.ativo ? 'secondary' : 'primary'}
              size="sm"
              onClick={() => void alternarAtivo(t)}
              disabled={t.id === sessao?.tenantId}
              title={
                t.id === sessao?.tenantId
                  ? 'Não é possível desativar o tenant em que você está'
                  : undefined
              }
            >
              {t.ativo ? 'Desativar' : 'Reativar'}
            </TButton>
          </>
        )}
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}

/**
 * "Acesso expirado" e "Inativo" são situações diferentes para quem vende: uma
 * é contrato vencido, a outra é decisão de alguém. A cor nunca vai sozinha —
 * o ícone acompanha.
 */
function SituacaoBadge({ tenant }: { tenant: TenantResponse }) {
  if (tenant.acessoExpirado)
    return (
      <TBadge tom="erro" icone={<IconAlerta className="size-3.5" />}>
        Acesso expirado
      </TBadge>
    )

  if (!tenant.ativo) return <TBadge tom="neutro">Inativo</TBadge>

  // O backend omite campos nulos no JSON — normalizar evita comparar undefined.
  const dias = tenant.diasParaExpirar ?? null
  if (dias !== null && dias <= DIAS_ALERTA)
    return (
      <TBadge tom="alerta" icone={<IconAlerta className="size-3.5" />}>
        {dias === 0 ? 'Vence hoje' : `Expira em ${dias} dia${dias === 1 ? '' : 's'}`}
      </TBadge>
    )

  return <TBadge tom="sucesso">Ativo</TBadge>
}
