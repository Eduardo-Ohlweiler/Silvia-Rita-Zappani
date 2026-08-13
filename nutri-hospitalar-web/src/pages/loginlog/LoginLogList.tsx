import { useCallback, useEffect, useState } from 'react'
import {
  TBadge,
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
import { loginLogService } from '@/services/loginLogService'
import type { Page } from '@/types/comum'
import { MOTIVO_FALHA_LABEL, TIPO_LOGOUT_LABEL, type LoginLogResponse } from '@/types/loginlog'
import { formatarDataHora, paraIso } from '@/utils/format'

type Escopo = 'tenant' | 'global'

export function LoginLogList() {
  const { sessao } = useAuth()
  const [escopo, setEscopo] = useState<Escopo>('tenant')
  const [sucesso, setSucesso] = useState('')
  const [ip, setIp] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<LoginLogResponse>>()
  const [carregando, setCarregando] = useState(true)

  const ipBusca = useDebounce(ip)

  const carregar = useCallback(() => {
    setCarregando(true)
    const filtros = {
      sucesso: sucesso === '' ? undefined : sucesso === 'true',
      ip: ipBusca || undefined,
      de: paraIso(de),
      ate: paraIso(ate),
      page: pagina,
      size: 20,
    }
    const chamada =
      escopo === 'global'
        ? loginLogService.getAllGlobal(filtros)
        : loginLogService.getAll(filtros)

    chamada.then(setDados).catch(handleApiError).finally(() => setCarregando(false))
    // `sessao.tenantId` não é lido aqui — o backend o deriva do token. Está nas
    // dependências como SINAL DE INVALIDAÇÃO: trocar de tenant precisa refazer
    // a consulta, senão a tela fica com os dados do tenant anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [escopo, sessao?.tenantId, sucesso, ipBusca, de, ate, pagina])

  useEffect(carregar, [carregar])

  const colunas: Coluna<LoginLogResponse>[] = [
    {
      chave: 'quando',
      cabecalho: 'Data',
      render: (l) => formatarDataHora(l.dataLogin),
    },
    {
      chave: 'quem',
      cabecalho: 'Usuário',
      render: (l) => (
        <span className="flex flex-col">
          <span>{l.usuarioNome ?? '—'}</span>
          <span className="text-caption text-txt-secondary">{l.emailTentativa}</span>
        </span>
      ),
    },
    ...(escopo === 'global'
      ? [
          {
            chave: 'tenant',
            cabecalho: 'Tenant',
            secundaria: true,
            render: (l: LoginLogResponse) => l.tenantNome ?? '—',
          },
        ]
      : []),
    {
      chave: 'resultado',
      cabecalho: 'Resultado',
      render: (l) =>
        l.sucesso ? (
          <TBadge tom="sucesso">Entrou</TBadge>
        ) : (
          <TBadge tom="erro">
            {l.motivoFalha ? MOTIVO_FALHA_LABEL[l.motivoFalha] : 'Falhou'}
          </TBadge>
        ),
    },
    {
      chave: 'saida',
      cabecalho: 'Saída',
      secundaria: true,
      render: (l) =>
        l.dataLogout ? (
          <span className="flex flex-col">
            <span>{formatarDataHora(l.dataLogout)}</span>
            <span className="text-caption text-txt-secondary">
              {l.tipoLogout ? TIPO_LOGOUT_LABEL[l.tipoLogout] : ''}
            </span>
          </span>
        ) : l.sucesso ? (
          <span className="text-txt-muted">Em aberto</span>
        ) : (
          '—'
        ),
    },
    { chave: 'ip', cabecalho: 'IP', secundaria: true, render: (l) => l.enderecoIp ?? '—' },
    {
      chave: 'impersonado',
      cabecalho: 'Acesso de suporte',
      secundaria: true,
      render: (l) =>
        l.impersonadoPorNome ? (
          <TBadge tom="alerta">{l.impersonadoPorNome}</TBadge>
        ) : (
          <span className="text-txt-muted">—</span>
        ),
    },
  ]

  return (
    <TPage
      title="Log de acesso"
      subtitle="Registra entrada, falha com motivo, saída e acesso de suporte. Retenção de 12 meses."
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <TSelect
            label="Escopo"
            opcoes={[
              { valor: 'tenant', rotulo: 'Tenant atual' },
              { valor: 'global', rotulo: 'Todos os tenants' },
            ]}
            value={escopo}
            onChange={(e) => {
              setEscopo(e.target.value as Escopo)
              setPagina(0)
            }}
          />
          <TSelect
            label="Resultado"
            vazio="Todos"
            opcoes={[
              { valor: 'true', rotulo: 'Entrou' },
              { valor: 'false', rotulo: 'Falhou' },
            ]}
            value={sucesso}
            onChange={(e) => {
              setSucesso(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="De"
            type="datetime-local"
            value={de}
            onChange={(e) => {
              setDe(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="Até"
            type="datetime-local"
            value={ate}
            onChange={(e) => {
              setAte(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="IP"
            placeholder="192.168."
            value={ip}
            onChange={(e) => {
              setIp(e.target.value)
              setPagina(0)
            }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(l) => l.id}
        carregando={carregando}
        tituloCartao={(l) => formatarDataHora(l.dataLogin)}
        vazioTitulo="Nenhum acesso registrado"
        vazioDescricao="Ajuste o período ou o escopo."
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}
