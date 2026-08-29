import { BarrasComZero } from '@/components/graficos/BarrasComZero'
import { SerieNoTempo } from '@/components/graficos/SerieNoTempo'
import { TituloGrafico } from '@/components/graficos/chrome'
import {
  OLIGURIA_ML_KG_H,
  PAM_ALVO_MMHG,
  REFERENCIAS,
  type ChaveReferencia,
} from '@/components/graficos/referencias'
import { MetaVersusOfertado } from '@/components/graficos/MetaVersusOfertado'
import type { DiaPlotado } from '@/components/uti/graficos/dias'
import { formatarData } from '@/utils/format'

const rotuloDia = (d: { data: string }) => formatarData(d.data)

/**
 * Volume prescrito contra recebido, em ml — **um eixo**.
 *
 * O eroERP põe ml e percentual juntos com dois eixos Y
 * (`...AcompanhamentoDashboard.tsx:255-265`), e aí "a linha passou da barra"
 * depende de qual escala se está lendo. Aqui o percentual tem gráfico próprio,
 * logo abaixo.
 */
export function VolumeNoTempo({ dias }: { dias: DiaPlotado[] }) {
  return (
    <MetaVersusOfertado
      dados={dias}
      chaveX="data"
      chaveMeta="volPrescrito24h"
      chaveOfertado="volRecebido24h"
      titulo="Volume de dieta"
      unidade="ml"
      referencia="Quando o dia não traz prescrito próprio, a referência é o volume da avaliação vigente."
      rotuloX={rotuloDia}
      vazio="Nenhum dia com volume informado no período."
    />
  )
}

/**
 * Os nove analitos, em **small multiples** de duas colunas.
 *
 * O eroERP resolve isso com dois gráficos de eixo duplo — um para
 * `K/Mg/lactato/PCR` contra sódio, outro para `pCO₂/HCO₃` contra pH. Além do
 * eixo duplo, aquilo obriga quatro grandezas de escalas diferentes a dividir um
 * eixo, e o resultado é que **nenhuma** delas pode ter a sua faixa de
 * referência desenhada. Aqui cada uma tem a sua escala e a sua faixa.
 *
 * Só aparece o analito que tem ao menos um dia informado — quadro vazio é
 * ruído, e o painel de um paciente costuma ter três ou quatro dos nove.
 */
export function LaboratorioNoTempo({ dias }: { dias: DiaPlotado[] }) {
  const chaves = Object.keys(REFERENCIAS) as ChaveReferencia[]
  const comDado = chaves.filter((c) => dias.some((d) => d[c] != null))

  if (comDado.length === 0) {
    return (
      <div>
        <TituloGrafico>Laboratório</TituloGrafico>
        <p className="rounded-md border border-dashed border-line px-4 py-8 text-center text-caption text-txt-muted">
          Nenhum exame informado nos dias do período.
        </p>
      </div>
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      {comDado.map((chave) => (
        <SerieNoTempo
          key={chave}
          dados={dias}
          chaveX="data"
          chaveY={chave}
          titulo={REFERENCIAS[chave].rotulo}
          referencia={REFERENCIAS[chave]}
          casas={REFERENCIAS[chave].max < 10 ? 2 : 0}
          rotuloX={rotuloDia}
        />
      ))}
    </div>
  )
}

/**
 * Diurese em ml/kg/h, com a linha de oligúria.
 *
 * **0,5 ml/kg/h** é o corte do KDIGO 2012 — e é ele que justifica o sistema ter
 * calculado ml/kg/h em vez de deixar o volume bruto, que não diz nada sem o
 * peso ao lado. O gráfico não existe no eroERP.
 */
export function DiureseNoTempo({ dias }: { dias: DiaPlotado[] }) {
  return (
    <SerieNoTempo
      dados={dias}
      chaveX="data"
      chaveY="diuresePorQuiloHora"
      titulo="Diurese"
      referencia={{
        rotulo: 'Diurese',
        unidade: 'ml/kg/h',
        forma: 'limiarSuperior',
        max: OLIGURIA_ML_KG_H,
        fonte: 'Oligúria: < 0,5 ml/kg/h por 6 h ou mais (KDIGO 2012). Abaixo da linha é o alerta.',
      }}
      casas={2}
      rotuloX={rotuloDia}
      altura={240}
      vazio="Nenhum dia com diurese informada — e ela precisa também do peso da avaliação vinculada."
    />
  )
}

/**
 * Pressão arterial média, com o alvo de 65 mmHg da Surviving Sepsis Campaign.
 */
export function PressaoNoTempo({ dias }: { dias: DiaPlotado[] }) {
  return (
    <SerieNoTempo
      dados={dias}
      chaveX="data"
      chaveY="pam"
      titulo="Pressão arterial média"
      referencia={{
        rotulo: 'PAM',
        unidade: 'mmHg',
        forma: 'limiarSuperior',
        max: PAM_ALVO_MMHG,
        fonte: 'Alvo de PAM ≥ 65 mmHg na sepse (Surviving Sepsis Campaign). Calculada de sistólica e diastólica.',
      }}
      casas={0}
      rotuloX={rotuloDia}
      altura={240}
      vazio="Nenhum dia com sistólica e diastólica informadas — a PAM precisa das duas."
    />
  )
}

/** Balanço hídrico: barra com linha de zero. Ver `BarrasComZero`. */
export function BalancoNoTempo({ dias }: { dias: DiaPlotado[] }) {
  return (
    <BarrasComZero
      dados={dias}
      chaveX="data"
      chaveY="balancoHidricoMl"
      titulo="Balanço hídrico"
      unidade="ml"
      referencia="Saldo de 24 h. Positivo é retenção, negativo é perda — nenhum dos dois é bom ou ruim por si."
      rotuloX={rotuloDia}
    />
  )
}
