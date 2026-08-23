import { useEffect, useState } from 'react'
import {
  TEntry,
  TPanel,
  TResult,
  TResultGroup,
  TResultTable,
  TSelect,
  TTabPanel,
  TTabs,
  type Aba,
  type ColunaResultado,
} from '@/components/common'
import {
  entradasVazias,
  paraRequisicao,
  type EntradasUti,
} from '@/components/uti/entradas'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { calculoUtiService, formulaEnteralService } from '@/services/utiService'
import {
  OPCOES_ETNIA,
  OPCOES_FASE,
  OPCOES_JANELA_PERDA,
  OPCOES_MODO_INFUSAO,
  OPCOES_ORIGEM_PESO,
  OPCOES_POPULACAO,
  OPCOES_SEGMENTO,
  OPCOES_SEXO,
  OPCOES_TERAPIA_RENAL,
  rotuloFormulaEnteral,
  type DegrauProgressao,
  type FormulaEnteralSelect,
  type FracaoAgua,
  type ResultadoUti,
} from '@/types/uti'
import { formatarNumero } from '@/utils/format'

interface Props {
  entradas: EntradasUti
  onChange: (entradas: EntradasUti) => void
  /**
   * Resultado já conhecido — o de uma avaliação salva. Enquanto o usuário não
   * mexe em nada, é ele que aparece; ao primeiro toque, o recálculo assume.
   */
  resultadoInicial?: ResultadoUti | null
  /** Conteúdo de uma aba extra, ao fim. A avaliação usa para as observações. */
  abaExtra?: { id: string; rotulo: string; conteudo: React.ReactNode }
}

const ABA_ANTROPOMETRIA = 'antropometria'
const ABA_NECESSIDADES = 'necessidades'
const ABA_DIETA = 'dieta'
const ABA_HIDRATACAO = 'hidratacao'

/**
 * O miolo das telas de cálculo da UTI adulto, em quatro abas.
 *
 * É o mesmo componente na calculadora e na avaliação — a diferença entre elas é
 * o que existe em volta (paciente, data, gravação), não a conta. Duas cópias
 * divergiriam, e no eroERP já divergiam.
 *
 * **Cada aba é autocontida**: poucas entradas, uma régua, e os resultados
 * daquelas entradas logo abaixo. Nenhuma entrada aparece em duas abas — o que
 * corre entre elas são os **resultados**, exibidos como faixa de dependência
 * somente-leitura no topo (doc 04 §7).
 *
 * **Nenhuma fórmula é calculada aqui.** As entradas vão para o `/uti/calculo`
 * com 500 ms de debounce e o resultado volta pronto, já com a origem de cada
 * valor por escrito.
 */
