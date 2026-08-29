import {
  Folha,
  LinhasDeValor,
  Observacao,
  Secao,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import { REFERENCIAS, pam, type ChaveReferencia, type Referencia } from '@/components/graficos/referencias'
import type { RegistroDiarioUtiResponse } from '@/types/uti'
import { formatarData, formatarNumero } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * Um dia de acompanhamento, como **evolução de prontuário**.
 *
 * O formulário tem cinco abas e sessenta campos; a folha tem quatro blocos e só
 * o que foi preenchido. É a diferença entre a ferramenta de digitação e o
 * registro: no papel ninguém precisa saber onde o campo ficava.
 *
 * Cada exame sai **com a sua faixa ao lado**, na coluna de detalhe — é ela que
 * transforma "K 3,1" em informação para quem lê a folha longe do sistema. As
 * fontes estão em `docs/10 §14`.
 */
export function DocumentoRegistroDiario({ registro }: { registro: RegistroDiarioUtiResponse }) {
  const r = registro

  const dieta: LinhaValor[] = [
    { rotulo: 'Dieta', valor: r.dieta ?? '—' },
    { rotulo: 'Volume prescrito em 24 h', valor: n(r.volPrescrito24h, 0, 'ml') },
    { rotulo: 'Volume recebido em 24 h', valor: n(r.volRecebido24h, 0, 'ml') },
    {
      rotulo: 'Adesão',
      valor: n(r.percentualRecebido, 1, '%'),
      detalhe: r.referenciaDoPercentual ?? '',
    },
    {
      rotulo: 'Energia recebida',
      valor: n(r.caloriasRecebidas, 0, 'kcal'),
      detalhe: r.caloriasPorQuilo != null ? `${n(r.caloriasPorQuilo, 1)} kcal/kg` : '',
    },
    {
      rotulo: 'Proteína recebida',
      valor: n(r.proteinaRecebida, 1, 'g'),
      detalhe: r.proteinaPorQuilo != null ? `${n(r.proteinaPorQuilo, 2)} g/kg` : '',
    },
    { rotulo: 'Ingestão oral média', valor: n(r.mediaIngestaoOral, 1, '%') },
  ]

  const refeicoes: LinhaValor[] = [
    { rotulo: 'Café da manhã', valor: n(r.cafeManha, 0, '%') },
    { rotulo: 'Lanche da manhã', valor: n(r.lancheManha, 0, '%') },
    { rotulo: 'Almoço', valor: n(r.almoco, 0, '%') },
    { rotulo: 'Lanche da tarde', valor: n(r.lancheTarde, 0, '%') },
    { rotulo: 'Jantar', valor: n(r.jantar, 0, '%') },
    { rotulo: 'Ceia', valor: n(r.ceia, 0, '%') },
  ].filter((l) => l.valor !== '—')

  // Só o exame que foi feito. Nove linhas com sete traços não é registro.
  const exames: LinhaValor[] = (Object.keys(REFERENCIAS) as ChaveReferencia[])
    .filter((c) => r[c] != null)
    .map((c) => {
      const ref: Referencia = REFERENCIAS[c]
      return {
        rotulo: `${ref.rotulo}${ref.unidade ? ` (${ref.unidade})` : ''}`,
        valor: n(r[c], ref.max < 10 ? 2 : 0),
        detalhe: textoDaFaixa(ref),
      }
    })

  const clinica: LinhaValor[] = [
    {
      rotulo: 'Suporte ventilatório',
      valor: r.suporteVentilatorioDescricao ?? '—',
      detalhe: r.fio2Perc != null ? `FiO₂ ${n(r.fio2Perc, 0, '%')}` : '',
    },
    {
      rotulo: 'Pressão arterial',
      valor:
        r.paSistolica != null && r.paDiastolica != null
          ? `${formatarNumero(r.paSistolica, 0)}/${formatarNumero(r.paDiastolica, 0)} mmHg`
          : '—',
      detalhe:
        pam(r.paSistolica, r.paDiastolica) != null
          ? `PAM ${n(pam(r.paSistolica, r.paDiastolica), 0)} mmHg · alvo ≥ 65 na sepse`
          : '',
    },
    {
      rotulo: 'Balanço hídrico',
      valor: n(r.balancoHidricoMl, 0, 'ml'),
      detalhe: 'Saldo de 24 h',
    },
    {
      rotulo: 'Diurese',
      valor: n(r.diureseMl, 0, 'ml'),
      detalhe:
        r.diuresePorQuiloHora != null
          ? `${n(r.diuresePorQuiloHora, 2)} ml/kg/h · oligúria abaixo de 0,5`
          : '',
    },
    { rotulo: 'Evacuação', valor: r.evacuacao ?? '—' },
  ]

  return (
    <Folha
      titulo="Evolução nutricional diária"
      paciente={r.pessoaNome}
      referencia={
        <>
          {formatarData(r.data)}
          {r.avaliacaoData
            ? ` · prescrição da avaliação de ${formatarData(r.avaliacaoData)}`
            : ' · sem avaliação vinculada'}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Recebido', valor: n(r.volRecebido24h, 0, 'ml') },
          { rotulo: 'Adesão', valor: n(r.percentualRecebido, 1, '%') },
          { rotulo: 'Energia', valor: n(r.caloriasPorQuilo, 1, 'kcal/kg') },
          { rotulo: 'Balanço', valor: n(r.balancoHidricoMl, 0, 'ml') },
          { rotulo: 'Diurese', valor: n(r.diuresePorQuiloHora, 2, 'ml/kg/h') },
        ]}
      />

      <Secao
        titulo="Terapia nutricional enteral"
        nota={
          r.motivoDerivados ??
          'A adesão não é nota de desempenho: a ESPEN recomenda oferta abaixo de 70 % nos primeiros dias.'
        }
      >
        <LinhasDeValor linhas={dieta} />
      </Secao>

      {refeicoes.length > 0 && (
        <Secao titulo="Aceitação da via oral" nota="Percentual aceito em cada refeição.">
          <LinhasDeValor linhas={refeicoes} />
        </Secao>
      )}

      {exames.length > 0 && (
        <Secao
          titulo="Laboratório"
          nota="A glicemia capilar traz o ALVO de terapia intensiva (140–180 mg/dL), não a faixa do adulto saudável."
        >
          <LinhasDeValor linhas={exames} />
        </Secao>
      )}

      <Secao titulo="Clínica e balanço">
        <LinhasDeValor linhas={clinica} />
      </Secao>

      <Observacao texto={r.observacao} />
    </Folha>
  )
}

function textoDaFaixa(r: Referencia): string {
  if (r.forma === 'limiarSuperior') {
    return r.grave != null
      ? `alerta acima de ${formatarNumero(r.max, 1)}, grave acima de ${formatarNumero(r.grave, 1)}`
      : `referência abaixo de ${formatarNumero(r.max, 1)}`
  }
  const faixa = `${formatarNumero(r.min ?? 0, 1)} a ${formatarNumero(r.max, 1)}`
  return r.forma === 'alvo' ? `alvo ${faixa}` : `referência ${faixa}`
}
