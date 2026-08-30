import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { FaixaOms, PainelPaciente } from '@/types/pediatria'
import { formatarData, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto as idade } from '@/utils/idade'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * A faixa da OMS por extenso, **e o rótulo depende do índice**.
 *
 * No painel da UTI a trajetória já vem com o rótulo pronto do servidor; aqui
 * vem o enum cru, e escrever `BAIXA` numa folha de prontuário seria despejar
 * nome de constante em cima de quem lê.
 *
 * Mas traduzir `BAIXA` por um texto só seria pior ainda: o `FaixaOms` do
 * backend documenta que *"Baixo peso, Baixa estatura e Magreza são a MESMA
 * faixa em índices diferentes"*. Um mapa único faria a tabela dizer "Elevado"
 * onde a seção de cima, com o rótulo do servidor, diz "Estatura alta" — o mesmo
 * dado com duas palavras na mesma folha. Estes textos são cópia literal de
 * `IndiceOms` do backend.
 */
const ROTULO_FAIXA: Record<'pesoIdade' | 'estaturaIdade' | 'imcIdade', Record<FaixaOms, string>> = {
  pesoIdade: { BAIXA: 'Baixo peso', ADEQUADA: 'Peso adequado', ALTA: 'Acima do peso' },
  estaturaIdade: { BAIXA: 'Baixa estatura', ADEQUADA: 'Adequada', ALTA: 'Estatura alta' },
  imcIdade: { BAIXA: 'Magreza', ADEQUADA: 'IMC adequado', ALTA: 'Sobrepeso' },
}

const faixa = (indice: keyof typeof ROTULO_FAIXA, f?: FaixaOms | null) =>
  f ? ROTULO_FAIXA[indice][f] : '—'

/**
 * O histórico de uma criança — **demonstrativo de crescimento**, não espelho
 * da tela.
 *
 * A tela mostra a curva da OMS com a criança plotada sobre ela; no papel a
 * curva vira tabela, porque é a tabela que sobrevive a uma fotocópia e é ela
 * que se anexa ao prontuário. A pergunta que a folha responde é **para onde
 * esta criança está crescendo**: a situação de hoje no topo, a trajetória em
 * seguida, e por quais fórmulas ela passou.
 *
 * Duas diferenças em relação ao painel da UTI, e as duas vêm da clínica:
 *
 * - **A idade entra em toda linha da trajetória.** Em adulto o peso de hoje se
 *   compara com o de ontem; em criança ele só significa alguma coisa contra a
 *   idade — 9 kg é adequado aos 12 meses e baixo aos 36.
 * - **As três classificações da OMS saem juntas** (peso/idade, estatura/idade
 *   e IMC/idade). Uma criança com IMC adequado e estatura baixa é um achado
 *   clínico; publicar só o IMC o esconderia.
 *
 * Como no painel da UTI, a trajetória vai **da mais recente para a mais
 * antiga**: no papel se lê de cima para baixo procurando o estado atual, ao
 * contrário do eixo do gráfico.
 */
