import {
  Folha,
  LinhasDeValor,
  Observacao,
  Secao,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { Escore, FichaAnamneseResponse, RespostaFichaResponse } from '@/types/clinica'
import { formatarData, formatarNumero } from '@/utils/format'

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
 *
 * <p><b>O escore impresso é o congelado no dia</b>, e a conta sai por extenso —
 * subtotal de cada bloco, o ajuste por idade e o total —, para poder ser refeita
 * à mão. Um prontuário que se contradiz faz quem confere concluir que o sistema
 * errou: foi assim que a folha do dia passou a imprimir "Prescrito 1.500 ·
 * Recebido 1.200 · Adesão 88 %".
 */
export function DocumentoFichaAnamnese({ ficha }: { ficha: FichaAnamneseResponse }) {
  const secoes = agrupar(ficha.respostas)
  const respondidas = ficha.respostas.filter((r) => temValor(r)).length
  const escore = ficha.escore

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
      {/* Com escala, os números da escala são o que se lê primeiro — e o nome
          do modelo já está na linha de referência, sob o cabeçalho. A tira
          aceita cinco: as duas escalas cabem exatas. */}
      <TiraIndicadores
        itens={[
          { rotulo: 'Data', valor: formatarData(ficha.dataPreenchimento) },
          ...(escore ? [] : [{ rotulo: 'Modelo', valor: ficha.modeloNome }]),
          {
            rotulo: 'Respondidas',
            valor: `${respondidas} de ${ficha.respostas.length}`,
          },
          ...(escore ? indicadoresDoEscore(escore) : []),
        ].slice(0, 5)}
      />

      {escore && (
        <Secao titulo="Escore" nota={`${escore.escalaNome} · ${escore.referencia}`}>
          <LinhasDeValor linhas={linhasDoEscore(escore)} />
          {/*
            A conduta sai numa tabela `prosa`, e não como mais uma linha da de
            cima. A coluna de valor daquela tem 22 % e alinha à direita, porque
            nasceu para "2,0 de 3" — e "Nenhum critério da pré-triagem, repetir
            a triagem semanalmente" saía quebrada em quatro linhas espremidas
            contra a margem, com dois terços da folha vazios ao lado. É a mesma
            armadilha que a fatia 12 pagou com as respostas da anamnese: ao
            reusar componente de impressão, conferir a forma do dado para o qual
            ele foi desenhado.
          */}
          {escore.conclusao && (
            <LinhasDeValor linhas={[{ rotulo: 'Conduta', valor: escore.conclusao }]} prosa />
          )}
        </Secao>
      )}

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

/**
 * A tira: um subtotal por bloco que pontua, e o total.
 *
 * <p>A pré-triagem da NRS-2002 não entra — ela é porta, não soma, e um "0 de 0"
 * ao lado dela seria número onde não há conta.
 */
function indicadoresDoEscore(escore: Escore): { rotulo: string; valor: string }[] {
  const grupos = escore.grupos
    .filter((g) => g.subtotal != null && g.maximo != null)
    .map((g) => ({
      rotulo: g.rotulo,
      valor: `${formatarNumero(g.subtotal, 1, 1)} de ${g.maximo}`,
    }))

  return [
    ...grupos,
    {
      rotulo: 'Escore total',
      valor:
        escore.total != null
          ? `${formatarNumero(escore.total, 1, 1)} de ${escore.totalMaximo}`
          : formatarNumero(null),
    },
  ]
}

/**
 * A conta inteira, na ordem em que se soma.
 *
 * <p>O <b>ajuste por idade sai como linha própria</b> sempre que existe. Somado
 * calado dentro do total, ele tornaria a folha impossível de conferir: quem
 * recontasse 2 + 2 acharia 4 debaixo de um 5 impresso, e concluiria que o
 * sistema errou.
 *
 * <p>E cada linha sem número leva o <b>motivo</b> ao lado, porque numa escala a
 * ausência é a informação: soma parcial não é escore menor, é escore errado.
 */
function linhasDoEscore(escore: Escore): LinhaValor[] {
  const linhas: LinhaValor[] = escore.grupos.map((g) => ({
    rotulo: g.rotulo,
    valor:
      g.subtotal != null
        ? `${formatarNumero(g.subtotal, 1, 1)}${g.maximo != null ? ` de ${g.maximo}` : ''}`
        : formatarNumero(null),
    detalhe: g.classificacao?.rotulo ?? g.motivoAusencia ?? undefined,
  }))

  if (escore.ajusteIdade != null) {
    linhas.push({
      rotulo: 'Ajuste por idade',
      valor: `+ ${formatarNumero(escore.ajusteIdade, 1, 1)}`,
      detalhe: escore.ajusteIdadeDescricao ?? undefined,
    })
  }

  linhas.push({
    rotulo: 'Escore total',
    valor:
      escore.total != null
        ? `${formatarNumero(escore.total, 1, 1)} de ${escore.totalMaximo}`
        : formatarNumero(null),
    detalhe: escore.classificacao?.rotulo ?? escore.motivoAusencia ?? undefined,
  })

  return linhas
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
