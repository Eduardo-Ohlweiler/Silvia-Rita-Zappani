import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconBloqueado, IconBusca } from '@/assets/icons'
import {
  TAcoesDeExportacao,
  TBadge,
  TButton,
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
import { produtoNutricionalService } from '@/services/utiService'
import type { Page } from '@/types/comum'
import {
  TIPOS_PRODUTO,
  type ProdutoNutricionalResponse,
  type TipoProdutoNutricional,
} from '@/types/uti'
import { formatarNumero, rotuloDe } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

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
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<ProdutoNutricionalResponse>>()

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

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number): Promise<ResultadoDaCarga<ProdutoNutricionalResponse>> => {
      const pagina = await produtoNutricionalService.getAll({
        nome: nomeBusca || undefined,
        tipo: (tipo || undefined) as TipoProdutoNutricional | undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        global: origem === '' ? undefined : origem === 'sistema',
        page: 0,
        size: limite,
      })
      return {
        linhas: pagina.content,
        restantes: Math.max(0, pagina.totalElements - pagina.content.length),
      }
    },
    [nomeBusca, tipo, ativo, origem],
  )

  /**
   * A composição inteira por medida, mais a embalagem.
   *
   * <p>A medida vai em duas colunas — nome e quantidade — porque numa planilha
   * "Colher 5" é texto que não se soma. E a <b>embalagem</b> entra porque é ela
   * que transforma a receita em pedido de compra: sem a quantidade da lata
   * fechada não há como calcular embalagens por mês.
   */
  const colunasExportadas: ColunaExportavel<ProdutoNutricionalResponse>[] = [
    { titulo: 'Produto', valor: (p) => p.nome },
    { titulo: 'Origem', valor: (p) => (p.global ? 'Do sistema' : 'Próprio') },
    { titulo: 'Tipo', valor: (p) => p.tipoDescricao },
    { titulo: 'Papel na dieta artesanal', valor: (p) => p.papelDescricao ?? '' },
    { titulo: 'Medida', valor: (p) => p.medidaNome },
    { titulo: 'Qtd. da medida', numerica: true, valor: (p) => txt(p.medidaQtd, 2) },
    { titulo: 'Embalagem', numerica: true, valor: (p) => txt(p.embalagemQtd, 2) },
    { titulo: 'Kcal / medida', numerica: true, valor: (p) => txt(p.kcal, 2) },
    { titulo: 'Proteína (g)', numerica: true, valor: (p) => txt(p.proteinaG, 2) },
    { titulo: 'Carboidrato (g)', numerica: true, valor: (p) => txt(p.choG, 2) },
    { titulo: 'Açúcar (g)', numerica: true, valor: (p) => txt(p.acucarG, 2) },
    { titulo: 'Lipídio (g)', numerica: true, valor: (p) => txt(p.lipG, 2) },
    { titulo: 'Fibras (g)', numerica: true, valor: (p) => txt(p.fibrasG, 2) },
    { titulo: 'Sódio (mg)', numerica: true, valor: (p) => txt(p.sodioMg, 1) },
    { titulo: 'Potássio (mg)', numerica: true, valor: (p) => txt(p.potassioMg, 1) },
    { titulo: 'Fósforo (mg)', numerica: true, valor: (p) => txt(p.fosforoMg, 1) },
    { titulo: 'Ferro (mg)', numerica: true, valor: (p) => txt(p.ferroMg, 2) },
    { titulo: 'Osmolaridade (mOsm/L)', numerica: true, valor: (p) => txt(p.osmolaridadeMosmL, 1) },
    { titulo: 'Situação', valor: (p) => (p.ativo ? 'Ativo' : 'Inativo') },
  ]

  const filtrosAplicados = [
    { rotulo: 'Nome', valor: nomeBusca },
    { rotulo: 'Tipo', valor: rotuloDe(TIPOS_PRODUTO, tipo) ?? '' },
    {
      rotulo: 'Origem',
      valor: origem === 'sistema' ? 'Do sistema' : origem === 'proprias' ? 'Meus produtos' : '',
    },
    { rotulo: 'Situação', valor: ativo === 'true' ? 'Ativo' : ativo === 'false' ? 'Inativo' : '' },
  ]

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
        <>
          <TAcoesDeExportacao
            nome="Produtos nutricionais"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/uti/produtos/novo')}>
            <IconAdicionar className="size-4" />
            Novo produto
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Produtos nutricionais"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(p) => p.id}
            resumo={[
              { rotulo: 'Produtos', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Do sistema',
                valor: String(paraImprimir.linhas.filter((p) => p.global).length),
              },
              {
                rotulo: 'Insumos artesanais',
                valor: String(paraImprimir.linhas.filter((p) => p.papelArtesanal).length),
              },
            ]}
            nota="Composição por medida do próprio produto, não por 100 g — trocar de marca muda a medida. Suplemento oral aceita composição incompleta; o que entra em cálculo, não."
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

/** Número para célula: vazio continua vazio, e não vira zero. */
function txt(valor: number | null | undefined, casas: number): string {
  return valor == null ? '' : formatarNumero(valor, casas, casas)
}
