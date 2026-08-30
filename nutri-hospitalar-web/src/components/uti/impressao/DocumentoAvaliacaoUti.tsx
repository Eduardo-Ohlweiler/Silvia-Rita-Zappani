import {
  Folha,
  LinhasDeValor,
  Observacao,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import {
  OPCOES_ETNIA,
  OPCOES_FASE,
  OPCOES_JANELA_PERDA,
  OPCOES_MODO_INFUSAO,
  OPCOES_POPULACAO,
  OPCOES_POSICAO_FAIXA,
  OPCOES_SEGMENTO,
  OPCOES_SEXO,
  OPCOES_TERAPIA_RENAL,
  type CalculoUtiRequest,
  type ResultadoUti,
} from '@/types/uti'
import { formatarData, formatarNumero, rotuloDe } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * Uma faixa "mínimo a máximo", com a unidade **do par**.
 *
 * Escrever `${n(min, 0)} a ${n(max, 0, 'kcal/dia')}` prendia a unidade ao
 * segundo termo: faltando o máximo, o papel imprimia "1.020 a —", um número sem
 * unidade nenhuma. Faltando qualquer um dos dois, não há faixa — e dizer isso é
 * melhor que meia faixa.
 */
const faixa = (min: number | null | undefined, max: number | null | undefined,
               casas: number, unidade: string) =>
  min == null || max == null
    ? '—'
    : `${formatarNumero(min, casas, casas)} a ${formatarNumero(max, casas, casas)} ${unidade}`

/**
 * A avaliação de terapia nutricional como **prontuário**.
 *
 * O formulário na tela pergunta; este papel responde. As entradas aparecem uma
 * vez, compactas, no alto — porque quem confere precisa saber de que medidas o
 * número saiu — e o resto da folha é resultado, com a **procedência ao lado de
 * cada valor**: "peso estimado · Rabito 2008", "da faixa da fase · máximo".
 * Prescrição sem procedência não é auditável, e no papel isso pesa mais ainda:
 * a folha vai circular sem o sistema junto.
 *
 * Serve os dois caminhos — a calculadora avulsa e a avaliação salva —, porque
 * os dois produzem o mesmo {@link ResultadoUti}. Sem paciente, o cabeçalho diz
 * que é cálculo rápido em vez de inventar um nome.
 */
export function DocumentoAvaliacaoUti({
  entradas,
  resultado,
  paciente,
  profissional,
  data,
  observacao,
}: {
  entradas: CalculoUtiRequest
  resultado?: ResultadoUti
  paciente?: string
  profissional?: string
  data?: string
  observacao?: string | null
}) {
  const antro = resultado?.antropometria
  const nec = resultado?.necessidades
  const dieta = resultado?.dieta
  const hidra = resultado?.hidratacao

  const segmentos = (entradas.segmentosAmputados ?? [])
    .map((seg) => rotuloDe(OPCOES_SEGMENTO, seg))
    .filter(Boolean)
    .join(', ')

  const medidas: LinhaValor[] = [
    {
      rotulo: 'Sexo e idade',
      valor: [
        rotuloDe(OPCOES_SEXO, entradas.sexo) ?? '—',
        entradas.idadeAnos != null
          ? `${entradas.idadeAnos} ${entradas.idadeAnos === 1 ? 'ano' : 'anos'}`
          : null,
      ]
        .filter(Boolean)
        .join(' · '),
      detalhe: rotuloDe(OPCOES_ETNIA, entradas.etnia)
        ? `etnia ${rotuloDe(OPCOES_ETNIA, entradas.etnia)?.toLowerCase()} — a equação de Chumlea é separada por etnia`
        : '',
    },
    { rotulo: 'Peso atual', valor: n(entradas.pesoAtualKg, 2, 'kg') },
    {
      rotulo: 'Peso habitual',
      valor: n(entradas.pesoUsualKg, 2, 'kg'),
      // O rótulo da janela vem do mapa de opções. `toLowerCase()` no enum
      // imprimia "janela de um_mes" — nome cru de enum em prontuário.
      detalhe: rotuloDe(OPCOES_JANELA_PERDA, entradas.janelaPerda)
        ? `janela de ${rotuloDe(OPCOES_JANELA_PERDA, entradas.janelaPerda)}`
        : '',
    },
    { rotulo: 'Altura', valor: n(entradas.alturaCm, 1, 'cm') },
    { rotulo: 'Altura do joelho', valor: n(entradas.alturaJoelhoCm, 1, 'cm') },
    { rotulo: 'Circunferência do braço', valor: n(entradas.circBracoCm, 1, 'cm') },
    { rotulo: 'Circunferência da panturrilha', valor: n(entradas.circPanturrilhaCm, 1, 'cm') },
    { rotulo: 'Circunferência abdominal', valor: n(entradas.circAbdominalCm, 1, 'cm') },
    ...(segmentos ? [{ rotulo: 'Segmentos amputados', valor: segmentos }] : []),
  ]

  /**
   * As escolhas que mudam a conta, e que não apareciam no papel.
   *
   * Sem elas a folha era inauditável na parte que mais importa: duas avaliações
   * com as mesmas medidas e metas diferentes ficavam inexplicáveis — a diferença
   * está na fase, no ponto da faixa ou na terapia renal, e nenhuma das três
   * saía impressa. O próprio docstring deste arquivo já prometia o contrário.
   */
  const escolhas: LinhaValor[] = [
    {
      rotulo: 'Fase da terapia',
      valor: rotuloDe(OPCOES_FASE, entradas.fase) ?? '—',
      detalhe: entradas.kcalPorKgAlvo != null || entradas.proteinaPorKgAlvo != null
        ? 'substituída por alvo personalizado'
        : 'define a faixa de energia e de proteína',
    },
    {
      rotulo: 'Ponto adotado na faixa',
      valor: rotuloDe(OPCOES_POSICAO_FAIXA, entradas.posicaoNaFaixa) ?? 'Máximo da faixa',
    },
    {
      rotulo: 'Alvo personalizado',
      valor:
        entradas.kcalPorKgAlvo != null || entradas.proteinaPorKgAlvo != null
          ? [
              entradas.kcalPorKgAlvo != null ? `${n(entradas.kcalPorKgAlvo, 1)} kcal/kg` : null,
              entradas.proteinaPorKgAlvo != null ? `${n(entradas.proteinaPorKgAlvo, 2)} g/kg` : null,
            ]
              .filter(Boolean)
              .join(' · ')
          : '—',
      detalhe: 'Quando informado, vence a faixa da fase',
    },
    {
      rotulo: 'Terapia renal substitutiva',
      valor: rotuloDe(OPCOES_TERAPIA_RENAL, entradas.terapiaRenal) ?? '—',
      detalhe: 'Substitui a meta proteica por 1,8 ou 2,0 g/kg',
    },
    {
      rotulo: 'Modo de infusão',
      valor: rotuloDe(OPCOES_MODO_INFUSAO, entradas.modoInfusao) ?? '—',
      detalhe:
        entradas.volumePorTempo != null && entradas.tempo != null
          ? `${n(entradas.volumePorTempo, 0)} × ${n(entradas.tempo, 0)}`
          : '',
    },
    {
      rotulo: 'População de referência',
      valor: rotuloDe(OPCOES_POPULACAO, entradas.populacaoReferencia) ?? '—',
      detalhe: 'Só muda o ajuste de CB e CP com IMC abaixo de 18,5',
    },
  ]

  const cascata: LinhaValor[] = [
    {
      rotulo: 'Peso de trabalho',
      valor: n(antro?.pesoDeTrabalhoKg, 2, 'kg'),
      detalhe: antro?.pesoDeTrabalhoOrigem ?? antro?.motivoPesoDeTrabalho ?? '',
    },
    {
      rotulo: 'Altura usada',
      valor: n(antro?.alturaUsadaCm, 1, 'cm'),
      detalhe: antro?.alturaUsadaOrigem ?? '',
    },
    {
      rotulo: 'IMC',
      valor: n(antro?.imc, 2),
      detalhe: antro?.classificacaoImcOms?.rotulo ?? antro?.motivoImc ?? '',
    },
    {
      rotulo: 'IMC — referência do idoso (OPAS)',
      valor: antro?.classificacaoImcOpas?.rotulo ?? '—',
      detalhe: 'Aplica-se a partir de 60 anos',
    },
    { rotulo: 'Peso ideal', valor: n(antro?.pesoIdealKg, 2, 'kg') },
    { rotulo: 'Peso ideal para IMC 25', valor: n(antro?.pesoIdealImc25Kg, 2, 'kg') },
    { rotulo: 'Peso ajustado', valor: n(antro?.pesoAjustadoKg, 2, 'kg') },
    { rotulo: 'Peso corrigido por amputação', valor: n(antro?.pesoCorrigidoAmputacaoKg, 2, 'kg') },
  ]

  const estado: LinhaValor[] = [
    {
      rotulo: 'Perda de peso',
      valor: n(antro?.percentualPerdaPeso, 2, '%'),
      detalhe: antro?.classificacaoPerdaPeso?.rotulo ?? antro?.motivoPerdaPeso ?? '',
    },
    {
      rotulo: 'Adequação da circunferência do braço',
      valor: n(antro?.adequacaoCircBracoPerc, 2, '%'),
      detalhe:
        antro?.classificacaoAdequacaoCircBraco?.rotulo ?? antro?.motivoAdequacaoCircBraco ?? '',
    },
    {
      rotulo: 'Massa muscular do braço',
      valor: antro?.classificacaoMassaMuscularBraco?.rotulo ?? '—',
      detalhe: antro?.circBracoAjustadaCm != null ? `CB ajustada ${n(antro.circBracoAjustadaCm, 1, 'cm')}` : '',
    },
    {
      rotulo: 'Depleção da panturrilha',
      valor: antro?.classificacaoDeplecaoPanturrilha?.rotulo ?? '—',
      detalhe:
        antro?.circPanturrilhaAjustadaCm != null
          ? `CP ajustada ${n(antro.circPanturrilhaAjustadaCm, 1, 'cm')}`
          : (antro?.motivoDeplecao ?? ''),
    },
  ]

  const necessidades: LinhaValor[] = [
    {
      rotulo: 'Energia — faixa recomendada',
      valor: faixa(nec?.energiaMinima, nec?.energiaMaxima, 0, 'kcal/dia'),
    },
    {
      rotulo: 'Energia — meta adotada',
      valor: n(nec?.metaEnergetica, 0, 'kcal/dia'),
      detalhe: nec?.metaEnergeticaOrigem ?? nec?.motivo ?? '',
    },
    {
      rotulo: 'Proteína — faixa recomendada',
      valor: faixa(nec?.proteinaMinima, nec?.proteinaMaxima, 1, 'g/dia'),
    },
    {
      rotulo: 'Proteína — meta adotada',
      valor: n(nec?.metaProteica, 1, 'g/dia'),
      detalhe: nec?.metaProteicaOrigem ?? '',
    },
    {
      rotulo: 'Proteína na terapia renal',
      valor: n(nec?.proteinaTerapiaRenal, 1, 'g/dia'),
      detalhe: 'Substitui a meta quando há hemodiálise',
    },
    {
      // A frase vai no DETALHE, não na coluna numérica: ela é longa e ali sairia
      // alinhada à direita com figuras tabulares, brigando com os números.
      rotulo: 'Base do peso usada na energia',
      valor: nec?.obeso ? 'Corrigida' : 'Peso de trabalho',
      detalhe: nec?.obeso
        ? (nec.baseDoPeso ?? 'Correção de obesidade da ASPEN/SCCM 2016 aplicada')
        : 'Sem correção de obesidade — a energia usa o peso de trabalho direto',
    },
  ]

  const prescricao: LinhaValor[] = [
    {
      rotulo: 'Fórmula',
      valor: dieta?.formulaNome ?? '—',
      detalhe: dieta?.densidadeKcalMl != null ? `${n(dieta.densidadeKcalMl, 2)} kcal/ml · ${n(dieta.proteinaGL, 1)} g PTN/L` : '',
    },
    {
      rotulo: 'Volume total',
      valor: n(dieta?.volumeTotalMl, 0, 'ml/dia'),
      detalhe: dieta?.volumeTotalDescricao ?? dieta?.motivo ?? '',
    },
    {
      rotulo: 'Energia ofertada',
      valor: n(dieta?.caloriasOfertadas, 0, 'kcal/dia'),
      detalhe: dieta?.percentualDoVct != null ? `${n(dieta.percentualDoVct, 1)} % da meta` : '',
    },
    {
      rotulo: 'Proteína ofertada',
      valor: n(dieta?.proteinaOfertada, 1, 'g/dia'),
      detalhe: dieta?.percentualDaProteina != null ? `${n(dieta.percentualDaProteina, 1)} % da meta` : '',
    },
    { rotulo: 'Energia por quilo', valor: n(dieta?.caloriasPorQuilo, 1, 'kcal/kg/dia') },
    { rotulo: 'Proteína por quilo', valor: n(dieta?.proteinaPorQuilo, 2, 'g/kg/dia') },
    { rotulo: 'Carboidrato ofertado', valor: n(dieta?.choOfertado, 1, 'g/dia') },
    { rotulo: 'Lipídio ofertado', valor: n(dieta?.lipOfertado, 1, 'g/dia') },
    { rotulo: 'Fibras ofertadas', valor: n(dieta?.fibrasOfertadas, 1, 'g/dia') },
    { rotulo: 'Potássio ofertado', valor: n(dieta?.potassioOfertado, 0, 'mg/dia') },
    {
      rotulo: 'Proteína suplementar necessária',
      valor: n(dieta?.proteinaSuplementar, 1, 'g/dia'),
      detalhe: 'Diferença entre a meta e o que a fórmula entrega',
    },
  ]

  const hidratacao: LinhaValor[] = [
    { rotulo: 'Necessidade hídrica mínima', valor: n(hidra?.necessidadeMinima, 0, 'ml/dia'), detalhe: '25 ml/kg' },
    { rotulo: 'Necessidade hídrica ideal', valor: n(hidra?.necessidadeIdeal, 0, 'ml/dia'), detalhe: '30 ml/kg' },
    {
      rotulo: 'Água na fórmula',
      valor: n(hidra?.percentualAgua, 1, '%'),
      detalhe: hidra?.percentualAguaOrigem ?? hidra?.motivo ?? '',
    },
    { rotulo: 'Água que a dieta entrega', valor: n(hidra?.aguaNaDieta, 0, 'ml/dia') },
    { rotulo: 'Água extra — mínima', valor: n(hidra?.aguaExtraMinima, 0, 'ml/dia') },
    { rotulo: 'Água extra — ideal', valor: n(hidra?.aguaExtraIdeal, 0, 'ml/dia') },
  ]

  return (
    <Folha
      titulo="Avaliação de terapia nutricional"
      paciente={paciente ?? 'Cálculo rápido — sem paciente'}
      referencia={
        <>
          {data ? `Avaliação de ${formatarData(data)}` : 'Cálculo não gravado'}
          {profissional ? ` · ${profissional}` : ''}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Peso de trabalho', valor: n(antro?.pesoDeTrabalhoKg, 2, 'kg') },
          { rotulo: 'IMC', valor: n(antro?.imc, 2) },
          { rotulo: 'Meta energética', valor: n(nec?.metaEnergetica, 0, 'kcal') },
          { rotulo: 'Meta proteica', valor: n(nec?.metaProteica, 1, 'g') },
          { rotulo: 'Volume', valor: n(dieta?.volumeTotalMl, 0, 'ml') },
        ]}
      />

      <Secao titulo="Medidas informadas">
        <LinhasDeValor linhas={medidas} />
      </Secao>

      <Secao
        titulo="Escolhas do cálculo"
        nota="O que a nutricionista decidiu e que muda o número — sem isto, duas avaliações com as mesmas medidas e metas diferentes ficam inexplicáveis."
      >
        <LinhasDeValor linhas={escolhas} />
      </Secao>

      <Secao
        titulo="Peso e altura de trabalho"
        nota="Todo o resto da folha usa estes dois valores. A origem de cada um vem ao lado — é o que torna a prescrição auditável."
      >
        <LinhasDeValor linhas={cascata} />
      </Secao>

      <Secao
        titulo="Estado nutricional"
        nota="Circunferências ajustadas pelo IMC antes da comparação com o ponto de corte, quando o ajuste se aplica (docs/10 §2.9)."
      >
        <LinhasDeValor linhas={estado} />
      </Secao>

      <Secao titulo="Necessidades nutricionais">
        <LinhasDeValor linhas={necessidades} />
      </Secao>

      <Secao titulo="Dieta enteral prescrita">
        <LinhasDeValor linhas={prescricao} />
      </Secao>

      {dieta?.progressao && dieta.progressao.length > 0 && (
        <Secao
          titulo="Progressão"
          nota="25, 50, 75 e 100 % da meta nos primeiros quatro dias — a escada da própria planilha."
        >
          <TabelaDoc
            linhas={dieta.progressao}
            chaveDe={(d) => d.dia}
            colunas={[
              { titulo: 'Dia', celula: (d) => String(d.dia) },
              { titulo: '% da meta', numerica: true, celula: (d) => n(d.percentual, 0, '%') },
              { titulo: 'Energia', numerica: true, celula: (d) => n(d.kcal, 0, 'kcal') },
              {
                titulo: dieta.unidadeDoVolume ?? 'Volume',
                numerica: true,
                celula: (d) => n(d.volume, 1),
              },
            ]}
          />
        </Secao>
      )}

      <Secao titulo="Hidratação">
        <LinhasDeValor linhas={hidratacao} />
      </Secao>

      {hidra?.distribuicaoIdeal && hidra.distribuicaoIdeal.length > 0 && (
        <Secao titulo="Como fracionar a água extra" nota="Do volume ideal, ao longo do dia.">
          <TabelaDoc
            linhas={hidra.distribuicaoIdeal}
            chaveDe={(f) => f.vezesAoDia}
            colunas={[
              { titulo: 'Vezes ao dia', celula: (f) => String(f.vezesAoDia) },
              { titulo: 'Por vez', numerica: true, celula: (f) => n(f.mlPorVez, 0, 'ml') },
            ]}
          />
        </Secao>
      )}

      <Observacao texto={observacao} />
    </Folha>
  )
}
