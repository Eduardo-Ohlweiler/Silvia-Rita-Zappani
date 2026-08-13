import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { IconTenants } from '@/assets/icons'
import { TButton, TCombo, TEntry, TPage, TPanel, TSelect } from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { tenantService } from '@/services/tenantService'
import { usuarioService } from '@/services/usuarioService'
import { ROLE_LABEL, type Role } from '@/types/auth'
import { OPCOES_PERIODO_ACESSO, type PeriodoAcesso } from '@/types/tenant'

const schema = z.object({
  nome: z.string().min(1, 'Informe o nome').max(255),
  email: z.string().min(1, 'Informe o e-mail').email('E-mail inválido'),
  senha: z.string().optional(),
  telefone: z.string().optional(),
  role: z.string().optional(),
  tenantId: z.string().optional(),
  periodoAcesso: z.string().optional(),
})

type FormData = z.infer<typeof schema>

/** Espelha a decisão do backend em `UsuarioService.ehClienteNovo`. */
type Destino = 'novo-cliente' | 'tenant-existente'

const OPCOES_ROLE = (['ADMIN', 'USER'] as Role[]).map((r) => ({
  valor: r,
  rotulo: ROLE_LABEL[r],
}))

export function UsuarioForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { sessao } = useAuth()
  const editando = !!id

  const [destino, setDestino] = useState<Destino>('novo-cliente')
  const [carregando, setCarregando] = useState(editando)
  const [tenantNome, setTenantNome] = useState<string>()

  /**
   * Dentro de um tenant (troca de tenant), o usuário sempre entra nele — o
   * backend usa o tenant efetivo. Não faz sentido oferecer a escolha.
   */
  const dentroDeTenant = sessao?.impersonating ?? false

  const {
    register,
    handleSubmit,
    reset,
    watch,
    control,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    // Indeterminado por padrão: o default nunca pode ser o que derruba um
    // cliente por esquecimento. Quem quer prazo escolhe.
    defaultValues: { role: 'USER', periodoAcesso: 'INDETERMINADO' },
  })

  useEffect(() => {
    if (!id) return
    usuarioService
      .findById(id)
      .then((u) => {
        reset({
          nome: u.nome,
          email: u.email,
          telefone: u.telefone ?? '',
          role: u.role,
        })
        setTenantNome(u.tenantNome)
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  const clienteNovo = !editando && !dentroDeTenant && destino === 'novo-cliente'

  async function onSubmit(dados: FormData) {
    try {
      if (editando) {
        await usuarioService.update(id!, {
          nome: dados.nome,
          email: dados.email,
          telefone: dados.telefone || undefined,
          role: dados.role as Role,
        })
        toast.success('Usuário salvo')
      } else {
        if (!dados.senha || dados.senha.length < 10) {
          toast.error('A senha deve ter no mínimo 10 caracteres')
          return
        }
        const criado = await usuarioService.create({
          nome: dados.nome,
          email: dados.email,
          senha: dados.senha,
          telefone: dados.telefone || undefined,
          // Cliente novo: sem tenantId, o backend cria o tenant e força ADMIN
          tenantId: clienteNovo ? undefined : dados.tenantId || undefined,
          role: clienteNovo ? undefined : (dados.role as Role),
          // Licença do tenant que está nascendo — ignorada nos demais casos.
          periodoAcesso: clienteNovo ? (dados.periodoAcesso as PeriodoAcesso) : undefined,
        })
        toast.success(
          clienteNovo
            ? `Cliente "${criado.tenantNome}" criado com ${criado.nome} como administrador`
            : 'Usuário criado',
        )
      }
      navigate('/usuarios')
    } catch (erro) {
      handleApiError(erro)
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
      title={editando ? 'Editar usuário' : 'Novo usuário'}
      /* Na edição o cliente aparece como campo próprio no painel, não aqui. */
      subtitle={
        !editando && dentroDeTenant
          ? `Será criado no tenant "${sessao?.tenantNome}"`
          : undefined
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
        {!editando && !dentroDeTenant && (
          <TPanel title="Destino">
            <fieldset className="flex flex-col gap-3">
              <legend className="sr-only">Onde criar o usuário</legend>

              <OpcaoDestino
                selecionado={destino === 'novo-cliente'}
                onSelect={() => setDestino('novo-cliente')}
                titulo="Cliente novo"
                descricao="Cria um tenant com o nome do usuário. Ele nasce administrador — um tenant sem administrador seria um tenant que ninguém consegue gerir."
              />
              {destino === 'novo-cliente' && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <TSelect
                    label="Período de acesso"
                    opcoes={OPCOES_PERIODO_ACESSO}
                    ajuda="Ao fim do prazo o cliente é desativado automaticamente"
                    {...register('periodoAcesso')}
                  />
                </div>
              )}
              <OpcaoDestino
                selecionado={destino === 'tenant-existente'}
                onSelect={() => setDestino('tenant-existente')}
                titulo="Adicionar a um cliente existente"
                descricao="O usuário entra num tenant que já existe, com o nível de acesso que você escolher."
              />
            </fieldset>

            {destino === 'tenant-existente' && (
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <Controller
                  control={control}
                  name="tenantId"
                  render={({ field }) => (
                    <TCombo
                      label="Cliente"
                      placeholder="Busque pelo nome"
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      buscar={tenantService.select}
                      error={errors.tenantId?.message}
                    />
                  )}
                />
                <TSelect label="Nível de acesso" opcoes={OPCOES_ROLE} {...register('role')} />
              </div>
            )}
          </TPanel>
        )}

        <TPanel title="Dados do usuário">
          {editando && (
            <div className="mb-4 flex flex-wrap items-center gap-x-2 gap-y-1 rounded-md bg-surface-alt px-3 py-2.5">
              <IconTenants className="size-4 shrink-0 text-txt-muted" />
              <span className="text-caption text-txt-secondary">Cliente:</span>
              <span className="text-body font-medium text-txt">{tenantNome ?? '—'}</span>
              <span className="text-caption text-txt-muted">
                · não é alterado por aqui
              </span>
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <TEntry
              label="Nome"
              autoFocus
              error={errors.nome?.message}
              ajuda={clienteNovo ? 'Também será o nome do tenant' : undefined}
              {...register('nome')}
            />
            <TEntry
              label="E-mail"
              type="email"
              autoComplete="off"
              ajuda="Único no sistema — é a credencial de login"
              error={errors.email?.message}
              {...register('email')}
            />
            <TEntry label="Telefone" placeholder="(51) 99999-0000" {...register('telefone')} />

            {!editando && (
              <TEntry
                label="Senha"
                type="password"
                autoComplete="new-password"
                ajuda="Mínimo de 10 caracteres"
                error={errors.senha?.message}
                {...register('senha')}
              />
            )}

            {(editando || dentroDeTenant) && (
              <TSelect label="Nível de acesso" opcoes={OPCOES_ROLE} {...register('role')} />
            )}
          </div>

          {clienteNovo && (
            <p className="mt-4 rounded-md bg-info-bg px-3 py-2 text-caption text-info">
              Será criado o tenant <strong>{watch('nome') || '…'}</strong> e este usuário
              como <strong>administrador</strong> dele.
            </p>
          )}
        </TPanel>

        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <TButton
            type="button"
            variant="secondary"
            onClick={() => navigate('/usuarios')}
            className="sm:w-auto"
          >
            Cancelar
          </TButton>
          <TButton type="submit" loading={isSubmitting}>
            Salvar
          </TButton>
        </div>
      </form>
    </TPage>
  )
}

function OpcaoDestino({
  selecionado,
  onSelect,
  titulo,
  descricao,
}: {
  selecionado: boolean
  onSelect: () => void
  titulo: string
  descricao: string
}) {
  return (
    <label
      className={`flex cursor-pointer gap-3 rounded-md border p-3 transition-colors
        ${selecionado ? 'border-primary bg-surface-alt' : 'border-line-strong hover:bg-surface-alt'}`}
    >
      <input
        type="radio"
        name="destino"
        checked={selecionado}
        onChange={onSelect}
        className="mt-1 size-4 shrink-0 accent-[var(--primary)]"
      />
      <span>
        <span className="block text-body font-medium text-txt">{titulo}</span>
        <span className="block text-caption text-txt-secondary">{descricao}</span>
      </span>
    </label>
  )
}
