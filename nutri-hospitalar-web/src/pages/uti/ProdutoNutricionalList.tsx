import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBloqueado, IconBusca } from '@/assets/icons'
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
import { produtoNutricionalService } from '@/services/utiService'
import type { Page } from '@/types/comum'
import {
  TIPOS_PRODUTO,
  type ProdutoNutricionalResponse,
  type TipoProdutoNutricional,
} from '@/types/uti'
import { formatarNumero } from '@/utils/format'

export function ProdutoNutricionalList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [tipo, setTipo] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [origem, setOrigem] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<ProdutoNutricionalResponse>>()
  const [carregando, setCarregando] = useState(true)

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    produtoNutricionalService
      .getAll({
        nome: nomeBusca || undefined,
        tipo: (tipo || undefined) as TipoProdutoNutricional | undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        global: origem === '' ? undefined : origem === 'sistema',
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com os dados do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, nomeBusca, tipo, ativo, origem, pagina])

  useEffect(carregar, [carregar])

  async function alternarAtivo(produto: ProdutoNutricionalResponse) {
    try {
      await produtoNutricionalService.alterarAtivo(produto.id, !produto.ativo)
      toast.success(
        produto.ativo
          ? `${produto.nome} saiu da lista de escolha`
          : `${produto.nome} voltou para a lista`,
      )
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<ProdutoNutricionalResponse>[] = [
    {
      chave: 'nome',
      cabecalho: 'Produto',
      render: (p) => (
        <span className="flex flex-wrap items-center gap-2">
          <span>{p.nome}</span>
          {/* O usuário precisa saber antes de clicar que não vai poder editar */}
          {p.global && (
            <TBadge tom="info" icone={<IconBloqueado className="size-3" />}>
              Do sistema
            </TBadge>
          )}
        </span>
      ),
    },
    {
      chave: 'tipo',
      cabecalho: 'Tipo',
      render: (p) => (
        <span className="flex flex-wrap items-center gap-2">
          <span>{p.tipoDescricao}</span>
          {/* O papel é o que faz o insumo entrar na receita — vale mostrar */}
          {p.papelDescricao && <TBadge tom="neutro">{p.papelDescricao}</TBadge>}
        </span>
      ),
    },
    {
      chave: 'medida',
      cabecalho: 'Medida',
      render: (p) => `${p.medidaNome} ${formatarNumero(p.medidaQtd)}`,
    },
    {
      chave: 'kcal',
      cabecalho: 'Kcal / medida',
      numerica: true,
      /*
       * Suplemento oral aceita composição incompleta — ali ele é consulta, não
       * operando. O que entra em cálculo o banco obriga a ter.
       */
      render: (p) =>
        p.kcal != null ? (
          formatarNumero(p.kcal)
        ) : (
          <span className="text-txt-muted">Não informado</span>
        ),
    },
    {
      chave: 'proteina',
      cabecalho: 'Proteína / medida',
      numerica: true,
      render: (p) =>
        p.proteinaG != null ? (
          `${formatarNumero(p.proteinaG)} g`
        ) : (
          <span className="text-txt-muted">Não informada</span>
        ),
    },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (p) =>
        p.ativo ? <TBadge tom="sucesso">Ativo</TBadge> : <TBadge tom="neutro">Inativo</TBadge>,
    },
  ]

  return (
    <TPage
      title="Produtos nutricionais"
      subtitle="Suplementos orais, módulos proteicos e insumos de dieta artesanal. Os módulos e os insumos alimentam o cálculo — cadastrar um novo faz efeito na sugestão."
      actions={
        <TButton onClick={() => navigate('/app/uti/produtos/novo')}>
          <IconAdicionar className="size-4" />
          Novo produto
        </TButton>
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
          <TSelect
            label="Tipo"
            vazio="Todos"
            opcoes={TIPOS_PRODUTO.map((t) => ({ valor: t.valor, rotulo: t.rotulo }))}
            value={tipo}
            onChange={(e) => {
              setTipo(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Origem"
            vazio="Todas"
            opcoes={[
              { valor: 'sistema', rotulo: 'Do sistema' },
              { valor: 'proprias', rotulo: 'Meus produtos' },
            ]}
            value={origem}
            onChange={(e) => {
              setOrigem(e.target.value)
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
        chaveDe={(p) => p.id}
        carregando={carregando}
        // O do sistema não abre para edição — clicar e levar um 400 seria pior
        onLinhaClick={(p) => (p.global ? undefined : navigate(`/app/uti/produtos/${p.id}`))}
        tituloCartao={(p) => p.nome}
        vazioTitulo="Nenhum produto encontrado"
        vazioDescricao="Ajuste os filtros ou cadastre um produto próprio."
        acoes={(p) =>
          p.global ? (
            <span className="text-caption text-txt-muted">Somente leitura</span>
          ) : (
            <TButton
              variant="secondary"
              size="sm"
              onClick={() => void alternarAtivo(p)}
              title={
                p.ativo
                  ? 'Inativar — some da escolha, e as avaliações já feitas não mudam'
                  : 'Reativar o produto'
              }
            >
              {p.ativo ? 'Inativar' : 'Reativar'}
            </TButton>
          )
        }
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}
