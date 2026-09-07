import {
  Folha,
  LinhasDeValor,
  Observacao,
  Secao,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { FichaAnamneseResponse, RespostaFichaResponse } from '@/types/clinica'
import { formatarData } from '@/utils/format'

/**
 * A ficha de anamnese no papel.
 *
 * <p><b>Tudo aqui sai do retrato gravado na ficha</b> — seção, pergunta, tipo e
 * opções vêm de cada resposta, nunca do modelo de hoje. É o que faz a folha
 * impressa hoje ser igual à impressa há um ano, mesmo que o modelo tenha sido
 * reescrito no meio do caminho.
 *
 * <p><b>Pergunta sem resposta não é omitida.</b> Um prontuário que esconde o que
 * não foi perguntado mente por omissão: quem lê não distingue "não tem" de "não
 * coube na folha". É o contrato já escrito no javadoc de {@code LinhasDeValor}.
 */
export function DocumentoFichaAnamnese({ ficha }: { ficha: FichaAnamneseResponse }) {
  const secoes = agrupar(ficha.respostas)
  const respondidas = ficha.respostas.filter((r) => temValor(r)).length

  return (
    <Folha
      titulo="Ficha de anamnese"
      paciente={ficha.pacienteNome}
      referencia={
        <>
          {ficha.modeloNome} · {formatarData(ficha.dataPreenchimento)}
          {ficha.profissionalNome && <> · {ficha.profissionalNome}</>}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Data', valor: formatarData(ficha.dataPreenchimento) },
          { rotulo: 'Modelo', valor: ficha.modeloNome },
          {
            rotulo: 'Respondidas',
            valor: `${respondidas} de ${ficha.respostas.length}`,
          },
        ]}
      />

      {secoes.map((secao) => (
        <Secao key={secao.titulo ?? '__sem-secao'} titulo={secao.titulo ?? 'Anamnese'}>
          {/* `prosa`: resposta de anamnese é texto, não número. */}
          <LinhasDeValor linhas={secao.linhas} prosa />
        </Secao>
      ))}

      <Observacao texto={ficha.observacao} />
    </Folha>
  )
}

interface SecaoImpressa {
  titulo: string | null
  linhas: LinhaValor[]
}

/** Ordem de aparição, como na tela: a seção nasce com a primeira pergunta dela. */
function agrupar(respostas: RespostaFichaResponse[]): SecaoImpressa[] {
  const secoes: SecaoImpressa[] = []

  for (const r of [...respostas].sort((a, b) => a.ordem - b.ordem)) {
    const titulo = r.secao ?? null
    const linha: LinhaValor = {
      rotulo: r.rotulo,
      valor: textoDaResposta(r),
      detalhe: r.obrigatorio && !temValor(r) ? 'Obrigatória, não respondida' : undefined,
    }

    const atual = secoes.find((s) => s.titulo === titulo)
    if (atual) atual.linhas.push(linha)
    else secoes.push({ titulo, linhas: [linha] })
  }

  return secoes
}

function temValor(r: RespostaFichaResponse): boolean {
  return r.valor != null && r.valor.trim() !== ''
}

/**
 * O {@code tipo} do retrato é o que torna o {@code valor} legível: sem ele,
 * {@code "true"} sairia impresso assim, e {@code ["Leite","Ovo"]} sairia com
 * colchetes num prontuário.
 */
function textoDaResposta(r: RespostaFichaResponse): string {
  /* No papel o traço é legítimo — a folha não tem como perguntar de novo. */
  if (!temValor(r)) return 'Não informado'
  const valor = r.valor as string

  switch (r.tipo) {
    case 'CHECKBOX':
      return valor === 'true' ? 'Sim' : 'Não'
    case 'DATA':
      return formatarData(valor)
    case 'MULTIPLAS_OPCOES': {
      try {
        const lido: unknown = JSON.parse(valor)
        return Array.isArray(lido) && lido.length > 0 ? (lido as string[]).join(', ') : 'Não informado'
      } catch {
        return valor
      }
    }
    default:
      return valor
  }
}
