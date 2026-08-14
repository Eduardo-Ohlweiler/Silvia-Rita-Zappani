import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { z } from 'zod'
import { IconAdicionar, IconFechar } from '@/assets/icons'
import {
  TButton,
  TCombo,
  TEntry,
  TPage,
  TPanel,
  TSelect,
  type OpcaoSelect,
} from '@/components/common'
import { PessoaRapidaModal } from '@/components/pessoa/PessoaRapidaModal'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { localidadeService } from '@/services/localidadeService'
import { pessoaService } from '@/services/pessoaService'
import {
  OPCOES_TIPO_PESSOA,
  OPCOES_TIPO_VINCULO,
  type TipoPessoa,
  type TipoVinculo,
} from '@/types/pessoa'
import {
  formatarDocumento,
  mascararCep,
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
  enderecos: z.array(
    z.object({
      id: z.string().optional(),
      tipoEnderecoId: z.string().min(1, 'Informe o tipo'),
      cidadeId: z.string().min(1, 'Informe a cidade'),
      cep: z.string().optional(),
      rua: z.string().optional(),
      numero: z.string().optional(),
      bairro: z.string().optional(),
      complemento: z.string().optional(),
      principal: z.boolean(),
    }),
  ),
  vinculos: z.array(
    z.object({
      id: z.string().optional(),
      pessoaId: z.string().min(1, 'Informe a pessoa'),
      tipo: z.enum(['RESPONSAVEL', 'DEPENDENTE', 'CONJUGE', 'FAMILIAR']),
      observacao: z.string().optional(),
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
  enderecos: [],
  vinculos: [],
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
  const [tiposEndereco, setTiposEndereco] = useState<OpcaoSelect[]>([])
  /** Índice da linha de vínculo que abriu o cadastro rápido. */
  const [linhaDoModal, setLinhaDoModal] = useState<number>()

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
  const enderecos = useFieldArray({ control, name: 'enderecos' })
  const vinculos = useFieldArray({ control, name: 'vinculos' })

  useEffect(() => {
    const paraOpcoes = (itens: { id: string; nome: string }[]) =>
      itens.map((i) => ({ valor: i.id, rotulo: i.nome }))

    Promise.all([
      catalogoService.tiposCadastro(),
      catalogoService.tiposTelefone(),
      catalogoService.tiposEmail(),
      catalogoService.tiposRedeSocial(),
      catalogoService.tiposEndereco(),
    ])
      .then(([cadastro, telefone, email, rede, endereco]) => {
        setTiposCadastro(paraOpcoes(cadastro))
        setTiposTelefone(paraOpcoes(telefone))
        setTiposEmail(paraOpcoes(email))
        setTiposRedeSocial(paraOpcoes(rede))
        setTiposEndereco(paraOpcoes(endereco))
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
          enderecos: p.enderecos.map((e) => ({
            id: e.id,
            tipoEnderecoId: e.tipoEnderecoId,
            cidadeId: e.cidadeId,
            cep: e.cep ? mascararCep(e.cep) : '',
            rua: e.rua ?? '',
            numero: e.numero ?? '',
            bairro: e.bairro ?? '',
            complemento: e.complemento ?? '',
            principal: e.principal,
          })),
          vinculos: p.vinculos.map((v) => ({
            id: v.id,
            pessoaId: v.pessoaId,
            tipo: v.tipo,
            observacao: v.observacao ?? '',
          })),
        }),
      )
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id, reset])

  /** Principal é exclusivo: marcar um desmarca os outros. */
  function marcarPrincipal(lista: 'telefones' | 'emails' | 'enderecos', indice: number) {
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
      enderecos: dados.enderecos.map((e) => ({
        id: e.id,
        tipoEnderecoId: e.tipoEnderecoId,
        cidadeId: e.cidadeId,
        cep: somenteDigitos(e.cep),
        rua: e.rua || undefined,
        numero: e.numero || undefined,
        bairro: e.bairro || undefined,
        complemento: e.complemento || undefined,
        principal: e.principal,
      })),
      vinculos: dados.vinculos.map((v) => ({
        id: v.id,
        pessoaId: v.pessoaId,
        tipo: v.tipo as TipoVinculo,
        observacao: v.observacao || undefined,
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

        <TPanel
          title="Endereços"
          actions={
            <BotaoAcrescentar
              onClick={() =>
                enderecos.append({
                  tipoEnderecoId: tiposEndereco[0]?.valor ?? '',
                  cidadeId: '',
                  cep: '',
                  rua: '',
                  numero: '',
                  bairro: '',
                  complemento: '',
                  principal: enderecos.fields.length === 0,
                })
              }
            />
          }
        >
          {enderecos.fields.length === 0 ? (
            <Vazio texto="Nenhum endereço cadastrado." />
          ) : (
            <ul className="flex flex-col gap-4">
              {/*
                Endereço tem campos demais para uma linha só: cada um é um
                bloco com borda, que no celular vira coluna única.
              */}
              {enderecos.fields.map((campo, i) => (
                <li key={campo.id} className="rounded-md border border-line-strong p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <label className="flex cursor-pointer items-center gap-1.5 text-caption text-txt-secondary">
                      <input
                        type="radio"
                        checked={watch(`enderecos.${i}.principal`)}
                        onChange={() => marcarPrincipal('enderecos', i)}
                        className="size-4 accent-(--primary)"
                      />
                      Principal
                    </label>
                    <BotaoRemover onClick={() => enderecos.remove(i)} />
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    <TSelect
                      label="Tipo"
                      opcoes={tiposEndereco}
                      {...register(`enderecos.${i}.tipoEnderecoId`)}
                    />
                    <Controller
                      control={control}
                      name={`enderecos.${i}.cidadeId`}
                      render={({ field }) => (
                        <TCombo
                          label="Cidade"
                          placeholder="Busque pelo nome"
                          value={field.value}
                          onChange={field.onChange}
                          buscar={localidadeService.cidadesParaCombo}
                          error={errors.enderecos?.[i]?.cidadeId?.message}
                        />
                      )}
                    />
                    <Controller
                      control={control}
                      name={`enderecos.${i}.cep`}
                      render={({ field }) => (
                        <TEntry
                          label="CEP"
                          inputMode="numeric"
                          placeholder="00000-000"
                          value={field.value ?? ''}
                          onChange={(e) => field.onChange(mascararCep(e.target.value))}
                        />
                      )}
                    />
                    <TEntry
                      label="Rua"
                      className="lg:col-span-2"
                      {...register(`enderecos.${i}.rua`)}
                    />
                    <TEntry label="Número" {...register(`enderecos.${i}.numero`)} />
                    <TEntry label="Bairro" {...register(`enderecos.${i}.bairro`)} />
                    <TEntry
                      label="Complemento"
                      className="lg:col-span-2"
                      {...register(`enderecos.${i}.complemento`)}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </TPanel>

        <TPanel
          title="Vínculos"
          actions={
            <BotaoAcrescentar
              onClick={() =>
                vinculos.append({ pessoaId: '', tipo: 'RESPONSAVEL', observacao: '' })
              }
            />
          }
        >
          {vinculos.fields.length === 0 ? (
            <Vazio texto="Nenhum vínculo cadastrado." />
          ) : (
            <ul className="flex flex-col gap-3">
              {vinculos.fields.map((campo, i) => (
                <li
                  key={campo.id}
                  className="grid gap-3 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
                >
                  <div className="flex items-end gap-2">
                    <Controller
                      control={control}
                      name={`vinculos.${i}.pessoaId`}
                      render={({ field }) => (
                        <TCombo
                          label="Pessoa"
                          className="flex-1"
                          placeholder="Busque por nome ou documento"
                          value={field.value}
                          onChange={field.onChange}
                          buscar={(termo) =>
                            pessoaService
                              .select(termo, undefined, id)
                              .then((pessoas) =>
                                pessoas.map((p) => ({
                                  id: p.id,
                                  nome: p.documento
                                    ? `${p.nome} (${formatarDocumento(p.documento)})`
                                    : p.nome,
                                })),
                              )
                          }
                          error={errors.vinculos?.[i]?.pessoaId?.message}
                        />
                      )}
                    />
                    <TButton
                      type="button"
                      variant="secondary"
                      onClick={() => setLinhaDoModal(i)}
                      title="Cadastrar uma pessoa nova sem sair daqui"
                      aria-label="Cadastrar pessoa nova"
                    >
                      <IconAdicionar className="size-4" />
                    </TButton>
                  </div>

                  <TSelect
                    label="É o quê desta pessoa"
                    opcoes={OPCOES_TIPO_VINCULO}
                    {...register(`vinculos.${i}.tipo`)}
                  />
                  <TEntry label="Observação" {...register(`vinculos.${i}.observacao`)} />
                  <div className="flex items-end sm:pb-1">
                    <BotaoRemover onClick={() => vinculos.remove(i)} />
                  </div>
                </li>
              ))}
            </ul>
          )}

          <p className="mt-4 rounded-md bg-info-bg px-3 py-2 text-caption text-info">
            O vínculo vale nos dois sentidos: marcar alguém como responsável faz
            esta pessoa aparecer como dependente no cadastro dele.
          </p>
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

      <PessoaRapidaModal
        aberto={linhaDoModal !== undefined}
        onFechar={() => setLinhaDoModal(undefined)}
        onCriada={(pessoa) => {
          // Já entra selecionada na linha que abriu o modal
          if (linhaDoModal !== undefined)
            setValue(`vinculos.${linhaDoModal}.pessoaId`, pessoa.id)
          setLinhaDoModal(undefined)
        }}
      />
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
