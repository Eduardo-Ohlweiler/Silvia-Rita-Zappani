import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { ContagemRotuladaUti, ContagemUti, DashboardUti } from '@/types/uti'
import { formatarNumero } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/** Distribuição em tabela: rótulo, quantidade e o peso relativo em percentual. */
function Distribuicao({
  linhas,
}: {
  linhas: (ContagemUti | ContagemRotuladaUti)[]
}) {
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
          celula: (l) => (total > 0 ? `${formatarNumero((l.quantidade / total) * 100, 1, 1)} %` : '—'),
        },
      ]}
    />
  )
}

/**
 * A visão gerencial no papel — **um demonstrativo do período**, não a captura
 * dos gráficos.
 *
 * O gráfico é para varrer a tela; a folha é para levar a uma reunião. Por isso
 * cada distribuição vira tabela com **participação percentual** ao lado da
 * contagem: no papel, "18 avaliações" só significa alguma coisa se o leitor
 * souber que são 60 % do período, e essa conta não deve ficar por conta de quem
 * lê.
 */
export function DocumentoDashboardUti({
  dados,
  periodo,
}: {
  dados: DashboardUti
  /** O recorte que estava aplicado, por extenso. */
  periodo: string
}) {
  const medias: LinhaValor[] = [
    { rotulo: 'Idade média', valor: n(dados.idadeMediaAnos, 1, 'anos') },
    { rotulo: 'Peso de trabalho médio', valor: n(dados.pesoTrabalhoMedio, 2, 'kg') },
    { rotulo: 'IMC médio', valor: n(dados.imcMedio, 2) },
    {
      rotulo: 'Em eutrofia',
      valor: n(dados.percEutrofia, 1, '%'),
      detalhe: 'Sobre as avaliações classificadas — não classificado não é inadequado',
    },
    { rotulo: 'Meta energética média', valor: n(dados.metaEnergeticaMedia, 0, 'kcal/dia') },
    { rotulo: 'Meta proteica média', valor: n(dados.metaProteicaMedia, 1, 'g/dia') },
    { rotulo: 'Energia prescrita por quilo', valor: n(dados.kcalPorQuiloMedio, 1, 'kcal/kg') },
    { rotulo: 'Proteína prescrita por quilo', valor: n(dados.proteinaPorQuiloMedio, 2, 'g/kg') },
    {
      rotulo: 'Adesão média à dieta',
      valor: n(dados.adesaoMedia, 1, '%'),
      detalhe: 'Não é nota: a meta é progressiva na primeira semana (ESPEN)',
    },
    {
      rotulo: 'Avaliações com correção de obesidade',
      valor: String(dados.avaliacoesObesidade),
      detalhe: 'ASPEN/SCCM 2016 — muda a base do peso',
    },
  ]

  return (
    <Folha
      titulo="Terapia nutricional em números"
      referencia={
        <>
          {periodo} · {dados.totalAvaliacoes} avaliações de {dados.totalPacientes} pacientes ·{' '}
          {dados.totalDiasRegistrados} dias de acompanhamento
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Avaliações', valor: String(dados.totalAvaliacoes) },
          { rotulo: 'Pacientes', valor: String(dados.totalPacientes) },
          { rotulo: 'Dias', valor: String(dados.totalDiasRegistrados) },
          { rotulo: 'IMC médio', valor: n(dados.imcMedio, 2) },
          { rotulo: 'Adesão', valor: n(dados.adesaoMedia, 1, '%') },
        ]}
      />

      <Secao
        titulo="Perfil do período"
        nota="As médias ignoram a avaliação em que o valor não existe — quem não tinha altura não entra na média de IMC."
      >
        <LinhasDeValor linhas={medias} />
      </Secao>

      <Secao titulo="Movimento por mês" nota="Mês sem movimento aparece com zero.">
        <TabelaDoc
          linhas={dados.porPeriodo}
          chaveDe={(p) => p.periodo}
          colunas={[
            { titulo: 'Mês', celula: (p) => p.periodo },
            { titulo: 'Avaliações', numerica: true, celula: (p) => String(p.avaliacoes) },
            { titulo: 'Dias registrados', numerica: true, celula: (p) => String(p.dias) },
          ]}
        />
      </Secao>

      <Secao
        titulo="Estado nutricional por IMC"
        nota="Classificação da OMS 1997, como foi gravada em cada avaliação."
      >
        <Distribuicao linhas={dados.classifImcOms} />
      </Secao>

      <Secao
        titulo="Adequação da circunferência do braço"
        nota="Seis faixas, cortes de Blackburn e Thornton (1979) — docs/10 §2.8."
      >
        <Distribuicao linhas={dados.classifAdequacaoCb} />
      </Secao>

      <Secao titulo="Perda de peso">
        <Distribuicao linhas={dados.classifPerdaPeso} />
      </Secao>

      <Secao
        titulo="Fórmulas prescritas"
        nota="Do retrato gravado na avaliação — a fórmula pode ter saído do catálogo desde então."
      >
        <Distribuicao linhas={dados.porFormula} />
      </Secao>

      <Secao titulo="Fase da terapia">
        <Distribuicao linhas={dados.porFase} />
      </Secao>

      <Secao titulo="Terapia renal substitutiva" nota="Substitui a meta proteica por 1,8 ou 2,0 g/kg.">
        <Distribuicao linhas={dados.porTerapiaRenal} />
      </Secao>

      <Secao titulo="Modo de infusão">
        <Distribuicao linhas={dados.porModoInfusao} />
      </Secao>

      <Secao titulo="Pacientes mais avaliados">
        <TabelaDoc
          linhas={dados.pacientesMaisAvaliados}
          chaveDe={(p) => p.pacienteId}
          vazio="Nenhuma avaliação no período escolhido."
          colunas={[
            { titulo: 'Paciente', celula: (p) => p.pacienteNome },
            { titulo: 'Avaliações', numerica: true, celula: (p) => String(p.avaliacoes) },
            { titulo: 'Dias registrados', numerica: true, celula: (p) => String(p.dias) },
          ]}
        />
      </Secao>
    </Folha>
  )
}
