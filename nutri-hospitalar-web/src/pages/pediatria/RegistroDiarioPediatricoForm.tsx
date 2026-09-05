import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import {
  TBotaoImprimir,
  TButton,
  TCombo,
  TEntry,
  TPage,
  TPanel,
  TResult,
  TResultGroup,
  TTextArea,
} from '@/components/common'
import { DocumentoAcompanhamentoPediatrico } from '@/components/pediatria/impressao/DocumentoAcompanhamentoPediatrico'
import { handleApiError } from '@/services/api'
import { catalogoService } from '@/services/catalogoService'
import { pessoaService } from '@/services/pessoaService'
import { registroDiarioPediatricoService } from '@/services/pediatriaService'
import type {
  AvaliacaoPediatricaSugerida,
  RegistroDiarioPediatricoResponse,
} from '@/types/pediatria'
import {
  formatarData,
  formatarDocumento,
  formatarNumero,
  paraNumero,
  textoDaMascara,
  hojeIso,
} from '@/utils/format'
import { idadeEmMesesTexto } from '@/utils/idade'


/** Texto cru, como foi digitado. A conversão acontece uma vez, no envio. */
type Campos = Record<string, string>

const VAZIO: Campos = {
  pesoKg: '',
  estaturaCm: '',
  volPrescrito24h: '',
  volRecebido24h: '',
  tomadasPrevistas: '',
  tomadasAceitas: '',
  observacao: '',
}

/**
 * Um dia de acompanhamento nutricional pediátrico — docs/11.
 *
 * **Uma tela só, sem abas**: são sete campos. O dia de UTI tem cinco abas porque
 * tem 29 — dividir sete esconderia metade atrás de um clique sem ganho nenhum.
 *
 * **Nenhum derivado é campo.** Percentual recebido, adequações, IMC e as três
 * classificações da OMS aparecem como resultado, calculados no servidor. E a
 * **idade não é digitada**: sai da data de nascimento e da data do registro, o
 * que faz a criança andar na curva ao longo da internação.
 *
 * **O vínculo com a avaliação é sugerido, nunca automático.** Ao escolher o
 * paciente e a data, o servidor diz qual avaliação estava valendo; o usuário
 * confirma ou desmarca. Ligar em silêncio faria a adequação calórica mudar sem
 * que ninguém tivesse escolhido a referência.
 */
export function RegistroDiarioPediatricoForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editando = !!id

  const [pessoaId, setPessoaId] = useState('')
  const [pessoaRotulo, setPessoaRotulo] = useState('')
  const [data, setData] = useState(hojeIso)
  const [campos, setCampos] = useState<Campos>(VAZIO)

  const [sugestao, setSugestao] = useState<AvaliacaoPediatricaSugerida>()
  const [vincular, setVincular] = useState(true)
  const [avaliacaoVinculada, setAvaliacaoVinculada] = useState<string | null>()

  const [registro, setRegistro] = useState<RegistroDiarioPediatricoResponse>()
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
    registroDiarioPediatricoService
      .findById(id)
      .then((r) => {
        setPessoaId(r.pessoaId)
        setPessoaRotulo(r.pessoaNome)
        setData(r.data)
        setAvaliacaoVinculada(r.avaliacaoId ?? undefined)
        setVincular(!!r.avaliacaoId)
        setRegistro(r)
        setCampos({
          // `textoDaMascara` com as MESMAS casas do campo: `String(9.2)` daria
          // "9,2" e a primeira tecla releria isso como os dígitos 92.
          pesoKg: textoDaMascara(r.pesoKg, 3),
          estaturaCm: textoDaMascara(r.estaturaCm, 2),
          volPrescrito24h: textoDaMascara(r.volPrescrito24h, 0),
          volRecebido24h: textoDaMascara(r.volRecebido24h, 0),
          tomadasPrevistas: r.tomadasPrevistas != null ? String(r.tomadasPrevistas) : '',
          tomadasAceitas: r.tomadasAceitas != null ? String(r.tomadasAceitas) : '',
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
    registroDiarioPediatricoService
      .avaliacaoSugerida(pessoaId, data)
      .then((s) => {
        if (cancelado) return
        setSugestao(s)
        // Só pré-seleciona em registro novo: em edição, respeita o que ficou.
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
      avaliacaoId: vincular ? (avaliacaoVinculada ?? null) : null,
      data,
      pesoKg: paraNumero(campos.pesoKg) ?? null,
      estaturaCm: paraNumero(campos.estaturaCm) ?? null,
      volPrescrito24h: paraNumero(campos.volPrescrito24h) ?? null,
      volRecebido24h: paraNumero(campos.volRecebido24h) ?? null,
      tomadasPrevistas: paraNumero(campos.tomadasPrevistas) ?? null,
      tomadasAceitas: paraNumero(campos.tomadasAceitas) ?? null,
      observacao: campos.observacao || null,
    }

    setSalvando(true)
    try {
      if (id) {
        setRegistro(await registroDiarioPediatricoService.update(id, payload))
        toast.success('Acompanhamento alterado')
      } else {
        const criado = await registroDiarioPediatricoService.create(payload)
        toast.success('Acompanhamento registrado')
        navigate(`/app/pediatria/acompanhamento/${criado.id}`, { replace: true })
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

  const d = registro?.derivados

  return (
    <TPage
      title={editando ? 'Editar acompanhamento' : 'Novo acompanhamento diário'}
      subtitle="O peso do dia classifica na curva da OMS, e o que chegou é comparado com o que a avaliação prescreveu."
      // Só há evolução para imprimir depois de gravada: os derivados nascem no
      // servidor, e uma folha sem eles seria o formulário de novo.
      actions={registro && <TBotaoImprimir rotulo="Imprimir evolução" />}
      documento={registro && <DocumentoAcompanhamentoPediatrico registro={registro} />}
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
              ajuda={
                d?.idadeMeses != null
                  ? `${idadeEmMesesTexto(d.idadeMeses)} nesta data`
                  : 'A idade sai da data de nascimento do paciente'
              }
            />
          </div>

          {/*
            O vínculo é escolha visível. Sem ele o dia grava do mesmo jeito —
            criança que internou de madrugada tem dia antes de avaliação.
          */}
          {sugestao && (
            <div className="mt-4 rounded-md border border-line bg-surface-alt px-3 py-2.5">
              {sugestao.id ? (
                <label className="flex flex-wrap items-center gap-2 text-body text-txt">
                  <input
                    type="checkbox"
                    className="size-4 accent-primary"
                    checked={vincular}
                    onChange={(e) => setVincular(e.target.checked)}
                  />
                  <span>
                    Comparar com a avaliação de{' '}
                    <strong>{formatarData(sugestao.dataAvaliacao!)}</strong>
                  </span>
                  <span className="text-caption text-txt-muted">
                    {formatarNumero(sugestao.peso)} kg ·{' '}
                    {formatarNumero(sugestao.volumeTotal, 0)} ml/dia
                    {sugestao.vet != null ? ` · ${formatarNumero(sugestao.vet, 0)} kcal/dia` : ''}
                    {sugestao.formulaNome ? ` · ${sugestao.formulaNome}` : ''}
                  </span>
                </label>
              ) : (
                <p className="text-caption text-txt-muted">{sugestao.aviso}</p>
              )}
            </div>
          )}
        </TPanel>

        {/* ───────────────────── As sete entradas ───────────────────────── */}
        <TPanel title="O que foi medido e ofertado">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <TEntry
              label="Peso do dia"
              mascara="decimal"
              casas={3}
              suffix="kg"
              value={campos.pesoKg}
              onChange={(e) => alterar('pesoKg', e.target.value)}
              ajuda="A medida de hoje — é ela que classifica na curva"
            />
            <TEntry
              label="Estatura"
              mascara="decimal"
              casas={2}
              suffix="cm"
              value={campos.estaturaCm}
              onChange={(e) => alterar('estaturaCm', e.target.value)}
              ajuda="Muda devagar: medir todo dia não é esperado"
            />
            <TEntry
              label="Volume prescrito"
              mascara="inteiro"
              suffix="ml"
              value={campos.volPrescrito24h}
              onChange={(e) => alterar('volPrescrito24h', e.target.value)}
              ajuda={
                vincular && sugestao?.volumeTotal != null
                  ? 'Em branco usa o prescrito da avaliação'
                  : undefined
              }
            />
            <TEntry
              label="Volume recebido"
              mascara="inteiro"
              suffix="ml"
              value={campos.volRecebido24h}
              onChange={(e) => alterar('volRecebido24h', e.target.value)}
            />
            <TEntry
              label="Tomadas previstas"
              mascara="inteiro"
              value={campos.tomadasPrevistas}
              onChange={(e) => alterar('tomadasPrevistas', e.target.value)}
            />
            <TEntry
              label="Tomadas aceitas"
              mascara="inteiro"
              value={campos.tomadasAceitas}
              onChange={(e) => alterar('tomadasAceitas', e.target.value)}
              ajuda="Não pode passar das previstas"
            />
          </div>

          <div className="mt-4">
            <TTextArea
              label="Observações"
              rows={3}
              value={campos.observacao}
              onChange={(e) => alterar('observacao', e.target.value)}
            />
          </div>
        </TPanel>

        {/* ───────────────────── Os derivados ──────────────────────────── */}
        {d && (
          <>
            <TPanel>
              <TResultGroup titulo="Estado nutricional no dia">
                <TResult
                  label="Idade"
                  valor={d.idadeMeses != null ? idadeEmMesesTexto(d.idadeMeses) : undefined}
                  referencia="da data de nascimento"
                  motivoAusencia={d.motivoIdade}
                />
                <TResult
                  label="IMC"
                  valor={d.imc != null ? formatarNumero(d.imc) : undefined}
                  unidade="kg/m²"
                  motivoAusencia={d.motivoImc}
                />
                {/* Só `classificacao` — ela JÁ pinta o rótulo. O `valor` do
                    TResult é o número que a classificação explica (na UTI,
                    "92,88 %" ao lado de "Eutrofia"); passar o rótulo nos dois
                    imprimia "Peso adequado  Peso adequado" na tela. */}
                <TResult
                  label="Peso para a idade"
                  classificacao={d.pesoIdade ?? undefined}
                  referencia="OMS · P15 e P85"
                  motivoAusencia={d.motivoEstadoNutricional}
                />
                <TResult
                  label="Estatura para a idade"
                  classificacao={d.estaturaIdade ?? undefined}
                  referencia="OMS · P15 e P85"
                  motivoAusencia={d.motivoEstadoNutricional}
                />
                <TResult
                  label="IMC para a idade"
                  classificacao={d.imcIdade ?? undefined}
                  referencia="OMS · P15 e P85"
                  motivoAusencia={d.motivoEstadoNutricional}
                />
              </TResultGroup>
            </TPanel>

            <TPanel>
              <TResultGroup titulo="O que a criança recebeu" colunas={4}>
                <TResult
                  label="Do prescrito"
                  valor={
                    d.percentualRecebido != null ? formatarNumero(d.percentualRecebido) : undefined
                  }
                  unidade="%"
                  referencia={d.referenciaDoRecebido}
                  motivoAusencia={d.motivoPercentualRecebido}
                />
                <TResult
                  label="Calorias recebidas"
                  valor={
                    d.caloriasRecebidas != null ? formatarNumero(d.caloriasRecebidas) : undefined
                  }
                  unidade="kcal/dia"
                  referencia={registro?.avaliacaoFormulaNome ?? undefined}
                  motivoAusencia={d.motivoOferta}
                />
                <TResult
                  label="Proteína recebida"
                  valor={d.proteinaRecebida != null ? formatarNumero(d.proteinaRecebida) : undefined}
                  unidade="g/dia"
                  motivoAusencia={d.motivoOferta}
                />
                <TResult
                  label="Por quilo — energia"
                  valor={d.caloriasPorKg != null ? formatarNumero(d.caloriasPorKg) : undefined}
                  unidade="kcal/kg"
                  referencia="com o peso do dia"
                  motivoAusencia={d.motivoPorQuilo}
                />
                <TResult
                  label="Por quilo — proteína"
                  valor={d.proteinaPorKg != null ? formatarNumero(d.proteinaPorKg) : undefined}
                  unidade="g/kg"
                  motivoAusencia={d.motivoPorQuilo}
                />
                <TResult
                  label="Adequação calórica"
                  valor={
                    d.adequacaoCalorica != null ? formatarNumero(d.adequacaoCalorica) : undefined
                  }
                  unidade="%"
                  referencia={
                    registro?.avaliacaoVet != null
                      // Uma casa, não zero: o VET de 765,5 saía "766" ao lado
                      // de 67,49 %, e 516,6/766 dá 67,44. A legenda tem de
                      // fechar com o número que ela explica.
                      ? `de ${formatarNumero(registro.avaliacaoVet, 1)} kcal/dia`
                      : undefined
                  }
                  motivoAusencia={d.motivoAdequacaoCalorica}
                />
                <TResult
                  label="Adequação proteica"
                  valor={
                    d.adequacaoProteica != null ? formatarNumero(d.adequacaoProteica) : undefined
                  }
                  unidade="%"
                  referencia={
                    registro?.avaliacaoProteinaNecessidade != null
                      ? `de ${formatarNumero(registro.avaliacaoProteinaNecessidade, 1)} g/dia`
                      : undefined
                  }
                  motivoAusencia={d.motivoAdequacaoProteica}
                />
                <TResult
                  label="Tomadas aceitas"
                  valor={
                    d.aceitacaoTomadas != null ? formatarNumero(d.aceitacaoTomadas) : undefined
                  }
                  unidade="%"
                  motivoAusencia={d.motivoAceitacao}
                />
              </TResultGroup>
            </TPanel>
          </>
        )}

        {erro && <p className="text-body text-danger">{erro}</p>}

        <div className="flex flex-wrap gap-3">
          <TButton onClick={() => void salvar()} disabled={salvando}>
            {salvando ? 'Salvando…' : editando ? 'Salvar alterações' : 'Registrar dia'}
          </TButton>
          <TButton variant="secondary" onClick={() => navigate('/app/pediatria/acompanhamento')}>
            Voltar
          </TButton>
        </div>
      </div>
    </TPage>
  )
}
