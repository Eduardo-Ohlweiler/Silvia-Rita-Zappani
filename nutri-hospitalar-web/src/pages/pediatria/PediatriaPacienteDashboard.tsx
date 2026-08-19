import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  TBadge,
  TButton,
  TCombo,
  TPage,
  TPanel,
  TSelect,
  TTabPanel,
  TTabs,
  type Aba,
} from '@/components/common'
import { CurvaCrescimento } from '@/components/pediatria/graficos/CurvaCrescimento'
import {
  CoberturaNoTempo,
  IngestaoEnergetica,
} from '@/components/pediatria/graficos/GraficosNutricao'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pediatriaService } from '@/services/pediatriaService'
import { pessoaService } from '@/services/pessoaService'
import type { CurvaOmsPonto, FaixaOms, PainelPaciente } from '@/types/pediatria'
import { formatarData, formatarDocumento, formatarNumero } from '@/utils/format'

const TOM_FAIXA: Record<FaixaOms, 'info' | 'sucesso' | 'alerta'> = {
  BAIXA: 'info',
  ADEQUADA: 'sucesso',
  ALTA: 'alerta',
}

const PERIODOS = [
  { valor: '0', rotulo: 'Todo o histórico' },
  { valor: '90', rotulo: 'Últimos 90 dias' },
  { valor: '180', rotulo: 'Últimos 6 meses' },
  { valor: '365', rotulo: 'Último ano' },
]

/** Folga em volta dos dados, para a criança não ficar colada na borda. */
const FOLGA_MESES = 3
/** Janela mínima: com uma avaliação só, sem largura não há faixa para desenhar. */
const JANELA_MINIMA = 12

/**
 * O acompanhamento de uma criança no tempo.
 *
 * <p>Em abas, como a tela de cálculo: oito blocos empilhados obrigariam a rolar
 * para comparar o que está no topo com o que está embaixo.
 *
 * <p>Os filtros ficam <b>acima das abas</b>, numa linha só — todo o painel
 * responde ao mesmo recorte, e filtro dentro de card faz o leitor achar que
 * vale só para aquele gráfico.
 */
