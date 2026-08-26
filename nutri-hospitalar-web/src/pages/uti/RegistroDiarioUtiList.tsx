import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { IconAdicionar, IconAlerta, IconBusca } from '@/assets/icons'
import {
  TBadge,
  TButton,
  TDataGrid,
  TDataGridFooter,
  TEntry,
  TModal,
  TPage,
  TPanel,
  type Coluna,
} from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { registroDiarioUtiService } from '@/services/utiService'
import type { Page } from '@/types/comum'
import type { RegistroDiarioUtiLista } from '@/types/uti'
import { formatarData, formatarNumero } from '@/utils/format'

export function RegistroDiarioUtiList() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [nome, setNome] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<RegistroDiarioUtiLista>>()
  const [carregando, setCarregando] = useState(true)
  const [aRemover, setARemover] = useState<RegistroDiarioUtiLista>()

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    registroDiarioUtiService
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
      await registroDiarioUtiService.remover(aRemover.id)
      toast.success('Acompanhamento removido')
      setARemover(undefined)
      carregar()
    } catch (erro) {
      handleApiError(erro)
    }
  }

  const colunas: Coluna<RegistroDiarioUtiLista>[] = [
    {
      chave: 'paciente',
      cabecalho: 'Paciente',
      render: (r) => (
        <span className="flex flex-wrap items-center gap-2">
          <span>{r.pessoaNome}</span>
          {/*
            Sem vínculo não há kcal/kg nem diurese por quilo. Marcar a linha é
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
    { chave: 'data', cabecalho: 'Data', render: (r) => formatarData(r.data) },
    {
      chave: 'volume',
      cabecalho: 'Recebido / prescrito',
      numerica: true,
      render: (r) => (
        <span className="flex flex-col items-end">
          <span>
            {formatarNumero(r.volRecebido24h)} / {formatarNumero(r.volPrescrito24h)} ml
          </span>
          {r.percentualRecebido != null && (
            <span className="text-caption text-txt-muted">
              {formatarNumero(r.percentualRecebido)} % da prescrição
            </span>
          )}
        </span>
      ),
    },
    {
      chave: 'kcalkg',
      cabecalho: 'Recebido por quilo',
      numerica: true,
      render: (r) =>
        r.caloriasPorQuilo != null ? (
          `${formatarNumero(r.caloriasPorQuilo)} kcal/kg`
        ) : (
          <span className="text-txt-muted">Precisa da avaliação</span>
        ),
    },
    {
      chave: 'balanco',
      cabecalho: 'Balanço e diurese',
      numerica: true,
      render: (r) => (
        <span className="flex flex-col items-end">
          <span className={r.balancoHidricoMl != null && r.balancoHidricoMl < 0 ? 'text-info' : ''}>
            {r.balancoHidricoMl != null ? `${formatarNumero(r.balancoHidricoMl)} ml` : '—'}
          </span>
          {r.diureseMl != null && (
            <span className="text-caption text-txt-muted">
              diurese {formatarNumero(r.diureseMl)} ml
            </span>
          )}
        </span>
      ),
    },
  ]

  return (
    <TPage
      title="Acompanhamento diário"
      subtitle="Um registro por paciente por dia. O que chegou é comparado com o que a avaliação prescreveu."
      actions={
        <TButton onClick={() => navigate('/app/uti/acompanhamento/novo')}>
          <IconAdicionar className="size-4" />
          Novo dia
        </TButton>
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
        onLinhaClick={(r) => navigate(`/app/uti/acompanhamento/${r.id}`)}
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
