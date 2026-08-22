import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { TButton, TEntry, TPage, TPanel, TSelect } from '@/components/common'
import { handleApiError } from '@/services/api'
import { produtoNutricionalService } from '@/services/utiService'
import {
  PAPEIS_ARTESANAIS,
  TIPOS_PRODUTO,
  type PapelArtesanal,
  type TipoProdutoNutricional,
} from '@/types/uti'
import { paraNumero } from '@/utils/format'

const numero = (mensagem: string, minimo: number) =>
  z
    .string()
    .min(1, mensagem)
    .refine((v) => {
      const n = paraNumero(v)
      return n !== undefined && n >= minimo
    }, mensagem)

/** Igual ao anterior, mas o vazio é resposta válida. */
const numeroOpcional = (mensagem: string, minimo: number) =>
  z.string().refine((v) => {
    if (v.trim() === '') return true
    const n = paraNumero(v)
    return n !== undefined && n >= minimo
  }, mensagem)

/**
 * As duas regras que dependem do tipo são conferidas aqui **e** no servidor.
 * Aqui, para o usuário não descobrir no 400; lá, porque validação de cliente não
 * é garantia de nada.
 *
 * `superRefine` em vez de `refine` porque as mensagens precisam cair no campo
 * certo — erro de formulário sem campo fica órfão no topo da tela.
 */
const schema = z
  .object({
    nome: z.string().min(1, 'Informe o nome do produto').max(255, 'No máximo 255 caracteres'),
    tipo: z.string().min(1, 'Informe o tipo do produto'),
    medidaNome: z
      .string()
      .min(1, 'Informe o nome da medida (medida, sachê, frasco, ml)')
      .max(30, 'No máximo 30 caracteres'),
    medidaQtd: numero('Informe a quantidade da medida, maior que zero', 0.001),
    embalagemQtd: numeroOpcional('A embalagem deve ser maior que zero', 0.001),
    kcal: numeroOpcional('As calorias não podem ser negativas', 0),
    proteinaG: numeroOpcional('A proteína não pode ser negativa', 0),
    choG: numeroOpcional('O carboidrato não pode ser negativo', 0),
    acucarG: numeroOpcional('O açúcar não pode ser negativo', 0),
    lipG: numeroOpcional('O lipídio não pode ser negativo', 0),
    sodioMg: numeroOpcional('O sódio não pode ser negativo', 0),
    potassioMg: numeroOpcional('O potássio não pode ser negativo', 0),
    fosforoMg: numeroOpcional('O fósforo não pode ser negativo', 0),
    ferroMg: numeroOpcional('O ferro não pode ser negativo', 0),
    fibrasG: numeroOpcional('As fibras não podem ser negativas', 0),
    osmolaridadeMosmL: numeroOpcional('A osmolaridade não pode ser negativa', 0),
    papelArtesanal: z.string(),
    observacao: z.string().max(255, 'No máximo 255 caracteres'),
    ativo: z.string(),
  })
  .superRefine((dados, ctx) => {
    const artesanal = dados.tipo === 'INSUMO_ARTESANAL'

    if (artesanal && dados.papelArtesanal === '') {
      ctx.addIssue({
        code: 'custom',
        path: ['papelArtesanal'],
        message: 'Insumo artesanal precisa do papel na receita — é ele que faz o insumo calcular',
      })
    }

    // Quem entra em cálculo precisa de composição: sem ela a dose sairia nula
    if (dados.tipo !== '' && dados.tipo !== 'SUPLEMENTO_ORAL') {
      if (dados.kcal.trim() === '') {
        ctx.addIssue({
          code: 'custom',
          path: ['kcal'],
          message: 'Obrigatório neste tipo: ele entra em cálculo',
        })
      }
      if (dados.proteinaG.trim() === '') {
        ctx.addIssue({
          code: 'custom',
          path: ['proteinaG'],
          message: 'Obrigatória neste tipo: ele entra em cálculo',
        })
      }
    }
  })

type FormData = z.infer<typeof schema>

const VAZIO: FormData = {
  nome: '',
  tipo: 'SUPLEMENTO_ORAL',
  medidaNome: '',
  medidaQtd: '',
  embalagemQtd: '',
  kcal: '',
  proteinaG: '',
  choG: '',
  acucarG: '',
  lipG: '',
  sodioMg: '',
  potassioMg: '',
  fosforoMg: '',
  ferroMg: '',
  fibrasG: '',
  osmolaridadeMosmL: '',
  papelArtesanal: '',
  observacao: '',
  ativo: 'true',
}

