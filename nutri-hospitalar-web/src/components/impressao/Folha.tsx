import type { ReactNode } from 'react'
import LogoFull from '@/assets/brand/logo-full.svg?react'

/**
 * A folha impressa — e ela **não é a tela**.
 *
 * Imprimir o formulário dava formulário: campos vazios, rótulos de entrada,
 * abas. O que quem lê no papel precisa é de um **demonstrativo**: faixa de
 * identificação no topo, tira de indicadores, seções com o número já
 * calculado. É o desenho que o eroERP monta com jsPDF em
 * `utils/geradorPdf.ts`.
 *
 * <b>O desenho é o mesmo; a ferramenta não.</b> jsPDF seria dependência nova,
 * layout imperativo em milímetros e — o que pesa de verdade — uma **segunda
 * montagem** do mesmo documento, com os números reescritos à mão. Duas
 * montagens divergem, e a do papel envelhece calada. Aqui o documento lê os
 * mesmos objetos que a tela leu, com os mesmos formatadores e os mesmos tokens
 * de cor.
 *
 * Na tela a folha não existe (`display: none`); no papel ela é a única coisa
 * que existe, porque a `TPage` marca o conteúdo de tela como `.nao-imprime`
 * assim que recebe um documento.
 */
export function Folha({
  titulo,
  paciente,
  referencia,
  children,
}: {
  /** O que este papel é: "Avaliação de terapia nutricional". */
  titulo: string
  /** De quem ele é. Sem paciente — a calculadora — diz o que for verdade. */
  paciente?: string
  /** Linha fina sob a faixa: período, profissional, o que situar o documento. */
  referencia?: ReactNode
  children: ReactNode
}) {
  return (
    <article className="folha">
      {/* Faixa de identificação: marca à esquerda, documento à direita. */}
      <header className="folha-faixa">
        <LogoFull className="folha-logo" />
        <div className="folha-identidade">
          <h1 className="folha-titulo">{titulo}</h1>
          {paciente && <p className="folha-paciente">{paciente}</p>}
        </div>
      </header>

      {referencia && <p className="folha-referencia">{referencia}</p>}

      <div className="folha-corpo">{children}</div>

      {/* A numeração de página vem do diálogo do navegador: `counter(page)`
          dentro de `@page` não é suportado por nenhum deles. */}
      <footer className="folha-rodape">
        Documento gerado por Nutri Hospitalar de Sucesso ·{' '}
        {new Date().toLocaleString('pt-BR')}
      </footer>
    </article>
  )
}

/**
 * A tira de indicadores do topo — o que se lê primeiro, em caixa destacada.
 *
 * Cinco no máximo por tira: acima disso a coluna fica estreita demais para o
 * número respirar, e o valor perde a hierarquia sobre o rótulo.
 */
export function TiraIndicadores({ itens }: { itens: { rotulo: string; valor: string }[] }) {
  if (itens.length === 0) return null
  return (
    <div className="folha-tira">
      {itens.map((i) => (
        <div key={i.rotulo} className="folha-tira-item">
          <span className="folha-tira-rotulo">{i.rotulo}</span>
          <span className="folha-tira-valor numeric">{i.valor}</span>
        </div>
      ))}
    </div>
  )
}

/** Uma seção do documento, com barra de título na cor da marca. */
export function Secao({
  titulo,
  nota,
  children,
}: {
  titulo: string
  /** A procedência, quando importa. Prescrição sem fonte não é auditável. */
  nota?: string
  children: ReactNode
}) {
  return (
    <section className="folha-secao">
      <h2 className="folha-secao-titulo">{titulo}</h2>
      {nota && <p className="folha-secao-nota">{nota}</p>}
      {children}
    </section>
  )
}

export interface LinhaValor {
  rotulo: string
  valor: string
  /** Classificação, origem do valor, o que qualifica o número à direita. */
  detalhe?: string
}

/**
 * Tabela rótulo → valor, que é o corpo da maior parte de um prontuário.
 *
 * <b>Linha sem valor não é omitida.</b> Um prontuário que esconde o que não foi
 * medido mente por omissão: quem lê não distingue "não tem" de "não coube na
 * folha". O traço aqui é legítimo, ao contrário do da tela — o papel não tem
 * como perguntar de novo, e o motivo, quando existe, vai no detalhe.
 */
export function LinhasDeValor({ linhas }: { linhas: LinhaValor[] }) {
  return (
    <table className="folha-tabela folha-tabela-valores">
      <tbody>
        {linhas.map((l) => (
          <tr key={l.rotulo}>
            <th scope="row">{l.rotulo}</th>
            <td className="numeric">{l.valor}</td>
            <td className="folha-detalhe">{l.detalhe ?? ''}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

/** Tabela de verdade — várias colunas, várias linhas. */
export function TabelaDoc<T>({
  colunas,
  linhas,
  chaveDe,
  vazio = 'Sem registro no período.',
}: {
  colunas: { titulo: string; celula: (linha: T) => ReactNode; numerica?: boolean }[]
  linhas: T[]
  chaveDe: (linha: T, indice: number) => string | number
  vazio?: string
}) {
  if (linhas.length === 0) return <p className="folha-vazio">{vazio}</p>

  return (
    <table className="folha-tabela">
      <thead>
        <tr>
          {colunas.map((c) => (
            <th key={c.titulo} className={c.numerica ? 'folha-num' : undefined}>
              {c.titulo}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {linhas.map((l, i) => (
          <tr key={chaveDe(l, i)}>
            {colunas.map((c) => (
              <td key={c.titulo} className={c.numerica ? 'folha-num numeric' : undefined}>
                {c.celula(l)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

/** Observação livre, quando o registro tem uma. */
export function Observacao({ texto }: { texto?: string | null }) {
  if (!texto) return null
  return (
    <section className="folha-secao">
      <h2 className="folha-secao-titulo">Observações</h2>
      <p className="folha-observacao">{texto}</p>
    </section>
  )
}
