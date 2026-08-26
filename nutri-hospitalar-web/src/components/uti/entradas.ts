import type { CalculoUtiRequest } from '@/types/uti'
import { paraNumero } from '@/utils/format'

/**
 * As entradas do cálculo da UTI, como a tela as guarda: **texto cru**, do jeito
 * que foi digitado.
 *
 * Guardar como número obrigaria a converter a cada tecla, e "1," viraria 1 no
 * meio da digitação de "1,5". A conversão acontece uma vez, ao montar a
 * requisição.
 *
 * Mora fora do componente porque a calculadora e a avaliação a compartilham —
 * e porque exportar constante junto com componente quebra o fast refresh.
 *
 * **Nenhum campo aparece em duas abas.** A divisão é por entrada, não por
 * assunto: o que corre entre as abas são os resultados.
 */
export interface EntradasUti {
  // ─── Aba 1 · Antropometria ────────────────────────────────────────
  sexo: string
  etnia: string
  idadeAnos: string
  alturaCm: string
  alturaJoelhoCm: string
  circBracoCm: string
  circPanturrilhaCm: string
  circAbdominalCm: string
  pesoAtualKg: string
  pesoUsualKg: string
  janelaPerda: string
  segmentosAmputados: string[]
  populacaoReferencia: string
  origemPesoPreferida: string

  // ─── Aba 2 · Necessidades ─────────────────────────────────────────
  fase: string
  terapiaRenal: string
  kcalPorKgAlvo: string
  proteinaPorKgAlvo: string
  posicaoNaFaixa: string

  // ─── Aba 3 · Dieta enteral ────────────────────────────────────────
  formulaEnteralId: string
  modoInfusao: string
  volumePorTempo: string
  tempo: string

  // ─── Aba 4 · Hidratação ───────────────────────────────────────────
  volumeDietaManualMl: string
}

export const ENTRADAS_UTI_VAZIAS: EntradasUti = {
  sexo: '',
  etnia: '',
  idadeAnos: '',
  alturaCm: '',
  alturaJoelhoCm: '',
  circBracoCm: '',
  circPanturrilhaCm: '',
  circAbdominalCm: '',
  pesoAtualKg: '',
  pesoUsualKg: '',
  janelaPerda: '',
  segmentosAmputados: [],
  populacaoReferencia: '',
  origemPesoPreferida: '',
  fase: '',
  terapiaRenal: '',
  kcalPorKgAlvo: '',
  proteinaPorKgAlvo: '',
  // O topo da faixa é o que a planilha mostra no seu próprio exemplo. Deixar em
  // branco esconderia a escolha, que é justamente o que este campo desfaz.
  posicaoNaFaixa: 'MAXIMO',
  formulaEnteralId: '',
  // A infusão contínua a 22 h/dia é o padrão de UTI (docs/10 §9). Deixar em
  // branco faria a aba da dieta parecer quebrada antes do primeiro toque.
  modoInfusao: 'CONTINUA',
  volumePorTempo: '',
  tempo: '22',
  volumeDietaManualMl: '',
}

/** Nada preenchido além dos padrões: não há o que perguntar ao servidor. */
export function entradasVazias(e: EntradasUti): boolean {
  return Object.entries(e).every(([chave, valor]) => {
    if (chave === 'modoInfusao' || chave === 'tempo' || chave === 'posicaoNaFaixa')
      return true
    return Array.isArray(valor) ? valor.length === 0 : valor === ''
  })
}

/**
 * Repovoa o formulário a partir do que foi gravado.
 *
 * Números viram texto com vírgula — é assim que a nutricionista digitou, e é
 * assim que ela vai reencontrar o campo.
 */
export function paraEntradas(c: CalculoUtiRequest): EntradasUti {
  return {
    sexo: c.sexo ?? '',
    etnia: c.etnia ?? '',
    idadeAnos: texto(c.idadeAnos),
    alturaCm: texto(c.alturaCm),
    alturaJoelhoCm: texto(c.alturaJoelhoCm),
    circBracoCm: texto(c.circBracoCm),
    circPanturrilhaCm: texto(c.circPanturrilhaCm),
    circAbdominalCm: texto(c.circAbdominalCm),
    pesoAtualKg: texto(c.pesoAtualKg),
    pesoUsualKg: texto(c.pesoUsualKg),
    janelaPerda: c.janelaPerda ?? '',
    segmentosAmputados: c.segmentosAmputados ?? [],
    populacaoReferencia: c.populacaoReferencia ?? '',
    origemPesoPreferida: c.origemPesoPreferida ?? '',
    fase: c.fase ?? '',
    terapiaRenal: c.terapiaRenal ?? '',
    kcalPorKgAlvo: texto(c.kcalPorKgAlvo),
    proteinaPorKgAlvo: texto(c.proteinaPorKgAlvo),
    posicaoNaFaixa: c.posicaoNaFaixa ?? 'MAXIMO',
    formulaEnteralId: c.formulaEnteralId ?? '',
    modoInfusao: c.modoInfusao ?? 'CONTINUA',
    volumePorTempo: texto(c.volumePorTempo),
    tempo: texto(c.tempo),
    volumeDietaManualMl: texto(c.volumeDietaManualMl),
  }
}

/** As entradas prontas para o corpo da requisição. */
export function paraRequisicao(e: EntradasUti): CalculoUtiRequest {
  return {
    sexo: (e.sexo as CalculoUtiRequest['sexo']) || null,
    etnia: (e.etnia as CalculoUtiRequest['etnia']) || null,
    idadeAnos: paraNumero(e.idadeAnos) ?? null,
    alturaCm: paraNumero(e.alturaCm) ?? null,
    alturaJoelhoCm: paraNumero(e.alturaJoelhoCm) ?? null,
    circBracoCm: paraNumero(e.circBracoCm) ?? null,
    circPanturrilhaCm: paraNumero(e.circPanturrilhaCm) ?? null,
    circAbdominalCm: paraNumero(e.circAbdominalCm) ?? null,
    pesoAtualKg: paraNumero(e.pesoAtualKg) ?? null,
    pesoUsualKg: paraNumero(e.pesoUsualKg) ?? null,
    janelaPerda: (e.janelaPerda as CalculoUtiRequest['janelaPerda']) || null,
    segmentosAmputados: e.segmentosAmputados as CalculoUtiRequest['segmentosAmputados'],
    populacaoReferencia:
      (e.populacaoReferencia as CalculoUtiRequest['populacaoReferencia']) || null,
    origemPesoPreferida:
      (e.origemPesoPreferida as CalculoUtiRequest['origemPesoPreferida']) || null,
    fase: (e.fase as CalculoUtiRequest['fase']) || null,
    terapiaRenal: (e.terapiaRenal as CalculoUtiRequest['terapiaRenal']) || null,
    kcalPorKgAlvo: paraNumero(e.kcalPorKgAlvo) ?? null,
    proteinaPorKgAlvo: paraNumero(e.proteinaPorKgAlvo) ?? null,
    posicaoNaFaixa: (e.posicaoNaFaixa as CalculoUtiRequest['posicaoNaFaixa']) || null,
    formulaEnteralId: e.formulaEnteralId || null,
    modoInfusao: (e.modoInfusao as CalculoUtiRequest['modoInfusao']) || null,
    volumePorTempo: paraNumero(e.volumePorTempo) ?? null,
    tempo: paraNumero(e.tempo) ?? null,
    volumeDietaManualMl: paraNumero(e.volumeDietaManualMl) ?? null,
  }
}

function texto(valor?: number | null): string {
  return valor == null ? '' : String(valor).replace('.', ',')
}
