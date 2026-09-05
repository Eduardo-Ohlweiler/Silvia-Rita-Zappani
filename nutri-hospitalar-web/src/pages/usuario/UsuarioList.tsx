import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBusca, IconDesbloquear } from '@/assets/icons'
import {
  TAcoesDeExportacao,
  TBadge,
  TButton,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TPage,
  TPanel,
  TCombo,
  TSelect,
  type Coluna,
  type PaginaDaCarga,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { tenantService } from '@/services/tenantService'
import { usuarioService } from '@/services/usuarioService'
import { ROLE_LABEL, type Role } from '@/types/auth'
import type { Page } from '@/types/comum'
import type { UsuarioResponse } from '@/types/usuario'
import { formatarData, formatarTelefone, rotuloDe } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

const OPCOES_ROLE = (Object.keys(ROLE_LABEL) as Role[]).map((r) => ({
  valor: r,
  rotulo: ROLE_LABEL[r],
}))

export function UsuarioList() {
  const navigate = useNavigate()
  const { sessao, switchTenant } = useAuth()

  /**
   * Estando dentro de um tenant (via troca de tenant), a lista se limita a ele
   * — foi uma escolha deliberada de quem entrou ali. No tenant raiz, mostra
   * **todos**: senão o usuário de um cliente recém-criado sumiria da tela.
   */
  const dentroDeTenant = sessao?.impersonating ?? false

  const [nome, setNome] = useState('')
  const [role, setRole] = useState('')
  const [ativo, setAtivo] = useState('')
  const [tenantId, setTenantId] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<UsuarioResponse>>()
  const [carregando, setCarregando] = useState(true)
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<UsuarioResponse>>()

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    const filtros = {
      nome: nomeBusca || undefined,
      role: (role as Role) || undefined,
      ativo: ativo === '' ? undefined : ativo === 'true',
      page: pagina,
      size: 20,
    }

    const chamada = dentroDeTenant
      ? usuarioService.getAll(filtros)
      : usuarioService.getAllGlobal({ ...filtros, tenantId: tenantId || undefined })

    chamada.then(setDados).catch(handleApiError).finally(() => setCarregando(false))
    // `sessao.tenantId` não é lido aqui — o backend o deriva do token. Está nas
    // dependências como SINAL DE INVALIDAÇÃO: trocar de tenant precisa refazer
    // a consulta, senão a tela fica com os dados do tenant anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dentroDeTenant, sessao?.tenantId, nomeBusca, role, ativo, tenantId, pagina])

  useEffect(carregar, [carregar])

  /**
   * Cobre o filtro inteiro, não a página aberta — e **repete o escopo da
   * tela**.
   *
   * <p>Este `if` é a razão de a exportação daqui não ser cópia das outras. Sem
   * ele o superadmin exportaria da lista global e receberia só os usuários do
   * próprio tenant, com a mesma contagem de linhas de sempre e nenhum aviso: o
   * relatório sairia parecendo completo. Ver `TAcoesDeExportacao`.
   */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<UsuarioResponse>> => {
      const filtros = {
        nome: nomeBusca || undefined,
        role: (role as Role) || undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        page: indice,
        size: limite,
      }
      const pagina = dentroDeTenant
        ? await usuarioService.getAll(filtros)
        : await usuarioService.getAllGlobal({ ...filtros, tenantId: tenantId || undefined })

      return {
        linhas: pagina.content,
        total: pagina.totalElements,
      }
    },
    [dentroDeTenant, nomeBusca, role, ativo, tenantId],
  )

  /**
   * Sem senha, sem hash, sem token — a lista de usuários é dado de acesso.
   *
   * <p>A coluna <b>Cliente</b> só existe fora de um tenant, como na tela: dentro
   * dele ela repetiria o mesmo nome em toda linha. E <b>Bloqueado</b> sai numa
   * coluna própria em vez de misturado com ativo/inativo, porque são coisas
   * diferentes: bloqueio é por tentativas de senha, e se desfaz num clique.
   */
  const colunasExportadas: ColunaExportavel<UsuarioResponse>[] = [
    { titulo: 'Nome', valor: (u) => u.nome },
    { titulo: 'E-mail', valor: (u) => u.email },
    ...(dentroDeTenant
      ? []
      : [{ titulo: 'Cliente', valor: (u: UsuarioResponse) => u.tenantNome }]),
    { titulo: 'Telefone', valor: (u) => formatarTelefone(u.codigoPais, u.telefone) },
    { titulo: 'Acesso', valor: (u) => ROLE_LABEL[u.role] },
    { titulo: 'Situação', valor: (u) => (u.ativo ? 'Ativo' : 'Inativo') },
    { titulo: 'Bloqueado', valor: (u) => (u.bloqueado ? 'Sim' : 'Não') },
    { titulo: 'Criado em', valor: (u) => formatarData(u.createdAt) },
  ]

  const filtrosAplicados = [
    {
      rotulo: 'Escopo',
      valor: dentroDeTenant ? `Tenant ${sessao?.tenantNome ?? ''}` : 'Todos os clientes',
    },
    { rotulo: 'Nome', valor: nomeBusca },
    { rotulo: 'Acesso', valor: rotuloDe(OPCOES_ROLE, role) ?? '' },
    { rotulo: 'Situação', valor: ativo === 'true' ? 'Ativo' : ativo === 'false' ? 'Inativo' : '' },
  ]

  async function desbloquear(usuario: UsuarioResponse) {
    try {
      await usuarioService.desbloquear(usuario.id)
      toast.success(`${usuario.nome} foi desbloqueado`)
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  /**
   * A visão global mostra usuários de todos os clientes, mas **escrever**
   * continua sendo dentro de um tenant: `GET/PUT /usuarios/{id}` filtram pelo
   * tenant efetivo, e devolveriam 404 para quem é de outro.
   *
   * Então abrir alguém de outro cliente entra naquele tenant antes — é o mesmo
   * que o combo do topo faz, só que num clique. A faixa de aviso aparece em
   * seguida deixando claro onde você está.
   */
  async function abrir(usuario: UsuarioResponse) {
    if (usuario.tenantId === sessao?.tenantId) {
      navigate(`/app/usuarios/${usuario.id}`)
      return
    }
    try {
      await switchTenant(usuario.tenantId)
      toast.info(`Você entrou em "${usuario.tenantNome}" para editar este usuário`)
      navigate(`/app/usuarios/${usuario.id}`)
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<UsuarioResponse>[] = [
    { chave: 'nome', cabecalho: 'Nome', render: (u) => u.nome },
    { chave: 'email', cabecalho: 'E-mail', render: (u) => u.email },
    ...(dentroDeTenant
      ? []
      : [
          {
            chave: 'tenant',
            cabecalho: 'Cliente',
            render: (u: UsuarioResponse) => u.tenantNome,
          },
        ]),
    {
      chave: 'telefone',
      cabecalho: 'Telefone',
      secundaria: true,
      render: (u) => formatarTelefone(u.codigoPais, u.telefone),
    },
    { chave: 'role', cabecalho: 'Acesso', render: (u) => ROLE_LABEL[u.role] },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (u) =>
        u.bloqueado ? (
          <TBadge tom="alerta">Bloqueado</TBadge>
        ) : u.ativo ? (
          <TBadge tom="sucesso">Ativo</TBadge>
        ) : (
          <TBadge tom="neutro">Inativo</TBadge>
        ),
    },
  ]

  return (
    <TPage
      title="Usuários"
      subtitle={
        dentroDeTenant
          ? `Dentro do tenant "${sessao?.tenantNome}" — mostrando só os usuários dele`
          : 'Todos os clientes. Use o filtro para restringir a um deles.'
      }
      actions={
        <>
          <TAcoesDeExportacao
            nome="Usuários"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/usuarios/novo')}>
            <IconAdicionar className="size-4" />
            Novo usuário
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Usuários"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(u) => u.id}
            resumo={[
              { rotulo: 'Usuários', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Ativos',
                valor: String(paraImprimir.linhas.filter((u) => u.ativo).length),
              },
              {
                rotulo: 'Bloqueados',
                valor: String(paraImprimir.linhas.filter((u) => u.bloqueado).length),
              },
            ]}
            nota="Nenhuma senha ou credencial sai nesta folha. Bloqueio é consequência de tentativas de login, e se desfaz na tela — não é o mesmo que inativo."
          />
        )
      }
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

          {!dentroDeTenant && (
            <TCombo
              label="Cliente"
              vazio="Todos"
              placeholder="Todos"
              value={tenantId}
              buscar={tenantService.select}
              onChange={(valor) => {
                setTenantId(valor)
                setPagina(0)
              }}
            />
          )}

          <TSelect
            label="Nível de acesso"
            vazio="Todos"
            opcoes={OPCOES_ROLE}
            value={role}
            onChange={(e) => {
              setRole(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Situação"
            vazio="Todas"
            opcoes={[
              { valor: 'true', rotulo: 'Ativo' },
              { valor: 'false', rotulo: 'Inativo' },
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
        chaveDe={(u) => u.id}
        carregando={carregando}
        onLinhaClick={(u) => void abrir(u)}
        tituloCartao={(u) => u.nome}
        vazioTitulo="Nenhum usuário encontrado"
        vazioDescricao="Ajuste os filtros ou cadastre o primeiro usuário."
        acoes={(u) =>
          u.bloqueado ? (
            <TButton
              variant="secondary"
              size="sm"
              onClick={() => void desbloquear(u)}
              title="Liberar conta bloqueada por tentativas de login"
            >
              <IconDesbloquear className="size-4" />
              Desbloquear
            </TButton>
          ) : null
        }
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}
