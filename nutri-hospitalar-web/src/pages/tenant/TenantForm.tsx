import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { IconAlerta } from '@/assets/icons'
import { TBadge, TButton, TEntry, TPage, TPanel, TSelect } from '@/components/common'
import { handleApiError } from '@/services/api'
import { tenantService } from '@/services/tenantService'
import {
  DIAS_ALERTA,
  OPCOES_PERIODO_ACESSO,
  type PeriodoAcesso,
  type TenantResponse,
} from '@/types/tenant'
import { formatarData } from '@/utils/format'

const schema = z.object({
  nome: z.string().min(1, 'Informe o nome').max(255),
})

type FormData = z.infer<typeof schema>

/**
 * Cliente não se cadastra por aqui — ele nasce junto do primeiro usuário. Esta
 * tela edita o que já existe: o nome e, principalmente, a licença.
 *
 * Nome e período são salvos separados de propósito. Prazo é ação com efeito
 * imediato — recontar a data e, se for o caso, religar o acesso — e não pode
 * acontecer de carona num "salvar cadastro".
 */
export function TenantForm() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [tenant, setTenant] = useState<TenantResponse>()
  const [periodo, setPeriodo] = useState<PeriodoAcesso>('INDETERMINADO')
  const [salvandoAcesso, setSalvandoAcesso] = useState(false)
  const [carregando, setCarregando] = useState(true)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (!id) return
    tenantService
      .findById(id)
      .then((t) => {
        setTenant(t)
        setPeriodo(t.periodoAcesso)
        reset({ nome: t.nome })
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  const vencido = tenant?.acessoExpirado ?? false
  const dias = tenant?.diasParaExpirar ?? null
  const alertando = !vencido && dias !== null && dias <= DIAS_ALERTA

  async function salvarNome(dados: FormData) {
    try {
      setTenant(await tenantService.update(id!, { nome: dados.nome }))
      toast.success('Cliente salvo')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  async function salvarAcesso() {
    setSalvandoAcesso(true)
    try {
      const atualizado = await tenantService.definirAcesso(id!, periodo)
      setTenant(atualizado)

      const prazo = atualizado.acessoExpiraEm
        ? `acesso até ${formatarData(atualizado.acessoExpiraEm)}`
        : 'acesso sem prazo'

      toast.success(
        vencido && atualizado.ativo
          ? `"${atualizado.nome}" reativado — ${prazo}`
          : `Período salvo — ${prazo}`,
      )
    } catch (erro) {
      handleApiError(erro)
    } finally {
      setSalvandoAcesso(false)
    }
  }

  if (carregando) {
    return (
      <div className="grid place-items-center py-20">
        <span
          role="status"
          aria-label="Carregando"
          className="size-8 animate-spin rounded-full border-2 border-primary border-t-transparent"
        />
      </div>
    )
  }

  return (
    <TPage
      title={tenant?.nome ?? 'Cliente'}
      subtitle="Nome e licença do cliente. O cadastro nasce com o primeiro usuário."
    >
      <TPanel title="Dados do cliente">
        <form
          onSubmit={handleSubmit(salvarNome)}
          noValidate
          className="grid gap-4 sm:grid-cols-2"
        >
          <TEntry label="Nome" autoFocus error={errors.nome?.message} {...register('nome')} />
          <div className="flex items-end">
            <TButton type="submit" loading={isSubmitting} className="sm:w-auto">
              Salvar nome
            </TButton>
          </div>
        </form>
      </TPanel>

      <TPanel title="Acesso">
        <div className="mb-4 flex flex-wrap items-center gap-x-2 gap-y-1 rounded-md bg-surface-alt px-3 py-2.5">
          <span className="text-caption text-txt-secondary">Situação:</span>
          {vencido ? (
            <TBadge tom="erro" icone={<IconAlerta className="size-3.5" />}>
              Acesso expirado
            </TBadge>
          ) : tenant?.ativo ? (
            <TBadge tom="sucesso">Ativo</TBadge>
          ) : (
            <TBadge tom="neutro">Inativo</TBadge>
          )}
          <span className="text-caption text-txt-secondary">
            {tenant?.acessoExpiraEm
              ? `Acesso até ${formatarData(tenant.acessoExpiraEm)}`
              : 'Sem prazo'}
          </span>
          {alertando && (
            <TBadge tom="alerta" icone={<IconAlerta className="size-3.5" />}>
              {dias === 0 ? 'Vence hoje' : `Expira em ${dias} dia${dias === 1 ? '' : 's'}`}
            </TBadge>
          )}
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <TSelect
            label="Período de acesso"
            opcoes={OPCOES_PERIODO_ACESSO}
            value={periodo}
            onChange={(e) => setPeriodo(e.target.value as PeriodoAcesso)}
          />
          <div className="flex items-end">
            <TButton
              type="button"
              loading={salvandoAcesso}
              onClick={() => void salvarAcesso()}
              className="sm:w-auto"
            >
              {vencido ? 'Renovar e reativar' : 'Salvar período'}
            </TButton>
          </div>
        </div>

        <p className="mt-4 rounded-md bg-info-bg px-3 py-2 text-caption text-info">
          A data é sempre recontada a partir de hoje. Ao fim do prazo o cliente é
          desativado automaticamente e ninguém da equipe dele consegue entrar.
        </p>
      </TPanel>

      <div>
        <TButton
          type="button"
          variant="secondary"
          onClick={() => navigate('/app/tenants')}
          className="sm:w-auto"
        >
          Voltar
        </TButton>
      </div>
    </TPage>
  )
}
