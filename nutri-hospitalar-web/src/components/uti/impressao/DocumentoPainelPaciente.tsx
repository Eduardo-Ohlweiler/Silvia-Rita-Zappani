import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { PainelPacienteUti } from '@/types/uti'
import { formatarData, formatarNumero } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * O histórico de um paciente de UTI — **demonstrativo de evolução**, não
 * espelho da tela.
 *
 * O painel na tela mostra gráficos; no papel o gráfico vira tabela, porque é
 * ela que se lê à beira do leito e é ela que sobrevive a uma fotocópia. A
 * pergunta que este documento responde é "para onde este paciente foi": a
 * situação de hoje no topo, a trajetória em seguida, e por quais fórmulas ele
 * passou.
 *
 * <b>A ordem da trajetória é da mais recente para a mais antiga</b>, ao
 * contrário do gráfico. No papel se lê de cima para baixo procurando o estado
 * atual primeiro; no eixo X do gráfico o tempo corre para a direita.
 */
export function DocumentoPainelPaciente({ dados }: { dados: PainelPacienteUti }) {
  const ultima = dados.ultima
  const antro = ultima?.resultado.antropometria
  const nec = ultima?.resultado.necessidades
  const dieta = ultima?.resultado.dieta

  const situacao: LinhaValor[] = [
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
      detalhe: [antro?.classificacaoImcOms?.rotulo, antro?.classificacaoImcOpas?.rotulo]
        .filter(Boolean)
        .join(' · '),
    },
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
      rotulo: 'Meta energética',
      valor: n(nec?.metaEnergetica, 0, 'kcal/dia'),
      detalhe: nec?.metaEnergeticaOrigem ?? nec?.motivo ?? '',
    },
    {
      rotulo: 'Meta proteica',
      valor: n(nec?.metaProteica, 1, 'g/dia'),
      detalhe: nec?.metaProteicaOrigem ?? '',
    },
    {
      rotulo: 'Dieta prescrita',
      valor: dieta?.formulaNome ?? '—',
      detalhe: dieta?.volumeTotalMl != null ? `${n(dieta.volumeTotalMl, 0)} ml/dia` : '',
    },
  ]

  // Do mais recente para o mais antigo: no papel se procura o estado de hoje.
  const trajetoria = [...dados.evolucao].reverse()

  return (
    <Folha
      titulo="Histórico de terapia nutricional"
      paciente={dados.pacienteNome}
      referencia={
        <>
          {dados.totalAvaliacoes}{' '}
          {dados.totalAvaliacoes === 1 ? 'avaliação' : 'avaliações'}
          {dados.primeiraAvaliacao
            ? ` · de ${formatarData(dados.primeiraAvaliacao)} a ${formatarData(dados.ultimaAvaliacao)}`
            : ''}
          {dados.totalDiasRegistrados > 0
            ? ` · ${dados.totalDiasRegistrados} dias de acompanhamento registrados`
            : ''}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          {
            rotulo: 'Idade',
            valor:
              dados.idadeAnosAtual != null
                ? `${dados.idadeAnosAtual} ${dados.idadeAnosAtual === 1 ? 'ano' : 'anos'}`
                : '—',
          },
          { rotulo: 'Avaliações', valor: String(dados.totalAvaliacoes) },
          { rotulo: 'Peso atual', valor: n(antro?.pesoDeTrabalhoKg, 2, 'kg') },
          { rotulo: 'IMC', valor: n(antro?.imc, 2) },
          { rotulo: 'Meta', valor: n(nec?.metaEnergetica, 0, 'kcal') },
        ]}
      />

      <Secao
        titulo={
          dados.ultimaAvaliacao
            ? `Situação na última avaliação — ${formatarData(dados.ultimaAvaliacao)}`
            : 'Situação atual'
        }
        nota="Números da avaliação como ela foi gravada; a origem de cada valor vem ao lado."
      >
        {ultima ? (
          <LinhasDeValor linhas={situacao} />
        ) : (
          <p className="folha-vazio">
            Este paciente ainda não tem avaliação de terapia nutricional no período.
          </p>
        )}
      </Secao>

      <Secao
        titulo="Trajetória"
        nota="Da avaliação mais recente para a mais antiga. Cada linha é o que foi gravado naquele dia — nada é recalculado."
      >
        <TabelaDoc
          linhas={trajetoria}
          chaveDe={(p) => p.dataAvaliacao}
          vazio="Nenhuma avaliação no período escolhido."
          colunas={[
            { titulo: 'Data', celula: (p) => formatarData(p.dataAvaliacao) },
            { titulo: 'Peso', numerica: true, celula: (p) => n(p.pesoTrabalhoKg, 2) },
            { titulo: 'IMC', numerica: true, celula: (p) => n(p.imc, 2) },
            { titulo: 'Classificação', celula: (p) => p.classifImcOms ?? '—' },
            { titulo: 'Perda', numerica: true, celula: (p) => n(p.percPerdaPeso, 2, '%') },
            { titulo: 'Meta kcal', numerica: true, celula: (p) => n(p.metaEnergetica, 0) },
            { titulo: 'Meta PTN', numerica: true, celula: (p) => n(p.metaProteica, 1) },
            { titulo: 'kcal/kg', numerica: true, celula: (p) => n(p.caloriasPorQuilo, 1) },
            { titulo: 'g PTN/kg', numerica: true, celula: (p) => n(p.proteinaPorQuilo, 2) },
            { titulo: '% do VCT', numerica: true, celula: (p) => n(p.percentualDoVct, 1) },
          ]}
        />
      </Secao>

      <Secao
        titulo="Fórmulas utilizadas"
        nota="Do retrato gravado em cada avaliação, não do catálogo — a fórmula pode ter saído dele desde então."
      >
        <TabelaDoc
          linhas={dados.historicoFormulas}
          chaveDe={(f) => f.formulaNome}
          vazio="Nenhuma avaliação com fórmula enteral escolhida no período."
          colunas={[
            { titulo: 'Fórmula', celula: (f) => f.formulaNome },
            { titulo: 'Avaliações', numerica: true, celula: (f) => String(f.avaliacoes) },
            { titulo: 'Primeiro uso', celula: (f) => formatarData(f.primeiroUso) },
            { titulo: 'Último uso', celula: (f) => formatarData(f.ultimoUso) },
          ]}
        />
      </Secao>
    </Folha>
  )
}
