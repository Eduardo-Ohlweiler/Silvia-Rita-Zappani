import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { PainelAcompanhamentoPediatrico } from '@/types/pediatria'
import { AUSENTE, formatarData, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto } from '@/utils/idade'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? AUSENTE : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * O acompanhamento de uma criança, como **relatório do período**.
 *
 * Espelha o `DocumentoAcompanhamento` da UTI na estrutura — consolidado,
 * prescrição de referência, e então os dias —, mas **as tabelas do meio são
 * outras**, e é aí que os dois módulos deixam de ser o mesmo relatório:
 *
 * - a UTI desce para laboratório, gasometria e balanço hídrico, que são
 *   variáveis de terapia intensiva adulta;
 * - a pediatria desce para **crescimento** — peso, estatura, IMC e as três
 *   classificações da OMS, com a **idade em toda linha**. Em adulto o peso de
 *   hoje se compara com o de ontem; em criança ele só significa alguma coisa
 *   contra a idade, e trinta dias de internação de um lactente atravessam uma
 *   linha inteira da tabela da OMS (`docs/11 §3`).
 *
 * A variação de peso do período vem em destaque pela mesma razão: é a pergunta
 * que faz esta tela existir.
 *
 * As três classificações saem **com o rótulo que o servidor mandou**, nunca
 * traduzidas aqui: "Baixo peso", "Baixa estatura" e "Magreza" são a MESMA faixa
 * em índices diferentes, e um mapa único faria a tabela discordar da seção de
 * cima na mesma folha.
 */
