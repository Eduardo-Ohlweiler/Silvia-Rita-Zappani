import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import {
  TButton,
  TCombo,
  TEntry,
  TBotaoImprimir,
  TPage,
  TPanel,
  TResult,
  TResultGroup,
  TSelect,
  TTabPanel,
  TTabs,
  TTextArea,
  type Aba,
} from '@/components/common'
import { DocumentoRegistroDiario } from '@/components/uti/impressao/DocumentoRegistroDiario'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { registroDiarioUtiService } from '@/services/utiService'
import {
  OPCOES_SUPORTE,
  type AvaliacaoSugerida,
  type RegistroDiarioUtiResponse,
  type SuporteVentilatorio,
} from '@/types/uti'
import {
  formatarData,
  formatarDocumento,
  formatarNumero,
  paraNumero,
  textoDaMascara,
  hojeIso,
} from '@/utils/format'


const ABA_DIETA = 'dieta'
const ABA_LAB = 'laboratorio'
const ABA_CLINICA = 'clinica'
const ABA_ORAL = 'oral'
const ABA_OBS = 'observacoes'

/** Texto cru, como foi digitado. A conversão acontece uma vez, no envio. */
type Campos = Record<string, string>

const VAZIO: Campos = {
  dieta: '',
  volPrescrito24h: '',
  volRecebido24h: '',
  mg: '',
  k: '',
  na: '',
  lactato: '',
  pcr: '',
  ph: '',
  pco2: '',
  hco3: '',
  hgt: '',
  suporteVentilatorio: '',
  fio2Perc: '',
  paSistolica: '',
  paDiastolica: '',
  balancoHidricoMl: '',
  diureseMl: '',
  evacuacao: '',
  cafeManha: '',
  lancheManha: '',
  almoco: '',
  lancheTarde: '',
  jantar: '',
  ceia: '',
  observacao: '',
}

/**
 * Um dia de acompanhamento na UTI, em cinco abas.
 *
 * **Não há campo de percentual recebido.** Ele é derivado do volume e aparece
 * como resultado — no eroERP é campo digitável ao lado de um calculado, e os
 * dois vão para o banco podendo divergir.
 *
 * **O vínculo com a avaliação é sugerido, nunca automático.** Ao escolher o
 * paciente e a data, o servidor diz qual avaliação estava valendo; o usuário
 * confirma ou desmarca. Ligar em silêncio faria o kcal/kg mudar sem que ninguém
 * tivesse escolhido a referência.
 */
