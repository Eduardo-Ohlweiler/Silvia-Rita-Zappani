import { useEffect, useState } from 'react'
import {
  TEntry,
  TPage,
  TPanel,
  TResult,
  TResultGroup,
  TResultTable,
  TSelect,
  TTabPanel,
  TTabs,
  type Aba,
  type ColunaResultado,
} from '@/components/common'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { ferramentasClinicasService, produtoNutricionalService } from '@/services/utiService'
import {
  OPCOES_PREPARO_NORA,
  rotuloProduto,
  type EmbalagemMes,
  type ItemReceita,
  type PapelArtesanal,
  type PreparoNoradrenalina,
  type ProdutoNutricionalSelect,
  type ResultadoFerramentas,
} from '@/types/uti'
import { formatarNumero, paraNumero } from '@/utils/format'

const ABA_NORA = 'noradrenalina'
const ABA_BALANCO = 'balanco'
const ABA_PROPOFOL = 'propofol'
const ABA_ARTESANAL = 'artesanal'

/** Texto cru, como foi digitado. A conversão acontece uma vez, no envio. */
interface Entradas {
  noraPesoKg: string
  noraVazaoMlH: string
  noraPreparo: string
  noraAmpolas: string
  noraVolumeSoroMl: string
  balancoProteinaG: string
  balancoUreiaG: string
  propofolVazaoMlH: string
  propofolHoras: string
  artesanalVetKcal: string
  artesanalPesoKg: string
  insumoBaseId: string
  dosesBase: string
  insumoCarboidratoId: string
  medidasCarboidrato: string
  insumoProteinaId: string
  medidasProteina: string
  insumoLipidioId: string
  medidasLipidio: string
  administracoesPorDia: string
}

const VAZIO: Entradas = {
  noraPesoKg: '',
  noraVazaoMlH: '',
  noraPreparo: 'SIMPLES_32',
  noraAmpolas: '',
  noraVolumeSoroMl: '',
  balancoProteinaG: '',
  balancoUreiaG: '',
  propofolVazaoMlH: '',
  // 24 h é o padrão, mas visível: na planilha é constante escondida na fórmula.
  propofolHoras: '24,00',
  artesanalVetKcal: '',
  artesanalPesoKg: '',
  insumoBaseId: '',
  dosesBase: '',
  insumoCarboidratoId: '',
  medidasCarboidrato: '',
  insumoProteinaId: '',
  medidasProteina: '',
  insumoLipidioId: '',
  medidasLipidio: '',
  administracoesPorDia: '4',
}

const PADROES: (keyof Entradas)[] = ['noraPreparo', 'propofolHoras', 'administracoesPorDia']

function vazio(e: Entradas): boolean {
  return (Object.keys(e) as (keyof Entradas)[]).every((k) => PADROES.includes(k) || e[k] === '')
}

/**
 * Ferramentas clínicas — quatro contas de beira de leito, em abas.
 *
 * Nada aqui é gravado. É a tela cujo formato a cliente aprovou, e o desenho das
 * entradas mudou em dois pontos que valem registro:
 *
 * **Noradrenalina pede o peso uma vez.** No eroERP a mesma aba tem dois blocos
 * que pedem peso (`noraPeso` e `ampPeso`) e a mesma vazão com dois nomes
 * (`noraVol` e `ampMlH`) — sete entradas para uma resposta. Aqui são peso, vazão
 * e preparo; ampolas e soro só aparecem quando o preparo não é um dos presets.
 *
 * **Os insumos da dieta artesanal vêm do catálogo.** No eroERP os quatro
 * produtos estão cravados em `calculoSistemaAberto.ts:6-11`. Os quatro *papéis*
 * continuam fixos — base, carboidrato, proteína e lipídio, porque a lógica de
 * cada um é específica —, mas quem os ocupa é o cadastro.
 */
