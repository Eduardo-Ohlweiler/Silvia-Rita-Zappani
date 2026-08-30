import {
  Folha,
  LinhasDeValor,
  Secao,
  TabelaDoc,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { ResultadoFerramentas } from '@/types/uti'
import { formatarNumero } from '@/utils/format'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? '—' : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

export type AbaFerramenta = 'noradrenalina' | 'balanco' | 'propofol' | 'artesanal'

const TITULO: Record<AbaFerramenta, string> = {
  noradrenalina: 'Dose de noradrenalina',
  balanco: 'Balanço nitrogenado',
  propofol: 'Aporte calórico do propofol',
  artesanal: 'Dieta artesanal — sistema aberto',
}

/**
 * A ferramenta clínica no papel — **a folha da aba aberta, e só dela**.
 *
 * As quatro ferramentas não têm nada em comum além de morarem na mesma tela:
 * são quatro conferências independentes, e imprimir as quatro juntas produziria
 * três páginas de traços para quem calculou uma. Por isso o documento recebe a
 * aba ativa e monta **uma folha só**.
 *
 * **A dieta artesanal é a que existe para sair no papel.** As outras três são
 * conta de beira de leito, conferidas na tela e esquecidas; a artesanal produz
 * uma *receita* e uma lista de *embalagens por mês* — papel que sai da tela e
 * vai para a cozinha e para a compra. É a única aba cujo resultado alguém
 * precisa levar na mão.
 *
 * Nada aqui é gravado: a folha traz a data de emissão e diz isso, para não ser
 * confundida com prontuário.
 */
export function DocumentoFerramentaClinica({
  aba,
  resultado,
}: {
  aba: AbaFerramenta
  resultado: ResultadoFerramentas
}) {
  return (
    <Folha
      titulo={TITULO[aba]}
      referencia="Conferência de beira de leito — não faz parte do prontuário e não foi gravada."
    >
      {aba === 'noradrenalina' && <Noradrenalina r={resultado.noradrenalina} />}
      {aba === 'balanco' && <Balanco r={resultado.balancoNitrogenado} />}
      {aba === 'propofol' && <Propofol r={resultado.propofol} />}
      {aba === 'artesanal' && <Artesanal r={resultado.artesanal} />}
    </Folha>
  )
}

function Noradrenalina({ r }: { r: ResultadoFerramentas['noradrenalina'] }) {
  const linhas: LinhaValor[] = [
    {
      rotulo: 'Dose infundida',
      valor: n(r.doseMcgKgMin, 6, 'mcg/kg/min'),
      detalhe: r.doseMcgKgMin == null ? (r.motivo ?? '') : 'É esta a dose que se titula',
    },
    {
      rotulo: 'Concentração da bolsa',
      valor: n(r.concentracaoMcgMl, 2, 'mcg/ml'),
      detalhe: 'O número que se confere na etiqueta antes de infundir',
    },
    { rotulo: 'Preparo', valor: r.preparoDescricao ?? '—' },
  ]

  return (
    <Secao
      titulo="Resultado"
      nota="A ampola brasileira é de 4 mg em 4 ml. A dose sai da concentração da bolsa e da vazão — trocar o preparo sem refazer a conta muda a dose sem mudar a vazão."
    >
      <LinhasDeValor linhas={linhas} />
    </Secao>
  )
}

function Balanco({ r }: { r: ResultadoFerramentas['balancoNitrogenado'] }) {
  const linhas: LinhaValor[] = [
    {
      rotulo: 'Nitrogênio ingerido',
      valor: n(r.nitrogenioIngerido, 4, 'g'),
      detalhe: 'Proteína ÷ 6,25',
    },
    {
      rotulo: 'Nitrogênio excretado',
      valor: n(r.nitrogenioExcretado, 4, 'g'),
      detalhe: 'Ureia urinária ÷ 2,14, mais 4 g de perdas insensíveis',
    },
    {
      rotulo: 'Balanço',
      valor: n(r.balanco, 4, 'g/dia'),
      detalhe: r.classificacao?.rotulo ?? r.motivo ?? '',
    },
  ]

  return (
    <>
      <TiraIndicadores
        itens={[
          { rotulo: 'Balanço', valor: n(r.balanco, 2, 'g') },
          { rotulo: 'Situação', valor: r.classificacao?.rotulo ?? '—' },
        ]}
      />
      <Secao
        titulo="Resultado"
        nota="Balanço negativo em doente crítico é esperado na fase aguda, e não indica sozinho que a oferta proteica está errada — é dado de tendência, lido em série."
      >
        <LinhasDeValor linhas={linhas} />
      </Secao>
    </>
  )
}

function Propofol({ r }: { r: ResultadoFerramentas['propofol'] }) {
  const linhas: LinhaValor[] = [
    {
      rotulo: 'Aporte calórico',
      valor: n(r.kcalDia, 2, 'kcal/dia'),
      detalhe: r.kcalDia == null ? (r.motivo ?? '') : 'A emulsão a 1 % entrega 1,1 kcal/ml',
    },
    {
      rotulo: 'Horas consideradas',
      valor: n(r.horasConsideradas, 1, 'h'),
      detalhe: 'Explícito de propósito: na planilha as 24 h ficavam escondidas na fórmula',
    },
  ]

  return (
    <Secao
      titulo="Resultado"
      nota="Estas calorias são lipídio, e entram no VET do dia. Prescrever a dieta sem descontá-las é a causa mais comum de superalimentação em UTI."
    >
      <LinhasDeValor linhas={linhas} />
    </Secao>
  )
}

function Artesanal({ r }: { r: ResultadoFerramentas['artesanal'] }) {
  const composicao: LinhaValor[] = [
    {
      rotulo: 'Energia total',
      valor: n(r.kcalTotal, 2, 'kcal/dia'),
      detalhe:
        r.kcalPorMacros != null
          ? `Pela soma dos macros: ${n(r.kcalPorMacros, 2)} kcal — os dois números têm de bater`
          : (r.motivo ?? ''),
    },
    { rotulo: 'Energia da base', valor: n(r.kcalBase, 2, 'kcal') },
    { rotulo: 'Doses da base', valor: n(r.dosesBase, 4) },
    { rotulo: 'Carboidrato', valor: n(r.choTotal, 2, 'g'), detalhe: pct(r.percChoSobreVet, r.percChoSobreOfertado) },
    { rotulo: 'Proteína', valor: n(r.ptnTotal, 2, 'g'), detalhe: pct(r.percPtnSobreVet, r.percPtnSobreOfertado) },
    { rotulo: 'Lipídio', valor: n(r.lipTotal, 2, 'g'), detalhe: pct(r.percLipSobreVet, r.percLipSobreOfertado) },
    { rotulo: 'Energia por quilo', valor: n(r.kcalPorQuilo, 2, 'kcal/kg') },
    { rotulo: 'Proteína por quilo', valor: n(r.proteinaPorQuilo, 2, 'g/kg') },
  ]

  const preparo: LinhaValor[] = [
    { rotulo: 'Água total', valor: n(r.aguaTotal, 2, 'ml/dia') },
    { rotulo: 'Administrações por dia', valor: n(r.administracoesPorDia, 0) },
    {
      rotulo: 'Água por administração',
      valor: n(r.aguaPorAdministracao, 2, 'ml'),
      detalhe: 'É este o volume que se completa em cada preparo',
    },
  ]

  return (
    <>
      <TiraIndicadores
        itens={[
          { rotulo: 'Energia', valor: n(r.kcalTotal, 0, 'kcal') },
          { rotulo: 'kcal/kg', valor: n(r.kcalPorQuilo, 1) },
          { rotulo: 'Proteína', valor: n(r.ptnTotal, 1, 'g') },
          { rotulo: 'g PTN/kg', valor: n(r.proteinaPorQuilo, 2) },
          { rotulo: 'Água', valor: n(r.aguaTotal, 0, 'ml') },
        ]}
      />

      {/* A receita vem primeiro: é o que a cozinha lê, e o resto é conferência. */}
      <Secao
        titulo="Receita por administração"
        nota="Medidas por administração, e o total do dia ao lado. A medida é a do próprio produto — cada insumo traz a sua no cadastro, e trocar de marca muda a medida."
      >
        <TabelaDoc
          linhas={r.receitaPorAdministracao}
          chaveDe={(i) => i.produto}
          vazio="Escolha os insumos e as medidas para gerar a receita."
          colunas={[
            { titulo: 'Produto', celula: (i) => i.produto },
            {
              titulo: 'Por administração',
              numerica: true,
              celula: (i) => n(i.medidasPorAdministracao, 2),
            },
            { titulo: 'Por dia', numerica: true, celula: (i) => n(i.medidasPorDia, 2) },
          ]}
        />
      </Secao>

      <Secao titulo="Preparo">
        <LinhasDeValor linhas={preparo} />
      </Secao>

      <Secao
        titulo="Composição"
        nota="A energia total e a soma dos macros por Atwater saem lado a lado de propósito: se divergirem, algum insumo está com a composição errada no cadastro."
      >
        <LinhasDeValor linhas={composicao} />
      </Secao>

      <Secao
        titulo="Embalagens por mês"
        /*
         * A fração NÃO é arredondada aqui, e a nota tem de dizer isso: o
         * servidor devolve o consumo, não o pedido. "14,33 latas" significa
         * comprar 15 e sobrar dois terços para o mês seguinte — arredondar no
         * papel esconderia a sobra e faria o estoque crescer sem ninguém ver.
         */
        nota="É o consumo, não o pedido: 14,33 latas significam comprar 15 e passar a sobra para o mês seguinte. Arredondar aqui esconderia o que sobra."
      >
        <TabelaDoc
          linhas={r.embalagensPorMes}
          chaveDe={(e) => e.produto}
          vazio="Sem insumos escolhidos."
          colunas={[
            { titulo: 'Produto', celula: (e) => e.produto },
            { titulo: 'Embalagens', numerica: true, celula: (e) => n(e.quantidade, 2) },
          ]}
        />
      </Secao>
    </>
  )
}

/** "42,1 % do VET · 40,0 % do ofertado" — os dois denominadores, nomeados. */
function pct(sobreVet?: number, sobreOfertado?: number): string {
  const partes = [
    sobreVet != null ? `${formatarNumero(sobreVet, 1, 1)} % do VET` : null,
    sobreOfertado != null ? `${formatarNumero(sobreOfertado, 1, 1)} % do ofertado` : null,
  ].filter(Boolean)
  return partes.join(' · ')
}
