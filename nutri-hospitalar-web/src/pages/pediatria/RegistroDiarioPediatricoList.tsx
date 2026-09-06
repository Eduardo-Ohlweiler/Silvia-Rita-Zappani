import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconAlerta, IconBusca } from '@/assets/icons'
import {
  TAcoesDeExportacao,
  TBadge,
  TButton,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TModal,
  TPage,
  TPanel,
  type Coluna,
  type PaginaDaCarga,
  type ResultadoDaCarga,
} from '@/components/common'
import { DocumentoLista } from '@/components/impressao/DocumentoLista'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { registroDiarioPediatricoService } from '@/services/pediatriaService'
import type { Page } from '@/types/comum'
import type { RegistroDiarioPediatricoLista } from '@/types/pediatria'
import { formatarData, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto } from '@/utils/idade'
import type { ColunaExportavel } from '@/utils/planilha'

/**
 * Os dias de acompanhamento nutricional pediátrico — docs/11.
 *
 * A coluna de **idade** existe porque na pediatria ela anda: dois registros do
 * mesmo paciente com 30 dias de intervalo são idades diferentes, e sem ela a
 * lista pareceria repetir a mesma criança.
 */
export function RegistroDiarioPediatricoList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<RegistroDiarioPediatricoLista>>()
  const [carregando, setCarregando] = useState(true)
  const [aRemover, setARemover] = useState<RegistroDiarioPediatricoLista>()
  const [paraImprimir, setParaImprimir] =
    useState<ResultadoDaCarga<RegistroDiarioPediatricoLista>>()

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    registroDiarioPediatricoService
      .getAll({
        pessoaNome: nomeBusca || undefined,
        de: de || undefined,
        ate: ate || undefined,
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a consulta.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, nomeBusca, de, ate, pagina])

  useEffect(carregar, [carregar])

  async function remover() {
    if (!aRemover) return
    try {
      await registroDiarioPediatricoService.remover(aRemover.id)
      toast.success('Acompanhamento removido')
      setARemover(undefined)
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  /** Cobre o filtro inteiro, não a página aberta. Ver `TAcoesDeExportacao`. */
  const carregarTudo = useCallback(
    async (limite: number, indice: number): Promise<PaginaDaCarga<RegistroDiarioPediatricoLista>> => {
      const pagina = await registroDiarioPediatricoService.getAll({
        pessoaNome: nomeBusca || undefined,
        de: de || undefined,
        ate: ate || undefined,
        page: indice,
        size: limite,
      })
      return {
        linhas: pagina.content,
        total: pagina.totalElements,
      }
    },
    [nomeBusca, de, ate],
  )

  /**
   * O resumo de um dia no papel. A idade entra logo depois da data porque é ela
   * que dá sentido ao peso — 9 kg aos 8 meses e 9 kg aos 3 anos são leituras
   * opostas.
   */
  const colunasExportadas: ColunaExportavel<RegistroDiarioPediatricoLista>[] = [
    { titulo: 'Paciente', valor: (r) => r.pessoaNome },
    { titulo: 'Data', valor: (r) => formatarData(r.data) },
    { titulo: 'Idade', valor: (r) => idadeEmMesesTexto(r.idadeMeses) },
    { titulo: 'Peso (kg)', numerica: true, valor: (r) => txt(r.pesoKg, 3) },
    // O prescrito da coluna é o que o percentual usou, e a procedência vem ao
    // lado: numa planilha lida offline é onde a diferença entre "o que digitei"
    // e "contra o que mediu" mais precisa estar escrita.
    { titulo: 'Prescrito (ml)', numerica: true, valor: (r) => txt(r.prescritoDeReferencia, 0) },
    { titulo: 'Referência do prescrito', valor: (r) => r.referenciaDoRecebido ?? '' },
    {
      titulo: 'Prescrito informado no dia (ml)',
      numerica: true,
      valor: (r) => txt(r.volPrescrito24h, 0),
    },
    { titulo: 'Recebido (ml)', numerica: true, valor: (r) => txt(r.volRecebido24h, 0) },
    { titulo: 'Recebido (%)', numerica: true, valor: (r) => txt(r.percentualRecebido, 1) },
    { titulo: 'Adequação calórica (%)', numerica: true, valor: (r) => txt(r.adequacaoCalorica, 1) },
    { titulo: 'Tomadas aceitas (%)', numerica: true, valor: (r) => txt(r.aceitacaoTomadas, 1) },
    {
      titulo: 'Avaliação vinculada',
      valor: (r) => (r.temAvaliacao ? 'Sim' : 'Não — as adequações não existem'),
    },
  ]

  const filtrosAplicados = [
    { rotulo: 'Paciente', valor: nomeBusca },
    { rotulo: 'De', valor: de ? formatarData(de) : '' },
    { rotulo: 'Até', valor: ate ? formatarData(ate) : '' },
  ]

  const colunas: Coluna<RegistroDiarioPediatricoLista>[] = [
    {
      chave: 'paciente',
      cabecalho: 'Paciente',
      render: (r) => (
        <span className="flex flex-wrap items-center gap-2">
          <span>{r.pessoaNome}</span>
          {/*
            Sem vínculo não há adequação calórica nem proteica. Marcar a linha é
            melhor que deixar duas colunas vazias sem explicação.
          */}
          {!r.temAvaliacao && (
            <TBadge tom="alerta" icone={<IconAlerta className="size-3" />}>
              Sem avaliação
            </TBadge>
          )}
        </span>
      ),
    },
    {
      chave: 'data',
      cabecalho: 'Data e idade',
      render: (r) => (
        <span className="flex flex-col">
          <span>{formatarData(r.data)}</span>
          {r.idadeMeses != null && (
            <span className="text-caption text-txt-muted">{idadeEmMesesTexto(r.idadeMeses)}</span>
          )}
        </span>
      ),
    },
    {
      chave: 'peso',
      cabecalho: 'Peso do dia',
      numerica: true,
      render: (r) =>
        r.pesoKg != null ? (
          `${formatarNumero(r.pesoKg, 3)} kg`
        ) : (
          <span className="text-txt-muted">não medido</span>
        ),
    },
    {
      chave: 'volume',
      cabecalho: 'Recebido / prescrito',
      numerica: true,
      render: (r) => (
        <span className="flex flex-col items-end">
          {/* O denominador é o que a conta usou, não o digitado no dia — senão
              a divisão na tela não fecha com o percentual logo abaixo. */}
          <span>
            {formatarNumero(r.volRecebido24h)} / {formatarNumero(r.prescritoDeReferencia)} ml
          </span>
          {r.percentualRecebido != null && (
            <span className="text-caption text-txt-muted">
              {formatarNumero(r.percentualRecebido)} % {r.referenciaDoRecebido ?? 'do prescrito'}
            </span>
          )}
        </span>
      ),
    },
    {
      chave: 'adequacao',
      cabecalho: 'Adequação e aceitação',
      numerica: true,
      render: (r) => (
        <span className="flex flex-col items-end">
          {r.adequacaoCalorica != null ? (
            <span>{formatarNumero(r.adequacaoCalorica)} % do VET</span>
          ) : (
            <span className="text-txt-muted">Precisa da avaliação</span>
          )}
          {r.aceitacaoTomadas != null && (
            <span className="text-caption text-txt-muted">
              {formatarNumero(r.aceitacaoTomadas)} % das tomadas
            </span>
          )}
        </span>
      ),
    },
  ]

  return (
    <TPage
      title="Acompanhamento diário"
      subtitle="Um registro por paciente por dia. O peso do dia classifica na curva da OMS, e o que chegou é comparado com o que a avaliação prescreveu."
      actions={
        <>
          <TAcoesDeExportacao
            nome="Acompanhamento diário pediátrico"
            colunas={colunasExportadas}
            carregar={carregarTudo}
            aoCarregarParaImprimir={setParaImprimir}
          />
          <TButton onClick={() => navigate('/app/pediatria/acompanhamento/novo')}>
            <IconAdicionar className="size-4" />
            Novo dia
          </TButton>
        </>
      }
      documento={
        paraImprimir && (
          <DocumentoLista
            titulo="Acompanhamento diário pediátrico"
            colunas={colunasExportadas}
            linhas={paraImprimir.linhas}
            truncado={paraImprimir.restantes}
            filtros={filtrosAplicados}
            chaveDe={(r) => r.id}
            resumo={[
              { rotulo: 'Dias', valor: String(paraImprimir.linhas.length) },
              {
                rotulo: 'Pacientes',
                valor: String(new Set(paraImprimir.linhas.map((r) => r.pessoaNome)).size),
              },
              {
                rotulo: 'Sem avaliação',
                valor: String(paraImprimir.linhas.filter((r) => !r.temAvaliacao).length),
              },
            ]}
            nota="A classificação usa o peso e a idade DO DIA, contra as curvas da OMS (docs/09 §4). As adequações são medidas contra o VET e a necessidade proteica da avaliação vigente — que as DRIs 2002 definem até 35 e 36 meses."
          />
        )
      }
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <TEntry
            label="Paciente"
            placeholder="Buscar por nome"
            value={nome}
            suffix={<IconBusca className="size-4" />}
            onChange={(e) => {
              setNome(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="De"
            type="date"
            value={de}
            onChange={(e) => {
              setDe(e.target.value)
              setPagina(0)
            }}
          />
          <TEntry
            label="Até"
            type="date"
            value={ate}
            onChange={(e) => {
              setAte(e.target.value)
              setPagina(0)
            }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(r) => r.id}
        carregando={carregando}
        onLinhaClick={(r) => navigate(`/app/pediatria/acompanhamento/${r.id}`)}
        tituloCartao={(r) => `${r.pessoaNome} · ${formatarData(r.data)}`}
        vazioTitulo="Nenhum dia registrado"
        vazioDescricao="Ajuste os filtros ou registre o dia de hoje."
        acoes={(r) => (
          <TButton variant="secondary" size="sm" onClick={() => setARemover(r)}>
            Remover
          </TButton>
        )}
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />

      <TModal
        aberto={!!aRemover}
        titulo="Remover acompanhamento"
        onFechar={() => setARemover(undefined)}
        acoes={
          <>
            <TButton variant="secondary" onClick={() => setARemover(undefined)}>
              Cancelar
            </TButton>
            <TButton onClick={() => void remover()}>Remover</TButton>
          </>
        }
      >
        <p className="text-body text-txt-secondary">
          O acompanhamento de <strong>{aRemover?.pessoaNome}</strong> em{' '}
          {aRemover && formatarData(aRemover.data)} será removido. Isso não pode ser desfeito.
        </p>
      </TModal>
    </TPage>
  )
}

/** Mesma implementação da lista de UTI: ausência é o traço, em toda exportação. */
function txt(valor?: number | null, casas = 1): string {
  return valor == null ? '—' : formatarNumero(valor, casas, casas)
}
