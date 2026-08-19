import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { TButton, TEntry, TPage, TPanel, TSelect } from '@/components/common'
import { handleApiError } from '@/services/api'
import { formulaLacteaService } from '@/services/pediatriaService'
import { paraNumero } from '@/utils/format'

/**
 * O campo continua string no formulário e a conversão acontece no envio — sem
 * `transform` no schema: ele faria o tipo de entrada divergir do de saída, e o
 * resolver do react-hook-form não aceita os dois diferentes.
 *
 * Aceita vírgula ou ponto: o rótulo do produto traz "1,65 g" e é assim que a
 * nutricionista digita.
 */
const numero = (mensagem: string, minimo: number) =>
  z
    .string()
    .min(1, mensagem)
    .refine((v) => {
      const n = paraNumero(v)
      return n !== undefined && n >= minimo
    }, mensagem)

const schema = z.object({
  nome: z.string().min(1, 'Informe o nome da fórmula').max(255, 'No máximo 255 caracteres'),
  kcalPor100ml: numero('Informe as calorias por 100 ml, maiores que zero', 0.001),
  proteinaPor100ml: numero('Informe a proteína por 100 ml', 0),
  ativo: z.string(),
})

type FormData = z.infer<typeof schema>

const VAZIO: FormData = {
  nome: '',
  kcalPor100ml: '',
  proteinaPor100ml: '',
  ativo: 'true',
}

export function FormulaLacteaForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [carregando, setCarregando] = useState(editando)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: VAZIO,
  })

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    formulaLacteaService
      .findById(id)
      .then((f) =>
        reset({
          nome: f.nome,
          kcalPor100ml: String(f.kcalPor100ml).replace('.', ','),
          proteinaPor100ml: String(f.proteinaPor100ml).replace('.', ','),
          ativo: String(f.ativo),
        }),
      )
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  async function salvar(dados: FormData) {
    const composicao = {
      nome: dados.nome.trim(),
      kcalPor100ml: paraNumero(dados.kcalPor100ml)!,
      proteinaPor100ml: paraNumero(dados.proteinaPor100ml)!,
    }

    try {
      if (id) {
        await formulaLacteaService.update(id, composicao)
        // `ativo` tem endpoint próprio — o PUT não o carrega
        await formulaLacteaService.alterarAtivo(id, dados.ativo === 'true')
        toast.success('Fórmula alterada')
      } else {
        await formulaLacteaService.create({ ...composicao, ativo: dados.ativo === 'true' })
        toast.success('Fórmula cadastrada')
      }
      navigate('/app/pediatria/formulas-lacteas')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  if (carregando) {
    return (
      <TPage title="Fórmula láctea">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  return (
    <TPage
      title={editando ? 'Editar fórmula láctea' : 'Nova fórmula láctea'}
      subtitle="A composição é a do rótulo, sempre por 100 ml."
    >
      <form onSubmit={handleSubmit(salvar)} noValidate className="flex flex-col gap-5">
        <TPanel>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Nome"
              autoFocus
              placeholder="Ex.: NAN 2"
              className="lg:col-span-2"
              error={errors.nome?.message}
              {...register('nome')}
            />
            <TEntry
              label="Calorias"
              suffix="kcal / 100 ml"
              inputMode="decimal"
              placeholder="Ex.: 73,8"
              error={errors.kcalPor100ml?.message}
              {...register('kcalPor100ml')}
            />
            <TEntry
              label="Proteína"
              suffix="g / 100 ml"
              inputMode="decimal"
              placeholder="Ex.: 1,65"
              error={errors.proteinaPor100ml?.message}
              {...register('proteinaPor100ml')}
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

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <TButton
            type="button"
            variant="secondary"
            className="sm:w-auto"
            onClick={() => navigate('/app/pediatria/formulas-lacteas')}
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
