import { useEffect, useRef, useState } from 'react'
import {
  TAviso,
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
import axios from 'axios'
import { handleApiError } from '@/services/api'
import {
  calculoUtiService,
  formulaEnteralService,
  produtoNutricionalService,
} from '@/services/utiService'
import {
  OPCOES_ETNIA,
  OPCOES_FASE,
  OPCOES_JANELA_PERDA,
  OPCOES_MODO_INFUSAO,
  OPCOES_ORIGEM_PESO,
  OPCOES_POPULACAO,
  OPCOES_POSICAO_FAIXA,
  OPCOES_SEGMENTO,
  OPCOES_SEXO,
  OPCOES_TERAPIA_RENAL,
  rotuloFormulaEnteral,
  rotuloProduto,
  type DegrauProgressao,
  type FormulaEnteralSelect,
  type FracaoAgua,
  type ProdutoNutricionalSelect,
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
  /**
   * Espelha o resultado corrente para quem está em volta.
   *
   * Existe por causa da **impressão**: o documento é um prontuário montado a
   * partir do resultado, e o resultado nasce aqui dentro. Sem este espelho a
   * calculadora imprimiria as entradas e nenhum número.
   */
  onResultado?: (resultado: ResultadoUti | null) => void
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
export function CalculoUti({
  entradas,
  onChange,
  resultadoInicial,
  abaExtra,
  onResultado,
}: Props) {
  const [aba, setAba] = useState(ABA_ANTROPOMETRIA)
  const [formulas, setFormulas] = useState<FormulaEnteralSelect[]>([])
  const [modulos, setModulos] = useState<ProdutoNutricionalSelect[]>([])
  const [resultado, setResultado] = useState<ResultadoUti | null>(resultadoInicial ?? null)
  const [recalculando, setRecalculando] = useState(false)

  /**
   * O que a validação do servidor recusou desta vez.
   *
   * <p>Nesta tela um 400 <b>não é evento, é estado do formulário</b>: ela
   * recalcula a cada 500 ms e na maior parte do tempo está pela metade, então
   * medida incompleta é o caso normal, não a exceção. Mandar isso para o
   * `toast` empilha três avisos vermelhos enquanto se digita um único número —
   * a máscara de centavos passa por 0,05 · 0,53 · 5,30 antes de chegar a 53,00,
   * e cada pausa acima do debounce dispara um. Medido: 3 toasts para digitar
   * "5300". Aqui a recusa aparece uma vez, no lugar, e some sozinha quando o
   * número fica inteiro.
   */
  const [recusa, setRecusa] = useState<string | null>(null)

  /**
   * Enquanto true, a tela está mostrando o que o BANCO gravou e ninguém mexeu.
   * `useRef` e não `useState`: isto não pinta nada, só decide se o efeito de
   * recálculo desiste — e como ref não entra na lista de dependências, o efeito
   * continua disparando só quando as entradas mudam.
   */
  const mostrandoOGravado = useRef(false)

  const entradasDebounce = useDebounce(JSON.stringify(entradas), 500)

  useEffect(() => {
    formulaEnteralService.select().then(setFormulas).catch(handleApiError)
    // Do catálogo, não de lista fixa: o hospital cadastra o módulo dele e ele
    // aparece aqui no mesmo dia.
    produtoNutricionalService.modulosProteicos().then(setModulos).catch(handleApiError)
  }, [])

  // Espelha para fora, sem virar fonte da verdade: quem calcula continua sendo
  // o efeito de debounce abaixo.
  useEffect(() => {
    onResultado?.(resultado)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resultado])

  useEffect(() => {
    setResultado(resultadoInicial ?? null)
    if (resultadoInicial) mostrandoOGravado.current = true
  }, [resultadoInicial])

  useEffect(() => {
    const atual: EntradasUti = JSON.parse(entradasDebounce)

    if (entradasVazias(atual)) {
      setResultado(null)
      setRecusa(null)
      return
    }

    // ABRIR AVALIAÇÃO SALVA NÃO RECALCULA. Sem esta guarda, o debounce dispara
    // 500 ms depois de o registro carregar e substitui os números GRAVADOS
    // pelos que o código de hoje produz — em silêncio, sem salvar. Some no dia
    // a dia, porque os dois batem; aparece no dia em que o cálculo muda, e ele
    // mudou (obesidade pela ASPEN 2016). Um prontuário de seis meses atrás
    // passaria a exibir outra dose.
    if (mostrandoOGravado.current) return

    // Resposta fora de ordem sobrescreveria um resultado mais novo por um mais
    // velho — a flag descarta o que chegou tarde.
    let cancelado = false
    setRecalculando(true)

    calculoUtiService
      .calcular(paraRequisicao(atual))
      .then((r) => {
        if (!cancelado) {
          setResultado(r)
          setRecusa(null)
        }
      })
      .catch((erro) => {
        if (cancelado) return
        // 400 é entrada implausível — inline. Qualquer outra coisa é falha de
        // verdade (500, sessão, rede) e continua indo para o toast.
        const mensagem =
          axios.isAxiosError<{ erro?: string }>(erro) && erro.response?.status === 400
            ? (erro.response.data?.erro ?? null)
            : null
        if (mensagem) setRecusa(mensagem)
        else handleApiError(erro)
      })
      .finally(() => {
        if (!cancelado) setRecalculando(false)
      })

    return () => {
      cancelado = true
    }
  }, [entradasDebounce])

  function alterar<K extends keyof EntradasUti>(campo: K, valor: EntradasUti[K]) {
    mostrandoOGravado.current = false
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

  /*
   * Onde a posição na faixa ainda governa — e quem responde isso é o SERVIDOR.
   *
   * A versão anterior deduzia aqui, dos dois alvos digitados, e errava por
   * baixo: a proteína tem TRÊS caminhos que atropelam a posição (alvo digitado,
   * terapia renal e protocolo de obesidade), não um. O caso "alvo calórico com
   * obesidade" deixava o seletor habilitado sem efeito em nenhuma das duas
   * metas — controle que não faz nada, que é o que `docs/10` §2.9 proíbe.
   *
   * O fallback só vale antes do primeiro cálculo: sem `nec` não há decisão
   * publicada, e travar por dedução própria seria reimplementar a regra.
   */
  const alvoCalorico = entradas.kcalPorKgAlvo.trim() !== ''
  const alvoProteico = entradas.proteinaPorKgAlvo.trim() !== ''
  const valeEnergia = nec ? nec.posicaoValeParaEnergia : !alvoCalorico
  const valeProteina = nec ? nec.posicaoValeParaProteina : !alvoProteico

  /*
   * As duas frases do meio INTERPOLAM a procedência do servidor em vez de
   * repetir "o alvo digitado vence": assim continuam certas quando o vencedor
   * for a terapia renal ou a obesidade, que é o que a versão anterior não
   * previa. Literal ao lado de interpolação, no mesmo bloco, é o cheiro.
   */
  const ajudaPosicao =
    valeEnergia && valeProteina
      ? 'Onde fixar a meta dentro da faixa acima.'
      : valeEnergia
        ? `Vale só para a energia — a proteína vem de ${nec?.metaProteicaOrigem ?? 'outra regra'}.`
        : valeProteina
          ? `Vale só para a proteína — a energia vem de ${nec?.metaEnergeticaOrigem ?? 'outra regra'}.`
          : /*
             * Travado nomeia OS DOIS vencedores, não uma ação a tentar.
             *
             * A primeira versão dizia "limpe o alvo calórico ou o proteico" — e
             * no navegador, com o alvo proteico já limpo e hemodiálise
             * escolhida, ela continuava travada mandando limpar um campo vazio:
             * quem tomou a proteína ali é a diálise, não o alvo. Frase que
             * nomeia a ação errada é a mesma família de "culpar o dado que está
             * lá", que já custou um motivo inteiro nesta tela. Interpolando a
             * procedência do servidor ela não tem como envelhecer.
             */
            `Sem efeito: a energia vem de ${nec?.metaEnergeticaOrigem ?? 'outra regra'} e a ` +
            `proteína de ${nec?.metaProteicaOrigem ?? 'outra regra'} — nenhuma das duas é ponto de faixa.`

  const abas: Aba[] = [
    { id: ABA_ANTROPOMETRIA, rotulo: 'Antropometria' },
    { id: ABA_NECESSIDADES, rotulo: 'Necessidades' },
    { id: ABA_DIETA, rotulo: 'Dieta enteral' },
    { id: ABA_HIDRATACAO, rotulo: 'Hidratação' },
    ...(abaExtra ? [{ id: abaExtra.id, rotulo: abaExtra.rotulo }] : []),
  ]

  /**
   * A recusa do servidor, em frases legíveis.
   *
   * <p>O `GlobalExceptionHandler` prefixa cada erro com o nome do campo em Java
   * — `circPanturrilhaCm: ...` — e junta tudo com `; `. O prefixo desambigua
   * mensagem genérica ("No máximo 2 casas decimais") e por isso continua lá no
   * servidor; aqui ele é ruído, porque estas frases já nomeiam o campo por
   * extenso. Uma por linha lê melhor que três coladas.
   */
  const linhasDaRecusa = (recusa ?? '')
    .split('; ')
    .map((linha) => linha.replace(/^[A-Za-z][A-Za-z0-9]*:\s*/, ''))
    .filter(Boolean)

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
      {recusa && (
        /*
         * Acima das abas de propósito: o campo recusado pode estar em qualquer
         * uma delas, e o aviso tem de ser visto de onde quer que se esteja
         * digitando. `role="status"` e não `alert`: isto acompanha a digitação,
         * não interrompe.
         */
        <div
          role="status"
          className="flex flex-col gap-1 rounded-md border border-warning/40 bg-warning-bg
                     px-3 py-2.5 text-caption text-txt"
        >
          {linhasDaRecusa.map((linha) => (
            <span key={linha}>{linha}</span>
          ))}
        </div>
      )}

      <TTabs abas={abas} ativa={aba} onChange={setAba} />

      <TPanel>
        {/* ───────────────────────── Antropometria ───────────────────── */}
        <TTabPanel id={ABA_ANTROPOMETRIA} ativa={aba} rotulo="Antropometria">
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
              mascara="inteiro"
              value={entradas.idadeAnos}
              onChange={(e) => alterar('idadeAnos', e.target.value)}
            />
            <TEntry
              label="Altura"
              suffix="cm"
              mascara="decimal"
              placeholder="Ex.: 168,50"
              ajuda="Em branco, estimamos pela altura do joelho."
              value={entradas.alturaCm}
              onChange={(e) => alterar('alturaCm', e.target.value)}
            />

            <TEntry
              label="Peso atual"
              suffix="kg"
              mascara="decimal"
              placeholder="Ex.: 68,40"
              ajuda="Em branco, estimamos pelas circunferências."
              value={entradas.pesoAtualKg}
              onChange={(e) => alterar('pesoAtualKg', e.target.value)}
            />
            <TEntry
              label="Altura do joelho"
              suffix="cm"
              mascara="decimal"
              placeholder="Ex.: 53,50"
              value={entradas.alturaJoelhoCm}
              onChange={(e) => alterar('alturaJoelhoCm', e.target.value)}
            />
            <TEntry
              label="Circunferência do braço"
              suffix="cm"
              mascara="decimal"
              placeholder="Ex.: 25,50"
              value={entradas.circBracoCm}
              onChange={(e) => alterar('circBracoCm', e.target.value)}
            />
            <TEntry
              label="Circunferência da panturrilha"
              suffix="cm"
              mascara="decimal"
              placeholder="Ex.: 34,50"
              value={entradas.circPanturrilhaCm}
              onChange={(e) => alterar('circPanturrilhaCm', e.target.value)}
            />
            <TEntry
              label="Circunferência abdominal"
              suffix="cm"
              mascara="decimal"
              placeholder="Ex.: 90,50"
              value={entradas.circAbdominalCm}
              onChange={(e) => alterar('circAbdominalCm', e.target.value)}
            />

            <TEntry
              label="Peso habitual"
              suffix="kg"
              mascara="decimal"
              placeholder="Ex.: 72,50"
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
                /* O motivo, quando há um, vem antes da explicação geral: é ele
                 * que diz por que ESTA coluna, e não a outra (docs/10 §2.9). */
                ajuda={
                  antro?.motivoPopulacaoReferencia
                    ? `${antro.motivoPopulacaoReferencia}. Em IMC abaixo de 18,5 as duas colunas divergem.`
                    : 'Em IMC abaixo de 18,5 as duas colunas divergem. O padrão clínico não soma centímetro, para não mascarar depleção.'
                }
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
                motivoAusencia={antro?.motivoMassaMuscularBraco}
                recalculando={recalculando}
              />
              <TResult
                label="Depleção pela panturrilha"
                valor={formatarNumero(antro?.circPanturrilhaAjustadaCm)}
                unidade="cm ajustados"
                classificacao={antro?.classificacaoDeplecaoPanturrilha}
                /* A coluna usada, não uma fonte fixa: em IMC < 18,5 com
                 * população clínica a CP não recebe o +4, e o número deixa de
                 * ser o de Gonzalez 2021. Ver docs/10 §2.9. */
                referencia={`CP ajustada pelo IMC · ${antro?.populacaoReferenciaUsada ?? ''}`}
                motivoAusencia={antro?.motivoDeplecaoPanturrilha}
                recalculando={recalculando}
              />
            </TResultGroup>
          </div>
        </TTabPanel>

        {/* ───────────────────────── Necessidades ────────────────────── */}
        <TTabPanel id={ABA_NECESSIDADES} ativa={aba} rotulo="Necessidades">
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

          {/*
            O aviso de obesidade afirmava "As metas vêm do protocolo de
            obesidade" sempre que `obeso` — e com os dois alvos digitados elas
            NÃO vêm de lá, vêm dos alvos. Hoje ele fala da base do peso, que é
            o que o protocolo decide de fato, e quem foi preterido sai no aviso
            abaixo, com número.
          */}
          {nec?.obeso && (
            <TAviso className="mb-4">
              Obesidade · IMC {formatarNumero(antro?.imc)} — <strong>a fase da terapia não se
              aplica</strong>: as faixas acima vêm do protocolo de obesidade
              {nec.baseDoPeso ? `, ${nec.baseDoPeso}.` : '.'}
            </TAviso>
          )}

          {/*
            A regra clínica que uma entrada manual apagou, com os dois números
            lado a lado.

            É a única mudança desta tela que fecha risco de dose, e por isso não
            pode ser legenda em cinza: com hemodiálise contínua e alvo de
            1,3 g/kg o sistema adotava 84,38 g em vez de 129,82, declarava a
            meta atingida e suprimia a sugestão do módulo proteico — até 45 g de
            déficit, calado.

            Quem DECIDE é o servidor, e é dele que vem o `referencia...` que
            dispara este aviso — a tela não reimplementa a precedência, ela só
            põe na frase os dois números que já recebeu. Compor aqui, e não lá,
            é o que impede a frase de andar: se o número viesse montado do
            servidor, ele sairia da régua da diálise, e corrigir 2,0 para 1,9
            faria toda avaliação salva reabrir dizendo outro valor.
          */}
          {nec?.alvoProteicoPreteriuTerapiaRenal && (
            <TAviso className="mb-4">
              A terapia renal recomenda{' '}
              <strong>{formatarNumero(nec.proteinaTerapiaRenal)} g/dia</strong> de proteína. O{' '}
              <strong>alvo proteico digitado vence</strong>, e a meta adotada é{' '}
              <strong>{formatarNumero(nec.metaProteica)} g/dia</strong> — é contra ela que a aba
              da dieta mede a adequação e a lacuna do módulo.
            </TAviso>
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
              /* Ressalva obrigatória: o alvo proteico digitado VENCE a renal,
                 e prometer o contrário aqui foi o que escondeu um déficit de
                 até 45 g na tela de quem prescreve. */
              ajuda="Substitui a faixa proteica da fase — mas o alvo proteico digitado ainda vence."
              value={entradas.terapiaRenal}
              onChange={(e) => alterar('terapiaRenal', e.target.value)}
            />
            <TEntry
              label="Alvo calórico"
              suffix="kcal/kg"
              mascara="decimal"
              placeholder="Ex.: 22,50"
              ajuda="Preenchido, vence a faixa da fase."
              value={entradas.kcalPorKgAlvo}
              onChange={(e) => alterar('kcalPorKgAlvo', e.target.value)}
            />
            <TEntry
              label="Alvo proteico"
              suffix="g/kg"
              mascara="decimal"
              placeholder="Ex.: 1,30"
              ajuda="Preenchido, vence a faixa, a terapia renal e a obesidade."
              value={entradas.proteinaPorKgAlvo}
              onChange={(e) => alterar('proteinaPorKgAlvo', e.target.value)}
            />
            <TSelect
              label="Meta na faixa"
              opcoes={OPCOES_POSICAO_FAIXA.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
              ajuda={ajudaPosicao}
              disabled={!valeEnergia && !valeProteina}
              value={entradas.posicaoNaFaixa}
              onChange={(e) => alterar('posicaoNaFaixa', e.target.value)}
            />
          </div>

          <hr className="my-5 border-line" />

          <div className="flex flex-col gap-6">
            <TResultGroup
              titulo="Faixa recomendada"
              descricao="O intervalo recomendado para este peso e esta fase — a meta abaixo é um ponto dele."
              colunas={4}
            >
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

            {/*
              "Meta que desce para a dieta" era jargão interno, e a folha
              impressa já chamava a mesma coisa de "meta adotada" — duas
              linguagens para um número só. Fica a da folha.
            */}
            <TResultGroup
              titulo="Meta adotada — o número que a dieta persegue"
              descricao="A faixa acima é um intervalo; a prescrição precisa de um número. Este é o número adotado, e ao lado dele vai de onde ele saiu."
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
              {/*
                A legenda era o literal "substitui a faixa da fase", ao lado de
                duas linhas que leem a procedência do servidor — e mentia
                sempre que o alvo proteico digitado vencia, que é justamente
                quando alguém confere. Hoje ela DIZ se venceu, lendo a origem
                publicada, e a ausência ganhou palavra em vez de traço mudo.
              */}
              <TResult
                label="Proteína na terapia renal"
                valor={formatarNumero(nec?.proteinaTerapiaRenal)}
                unidade="g/dia"
                referencia={nec?.referenciaProteinaTerapiaRenal}
                motivoAusencia={nec?.motivoProteinaTerapiaRenal}
                recalculando={recalculando}
              />
            </TResultGroup>
          </div>
        </TTabPanel>

        {/* ───────────────────────── Dieta enteral ───────────────────── */}
        <TTabPanel id={ABA_DIETA} ativa={aba} rotulo="Dieta enteral">
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
              mascara="decimal"
              placeholder={entradas.modoInfusao === 'INTERMITENTE' ? 'Ex.: 133,50' : 'Ex.: 62,50'}
              value={entradas.volumePorTempo}
              onChange={(e) => alterar('volumePorTempo', e.target.value)}
            />
            <TEntry
              label={entradas.modoInfusao === 'INTERMITENTE' ? 'Horários por dia' : 'Horas de infusão'}
              suffix={entradas.modoInfusao === 'INTERMITENTE' ? 'horários' : 'h'}
              mascara="decimal"
              ajuda={entradas.modoInfusao === 'CONTINUA' ? 'UTI: 22 h/dia' : undefined}
              value={entradas.tempo}
              onChange={(e) => alterar('tempo', e.target.value)}
            />
            {/*
              O módulo é escolha independente da fórmula: quem tem lacuna
              escolhe, quem não tem deixa em branco e o resultado explica que
              não há o que suplementar.
            */}
            <TSelect
              label="Módulo proteico"
              vazio="Nenhum"
              className="lg:col-span-2"
              ajuda="Para cobrir a proteína que a dieta não alcança"
              opcoes={modulos.map((m) => ({ valor: m.id, rotulo: rotuloProduto(m) }))}
              value={entradas.moduloProteicoId}
              onChange={(e) => alterar('moduloProteicoId', e.target.value)}
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

            {/*
              A lacuna acima dizia o tamanho do problema e parava ali. Aqui está
              o passo que faltava: com que produto, e quanto dele.
            */}
            <TResultGroup
              titulo={
                dieta?.moduloNome
                  ? `Módulo proteico — ${dieta.moduloNome}`
                  : 'Módulo proteico'
              }
              colunas={3}
            >
              {/*
                Sem sugestão não há três resultados a mostrar — há uma frase.

                Três traços com o motivo pendurado só no primeiro campo leem
                como tela quebrada, e foi assim que "o módulo não calcula" virou
                queixa de quem usa: a lacuna era zero, o servidor explicava
                corretamente, e a forma dizia "defeito". Pior, os outros dois
                campos ficavam com a legenda órfã ("do rótulo do produto") ao
                lado de um valor vazio, porque o `TResult` só esconde a
                referência de quem tem motivo para esconder.
              */}
              {dieta?.motivoModulo ? (
                <p className="text-caption text-txt-muted sm:col-span-2 lg:col-span-3">
                  {dieta.motivoModulo}
                </p>
              ) : (
                <>
                  <TResult
                    label="Medidas por dia"
                    valor={formatarNumero(dieta?.moduloMedidas)}
                    unidade="medidas"
                    /* A nota da própria planilha (`Contínuo!V21`). É conduta: sai da
                       tela e a prescrição perde o quando. */
                    referencia="iniciar o módulo a partir do 4º dia"
                    recalculando={recalculando}
                  />
                  <TResult
                    label="Quantidade"
                    valor={formatarNumero(dieta?.moduloGramas)}
                    unidade="g/dia"
                    recalculando={recalculando}
                  />
                  <TResult
                    label="Calorias que o módulo soma"
                    valor={formatarNumero(dieta?.moduloKcal)}
                    unidade="kcal/dia"
                    referencia="do rótulo do produto — entram no total do dia"
                    recalculando={recalculando}
                  />
                </>
              )}
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
              // Na avaliação salva a tabela não foi gravada, e dizer "escolha a
              // fórmula" ali seria mentira: ela está escolhida.
              vazio={
                dieta?.motivoProgressao ??
                'Escolha a fórmula e informe o tempo para ver a progressão.'
              }
            />
          </div>
        </TTabPanel>

        {/* ───────────────────────── Hidratação ──────────────────────── */}
        <TTabPanel id={ABA_HIDRATACAO} ativa={aba} rotulo="Hidratação">
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
              mascara="decimal"
              placeholder="Ex.: 1.600,00"
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
              vazio={
                hidra?.motivoDistribuicao ??
                'Informe o peso e a dieta para calcular a água extra.'
              }
            />
          </div>
        </TTabPanel>

        {abaExtra && (
          <TTabPanel id={abaExtra.id} ativa={aba} rotulo={abaExtra.rotulo}>
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
