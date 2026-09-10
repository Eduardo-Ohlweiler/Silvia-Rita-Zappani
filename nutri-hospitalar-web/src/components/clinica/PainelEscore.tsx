import { TResult } from '@/components/common'
import type { Escore } from '@/types/clinica'
import { formatarNumero } from '@/utils/format'

/**
 * O escore de uma escala nutricional pontuada — MNA®, NRS-2002 (docs/13).
 *
 * <p><b>Esta tela não soma nada.</b> Todo número, faixa, frase e procedência
 * vem do servidor, que é onde a régua mora com fonte citada. A regra do
 * denominador da adesão chegou a existir em quatro linguagens neste sistema, e
 * sete telas erraram — um somador em TypeScript seria a quinta.
 *
 * <p>E ausência é sempre explicada: numa escala, soma parcial não é escore
 * menor, é escore <b>errado</b>. Cinco perguntas em branco fazem a MNA somar 12
 * e parecer "sob risco" num paciente que pode ser normal. Por isso o subtotal
 * vem nulo enquanto o bloco estiver incompleto, e o motivo nomeia as perguntas
 * que faltam.
 */
export function PainelEscore({
  escore,
  recalculando = false,
}: {
  escore: Escore
  recalculando?: boolean
}) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {escore.grupos.map((g) => (
          <TResult
            key={g.grupo}
            label={g.rotulo}
            /*
             * `undefined`, e nunca um traço montado à mão: `formatarNumero(null)`
             * devolve a string '—', e foi assim que 26 motivos ficaram mudos
             * numa tela de UTI enquanto o servidor os calculava fielmente. O
             * `TResult` reconhece a constante `AUSENTE`, mas o ponto novo não
             * depende disso para estar certo.
             */
            valor={g.subtotal != null ? formatarNumero(g.subtotal, 1, 1) : undefined}
            unidade={g.subtotal != null && g.maximo != null ? `de ${g.maximo}` : undefined}
            classificacao={g.classificacao}
            motivoAusencia={g.motivoAusencia}
            recalculando={recalculando}
          />
        ))}

        {/*
          O ajuste por idade aparece como LINHA PRÓPRIA, e não somado calado
          dentro do total. Quando a regra soma ou descarta um valor, o servidor
          publica o valor — senão a conta não pode ser refeita à mão, e um
          prontuário que não fecha faz quem confere concluir que o sistema errou.
        */}
        {escore.ajusteIdade != null && (
          <TResult
            label="Ajuste por idade"
            valor={`+ ${formatarNumero(escore.ajusteIdade, 1, 1)}`}
            referencia={escore.ajusteIdadeDescricao ?? undefined}
            recalculando={recalculando}
          />
        )}

        <TResult
          label="Escore total"
          valor={escore.total != null ? formatarNumero(escore.total, 1, 1) : undefined}
          unidade={escore.total != null ? `de ${escore.totalMaximo}` : undefined}
          classificacao={escore.classificacao}
          referencia={escore.referencia}
          motivoAusencia={escore.motivoAusencia}
          recalculando={recalculando}
        />
      </div>

      {/* A conduta que a PUBLICAÇÃO prescreve. A MNA não prescreve nenhuma, e
          por isso não inventamos uma para ela. */}
      {escore.conclusao && (
        <p className="text-body font-medium text-txt">{escore.conclusao}</p>
      )}

      {/* As perguntas que faltam, por extenso. A frase do motivo cita duas e
          conta o resto; quem precisa ir preencher precisa da lista. */}
      {escore.grupos
        .filter((g) => g.perguntasSemResposta.length > 0)
        .map((g) => (
          <div key={`${g.grupo}-faltando`} className="text-caption text-txt-muted">
            <span className="font-medium">{g.rotulo}</span> — falta responder:
            <ul className="mt-1 list-disc pl-5">
              {g.perguntasSemResposta.map((r) => (
                <li key={r}>{r}</li>
              ))}
            </ul>
          </div>
        ))}
    </div>
  )
}