/**
 * O combo de um papel só faz sentido quando há o que escolher.
 *
 * Enquanto o catálogo tem um produto para o papel, o combo fica parecendo
 * quebrado — foi o que aconteceu com a base. Em vez de esconder o campo (o que
 * tiraria a possibilidade de trocar depois de cadastrar), o campo diz por que
 * está assim e onde se cadastra mais.
 */
function ajudaDoPapel(produtos: ProdutoNutricionalSelect[]): string | undefined {
  if (produtos.length === 0) return 'Nenhum produto cadastrado para este papel.'
  if (produtos.length === 1) return 'Único cadastrado — cadastre outros em Suplementos e módulos.'
  return undefined
}

export function FerramentasClinicas() {
  const [aba, setAba] = useState(ABA_NORA)
  const [entradas, setEntradas] = useState<Entradas>(VAZIO)
  const [resultado, setResultado] = useState<ResultadoFerramentas | null>(null)
  const [recalculando, setRecalculando] = useState(false)
  const [insumos, setInsumos] = useState<Record<PapelArtesanal, ProdutoNutricionalSelect[]>>({
    BASE: [],
    CARBOIDRATO: [],
    PROTEINA: [],
    LIPIDIO: [],
  })

  const entradasDebounce = useDebounce(JSON.stringify(entradas), 500)

  useEffect(() => {
    const papeis: PapelArtesanal[] = ['BASE', 'CARBOIDRATO', 'PROTEINA', 'LIPIDIO']
    Promise.all(papeis.map((p) => produtoNutricionalService.insumosPorPapel(p)))
      .then(([base, carboidrato, proteina, lipidio]) =>
        setInsumos({ BASE: base, CARBOIDRATO: carboidrato, PROTEINA: proteina, LIPIDIO: lipidio }),
      )
      .catch(handleApiError)
  }, [])

  useEffect(() => {
    const atual: Entradas = JSON.parse(entradasDebounce)

    if (vazio(atual)) {
      setResultado(null)
      return
    }

    let cancelado = false
    setRecalculando(true)

    ferramentasClinicasService
      .calcular({
        noraPesoKg: paraNumero(atual.noraPesoKg) ?? null,
        noraVazaoMlH: paraNumero(atual.noraVazaoMlH) ?? null,
        noraPreparo: (atual.noraPreparo as PreparoNoradrenalina) || null,
        noraAmpolas: paraNumero(atual.noraAmpolas) ?? null,
        noraVolumeSoroMl: paraNumero(atual.noraVolumeSoroMl) ?? null,
        balancoProteinaG: paraNumero(atual.balancoProteinaG) ?? null,
        balancoUreiaG: paraNumero(atual.balancoUreiaG) ?? null,
        propofolVazaoMlH: paraNumero(atual.propofolVazaoMlH) ?? null,
        propofolHoras: paraNumero(atual.propofolHoras) ?? null,
        artesanalVetKcal: paraNumero(atual.artesanalVetKcal) ?? null,
        artesanalPesoKg: paraNumero(atual.artesanalPesoKg) ?? null,
        insumoBaseId: atual.insumoBaseId || null,
        dosesBase: paraNumero(atual.dosesBase) ?? null,
        insumoCarboidratoId: atual.insumoCarboidratoId || null,
        medidasCarboidrato: paraNumero(atual.medidasCarboidrato) ?? null,
        insumoProteinaId: atual.insumoProteinaId || null,
        medidasProteina: paraNumero(atual.medidasProteina) ?? null,
        insumoLipidioId: atual.insumoLipidioId || null,
        medidasLipidio: paraNumero(atual.medidasLipidio) ?? null,
        administracoesPorDia: paraNumero(atual.administracoesPorDia) ?? null,
      })
      .then((r) => {
        if (!cancelado) setResultado(r)
      })
      .catch((erro) => {
        if (!cancelado) handleApiError(erro)
      })
      .finally(() => {
        if (!cancelado) setRecalculando(false)
      })

    return () => {
      cancelado = true
    }
  }, [entradasDebounce])

  function alterar(campo: keyof Entradas, valor: string) {
    setEntradas((atual) => ({ ...atual, [campo]: valor }))
  }

  const nora = resultado?.noradrenalina
  const balanco = resultado?.balancoNitrogenado
  const propofol = resultado?.propofol
  const arte = resultado?.artesanal

  const preparoProprio = entradas.noraPreparo === 'AMPOLAS_E_SORO'

  const abas: Aba[] = [
    { id: ABA_NORA, rotulo: 'Noradrenalina' },
    { id: ABA_BALANCO, rotulo: 'Balanço nitrogenado' },
    { id: ABA_PROPOFOL, rotulo: 'Propofol' },
    { id: ABA_ARTESANAL, rotulo: 'Dieta artesanal' },
  ]

  return (
    <TPage
      title="Ferramentas clínicas"
      subtitle="Quatro conferências de beira de leito. Nada aqui é gravado."
    >
      <div className="flex flex-col gap-4">
        <TTabs abas={abas} ativa={aba} onChange={setAba} />

        <TPanel>
          {/* ─────────────────────── Noradrenalina ───────────────────── */}
          <TTabPanel id={ABA_NORA} ativa={aba}>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TEntry
                label="Peso"
                suffix="kg"
                mascara="decimal"
                value={entradas.noraPesoKg}
                onChange={(e) => alterar('noraPesoKg', e.target.value)}
              />
              <TEntry
                label="Vazão da bomba"
                suffix="ml/h"
                mascara="decimal"
                value={entradas.noraVazaoMlH}
                onChange={(e) => alterar('noraVazaoMlH', e.target.value)}
              />
              <TSelect
                label="Preparo"
                className="lg:col-span-2"
                opcoes={OPCOES_PREPARO_NORA.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
                ajuda="Os preparos padrão são 2 e 4 ampolas de 4 mg em 250 ml."
                value={entradas.noraPreparo}
                onChange={(e) => alterar('noraPreparo', e.target.value)}
              />

              {/* Só no preparo do serviço: nos presets, ampolas e soro já são
                  conhecidos, e pedi-los seria repetir o que o sistema sabe. */}
              {preparoProprio && (
                <>
                  <TEntry
                    label="Ampolas"
                    suffix="de 4 mg"
                    mascara="decimal"
                    value={entradas.noraAmpolas}
                    onChange={(e) => alterar('noraAmpolas', e.target.value)}
                  />
                  <TEntry
                    label="Volume do soro"
                    suffix="ml"
                    mascara="decimal"
                    ajuda="O volume final da bolsa, já com as ampolas."
                    value={entradas.noraVolumeSoroMl}
                    onChange={(e) => alterar('noraVolumeSoroMl', e.target.value)}
                  />
                </>
              )}
            </div>

            <hr className="my-5 border-line" />

            <TResultGroup titulo="Dose infundida" colunas={2}>
              <TResult
                label="Dose"
                valor={formatarNumero(nora?.doseMcgKgMin, 6)}
                unidade="mcg/kg/min"
                referencia={nora?.preparoDescricao}
                motivoAusencia={nora?.motivo}
                recalculando={recalculando}
              />
              <TResult
                label="Concentração da bolsa"
                valor={formatarNumero(nora?.concentracaoMcgMl)}
                unidade="mcg/ml"
                referencia="confira contra o rótulo da bolsa"
                recalculando={recalculando}
              />
            </TResultGroup>
          </TTabPanel>

          {/* ────────────────── Balanço nitrogenado ──────────────────── */}
          <TTabPanel id={ABA_BALANCO} ativa={aba}>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TEntry
                label="Proteína ingerida em 24 h"
                suffix="g"
                mascara="decimal"
                value={entradas.balancoProteinaG}
                onChange={(e) => alterar('balancoProteinaG', e.target.value)}
              />
              <TEntry
                label="Ureia urinária de 24 h"
                suffix="g"
                mascara="decimal"
                value={entradas.balancoUreiaG}
                onChange={(e) => alterar('balancoUreiaG', e.target.value)}
              />
            </div>

            <hr className="my-5 border-line" />

            <TResultGroup titulo="Balanço de 24 horas">
              <TResult
                label="Balanço"
                valor={formatarNumero(balanco?.balanco)}
                unidade="g N/dia"
                classificacao={balanco?.classificacao}
                motivoAusencia={balanco?.motivo}
                recalculando={recalculando}
              />
              <TResult
                label="Nitrogênio ingerido"
                valor={formatarNumero(balanco?.nitrogenioIngerido)}
                unidade="g/dia"
                referencia="proteína ÷ 6,25"
                recalculando={recalculando}
              />
              <TResult
                label="Nitrogênio excretado"
                valor={formatarNumero(balanco?.nitrogenioExcretado)}
                unidade="g/dia"
                referencia="ureia ÷ 2,14 + 4 de perdas insensíveis"
                recalculando={recalculando}
              />
            </TResultGroup>
          </TTabPanel>

          {/* ───────────────────────── Propofol ──────────────────────── */}
          <TTabPanel id={ABA_PROPOFOL} ativa={aba}>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TEntry
                label="Vazão do propofol"
                suffix="ml/h"
                mascara="decimal"
                value={entradas.propofolVazaoMlH}
                onChange={(e) => alterar('propofolVazaoMlH', e.target.value)}
              />
              <TEntry
                label="Horas de infusão"
                suffix="h"
                mascara="decimal"
                ajuda="Desligado ao meio-dia não entregou 24 horas de caloria."
                value={entradas.propofolHoras}
                onChange={(e) => alterar('propofolHoras', e.target.value)}
              />
            </div>

            <hr className="my-5 border-line" />

            <TResultGroup
              titulo="Calorias do sedativo"
              descricao="Não é caloria prescrita — é caloria que chega, e desconta da meta."
              colunas={2}
            >
              <TResult
                label="Calorias"
                valor={formatarNumero(propofol?.kcalDia)}
                unidade="kcal/dia"
                referencia={
                  propofol?.horasConsideradas
                    ? `emulsão a 1,1 kcal/ml · ${formatarNumero(propofol.horasConsideradas)} h`
                    : 'emulsão lipídica a 1,1 kcal/ml'
                }
                motivoAusencia={propofol?.motivo}
                recalculando={recalculando}
              />
            </TResultGroup>
          </TTabPanel>

          {/* ────────────────────── Dieta artesanal ──────────────────── */}
          <TTabPanel id={ABA_ARTESANAL} ativa={aba}>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TEntry
                label="VET desejado"
                suffix="kcal/dia"
                mascara="decimal"
                value={entradas.artesanalVetKcal}
                onChange={(e) => alterar('artesanalVetKcal', e.target.value)}
              />
              <TEntry
                label="Peso"
                suffix="kg"
                mascara="decimal"
                value={entradas.artesanalPesoKg}
                onChange={(e) => alterar('artesanalPesoKg', e.target.value)}
              />
              <TEntry
                label="Administrações por dia"
                mascara="inteiro"
                ajuda="A receita é dividida por este número."
                value={entradas.administracoesPorDia}
                onChange={(e) => alterar('administracoesPorDia', e.target.value)}
              />
            </div>

            <p className="text-caption mb-2 mt-5 text-txt-secondary">
              Os quatro papéis da receita são fixos; quem os ocupa vem do cadastro de produtos.
            </p>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TSelect
                label="Base"
                vazio="Escolha o produto"
                ajuda={ajudaDoPapel(insumos.BASE)}
                opcoes={insumos.BASE.map((p) => ({ valor: p.id, rotulo: rotuloProduto(p) }))}
                value={entradas.insumoBaseId}
                onChange={(e) => alterar('insumoBaseId', e.target.value)}
              />
              <TEntry
                label="Doses da base"
                mascara="decimal"
                ajuda="Em branco, sugerimos pelo VET."
                value={entradas.dosesBase}
                onChange={(e) => alterar('dosesBase', e.target.value)}
              />
              <TSelect
                label="Carboidrato"
                vazio="Nenhum"
                ajuda={ajudaDoPapel(insumos.CARBOIDRATO)}
                opcoes={insumos.CARBOIDRATO.map((p) => ({ valor: p.id, rotulo: rotuloProduto(p) }))}
                value={entradas.insumoCarboidratoId}
                onChange={(e) => alterar('insumoCarboidratoId', e.target.value)}
              />
              <TEntry
                label="Medidas de carboidrato"
                mascara="decimal"
                value={entradas.medidasCarboidrato}
                onChange={(e) => alterar('medidasCarboidrato', e.target.value)}
              />
              <TSelect
                label="Proteína"
                vazio="Nenhuma"
                ajuda={ajudaDoPapel(insumos.PROTEINA)}
                opcoes={insumos.PROTEINA.map((p) => ({ valor: p.id, rotulo: rotuloProduto(p) }))}
                value={entradas.insumoProteinaId}
                onChange={(e) => alterar('insumoProteinaId', e.target.value)}
              />
              <TEntry
                label="Medidas de proteína"
                mascara="decimal"
                value={entradas.medidasProteina}
                onChange={(e) => alterar('medidasProteina', e.target.value)}
              />
              <TSelect
                label="Lipídio"
                vazio="Nenhum"
                ajuda={ajudaDoPapel(insumos.LIPIDIO)}
                opcoes={insumos.LIPIDIO.map((p) => ({ valor: p.id, rotulo: rotuloProduto(p) }))}
                value={entradas.insumoLipidioId}
                onChange={(e) => alterar('insumoLipidioId', e.target.value)}
              />
              <TEntry
                label="Medidas de lipídio"
                mascara="decimal"
                value={entradas.medidasLipidio}
                onChange={(e) => alterar('medidasLipidio', e.target.value)}
              />
            </div>

            <hr className="my-5 border-line" />

            <div className="flex flex-col gap-6">
              <TResultGroup titulo="Energia e macronutrientes" colunas={4}>
                <TResult
                  label="Doses da base"
                  valor={formatarNumero(arte?.dosesBase)}
                  unidade="medidas/dia"
                  motivoAusencia={arte?.motivo}
                  recalculando={recalculando}
                />
                <TResult
                  label="Calorias totais"
                  valor={formatarNumero(arte?.kcalTotal)}
                  unidade="kcal/dia"
                  referencia="pela composição dos produtos"
                  recalculando={recalculando}
                />
                <TResult
                  label="Calorias por quilo"
                  valor={formatarNumero(arte?.kcalPorQuilo)}
                  unidade="kcal/kg"
                  recalculando={recalculando}
                />
                <TResult
                  label="Proteína por quilo"
                  valor={formatarNumero(arte?.proteinaPorQuilo)}
                  unidade="g/kg"
                  recalculando={recalculando}
                />
                <TResult
                  label="Carboidrato"
                  valor={formatarNumero(arte?.choTotal)}
                  unidade="g/dia"
                  recalculando={recalculando}
                />
                <TResult
                  label="Proteína"
                  valor={formatarNumero(arte?.ptnTotal)}
                  unidade="g/dia"
                  recalculando={recalculando}
                />
                <TResult
                  label="Lipídio"
                  valor={formatarNumero(arte?.lipTotal)}
                  unidade="g/dia"
                  recalculando={recalculando}
                />
                <TResult
                  label="Conferência por Atwater"
                  valor={formatarNumero(arte?.kcalPorMacros)}
                  unidade="kcal/dia"
                  referencia="4·CHO + 4·PTN + 9·LIP — tem de bater com as calorias totais"
                  recalculando={recalculando}
                />
              </TResultGroup>

              <TResultGroup
                titulo="Distribuição calórica"
                descricao="Dois denominadores, nomeados: sobre o VET desejado (o número da planilha, que não soma 100) e sobre o que a receita de fato entrega."
                colunas={3}
              >
                <TResult
                  label="Carboidrato — sobre o ofertado"
                  valor={formatarNumero(arte?.percChoSobreOfertado)}
                  unidade="%"
                  referencia={`sobre o VET: ${formatarNumero(arte?.percChoSobreVet)} %`}
                  recalculando={recalculando}
                />
                <TResult
                  label="Proteína — sobre o ofertado"
                  valor={formatarNumero(arte?.percPtnSobreOfertado)}
                  unidade="%"
                  referencia={`sobre o VET: ${formatarNumero(arte?.percPtnSobreVet)} %`}
                  recalculando={recalculando}
                />
                <TResult
                  label="Lipídio — sobre o ofertado"
                  valor={formatarNumero(arte?.percLipSobreOfertado)}
                  unidade="%"
                  referencia={`sobre o VET: ${formatarNumero(arte?.percLipSobreVet)} %`}
                  recalculando={recalculando}
                />
              </TResultGroup>

              <TResultGroup titulo="Água" colunas={2}>
                <TResult
                  label="Água total"
                  valor={formatarNumero(arte?.aguaTotal)}
                  unidade="ml/dia"
                  recalculando={recalculando}
                />
                <TResult
                  label="Por administração"
                  valor={formatarNumero(arte?.aguaPorAdministracao)}
                  unidade="ml"
                  referencia={
                    arte?.administracoesPorDia
                      ? `dividido em ${arte.administracoesPorDia}×`
                      : undefined
                  }
                  recalculando={recalculando}
                />
              </TResultGroup>

              <TResultTable
                titulo="Receita por administração"
                descricao="Quanto de cada insumo vai em cada uma."
                colunas={COLUNAS_RECEITA}
                linhas={arte?.receitaPorAdministracao ?? []}
                chaveDe={(i) => i.produto}
                recalculando={recalculando}
                vazio="Escolha os insumos para montar a receita."
              />

              <TResultTable
                titulo="Embalagens estimadas por mês"
                descricao="31 dias, pela medida declarada no cadastro de cada produto."
                colunas={COLUNAS_EMBALAGEM}
                linhas={arte?.embalagensPorMes ?? []}
                chaveDe={(e) => e.produto}
                recalculando={recalculando}
                vazio="Cadastre a embalagem fechada dos produtos para estimar a compra."
              />
            </div>
          </TTabPanel>
        </TPanel>

        <p className="text-caption text-txt-muted">
          Os cálculos são refeitos automaticamente ao alterar os campos.
        </p>
      </div>
    </TPage>
  )
}

const COLUNAS_RECEITA: ColunaResultado<ItemReceita>[] = [
  { chave: 'produto', cabecalho: 'Insumo', render: (i) => i.produto },
  {
    chave: 'dia',
    cabecalho: 'Medidas por dia',
    numerica: true,
    render: (i) => formatarNumero(i.medidasPorDia),
  },
  {
    chave: 'adm',
    cabecalho: 'Por administração',
    numerica: true,
    render: (i) => formatarNumero(i.medidasPorAdministracao),
  },
]

const COLUNAS_EMBALAGEM: ColunaResultado<EmbalagemMes>[] = [
  { chave: 'produto', cabecalho: 'Produto', render: (e) => e.produto },
  {
    chave: 'qtd',
    cabecalho: 'Embalagens/mês',
    numerica: true,
    render: (e) => formatarNumero(e.quantidade),
  },
]
