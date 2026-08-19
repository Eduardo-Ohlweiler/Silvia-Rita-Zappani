import { useEffect, useState } from 'react'
import {
  TEntry,
  TPanel,
  TResult,
  TSelect,
  TTabPanel,
  TTabs,
  type Aba,
} from '@/components/common'
import { type EntradasCalculo } from '@/components/pediatria/entradas'
import { useDebounce } from '@/hooks/useDebounce'
import { handleApiError } from '@/services/api'
import { formulaLacteaService, pediatriaService } from '@/services/pediatriaService'
import { OPCOES_SEXO, type Sexo } from '@/types/pessoa'
import {
  rotuloFormula,
  type FormulaLacteaSelect,
  type ResultadoPediatrico,
} from '@/types/pediatria'
import { formatarNumero, paraNumero } from '@/utils/format'

interface Props {
  entradas: EntradasCalculo
  onChange: (entradas: EntradasCalculo) => void
  /**
   * Resultado já conhecido — o de uma avaliação salva. Enquanto o usuário não
   * mexe em nada, é ele que aparece; ao primeiro toque, o recálculo assume.
   */
  resultadoInicial?: ResultadoPediatrico | null
  /** Conteúdo de uma aba extra, ao fim. A avaliação usa para as observações. */
  abaExtra?: { id: string; rotulo: string; conteudo: React.ReactNode }
}

const ABA_NUTRICIONAL = 'nutricional'
const ABA_DIETA = 'dieta'

/**
 * O miolo das telas de cálculo pediátrico, em abas.
 *
 * É o mesmo componente na calculadora e na avaliação — a diferença entre elas é
 * o que existe em volta (paciente, data, gravação), não a conta. Duas cópias
 * divergiriam, e no eroERP já divergiam.
 *
 * **Cada aba é autocontida**: poucas entradas, uma régua, e os resultados
 * daquelas entradas logo abaixo. Nenhuma entrada aparece em duas abas — VET e
 * necessidade proteica ficam na primeira porque usam idade e peso, exatamente
 * como o estado nutricional.
 *
 * **Nenhuma fórmula é calculada aqui.** As entradas vão para o `/calcular` com
 * 500 ms de debounce e o resultado volta pronto (doc 04 §7).
 */
