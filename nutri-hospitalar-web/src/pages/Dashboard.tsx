import { TPanel } from '@/components/common'
import { useAuth } from '@/hooks/useAuth'

/** Provisório: prova que a sessão chegou. As telas reais vêm nas próximas fatias. */
export function Dashboard() {
  const { sessao } = useAuth()

  return (
    <TPanel title={`Olá, ${sessao?.nome.split(' ')[0]}`}>
      <dl className="grid gap-3 sm:grid-cols-2">
        {[
          ['E-mail', sessao?.email],
          ['Nível de acesso', sessao?.role],
          ['Tenant', sessao?.tenantNome],
          ['Id do tenant', sessao?.tenantId],
        ].map(([rotulo, valor]) => (
          <div key={rotulo}>
            <dt className="text-caption text-txt-secondary">{rotulo}</dt>
            <dd className="text-body text-txt">{valor}</dd>
          </div>
        ))}
      </dl>
    </TPanel>
  )
}
