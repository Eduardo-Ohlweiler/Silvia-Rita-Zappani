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
import { formulaLacteaService } from '@/services/pediatriaService'
import type { Page } from '@/types/comum'
import type { FormulaLacteaResponse } from '@/types/pediatria'
import { formatarNumero } from '@/utils/format'

export function FormulaLacteaList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [ativo, setAtivo] = useState('true')
  const [origem, setOrigem] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<FormulaLacteaResponse>>()
  const [carregando, setCarregando] = useState(true)

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
        <TButton onClick={() => navigate('/app/pediatria/formulas-lacteas/nova')}>
          <IconAdicionar className="size-4" />
          Nova fórmula
        </TButton>
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
