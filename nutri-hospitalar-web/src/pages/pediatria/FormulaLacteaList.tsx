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
import { formulaLacteaService } from '@/services/pediatriaService'
import type { Page } from '@/types/comum'
import type { FormulaLacteaResponse } from '@/types/pediatria'
import { formatarNumero } from '@/utils/format'
import type { ColunaExportavel } from '@/utils/planilha'

export function FormulaLacteaList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [origem, setOrigem] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<FormulaLacteaResponse>>()
  const [carregando, setCarregando] = useState(true)

  const [paraImprimir, setParaImprimir] = useState<ResultadoDaCarga<FormulaLacteaResponse>>()

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    formulaLacteaService
      .getAll({
        nome: nomeBusca || undefined,
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
  }, [sessao?.tenantId, nomeBusca, ativo, origem, pagina])

  useEffect(carregar, [carregar])

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number): Promise<ResultadoDaCarga<FormulaLacteaResponse>> => {
      const pagina = await formulaLacteaService.getAll({
        nome: nomeBusca || undefined,
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
    [nomeBusca, ativo, origem],
  )

  /**
   * A composição, mais a razão que o cadastro usa para recusar erro de
   * digitação.
   *
   * <p>A coluna <b>proteína por 100 kcal</b> não está na tela e existe só aqui:
   * é a régua invariante de escala, e é ela que permite conferir uma lista
   * inteira de uma olhada — todo produto real cai entre 1,8 e 3,0 (Codex
   * CXS 72-1981). Uma linha fora da faixa é rótulo mal transcrito, e numa
   * planilha isso se acha ordenando a coluna.
   */
  const colunasExportadas: ColunaExportavel<FormulaLacteaResponse>[] = [
    { titulo: 'Fórmula', valor: (f) => f.nome },
    { titulo: 'Origem', valor: (f) => (f.global ? 'Do sistema' : 'Própria') },
    {
      titulo: 'Kcal / 100 ml',
      numerica: true,
      valor: (f) => formatarNumero(f.kcalPor100ml, 1, 1),
    },
    {
      titulo: 'Proteína g / 100 ml',
      numerica: true,
      valor: (f) => formatarNumero(f.proteinaPor100ml, 2, 2),
    },
    {
      titulo: 'Proteína g / 100 kcal',
      numerica: true,
      valor: (f) =>
        f.kcalPor100ml > 0
          ? formatarNumero((f.proteinaPor100ml / f.kcalPor100ml) * 100, 2, 2)
          : '',
    },
    { titulo: 'Situação', valor: (f) => (f.ativo ? 'Ativa' : 'Inativa') },
  ]

  const filtrosAplicados = [
    { rotulo: 'Nome', valor: nomeBusca },
    {
      rotulo: 'Origem',
      valor: origem === 'sistema' ? 'Do sistema' : origem === 'proprias' ? 'Minhas fórmulas' : '',
    },
    { rotulo: 'Situação', valor: ativo === 'true' ? 'Ativa' : ativo === 'false' ? 'Inativa' : '' },
  ]

  async function alternarAtivo(formula: FormulaLacteaResponse) {
    try {
      await formulaLacteaService.alterarAtivo(formula.id, !formula.ativo)
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

  const colunas: Coluna<FormulaLacteaResponse>[] = [
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
      chave: 'kcal',
      cabecalho: 'Kcal / 100 ml',
      numerica: true,
      render: (f) => formatarNumero(f.kcalPor100ml),
    },
    {
      chave: 'proteina',
      cabecalho: 'Proteína / 100 ml',
      numerica: true,
      render: (f) => `${formatarNumero(f.proteinaPor100ml)} g`,
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
      title="Fórmulas lácteas"
      subtitle="As do sistema vêm prontas e valem para todos. Cadastre as suas quando precisar de outra composição."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Fórmulas lácteas"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/pediatria/formulas-lacteas/nova')}>
            <IconAdicionar className="size-4" />
            Nova fórmula
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Fórmulas lácteas"
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
                rotulo: 'Ativas',
                valor: String(paraImprimir.linhas.filter((f) => f.ativo).length),
              },
            ]}
            nota="Composição por 100 ml do produto reconstituído, nunca por 100 g de pó. A coluna de proteína por 100 kcal é a régua de conferência: produto real cai entre 1,8 e 3,0 (Codex CXS 72-1981)."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
        onLinhaClick={(f) =>
          f.global ? undefined : navigate(`/app/pediatria/formulas-lacteas/${f.id}`)
        }
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
