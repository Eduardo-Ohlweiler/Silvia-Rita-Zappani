import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { TButton, TEntry, TPage, TPanel, TSelect } from '@/components/common'
import { handleApiError } from '@/services/api'
import { formulaEnteralService } from '@/services/utiService'
import { CATEGORIAS_ENTERAIS, type CategoriaFormulaEnteral } from '@/types/uti'
import { formatarNumero, paraNumero } from '@/utils/format'

/**
 * O campo continua string no formulário e a conversão acontece no envio — sem
 * `transform` no schema: ele faria o tipo de entrada divergir do de saída, e o
 * resolver do react-hook-form não aceita os dois diferentes.
 *
 * Aceita vírgula ou ponto: o rótulo do produto traz "1,5 kcal/ml" e é assim que
 * a nutricionista digita.
 */
const numero = (mensagem: string, minimo: number, maximo?: number) =>
  z
    .string()
    .min(1, mensagem)
    .refine((v) => {
      const n = paraNumero(v)
      return n !== undefined && n >= minimo && (maximo === undefined || n <= maximo)
    }, mensagem)

/** Igual ao anterior, mas o vazio é resposta válida. */
const numeroOpcional = (mensagem: string, minimo: number, maximo?: number) =>
  z.string().refine((v) => {
    if (v.trim() === '') return true
    const n = paraNumero(v)
    return n !== undefined && n >= minimo && (maximo === undefined || n <= maximo)
  }, mensagem)

const schema = z.object({
  nome: z.string().min(1, 'Informe o nome da fórmula').max(255, 'No máximo 255 caracteres'),
  categoria: z.string(),
  densidadeKcalMl: numero('Informe a densidade, de 0 a 5 kcal/ml', 0.001, 5),
  proteinaGL: numero('Informe a proteína por litro', 0),
  choGL: numeroOpcional('O carboidrato não pode ser negativo', 0),
  lipGL: numeroOpcional('O lipídio não pode ser negativo', 0),
  fibrasGL: numeroOpcional('As fibras não podem ser negativas', 0),
  potassioMgL: numeroOpcional('O potássio não pode ser negativo', 0),
  osmolaridadeMosmL: numeroOpcional('A osmolaridade não pode ser negativa', 0),
  aguaLivrePerc: numeroOpcional('A água livre vai de 0 a 100 %', 0, 100),
  ativo: z.string(),
})

type FormData = z.infer<typeof schema>

const VAZIO: FormData = {
  nome: '',
  categoria: '',
  densidadeKcalMl: '',
  proteinaGL: '',
  choGL: '',
  lipGL: '',
  fibrasGL: '',
  potassioMgL: '',
  osmolaridadeMosmL: '',
  aguaLivrePerc: '',
  ativo: 'true',
}

/** Vazio vira ausência, não zero: campo em branco é "não informado". */
function opcional(valor: string): number | undefined {
  return valor.trim() === '' ? undefined : paraNumero(valor)
}

function texto(valor: number | undefined | null): string {
  return valor == null ? '' : String(valor).replace('.', ',')
}