export function CalculoPediatrico({
  entradas,
  onChange,
  resultadoInicial,
  abaExtra,
}: Props) {
  const [aba, setAba] = useState(ABA_NUTRICIONAL)
  const [formulas, setFormulas] = useState<FormulaLacteaSelect[]>([])
  const [resultado, setResultado] = useState<ResultadoPediatrico | null>(
    resultadoInicial ?? null,
  )
  const [recalculando, setRecalculando] = useState(false)

  const entradasDebounce = useDebounce(JSON.stringify(entradas), 500)

  useEffect(() => {
    formulaLacteaService.select().then(setFormulas).catch(handleApiError)
  }, [])

  useEffect(() => {
    setResultado(resultadoInicial ?? null)
  }, [resultadoInicial])

  useEffect(() => {
    const atual: EntradasCalculo = JSON.parse(entradasDebounce)

    // Sem nada preenchido não há o que perguntar ao servidor.
    const vazio = Object.values(atual).every((v) => v === '')
    if (vazio) {
      setResultado(null)
      return
    }

    let cancelado = false
    setRecalculando(true)

    pediatriaService
      .calcular({
        sexo: (atual.sexo as Sexo) || null,
        idadeMeses: paraNumero(atual.idadeMeses) ?? null,
        peso: paraNumero(atual.peso) ?? null,
        estatura: paraNumero(atual.estatura) ?? null,
        formulaLacteaId: atual.formulaLacteaId || null,
        volumeMl: paraNumero(atual.volumeMl) ?? null,
        frequenciaHoras: paraNumero(atual.frequenciaHoras) ?? null,
      })
      // Respostas fora de ordem sobrescreveriam a mais nova pela mais velha.
      .then((r) => !cancelado && setResultado(r))
      .catch((erro) => !cancelado && handleApiError(erro))
      .finally(() => !cancelado && setRecalculando(false))

    return () => {
      cancelado = true
    }
  }, [entradasDebounce])

  function alterar(campo: keyof EntradasCalculo, valor: string) {
    onChange({ ...entradas, [campo]: valor })
  }

  const estado = resultado?.estadoNutricional
  const necessidades = resultado?.necessidades
  const dieta = resultado?.dieta

  const abas: Aba[] = [
    { id: ABA_NUTRICIONAL, rotulo: 'Estado nutricional e necessidades' },
    { id: ABA_DIETA, rotulo: 'Dieta láctea' },
    ...(abaExtra ? [{ id: abaExtra.id, rotulo: abaExtra.rotulo }] : []),
  ]

  return (
    <div className="flex flex-col gap-4">
      <TTabs abas={abas} ativa={aba} onChange={setAba} />

      <TPanel>
        {/* ─── Estado nutricional e necessidades ───────────────────── */}
        <TTabPanel id={ABA_NUTRICIONAL} ativa={aba}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TSelect
              label="Sexo"
              vazio="Selecione"
              opcoes={OPCOES_SEXO}
              value={entradas.sexo}
              onChange={(e) => alterar('sexo', e.target.value)}
              ajuda="As curvas da OMS são separadas por sexo."
            />
            <TEntry
              label="Idade"
              suffix="meses"
              inputMode="numeric"
              placeholder="Ex.: 8"
              value={entradas.idadeMeses}
              onChange={(e) => alterar('idadeMeses', e.target.value)}
            />
            <TEntry
              label="Peso"
              suffix="kg"
              inputMode="decimal"
              placeholder="Ex.: 9"
              value={entradas.peso}
              onChange={(e) => alterar('peso', e.target.value)}
            />
            <TEntry
              label="Estatura"
              suffix="cm"
              inputMode="decimal"
              placeholder="Ex.: 70"
              value={entradas.estatura}
              onChange={(e) => alterar('estatura', e.target.value)}
            />
          </div>

          <hr className="my-5 border-line" />

          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <TResult
              label="IMC"
              valor={estado?.imc != null ? formatarNumero(estado.imc) : undefined}
              unidade="kg/m²"
              motivoAusencia={estado?.motivoImc}
              recalculando={recalculando}
            />
            <TResult
              label="Peso para a idade"
              classificacao={estado?.pesoIdade}
              referencia="OMS · 0 a 60 meses"
              motivoAusencia={estado?.motivo}
              recalculando={recalculando}
            />
            <TResult
              label="Estatura para a idade"
              classificacao={estado?.estaturaIdade}
              referencia="OMS · 0 a 60 meses"
              motivoAusencia={estado?.motivo}
              recalculando={recalculando}
            />
            <TResult
              label="IMC para a idade"
              classificacao={estado?.imcIdade}
              referencia="OMS · 0 a 60 meses"
              motivoAusencia={estado?.motivo ?? estado?.motivoImc}
              recalculando={recalculando}
            />
            <TResult
              label="VET"
              valor={
                necessidades?.vet != null ? formatarNumero(necessidades.vet) : undefined
              }
              unidade="kcal/dia"
              referencia="DRIs 2002 · até 35 meses"
              motivoAusencia={necessidades?.motivoVet}
              recalculando={recalculando}
            />
            <TResult
              label="Proteína"
              valor={
                necessidades?.proteina != null
                  ? formatarNumero(necessidades.proteina, 1)
                  : undefined
              }
              unidade="g/dia"
              referencia="DRIs 2002 · até 36 meses"
              motivoAusencia={necessidades?.motivoProteina}
              recalculando={recalculando}
            />
          </div>
        </TTabPanel>

        {/* ─── Dieta láctea ────────────────────────────────────────── */}
        <TTabPanel id={ABA_DIETA} ativa={aba}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TSelect
              label="Fórmula láctea"
              vazio="Selecione"
              className="lg:col-span-2"
              opcoes={formulas.map((f) => ({ valor: f.id, rotulo: rotuloFormula(f) }))}
              value={entradas.formulaLacteaId}
              onChange={(e) => alterar('formulaLacteaId', e.target.value)}
            />
            <TEntry
              label="Volume por tomada"
              suffix="ml"
              inputMode="decimal"
              placeholder="Ex.: 110"
              value={entradas.volumeMl}
              onChange={(e) => alterar('volumeMl', e.target.value)}
            />
            <TEntry
              label="Frequência"
              suffix="horas"
              inputMode="decimal"
              placeholder="Ex.: 3"
              ajuda="Intervalo entre as tomadas, não quantas são."
              value={entradas.frequenciaHoras}
              onChange={(e) => alterar('frequenciaHoras', e.target.value)}
            />
          </div>

          <hr className="my-5 border-line" />

          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <TResult
              label="Tomadas por dia"
              valor={dieta?.vezesDia != null ? formatarNumero(dieta.vezesDia) : undefined}
              unidade="× ao dia"
              motivoAusencia={dieta?.motivo}
              recalculando={recalculando}
            />
            <TResult
              label="Volume total"
              valor={
                dieta?.volumeTotal != null ? formatarNumero(dieta.volumeTotal) : undefined
              }
              unidade="ml/dia"
              motivoAusencia={dieta?.motivo}
              recalculando={recalculando}
            />
            <TResult
              label="Calorias ofertadas"
              valor={
                dieta?.caloriasTotais != null
                  ? formatarNumero(dieta.caloriasTotais)
                  : undefined
              }
              unidade="kcal/dia"
              motivoAusencia={dieta?.motivo}
              recalculando={recalculando}
            />
            <TResult
              label="Proteína ofertada"
              valor={
                dieta?.proteinaTotal != null
                  ? formatarNumero(dieta.proteinaTotal, 2)
                  : undefined
              }
              unidade="g/dia"
              motivoAusencia={dieta?.motivo}
              recalculando={recalculando}
            />
            <TResult
              label="Adequação calórica"
              valor={
                dieta?.percCalorico != null ? formatarNumero(dieta.percCalorico) : undefined
              }
              unidade="%"
              /* Sem dizer de que VET, o percentual não significa nada — e ele
               * é calculado na outra aba. Ver doc 04 §7. */
              referencia={
                necessidades?.vet != null
                  ? `de ${formatarNumero(necessidades.vet)} kcal/dia`
                  : undefined
              }
              motivoAusencia={dieta?.motivo ?? necessidades?.motivoVet}
              recalculando={recalculando}
            />
            <TResult
              label="Adequação proteica"
              valor={
                dieta?.percProteico != null ? formatarNumero(dieta.percProteico) : undefined
              }
              unidade="%"
              referencia={
                necessidades?.proteina != null
                  ? `de ${formatarNumero(necessidades.proteina, 1)} g/dia`
                  : undefined
              }
              motivoAusencia={dieta?.motivo ?? necessidades?.motivoProteina}
              recalculando={recalculando}
            />
          </div>
        </TTabPanel>

        {abaExtra && (
          <TTabPanel id={abaExtra.id} ativa={aba}>
            {abaExtra.conteudo}
          </TTabPanel>
        )}
      </TPanel>

      <p className="text-caption text-txt-muted">
        Cálculos refeitos automaticamente ao alterar os campos, no servidor.
      </p>
    </div>
  )
}
