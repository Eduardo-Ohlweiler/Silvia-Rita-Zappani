import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import LogoFull from '@/assets/brand/logo-full.svg?react'
import { TButton, TEntry, TThemeToggle } from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import axios from 'axios'

const schema = z.object({
  email: z.string().min(1, 'Informe o e-mail').email('E-mail inválido'),
  senha: z.string().min(1, 'Informe a senha'),
})

type FormData = z.infer<typeof schema>

export function Login() {
  const { login, autenticado } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [erroLogin, setErroLogin] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  /** Volta para onde o usuário tentou ir antes de ser mandado ao login. */
  const destino = (location.state as { de?: string } | null)?.de ?? '/app'

  // Com o login fora da raiz, chegar aqui já logado passa a ser comum — pelo
  // link do rodapé da landing ou por favorito antigo.
  if (autenticado) return <Navigate to={destino} replace />

  async function onSubmit(dados: FormData) {
    setErroLogin(null)
    try {
      await login(dados)
      navigate(destino, { replace: true })
    } catch (erro) {
      // 401 e 429 são resposta esperada aqui e aparecem no formulário, não em
      // toast — o usuário está olhando para o campo que errou.
      if (axios.isAxiosError(erro) && (erro.response?.status === 401 || erro.response?.status === 429)) {
        setErroLogin(erro.response.data?.erro ?? 'E-mail ou senha inválidos')
        return
      }
      handleApiError(erro)
    }
  }

  return (
    <main className="relative grid min-h-dvh place-items-center bg-bg p-4 sm:p-6">
      {/* Alternar tema antes de entrar — quem trabalha à noite não deve
          precisar logar no claro para depois trocar. */}
      <div className="absolute right-3 top-3 sm:right-4 sm:top-4">
        <TThemeToggle />
      </div>

      <div className="w-full max-w-100">
        <div className="mb-9 flex justify-center">
          <LogoFull className="h-20 text-txt" />
        </div>

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-6 shadow-card sm:p-7"
        >
          <div className="mb-1">
            <h1 className="text-h1 font-medium text-txt">Entrar</h1>
            <p className="mt-1 text-caption text-txt-secondary">
              Acesse com as suas credenciais.
            </p>
          </div>

          <TEntry
            label="E-mail"
            type="email"
            autoComplete="email"
            autoFocus
            placeholder="voce@clinica.com"
            error={errors.email?.message}
            {...register('email')}
          />

          <TEntry
            label="Senha"
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            error={errors.senha?.message}
            {...register('senha')}
          />

          {erroLogin && (
            <p
              role="alert"
              className="rounded-md bg-danger-bg px-3 py-2 text-caption text-danger"
            >
              {erroLogin}
            </p>
          )}

          <TButton type="submit" loading={isSubmitting} className="mt-2 w-full">
            Entrar
          </TButton>
        </form>

        <p className="mt-6 text-center text-caption text-txt-muted">
          O acesso é liberado pelo administrador do sistema.
        </p>
      </div>
    </main>
  )
}