export function FormulaEnteralForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [carregando, setCarregando] = useState(editando)

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: VAZIO,
  })

  /*
   * O fechamento energético é conferido no servidor e no banco, mas mostrar
   * aqui evita o ida-e-volta do 400 — e é onde o erro mais comum aparece:
   * composição de frasco de 500 ml lançada como se fosse por litro fecha perto
   * de −50 %.
   */
  const [densidade, proteina, cho, lip] = useWatch({
    control,
    name: ['densidadeKcalMl', 'proteinaGL', 'choGL', 'lipGL'],
  })

  const fechamento = useMemo(() => {
    const d = paraNumero(densidade)
    const p = paraNumero(proteina)
    const c = opcional(cho)
    const l = opcional(lip)
    if (d === undefined || p === undefined || c === undefined || l === undefined || d <= 0) {
      return undefined
    }
    const kcalDeclarada = d * 1000
    const kcalDosMacros = p * 4 + c * 4 + l * 9
    return {
      kcalDeclarada,
      kcalDosMacros,
      desvioPerc: ((kcalDosMacros - kcalDeclarada) / kcalDeclarada) * 100,
    }
  }, [densidade, proteina, cho, lip])

  const fechaOk = fechamento !== undefined && Math.abs(fechamento.desvioPerc) <= 12

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    formulaEnteralService
      .findById(id)
      .then((f) =>
        reset({
          nome: f.nome,
          categoria: f.categoria ?? '',
          densidadeKcalMl: texto(f.densidadeKcalMl),
          proteinaGL: texto(f.proteinaGL),
          choGL: texto(f.choGL),
          lipGL: texto(f.lipGL),
          fibrasGL: texto(f.fibrasGL),
          potassioMgL: texto(f.potassioMgL),
          osmolaridadeMosmL: texto(f.osmolaridadeMosmL),
          aguaLivrePerc: texto(f.aguaLivrePerc),
          ativo: String(f.ativo),
        }),
      )
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  async function salvar(dados: FormData) {
    const composicao = {
      nome: dados.nome.trim(),
      categoria: (dados.categoria || undefined) as CategoriaFormulaEnteral | undefined,
      densidadeKcalMl: paraNumero(dados.densidadeKcalMl)!,
      proteinaGL: paraNumero(dados.proteinaGL)!,
      choGL: opcional(dados.choGL),
      lipGL: opcional(dados.lipGL),
      fibrasGL: opcional(dados.fibrasGL),
      potassioMgL: opcional(dados.potassioMgL),
      osmolaridadeMosmL: opcional(dados.osmolaridadeMosmL),
      aguaLivrePerc: opcional(dados.aguaLivrePerc),
    }

    try {
      if (id) {
        await formulaEnteralService.update(id, composicao)
        // `ativo` tem endpoint próprio — o PUT não o carrega
        await formulaEnteralService.alterarAtivo(id, dados.ativo === 'true')
        toast.success('Fórmula alterada')
      } else {
        await formulaEnteralService.create({ ...composicao, ativo: dados.ativo === 'true' })
        toast.success('Fórmula cadastrada')
      }
      navigate('/app/uti/formulas-enterais')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  if (carregando) {
    return (
      <TPage title="Fórmula enteral">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  return (
    <TPage
      title={editando ? 'Editar fórmula enteral' : 'Nova fórmula enteral'}
      subtitle="A composição é a do rótulo, sempre por litro. Produto em frasco de 500 ml precisa ser convertido: 50 g em 500 ml são 100 g por litro."
    >
      <form onSubmit={handleSubmit(salvar)} noValidate className="flex flex-col gap-5">
        <TPanel title="Identificação">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Nome"
              autoFocus
              placeholder="Ex.: Peptamen Intense"
              className="lg:col-span-2"
              error={errors.nome?.message}
              {...register('nome')}
            />
            <TSelect
              label="Categoria"
              vazio="Não informada"
              opcoes={CATEGORIAS_ENTERAIS.map((c) => ({ valor: c.valor, rotulo: c.rotulo }))}
              ajuda="Só organiza a escolha — não entra em cálculo."
              error={errors.categoria?.message}
              {...register('categoria')}
            />
            <TSelect
              label="Situação"
              opcoes={[
                { valor: 'true', rotulo: 'Ativa' },
                { valor: 'false', rotulo: 'Inativa' },
              ]}
              ajuda="Inativa some da escolha, sem alterar avaliações já feitas."
              error={errors.ativo?.message}
              {...register('ativo')}
            />
          </div>
        </TPanel>

        <TPanel title="Composição por litro">
          <p className="text-caption mb-4 text-txt-secondary">
            Densidade e proteína são obrigatórias — todo o cálculo da dieta sai delas.
          </p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Densidade"
              suffix="kcal / ml"
              inputMode="decimal"
              placeholder="Ex.: 1,5"
              error={errors.densidadeKcalMl?.message}
              {...register('densidadeKcalMl')}
            />
            <TEntry
              label="Proteína"
              suffix="g / L"
              inputMode="decimal"
              placeholder="Ex.: 92"
              error={errors.proteinaGL?.message}
              {...register('proteinaGL')}
            />
            <TEntry
              label="Carboidrato"
              suffix="g / L"
              inputMode="decimal"
              placeholder="Ex.: 131"
              error={errors.choGL?.message}
              {...register('choGL')}
            />
            <TEntry
              label="Lipídio"
              suffix="g / L"
              inputMode="decimal"
              placeholder="Ex.: 70"
              error={errors.lipGL?.message}
              {...register('lipGL')}
            />
          </div>

          {/*
            Conferência de fechamento energético por Atwater. Aparece só quando
            os três macros estão preenchidos, porque é só aí que existe o que
            fechar — e o texto nomeia a causa provável em vez de dizer só "não
            fecha", que mandaria o usuário conferir sete campos.
          */}
          {fechamento && (
            <p
              className={`text-caption mt-4 rounded-md border px-3 py-2 ${
                fechaOk
                  ? 'border-line bg-surface-muted text-txt-secondary'
                  : 'border-danger/40 bg-danger/5 text-danger'
              }`}
            >
              {fechaOk ? (
                <>
                  Confere: os macros somam {formatarNumero(fechamento.kcalDosMacros, 0)} kcal/L e a
                  densidade declara {formatarNumero(fechamento.kcalDeclarada, 0)} kcal/L — desvio de{' '}
                  {formatarNumero(fechamento.desvioPerc, 1)} %.
                </>
              ) : (
                <>
                  A composição não fecha com a densidade: os macros somam{' '}
                  {formatarNumero(fechamento.kcalDosMacros, 0)} kcal/L por Atwater (4·PTN + 4·CHO +
                  9·LIP) e a densidade declara {formatarNumero(fechamento.kcalDeclarada, 0)} kcal/L
                  — desvio de {formatarNumero(fechamento.desvioPerc, 1)} %.{' '}
                  {fechamento.desvioPerc < 0
                    ? 'Confira se a composição está por litro: produto em frasco de 500 ml lançado como litro fecha perto de −50 %.'
                    : 'Confira se algum macro está com o ponto decimal deslocado.'}
                </>
              )}
            </p>
          )}
        </TPanel>

        <TPanel title="Demais dados do rótulo">
          <p className="text-caption mb-4 text-txt-secondary">
            Todos opcionais. Deixar em branco é resposta válida — o cálculo trata a ausência e diz
            o que estimou.
          </p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Fibras"
              suffix="g / L"
              inputMode="decimal"
              placeholder="Ex.: 15"
              error={errors.fibrasGL?.message}
              {...register('fibrasGL')}
            />
            <TEntry
              label="Potássio"
              suffix="mg / L"
              inputMode="decimal"
              placeholder="Ex.: 1250"
              error={errors.potassioMgL?.message}
              {...register('potassioMgL')}
            />
            <TEntry
              label="Osmolaridade"
              suffix="mOsm / L"
              inputMode="decimal"
              placeholder="Ex.: 300"
              error={errors.osmolaridadeMosmL?.message}
              {...register('osmolaridadeMosmL')}
            />
            <TEntry
              label="Água livre"
              suffix="%"
              inputMode="decimal"
              placeholder="Ex.: 76"
              ajuda="Do rótulo. Em branco, o cálculo estima pela densidade e informa que estimou."
              error={errors.aguaLivrePerc?.message}
              {...register('aguaLivrePerc')}
            />
          </div>
        </TPanel>

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <TButton
            type="button"
            variant="secondary"
            className="sm:w-auto"
            onClick={() => navigate('/app/uti/formulas-enterais')}
          >
            Cancelar
          </TButton>
          <TButton type="submit" loading={isSubmitting}>
            Salvar
          </TButton>
        </div>
      </form>
    </TPage>
  )
}