export function CalculoUti({ entradas, onChange, resultadoInicial, abaExtra }: Props) {
  const [aba, setAba] = useState(ABA_ANTROPOMETRIA)
  const [formulas, setFormulas] = useState<FormulaEnteralSelect[]>([])
  const [resultado, setResultado] = useState<ResultadoUti | null>(resultadoInicial ?? null)
  const [recalculando, setRecalculando] = useState(false)

  const entradasDebounce = useDebounce(JSON.stringify(entradas), 500)

  useEffect(() => {
    formulaEnteralService.select().then(setFormulas).catch(handleApiError)
  }, [])

  useEffect(() => {
    setResultado(resultadoInicial ?? null)
  }, [resultadoInicial])

  useEffect(() => {
    const atual: EntradasUti = JSON.parse(entradasDebounce)

    if (entradasVazias(atual)) {
      setResultado(null)
      return
    }

    // Resposta fora de ordem sobrescreveria um resultado mais novo por um mais
    // velho — a flag descarta o que chegou tarde.
    let cancelado = false
    setRecalculando(true)

    calculoUtiService
      .calcular(paraRequisicao(atual))
      .then((r) => {
        if (!cancelado) setResultado(r)
      })
      .catch((erro) => {
        if (!cancelado) handleApiError(erro)
      })
      .finally(() => {
        if (!cancelado) setRecalculando(false)
      })

    return () => {
      cancelado = true
    }
  }, [entradasDebounce])

  function alterar<K extends keyof EntradasUti>(campo: K, valor: EntradasUti[K]) {
    onChange({ ...entradas, [campo]: valor })
  }

  function alternarSegmento(segmento: string) {
    const atuais = entradas.segmentosAmputados
    alterar(
      'segmentosAmputados',
      atuais.includes(segmento) ? atuais.filter((s) => s !== segmento) : [...atuais, segmento],
    )
  }

  const antro = resultado?.antropometria
  const nec = resultado?.necessidades
  const dieta = resultado?.dieta
  const hidra = resultado?.hidratacao

  const abas: Aba[] = [
    { id: ABA_ANTROPOMETRIA, rotulo: 'Antropometria' },
    { id: ABA_NECESSIDADES, rotulo: 'Necessidades' },
    { id: ABA_DIETA, rotulo: 'Dieta enteral' },
    { id: ABA_HIDRATACAO, rotulo: 'Hidratação' },
    ...(abaExtra ? [{ id: abaExtra.id, rotulo: abaExtra.rotulo }] : []),
  ]

  /** A faixa de dependência: o que esta aba recebeu de outra, e de onde. */
  function dependencias(itens: React.ReactNode) {
    return (
      <div className="mb-5 flex flex-col gap-1 rounded-md border border-line bg-surface-alt px-3 py-2.5">
        {itens}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <TTabs abas={abas} ativa={aba} onChange={setAba} />

      <TPanel>
        {/* ───────────────────────── Antropometria ───────────────────── */}
        <TTabPanel id={ABA_ANTROPOMETRIA} ativa={aba}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TSelect
              label="Sexo"
              vazio="Não informado"
              opcoes={OPCOES_SEXO.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              value={entradas.sexo}
              onChange={(e) => alterar('sexo', e.target.value)}
            />
            <TSelect
              label="Etnia"
              vazio="Não informada"
              opcoes={OPCOES_ETNIA.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              ajuda="Só a equação de Chumlea 1988 a usa."
              value={entradas.etnia}
              onChange={(e) => alterar('etnia', e.target.value)}
            />
            <TEntry
              label="Idade"
              suffix="anos"
              inputMode="numeric"
              value={entradas.idadeAnos}
              onChange={(e) => alterar('idadeAnos', e.target.value)}
            />
            <TEntry
              label="Altura"
              suffix="cm"
              inputMode="decimal"
              ajuda="Em branco, estimamos pela altura do joelho."
              value={entradas.alturaCm}
              onChange={(e) => alterar('alturaCm', e.target.value)}
            />

            <TEntry
              label="Peso atual"
              suffix="kg"
              inputMode="decimal"
              ajuda="Em branco, estimamos pelas circunferências."
              value={entradas.pesoAtualKg}
              onChange={(e) => alterar('pesoAtualKg', e.target.value)}
            />
            <TEntry
              label="Altura do joelho"
              suffix="cm"
              inputMode="decimal"
              value={entradas.alturaJoelhoCm}
              onChange={(e) => alterar('alturaJoelhoCm', e.target.value)}
            />
            <TEntry
              label="Circunferência do braço"
              suffix="cm"
              inputMode="decimal"
              value={entradas.circBracoCm}
              onChange={(e) => alterar('circBracoCm', e.target.value)}
            />
            <TEntry
              label="Circunferência da panturrilha"
              suffix="cm"
              inputMode="decimal"
              value={entradas.circPanturrilhaCm}
              onChange={(e) => alterar('circPanturrilhaCm', e.target.value)}
            />
            <TEntry
              label="Circunferência abdominal"
              suffix="cm"
              inputMode="decimal"
              value={entradas.circAbdominalCm}
              onChange={(e) => alterar('circAbdominalCm', e.target.value)}
            />

            <TEntry
              label="Peso habitual"
              suffix="kg"
              inputMode="decimal"
              value={entradas.pesoUsualKg}
              onChange={(e) => alterar('pesoUsualKg', e.target.value)}
            />
            <TSelect
              label="Perdido em"
              vazio="Escolha a janela"
              opcoes={OPCOES_JANELA_PERDA.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              ajuda="5 % em uma semana é grave; em seis meses, moderado."
              value={entradas.janelaPerda}
              onChange={(e) => alterar('janelaPerda', e.target.value)}
            />
            <TSelect
              label="Peso a usar no cálculo"
              vazio="Escolher automaticamente"
              opcoes={OPCOES_ORIGEM_PESO.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              ajuda="Automático usa o informado; sem ele, a primeira estimativa possível."
              value={entradas.origemPesoPreferida}
              onChange={(e) => alterar('origemPesoPreferida', e.target.value)}
            />

            {/*
              A coluna de ajuste só aparece quando muda alguma coisa. Acima de
              IMC 18,5 as duas são idênticas, e exibir um controle que não altera
              o resultado seria mentir sobre a interface. Ver docs/10 §2.9.
            */}
            {antro?.ajustePeloImcRelevante && (
              <TSelect
                label="Referência de ajuste"
                opcoes={OPCOES_POPULACAO.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
                ajuda="Em IMC abaixo de 18,5 as duas colunas divergem. O padrão clínico não soma centímetro, para não mascarar depleção."
                value={entradas.populacaoReferencia || 'POPULACAO_CLINICA'}
                onChange={(e) => alterar('populacaoReferencia', e.target.value)}
              />
            )}
          </div>

          <fieldset className="mt-4">
            <legend className="text-caption text-txt-secondary">Segmentos amputados</legend>
            <p className="text-caption mb-2 text-txt-muted">
              Os segmentos se contêm: marcar &quot;Membro superior&quot; e &quot;Mão&quot; junto é
              recusado, porque descontaria a mão duas vezes.
            </p>
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              {OPCOES_SEGMENTO.map((s) => (
                <label key={s.valor} className="flex items-center gap-2 text-body text-txt">
                  <input
                    type="checkbox"
                    className="size-4 accent-primary"
                    checked={entradas.segmentosAmputados.includes(s.valor)}
                    onChange={() => alternarSegmento(s.valor)}
                  />
                  {s.rotulo}
                </label>
              ))}
            </div>
          </fieldset>

          <hr className="my-5 border-line" />

          <div className="flex flex-col gap-6">
            <TResultGroup
              titulo="Estimativas"
              descricao="Para quem não pode ser medido nem pesado de pé."
              colunas={4}
            >
              <TResult
                label="Altura estimada"
                valor={formatarNumero(antro?.alturaEstimadaCm)}
                unidade="cm"
                referencia="Chumlea 1985"
                motivoAusencia={antro?.motivoEstimativas}
                recalculando={recalculando}
              />
              <TResult
                label="Peso — Chumlea 1988"
                valor={formatarNumero(antro?.pesoChumleaKg)}
                unidade="kg"
                referencia="altura do joelho e braço"
                recalculando={recalculando}
              />
              <TResult
                label="Peso — Jung 2004"
                valor={formatarNumero(antro?.pesoJungKg)}
                unidade="kg"
                recalculando={recalculando}
              />
              <TResult
                label="Peso — Rabito 2008"
                valor={formatarNumero(antro?.pesoRabitoKg)}
                unidade="kg"
                referencia="três circunferências"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultGroup
              titulo="Diagnóstico"
              descricao="O peso e a altura que todo o resto do cálculo usa."
            >
              <TResult
                label="Peso considerado"
                valor={formatarNumero(antro?.pesoDeTrabalhoKg)}
                unidade="kg"
                referencia={antro?.pesoDeTrabalhoOrigem}
                motivoAusencia={antro?.motivoPesoDeTrabalho}
                recalculando={recalculando}
              />
              <TResult
                label="Altura considerada"
                valor={formatarNumero(antro?.alturaUsadaCm)}
                unidade="cm"
                referencia={antro?.alturaUsadaOrigem}
                recalculando={recalculando}
              />
              <TResult
                label="IMC"
                valor={formatarNumero(antro?.imc)}
                unidade="kg/m²"
                motivoAusencia={antro?.motivoImc}
                recalculando={recalculando}
              />
              <TResult
                label="Classificação — OMS 1997"
                valor={formatarNumero(antro?.imc)}
                unidade="kg/m²"
                classificacao={antro?.classificacaoImcOms}
                recalculando={recalculando}
              />
              <TResult
                label="Classificação — OPAS 2002 (idoso)"
                valor={formatarNumero(antro?.imc)}
                unidade="kg/m²"
                classificacao={antro?.classificacaoImcOpas}
                referencia="quatro faixas, estudo SABE"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultGroup titulo="Metas de peso" colunas={4}>
              <TResult
                label="Peso ideal"
                valor={formatarNumero(antro?.pesoIdealKg)}
                unidade="kg"
                referencia="IMC 22 (H) · 20,8 (M)"
                recalculando={recalculando}
              />
              <TResult
                label="Peso ideal — IMC 25"
                valor={formatarNumero(antro?.pesoIdealImc25Kg)}
                unidade="kg"
                recalculando={recalculando}
              />
              <TResult
                label="Peso ajustado"
                valor={formatarNumero(antro?.pesoAjustadoKg)}
                unidade="kg"
                referencia="33 % da diferença"
                recalculando={recalculando}
              />
              <TResult
                label="Corrigido por amputação"
                valor={formatarNumero(antro?.pesoCorrigidoAmputacaoKg)}
                unidade="kg"
                referencia="Osterkamp 1995 · referência, não substitui o peso considerado"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultGroup titulo="Perda de peso e depleção muscular">
              <TResult
                label="Perda de peso"
                valor={formatarNumero(antro?.percentualPerdaPeso)}
                unidade="%"
                classificacao={antro?.classificacaoPerdaPeso}
                referencia="Blackburn 1977"
                motivoAusencia={antro?.motivoPerdaPeso}
                recalculando={recalculando}
              />
              <TResult
                label="Adequação da CB"
                valor={formatarNumero(antro?.adequacaoCircBracoPerc)}
                unidade="%"
                classificacao={antro?.classificacaoAdequacaoCircBraco}
                referencia={
                  antro?.p50CircBracoCm
                    ? `P50 da faixa etária: ${formatarNumero(antro.p50CircBracoCm)} cm`
                    : undefined
                }
                motivoAusencia={antro?.motivoAdequacaoCircBraco}
                recalculando={recalculando}
              />
              <TResult
                label="Massa muscular pelo braço"
                valor={formatarNumero(antro?.circBracoAjustadaCm)}
                unidade="cm ajustados"
                classificacao={antro?.classificacaoMassaMuscularBraco}
                referencia={`CB ajustada pelo IMC · ${antro?.populacaoReferenciaUsada ?? ''}`}
                motivoAusencia={antro?.motivoDeplecao}
                recalculando={recalculando}
              />
              <TResult
                label="Depleção pela panturrilha"
                valor={formatarNumero(antro?.circPanturrilhaAjustadaCm)}
                unidade="cm ajustados"
                classificacao={antro?.classificacaoDeplecaoPanturrilha}
                referencia="CP ajustada pelo IMC · Gonzalez 2021"
                recalculando={recalculando}
              />
            </TResultGroup>
          </div>
        </TTabPanel>

        {/* ───────────────────────── Necessidades ────────────────────── */}
        <TTabPanel id={ABA_NECESSIDADES} ativa={aba}>
          {dependencias(
            <>
              <TResult
                compacto
                label="Peso considerado"
                valor={formatarNumero(antro?.pesoDeTrabalhoKg)}
                unidade="kg"
                referencia={antro?.pesoDeTrabalhoOrigem}
                motivoAusencia={antro?.motivoPesoDeTrabalho}
                recalculando={recalculando}
              />
              <TResult
                compacto
                label="IMC"
                valor={formatarNumero(antro?.imc)}
                unidade="kg/m²"
                referencia={antro?.classificacaoImcOms?.rotulo}
                motivoAusencia={antro?.motivoImc}
                recalculando={recalculando}
              />
            </>,
          )}

          {nec?.obeso && (
            <p className="text-caption mb-4 rounded-md border border-warning/40 bg-warning-bg px-3 py-2 text-warning">
              Obesidade · IMC {formatarNumero(antro?.imc)} — <strong>a fase da terapia não se
              aplica</strong>. As metas vêm do protocolo de obesidade
              {nec.baseDoPeso ? `: ${nec.baseDoPeso}.` : '.'}
            </p>
          )}

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TSelect
              label="Fase da terapia"
              vazio="Não informada"
              opcoes={OPCOES_FASE.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              value={entradas.fase}
              onChange={(e) => alterar('fase', e.target.value)}
            />
            <TSelect
              label="Terapia renal substitutiva"
              vazio="Nenhuma"
              opcoes={OPCOES_TERAPIA_RENAL.filter((o) => o.valor !== 'NENHUMA').map((o) => ({
                valor: o.valor,
                rotulo: o.rotulo,
              }))}
              ajuda="Quando presente, substitui a meta proteica."
              value={entradas.terapiaRenal}
              onChange={(e) => alterar('terapiaRenal', e.target.value)}
            />
            <TEntry
              label="Alvo calórico"
              suffix="kcal/kg"
              inputMode="decimal"
              ajuda="Preenchido, vence a faixa da fase."
              value={entradas.kcalPorKgAlvo}
              onChange={(e) => alterar('kcalPorKgAlvo', e.target.value)}
            />
            <TEntry
              label="Alvo proteico"
              suffix="g/kg"
              inputMode="decimal"
              value={entradas.proteinaPorKgAlvo}
              onChange={(e) => alterar('proteinaPorKgAlvo', e.target.value)}
            />
          </div>

          <hr className="my-5 border-line" />

          <div className="flex flex-col gap-6">
            <TResultGroup titulo="Faixa recomendada" colunas={4}>
              <TResult
                label="Energia — mínimo"
                valor={formatarNumero(nec?.energiaMinima)}
                unidade="kcal/dia"
                motivoAusencia={nec?.motivo}
                recalculando={recalculando}
              />
              <TResult
                label="Energia — máximo"
                valor={formatarNumero(nec?.energiaMaxima)}
                unidade="kcal/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Proteína — mínimo"
                valor={formatarNumero(nec?.proteinaMinima)}
                unidade="g/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Proteína — máximo"
                valor={formatarNumero(nec?.proteinaMaxima)}
                unidade="g/dia"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultGroup
              titulo="Meta que desce para a dieta"
              descricao="É esta que a aba da dieta persegue — e ela leva a procedência junto."
            >
              <TResult
                label="Meta energética"
                valor={formatarNumero(nec?.metaEnergetica)}
                unidade="kcal/dia"
                referencia={nec?.metaEnergeticaOrigem}
                recalculando={recalculando}
              />
              <TResult
                label="Meta proteica"
                valor={formatarNumero(nec?.metaProteica)}
                unidade="g/dia"
                referencia={nec?.metaProteicaOrigem}
                recalculando={recalculando}
              />
              <TResult
                label="Proteína na terapia renal"
                valor={formatarNumero(nec?.proteinaTerapiaRenal)}
                unidade="g/dia"
                referencia="substitui a faixa da fase"
                recalculando={recalculando}
              />
            </TResultGroup>
          </div>
        </TTabPanel>

        {/* ───────────────────────── Dieta enteral ───────────────────── */}
        <TTabPanel id={ABA_DIETA} ativa={aba}>
          {dependencias(
            <>
              <TResult
                compacto
                label="Peso considerado"
                valor={formatarNumero(antro?.pesoDeTrabalhoKg)}
                unidade="kg"
                referencia={antro?.pesoDeTrabalhoOrigem}
                motivoAusencia={antro?.motivoPesoDeTrabalho}
                recalculando={recalculando}
              />
              <TResult
                compacto
                label="Meta energética"
                valor={formatarNumero(nec?.metaEnergetica)}
                unidade="kcal/dia"
                referencia={nec?.metaEnergeticaOrigem}
                motivoAusencia={nec?.motivo}
                recalculando={recalculando}
              />
              <TResult
                compacto
                label="Meta proteica"
                valor={formatarNumero(nec?.metaProteica)}
                unidade="g/dia"
                referencia={nec?.metaProteicaOrigem}
                recalculando={recalculando}
              />
            </>,
          )}

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TSelect
              label="Fórmula enteral"
              vazio="Escolha a fórmula"
              className="lg:col-span-2"
              opcoes={formulas.map((f) => ({ valor: f.id, rotulo: rotuloFormulaEnteral(f) }))}
              value={entradas.formulaEnteralId}
              onChange={(e) => alterar('formulaEnteralId', e.target.value)}
            />
            <TSelect
              label="Modo de infusão"
              opcoes={OPCOES_MODO_INFUSAO.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              value={entradas.modoInfusao}
              onChange={(e) => alterar('modoInfusao', e.target.value)}
            />
            <TEntry
              label={entradas.modoInfusao === 'INTERMITENTE' ? 'Volume por horário' : 'Vazão'}
              suffix={entradas.modoInfusao === 'INTERMITENTE' ? 'ml' : 'ml/h'}
              inputMode="decimal"
              value={entradas.volumePorTempo}
              onChange={(e) => alterar('volumePorTempo', e.target.value)}
            />
            <TEntry
              label={entradas.modoInfusao === 'INTERMITENTE' ? 'Horários por dia' : 'Horas de infusão'}
              suffix={entradas.modoInfusao === 'INTERMITENTE' ? 'horários' : 'h'}
              inputMode="decimal"
              ajuda={entradas.modoInfusao === 'CONTINUA' ? 'UTI: 22 h/dia' : undefined}
              value={entradas.tempo}
              onChange={(e) => alterar('tempo', e.target.value)}
            />
          </div>

          <hr className="my-5 border-line" />

          <div className="flex flex-col gap-6">
            <TResultGroup titulo="O que a prescrição entrega" colunas={4}>
              <TResult
                label="Volume total"
                valor={formatarNumero(dieta?.volumeTotalMl)}
                unidade="ml/dia"
                referencia={dieta?.volumeTotalDescricao}
                motivoAusencia={dieta?.motivo}
                recalculando={recalculando}
              />
              <TResult
                label="Calorias"
                valor={formatarNumero(dieta?.caloriasOfertadas)}
                unidade="kcal/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Proteína"
                valor={formatarNumero(dieta?.proteinaOfertada)}
                unidade="g/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Calorias por quilo"
                valor={formatarNumero(dieta?.caloriasPorQuilo)}
                unidade="kcal/kg"
                recalculando={recalculando}
              />
              <TResult
                label="Proteína por quilo"
                valor={formatarNumero(dieta?.proteinaPorQuilo)}
                unidade="g/kg"
                recalculando={recalculando}
              />
              <TResult
                label="% da meta calórica"
                valor={formatarNumero(dieta?.percentualDoVct)}
                unidade="%"
                recalculando={recalculando}
              />
              <TResult
                label="% da meta proteica"
                valor={formatarNumero(dieta?.percentualDaProteina)}
                unidade="%"
                recalculando={recalculando}
              />
              <TResult
                label="Proteína ainda em falta"
                valor={formatarNumero(dieta?.proteinaSuplementar)}
                unidade="g/dia"
                referencia="zero quando a meta é atingida"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultGroup titulo="Demais nutrientes ofertados" colunas={4}>
              <TResult
                label="Carboidrato"
                valor={formatarNumero(dieta?.choOfertado)}
                unidade="g/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Lipídio"
                valor={formatarNumero(dieta?.lipOfertado)}
                unidade="g/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Fibras"
                valor={formatarNumero(dieta?.fibrasOfertadas)}
                unidade="g/dia"
                recalculando={recalculando}
              />
              <TResult
                label="Potássio"
                valor={formatarNumero(dieta?.potassioOfertado)}
                unidade="mg/dia"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultGroup titulo="Se a meta fosse atingida" colunas={2}>
              <TResult
                label="Volume pleno"
                valor={formatarNumero(dieta?.volumePleno)}
                unidade={dieta?.unidadeDoVolume}
                referencia="a vazão que entregaria a meta calórica inteira"
                recalculando={recalculando}
              />
              <TResult
                label="Proteína no volume pleno"
                valor={formatarNumero(dieta?.proteinaNoVolumePleno)}
                unidade="g/dia"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultTable
              titulo="Progressão"
              descricao="25, 50, 75 e 100 % da meta nos primeiros quatro dias."
              colunas={COLUNAS_PROGRESSAO(dieta?.unidadeDoVolume)}
              linhas={dieta?.progressao ?? []}
              chaveDe={(d) => d.dia}
              recalculando={recalculando}
              vazio="Escolha a fórmula e informe o tempo para ver a progressão."
            />
          </div>
        </TTabPanel>

        {/* ───────────────────────── Hidratação ──────────────────────── */}
        <TTabPanel id={ABA_HIDRATACAO} ativa={aba}>
          {dependencias(
            <>
              <TResult
                compacto
                label="Peso considerado"
                valor={formatarNumero(antro?.pesoDeTrabalhoKg)}
                unidade="kg"
                referencia={antro?.pesoDeTrabalhoOrigem}
                motivoAusencia={antro?.motivoPesoDeTrabalho}
                recalculando={recalculando}
              />
              <TResult
                compacto
                label="Volume da dieta"
                valor={formatarNumero(hidra?.volumeDietaConsiderado)}
                unidade="ml/dia"
                referencia={dieta?.formulaNome}
                recalculando={recalculando}
              />
              <TResult
                compacto
                label="Água na fórmula"
                valor={formatarNumero(hidra?.percentualAgua)}
                unidade="%"
                referencia={hidra?.percentualAguaOrigem}
                motivoAusencia={hidra?.motivo}
                recalculando={recalculando}
              />
            </>,
          )}

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Volume de dieta"
              suffix="ml/dia"
              inputMode="decimal"
              ajuda="Só se você não preencheu a aba da dieta. O da dieta tem prioridade."
              value={entradas.volumeDietaManualMl}
              onChange={(e) => alterar('volumeDietaManualMl', e.target.value)}
            />
          </div>

          <hr className="my-5 border-line" />

          <div className="flex flex-col gap-6">
            <TResultGroup titulo="Necessidade hídrica" descricao="25 a 30 ml/kg/dia." colunas={4}>
              <TResult
                label="Mínima"
                valor={formatarNumero(hidra?.necessidadeMinima)}
                unidade="ml/dia"
                referencia="25 ml/kg"
                motivoAusencia={hidra?.motivo}
                recalculando={recalculando}
              />
              <TResult
                label="Ideal"
                valor={formatarNumero(hidra?.necessidadeIdeal)}
                unidade="ml/dia"
                referencia="30 ml/kg"
                recalculando={recalculando}
              />
              <TResult
                label="Água que a dieta já entrega"
                valor={formatarNumero(hidra?.aguaNaDieta)}
                unidade="ml/dia"
                referencia={hidra?.percentualAguaOrigem}
                recalculando={recalculando}
              />
              <TResult
                label="Água extra — ideal"
                valor={formatarNumero(hidra?.aguaExtraIdeal)}
                unidade="ml/dia"
                referencia="descontada a da dieta"
                recalculando={recalculando}
              />
            </TResultGroup>

            <TResultTable
              titulo="Como fracionar a água extra"
              descricao="Do volume ideal, ao longo do dia."
              colunas={COLUNAS_FRACAO}
              linhas={hidra?.distribuicaoIdeal ?? []}
              chaveDe={(f) => f.vezesAoDia}
              recalculando={recalculando}
              vazio="Informe o peso e a dieta para calcular a água extra."
            />
          </div>
        </TTabPanel>

        {abaExtra && (
          <TTabPanel id={abaExtra.id} ativa={aba}>
            {abaExtra.conteudo}
          </TTabPanel>
        )}
      </TPanel>

      <p className="text-caption text-txt-muted">
        Os cálculos são refeitos automaticamente ao alterar os campos. O peso, a altura e as metas
        mostram de onde vieram.
      </p>
    </div>
  )
}

const COLUNAS_PROGRESSAO = (unidade?: string): ColunaResultado<DegrauProgressao>[] => [
  { chave: 'dia', cabecalho: 'Dia', render: (d) => `Dia ${d.dia}` },
  {
    chave: 'pct',
    cabecalho: '% da meta',
    numerica: true,
    render: (d) => `${formatarNumero(d.percentual, 0)} %`,
  },
  {
    chave: 'kcal',
    cabecalho: 'kcal/dia',
    numerica: true,
    render: (d) => formatarNumero(d.kcal),
  },
  {
    chave: 'volume',
    cabecalho: unidade ?? 'volume',
    numerica: true,
    render: (d) => formatarNumero(d.volume),
  },
]

const COLUNAS_FRACAO: ColunaResultado<FracaoAgua>[] = [
  { chave: 'vezes', cabecalho: 'Vezes ao dia', render: (f) => `${f.vezesAoDia}×` },
  {
    chave: 'ml',
    cabecalho: 'ml por vez',
    numerica: true,
    render: (f) => formatarNumero(f.mlPorVez),
  },
]
