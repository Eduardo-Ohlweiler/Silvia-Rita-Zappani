import {
  Folha,
  LinhasDeValor,
  Observacao,
  Secao,
  TiraIndicadores,
  type LinhaValor,
} from '@/components/impressao/Folha'
import type { RegistroDiarioPediatricoResponse } from '@/types/pediatria'
import { AUSENTE, formatarData, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto } from '@/utils/idade'

const n = (valor?: number | null, casas = 1, unidade = '') =>
  valor == null ? AUSENTE : `${formatarNumero(valor, casas, casas)}${unidade ? ` ${unidade}` : ''}`

/**
 * Um dia de acompanhamento pediátrico, como **evolução de prontuário**.
 *
 * A folha traz três blocos: quem e quando (com a **idade daquele dia**), o estado
 * nutricional medido, e o que a criança recebeu contra o que estava prescrito.
 *
 * **A idade vai no cabeçalho, não numa linha qualquer.** É ela que dá sentido a
 * todo o resto: 9 kg aos 8 meses e 9 kg aos 3 anos são leituras opostas, e quem
 * lê a folha longe do sistema não tem como calcular.
 *
 * **Motivo de ausência aparece no papel.** Quando um número não saiu, a linha diz
 * por quê em vez de mostrar traço — é o mesmo princípio da tela, e no papel vale
 * mais, porque ninguém pode clicar para descobrir.
 */
export function DocumentoAcompanhamentoPediatrico({
  registro,
}: {
  registro: RegistroDiarioPediatricoResponse
}) {
  const r = registro
  const d = r.derivados

  const antropometria: LinhaValor[] = [
    {
      rotulo: 'Peso do dia',
      valor: n(r.pesoKg, 3, 'kg'),
      detalhe: d.pesoIdade?.rotulo ?? d.motivoEstadoNutricional ?? '',
    },
    {
      rotulo: 'Estatura',
      valor: n(r.estaturaCm, 1, 'cm'),
      detalhe: d.estaturaIdade?.rotulo ?? '',
    },
    {
      rotulo: 'IMC',
      valor: n(d.imc, 2, 'kg/m²'),
      detalhe: d.imcIdade?.rotulo ?? d.motivoImc ?? '',
    },
  ]

  /*
   * O prescrito CONTRA O QUAL a adesão foi medida — a ordem era a inversa, e
   * com ela a folha se contradizia: 700 de 880 não é 83,3 %. Quem resolve a
   * precedência é o servidor (`docs/11 §5`), e ele manda o número escolhido:
   * a folha só o mostra.
   */
  const dieta: LinhaValor[] = [
    {
      rotulo: 'Volume prescrito em 24 h',
      valor: n(d.prescritoDeReferencia, 0, 'ml'),
      detalhe: d.referenciaDoRecebido ?? '',
    },
    { rotulo: 'Volume recebido em 24 h', valor: n(r.volRecebido24h, 0, 'ml') },
    {
      rotulo: 'Do prescrito',
      valor: n(d.percentualRecebido, 1, '%'),
      // A referência diz contra o quê comparou; o motivo, por que não comparou.
      detalhe: d.referenciaDoRecebido ?? d.motivoPercentualRecebido ?? '',
    },
    {
      rotulo: 'Energia recebida',
      valor: n(d.caloriasRecebidas, 0, 'kcal'),
      detalhe:
        d.caloriasPorKg != null
          ? `${n(d.caloriasPorKg, 1)} kcal/kg`
          : (d.motivoPorQuilo ?? d.motivoOferta ?? ''),
    },
    {
      rotulo: 'Proteína recebida',
      valor: n(d.proteinaRecebida, 1, 'g'),
      detalhe: d.proteinaPorKg != null ? `${n(d.proteinaPorKg, 2)} g/kg` : '',
    },
    {
      rotulo: 'Tomadas aceitas',
      valor:
        r.tomadasAceitas != null && r.tomadasPrevistas != null
          ? `${r.tomadasAceitas} de ${r.tomadasPrevistas}`
          : AUSENTE,
      detalhe:
        d.aceitacaoTomadas != null
          ? `${n(d.aceitacaoTomadas, 1)} %`
          : (d.motivoAceitacao ?? ''),
    },
  ]

  const adequacoes: LinhaValor[] = [
    {
      rotulo: 'Adequação calórica',
      valor: n(d.adequacaoCalorica, 1, '%'),
      detalhe:
        r.avaliacaoVet != null
          ? `de ${n(r.avaliacaoVet, 1)} kcal/dia`
          : (d.motivoAdequacaoCalorica ?? ''),
    },
    {
      rotulo: 'Adequação proteica',
      valor: n(d.adequacaoProteica, 1, '%'),
      detalhe:
        r.avaliacaoProteinaNecessidade != null
          ? `de ${n(r.avaliacaoProteinaNecessidade, 1)} g/dia`
          : (d.motivoAdequacaoProteica ?? ''),
    },
  ]

  return (
    <Folha
      titulo="Evolução nutricional pediátrica"
      paciente={r.pessoaNome}
      referencia={
        <>
          {formatarData(r.data)}
          {/* A idade DAQUELE dia, não a de hoje: é o que torna a folha legível. */}
          {d.idadeMeses != null ? ` · ${idadeEmMesesTexto(d.idadeMeses)}` : ''}
          {r.avaliacaoData
            ? ` · prescrição da avaliação de ${formatarData(r.avaliacaoData)}`
            : ' · sem avaliação vinculada'}
        </>
      }
    >
      <TiraIndicadores
        itens={[
          { rotulo: 'Peso', valor: n(r.pesoKg, 3, 'kg') },
          { rotulo: 'Recebido', valor: n(r.volRecebido24h, 0, 'ml') },
          { rotulo: 'Do prescrito', valor: n(d.percentualRecebido, 1, '%') },
          { rotulo: 'Energia', valor: n(d.caloriasPorKg, 1, 'kcal/kg') },
          { rotulo: 'Do VET', valor: n(d.adequacaoCalorica, 1, '%') },
        ]}
      />

      <Secao
        titulo="Estado nutricional no dia"
        nota={
          d.motivoEstadoNutricional ??
          'Classificação pelas curvas da OMS, com o peso e a idade deste dia — P15 e P85 (docs/09 §4).'
        }
      >
        <LinhasDeValor linhas={antropometria} />
      </Secao>

      <Secao titulo="Dieta láctea recebida">
        <LinhasDeValor linhas={dieta} />
      </Secao>

      <Secao
        titulo="Adequação às necessidades"
        nota="As metas vêm da avaliação vinculada, e as DRIs 2002 as definem até 35 meses (energia) e 36 meses (proteína). Acima disso o sistema classifica o estado nutricional, mas não calcula necessidade."
      >
        <LinhasDeValor linhas={adequacoes} />
      </Secao>

      {r.observacao && <Observacao texto={r.observacao} />}
    </Folha>
  )
}
