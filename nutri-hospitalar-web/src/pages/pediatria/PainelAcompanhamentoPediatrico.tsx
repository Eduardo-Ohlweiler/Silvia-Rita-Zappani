import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  TBotaoImprimir,
  TCombo,
  TEntry,
  TPage,
  TPanel,
  TTabPanel,
  TTabs,
  type Aba,
} from '@/components/common'
import { DocumentoPainelAcompanhamentoPediatrico } from '@/components/pediatria/impressao/DocumentoPainelAcompanhamentoPediatrico'
import { Indicador } from '@/components/graficos/chrome'
import { AdesaoNoTempo } from '@/components/graficos/MetaVersusOfertado'
import { SerieNoTempo } from '@/components/graficos/SerieNoTempo'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { registroDiarioPediatricoService } from '@/services/pediatriaService'
import type { PainelAcompanhamentoPediatrico as Painel } from '@/types/pediatria'
import { formatarData, formatarDocumento, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto } from '@/utils/idade'

const ABA_RESUMO = 'resumo'
const ABA_CRESCIMENTO = 'crescimento'
const ABA_OFERTA = 'oferta'
const ABA_TABELA = 'tabela'

/**
 * Os dias de acompanhamento de uma criança — o **terceiro painel** da
 * pediatria, que só existe porque agora há eixo do tempo.
 *
 * **O que ele mostra e o painel do paciente não mostrava:** a criança entre as
 * avaliações. Antes havia retratos soltos; aqui há a linha que os liga, com o
 * peso medido dia a dia e a oferta contra a prescrição.
 *
 * **Nada aqui é nota de desempenho.** A adequação calórica depende do dia da
 * internação e da conduta, e o sistema não classifica se o ganho de peso é
 * adequado — a velocidade esperada por idade não está na `Pediatria.xlsx` e não
 * entrou nesta fatia (docs/11 §2).
 */