export function DocumentoPainelAcompanhamentoPediatrico({
  dados,
}: {
  dados: PainelAcompanhamentoPediatrico
}) {
  const dias = dados.dias

  const periodo = [
    dados.de ? `de ${formatarData(dados.de)}` : null,
    dados.ate ? `até ${formatarData(dados.ate)}` : null,
  ]
    .filter(Boolean)
    .join(' ')

  const crescimento: LinhaValor[] = [
    {
      rotulo: 'Peso no início do período',
      valor: n(dados.pesoInicialKg, 3, 'kg'),
      detalhe:
        dados.idadeInicialMeses != null ? `com ${idadeEmMesesTexto(dados.idadeInicialMeses)}` : '',
    },
    {
      rotulo: 'Peso no fim do período',
      valor: n(dados.pesoFinalKg, 3, 'kg'),
      detalhe:
        dados.idadeFinalMeses != null ? `com ${idadeEmMesesTexto(dados.idadeFinalMeses)}` : '',
    },
    {
      rotulo: 'Variação de peso',
      valor: n(dados.variacaoPesoKg, 3, 'kg'),
      /*
       * A variação usa os dias que TÊM peso, não as bordas do período — se a
       * criança não foi pesada no primeiro dia, a conta começa no primeiro em
       * que foi. Há teste para isso.
       */
      detalhe: 'Entre o primeiro e o último dia com peso medido, não as bordas do período',
    },
  ]

  const consolidado: LinhaValor[] = [
    {
      rotulo: 'Adesão média ao volume',
      valor: n(dados.adesaoMedia, 1, '%'),
      detalhe: 'Recebido sobre prescrito, dia a dia',
    },
    {
      rotulo: 'Aceitação média das tomadas',
      valor: n(dados.aceitacaoTomadasMedia, 1, '%'),
      detalhe: 'A unidade da pediatria é a tomada, não a refeição',
    },
    { rotulo: 'Energia recebida', valor: n(dados.caloriasPorKgMedia, 1, 'kcal/kg/dia') },
    { rotulo: 'Proteína recebida', valor: n(dados.proteinaPorKgMedia, 2, 'g/kg/dia') },
    {
      rotulo: 'Adequação calórica média',
      valor: n(dados.adequacaoCaloricaMedia, 1, '%'),
      detalhe: 'Sobre o VET da avaliação de referência',
    },
    {
      rotulo: 'Adequação proteica média',
      valor: n(dados.adequacaoProteicaMedia, 1, '%'),
      detalhe: 'Sobre a necessidade proteica das DRIs',
    },
  ]

  const prescricao: LinhaValor[] = [
    { rotulo: 'Volume prescrito', valor: n(dados.volumePrescritoNaAvaliacao, 0, 'ml/dia') },
    { rotulo: 'Necessidade energética', valor: n(dados.vet, 0, 'kcal/dia') },
    { rotulo: 'Necessidade proteica', valor: n(dados.proteinaNecessidade, 2, 'g/dia') },
  ]

  return (
    <Folha
      titulo="Relatório de acompanhamento pediátrico"
      paciente={dados.pessoaNome}
      referencia={
        <>
          {dados.totalDias} {dados.totalDias === 1 ? 'dia registrado' : 'dias registrados'}
          {periodo ? ` · ${periodo}` : ''}
          {dados.ultimaAvaliacao
            ? ` · avaliação de referência em ${formatarData(dados.ultimaAvaliacao)}`
            : ''}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Dias', valor: String(dados.totalDias) },
          { rotulo: 'Variação de peso', valor: n(dados.variacaoPesoKg, 3, 'kg') },
          { rotulo: 'Adesão média', valor: n(dados.adesaoMedia, 1, '%') },
          { rotulo: 'Energia', valor: n(dados.caloriasPorKgMedia, 1, 'kcal/kg') },
          { rotulo: 'Proteína', valor: n(dados.proteinaPorKgMedia, 2, 'g/kg') },
        ]}
      />

      <Secao
        titulo="Crescimento no período"
        nota="É o que só a pediatria tem: entre uma avaliação e outra, a idade anda — e o peso só se lê contra ela."
      >
        <LinhasDeValor linhas={crescimento} />
      </Secao>

      <Secao
        titulo="Consolidado do período"
        nota="As médias ignoram o dia em que o valor não foi informado — ausência não é zero."
      >
        <LinhasDeValor linhas={consolidado} />
      </Secao>

      <Secao
        titulo="Prescrição de referência"
        nota={
          dados.ultimaAvaliacao
            ? `Da avaliação de ${formatarData(dados.ultimaAvaliacao)} — é contra ela que a adesão e as adequações foram medidas.`
            : 'Nenhuma avaliação vinculada aos dias do período.'
        }
      >
        <LinhasDeValor linhas={prescricao} />
      </Secao>

      {dados.diasSemAvaliacao > 0 && (
        <Secao titulo="O que ficou de fora">
          <p className="folha-vazio">
            {dados.diasSemAvaliacao === 1
              ? 'Um dia não tem avaliação vinculada'
              : `${dados.diasSemAvaliacao} dias não têm avaliação vinculada`}
            : neles a adequação calórica e a proteica não existem, porque dependem do VET e da
            necessidade proteica prescritos. Esses dias ficam fora das médias acima — o volume
            recebido continua registrado.
          </p>
        </Secao>
      )}

      <Secao
        titulo="Crescimento, dia a dia"
        nota="Classificação da OMS, 0 a 60 meses. A idade acompanha cada linha porque é ela que dá sentido ao peso — e ela muda dentro do próprio período."
      >
        <TabelaDoc
          linhas={dias}
          chaveDe={(d) => d.id}
          vazio="Nenhum dia registrado no período."
          colunas={[
            { titulo: 'Data', celula: (d) => formatarData(d.data) },
            {
              titulo: 'Idade (m)',
              numerica: true,
              celula: (d) =>
                d.derivados.idadeMeses != null ? String(d.derivados.idadeMeses) : AUSENTE,
            },
            { titulo: 'Peso', numerica: true, celula: (d) => n(d.pesoKg, 3) },
            { titulo: 'Estatura', numerica: true, celula: (d) => n(d.estaturaCm, 1) },
            { titulo: 'IMC', numerica: true, celula: (d) => n(d.derivados.imc, 2) },
            { titulo: 'P/I', celula: (d) => d.derivados.pesoIdade?.rotulo ?? AUSENTE },
            { titulo: 'E/I', celula: (d) => d.derivados.estaturaIdade?.rotulo ?? AUSENTE },
            { titulo: 'IMC/I', celula: (d) => d.derivados.imcIdade?.rotulo ?? AUSENTE },
          ]}
        />
      </Secao>

      <Secao
        titulo="Dieta, dia a dia"
        /*
         * A coluna "Prescrito" mostra o volume CONTRA O QUAL a adesão foi
         * medida, e não o digitado no dia — porque é ele que faz a linha
         * fechar. `docs/11 §5` manda o prescrito da avaliação vencer o
         * digitado, e o servidor devolve a procedência em
         * `referenciaDoRecebido`. Exibir o digitado ao lado de um percentual
         * calculado sobre outro número daria uma conta que não fecha num
         * prontuário: 700 de 800 não é 83,3 %, e quem confere no papel
         * concluiria que o sistema errou.
         */
        nota="A adesão é medida contra o volume prescrito na avaliação, não contra o digitado no dia — é o que estava de fato prescrito. Sem avaliação vinculada, cai no digitado. A oferta sai da composição da fórmula gravada, declarada por 100 ml."
      >
        <TabelaDoc
          linhas={dias}
          chaveDe={(d) => d.id}
          vazio="Nenhum dia registrado no período."
          colunas={[
            { titulo: 'Data', celula: (d) => formatarData(d.data) },
            { titulo: 'Fórmula', celula: (d) => d.avaliacaoFormulaNome ?? AUSENTE },
            {
              titulo: 'Prescrito',
              numerica: true,
              celula: (d) => n(d.avaliacaoVolumeTotal ?? d.volPrescrito24h, 0),
            },
            { titulo: 'Recebido', numerica: true, celula: (d) => n(d.volRecebido24h, 0) },
            {
              titulo: 'Adesão',
              numerica: true,
              celula: (d) => n(d.derivados.percentualRecebido, 1, '%'),
            },
            {
              titulo: 'Tomadas',
              numerica: true,
              celula: (d) =>
                d.tomadasAceitas != null && d.tomadasPrevistas != null
                  ? `${formatarNumero(d.tomadasAceitas, 0)}/${formatarNumero(d.tomadasPrevistas, 0)}`
                  : AUSENTE,
            },
            { titulo: 'kcal/kg', numerica: true, celula: (d) => n(d.derivados.caloriasPorKg, 1) },
            { titulo: 'g PTN/kg', numerica: true, celula: (d) => n(d.derivados.proteinaPorKg, 2) },
            {
              titulo: '% VET',
              numerica: true,
              celula: (d) => n(d.derivados.adequacaoCalorica, 1),
            },
            {
              titulo: '% PTN',
              numerica: true,
              celula: (d) => n(d.derivados.adequacaoProteica, 1),
            },
          ]}
        />
      </Secao>
    </Folha>
  )
}
