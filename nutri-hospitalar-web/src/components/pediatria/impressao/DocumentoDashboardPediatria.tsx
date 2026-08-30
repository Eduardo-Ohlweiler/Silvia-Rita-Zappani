import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { Contagem, ContagemFaixa, DashboardGeral } from '@/types/pediatria'
import { formatarNumero } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/** Distribuição em tabela: rótulo, quantidade e o peso relativo em percentual. */
function Distribuicao({ linhas }: { linhas: (Contagem | ContagemFaixa)[] }) {
  const total = linhas.reduce((soma, l) => soma + l.quantidade, 0)
  return (
    <TabelaDoc
      linhas={linhas}
      chaveDe={(l) => l.rotulo}
      vazio="Sem dados no período."
      colunas={[
        { titulo: 'Classificação', celula: (l) => l.rotulo },
        { titulo: 'Avaliações', numerica: true, celula: (l) => String(l.quantidade) },
        {
          titulo: 'Participação',
          numerica: true,
          celula: (l) =>
            total > 0 ? `${formatarNumero((l.quantidade / total) * 100, 1, 1)} %` : '—',
        },
      ]}
    />
  )
}

/**
 * A visão gerencial da pediatria no papel — **um demonstrativo do período**,
 * não a captura dos gráficos.
 *
 * O gráfico é para varrer a tela; a folha é para levar a uma reunião. Por isso
 * cada distribuição vira tabela com **participação percentual** ao lado da
 * contagem: no papel, "18 avaliações" só significa alguma coisa se o leitor
 * souber que são 60 % do período, e essa conta não deve ficar por conta de quem
 * lê.
 *
 * **As três classificações da OMS saem inteiras e separadas.** Peso/idade,
 * estatura/idade e IMC/idade respondem a perguntas diferentes: um serviço pode
 * ter IMC adequado na maioria e estatura baixa em metade — desnutrição crônica
 * que o IMC sozinho não mostra. Resumi-las num indicador só apagaria o achado.
 */
export function DocumentoDashboardPediatria({
  dados,
  periodo,
  filtros,
}: {
  dados: DashboardGeral
  /** O recorte de tempo que estava aplicado, por extenso. */
  periodo: string
  /** Os demais filtros aplicados, já por extenso — vazio quando não há. */
  filtros?: string
}) {
  const medias: LinhaValor[] = [
    {
      rotulo: 'Idade média',
      valor: n(dados.idadeMediaMeses, 1, 'meses'),
      detalhe: 'A classificação da OMS vale até 60 meses',
    },
    { rotulo: 'Peso médio', valor: n(dados.pesoMedio, 3, 'kg') },
    {
      rotulo: 'IMC médio',
      valor: n(dados.imcMedio, 2),
      detalhe: 'Só entre quem tinha estatura medida — sem ela não há IMC',
    },
    {
      rotulo: 'Com IMC para a idade adequado',
      valor: n(dados.percImcAdequado, 1, '%'),
      detalhe: 'Sobre as avaliações classificadas — não classificado não é inadequado',
    },
    {
      rotulo: 'Cobertura calórica média',
      valor: n(dados.coberturaCaloricaMedia, 1, '%'),
      detalhe: 'Quanto do VET a dieta prescrita entrega',
    },
  ]

  return (
    <Folha
      titulo="Pediatria em números"
      referencia={
        <>
          {periodo} · {dados.totalAvaliacoes} avaliações de {dados.totalPacientes}{' '}
          {dados.totalPacientes === 1 ? 'criança' : 'crianças'}
          {filtros ? ` · ${filtros}` : ''}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Avaliações', valor: String(dados.totalAvaliacoes) },
          { rotulo: 'Crianças', valor: String(dados.totalPacientes) },
          { rotulo: 'No mês', valor: String(dados.avaliacoesMes) },
          { rotulo: 'Idade média', valor: n(dados.idadeMediaMeses, 1, 'm') },
          { rotulo: 'IMC médio', valor: n(dados.imcMedio, 2) },
        ]}
      />

      <Secao
        titulo="Perfil do período"
        nota="As médias ignoram a avaliação em que o valor não existe — quem não tinha estatura não entra na média de IMC. Contar como zero faria o serviço parecer pior do que é."
      >
        <LinhasDeValor linhas={medias} />
      </Secao>

      <Secao
        titulo="Movimento por mês"
        nota="Mês sem avaliação aparece com zero. Encurtar o intervalo insinuaria atividade que não houve."
      >
        <TabelaDoc
          linhas={dados.porPeriodo}
          chaveDe={(p) => p.periodo}
          vazio="Nenhuma avaliação no período escolhido."
          colunas={[
            { titulo: 'Mês', celula: (p) => p.periodo },
            { titulo: 'Avaliações', numerica: true, celula: (p) => String(p.avaliacoes) },
          ]}
        />
      </Secao>

      <Secao
        titulo="Peso para a idade"
        nota="Classificação da OMS, 0 a 60 meses, como foi gravada em cada avaliação."
      >
        <Distribuicao linhas={dados.classifPesoIdade} />
      </Secao>

      <Secao
        titulo="Estatura para a idade"
        nota="É aqui que a desnutrição crônica aparece: estatura baixa com IMC adequado é criança que parou de crescer, não criança magra."
      >
        <Distribuicao linhas={dados.classifEstaturaIdade} />
      </Secao>

      <Secao titulo="IMC para a idade">
        <Distribuicao linhas={dados.classifImcIdade} />
      </Secao>

      <Secao titulo="Faixa etária">
        <Distribuicao linhas={dados.porFaixaEtaria} />
      </Secao>

      <Secao titulo="Sexo">
        <Distribuicao linhas={dados.porSexo} />
      </Secao>

      <Secao
        titulo="Fórmulas prescritas"
        nota="Do retrato gravado na avaliação — a fórmula pode ter saído do catálogo desde então."
      >
        <Distribuicao linhas={dados.porFormula} />
      </Secao>

      <Secao titulo="Crianças mais avaliadas">
        <TabelaDoc
          linhas={dados.pacientesMaisAvaliados}
          chaveDe={(p) => p.pacienteId}
          vazio="Nenhuma avaliação no período escolhido."
          colunas={[
            { titulo: 'Paciente', celula: (p) => p.pacienteNome },
            { titulo: 'Avaliações', numerica: true, celula: (p) => String(p.avaliacoes) },
          ]}
        />
      </Secao>
    </Folha>
  )
}