export function PediatriaPacienteDashboard() {
  const navigate = useNavigate()
  const { sessao } = useAuth()
  // O ranking do painel gerencial linka para cá com o paciente na URL.
  const [parametros] = useSearchParams()

  const [pacienteId, setPacienteId] = useState(parametros.get('pacienteId') ?? '')
  const [pacienteRotulo, setPacienteRotulo] = useState('')
  const [dias, setDias] = useState('0')
  const [aba, setAba] = useState('resumo')

  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [dados, setDados] = useState<PainelPaciente>()
  const [curva, setCurva] = useState<CurvaOmsPonto[]>([])
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    if (!pacienteId) {
      setDados(undefined)
      return
    }
    setCarregando(true)
    pediatriaService
      .painelPaciente(pacienteId, Number(dias))
      .then((painel) => {
        setDados(painel)
        setPacienteRotulo(painel.pacienteNome)
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, pacienteId, dias])

  useEffect(carregar, [carregar])

  /** A janela da curva acompanha a criança, não os 60 meses inteiros. */
  const janela = useMemo(() => {
    const idades = (dados?.evolucao ?? []).map((p) => p.idadeMeses)
    if (idades.length === 0) return null

    const min = Math.min(...idades)
    const max = Math.max(...idades)
    let de = Math.max(0, min - FOLGA_MESES)
    let ate = max + FOLGA_MESES

    if (ate - de < JANELA_MINIMA) {
      const meio = Math.round((min + max) / 2)
      de = Math.max(0, meio - JANELA_MINIMA / 2)
      ate = de + JANELA_MINIMA
    }
    return { de, ate: Math.min(60, ate) }
  }, [dados])

  useEffect(() => {
    if (!dados?.sexo || !janela) {
      setCurva([])
      return
    }
    pediatriaService
      .curvaOms(dados.sexo, janela.de, janela.ate)
      .then(setCurva)
      .catch(handleApiError)
  }, [dados?.sexo, janela])

  const abas: Aba[] = [
    { id: 'resumo', rotulo: 'Resumo' },
    { id: 'crescimento', rotulo: 'Crescimento' },
    { id: 'nutricao', rotulo: 'Nutrição' },
    { id: 'historico', rotulo: 'Histórico' },
  ]

  const ultima = dados?.ultima

  return (
    <TPage
      title="Painel do paciente"
      subtitle="Onde a criança está hoje e por onde andou."
    >
      <div className="flex flex-col gap-4">
        {/* Uma linha de filtros para tudo o que vem abaixo */}
        <TPanel>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <TCombo
              label="Paciente"
              className="lg:col-span-2"
              placeholder="Buscar por nome ou documento"
              value={pacienteId}
              rotuloInicial={pacienteRotulo}
              onChange={setPacienteId}
              buscar={(termo) =>
                pessoaService.select(termo, tipoPacienteId).then((ps) =>
                  ps.map((p) => ({
                    id: p.id,
                    nome: p.documento
                      ? `${p.nome} (${formatarDocumento(p.documento)})`
                      : p.nome,
                  })),
                )
              }
            />
            <TSelect
              label="Período"
              opcoes={PERIODOS}
              value={dias}
              onChange={(e) => setDias(e.target.value)}
            />
          </div>
        </TPanel>

        {!pacienteId ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">
              Escolha um paciente para ver o acompanhamento.
            </p>
          </TPanel>
        ) : !dados ? (
          <TPanel>
            <p className="py-8 text-center text-body text-txt-secondary">Carregando…</p>
          </TPanel>
        ) : (
          <>
            <TTabs abas={abas} ativa={aba} onChange={setAba} />

            {/* Enquanto refaz a consulta, o desenho anterior fica esmaecido —
                pular para um esqueleto faria a página saltar. */}
            <div className={carregando ? 'opacity-50 transition-opacity' : undefined}>
              {/* ─── Resumo ───────────────────────────────────────── */}
              <TTabPanel id="resumo" ativa={aba}>
                <TPanel>
                  <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
                    <Indicador rotulo="Idade atual" valor={idade(dados.idadeMesesAtual)} />
                    <Indicador rotulo="Avaliações" valor={String(dados.totalAvaliacoes)} />
                    <Indicador rotulo="Nascimento" valor={formatarData(dados.dataNascimento)} />
                    <Indicador rotulo="1ª avaliação" valor={formatarData(dados.primeiraAvaliacao)} />
                    <Indicador rotulo="Última" valor={formatarData(dados.ultimaAvaliacao)} />
                  </div>

                  {ultima ? (
                    <>
                      <hr className="my-5 border-line" />
                      <h3 className="mb-4 text-h3 font-medium text-txt">
                        Última avaliação{' '}
                        <span className="text-caption font-normal text-txt-muted">
                          {formatarData(ultima.dataAvaliacao)} · {idade(ultima.idadeMeses)}
                        </span>
                      </h3>

                      <div className="mb-5 flex flex-wrap gap-2">
                        <Classificacao
                          rotulo="Peso / idade"
                          faixa={ultima.resultado.estadoNutricional.pesoIdade}
                        />
                        <Classificacao
                          rotulo="Estatura / idade"
                          faixa={ultima.resultado.estadoNutricional.estaturaIdade}
                        />
                        <Classificacao
                          rotulo="IMC / idade"
                          faixa={ultima.resultado.estadoNutricional.imcIdade}
                        />
                      </div>

                      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                        <Indicador rotulo="Peso" valor={num(ultima.peso, 'kg', 2)} />
                        <Indicador rotulo="Estatura" valor={num(ultima.estatura, 'cm', 1)} />
                        <Indicador
                          rotulo="IMC"
                          valor={num(ultima.resultado.estadoNutricional.imc, '', 2)}
                        />
                        <Indicador rotulo="Fórmula" valor={ultima.formulaNome ?? '—'} />
                        <Indicador
                          rotulo="VET"
                          valor={num(ultima.resultado.necessidades.vet, 'kcal/dia', 0)}
                        />
                        <Indicador
                          rotulo="Necessidade proteica"
                          valor={num(ultima.resultado.necessidades.proteina, 'g/dia', 1)}
                        />
                        <Indicador
                          rotulo="Cobertura calórica"
                          valor={num(ultima.resultado.dieta.percCalorico, '%', 1)}
                        />
                        <Indicador
                          rotulo="Cobertura proteica"
                          valor={num(ultima.resultado.dieta.percProteico, '%', 1)}
                        />
                      </div>

                      <div className="mt-5">
                        <TButton
                          variant="secondary"
                          onClick={() => navigate(`/app/pediatria/avaliacoes/${ultima.id}`)}
                        >
                          Abrir a avaliação
                        </TButton>
                      </div>
                    </>
                  ) : (
                    <p className="mt-5 text-body text-txt-secondary">
                      Este paciente ainda não tem avaliação no período escolhido.
                    </p>
                  )}
                </TPanel>
              </TTabPanel>

              {/* ─── Crescimento ──────────────────────────────────── */}
              <TTabPanel id="crescimento" ativa={aba}>
                <div className="flex flex-col gap-4">
                  {!dados.sexo && (
                    <TPanel>
                      <p className="text-body text-txt-secondary">
                        O sexo do paciente não está no cadastro, e as curvas da OMS
                        são separadas por sexo. Informe-o no cadastro da pessoa para
                        ver o crescimento.
                      </p>
                    </TPanel>
                  )}
                  {dados.sexo && (
                    <>
                      <TPanel>
                        <CurvaCrescimento
                          medida="peso"
                          curva={curva}
                          evolucao={dados.evolucao}
                          pacienteNome={dados.pacienteNome}
                        />
                      </TPanel>
                      <TPanel>
                        <CurvaCrescimento
                          medida="estatura"
                          curva={curva}
                          evolucao={dados.evolucao}
                          pacienteNome={dados.pacienteNome}
                        />
                      </TPanel>
                      <TPanel>
                        <CurvaCrescimento
                          medida="imc"
                          curva={curva}
                          evolucao={dados.evolucao}
                          pacienteNome={dados.pacienteNome}
                        />
                      </TPanel>
                    </>
                  )}
                </div>
              </TTabPanel>

              {/* ─── Nutrição ─────────────────────────────────────── */}
              <TTabPanel id="nutricao" ativa={aba}>
                <div className="flex flex-col gap-4">
                  <TPanel>
                    <CoberturaNoTempo evolucao={dados.evolucao} />
                  </TPanel>
                  <TPanel>
                    <IngestaoEnergetica evolucao={dados.evolucao} />
                  </TPanel>
                </div>
              </TTabPanel>

              {/* ─── Histórico ────────────────────────────────────── */}
              <TTabPanel id="historico" ativa={aba}>
                <div className="flex flex-col gap-4">
                  <TPanel title="Fórmulas utilizadas">
                    {dados.historicoFormulas.length === 0 ? (
                      <p className="text-body text-txt-secondary">
                        Nenhuma dieta prescrita no período.
                      </p>
                    ) : (
                      <ul className="flex flex-col divide-y divide-line">
                        {dados.historicoFormulas.map((f) => (
                          <li
                            key={f.formulaNome}
                            className="flex flex-wrap items-baseline gap-x-3 gap-y-1 py-2"
                          >
                            <span className="text-body text-txt">{f.formulaNome}</span>
                            <span className="text-caption text-txt-muted">
                              {formatarData(f.primeiroUso)} a {formatarData(f.ultimoUso)}
                            </span>
                            <span className="numeric ml-auto text-caption text-txt-secondary">
                              {f.avaliacoes}{' '}
                              {f.avaliacoes === 1 ? 'avaliação' : 'avaliações'}
                            </span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </TPanel>

                  {/* A tabela é o par legível dos gráficos: todo valor
                      desenhado acima também se lê aqui, sem depender de cor. */}
                  <TPanel title="Avaliações do período">
                    {dados.evolucao.length === 0 ? (
                      <p className="text-body text-txt-secondary">
                        Nenhuma avaliação no período.
                      </p>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[640px] text-body">
                          <thead>
                            <tr className="border-b border-line text-caption text-txt-secondary">
                              <th className="py-2 text-left font-medium">Data</th>
                              <th className="py-2 text-right font-medium">Idade</th>
                              <th className="py-2 text-right font-medium">Peso</th>
                              <th className="py-2 text-right font-medium">Estatura</th>
                              <th className="py-2 text-right font-medium">IMC</th>
                              <th className="py-2 text-right font-medium">VET</th>
                              <th className="py-2 text-right font-medium">Cobertura</th>
                            </tr>
                          </thead>
                          <tbody className="numeric">
                            {dados.evolucao.map((p) => (
                              <tr key={p.dataAvaliacao + p.idadeMeses} className="border-b border-line">
                                <td className="py-2 text-left">{formatarData(p.dataAvaliacao)}</td>
                                <td className="py-2 text-right">{p.idadeMeses} m</td>
                                <td className="py-2 text-right">{formatarNumero(p.peso, 2)}</td>
                                <td className="py-2 text-right">{formatarNumero(p.estatura, 1)}</td>
                                <td className="py-2 text-right">{formatarNumero(p.imc, 2)}</td>
                                <td className="py-2 text-right">{formatarNumero(p.vet, 0)}</td>
                                <td className="py-2 text-right">
                                  {p.percCalorico != null
                                    ? `${formatarNumero(p.percCalorico, 1)}%`
                                    : '—'}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </TPanel>
                </div>
              </TTabPanel>
            </div>
          </>
        )}
      </div>
    </TPage>
  )
}

// ─────────────────────────────────────────────────────────────────────

function Indicador({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-caption text-txt-secondary">{rotulo}</span>
      {/* Figuras proporcionais: `tabular-nums` afrouxa número grande solto */}
      <span className="text-h3 font-semibold text-txt">{valor}</span>
    </div>
  )
}

function Classificacao({
  rotulo,
  faixa,
}: {
  rotulo: string
  faixa?: { faixa: FaixaOms; rotulo: string } | null
}) {
  return (
    <span className="flex items-center gap-2">
      <span className="text-caption text-txt-secondary">{rotulo}:</span>
      {faixa ? (
        <TBadge tom={TOM_FAIXA[faixa.faixa]}>{faixa.rotulo}</TBadge>
      ) : (
        <span className="text-caption text-txt-muted">—</span>
      )}
    </span>
  )
}

function idade(meses?: number | null): string {
  if (meses == null) return '—'
  return `${meses} ${meses === 1 ? 'mês' : 'meses'}`
}

function num(valor: number | null | undefined, unidade: string, casas: number): string {
  if (valor == null) return '—'
  return `${formatarNumero(valor, casas)}${unidade ? ` ${unidade}` : ''}`
}
