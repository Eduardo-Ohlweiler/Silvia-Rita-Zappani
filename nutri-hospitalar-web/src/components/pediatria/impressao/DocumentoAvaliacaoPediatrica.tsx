import {
  Folha,
  LinhasDeValor,
  Observacao,
  Secao,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { CalculoPediatricoRequest, ResultadoPediatrico } from '@/types/pediatria'
import { formatarData, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto as idade } from '@/utils/idade'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * A avaliação pediátrica como **prontuário**.
 *
 * O que o papel acrescenta à tela é o **motivo da ausência escrito por
 * extenso**, ao lado do traço. Numa tela vazia dá para tocar num campo e
 * descobrir; numa folha, não — e uma criança de 37 meses sem VET, sem
 * explicação, faz quem lê achar que o sistema falhou. O motivo é informação
 * clínica ("acima de 35 meses"), não mensagem de erro.
 *
 * Serve a calculadora e a avaliação salva, que produzem o mesmo
 * {@link ResultadoPediatrico}.
 */
export function DocumentoAvaliacaoPediatrica({
  entradas,
  resultado,
  formula,
  paciente,
  profissional,
  data,
  observacao,
}: {
  entradas: CalculoPediatricoRequest
  resultado?: ResultadoPediatrico
  /** O retrato da fórmula escolhida — nome e composição por 100 ml. */
  formula?: { nome?: string | null; kcalPor100ml?: number | null; proteinaPor100ml?: number | null }
  paciente?: string
  profissional?: string
  data?: string
  observacao?: string | null
}) {
  const estado = resultado?.estadoNutricional
  const nec = resultado?.necessidades
  const dieta = resultado?.dieta

  const medidas: LinhaValor[] = [
    {
      rotulo: 'Sexo e idade',
      valor: [
        entradas.sexo === 'MASCULINO'
          ? 'Masculino'
          : entradas.sexo === 'FEMININO'
            ? 'Feminino'
            : '—',
        entradas.idadeMeses != null ? idade(entradas.idadeMeses) : null,
      ]
        .filter(Boolean)
        .join(' · '),
    },
    { rotulo: 'Peso', valor: n(entradas.peso, 2, 'kg') },
    { rotulo: 'Estatura', valor: n(entradas.estatura, 1, 'cm') },
  ]

  const antropometria: LinhaValor[] = [
    {
      rotulo: 'IMC',
      valor: n(estado?.imc, 2),
      detalhe: estado?.motivoImc ?? '',
    },
    {
      rotulo: 'Peso para a idade',
      valor: estado?.pesoIdade?.rotulo ?? '—',
      detalhe: estado?.pesoIdade ? '' : (estado?.motivo ?? ''),
    },
    {
      rotulo: 'Estatura para a idade',
      valor: estado?.estaturaIdade?.rotulo ?? '—',
      detalhe: estado?.estaturaIdade ? '' : (estado?.motivo ?? ''),
    },
    {
      rotulo: 'IMC para a idade',
      valor: estado?.imcIdade?.rotulo ?? '—',
      detalhe: estado?.imcIdade ? '' : (estado?.motivo ?? estado?.motivoImc ?? ''),
    },
  ]

  const necessidades: LinhaValor[] = [
    {
      rotulo: 'Energia (VET)',
      valor: n(nec?.vet, 0, 'kcal/dia'),
      detalhe: nec?.motivoVet ?? '',
    },
    {
      rotulo: 'Proteína',
      valor: n(nec?.proteina, 1, 'g/dia'),
      detalhe: nec?.motivoProteina ?? '',
    },
  ]

  const prescricao: LinhaValor[] = [
    {
      rotulo: 'Fórmula láctea',
      valor: formula?.nome ?? '—',
      detalhe:
        formula?.kcalPor100ml != null
          ? `${n(formula.kcalPor100ml, 1)} kcal e ${n(formula.proteinaPor100ml, 2)} g PTN por 100 ml`
          : '',
    },
    {
      rotulo: 'Volume por tomada',
      valor: n(entradas.volumeMl, 0, 'ml'),
      detalhe:
        entradas.frequenciaHoras != null ? `a cada ${n(entradas.frequenciaHoras, 0)} horas` : '',
    },
    {
      rotulo: 'Tomadas por dia',
      valor: n(dieta?.vezesDia, 1),
      detalhe: dieta?.motivo ?? '',
    },
    { rotulo: 'Volume total', valor: n(dieta?.volumeTotal, 0, 'ml/dia') },
    { rotulo: 'Energia ofertada', valor: n(dieta?.caloriasTotais, 0, 'kcal/dia') },
    { rotulo: 'Proteína ofertada', valor: n(dieta?.proteinaTotal, 2, 'g/dia') },
    {
      rotulo: 'Adequação calórica',
      valor: n(dieta?.percCalorico, 1, '%'),
      // A adequação cai junto com a meta: sem denominador não há percentual, e
      // é o motivo da meta que explica o vazio.
      detalhe: dieta?.percCalorico == null ? (dieta?.motivo ?? nec?.motivoVet ?? '') : '',
    },
    {
      rotulo: 'Adequação proteica',
      valor: n(dieta?.percProteico, 1, '%'),
      detalhe: dieta?.percProteico == null ? (dieta?.motivo ?? nec?.motivoProteina ?? '') : '',
    },
  ]

  return (
    <Folha
      titulo="Avaliação nutricional pediátrica"
      paciente={paciente ?? 'Cálculo rápido — sem paciente'}
      referencia={
        <>
          {data ? `Avaliação de ${formatarData(data)}` : 'Cálculo não gravado'}
          {entradas.idadeMeses != null ? ` · ${idade(entradas.idadeMeses)}` : ''}
          {profissional ? ` · ${profissional}` : ''}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Peso', valor: n(entradas.peso, 2, 'kg') },
          { rotulo: 'Estatura', valor: n(entradas.estatura, 1, 'cm') },
          { rotulo: 'IMC', valor: n(estado?.imc, 2) },
          { rotulo: 'VET', valor: n(nec?.vet, 0, 'kcal') },
          { rotulo: 'Volume', valor: n(dieta?.volumeTotal, 0, 'ml') },
        ]}
      />

      <Secao titulo="Medidas informadas">
        <LinhasDeValor linhas={medidas} />
      </Secao>

      <Secao
        titulo="Estado nutricional"
        nota="Curvas de crescimento da OMS, faixas P15 e P85. A classificação é a que se leu no dia da avaliação."
      >
        <LinhasDeValor linhas={antropometria} />
      </Secao>

      <Secao
        titulo="Necessidades nutricionais"
        nota="DRIs 2002 (IOM). A faixa de validade vem escrita quando o valor não sai — não é falha, é limite da referência."
      >
        <LinhasDeValor linhas={necessidades} />
      </Secao>

      <Secao titulo="Dieta láctea prescrita">
        <LinhasDeValor linhas={prescricao} />
      </Secao>

      <Observacao texto={observacao} />
    </Folha>
  )
}
