import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import {
  REFERENCIAS,
  pam,
  type ChaveReferencia,
  type Referencia,
} from '@/components/graficos/referencias'
import type { PainelAcompanhamentoUti, RegistroDiarioUtiResponse } from '@/types/uti'
import { formatarData, formatarNumero } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * O acompanhamento de um paciente, como **relatório do período** — não como o
 * formulário de um dia.
 *
 * A diferença que importa: o formulário responde "o que eu digito hoje"; este
 * papel responde "como este paciente evoluiu". Por isso ele abre com o
 * consolidado, mostra a prescrição de referência contra a qual tudo foi
 * comparado, e só então desce para os dias — em três tabelas separadas por
 * assunto (dieta, laboratório, clínica), porque uma tabela de 25 colunas não
 * cabe em A4 nem se lê.
 *
 * Os exames saem **com a faixa de referência na nota da seção**, que é o que
 * transforma "K 3,1" em informação. As faixas e as suas fontes estão em
 * `graficos/referencias.ts` e em `docs/10 §14`.
 */
export function DocumentoAcompanhamento({ dados }: { dados: PainelAcompanhamentoUti }) {
  const dias = dados.dias

  const periodo = [
    dados.de ? `de ${formatarData(dados.de)}` : null,
    dados.ate ? `até ${formatarData(dados.ate)}` : null,
  ]
    .filter(Boolean)
    .join(' ')

  const consolidado: LinhaValor[] = [
    {
      rotulo: 'Adesão média à dieta',
      valor: n(dados.adesaoMedia, 1, '%'),
      detalhe:
        'Meta progressiva na primeira semana (ESPEN): abaixo de 70 % nos primeiros dias é conduta',
    },
    { rotulo: 'Energia recebida', valor: n(dados.caloriasPorQuiloMedia, 1, 'kcal/kg/dia') },
    { rotulo: 'Proteína recebida', valor: n(dados.proteinaPorQuiloMedia, 2, 'g/kg/dia') },
    {
      rotulo: 'Balanço hídrico acumulado',
      valor: n(dados.balancoAcumuladoMl, 0, 'ml'),
      detalhe: 'Soma dos saldos de 24 h do período',
    },
    {
      rotulo: 'Diurese média',
      valor: n(dados.diureseMediaMlKgHora, 2, 'ml/kg/h'),
      detalhe: 'Oligúria abaixo de 0,5 ml/kg/h (KDIGO 2012)',
    },
    { rotulo: 'Ingestão oral média', valor: n(dados.ingestaoOralMedia, 1, '%') },
  ]

  const prescricao: LinhaValor[] = [
    { rotulo: 'Volume prescrito', valor: n(dados.volumePrescritoNaAvaliacao, 0, 'ml/dia') },
    { rotulo: 'Meta energética', valor: n(dados.metaEnergetica, 0, 'kcal/dia') },
    { rotulo: 'Meta proteica', valor: n(dados.metaProteica, 1, 'g/dia') },
  ]

  // Só entra a coluna de exame que tem ao menos um dia informado — nove colunas
  // fixas dariam uma tabela com sete traços por linha.
  const examesUsados = (Object.keys(REFERENCIAS) as ChaveReferencia[]).filter((c) =>
    dias.some((d) => d[c] != null),
  )

  return (
    <Folha
      titulo="Relatório de acompanhamento nutricional"
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
          { rotulo: 'Adesão média', valor: n(dados.adesaoMedia, 1, '%') },
          { rotulo: 'Energia', valor: n(dados.caloriasPorQuiloMedia, 1, 'kcal/kg') },
          { rotulo: 'Proteína', valor: n(dados.proteinaPorQuiloMedia, 2, 'g/kg') },
          { rotulo: 'Balanço', valor: n(dados.balancoAcumuladoMl, 0, 'ml') },
        ]}
      />

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
            ? `Da avaliação de ${formatarData(dados.ultimaAvaliacao)} — é contra ela que a adesão foi medida.`
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
            : neles kcal/kg, proteína por quilo e diurese por quilo não existem, porque dependem do
            peso e da fórmula prescritos. Esses dias ficam fora das médias acima.
          </p>
        </Secao>
      )}

      <Secao titulo="Dieta, dia a dia">
        <TabelaDoc
          linhas={dias}
          chaveDe={(d) => d.id}
          colunas={[
            { titulo: 'Data', celula: (d) => formatarData(d.data) },
            { titulo: 'Dieta', celula: (d) => d.dieta ?? '—' },
            { titulo: 'Prescrito', numerica: true, celula: (d) => n(d.volPrescrito24h, 0) },
            { titulo: 'Recebido', numerica: true, celula: (d) => n(d.volRecebido24h, 0) },
            { titulo: 'Adesão', numerica: true, celula: (d) => n(d.percentualRecebido, 1, '%') },
            { titulo: 'kcal/kg', numerica: true, celula: (d) => n(d.caloriasPorQuilo, 1) },
            { titulo: 'g PTN/kg', numerica: true, celula: (d) => n(d.proteinaPorQuilo, 2) },
            { titulo: 'Oral', numerica: true, celula: (d) => n(d.mediaIngestaoOral, 0, '%') },
          ]}
        />
      </Secao>

      {examesUsados.length > 0 && (
        <Secao
          titulo="Laboratório"
          nota={`Referência — ${examesUsados
            .map((c) => `${REFERENCIAS[c].rotulo} ${faixaEmTexto(c)}`)
            .join(' · ')}. A glicemia é ALVO de terapia intensiva, não faixa de normalidade.`}
        >
          <TabelaDoc
            linhas={dias.filter((d) => examesUsados.some((c) => d[c] != null))}
            chaveDe={(d) => d.id}
            vazio="Nenhum exame informado no período."
            colunas={[
              {
                titulo: 'Data',
                celula: (d: RegistroDiarioUtiResponse) => formatarData(d.data),
              },
              ...examesUsados.map((c) => ({
                titulo: `${REFERENCIAS[c].rotulo}${
                  REFERENCIAS[c].unidade ? ` (${REFERENCIAS[c].unidade})` : ''
                }`,
                numerica: true,
                celula: (d: RegistroDiarioUtiResponse) => n(d[c], REFERENCIAS[c].max < 10 ? 2 : 0),
              })),
            ]}
          />
        </Secao>
      )}

      <Secao
        titulo="Clínica e balanço"
        nota="PAM calculada da sistólica e da diastólica; alvo ≥ 65 mmHg na sepse (Surviving Sepsis Campaign)."
      >
        <TabelaDoc
          linhas={dias}
          chaveDe={(d) => d.id}
          colunas={[
            { titulo: 'Data', celula: (d) => formatarData(d.data) },
            { titulo: 'Suporte', celula: (d) => d.suporteVentilatorioDescricao ?? '—' },
            { titulo: 'FiO₂', numerica: true, celula: (d) => n(d.fio2Perc, 0, '%') },
            {
              titulo: 'PA',
              numerica: true,
              celula: (d) =>
                d.paSistolica != null && d.paDiastolica != null
                  ? `${formatarNumero(d.paSistolica, 0)}/${formatarNumero(d.paDiastolica, 0)}`
                  : '—',
            },
            {
              titulo: 'PAM',
              numerica: true,
              celula: (d) => n(pam(d.paSistolica, d.paDiastolica), 0),
            },
            { titulo: 'Balanço (ml)', numerica: true, celula: (d) => n(d.balancoHidricoMl, 0) },
            { titulo: 'Diurese (ml)', numerica: true, celula: (d) => n(d.diureseMl, 0) },
            { titulo: 'ml/kg/h', numerica: true, celula: (d) => n(d.diuresePorQuiloHora, 2) },
            { titulo: 'Evacuação', celula: (d) => d.evacuacao ?? '—' },
          ]}
        />
      </Secao>
    </Folha>
  )
}

/**
 * A faixa em uma linha, para a nota da seção.
 *
 * O tipo passa por `Referencia` porque `REFERENCIAS` é `as const`: sem isso o
 * TypeScript vê a união dos nove literais e `grave`, que só o lactato tem, não
 * existe no ramo dos outros oito.
 */
function faixaEmTexto(chave: ChaveReferencia): string {
  const r: Referencia = REFERENCIAS[chave]
  if (r.forma === 'limiarSuperior') {
    return r.grave != null
      ? `> ${formatarNumero(r.max, 1)} alerta, > ${formatarNumero(r.grave, 1)} grave`
      : `< ${formatarNumero(r.max, 1)}`
  }
  return `${formatarNumero(r.min ?? 0, 1)}–${formatarNumero(r.max, 1)}`
}
