import { useCallback, useEffect, useState } from 'react'
import {
  TAcoesDeExportacao,
  TBadge,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TPage,
  TPanel,
  TSelect,
  type Coluna,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { loginLogService } from '@/services/loginLogService'
import type { Page } from '@/types/comum'
import { MOTIVO_FALHA_LABEL, TIPO_LOGOUT_LABEL, type LoginLogResponse } from '@/types/loginlog'
import { formatarData, formatarDataHora, paraIso } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

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
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<LoginLogResponse>>()

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

  /**
   * Cobre o filtro inteiro, não a página aberta — e **repete o escopo da
   * tela**.
   *
   * <p>Como na lista de usuários, este `if` é o que impede o relatório de sair
   * parecendo completo: exportando do escopo global sem ele, viriam só os
   * acessos do tenant atual, com contagem plausível e sem aviso. Ver
   * `TAcoesDeExportacao`.
   */
  const carregarTudo = useCallback(
    async (limite: number): Promise<ResultadoDaCarga<LoginLogResponse>> => {
      const filtros = {
        sucesso: sucesso === '' ? undefined : sucesso === 'true',
        ip: ipBusca || undefined,
        de: paraIso(de),
        ate: paraIso(ate),
        page: 0,
        size: limite,
      }
      const pagina =
        escopo === 'global'
          ? await loginLogService.getAllGlobal(filtros)
          : await loginLogService.getAll(filtros)

      return {
        linhas: pagina.content,
        restantes: Math.max(0, pagina.totalElements - pagina.content.length),
      }
    },
    [escopo, sucesso, ipBusca, de, ate],
  )

  /**
   * O que uma auditoria de acesso precisa responder: quem, quando, de onde, se
   * entrou, por que não entrou, e se alguém entrou por ele.
   *
   * <p><b>O e-mail tentado sai mesmo sem usuário</b> — numa tentativa contra
   * e-mail inexistente ele é o único vestígio, e é justamente a linha que
   * importa quando se investiga varredura de credenciais.
   *
   * <p>O <b>user agent</b> entra só aqui, não na tela: é texto longo que
   * arruinaria a grade e é exatamente o que se cruza depois, numa planilha,
   * para separar acesso humano de robô.
   */
  const colunasExportadas: ColunaExportavel<LoginLogResponse>[] = [
    { titulo: 'Data e hora', valor: (l) => formatarDataHora(l.dataLogin) },
    { titulo: 'Usuário', valor: (l) => l.usuarioNome ?? '' },
    { titulo: 'E-mail tentado', valor: (l) => l.emailTentativa ?? '' },
    ...(escopo === 'global'
      ? [{ titulo: 'Tenant', valor: (l: LoginLogResponse) => l.tenantNome ?? '' }]
      : []),
    { titulo: 'Resultado', valor: (l) => (l.sucesso ? 'Entrou' : 'Falhou') },
    {
      titulo: 'Motivo da falha',
      valor: (l) => (l.motivoFalha ? MOTIVO_FALHA_LABEL[l.motivoFalha] : ''),
    },
    { titulo: 'Saída', valor: (l) => (l.dataLogout ? formatarDataHora(l.dataLogout) : '') },
    {
      titulo: 'Tipo de saída',
      valor: (l) => (l.tipoLogout ? TIPO_LOGOUT_LABEL[l.tipoLogout] : ''),
    },
    { titulo: 'IP', valor: (l) => l.enderecoIp ?? '' },
    { titulo: 'Acesso de suporte por', valor: (l) => l.impersonadoPorNome ?? '' },
    { titulo: 'Navegador', valor: (l) => l.userAgent ?? '' },
  ]

  const filtrosAplicados = [
    { rotulo: 'Escopo', valor: escopo === 'global' ? 'Todos os tenants' : 'Tenant atual' },
    { rotulo: 'Resultado', valor: sucesso === 'true' ? 'Entrou' : sucesso === 'false' ? 'Falhou' : '' },
    { rotulo: 'IP', valor: ipBusca },
    { rotulo: 'De', valor: de ? formatarData(de) : '' },
    { rotulo: 'Até', valor: ate ? formatarData(ate) : '' },
  ]

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
      actions={
        <TAcoesDeExportacao
          nome="Log de acesso"
          colunas={colunasExportadas}
          carregar={carregarTudo}
          aoCarregarParaImprimir={setParaImprimir}
        />
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Log de acesso"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(l) => l.id}
            resumo={[
              { rotulo: 'Registros', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Entradas',
                valor: String(paraImprimir.linhas.filter((l) => l.sucesso).length),
              },
              {
                rotulo: 'Falhas',
                valor: String(paraImprimir.linhas.filter((l) => !l.sucesso).length),
              },
              {
                rotulo: 'Acesso de suporte',
                valor: String(paraImprimir.linhas.filter((l) => l.impersonadoPorNome).length),
              },
            ]}
            nota="A falha guarda o e-mail tentado mesmo quando não existe usuário — é o único vestígio de uma varredura de credenciais. O log é retido por 12 meses e expurgado por rotina mensal."
          />
        )
      }
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
