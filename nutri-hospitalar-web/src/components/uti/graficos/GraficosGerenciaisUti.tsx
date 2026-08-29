import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  LabelList,
  Legend,
  Line,
  Pie,
  PieChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EIXO, GRADE, GraficoVazio, TooltipCartao } from '@/components/graficos/chrome'
import { formatarNumero } from '@/utils/format'
import type { ContagemRotuladaUti, ContagemUti, PontoPeriodoUti, TomResultado } from '@/types/uti'

/**
 * Cor por **tom gravado**, e nunca por texto.
 *
 * O tom vem do servidor junto com o rótulo, decidido pelo mesmo classificador
 * que decidiu o nome. O eroERP colore com `texto.includes('adequado')`, o que
 * quebra em "Adequada" e em qualquer rótulo que use a palavra por acaso.
 *
 * `NEUTRO` é também a cor de "não classificado": a ausência de classificação
 * não é uma categoria clínica e não pode ganhar cor de uma.
 */
const COR_TOM: Record<TomResultado, string> = {
  NEUTRO: 'var(--txt-muted)',
  ADEQUADO: 'var(--viz-ordinal-1)',
  ATENCAO: 'var(--viz-ordinal-3)',
  CRITICO: 'var(--viz-ordinal-5)',
}

/**
 * Avaliações e dias de acompanhamento por mês.
 *
 * As duas séries são **contagens** — mesma unidade, mesmo eixo. As barras são
 * as avaliações, e os dias vêm como linha porque são muito mais numerosos e
 * duas séries de barras da mesma largura disputariam a leitura.
 */
export function MovimentoPorPeriodo({ dados }: { dados: PontoPeriodoUti[] }) {
  if (dados.length === 0) return <GraficoVazio>Sem movimento no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={260}>
      <ComposedChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="periodo" {...EIXO} />
        <YAxis {...EIXO} width={44} allowDecimals={false} />

        <Bar
          dataKey="avaliacoes"
          name="Avaliações"
          fill="var(--viz-serie-1)"
          radius={[3, 3, 0, 0]}
          maxBarSize={40}
          isAnimationActive={false}
        />
        <Line
          type="monotone"
          dataKey="dias"
          name="Dias de acompanhamento"
          stroke="var(--viz-serie-2)"
          strokeWidth={2}
          isAnimationActive={false}
          dot={false}
        />

        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as PontoPeriodoUti
            return (
              <TooltipCartao
                titulo={p.periodo}
                linhas={[
                  { rotulo: 'Avaliações', cor: 'var(--viz-serie-1)', valor: p.avaliacoes },
                  { rotulo: 'Dias', cor: 'var(--viz-serie-2)', valor: p.dias },
                ]}
              />
            )
          }}
        />
        <Legend
          verticalAlign="top"
          align="right"
          wrapperStyle={{ fontSize: 12, color: 'var(--txt-secondary)', paddingBottom: 8 }}
        />
      </ComposedChart>
    </ResponsiveContainer>
  )
}

/**
 * Distribuição de uma classificação clínica, em barras horizontais.
 *
 * Horizontal porque os rótulos são longos — "Obesidade grau III", "Desnutrição
 * grave" — e na vertical eles se inclinam ou se cortam.
 */
