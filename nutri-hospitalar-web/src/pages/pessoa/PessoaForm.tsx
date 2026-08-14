import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { IconAdicionar, IconFechar } from '@/assets/icons'
import {
  TButton,
  TEntry,
  TPage,
  TPanel,
  TSelect,
  type OpcaoSelect,
} from '@/components/common'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { OPCOES_TIPO_PESSOA, type TipoPessoa } from '@/types/pessoa'
import {
  mascararCnpj,
  mascararCpf,
  mascararTelefone,
  somenteDigitos,
} from '@/utils/format'

const schema = z.object({
  nome: z.string().min(1, 'Informe o nome').max(255),
  tipoPessoa: z.enum(['PESSOA_FISICA', 'PESSOA_JURIDICA']),
  dataNascimento: z.string().optional(),
  cpf: z.string().optional(),
  rg: z.string().optional(),
  cnpj: z.string().optional(),
  inscricaoEstadual: z.string().optional(),
  inscricaoMunicipal: z.string().optional(),
  nomeFantasia: z.string().optional(),
  razaoSocial: z.string().optional(),
  observacao: z.string().max(500).optional(),
  tiposCadastroIds: z.array(z.string()).min(1, 'Selecione ao menos um tipo de cadastro'),
  telefones: z.array(
    z.object({
      id: z.string().optional(),
      tipoTelefoneId: z.string().min(1, 'Informe o tipo'),
      numero: z.string().min(1, 'Informe o número'),
      observacao: z.string().optional(),
      principal: z.boolean(),
    }),
  ),
  emails: z.array(
    z.object({
      id: z.string().optional(),
      tipoEmailId: z.string().min(1, 'Informe o tipo'),
      email: z.string().min(1, 'Informe o e-mail').email('E-mail inválido'),
      observacao: z.string().optional(),
      principal: z.boolean(),
    }),
  ),
  redesSociais: z.array(
    z.object({
      id: z.string().optional(),
      tipoRedeSocialId: z.string().min(1, 'Informe a rede'),
      usuario: z.string().optional(),
      url: z.string().optional(),
    }),
  ),
})

type FormData = z.infer<typeof schema>

const VAZIO: FormData = {
  nome: '',
  tipoPessoa: 'PESSOA_FISICA',
  tiposCadastroIds: [],
  telefones: [],
  emails: [],
  redesSociais: [],
}

