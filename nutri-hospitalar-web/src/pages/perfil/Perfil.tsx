import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { TBadge, TButton, TEntry, TPage, TPanel } from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { usuarioService } from '@/services/usuarioService'
import { ROLE_LABEL } from '@/types/auth'

const perfilSchema = z.object({
  nome: z.string().min(1, 'Informe o nome').max(255),
  telefone: z.string().optional(),
})

const senhaSchema = z
  .object({
    senhaAtual: z.string().min(1, 'Informe a senha atual'),
    senhaNova: z.string().min(10, 'A nova senha deve ter no mínimo 10 caracteres'),
    confirmacao: z.string().min(1, 'Repita a nova senha'),
  })
  .refine((d) => d.senhaNova === d.confirmacao, {
    message: 'As senhas não conferem',
    path: ['confirmacao'],
  })

type PerfilData = z.infer<typeof perfilSchema>
type SenhaData = z.infer<typeof senhaSchema>

export function Perfil() {
  const { sessao } = useAuth()
  const [carregando, setCarregando] = useState(true)

  const perfilForm = useForm<PerfilData>({ resolver: zodResolver(perfilSchema) })
  const senhaForm = useForm<SenhaData>({ resolver: zodResolver(senhaSchema) })

  useEffect(() => {
    usuarioService
      .getPerfil()
      .then((u) => perfilForm.reset({ nome: u.nome, telefone: u.telefone ?? '' }))
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function salvarPerfil(dados: PerfilData) {
    try {
      await usuarioService.updatePerfil({
        nome: dados.nome,
        telefone: dados.telefone || undefined,
      })
      toast.success('Perfil salvo. O nome no topo atualiza no próximo acesso.')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  async function trocarSenha(dados: SenhaData) {
    try {
      await usuarioService.alterarSenha({
        senhaAtual: dados.senhaAtual,
        senhaNova: dados.senhaNova,
      })
      toast.success('Senha alterada')
      senhaForm.reset()
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
    <TPage title="Meu perfil">
      <div className="grid gap-5 lg:grid-cols-2">
        <TPanel title="Dados">
          <form
            onSubmit={perfilForm.handleSubmit(salvarPerfil)}
            noValidate
            className="flex flex-col gap-4"
          >
            <TEntry
              label="Nome"
              error={perfilForm.formState.errors.nome?.message}
              {...perfilForm.register('nome')}
            />
            <TEntry
              label="Telefone"
              placeholder="(51) 99999-0000"
              {...perfilForm.register('telefone')}
            />

            <div className="grid gap-3 rounded-md bg-surface-alt p-3 sm:grid-cols-2">
              <Campo rotulo="E-mail" valor={sessao?.email} />
              <Campo rotulo="Tenant" valor={sessao?.tenantNome} />
              <div>
                <p className="text-caption text-txt-secondary">Nível de acesso</p>
                <div className="mt-0.5">
                  <TBadge tom="info">{sessao && ROLE_LABEL[sessao.role]}</TBadge>
                </div>
              </div>
            </div>

            <p className="text-caption text-txt-muted">
              E-mail, nível de acesso e tenant são alterados pelo administrador do sistema.
            </p>

            <TButton type="submit" loading={perfilForm.formState.isSubmitting} className="self-start">
              Salvar
            </TButton>
          </form>
        </TPanel>

        <TPanel title="Trocar senha">
          <form
            onSubmit={senhaForm.handleSubmit(trocarSenha)}
            noValidate
            className="flex flex-col gap-4"
          >
            <TEntry
              label="Senha atual"
              type="password"
              autoComplete="current-password"
              error={senhaForm.formState.errors.senhaAtual?.message}
              {...senhaForm.register('senhaAtual')}
            />
            <TEntry
              label="Nova senha"
              type="password"
              autoComplete="new-password"
              ajuda="Mínimo de 10 caracteres"
              error={senhaForm.formState.errors.senhaNova?.message}
              {...senhaForm.register('senhaNova')}
            />
            <TEntry
              label="Repita a nova senha"
              type="password"
              autoComplete="new-password"
              error={senhaForm.formState.errors.confirmacao?.message}
              {...senhaForm.register('confirmacao')}
            />

            <TButton type="submit" loading={senhaForm.formState.isSubmitting} className="self-start">
              Trocar senha
            </TButton>
          </form>
        </TPanel>
      </div>
    </TPage>
  )
}

function Campo({ rotulo, valor }: { rotulo: string; valor?: string }) {
  return (
    <div>
      <p className="text-caption text-txt-secondary">{rotulo}</p>
      <p className="text-body text-txt">{valor ?? '—'}</p>
    </div>
  )
}