export function DistribuicaoPorTom({ dados }: { dados: ContagemRotuladaUti[] }) {
  if (dados.length === 0) return <GraficoVazio>Nenhuma avaliação classificada no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={Math.max(180, dados.length * 34 + 40)}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 32, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="rotulo" {...EIXO} width={150} />
        <Bar dataKey="quantidade" isAnimationActive={false} maxBarSize={22} radius={[0, 4, 4, 0]}>
          {dados.map((d) => (
            <Cell key={d.rotulo} fill={COR_TOM[d.tom ?? 'NEUTRO']} />
          ))}
          <LabelList
            dataKey="quantidade"
            position="right"
            fill="var(--txt-secondary)"
            fontSize={12}
          />
        </Bar>
        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as ContagemRotuladaUti
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[
                  {
                    rotulo: 'Avaliações',
                    cor: COR_TOM[p.tom ?? 'NEUTRO'],
                    valor: p.quantidade,
                  },
                ]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * Categoria sem ordem natural — nome de fórmula, modo de infusão — e por isso
 * **uma cor só**. Colorir cada barra duplicaria o que o comprimento já diz.
 */
export function BarrasNominaisUti({
  dados,
  rotuloValor = 'Avaliações',
}: {
  dados: ContagemUti[]
  rotuloValor?: string
}) {
  if (dados.length === 0) return <GraficoVazio>Sem dados no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={Math.max(180, dados.length * 34 + 40)}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 32, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="rotulo" {...EIXO} width={150} />
        <Bar
          dataKey="quantidade"
          fill="var(--viz-serie-1)"
          isAnimationActive={false}
          maxBarSize={22}
          radius={[0, 4, 4, 0]}
        >
          <LabelList
            dataKey="quantidade"
            position="right"
            fill="var(--txt-secondary)"
            fontSize={12}
          />
        </Bar>
        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as ContagemUti
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[{ rotulo: rotuloValor, cor: 'var(--viz-serie-1)', valor: p.quantidade }]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * A rampa ordinal do tema. Cinco degraus, e **cinco é o teto**: `docs/05 §10.4`
 * mede que seis não mantêm o intervalo mínimo de luminosidade entre vizinhos.
 *
 * A rampa é monotônica em luminosidade nos dois temas — verificado, não
 * suposto. É por isso que ela pode ser lida como escala mesmo em preto e branco.
 */
const RAMPA_ORDINAL = [
  'var(--viz-ordinal-1)',
  'var(--viz-ordinal-2)',
  'var(--viz-ordinal-3)',
  'var(--viz-ordinal-4)',
  'var(--viz-ordinal-5)',
]

/**
 * Faixa etária em **colunas**, com a rampa ordinal.
 *
 * Coluna e não barra horizontal porque os rótulos são curtos e a leitura é de
 * uma escala que anda para a direita — idade tem ordem, e a vertical é onde o
 * olho espera encontrar magnitude ao longo dela.
 *
 * A barra de "idade não informada" fica **fora da rampa**, em cinza: ela não é
 * um degrau da escala, é a ausência dela. Pintá-la com o sexto tom diria que
 * existe uma faixa etária acima de 80.
 */
export function ColunasFaixaEtaria({ dados }: { dados: ContagemUti[] }) {
  if (dados.every((d) => d.quantidade === 0))
    return <GraficoVazio>Nenhuma avaliação com idade informada no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={dados} margin={{ top: 20, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="rotulo" {...EIXO} interval={0} />
        <YAxis {...EIXO} width={40} allowDecimals={false} />
        <Bar dataKey="quantidade" isAnimationActive={false} radius={[4, 4, 0, 0]} maxBarSize={64}>
          {dados.map((d, i) => (
            <Cell
              key={d.rotulo}
              fill={
                d.rotulo.startsWith('Idade não')
                  ? 'var(--txt-muted)'
                  : RAMPA_ORDINAL[Math.min(i, RAMPA_ORDINAL.length - 1)]
              }
            />
          ))}
          <LabelList
            dataKey="quantidade"
            position="top"
            fill="var(--txt-secondary)"
            fontSize={12}
          />
        </Bar>
        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as ContagemUti
            return (
              <TooltipCartao
                titulo={p.rotulo}
                linhas={[{ rotulo: 'Avaliações', valor: p.quantidade }]}
              />
            )
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * Rosca de composição — **e só onde a rosca é honesta**.
 *
 * O guia de visualização e o `docs/05 §10.5` recusam pizza nos mesmos dois
 * casos: para comparar valores próximos, e com duas fatias. Por isso este
 * componente **se recusa a desenhar** com menos de três segmentos e devolve uma
 * barra de proporção no lugar — é o caso de "fase da terapia" e "modo de
 * infusão", que têm duas opções cada.
 *
 * Com três a seis segmentos de tamanhos distintos, a rosca faz o que nenhuma
 * barra faz bem: mostra a **parte contra o todo** de relance, sem o leitor
 * precisar somar as barras de cabeça.
 */
export function RoscaDeComposicao({
  dados,
  rotuloValor = 'Avaliações',
}: {
  dados: ContagemUti[]
  rotuloValor?: string
}) {
  const comValor = dados.filter((d) => d.quantidade > 0)
  const total = comValor.reduce((soma, d) => soma + d.quantidade, 0)

  if (total === 0) return <GraficoVazio>Sem dados no período.</GraficoVazio>

  // Menos de três fatias: a rosca vira decoração e a barra diz melhor.
  if (comValor.length < 3) return <BarraDeProporcao dados={comValor} rotuloValor={rotuloValor} />

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <ResponsiveContainer width="100%" height={200} className="sm:!w-1/2">
        <PieChart>
          <Pie
            data={comValor}
            dataKey="quantidade"
            nameKey="rotulo"
            innerRadius="58%"
            outerRadius="86%"
            /* 2 px de superfície entre as fatias — separação por vão, nunca por
               contorno desenhado em volta da marca. */
            paddingAngle={2}
            stroke="var(--surface)"
            strokeWidth={2}
            isAnimationActive={false}
          >
            {comValor.map((d, i) => (
              <Cell key={d.rotulo} fill={RAMPA_ORDINAL[i % RAMPA_ORDINAL.length]} />
            ))}
          </Pie>
          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const p = payload[0].payload as ContagemUti
              return (
                <TooltipCartao
                  titulo={p.rotulo}
                  linhas={[
                    { rotulo: rotuloValor, valor: p.quantidade },
                    {
                      rotulo: 'Participação',
                      valor: `${formatarNumero((p.quantidade / total) * 100, 1, 1)} %`,
                    },
                  ]}
                />
              )
            }}
          />
        </PieChart>
      </ResponsiveContainer>

      {/* A legenda é tabela, não caixa de cores: identidade nunca fica só na
          cor, e o número ao lado dispensa medir a fatia com o olho. */}
      <ul className="flex flex-1 flex-col divide-y divide-line">
        {comValor.map((d, i) => (
          <li key={d.rotulo} className="flex items-center gap-2 py-1.5">
            <span
              aria-hidden="true"
              className="size-2.5 shrink-0 rounded-full"
              style={{ background: RAMPA_ORDINAL[i % RAMPA_ORDINAL.length] }}
            />
            <span className="truncate text-caption text-txt-secondary">{d.rotulo}</span>
            <span className="numeric ml-auto shrink-0 text-caption text-txt">
              {d.quantidade}
              <span className="ml-1.5 text-txt-muted">
                {formatarNumero((d.quantidade / total) * 100, 1, 1)} %
              </span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * Divisão de duas ou três partes numa barra só, com rótulo direto em cada uma.
 *
 * É o que substitui a pizza de duas fatias: mostra a proporção sem pedir que o
 * olho compare ângulos, que é justamente o que a pizza faz mal.
 */
export function BarraDeProporcao({
  dados,
  rotuloValor = 'Avaliações',
}: {
  dados: ContagemUti[]
  rotuloValor?: string
}) {
  const comValor = dados.filter((d) => d.quantidade > 0)
  const total = comValor.reduce((soma, d) => soma + d.quantidade, 0)

  if (total === 0) return <GraficoVazio>Sem dados no período.</GraficoVazio>

  return (
    <div className="flex flex-col gap-3">
      {/* `gap-0.5` é o vão de superfície entre os segmentos — separação por
          espaço, não por borda em volta de cada um. */}
      <div className="flex h-8 gap-0.5 overflow-hidden rounded-md">
        {comValor.map((d, i) => (
          <div
            key={d.rotulo}
            className="grid place-items-center"
            style={{
              width: `${(d.quantidade / total) * 100}%`,
              background: RAMPA_ORDINAL[i % RAMPA_ORDINAL.length],
            }}
            title={`${d.rotulo}: ${d.quantidade}`}
          >
            {/* Rótulo dentro só quando a fatia comporta — senão ele é cortado,
                que é pior do que não estar. A lista abaixo carrega todos. */}
            {d.quantidade / total > 0.15 && (
              <span className="numeric text-caption font-medium text-txt-inverse">
                {formatarNumero((d.quantidade / total) * 100, 0)} %
              </span>
            )}
          </div>
        ))}
      </div>

      <ul className="flex flex-col divide-y divide-line">
        {comValor.map((d, i) => (
          <li key={d.rotulo} className="flex items-center gap-2 py-1.5">
            <span
              aria-hidden="true"
              className="size-2.5 shrink-0 rounded-full"
              style={{ background: RAMPA_ORDINAL[i % RAMPA_ORDINAL.length] }}
            />
            <span className="truncate text-caption text-txt-secondary">{d.rotulo}</span>
            <span className="numeric ml-auto shrink-0 text-caption text-txt">
              {d.quantidade}
              <span className="ml-1.5 text-txt-muted">
                {formatarNumero((d.quantidade / total) * 100, 1, 1)} %
              </span>
            </span>
          </li>
        ))}
      </ul>
      <span className="sr-only">
        {rotuloValor}: {total}
      </span>
    </div>
  )
}

/**
 * A adesão média mês a mês, em linha, com o limiar dos 100 % tracejado.
 *
 * **Tracejado significa limiar, nunca grade** — é a única coisa tracejada nos
 * gráficos deste sistema. E o desvio continua sem cor: o mesmo 60 % é conduta
 * na primeira semana e alerta na terceira, então quem julga é quem prescreve.
 *
 * Mês sem dia com prescrição vem sem valor e a linha **salta** — `connectNulls`
 * está desligado de propósito. Emendar o traço por cima do buraco inventaria
 * uma adesão que ninguém mediu.
 */
export function AdesaoPorPeriodo({ dados }: { dados: PontoPeriodoUti[] }) {
  const comAdesao = dados.filter((d) => d.adesaoMedia != null)

  if (comAdesao.length === 0)
    return (
      <GraficoVazio>
        Nenhum mês com dia de acompanhamento e volume prescrito no período.
      </GraficoVazio>
    )

  return (
    <>
      {/* Um ponto sozinho parece defeito, e não é: só um mês teve dia medido.
          Dizer isso custa uma linha e evita que alguém saia procurando bug
          num gráfico correto — foi a primeira pergunta de quem olhou. */}
      {comAdesao.length === 1 && (
        <p className="mb-2 text-caption text-txt-muted">
          Só <b>{comAdesao[0].periodo}</b> tem dia de acompanhamento medido no período — daí o
          ponto único. A linha aparece a partir do segundo mês.
        </p>
      )}
      <ResponsiveContainer width="100%" height={240}>
      <ComposedChart data={dados} margin={{ top: 8, right: 16, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="periodo" {...EIXO} />
        <YAxis {...EIXO} width={48} tickFormatter={(v: number) => `${formatarNumero(v, 0)}%`} />

        <ReferenceLine
          y={100}
          stroke="var(--txt-muted)"
          strokeDasharray="4 4"
          label={{
            value: 'prescrito',
            position: 'right',
            fill: 'var(--txt-muted)',
            fontSize: 11,
          }}
        />

        <Line
          type="monotone"
          dataKey="adesaoMedia"
          name="Adesão média"
          stroke="var(--viz-serie-1)"
          strokeWidth={2}
          isAnimationActive={false}
          dot={{ r: 4, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
        />

        <Tooltip
          cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as PontoPeriodoUti
            return (
              <TooltipCartao
                titulo={p.periodo}
                linhas={[
                  {
                    rotulo: 'Adesão média',
                    cor: 'var(--viz-serie-1)',
                    valor: p.adesaoMedia != null ? `${formatarNumero(p.adesaoMedia, 1)}%` : '—',
                  },
                  { rotulo: 'Dias medidos', valor: p.dias },
                ]}
              />
            )
          }}
        />
        </ComposedChart>
      </ResponsiveContainer>
    </>
  )
}

/**
 * Composição numa barra empilhada horizontal — a forma que o `docs/05 §10.5`
 * pede para classificação clínica.
 *
 * Horizontal porque os rótulos são longos ("Obesidade grau III"), e empilhada
 * porque a pergunta é **parte contra o todo**: quanto da casuística está em
 * cada faixa. A cor vem do tom gravado, não da posição.
 */
export function ComposicaoPorTom({ dados }: { dados: ContagemRotuladaUti[] }) {
  const comValor = dados.filter((d) => d.quantidade > 0)
  const total = comValor.reduce((soma, d) => soma + d.quantidade, 0)

  if (total === 0) return <GraficoVazio>Nenhuma avaliação classificada no período.</GraficoVazio>

  return (
    <div className="flex flex-col gap-3">
      <div className="flex h-8 gap-0.5 overflow-hidden rounded-md">
        {comValor.map((d) => (
          <div
            key={d.rotulo}
            className="grid place-items-center"
            style={{
              width: `${(d.quantidade / total) * 100}%`,
              background: COR_TOM[d.tom ?? 'NEUTRO'],
            }}
            title={`${d.rotulo}: ${d.quantidade}`}
          >
            {d.quantidade / total > 0.12 && (
              <span className="numeric text-caption font-medium text-txt-inverse">
                {d.quantidade}
              </span>
            )}
          </div>
        ))}
      </div>

      <ul className="flex flex-col divide-y divide-line">
        {comValor.map((d) => (
          <li key={d.rotulo} className="flex items-center gap-2 py-1.5">
            <span
              aria-hidden="true"
              className="size-2.5 shrink-0 rounded-full"
              style={{ background: COR_TOM[d.tom ?? 'NEUTRO'] }}
            />
            <span className="truncate text-caption text-txt-secondary">{d.rotulo}</span>
            <span className="numeric ml-auto shrink-0 text-caption text-txt">
              {d.quantidade}
              <span className="ml-1.5 text-txt-muted">
                {formatarNumero((d.quantidade / total) * 100, 1, 1)} %
              </span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * Avaliações por mês, em área — o mesmo tratamento de `AvaliacoesPorPeriodo` na
 * pediatria, e de propósito: os dois painéis respondem à mesma pergunta.
 *
 * <b>Uma série só.</b> A tentação era pôr os dias de acompanhamento junto, já
 * que ambos são contagem — mas 3 avaliações contra 18 dias no mesmo eixo achata
 * a barra até sumir. Duas contagens de magnitude muito diferente são o erro do
 * eixo duplo com outra roupa. Os dias já têm cartão de indicador no topo.
 *
 * O ponto fica **visível** (`dot`), e não some como numa série longa: com um
 * mês só de dados, uma linha sem marcador não desenha nada.
 */
export function AvaliacoesPorPeriodo({ dados }: { dados: PontoPeriodoUti[] }) {
  if (dados.length === 0) return <GraficoVazio>Sem avaliações no período.</GraficoVazio>

  return (
    <ResponsiveContainer width="100%" height={260}>
      <AreaChart data={dados} margin={{ top: 8, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid {...GRADE} />
        <XAxis dataKey="periodo" {...EIXO} />
        <YAxis {...EIXO} width={40} allowDecimals={false} />
        <Area
          type="monotone"
          dataKey="avaliacoes"
          name="Avaliações"
          stroke="var(--viz-serie-1)"
          strokeWidth={2}
          fill="var(--viz-faixa-externa)"
          isAnimationActive={false}
          dot={{ r: 3, fill: 'var(--viz-serie-1)', stroke: 'var(--surface)', strokeWidth: 2 }}
        />
        <Tooltip
          cursor={{ stroke: 'var(--line-strong)', strokeWidth: 1 }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as PontoPeriodoUti
            return (
              <TooltipCartao
                titulo={p.periodo}
                linhas={[
                  { rotulo: 'Avaliações', cor: 'var(--viz-serie-1)', valor: p.avaliacoes },
                  { rotulo: 'Dias registrados', valor: p.dias },
                ]}
              />
            )
          }}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}

/**
 * As três classificações clínicas **num quadro só**, uma linha cada.
 *
 * É a forma de `DistribuicaoClassificacoes` na pediatria, e ela resolve dois
 * problemas de uma vez: dá a comparação entre os índices — a mesma casuística
 * lida por três réguas — e recupera a largura inteira da página, em vez de três
 * cartões estreitos e altos, cada um com duas barrinhas.
 *
 * A cor vem do **tom gravado** na avaliação, nunca da posição da fatia.
 */
export function ClassificacoesEmpilhadas({
  imc,
  circBraco,
  perdaPeso,
}: {
  imc: ContagemRotuladaUti[]
  circBraco: ContagemRotuladaUti[]
  perdaPeso: ContagemRotuladaUti[]
}) {
  const indices = [
    { indice: 'IMC (OMS)', faixas: imc },
    { indice: 'Circ. do braço', faixas: circBraco },
    { indice: 'Perda de peso', faixas: perdaPeso },
  ]

  const total = indices.reduce((s, l) => s + l.faixas.reduce((a, f) => a + f.quantidade, 0), 0)
  if (total === 0) return <GraficoVazio>Nenhuma avaliação classificada no período.</GraficoVazio>

  // Uma série por TOM, não por rótulo: os rótulos diferem entre os três índices
  // ("Eutrofia" × "Adequado"), mas o tom é a régua comum que permite empilhar.
  const TONS: TomResultado[] = ['ADEQUADO', 'ATENCAO', 'CRITICO', 'NEUTRO']
  const NOME_DO_TOM: Record<TomResultado, string> = {
    ADEQUADO: 'Adequado',
    ATENCAO: 'Atenção',
    CRITICO: 'Crítico',
    NEUTRO: 'Não classificado',
  }

  const dados = indices.map((l) => {
    const linha: Record<string, string | number> = { indice: l.indice }
    for (const tom of TONS) {
      linha[tom] = l.faixas
        .filter((f) => (f.tom ?? 'NEUTRO') === tom)
        .reduce((a, f) => a + f.quantidade, 0)
      // Os rótulos reais daquele índice, para o tooltip não dizer só "Atenção".
      linha[`${tom}_rotulos`] = l.faixas
        .filter((f) => (f.tom ?? 'NEUTRO') === tom && f.quantidade > 0)
        .map((f) => f.rotulo)
        .join(', ')
    }
    return linha
  })

  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 12, left: 8, bottom: 4 }}>
        <CartesianGrid {...GRADE} horizontal={false} vertical />
        <XAxis type="number" {...EIXO} allowDecimals={false} />
        <YAxis type="category" dataKey="indice" {...EIXO} width={120} />

        {TONS.map((tom) => (
          <Bar
            key={tom}
            dataKey={tom}
            name={NOME_DO_TOM[tom]}
            stackId="t"
            fill={COR_TOM[tom]}
            /* 2 px da superfície entre segmentos, no lugar de contorno */
            stroke="var(--surface)"
            strokeWidth={2}
            isAnimationActive={false}
            maxBarSize={26}
          >
            {/* Número dentro do segmento só quando cabe — espremido e cortado
                é pior que ausente, e o tooltip carrega todos. */}
            <LabelList
              dataKey={tom}
              position="center"
              fill="var(--txt-inverse)"
              fontSize={11}
              formatter={(v: unknown) => (Number(v) > 0 ? String(v) : '')}
            />
          </Bar>
        ))}

        <Tooltip
          cursor={{ fill: 'var(--surface-alt)' }}
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null
            const p = payload[0].payload as Record<string, string | number>
            return (
              <TooltipCartao
                titulo={String(p.indice)}
                linhas={TONS.filter((t) => Number(p[t]) > 0).map((t) => ({
                  rotulo: String(p[`${t}_rotulos`]) || NOME_DO_TOM[t],
                  cor: COR_TOM[t],
                  valor: Number(p[t]),
                }))}
              />
            )
          }}
        />
        <Legend
          verticalAlign="top"
          align="right"
          wrapperStyle={{ fontSize: 12, color: 'var(--txt-secondary)', paddingBottom: 8 }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * Uma linha de conduta: rótulo, proporção e números.
 *
 * <b>Quando há uma categoria só, não desenha barra.</b> Uma barra de 100 % não
 * informa nada que a frase "todas as 3 avaliações" não diga melhor — é o
 * anti-padrão do gráfico de uma fatia. Três painéis dizendo "100 %" foi
 * exatamente o que apareceu ao olhar a tela pela primeira vez.
 */
export function LinhaDeConduta({
  titulo,
  dados,
  nota,
}: {
  titulo: string
  dados: ContagemUti[]
  nota?: string
}) {
  const comValor = dados.filter((d) => d.quantidade > 0)
  const total = comValor.reduce((soma, d) => soma + d.quantidade, 0)

  return (
    <div className="flex flex-col gap-1.5 py-3">
      <div className="flex items-baseline justify-between gap-3">
        <span className="text-caption font-medium text-txt">{titulo}</span>
        {total > 0 && (
          <span className="text-caption text-txt-muted">
            {total} {total === 1 ? 'avaliação' : 'avaliações'}
          </span>
        )}
      </div>

      {total === 0 ? (
        <span className="text-caption text-txt-muted">Sem dados no período.</span>
      ) : comValor.length === 1 ? (
        <span className="text-body text-txt">
          {comValor[0].rotulo}
          <span className="ml-2 text-caption text-txt-muted">
            em {total === 1 ? 'toda a casuística' : 'todas as avaliações do período'}
          </span>
        </span>
      ) : (
        <>
          <div className="flex h-6 gap-0.5 overflow-hidden rounded">
            {comValor.map((d, i) => (
              <div
                key={d.rotulo}
                style={{
                  width: `${(d.quantidade / total) * 100}%`,
                  background: RAMPA_ORDINAL[i % RAMPA_ORDINAL.length],
                }}
                title={`${d.rotulo}: ${d.quantidade}`}
              />
            ))}
          </div>
          <div className="flex flex-wrap gap-x-4 gap-y-1">
            {comValor.map((d, i) => (
              <span key={d.rotulo} className="flex items-center gap-1.5 text-caption">
                <span
                  aria-hidden="true"
                  className="size-2 shrink-0 rounded-full"
                  style={{ background: RAMPA_ORDINAL[i % RAMPA_ORDINAL.length] }}
                />
                <span className="text-txt-secondary">{d.rotulo}</span>
                <span className="numeric text-txt-muted">
                  {formatarNumero((d.quantidade / total) * 100, 0)} %
                </span>
              </span>
            ))}
          </div>
        </>
      )}

      {nota && <span className="text-caption text-txt-muted">{nota}</span>}
    </div>
  )
}