export function PainelAcompanhamentoPediatrico() {
  const { sessao } = useAuth()
  // A lista e o painel do paciente linkam para cá com a pessoa na URL.
  const [parametros] = useSearchParams()

  const [pessoaId, setPessoaId] = useState(parametros.get('pessoaId') ?? '')
  const [pessoaRotulo, setPessoaRotulo] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [aba, setAba] = useState(ABA_RESUMO)

  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [dados, setDados] = useState<Painel>()
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id))
      .catch(handleApiError)
  }, [])

  const carregar = useCallback(() => {
    if (!pessoaId) {
      setDados(undefined)
      return
    }
    setCarregando(true)
    registroDiarioPediatricoService
      .painel(pessoaId, de || undefined, ate || undefined)
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId, pessoaId, de, ate])

  useEffect(carregar, [carregar])

  /**
   * Os pontos que os gráficos plotam.
   *
   * Achatados a partir dos dias inteiros — os derivados vivem dentro de
   * `derivados`, e o recharts precisa de campos rasos. **Nenhum número é
   * recalculado aqui**: tudo vem do servidor.
   */
  const pontos = (dados?.dias ?? []).map((d) => ({
    data: d.data,
    idadeMeses: d.derivados.idadeMeses ?? null,
    pesoKg: d.pesoKg ?? null,
    estaturaCm: d.estaturaCm ?? null,
    imc: d.derivados.imc ?? null,
    percentualRecebido: d.derivados.percentualRecebido ?? null,
    caloriasPorKg: d.derivados.caloriasPorKg ?? null,
    proteinaPorKg: d.derivados.proteinaPorKg ?? null,
    adequacaoCalorica: d.derivados.adequacaoCalorica ?? null,
    adequacaoProteica: d.derivados.adequacaoProteica ?? null,
    aceitacaoTomadas: d.derivados.aceitacaoTomadas ?? null,
  }))

  const abas: Aba[] = [
    { id: ABA_RESUMO, rotulo: 'Resumo' },
    { id: ABA_CRESCIMENTO, rotulo: 'Crescimento' },
    { id: ABA_OFERTA, rotulo: 'Oferta e adequação' },
    { id: ABA_TABELA, rotulo: 'Tabela' },
  ]

  const rotuloDoDia = (p: { data: string; idadeMeses: number | null }) =>
    `${formatarData(p.data)}${p.idadeMeses != null ? ` · ${idadeEmMesesTexto(p.idadeMeses)}` : ''}`

  return (
    <TPage
      title="Acompanhamento do paciente"
      subtitle="A criança entre as avaliações: o peso medido dia a dia, e o que ela recebeu contra o que foi prescrito."
      actions={dados && <TBotaoImprimir rotulo="Imprimir relatório" />}
      documento={dados && <DocumentoPainelAcompanhamentoPediatrico dados={dados} />}
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <TCombo
            label="Paciente"
            placeholder="Buscar por nome ou documento"
            value={pessoaId}
            rotuloInicial={pessoaRotulo}
            onChange={(id) => {
              setPessoaId(id)
              setPessoaRotulo('')
            }}
            buscar={(termo) =>
              pessoaService.select(termo, tipoPacienteId).then((ps) =>
                ps.map((p) => ({
                  id: p.id,
                  nome: p.documento ? `${p.nome} (${formatarDocumento(p.documento)})` : p.nome,
                })),
              )
            }
          />
          <TEntry label="De" type="date" value={de} onChange={(e) => setDe(e.target.value)} />
          <TEntry label="Até" type="date" value={ate} onChange={(e) => setAte(e.target.value)} />
        </div>
      </TPanel>

      {!pessoaId && (
        <TPanel>
          <p className="text-body text-txt-secondary">
            Escolha um paciente para ver os dias de acompanhamento.
          </p>
        </TPanel>
      )}

      {pessoaId && carregando && (
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      )}

      {dados && !carregando && dados.totalDias === 0 && (
        <TPanel>
          <p className="text-body text-txt-secondary">
            Nenhum dia registrado para <strong>{dados.pessoaNome}</strong> no período. Ajuste as
            datas ou registre o dia de hoje.
          </p>
        </TPanel>
      )}

      {dados && !carregando && dados.totalDias > 0 && (
        <>
          <TTabs abas={abas} ativa={aba} onChange={setAba} />

          <TPanel>
            {/* ───────────────────────── Resumo ─────────────────────────── */}
            <TTabPanel id={ABA_RESUMO} ativa={aba} rotulo="Resumo">
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <Indicador rotulo="Dias registrados" valor={String(dados.totalDias)} />
                <Indicador
                  rotulo={
                    dados.pesoInicialKg != null && dados.pesoFinalKg != null
                      ? `Variação de peso · de ${formatarNumero(dados.pesoInicialKg, 3)} a ${formatarNumero(dados.pesoFinalKg, 3)} kg`
                      : 'Variação de peso'
                  }
                  valor={
                    dados.variacaoPesoKg != null
                      ? `${dados.variacaoPesoKg > 0 ? '+' : ''}${formatarNumero(dados.variacaoPesoKg, 3)} kg`
                      : '—'
                  }
                />
                <Indicador
                  rotulo="Do prescrito, em média"
                  valor={dados.adesaoMedia != null ? `${formatarNumero(dados.adesaoMedia)} %` : '—'}
                />
                <Indicador
                  rotulo={
                    dados.vet != null
                      ? `Do VET, em média · meta de ${formatarNumero(dados.vet, 0)} kcal/dia`
                      : 'Do VET, em média · sem avaliação vinculada'
                  }
                  valor={
                    dados.adequacaoCaloricaMedia != null
                      ? `${formatarNumero(dados.adequacaoCaloricaMedia)} %`
                      : '—'
                  }
                />
                <Indicador
                  rotulo="Energia por quilo"
                  valor={
                    dados.caloriasPorKgMedia != null
                      ? `${formatarNumero(dados.caloriasPorKgMedia)} kcal/kg`
                      : '—'
                  }
                />
                <Indicador
                  rotulo="Proteína por quilo"
                  valor={
                    dados.proteinaPorKgMedia != null
                      ? `${formatarNumero(dados.proteinaPorKgMedia, 2)} g/kg`
                      : '—'
                  }
                />
                <Indicador
                  rotulo="Tomadas aceitas"
                  valor={
                    dados.aceitacaoTomadasMedia != null
                      ? `${formatarNumero(dados.aceitacaoTomadasMedia)} %`
                      : '—'
                  }
                />
                <Indicador
                  rotulo={
                    dados.diasSemAvaliacao > 0
                      ? 'Dias sem avaliação · neles não há adequação'
                      : 'Dias sem avaliação'
                  }
                  valor={String(dados.diasSemAvaliacao)}
                />
              </div>

              <p className="mt-4 text-caption text-txt-muted">
                As médias ignoram o dia em que o valor não existe — contar ausência como zero faria
                a criança parecer pior do que está. A adequação{' '}
                <strong>não é nota de desempenho</strong>: ela depende do dia da internação e da
                conduta.
              </p>
            </TTabPanel>

            {/* ─────────────────────── Crescimento ──────────────────────── */}
            <TTabPanel id={ABA_CRESCIMENTO} ativa={aba} rotulo="Crescimento">
              <div className="grid gap-6 lg:grid-cols-2">
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="pesoKg"
                  titulo="Peso medido"
                  casas={3}
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                  vazio="Nenhum dia com peso medido no período."
                />
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="estaturaCm"
                  titulo="Estatura medida"
                  casas={1}
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                  vazio="Nenhum dia com estatura medida no período."
                />
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="imc"
                  titulo="IMC"
                  casas={2}
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                />
              </div>

              <p className="mt-4 text-caption text-txt-muted">
                A classificação de cada dia usa a curva da OMS para a idade{' '}
                <strong>daquele dia</strong> — e a idade anda durante a internação. Para ver a
                criança sobre a curva inteira, use o painel do paciente. O sistema{' '}
                <strong>não</strong> classifica se o ganho de peso é adequado: a velocidade esperada
                por idade não está na fonte que a pediatria usa.
              </p>
            </TTabPanel>

            {/* ──────────────────── Oferta e adequação ──────────────────── */}
            <TTabPanel id={ABA_OFERTA} ativa={aba} rotulo="Oferta e adequação">
              <AdesaoNoTempo
                dados={pontos}
                chaveX="data"
                chaveAdesao="percentualRecebido"
                rotuloX={rotuloDoDia}
                formatarX={formatarData}
              />

              <div className="mt-6 grid gap-6 lg:grid-cols-2">
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="adequacaoCalorica"
                  titulo="Adequação calórica (% do VET)"
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                  vazio="Nenhum dia com avaliação vinculada: sem meta não há adequação."
                />
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="adequacaoProteica"
                  titulo="Adequação proteica (% da necessidade)"
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                  vazio="Nenhum dia com avaliação vinculada: sem meta não há adequação."
                />
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="caloriasPorKg"
                  titulo="Energia por quilo (kcal/kg)"
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                />
                <SerieNoTempo
                  dados={pontos}
                  chaveX="data"
                  chaveY="aceitacaoTomadas"
                  titulo="Tomadas aceitas (%)"
                  rotuloX={rotuloDoDia}
                  formatarX={formatarData}
                />
              </div>
            </TTabPanel>

            {/* ───────────────────────── Tabela ─────────────────────────── */}
            <TTabPanel id={ABA_TABELA} ativa={aba} rotulo="Tabela">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[46rem] text-body">
                  <thead>
                    <tr className="border-b border-line text-caption text-txt-secondary">
                      <th className="px-2 py-2 text-left">Data</th>
                      <th className="px-2 py-2 text-left">Idade</th>
                      <th className="px-2 py-2 text-right">Peso</th>
                      <th className="px-2 py-2 text-left">Peso/idade</th>
                      <th className="px-2 py-2 text-right">Recebido</th>
                      <th className="px-2 py-2 text-right">Do prescrito</th>
                      <th className="px-2 py-2 text-right">Do VET</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dados.dias.map((d) => (
                      <tr key={d.id} className="border-b border-line/60">
                        <td className="px-2 py-2">{formatarData(d.data)}</td>
                        <td className="px-2 py-2 text-txt-secondary">
                          {idadeEmMesesTexto(d.derivados.idadeMeses)}
                        </td>
                        <td className="px-2 py-2 text-right">
                          {d.pesoKg != null ? `${formatarNumero(d.pesoKg, 3)} kg` : '—'}
                        </td>
                        <td className="px-2 py-2 text-txt-secondary">
                          {d.derivados.pesoIdade?.rotulo ?? '—'}
                        </td>
                        <td className="px-2 py-2 text-right">
                          {d.volRecebido24h != null ? `${formatarNumero(d.volRecebido24h, 0)} ml` : '—'}
                        </td>
                        <td className="px-2 py-2 text-right">
                          {d.derivados.percentualRecebido != null
                            ? `${formatarNumero(d.derivados.percentualRecebido)} %`
                            : '—'}
                        </td>
                        <td className="px-2 py-2 text-right">
                          {d.derivados.adequacaoCalorica != null
                            ? `${formatarNumero(d.derivados.adequacaoCalorica)} %`
                            : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </TTabPanel>
          </TPanel>
        </>
      )}
    </TPage>
  )
}