export function DocumentoPainelPacientePediatrico({ dados }: { dados: PainelPaciente }) {
  const ultima = dados.ultima
  const estado = ultima?.resultado.estadoNutricional
  const nec = ultima?.resultado.necessidades
  const dieta = ultima?.resultado.dieta

  const situacao: LinhaValor[] = [
    {
      rotulo: 'Idade na avaliação',
      valor: idade(ultima?.idadeMeses),
    },
    { rotulo: 'Peso', valor: n(ultima?.peso, 3, 'kg') },
    {
      rotulo: 'Estatura',
      valor: n(ultima?.estatura, 1, 'cm'),
      detalhe: ultima?.estatura == null ? 'Não medida — sem ela não há IMC nem estatura/idade' : '',
    },
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
    },
    {
      rotulo: 'IMC para a idade',
      valor: estado?.imcIdade?.rotulo ?? '—',
    },
    {
      rotulo: 'Necessidade energética',
      valor: n(nec?.vet, 0, 'kcal/dia'),
      detalhe: nec?.motivoVet ?? '',
    },
    {
      rotulo: 'Necessidade proteica',
      valor: n(nec?.proteina, 2, 'g/dia'),
      detalhe: nec?.motivoProteina ?? '',
    },
    {
      rotulo: 'Dieta prescrita',
      valor: ultima?.formulaNome ?? '—',
      detalhe:
        dieta?.volumeTotal != null
          ? `${n(dieta.volumeTotal, 0)} ml/dia em ${n(dieta.vezesDia, 1)} tomadas`
          : (dieta?.motivo ?? ''),
    },
    {
      /*
       * Sem VET não há cobertura — e o traço aqui apareceria mudo, logo abaixo
       * de uma dieta que existe. O motivo da necessidade é o motivo desta
       * ausência: é ele que explica por que a conta não fecha.
       */
      rotulo: 'Cobertura da necessidade',
      valor: n(dieta?.percCalorico, 1, '% do VET'),
      detalhe:
        dieta?.percCalorico != null
          ? dieta.percProteico != null
            ? `${n(dieta.percProteico, 1)} % da proteína`
            : ''
          : (nec?.motivoVet ?? dieta?.motivo ?? 'Sem necessidade energética, não há o que cobrir'),
    },
  ]

  // Do mais recente para o mais antigo: no papel se procura o estado de hoje.
  const trajetoria = [...dados.evolucao].reverse()

  return (
    <Folha
      titulo="Histórico de acompanhamento pediátrico"
      paciente={dados.pacienteNome}
      referencia={
        <>
          {dados.totalAvaliacoes} {dados.totalAvaliacoes === 1 ? 'avaliação' : 'avaliações'}
          {dados.primeiraAvaliacao
            ? ` · de ${formatarData(dados.primeiraAvaliacao)} a ${formatarData(dados.ultimaAvaliacao)}`
            : ''}
          {dados.dataNascimento ? ` · nascimento em ${formatarData(dados.dataNascimento)}` : ''}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Idade atual', valor: idade(dados.idadeMesesAtual) },
          { rotulo: 'Avaliações', valor: String(dados.totalAvaliacoes) },
          { rotulo: 'Peso', valor: n(ultima?.peso, 3, 'kg') },
          { rotulo: 'Estatura', valor: n(ultima?.estatura, 1, 'cm') },
          { rotulo: 'IMC', valor: n(estado?.imc, 2) },
        ]}
      />

      <Secao
        titulo={
          dados.ultimaAvaliacao
            ? `Situação na última avaliação — ${formatarData(dados.ultimaAvaliacao)}`
            : 'Situação atual'
        }
        nota="Números da avaliação como ela foi gravada. Onde falta valor, o motivo está ao lado — ausência aqui é informação clínica, não falha."
      >
        {ultima ? (
          <LinhasDeValor linhas={situacao} />
        ) : (
          <p className="folha-vazio">
            Esta criança ainda não tem avaliação pediátrica no período escolhido.
          </p>
        )}
      </Secao>

      <Secao
        titulo="Trajetória de crescimento"
        nota="Da avaliação mais recente para a mais antiga. A idade acompanha cada linha porque em criança o peso só se lê contra ela. Classificação da OMS, 0 a 60 meses."
      >
        <TabelaDoc
          linhas={trajetoria}
          chaveDe={(p) => `${p.dataAvaliacao}-${p.idadeMeses}`}
          vazio="Nenhuma avaliação no período escolhido."
          colunas={[
            { titulo: 'Data', celula: (p) => formatarData(p.dataAvaliacao) },
            // A unidade vai no cabeçalho: "37 m" na célula quebrava em duas
            // linhas e engordava toda a tabela por causa de um espaço.
            { titulo: 'Idade (m)', numerica: true, celula: (p) => String(p.idadeMeses) },
            { titulo: 'Peso', numerica: true, celula: (p) => n(p.peso, 3) },
            { titulo: 'Estatura', numerica: true, celula: (p) => n(p.estatura, 1) },
            { titulo: 'IMC', numerica: true, celula: (p) => n(p.imc, 2) },
            { titulo: 'P/I', celula: (p) => faixa('pesoIdade', p.classifPesoIdade) },
            { titulo: 'E/I', celula: (p) => faixa('estaturaIdade', p.classifEstaturaIdade) },
            { titulo: 'IMC/I', celula: (p) => faixa('imcIdade', p.classifImcIdade) },
            { titulo: 'VET', numerica: true, celula: (p) => n(p.vet, 0) },
            { titulo: 'Ofertado', numerica: true, celula: (p) => n(p.caloriasTotais, 0) },
            { titulo: '% VET', numerica: true, celula: (p) => n(p.percCalorico, 1) },
            { titulo: '% PTN', numerica: true, celula: (p) => n(p.percProteico, 1) },
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
          vazio="Nenhuma avaliação com fórmula láctea escolhida no período."
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