export function PessoaForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [carregando, setCarregando] = useState(editando)
  const [tiposCadastro, setTiposCadastro] = useState<OpcaoSelect[]>([])
  const [tiposTelefone, setTiposTelefone] = useState<OpcaoSelect[]>([])
  const [tiposEmail, setTiposEmail] = useState<OpcaoSelect[]>([])
  const [tiposRedeSocial, setTiposRedeSocial] = useState<OpcaoSelect[]>([])

  const {
    register,
    handleSubmit,
    control,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: VAZIO })

  const tipoPessoa = watch('tipoPessoa')
  const ehFisica = tipoPessoa === 'PESSOA_FISICA'

  const telefones = useFieldArray({ control, name: 'telefones' })
  const emails = useFieldArray({ control, name: 'emails' })
  const redes = useFieldArray({ control, name: 'redesSociais' })

  useEffect(() => {
    const paraOpcoes = (itens: { id: string; nome: string }[]) =>
      itens.map((i) => ({ valor: i.id, rotulo: i.nome }))

    Promise.all([
      catalogoService.tiposCadastro(),
      catalogoService.tiposTelefone(),
      catalogoService.tiposEmail(),
      catalogoService.tiposRedeSocial(),
    ])
      .then(([cadastro, telefone, email, rede]) => {
        setTiposCadastro(paraOpcoes(cadastro))
        setTiposTelefone(paraOpcoes(telefone))
        setTiposEmail(paraOpcoes(email))
        setTiposRedeSocial(paraOpcoes(rede))
      })
      .catch(handleApiError)
  }, [])

  useEffect(() => {
    if (!id) return
    pessoaService
      .findById(id)
      .then((p) =>
        reset({
          nome: p.nome,
          tipoPessoa: p.tipoPessoa,
          dataNascimento: p.dataNascimento ?? '',
          cpf: p.cpf ? mascararCpf(p.cpf) : '',
          rg: p.rg ?? '',
          cnpj: p.cnpj ? mascararCnpj(p.cnpj) : '',
          inscricaoEstadual: p.inscricaoEstadual ?? '',
          inscricaoMunicipal: p.inscricaoMunicipal ?? '',
          nomeFantasia: p.nomeFantasia ?? '',
          razaoSocial: p.razaoSocial ?? '',
          observacao: p.observacao ?? '',
          tiposCadastroIds: p.tiposCadastro.map((t) => t.id),
          telefones: p.telefones.map((t) => ({
            id: t.id,
            tipoTelefoneId: t.tipoTelefoneId,
            numero: mascararTelefone(t.numero),
            observacao: t.observacao ?? '',
            principal: t.principal,
          })),
          emails: p.emails.map((e) => ({
            id: e.id,
            tipoEmailId: e.tipoEmailId,
            email: e.email,
            observacao: e.observacao ?? '',
            principal: e.principal,
          })),
          redesSociais: p.redesSociais.map((r) => ({
            id: r.id,
            tipoRedeSocialId: r.tipoRedeSocialId,
            usuario: r.usuario ?? '',
            url: r.url ?? '',
          })),
        }),
      )
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  /** Principal é exclusivo: marcar um desmarca os outros. */
  function marcarPrincipal(lista: 'telefones' | 'emails', indice: number) {
    const atual = watch(lista)
    atual.forEach((_, i) => setValue(`${lista}.${i}.principal`, i === indice))
  }

  async function onSubmit(dados: FormData) {
    const fisica = dados.tipoPessoa === 'PESSOA_FISICA'

    const payload = {
      nome: dados.nome,
      tipoPessoa: dados.tipoPessoa as TipoPessoa,
      observacao: dados.observacao || undefined,
      tiposCadastroIds: dados.tiposCadastroIds,

      // Campos do outro tipo nem são enviados: o backend os recusa com 400,
      // e mandá-los sujaria a mensagem de erro para quem só trocou o tipo.
      dataNascimento: fisica ? dados.dataNascimento || undefined : undefined,
      cpf: fisica ? somenteDigitos(dados.cpf) : undefined,
      rg: fisica ? dados.rg || undefined : undefined,
      cnpj: fisica ? undefined : somenteDigitos(dados.cnpj),
      inscricaoEstadual: fisica ? undefined : dados.inscricaoEstadual || undefined,
      inscricaoMunicipal: fisica ? undefined : dados.inscricaoMunicipal || undefined,
      nomeFantasia: fisica ? undefined : dados.nomeFantasia || undefined,
      razaoSocial: fisica ? undefined : dados.razaoSocial || undefined,

      telefones: dados.telefones.map((t) => ({
        id: t.id,
        tipoTelefoneId: t.tipoTelefoneId,
        numero: somenteDigitos(t.numero) ?? '',
        observacao: t.observacao || undefined,
        principal: t.principal,
      })),
      emails: dados.emails.map((e) => ({
        id: e.id,
        tipoEmailId: e.tipoEmailId,
        email: e.email,
        observacao: e.observacao || undefined,
        principal: e.principal,
      })),
      redesSociais: dados.redesSociais.map((r) => ({
        id: r.id,
        tipoRedeSocialId: r.tipoRedeSocialId,
        usuario: r.usuario || undefined,
        url: r.url || undefined,
      })),
    }

    try {
      if (editando) {
        await pessoaService.update(id!, payload)
        toast.success('Pessoa salva')
      } else {
        await pessoaService.create(payload)
        toast.success('Pessoa cadastrada')
      }
      navigate('/pessoas')
    } catch (erro) {
      handleApiError(erro)
    }
  }

  if (carregando) {
    return (
      <div className="grid place-items-center py-20">
        <span
          role="status"
          aria-label="Carregando"
          className="size-8 animate-spin rounded-full border-2 border-primary border-t-transparent"
        />
      </div>
    )
  }

  return (
    <TPage title={editando ? 'Editar pessoa' : 'Nova pessoa'}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
        <TPanel title="Identificação">
          <div className="grid gap-4 sm:grid-cols-2">
            <TSelect
              label="Tipo de pessoa"
              opcoes={OPCOES_TIPO_PESSOA}
              ajuda={editando ? 'Trocar o tipo limpa os campos do tipo anterior' : undefined}
              {...register('tipoPessoa')}
            />
            <TEntry
              label={ehFisica ? 'Nome' : 'Razão social'}
              autoFocus
              error={errors.nome?.message}
              {...register('nome')}
            />
          </div>

          {ehFisica ? (
            <div className="mt-4 grid gap-4 sm:grid-cols-3">
              <Controller
                control={control}
                name="cpf"
                render={({ field }) => (
                  <TEntry
                    label="CPF"
                    inputMode="numeric"
                    placeholder="000.000.000-00"
                    value={field.value ?? ''}
                    onChange={(e) => field.onChange(mascararCpf(e.target.value))}
                    error={errors.cpf?.message}
                  />
                )}
              />
              <TEntry label="RG" error={errors.rg?.message} {...register('rg')} />
              <TEntry label="Data de nascimento" type="date" {...register('dataNascimento')} />
            </div>
          ) : (
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Controller
                control={control}
                name="cnpj"
                render={({ field }) => (
                  <TEntry
                    label="CNPJ"
                    inputMode="numeric"
                    placeholder="00.000.000/0000-00"
                    value={field.value ?? ''}
                    onChange={(e) => field.onChange(mascararCnpj(e.target.value))}
                    error={errors.cnpj?.message}
                  />
                )}
              />
              <TEntry label="Nome fantasia" {...register('nomeFantasia')} />
              <TEntry label="Inscrição estadual" {...register('inscricaoEstadual')} />
              <TEntry label="Inscrição municipal" {...register('inscricaoMunicipal')} />
            </div>
          )}
        </TPanel>

        <TPanel title="Classificação">
          <Controller
            control={control}
            name="tiposCadastroIds"
            render={({ field }) => (
              <fieldset className="flex flex-wrap gap-2">
                <legend className="mb-2 text-caption text-txt-secondary">
                  Tipo de cadastro
                </legend>
                {tiposCadastro.map((tipo) => {
                  const marcado = field.value.includes(tipo.valor)
                  return (
                    <label
                      key={tipo.valor}
                      className={`flex cursor-pointer items-center gap-2 rounded-md border px-3 py-2
                        text-body transition-colors
                        ${
                          marcado
                            ? 'border-primary bg-surface-alt text-txt'
                            : 'border-line-strong text-txt-secondary hover:bg-surface-alt'
                        }`}
                    >
                      <input
                        type="checkbox"
                        checked={marcado}
                        onChange={() =>
                          field.onChange(
                            marcado
                              ? field.value.filter((v) => v !== tipo.valor)
                              : [...field.value, tipo.valor],
                          )
                        }
                        className="size-4 shrink-0 accent-(--primary)"
                      />
                      {tipo.rotulo}
                    </label>
                  )
                })}
              </fieldset>
            )}
          />
          {errors.tiposCadastroIds && (
            <p role="alert" className="mt-2 text-caption text-danger">
              {errors.tiposCadastroIds.message}
            </p>
          )}

          <div className="mt-4">
            <TEntry label="Observação" {...register('observacao')} />
          </div>
        </TPanel>

        <TPanel
          title="Telefones"
          actions={
            <BotaoAcrescentar
              onClick={() =>
                telefones.append({
                  tipoTelefoneId: tiposTelefone[0]?.valor ?? '',
                  numero: '',
                  observacao: '',
                  principal: telefones.fields.length === 0,
                })
              }
            />
          }
        >
          {telefones.fields.length === 0 ? (
            <Vazio texto="Nenhum telefone cadastrado." />
          ) : (
            <ul className="flex flex-col gap-3">
              {telefones.fields.map((campo, i) => (
                <li
                  key={campo.id}
                  className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
                >
                  <TSelect
                    label="Tipo"
                    opcoes={tiposTelefone}
                    {...register(`telefones.${i}.tipoTelefoneId`)}
                  />
                  <Controller
                    control={control}
                    name={`telefones.${i}.numero`}
                    render={({ field }) => (
                      <TEntry
                        label="Número"
                        inputMode="numeric"
                        placeholder="(51) 99999-0000"
                        value={field.value}
                        onChange={(e) => field.onChange(mascararTelefone(e.target.value))}
                        error={errors.telefones?.[i]?.numero?.message}
                      />
                    )}
                  />
                  <TEntry label="Observação" {...register(`telefones.${i}.observacao`)} />
                  <AcoesLinha
                    marcado={watch(`telefones.${i}.principal`)}
                    onPrincipal={() => marcarPrincipal('telefones', i)}
                    onRemover={() => telefones.remove(i)}
                  />
                </li>
              ))}
            </ul>
          )}
        </TPanel>

        <TPanel
          title="E-mails"
          actions={
            <BotaoAcrescentar
              onClick={() =>
                emails.append({
                  tipoEmailId: tiposEmail[0]?.valor ?? '',
                  email: '',
                  observacao: '',
                  principal: emails.fields.length === 0,
                })
              }
            />
          }
        >
          {emails.fields.length === 0 ? (
            <Vazio texto="Nenhum e-mail cadastrado." />
          ) : (
            <ul className="flex flex-col gap-3">
              {emails.fields.map((campo, i) => (
                <li
                  key={campo.id}
                  className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
                >
                  <TSelect
                    label="Tipo"
                    opcoes={tiposEmail}
                    {...register(`emails.${i}.tipoEmailId`)}
                  />
                  <TEntry
                    label="E-mail"
                    type="email"
                    error={errors.emails?.[i]?.email?.message}
                    {...register(`emails.${i}.email`)}
                  />
                  <TEntry label="Observação" {...register(`emails.${i}.observacao`)} />
                  <AcoesLinha
                    marcado={watch(`emails.${i}.principal`)}
                    onPrincipal={() => marcarPrincipal('emails', i)}
                    onRemover={() => emails.remove(i)}
                  />
                </li>
              ))}
            </ul>
          )}
        </TPanel>

        <TPanel
          title="Redes sociais"
          actions={
            <BotaoAcrescentar
              onClick={() =>
                redes.append({
                  tipoRedeSocialId: tiposRedeSocial[0]?.valor ?? '',
                  usuario: '',
                  url: '',
                })
              }
            />
          }
        >
          {redes.fields.length === 0 ? (
            <Vazio texto="Nenhuma rede social cadastrada." />
          ) : (
            <ul className="flex flex-col gap-3">
              {redes.fields.map((campo, i) => (
                <li
                  key={campo.id}
                  className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
                >
                  <TSelect
                    label="Rede"
                    opcoes={tiposRedeSocial}
                    {...register(`redesSociais.${i}.tipoRedeSocialId`)}
                  />
                  <TEntry label="Usuário" {...register(`redesSociais.${i}.usuario`)} />
                  <TEntry label="Link" {...register(`redesSociais.${i}.url`)} />
                  <div className="flex items-end">
                    <BotaoRemover onClick={() => redes.remove(i)} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </TPanel>

        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <TButton
            type="button"
            variant="secondary"
            onClick={() => navigate('/pessoas')}
            className="sm:w-auto"
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

function BotaoAcrescentar({ onClick }: { onClick: () => void }) {
  return (
    <TButton type="button" variant="secondary" size="sm" onClick={onClick}>
      <IconAdicionar className="size-4" />
      Acrescentar
    </TButton>
  )
}

function BotaoRemover({ onClick }: { onClick: () => void }) {
  return (
    <TButton
      type="button"
      variant="ghost"
      size="sm"
      onClick={onClick}
      title="Remover"
      aria-label="Remover"
    >
      <IconFechar className="size-4" />
    </TButton>
  )
}

function Vazio({ texto }: { texto: string }) {
  return <p className="text-caption text-txt-secondary">{texto}</p>
}

/** "Principal" é rádio, não caixa: só um item da lista pode ser. */
function AcoesLinha({
  marcado,
  onPrincipal,
  onRemover,
}: {
  marcado: boolean
  onPrincipal: () => void
  onRemover: () => void
}) {
  return (
    <div className="flex items-end gap-2 sm:pb-1">
      <label className="flex cursor-pointer items-center gap-1.5 text-caption text-txt-secondary">
        <input
          type="radio"
          checked={marcado}
          onChange={onPrincipal}
          className="size-4 accent-(--primary)"
        />
        Principal
      </label>
      <BotaoRemover onClick={onRemover} />
    </div>
  )
}
