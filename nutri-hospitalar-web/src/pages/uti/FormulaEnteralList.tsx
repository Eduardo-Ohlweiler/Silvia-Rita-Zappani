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
  type PaginaDaCarga,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { formulaEnteralService } from '@/services/utiService'
import type { Page } from '@/types/comum'
import { CATEGORIAS_ENTERAIS, type CategoriaFormulaEnteral } from '@/types/uti'
import type { FormulaEnteralResponse } from '@/types/uti'
import { formatarNumero, rotuloDe } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

export function FormulaEnteralList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [categoria, setCategoria] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [origem, setOrigem] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<FormulaEnteralResponse>>()
  const [carregando, setCarregando] = useState(true)
  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<FormulaEnteralResponse>>()

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    formulaEnteralService
      .getAll({
        nome: nomeBusca || undefined,
        categoria: (categoria || undefined) as CategoriaFormulaEnteral | undefined,
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
  }, [sessao?.tenantId, nomeBusca, categoria, ativo, origem, pagina])

  useEffect(carregar, [carregar])

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<FormulaEnteralResponse>> => {
      const pagina = await formulaEnteralService.getAll({
        nome: nomeBusca || undefined,
        categoria: (categoria || undefined) as CategoriaFormulaEnteral | undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        global: origem === '' ? undefined : origem === 'sistema',
        page: indice,
        size: limite,
      })
      return {
        linhas: pagina.content,
        total: pagina.totalElements,
      }
    },
    [nomeBusca, categoria, ativo, origem],
  )

  /**
   * A composição inteira, e não as seis colunas da tela.
   *
   * <p>Quem exporta este catálogo está conferindo rótulo contra cadastro ou
   * levando a lista para a comissão de padronização — e para isso a osmolaridade
   * e o potássio importam tanto quanto a densidade, mesmo cabendo mal na grade.
   * A água livre sai dizendo <b>quando é estimada</b>: numa planilha, "22" e
   * "22 estimado" não podem ser a mesma célula.
   */
  const colunasExportadas: ColunaExportavel<FormulaEnteralResponse>[] = [
    { titulo: 'Fórmula', valor: (f) => f.nome },
    { titulo: 'Origem', valor: (f) => (f.global ? 'Do sistema' : 'Própria') },
    { titulo: 'Categoria', valor: (f) => f.categoriaDescricao ?? 'Não informada' },
    { titulo: 'Densidade (kcal/ml)', numerica: true, valor: (f) => txt(f.densidadeKcalMl, 2) },
    { titulo: 'Proteína (g/L)', numerica: true, valor: (f) => txt(f.proteinaGL, 2) },
    { titulo: 'Carboidrato (g/L)', numerica: true, valor: (f) => txt(f.choGL, 2) },
    { titulo: 'Lipídio (g/L)', numerica: true, valor: (f) => txt(f.lipGL, 2) },
    { titulo: 'Fibras (g/L)', numerica: true, valor: (f) => txt(f.fibrasGL, 2) },
    { titulo: 'Potássio (mg/L)', numerica: true, valor: (f) => txt(f.potassioMgL, 1) },
    { titulo: 'Osmolaridade (mOsm/L)', numerica: true, valor: (f) => txt(f.osmolaridadeMosmL, 1) },
    {
      titulo: 'Água livre (%)',
      numerica: true,
      valor: (f) => (f.aguaLivrePerc != null ? txt(f.aguaLivrePerc, 1) : 'Estimada pela densidade'),
    },
    { titulo: 'Situação', valor: (f) => (f.ativo ? 'Ativa' : 'Inativa') },
  ]

  const filtrosAplicados = [
    { rotulo: 'Nome', valor: nomeBusca },
    { rotulo: 'Categoria', valor: rotuloDe(CATEGORIAS_ENTERAIS, categoria) ?? '' },
    { rotulo: 'Origem', valor: origem === 'sistema' ? 'Do sistema' : origem === 'proprias' ? 'Minhas fórmulas' : '' },
    { rotulo: 'Situação', valor: ativo === 'true' ? 'Ativa' : ativo === 'false' ? 'Inativa' : '' },
  ]

  async function alternarAtivo(formula: FormulaEnteralResponse) {
    try {
      await formulaEnteralService.alterarAtivo(formula.id, !formula.ativo)
      toast.success(
        formula.ativo
          ? `${formula.nome} saiu da lista de escolha`
          : `${formula.nome} voltou para a lista`,
      )
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<FormulaEnteralResponse>[] = [
    {
      chave: 'nome',
      cabecalho: 'Fórmula',
      render: (f) => (
        <span className="flex flex-wrap items-center gap-2">
          <span>{f.nome}</span>
          {/* O usuário precisa saber antes de clicar que não vai poder editar */}
          {f.global && (
            <TBadge tom="info" icone={<IconBloqueado className="size-3" />}>
              Do sistema
            </TBadge>
          )}
        </span>
      ),
    },
    {
      chave: 'categoria',
      cabecalho: 'Categoria',
      // Categoria é anulável: produto sem categoria declarada continua utilizável
      render: (f) => f.categoriaDescricao ?? <span className="text-txt-muted">Não informada</span>,
    },
    {
      chave: 'densidade',
      cabecalho: 'Densidade',
      numerica: true,
      render: (f) => `${formatarNumero(f.densidadeKcalMl)} kcal/ml`,
    },
    {
      chave: 'proteina',
      cabecalho: 'Proteína / L',
      numerica: true,
      render: (f) => `${formatarNumero(f.proteinaGL)} g`,
    },
    {
      chave: 'aguaLivre',
      cabecalho: 'Água livre',
      numerica: true,
      /*
       * A ausência carrega o motivo. Sem o dado do rótulo o cálculo estima pela
       * densidade — e quem cadastra precisa ver onde falta preencher, não um
       * traço mudo.
       */
      render: (f) =>
        f.aguaLivrePerc != null ? (
          `${formatarNumero(f.aguaLivrePerc)} %`
        ) : (
          <span className="text-txt-muted" title="Sem o dado do rótulo, o cálculo estima pela densidade">
            Estimada
          </span>
        ),
    },
    {
      chave: 'situacao',
      cabecalho: 'Situação',
      render: (f) =>
        f.ativo ? <TBadge tom="sucesso">Ativa</TBadge> : <TBadge tom="neutro">Inativa</TBadge>,
    },
  ]

  return (
    <TPage
      title="Fórmulas enterais"
      subtitle="Composição sempre por litro — inclusive a de produto em frasco de 500 ml. As do sistema vêm prontas e valem para todos."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Fórmulas enterais"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/uti/formulas-enterais/nova')}>
            <IconAdicionar className="size-4" />
            Nova fórmula
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Fórmulas enterais"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(f) => f.id}
            resumo={[
              { rotulo: 'Fórmulas', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Do sistema',
                valor: String(paraImprimir.linhas.filter((f) => f.global).length),
              },
              {
                rotulo: 'Água livre no rótulo',
                valor: String(paraImprimir.linhas.filter((f) => f.aguaLivrePerc != null).length),
              },
            ]}
            nota="Composição sempre por litro, inclusive a de produto em frasco de 500 ml. Onde a água livre não veio do rótulo, o cálculo a estima pela densidade — e a coluna diz isso."
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
            label="Categoria"
            vazio="Todas"
            opcoes={CATEGORIAS_ENTERAIS.map((c) => ({ valor: c.valor, rotulo: c.rotulo }))}
            value={categoria}
            onChange={(e) => {
              setCategoria(e.target.value)
              setPagina(0)
            }}
          />
          <TSelect
            label="Origem"
            vazio="Todas"
            opcoes={[
              { valor: 'sistema', rotulo: 'Do sistema' },
              { valor: 'proprias', rotulo: 'Minhas fórmulas' },
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
              { valor: 'true', rotulo: 'Ativa' },
              { valor: 'false', rotulo: 'Inativa' },
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
        chaveDe={(f) => f.id}
        carregando={carregando}
        // A do sistema não abre para edição — clicar e levar um 400 seria pior
        onLinhaClick={(f) => (f.global ? undefined : navigate(`/app/uti/formulas-enterais/${f.id}`))}
        tituloCartao={(f) => f.nome}
        vazioTitulo="Nenhuma fórmula encontrada"
        vazioDescricao="Ajuste os filtros ou cadastre uma fórmula própria."
        acoes={(f) =>
          f.global ? (
            <span className="text-caption text-txt-muted">Somente leitura</span>
          ) : (
            <TButton
              variant="secondary"
              size="sm"
              onClick={() => void alternarAtivo(f)}
              title={
                f.ativo
                  ? 'Inativar — some da escolha, e as avaliações já feitas não mudam'
                  : 'Reativar a fórmula'
              }
            >
              {f.ativo ? 'Inativar' : 'Reativar'}
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
