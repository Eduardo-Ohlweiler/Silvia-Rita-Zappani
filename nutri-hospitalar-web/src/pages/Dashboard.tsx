import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { IconAdicionar } from '@/assets/icons'
import { TBadge, TButton, TDataGrid, TPage, TPanel, type Coluna } from '@/components/common'
import { useAuth } from '@/hooks/useAuth'
import { handleApiError } from '@/services/api'
import { inicioService } from '@/services/inicioService'
import type {
  AlertaAdesao,
  MudancaDeFaixa,
  LinhaDaRonda,
  PainelInicial,
  SemAvaliacao,
} from '@/types/inicio'
import { formatarData, formatarNumero } from '@/utils/format'
import { idadeEmMesesTexto } from '@/utils/idade'

/**
 * A tela inicial — **a lista de trabalho do dia**, não um resumo gerencial.
 *
 * A pergunta que ela responde é *"o que preciso fazer hoje"*. Paciente de UTI
 * não tem hora marcada: ele está no leito, e a nutricionista faz ronda. Por
 * isso aqui não há agenda — há **quem ainda não foi atendido hoje**.
 *
 * Para "como foi o período" existem as duas telas *em números*, e esta tela
 * aponta para elas em vez de repeti-las.
 *
 * **Nenhuma constante clínica nasce aqui.** O único julgamento é o alerta de
 * adesão, cuja régua já é do projeto (ESPEN, `docs/10`), e ele vem com a
 * procedência escrita ao lado. "Há quantos dias sem avaliar" sai como número e
 * ordem, **sem cor e sem limite**: não existe no sistema regra de "reavaliar a
 * cada N dias", e cravá-la numa tela seria inventar constante clínica.
 */