/** Vazio vira ausência, não zero: campo em branco é "não informado". */
function opcional(valor: string): number | undefined {
  return valor.trim() === '' ? undefined : paraNumero(valor)
}

function texto(valor: number | undefined | null): string {
  return valor == null ? '' : String(valor).replace('.', ',')
}

export function ProdutoNutricionalForm() {
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
   * O papel na receita só aparece em insumo artesanal — nos outros dois tipos
   * ele é proibido pelo banco, e exibir um campo que não teria efeito seria
   * mentir sobre a interface.
   */
  const tipo = useWatch({ control, name: 'tipo' })
  const artesanal = tipo === 'INSUMO_ARTESANAL'
  const entraEmCalculo = tipo !== '' && tipo !== 'SUPLEMENTO_ORAL'

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    produtoNutricionalService
      .findById(id)
      .then((p) =>
        reset({
          nome: p.nome,
          tipo: p.tipo,
          medidaNome: p.medidaNome,
          medidaQtd: texto(p.medidaQtd),
          embalagemQtd: texto(p.embalagemQtd),
          kcal: texto(p.kcal),
          proteinaG: texto(p.proteinaG),
          choG: texto(p.choG),
          acucarG: texto(p.acucarG),
          lipG: texto(p.lipG),
          sodioMg: texto(p.sodioMg),
          potassioMg: texto(p.potassioMg),
          fosforoMg: texto(p.fosforoMg),
          ferroMg: texto(p.ferroMg),
          fibrasG: texto(p.fibrasG),
          osmolaridadeMosmL: texto(p.osmolaridadeMosmL),
          papelArtesanal: p.papelArtesanal ?? '',
          observacao: p.observacao ?? '',
          ativo: String(p.ativo),
        }),
      )
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  async function salvar(dados: FormData) {
    const produto = {
      nome: dados.nome.trim(),
      tipo: dados.tipo as TipoProdutoNutricional,
      medidaNome: dados.medidaNome.trim(),
      medidaQtd: paraNumero(dados.medidaQtd)!,
      embalagemQtd: opcional(dados.embalagemQtd),
      kcal: opcional(dados.kcal),
      proteinaG: opcional(dados.proteinaG),
      choG: opcional(dados.choG),
      acucarG: opcional(dados.acucarG),
      lipG: opcional(dados.lipG),
      sodioMg: opcional(dados.sodioMg),
      potassioMg: opcional(dados.potassioMg),
      fosforoMg: opcional(dados.fosforoMg),
      ferroMg: opcional(dados.ferroMg),
      fibrasG: opcional(dados.fibrasG),
      osmolaridadeMosmL: opcional(dados.osmolaridadeMosmL),
      // Papel só vai quando o tipo o admite — o banco recusa o resto
      papelArtesanal:
        dados.tipo === 'INSUMO_ARTESANAL'
          ? (dados.papelArtesanal as PapelArtesanal)
          : undefined,
      observacao: dados.observacao.trim() || undefined,
    }

    try {
      if (id) {
        await produtoNutricionalService.update(id, produto)
        // `ativo` tem endpoint próprio — o PUT não o carrega
        await produtoNutricionalService.alterarAtivo(id, dados.ativo === 'true')
        toast.success('Produto alterado')
      } else {
        await produtoNutricionalService.create({ ...produto, ativo: dados.ativo === 'true' })
        toast.success('Produto cadastrado')
      }
      navigate('/app/uti/produtos')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  if (carregando) {
    return (
      <TPage title="Produto nutricional">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  return (
    <TPage
      title={editando ? 'Editar produto nutricional' : 'Novo produto nutricional'}
      subtitle="A composição é a do rótulo, por medida. Declare a medida a que ela se refere — cada rótulo usa a sua."
    >
      <form onSubmit={handleSubmit(salvar)} noValidate className="flex flex-col gap-5">
        <TPanel title="Identificação">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Nome"
              autoFocus
              placeholder="Ex.: Trophic Basic"
              className="lg:col-span-2"
              error={errors.nome?.message}
              {...register('nome')}
            />
            <TSelect
              label="Tipo"
              opcoes={TIPOS_PRODUTO.map((t) => ({ valor: t.valor, rotulo: t.rotulo }))}
              ajuda={
                entraEmCalculo
                  ? 'Este tipo entra em cálculo: calorias e proteína são obrigatórias.'
                  : 'Suplemento oral é consulta — aceita composição incompleta.'
              }
              error={errors.tipo?.message}
              {...register('tipo')}
            />
            <TSelect
              label="Situação"
              opcoes={[
                { valor: 'true', rotulo: 'Ativo' },
                { valor: 'false', rotulo: 'Inativo' },
              ]}
              ajuda="Inativo some da escolha, sem alterar avaliações já feitas."
              error={errors.ativo?.message}
              {...register('ativo')}
            />

            {/* Só em insumo artesanal: nos outros o banco o proíbe */}
            {artesanal && (
              <TSelect
                label="Papel na receita"
                vazio="Escolha o papel"
                opcoes={PAPEIS_ARTESANAIS.map((p) => ({ valor: p.valor, rotulo: p.rotulo }))}
                ajuda="Os quatro papéis são fixos; o catálogo diz qual produto ocupa cada um."
                error={errors.papelArtesanal?.message}
                {...register('papelArtesanal')}
              />
            )}
          </div>
        </TPanel>

        <TPanel title="A medida da composição">
          <p className="text-caption mb-4 text-txt-secondary">
            Tudo o que vier abaixo se refere a <strong>uma medida</strong>. A embalagem fechada é
            outro número — e é dela que sai o cálculo de latas por mês.
          </p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Nome da medida"
              placeholder="Ex.: medida"
              error={errors.medidaNome?.message}
              {...register('medidaNome')}
            />
            <TEntry
              label="Quantidade da medida"
              suffix="g ou ml"
              inputMode="decimal"
              placeholder="Ex.: 7,8"
              error={errors.medidaQtd?.message}
              {...register('medidaQtd')}
            />
            <TEntry
              label="Embalagem fechada"
              suffix="g ou ml"
              inputMode="decimal"
              placeholder="Ex.: 800"
              ajuda="Sem ela não há cálculo de latas por mês."
              error={errors.embalagemQtd?.message}
              {...register('embalagemQtd')}
            />
          </div>
        </TPanel>

        <TPanel title="Macronutrientes por medida">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Calorias"
              suffix="kcal"
              inputMode="decimal"
              placeholder="Ex.: 30"
              error={errors.kcal?.message}
              {...register('kcal')}
            />
            <TEntry
              label="Proteína"
              suffix="g"
              inputMode="decimal"
              placeholder="Ex.: 1,2"
              error={errors.proteinaG?.message}
              {...register('proteinaG')}
            />
            <TEntry
              label="Carboidrato"
              suffix="g"
              inputMode="decimal"
              placeholder="Ex.: 5,6"
              error={errors.choG?.message}
              {...register('choG')}
            />
            <TEntry
              label="Açúcar"
              suffix="g"
              inputMode="decimal"
              error={errors.acucarG?.message}
              {...register('acucarG')}
            />
            <TEntry
              label="Lipídio"
              suffix="g"
              inputMode="decimal"
              error={errors.lipG?.message}
              {...register('lipG')}
            />
            <TEntry
              label="Fibras"
              suffix="g"
              inputMode="decimal"
              error={errors.fibrasG?.message}
              {...register('fibrasG')}
            />
          </div>
        </TPanel>

        <TPanel title="Micronutrientes e osmolaridade">
          <p className="text-caption mb-4 text-txt-secondary">
            Todos opcionais — deixar em branco é resposta válida.
          </p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <TEntry
              label="Sódio"
              suffix="mg"
              inputMode="decimal"
              error={errors.sodioMg?.message}
              {...register('sodioMg')}
            />
            <TEntry
              label="Potássio"
              suffix="mg"
              inputMode="decimal"
              error={errors.potassioMg?.message}
              {...register('potassioMg')}
            />
            <TEntry
              label="Fósforo"
              suffix="mg"
              inputMode="decimal"
              error={errors.fosforoMg?.message}
              {...register('fosforoMg')}
            />
            <TEntry
              label="Ferro"
              suffix="mg"
              inputMode="decimal"
              error={errors.ferroMg?.message}
              {...register('ferroMg')}
            />
            <TEntry
              label="Osmolaridade"
              suffix="mOsm / L"
              inputMode="decimal"
              error={errors.osmolaridadeMosmL?.message}
              {...register('osmolaridadeMosmL')}
            />
            <TEntry
              label="Observação"
              className="sm:col-span-2 lg:col-span-3"
              placeholder="Ex.: sabor neutro, isento de lactose"
              error={errors.observacao?.message}
              {...register('observacao')}
            />
          </div>
        </TPanel>

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <TButton
            type="button"
            variant="secondary"
            className="sm:w-auto"
            onClick={() => navigate('/app/uti/produtos')}
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