export function RegistroDiarioUtiForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [aba, setAba] = useState(ABA_DIETA)
  /*
   * A tela inicial linka para cá com o paciente na URL. Sem ler isto, o clique
   * na ronda abria o formulário em branco e obrigava a procurar de novo quem a
   * própria tela acabou de apontar.
   */
  const [parametros] = useSearchParams()
  const [pessoaId, setPessoaId] = useState(() =>
    editando ? '' : (parametros.get('pessoaId') ?? ''),
  )
  const [pessoaRotulo, setPessoaRotulo] = useState(() =>
    editando ? '' : (parametros.get('pessoaNome') ?? ''),
  )
  const [data, setData] = useState(hojeIso)
  const [campos, setCampos] = useState<Campos>(VAZIO)

  const [sugestao, setSugestao] = useState<AvaliacaoSugerida>()
  const [vincular, setVincular] = useState(true)
  const [avaliacaoVinculada, setAvaliacaoVinculada] = useState<string>()

  const [derivados, setDerivados] = useState<RegistroDiarioUtiResponse>()
  const [tipoPacienteId, setTipoPacienteId] = useState<string>()
  const [carregando, setCarregando] = useState(editando)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string>()

  useEffect(() => {
    catalogoService
      .tiposCadastro()
      .then((tipos) => setTipoPacienteId(tipos.find((t) => t.nome === 'Paciente')?.id))
      .catch(handleApiError)
  }, [])

  useEffect(() => {
    if (!id) return
    setCarregando(true)
    registroDiarioUtiService
      .findById(id)
      .then((r) => {
        setPessoaId(r.pessoaId)
        setPessoaRotulo(r.pessoaNome)
        setData(r.data)
        setAvaliacaoVinculada(r.avaliacaoId ?? undefined)
        setVincular(!!r.avaliacaoId)
        setDerivados(r)
        setCampos({
          dieta: r.dieta ?? '',
          volPrescrito24h: texto(r.volPrescrito24h),
          volRecebido24h: texto(r.volRecebido24h),
          mg: texto(r.mg),
          k: texto(r.k),
          na: texto(r.na),
          lactato: texto(r.lactato),
          pcr: texto(r.pcr),
          ph: texto(r.ph),
          pco2: texto(r.pco2),
          hco3: texto(r.hco3),
          hgt: texto(r.hgt),
          suporteVentilatorio: r.suporteVentilatorio ?? '',
          fio2Perc: texto(r.fio2Perc),
          paSistolica: inteiro(r.paSistolica),
          paDiastolica: inteiro(r.paDiastolica),
          balancoHidricoMl: texto(r.balancoHidricoMl),
          diureseMl: texto(r.diureseMl),
          evacuacao: r.evacuacao ?? '',
          cafeManha: texto(r.cafeManha),
          lancheManha: texto(r.lancheManha),
          almoco: texto(r.almoco),
          lancheTarde: texto(r.lancheTarde),
          jantar: texto(r.jantar),
          ceia: texto(r.ceia),
          observacao: r.observacao ?? '',
        })
      })
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [id])

  /* A sugestão depende do paciente E da data: mudar qualquer um a refaz. */
  useEffect(() => {
    if (!pessoaId || !data) {
      setSugestao(undefined)
      return
    }
    let cancelado = false
    registroDiarioUtiService
      .avaliacaoSugerida(pessoaId, data)
      .then((s) => {
        if (cancelado) return
        setSugestao(s)
        /*
         * Registro novo já nasce apontando para a sugestão.
         *
         * Em edição a regra é outra, e ela tem uma metade que faltava: respeitar
         * o que ficou gravado, MAS deixar o usuário criar um vínculo que não
         * existia. Sem a segunda metade, marcar a caixa num dia salvo sem
         * avaliação não fazia nada — `avaliacaoVinculada` continuava vazio e o
         * salvar mandava `null`, então a caixa voltava desmarcada e ninguém
         * dizia por quê.
         */
        if (!editando) setAvaliacaoVinculada(s.id)
      })
      .catch(handleApiError)
    return () => {
      cancelado = true
    }
  }, [pessoaId, data, editando])

  function alterar(campo: string, valor: string) {
    setCampos((atual) => ({ ...atual, [campo]: valor }))
  }

  async function salvar() {
    setErro(undefined)
    if (!pessoaId) return setErro('Selecione o paciente')
    if (!data) return setErro('Informe a data')

    const payload = {
      pessoaId,
      // A caixa marcada vale a sugestão, mesmo que o estado não tenha sido
      // tocado — é a rede que impede o vínculo de sumir em silêncio.
      avaliacaoId: vincular ? (avaliacaoVinculada ?? sugestao?.id ?? null) : null,
      data,
      dieta: campos.dieta || null,
      volPrescrito24h: paraNumero(campos.volPrescrito24h) ?? null,
      volRecebido24h: paraNumero(campos.volRecebido24h) ?? null,
      mg: paraNumero(campos.mg) ?? null,
      k: paraNumero(campos.k) ?? null,
      na: paraNumero(campos.na) ?? null,
      lactato: paraNumero(campos.lactato) ?? null,
      pcr: paraNumero(campos.pcr) ?? null,
      ph: paraNumero(campos.ph) ?? null,
      pco2: paraNumero(campos.pco2) ?? null,
      hco3: paraNumero(campos.hco3) ?? null,
      hgt: paraNumero(campos.hgt) ?? null,
      suporteVentilatorio: (campos.suporteVentilatorio as SuporteVentilatorio) || null,
      fio2Perc: paraNumero(campos.fio2Perc) ?? null,
      paSistolica: paraNumero(campos.paSistolica) ?? null,
      paDiastolica: paraNumero(campos.paDiastolica) ?? null,
      balancoHidricoMl: paraNumero(campos.balancoHidricoMl) ?? null,
      diureseMl: paraNumero(campos.diureseMl) ?? null,
      evacuacao: campos.evacuacao || null,
      cafeManha: paraNumero(campos.cafeManha) ?? null,
      lancheManha: paraNumero(campos.lancheManha) ?? null,
      almoco: paraNumero(campos.almoco) ?? null,
      lancheTarde: paraNumero(campos.lancheTarde) ?? null,
      jantar: paraNumero(campos.jantar) ?? null,
      ceia: paraNumero(campos.ceia) ?? null,
      observacao: campos.observacao || null,
    }

    setSalvando(true)
    try {
      if (id) {
        setDerivados(await registroDiarioUtiService.update(id, payload))
        toast.success('Acompanhamento alterado')
      } else {
        const criado = await registroDiarioUtiService.create(payload)
        toast.success('Acompanhamento registrado')
        navigate(`/app/uti/acompanhamento/${criado.id}`, { replace: true })
      }
    } catch (e) {
      handleApiError(e)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) {
    return (
      <TPage title="Acompanhamento diário">
        <TPanel>
          <p className="text-body text-txt-secondary">Carregando…</p>
        </TPanel>
      </TPage>
    )
  }

  const abas: Aba[] = [
    { id: ABA_DIETA, rotulo: 'Dieta e TNE' },
    { id: ABA_LAB, rotulo: 'Laboratório' },
    { id: ABA_CLINICA, rotulo: 'Clínica e balanço' },
    { id: ABA_ORAL, rotulo: 'Ingestão oral' },
    { id: ABA_OBS, rotulo: 'Observações' },
  ]

  return (
    <TPage
      title={editando ? 'Editar acompanhamento' : 'Novo acompanhamento diário'}
      subtitle="Um registro por paciente por dia. O que foi recebido é comparado com o que a avaliação prescreveu."
      // Só há evolução para imprimir depois de gravada: os derivados nascem no
      // servidor, e uma folha sem eles seria o formulário de novo.
      actions={derivados && <TBotaoImprimir rotulo="Imprimir evolução" />}
      documento={derivados && <DocumentoRegistroDiario registro={derivados} />}
    >
      <div className="flex flex-col gap-5">
        <TPanel>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <TCombo
              label="Paciente"
              placeholder="Buscar por nome ou documento"
              value={pessoaId}
              rotuloInicial={pessoaRotulo}
              onChange={setPessoaId}
              buscar={(termo) =>
                pessoaService.select(termo, tipoPacienteId).then((ps) =>
                  ps.map((p) => ({
                    id: p.id,
                    nome: p.documento
                      ? `${p.nome} (${formatarDocumento(p.documento)})`
                      : p.nome,
                  })),
                )
              }
            />
            <TEntry
              label="Data"
              type="date"
              value={data}
              onChange={(e) => setData(e.target.value)}
            />
          </div>

          {/*
            O vínculo é escolha visível. Sem ele o dia grava do mesmo jeito —
            paciente que internou de madrugada tem dia antes de avaliação.
          */}
          {sugestao && (
            <div className="mt-4 rounded-md border border-line bg-surface-alt px-3 py-2.5">
              {sugestao.id ? (
                <label className="flex flex-wrap items-center gap-2 text-body text-txt">
                  <input
                    type="checkbox"
                    className="size-4 accent-primary"
                    checked={vincular}
                    onChange={(e) => {
                      setVincular(e.target.checked)
                      // Marcar a caixa É escolher a avaliação que ela nomeia.
                      // Em edição de um dia salvo sem vínculo, este era o passo
                      // que faltava.
                      if (e.target.checked && !avaliacaoVinculada) {
                        setAvaliacaoVinculada(sugestao.id ?? undefined)
                      }
                    }}
                  />
                  <span>
                    Comparar com a avaliação de{' '}
                    <strong>{formatarData(sugestao.dataAvaliacao!)}</strong>
                  </span>
                  <span className="text-caption text-txt-muted">
                    {formatarNumero(sugestao.pesoTrabalhoKg)} kg ·{' '}
                    {formatarNumero(sugestao.metaEnergetica)} kcal/dia
                    {sugestao.formulaNome ? ` · ${sugestao.formulaNome}` : ''}
                  </span>
                </label>
              ) : (
                <p className="text-caption text-txt-muted">{sugestao.motivo}</p>
              )}
            </div>
          )}
        </TPanel>

        <TTabs abas={abas} ativa={aba} onChange={setAba} />

        <TPanel>
          {/* ───────────────────── Dieta e TNE ────────────────────────── */}
          <TTabPanel id={ABA_DIETA} ativa={aba} rotulo="Dieta e TNE">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TEntry
                label="Dieta"
                className="lg:col-span-2"
                placeholder="Fórmula e via em uso"
                value={campos.dieta}
                onChange={(e) => alterar('dieta', e.target.value)}
              />
              <TEntry
                label="Volume prescrito em 24 h"
                suffix="ml"
                mascara="decimal"
                ajuda="Só se não houver avaliação vinculada — ela já traz o prescrito."
                value={campos.volPrescrito24h}
                onChange={(e) => alterar('volPrescrito24h', e.target.value)}
              />
              <TEntry
                label="Volume recebido em 24 h"
                suffix="ml"
                mascara="decimal"
                value={campos.volRecebido24h}
                onChange={(e) => alterar('volRecebido24h', e.target.value)}
              />
            </div>

            <hr className="my-5 border-line" />

            <TResultGroup
              titulo="O que de fato chegou"
              descricao="Derivado do volume recebido — não há campo para digitar percentual."
              colunas={4}
            >
              <TResult
                label="% recebido"
                valor={formatarNumero(derivados?.percentualRecebido)}
                unidade="%"
                referencia={derivados?.referenciaDoPercentual}
                motivoAusencia={derivados ? undefined : 'Salve para ver os derivados'}
              />
              <TResult
                label="Calorias recebidas"
                valor={formatarNumero(derivados?.caloriasRecebidas)}
                unidade="kcal"
                motivoAusencia={derivados?.motivoDerivados}
              />
              <TResult
                label="Calorias por quilo"
                valor={formatarNumero(derivados?.caloriasPorQuilo)}
                unidade="kcal/kg"
              />
              <TResult
                label="Proteína por quilo"
                valor={formatarNumero(derivados?.proteinaPorQuilo)}
                unidade="g/kg"
              />
            </TResultGroup>
          </TTabPanel>

          {/* ───────────────────── Laboratório ────────────────────────── */}
          <TTabPanel id={ABA_LAB} ativa={aba} rotulo="Laboratório">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {/*
                O exemplo é da faixa normal do exame e mostra a casa decimal:
                pH digitado como 7 em vez de 7,38 não é arredondamento, é outro
                paciente. Aceita vírgula ou ponto — ver `paraNumero`.
              */}
              {[
                ['k', 'Potássio', 'mEq/L', 'Ex.: 4,20'],
                ['na', 'Sódio', 'mEq/L', 'Ex.: 138,00'],
                ['mg', 'Magnésio', 'mg/dL', 'Ex.: 1,90'],
                ['lactato', 'Lactato', 'mmol/L', 'Ex.: 1,40'],
                ['ph', 'pH', '', 'Ex.: 7,38'],
                ['pco2', 'pCO₂', 'mmHg', 'Ex.: 40,50'],
                ['hco3', 'Bicarbonato', 'mEq/L', 'Ex.: 24,30'],
                ['pcr', 'PCR', 'mg/dL', 'Ex.: 0,42'],
                ['hgt', 'Glicemia (HGT)', 'mg/dL', 'Ex.: 152,00'],
              ].map(([campo, rotulo, unidade, exemplo]) => (
                <TEntry
                  key={campo}
                  label={rotulo}
                  suffix={unidade || undefined}
                  mascara="decimal"
                  placeholder={exemplo}
                  value={campos[campo]}
                  onChange={(e) => alterar(campo, e.target.value)}
                />
              ))}
            </div>
          </TTabPanel>

          {/* ────────────────── Clínica e balanço ─────────────────────── */}
          <TTabPanel id={ABA_CLINICA} ativa={aba} rotulo="Clínica e balanço">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TSelect
                label="Suporte ventilatório"
                vazio="Não informado"
                className="lg:col-span-2"
                opcoes={OPCOES_SUPORTE.map((o) => ({ valor: o.valor, rotulo: o.rotulo }))}
                value={campos.suporteVentilatorio}
                onChange={(e) => alterar('suporteVentilatorio', e.target.value)}
              />
              <TEntry
                label="FiO₂"
                suffix="%"
                mascara="decimal"
                ajuda="De 21 (ar ambiente) a 100."
                value={campos.fio2Perc}
                onChange={(e) => alterar('fio2Perc', e.target.value)}
              />
              <div className="grid grid-cols-2 gap-2">
                <TEntry
                  label="PA sistólica"
                  suffix="mmHg"
                  mascara="inteiro"
                  value={campos.paSistolica}
                  onChange={(e) => alterar('paSistolica', e.target.value)}
                />
                <TEntry
                  label="PA diastólica"
                  suffix="mmHg"
                  mascara="inteiro"
                  value={campos.paDiastolica}
                  onChange={(e) => alterar('paDiastolica', e.target.value)}
                />
              </div>

              <TEntry
                label="Balanço hídrico"
                suffix="ml"
                mascara="decimalComSinal"
                ajuda="Aceita negativo."
                value={campos.balancoHidricoMl}
                onChange={(e) => alterar('balancoHidricoMl', e.target.value)}
              />
              <TEntry
                label="Diurese"
                suffix="ml"
                mascara="decimal"
                value={campos.diureseMl}
                onChange={(e) => alterar('diureseMl', e.target.value)}
              />
              <TEntry
                label="Evacuação"
                className="lg:col-span-2"
                placeholder="Ex.: uma vez, pastosa"
                value={campos.evacuacao}
                onChange={(e) => alterar('evacuacao', e.target.value)}
              />
            </div>

            <hr className="my-5 border-line" />

            <TResultGroup titulo="Derivado" colunas={2}>
              <TResult
                label="Diurese por quilo"
                valor={formatarNumero(derivados?.diuresePorQuiloHora)}
                unidade="ml/kg/h"
                referencia="precisa do peso da avaliação"
                motivoAusencia={derivados?.motivoDerivados}
              />
            </TResultGroup>
          </TTabPanel>

          {/* ───────────────────── Ingestão oral ──────────────────────── */}
          <TTabPanel id={ABA_ORAL} ativa={aba} rotulo="Ingestão oral">
            <p className="text-caption mb-4 text-txt-secondary">
              Percentual de aceitação de cada refeição. Deixar em branco não é o mesmo que
              zero — refeição não registrada fica fora da média.
            </p>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {[
                ['cafeManha', 'Café da manhã'],
                ['lancheManha', 'Lanche da manhã'],
                ['almoco', 'Almoço'],
                ['lancheTarde', 'Lanche da tarde'],
                ['jantar', 'Jantar'],
                ['ceia', 'Ceia'],
              ].map(([campo, rotulo]) => (
                <TEntry
                  key={campo}
                  label={rotulo}
                  suffix="%"
                  mascara="decimal"
                  value={campos[campo]}
                  onChange={(e) => alterar(campo, e.target.value)}
                />
              ))}
            </div>

            <hr className="my-5 border-line" />

            <TResultGroup titulo="Derivado" colunas={2}>
              <TResult
                label="Média de aceitação"
                valor={formatarNumero(derivados?.mediaIngestaoOral)}
                unidade="%"
                referencia="só das refeições informadas"
              />
            </TResultGroup>
          </TTabPanel>

          {/* ───────────────────── Observações ────────────────────────── */}
          <TTabPanel id={ABA_OBS} ativa={aba} rotulo="Observações">
            <TTextArea
              label="Observações"
              value={campos.observacao}
              onChange={(e) => alterar('observacao', e.target.value)}
              placeholder="Intercorrências, conduta, o que mais precisar ficar registrado."
            />
          </TTabPanel>
        </TPanel>

        {erro && (
          <p role="alert" className="text-caption text-danger">
            {erro}
          </p>
        )}

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <TButton
            type="button"
            variant="secondary"
            className="sm:w-auto"
            onClick={() => navigate('/app/uti/acompanhamento')}
          >
            Voltar
          </TButton>
          <TButton type="button" loading={salvando} onClick={() => void salvar()}>
            Salvar
          </TButton>
        </div>
      </div>
    </TPage>
  )
}

/** No formato da máscara — ver `textoDaMascara`. */
function texto(valor?: number | null): string {
  return textoDaMascara(valor, 2)
}

/** PA é mmHg redondo, e o DTO só aceita uma casa. */
function inteiro(valor?: number | null): string {
  return textoDaMascara(valor, 0)
}