export function Dashboard() {
  const navigate = useNavigate()
  const { sessao } = useAuth()

  const [dados, setDados] = useState<PainelInicial>()
  const [carregando, setCarregando] = useState(true)

  const carregar = useCallback(() => {
    setCarregando(true)
    inicioService
      .painel()
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
    // `sessao.tenantId` é sinal de invalidação: trocar de tenant refaz a
    // consulta, senão a tela fica com o trabalho do cliente anterior.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessao?.tenantId])

  useEffect(carregar, [carregar])

  const ronda: Coluna<LinhaDaRonda>[] = [
    { chave: 'nome', cabecalho: 'Paciente', render: (p) => p.pessoaNome },
    {
      chave: 'ultimo',
      cabecalho: 'Último registro',
      render: (p) => formatarData(p.ultimoDia),
    },
    {
      chave: 'dias',
      cabecalho: 'Dias em aberto',
      numerica: true,
      // Número puro, sem cor: quem lê decide o que é muito.
      render: (p) => p.diasSemRegistro,
    },
    {
      /*
       * O estado em cada linha é o que faz a lista ser a RONDA. Sem ele a tela
       * dizia "1 de 1 registrados" com nada embaixo — o número e a lista se
       * contradizendo, e quem lia entendia "não entrou nada" em vez de
       * "terminei".
       */
      chave: 'estado',
      cabecalho: 'Hoje',
      render: (p) =>
        p.registradoHoje ? (
          <TBadge tom="sucesso">Registrado</TBadge>
        ) : (
          <TBadge tom="alerta">Pendente</TBadge>
        ),
    },
  ]

  const alertas: Coluna<AlertaAdesao>[] = [
    { chave: 'nome', cabecalho: 'Paciente', render: (a) => a.pessoaNome },
    {
      chave: 'adesao',
      cabecalho: 'Adesão média',
      numerica: true,
      render: (a) => <TBadge tom="alerta">{formatarNumero(a.adesaoMedia, 1)} %</TBadge>,
    },
    {
      chave: 'dia',
      cabecalho: 'Dia da terapia',
      numerica: true,
      render: (a) => a.diasDeTerapia,
    },
  ]

  const faixas: Coluna<MudancaDeFaixa>[] = [
    { chave: 'nome', cabecalho: 'Criança', render: (m) => m.pacienteNome },
    {
      chave: 'indice',
      cabecalho: 'Índice',
      secundaria: true,
      render: (m) => m.indice,
    },
    {
      chave: 'mudanca',
      cabecalho: 'Mudou de',
      render: (m) => (
        <span className="flex flex-wrap items-center gap-1.5">
          <span className="text-txt-secondary">{m.de}</span>
          <span className="text-txt-muted">→</span>
          <span className="font-medium text-txt">{m.para}</span>
        </span>
      ),
    },
    {
      chave: 'quando',
      cabecalho: 'Na avaliação de',
      secundaria: true,
      render: (m) => `${formatarData(m.dataAtual)} · ${idadeEmMesesTexto(m.idadeMeses)}`,
    },
  ]

  const semAvaliacao: Coluna<SemAvaliacao>[] = [
    { chave: 'nome', cabecalho: 'Paciente', render: (s) => s.pacienteNome },
    { chave: 'modulo', cabecalho: 'Módulo', secundaria: true, render: (s) => s.modulo },
    {
      chave: 'ultima',
      cabecalho: 'Última avaliação',
      render: (s) => formatarData(s.ultimaAvaliacao),
    },
    {
      chave: 'dias',
      cabecalho: 'Dias',
      numerica: true,
      render: (s) => s.diasSemAvaliacao,
    },
  ]

  return (
    <TPage
      title={`Olá, ${sessao?.nome.split(' ')[0] ?? ''}`}
      subtitle={
        dados
          ? `${formatarData(dados.hoje)} — o que precisa de você hoje.`
          : 'Carregando o trabalho do dia…'
      }
      actions={
        <>
          <TButton variant="secondary" onClick={() => navigate('/app/uti/avaliacoes/nova')}>
            <IconAdicionar className="size-4" />
            Avaliação de UTI
          </TButton>
          <TButton variant="secondary" onClick={() => navigate('/app/pediatria/avaliacoes/nova')}>
            <IconAdicionar className="size-4" />
            Avaliação pediátrica
          </TButton>
          <TButton onClick={() => navigate('/app/uti/acompanhamento/novo')}>
            <IconAdicionar className="size-4" />
            Registrar o dia
          </TButton>
        </>
      }
    >
      <div className={`flex flex-col gap-4 ${carregando ? 'opacity-50 transition-opacity' : ''}`}>
        {/* ─── A ronda de hoje ───────────────────────────────────────── */}
        <TPanel
          title="A ronda de hoje"
          // A frase inteira, e não só o número: "3" sozinho não diz de quantos.
          subtitle={
            dados
              ? dados.totalEmAcompanhamento === 0
                ? 'Nenhum paciente em acompanhamento no momento.'
                : dados.registradosHoje === dados.totalEmAcompanhamento
                  ? `Ronda concluída — ${dados.registradosHoje} de ${dados.totalEmAcompanhamento} registrados hoje.`
                  : `${dados.registradosHoje} de ${dados.totalEmAcompanhamento} já registrados hoje.`
              : undefined
          }
        >
          <TDataGrid
            colunas={ronda}
            linhas={dados?.ronda ?? []}
            chaveDe={(p) => p.pessoaId}
            carregando={carregando}
            tituloCartao={(p) => p.pessoaNome}
            /*
             * Quem já registrou abre AQUELE dia; quem não, abre um novo com o
             * paciente escolhido. Mandar todo mundo para "novo" fazia quem já
             * tinha registro cair num formulário em branco — e ainda arriscava
             * o 409 do um-registro-por-dia.
             */
            onLinhaClick={(p) =>
              navigate(
                p.registroDeHojeId
                  ? `/app/uti/acompanhamento/${p.registroDeHojeId}`
                  : `/app/uti/acompanhamento/novo?pessoaId=${p.pessoaId}` +
                      `&pessoaNome=${encodeURIComponent(p.pessoaNome)}`,
              )
            }
            vazioTitulo="Nenhum acompanhamento em curso"
            vazioDescricao="Um paciente entra aqui ao ganhar o primeiro dia de acompanhamento, e sai quando o acompanhamento é encerrado."
          />
        </TPanel>

        <div className="grid gap-4 xl:grid-cols-2">
          {/* ─── Adesão ─────────────────────────────────────────────── */}
          <TPanel
            title="Adesão abaixo de 70 %"
            /*
             * A procedência na própria tela, e não só no documento: sem a
             * segunda metade da regra o alerta pareceria condenar a progressão,
             * que é justamente o que a ESPEN recomenda fazer.
             */
            subtitle="Só depois do 7º dia de terapia. Nos primeiros dias a oferta baixa é conduta (ESPEN); sustentada depois disso, associa-se a pior desfecho."
          >
            <TDataGrid
              colunas={alertas}
              linhas={dados?.adesaoBaixa ?? []}
              chaveDe={(a) => a.pessoaId}
              carregando={carregando}
              tituloCartao={(a) => a.pessoaNome}
              onLinhaClick={(a) =>
                navigate(`/app/uti/painel-acompanhamento?pacienteId=${a.pessoaId}`)
              }
              vazioTitulo="Nenhum alerta"
              vazioDescricao="Ninguém com adesão sustentadamente baixa fora da primeira semana."
            />
          </TPanel>

          {/* ─── Mudança de faixa ───────────────────────────────────── */}
          <TPanel
            title="Mudaram de faixa"
            subtitle="Entre as duas últimas avaliações, na classificação da OMS. A tela mostra a mudança; a leitura é sua."
          >
            <TDataGrid
              colunas={faixas}
              linhas={dados?.mudancasDeFaixa ?? []}
              chaveDe={(m) => `${m.avaliacaoId}-${m.indice}`}
              carregando={carregando}
              tituloCartao={(m) => m.pacienteNome}
              onLinhaClick={(m) => navigate(`/app/pediatria/avaliacoes/${m.avaliacaoId}`)}
              vazioTitulo="Nenhuma mudança"
              vazioDescricao="Nenhuma criança trocou de faixa na última avaliação. Quem tem só uma avaliação não aparece — não há de onde comparar."
            />
          </TPanel>
        </div>

        {/* ─── Há mais tempo sem avaliação ───────────────────────────── */}
        <TPanel
          title="Há mais tempo sem avaliação"
          subtitle="Ordenado do mais antigo. Sem limite e sem cor de propósito: não existe aqui uma regra de reavaliar a cada N dias, e o número já responde."
        >
          <TDataGrid
            colunas={semAvaliacao}
            linhas={dados?.haMaisTempoSemAvaliacao ?? []}
            chaveDe={(s) => `${s.pacienteId}-${s.modulo}`}
            carregando={carregando}
            tituloCartao={(s) => s.pacienteNome}
            vazioTitulo="Nenhum paciente avaliado ainda"
            vazioDescricao="Cadastre uma pessoa e faça a primeira avaliação para ela aparecer aqui."
          />
        </TPanel>

        {/* ─── Números do mês ────────────────────────────────────────── */}
        <TPanel
          title="Últimos 30 dias"
          /*
           * Corridos, e não o mês corrente: no dia 1º um bloco "este mês"
           * nasceria zerado e pareceria quebrado. O recorte de fechamento vive
           * nas telas em números, com filtro de período.
           */
          subtitle="O volume recente. O resumo por período está nas telas em números, que é onde ele cabe."
        >
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {/* Pacientes, e não avaliações: é uma linha por paciente. */}
            <Numero rotulo="Pacientes de UTI avaliados" valor={dados?.numeros.pacientesUti} />
            <Numero rotulo="Crianças avaliadas" valor={dados?.numeros.pacientesPediatria} />
            <Numero rotulo="Dias registrados" valor={dados?.numeros.diasRegistrados} />
            <Numero
              rotulo="Adesão média"
              valor={dados?.numeros.adesaoMediaUti}
              unidade="%"
              nota="não é nota: a meta é progressiva na 1ª semana"
            />
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <TButton variant="secondary" size="sm" onClick={() => navigate('/app/uti/painel')}>
              UTI em números
            </TButton>
            <TButton
              variant="secondary"
              size="sm"
              onClick={() => navigate('/app/pediatria/dashboard')}
            >
              Pediatria em números
            </TButton>
          </div>
        </TPanel>
      </div>
    </TPage>
  )
}

/** Ausência continua sendo ausência: nulo vira traço, não zero. */
function Numero({
  rotulo,
  valor,
  unidade,
  nota,
}: {
  rotulo: string
  valor?: number | null
  unidade?: string
  nota?: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-caption text-txt-secondary">{rotulo}</span>
      <span className="text-display font-semibold text-txt">
        {valor == null ? '—' : formatarNumero(valor, 1)}
        {valor != null && unidade ? <span className="text-caption"> {unidade}</span> : null}
      </span>
      {nota && <span className="text-caption text-txt-muted">{nota}</span>}
    </div>
  )
}
