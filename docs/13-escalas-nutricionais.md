# Escalas nutricionais pontuadas — MNA® e NRS-2002

> **Especificação numérica.** Este documento vence a transcrição, e a transcrição
> vence o seed. Nenhum ponto, faixa ou ponto de corte pode ser alterado no
> `031-escalas-nutricionais.xml` sem ser alterado aqui primeiro — é a mesma regra
> de [docs/09](09-calculos-pediatria.md) e [docs/10](10-calculos-uti-adulto.md).

---

## 1. Por que esta fatia existe

A fatia 12 semeou três modelos de ficha e **recusou explicitamente** semear
qualquer instrumento pontuado. A migration 030 diz, em comentário:

> *"NENHUM INSTRUMENTO PONTUADO ENTRA NO SEED. NRS-2002, MUST, MNA-SF e
> STRONGkids são escalas com pontos de corte e validação publicada — o mesmo tipo
> de conhecimento que docs/09 e docs/10 exigiram documentar antes de virar código.
> Um questionário descritivo não tem esse problema: ele registra o que foi dito,
> não calcula um escore."*

E o [docs/12 §5.1](12-fichas-de-anamnese.md) deixou a condição escrita: *"Se a
cliente pedir, entram como fatia própria, com fonte citada."* A cliente pediu as
duas, em 07/09/2026. Este documento é a fonte citada.

### 1.1 O que o sistema ganha, e não é a soma

Somar dezoito números não é trabalho que justifique uma fatia. O que justifica é
a recusa que o papel não sabe fazer:

**Numa escala, soma parcial não é escore menor — é escore errado.**

Uma MNA com cinco perguntas em branco soma 12 e parece *sob risco de desnutrição*
quando o paciente pode ser *normal*. A folha de papel não tem como se recusar a
ser somada pela metade; o sistema tem, e é a mesma recusa que ele já pratica
quando não mostra VET sem peso e escreve o porquê ao lado. Por isso **todo escore
deste documento é nulo-com-motivo enquanto o seu grupo estiver incompleto**, e o
motivo nomeia as perguntas que faltam.

### 1.2 Onde mora cada coisa

| | Onde | Por quê |
|---|---|---|
| Pontos de cada opção, grupo de cada pergunta | **Banco** — `campo_ficha.pontos`, `campo_ficha.grupo_escore` | São dado. E como dado eles entram no **retrato** da resposta, que é o que congela o escore de uma ficha salva |
| Porta, faixas, ponto de corte, ajuste por idade, frase da conclusão, citação | **Java** — `clinica/escore/` | Conhecimento clínico com publicação atrás. Vira classe testada com fonte citada, nunca configuração num JSON que ninguém revisa |

A classe Java opera sobre **subtotal de grupo**. Ela nunca procura uma pergunta
pelo rótulo — se procurasse, clonar o modelo e reescrever um enunciado quebraria o
casamento em silêncio.

---

## 2. MNA® — Mini Nutritional Assessment

### 2.1 Fonte

- Vellas B, Villars H, Abellan G, et al. **Overview of the MNA® — Its History and
  Challenges.** *J Nutr Health Aging* 2006;10:456-465.
- Rubenstein LZ, Harker JO, Salva A, Guigoz Y, Vellas B. **Screening for
  Undernutrition in Geriatric Practice: Developing the Short-Form Mini Nutritional
  Assessment (MNA-SF).** *J Gerontol* 2001;56A:M366-377.
- Guigoz Y. **The Mini-Nutritional Assessment (MNA®) Review of the Literature —
  What does it tell us?** *J Nutr Health Aging* 2006;10:466-487.

**Marca e direito autoral.** MNA® é marca registrada da Société des Produits
Nestlé SA. © Société des Produits Nestlé SA, 1994, revisão 2009. O instrumento é
de uso clínico livre **mediante atribuição**, distribuído em
`www.mna-elderly.com`. Como o sistema **redistribui** o instrumento a todos os
clientes, o crédito é obrigatório e aparece em dois lugares: na `descricao` do
modelo semeado e no rodapé da folha impressa. A versão transcrita é a **portuguesa
do Brasil**, revisão 2009.

### 2.2 Triagem — grupo `TRIAGEM`, máximo 14

| # | Pergunta | Opções → pontos |
|---|---|---|
| A | Nos últimos três meses houve diminuição da ingesta alimentar devido a perda de apetite, problemas digestivos ou dificuldade para mastigar ou deglutir? | Diminuição severa **0** · moderada **1** · sem diminuição **2** |
| B | Perda de peso nos últimos três meses | Superior a três quilos **0** · não sabe informar **1** · entre um e três quilos **2** · sem perda de peso **3** |
| C | Mobilidade | Restrito ao leito ou à cadeira de rodas **0** · deambula mas não sai de casa **1** · normal **2** |
| D | Passou por algum estresse psicológico ou doença aguda nos últimos três meses? | Sim **0** · Não **2** |
| E | Problemas neuropsicológicos | Demência ou depressão graves **0** · demência leve **1** · sem problemas psicológicos **2** |
| F | Índice de massa corporal (peso em kg / estatura em m²) | IMC < 19 **0** · 19 ≤ IMC < 21 **1** · 21 ≤ IMC < 23 **2** · IMC ≥ 23 **3** |

Máximo: 2 + 3 + 2 + 2 + 2 + 3 = **14**.

**Faixas da triagem** — ela classifica sozinha, e é o instrumento inteiro na versão
abreviada (MNA-SF):

| Escore | Classificação | Tom |
|---|---|---|
| 12 a 14 | Estado nutricional normal | `ADEQUADO` |
| 8 a 11 | Sob risco de desnutrição | `ATENCAO` |
| 0 a 7 | Desnutrido | `CRITICO` |

**A porta.** A folha instrui: *"Se a pontuação obtida for igual ou menor que 11,
continue o preenchimento do questionário."* É orientação de **economia de tempo**,
não de validade — nada impede aplicar a avaliação global com triagem 12. Por isso
o sistema **não bloqueia** as perguntas G a R quando a triagem passa de 11; ele
apenas registra a conclusão da triagem, que já vale por si.

### 2.3 Avaliação global — grupo `GLOBAL`, máximo 16

| # | Pergunta | Opções → pontos |
|---|---|---|
| G | O paciente vive em sua própria casa (não em casa geriátrica ou hospital)? | Sim **1** · Não **0** |
| H | Utiliza mais de três medicamentos diferentes por dia? | Sim **0** · Não **1** |
| I | Lesões de pele ou escaras? | Sim **0** · Não **1** |
| J | Quantas refeições faz por dia? | Uma **0** · duas **1** · três **2** |
| K | Consome pelo menos uma porção diária de leite ou derivados, duas ou mais porções semanais de leguminosas ou ovos, e carne, peixe ou aves todos os dias? | Nenhuma ou uma resposta sim **0** · duas **0,5** · três **1** |
| L | Consome duas ou mais porções diárias de fruta ou produtos hortícolas? | Não **0** · Sim **1** |
| M | Quantos copos de líquidos (água, suco, café, chá, leite) consome por dia? | Menos de três **0** · três a cinco **0,5** · mais de cinco **1** |
| N | Modo de se alimentar | Não é capaz sozinho **0** · sozinho com dificuldade **1** · sozinho sem dificuldade **2** |
| O | O paciente acredita ter algum problema nutricional? | Acredita estar desnutrido **0** · não sabe dizer **1** · acredita não ter **2** |
| P | Em comparação a outras pessoas da mesma idade, como considera a sua própria saúde? | Pior **0** · não sabe **0,5** · igual **1** · melhor **2** |
| Q | Perímetro braquial (PB) em cm | PB < 21 **0** · 21 ≤ PB ≤ 22 **0,5** · PB > 22 **1** |
| R | Perímetro da perna (PP) em cm | PP < 31 **0** · PP ≥ 31 **1** |

Máximo: 1 + 1 + 1 + 2 + 1 + 1 + 1 + 2 + 2 + 2 + 1 + 1 = **16**.

A avaliação global **não classifica sozinha** — não há faixa publicada para os 16
pontos isolados. Ela só existe para compor o total.

### 2.4 Escore total — máximo 30

| Escore | Classificação | Tom |
|---|---|---|
| 24 a 30 | Estado nutricional normal | `ADEQUADO` |
| 17 a 23,5 | Sob risco de desnutrição | `ATENCAO` |
| Menos de 17 | Desnutrido | `CRITICO` |

### 2.5 Transcrições que exigiram decisão

**O item K vira uma pergunta, não quatro.** No papel, K são três sim/não (leite ·
leguminosas ou ovos · carne, peixe ou aves) cuja *contagem* produz 0,0 · 0,5 · 1,0.
Transcrevê-lo como três `CHECKBOX` mais um item somado guardaria o mesmo fato em
dois lugares do prontuário — que é exatamente o que o [docs/12 §5](12-fichas-de-anamnese.md)
evita ao não pedir peso na anamnese do adulto. Vira **uma** pergunta de opção com
os três níveis, e o enunciado cita as três porções.

**F, Q e R pedem medidas que a avaliação de UTI coleta**, e mesmo assim entram.
Não é a duplicação proibida: aqui elas entram como **faixa pontuada**, não como
medida — o que é gravado é *"IMC de 21 a menos de 23"*, e nenhum número sai daqui
para lugar nenhum. A `AvaliacaoUti` continua sendo a única fonte de IMC do sistema.

**Meio ponto é real, e o corte da faixa intermediária é literalmente 17 a 23,5.**
A aritmética é `BigDecimal` e as colunas são `NUMERIC`, escala 1.

Vale ser preciso sobre o porquê, porque a razão fácil é falsa: 0,5 **é**
exatamente representável em binário, e somar meios em `double` não perde nada
hoje. O motivo é outro, e é de projeto:

- a coluna é `NUMERIC`, e `BigDecimal` é o tipo que a espelha sem conversão;
- **a próxima escala pode não ter essa sorte.** MUST e STRONGkids pontuam em
  inteiros, mas nada garante que o terceiro instrumento não use décimos — e
  `0.1 + 0.2 != 0.3` em `double`. Uma comparação de faixa erraria em silêncio,
  numa classificação clínica;
- escala fixa em uma casa faz `9` e `9,0` serem o mesmo número no JSON e na
  folha, sem o formatador precisar decidir.

Depender de "0,5 é exato" seria construir sobre uma propriedade que a próxima
migration de seed pode revogar sem que ninguém perceba.

### 2.6 Gabarito de teste — `mnaGabaritoCompleto`

| Item | Resposta | Pontos |
|---|---|---|
| A | Diminuição moderada da ingesta | 1 |
| B | Entre um e três quilos | 2 |
| C | Normal | 2 |
| D | Não | 2 |
| E | Demência leve | 1 |
| F | IMC de 19 a menos de 21 | 1 |
| | **Triagem** | **9,0 / 14 · Sob risco de desnutrição** |
| G | Sim | 1 |
| H | Sim | 0 |
| I | Não | 1 |
| J | Três refeições | 2 |
| K | Duas respostas sim | 0,5 |
| L | Sim | 1 |
| M | Três a cinco copos | 0,5 |
| N | Alimenta-se sozinho sem dificuldade | 2 |
| O | Não sabe dizer | 1 |
| P | Igual | 1 |
| Q | PB de 21 a 22 | 0,5 |
| R | PP de 31 ou mais | 1 |
| | **Avaliação global** | **11,5 / 16** |
| | **Total** | **20,5 / 30 · Sob risco de desnutrição** |

Gabarito auxiliar `mnaMeioPontoSoma`: K, M, P e Q respondidos nos seus níveis de
meio ponto somam **2,0** exatos — a asserção é sobre a igualdade, não sobre uma
tolerância, porque tolerância aceitaria o `double`.

---

## 3. NRS-2002 — Nutritional Risk Screening

### 3.1 Fonte

Kondrup J, Rasmussen HH, Hamberg O, Stanga Z; ad hoc ESPEN Working Group.
**Nutritional risk screening (NRS 2002): a new method based on an analysis of
controlled clinical trials.** *Clin Nutr* 2003;22(3):321-336.

Recomendado pela ESPEN para triagem de risco nutricional em pacientes
hospitalizados. Publicação científica, sem restrição de marca — a atribuição é a
que se deve a qualquer fonte, e não uma condição de licença como a da MNA®.

### 3.2 Etapa 1 — pré-triagem, grupo `PRE_TRIAGEM`, sem pontos

Quatro perguntas de sim/não:

1. IMC menor que 20,5 kg/m²?
2. Houve perda de peso nos últimos três meses?
3. Houve redução da ingestão alimentar na última semana?
4. O paciente apresenta doença grave, por exemplo necessidade de terapia intensiva?

**A porta.** Se **todas** forem "não", o instrumento termina aqui: não há escore, e
a conduta é *repetir a triagem semanalmente*. Se **qualquer uma** for "sim",
aplicam-se as etapas 2 e 3.

Isto é **resultado da escala, não falta de dado** — e a diferença importa na tela:
a porta fechada produz uma **conclusão**, não uma queixa de campo em branco.

**E a porta fechada desliga as etapas 2 e 3.** Elas voltam com
`naoSeAplica = true`, subtotal **nulo** e o motivo *"Não se aplica — a pré-triagem
não encontrou critério."* — mesmo que tenham sido respondidas.

Isso é consequência de quem preenche de baixo para cima: respondidas as etapas e
marcados depois os quatro "não", a tela publicava *"Estado nutricional 2 de 3"* e
*"Gravidade da doença 2 de 3"* ao lado de *"nenhum critério"* e de um total vazio.
Nenhum dos dois números estava errado; os dois estavam **fora de qualquer conta**,
esperando que quem confere os somasse. É a armadilha do denominador da adesão —
número exibido sem a regra que o governa.

As respostas **não** são apagadas: continuam na ficha, na folha e no retrato,
porque são o que a profissional registrou. O que some é o ponto. E o formulário
**desabilita** as duas seções em vez de escondê-las: esconder tiraria da vista
respostas que continuam sendo gravadas, e dado invisível em prontuário é pior que
uma seção a mais na tela.

### 3.3 Etapa 2 — estado nutricional, grupo `ESTADO_NUTRICIONAL`, máximo 3

| Pontos | Situação |
|---|---|
| **0** | Estado nutricional normal |
| **1** | Perda de peso acima de 5% em três meses, **ou** ingestão de 50 a 75% das necessidades na última semana |
| **2** | Perda acima de 5% em dois meses, **ou** IMC de 18,5 a 20,5 com pior condição geral, **ou** ingestão de 25 a 60% |
| **3** | Perda acima de 5% em um mês ou acima de 15% em três meses, **ou** IMC abaixo de 18,5 com pior condição geral, **ou** ingestão de 0 a 25% |

### 3.4 Etapa 3 — gravidade da doença, grupo `GRAVIDADE_DOENCA`, máximo 3

| Pontos | Exemplos |
|---|---|
| **0** | Necessidades nutricionais habituais |
| **1** | Doença crônica com complicação aguda: DPOC, cirrose, diabetes, câncer, hemodiálise |
| **2** | Cirurgia abdominal de grande porte, AVC, pneumonia grave, neoplasias hematológicas |
| **3** | Paciente crítico, trauma craniano, transplante de medula, UTI com APACHE acima de 10 |

### 3.5 Ajuste por idade — +1 se ≥ 70 anos

Escore total = etapa 2 + etapa 3 + **1 ponto se o paciente tem 70 anos ou mais**.
Máximo **7**.

**A idade é calculada, não perguntada.** Vem de `pessoa.dataNascimento` na
`ficha_anamnese.dataPreenchimento`, com anos completos. O `docs/12` já recusa pedir
na anamnese o que o sistema tem no cadastro, e aqui o argumento é mais forte: um
campo respondido à mão pode dizer "não" para um paciente de 81 anos, e nada
denunciaria.

**Sem data de nascimento no cadastro, o escore não sai.** O ponto por idade não
pode ser decidido, e um total um ponto menor, entregue calado, cairia exatamente
na faixa que decide iniciar terapia nutricional. A tela recebe o motivo escrito.

**O ajuste é publicado separado do total** (`ajusteIdade` e
`ajusteIdadeDescricao` no `EscoreDto`), para que a conta possa ser refeita à mão
no papel: `2 + 2 + 1 = 5`. É a lição da armadilha do denominador da adesão com o
sinal invertido — quando a regra soma ou descarta um valor, o servidor publica o
valor, senão cada tela reimplementa a decisão e a enésima erra.

### 3.6 Ponto de corte

| Escore | Classificação | Tom | Conduta |
|---|---|---|---|
| **≥ 3** | Em risco nutricional | `ATENCAO` | Iniciar plano de terapia nutricional |
| **< 3** | Sem risco nutricional no momento | `ADEQUADO` | Reavaliar semanalmente |

### 3.7 Ambiguidades da fonte recebida

A cliente enviou as três etapas por escrito, e **duas coisas da publicação original
não estavam na mensagem**. As duas foram incluídas, por decisão registrada em
07/09/2026:

| O que faltava | Por que entrou mesmo assim |
|---|---|
| **O +1 por idade ≥ 70 anos** | É parte do escore publicado. Sem ele, todo paciente de 70 anos ou mais pontua um a menos — e como o corte é 3, um idoso com etapa 2 = 2 e etapa 3 = 0 sairia com 2 (*reavaliar semanalmente*) onde o instrumento diz 3 (*em risco, iniciar terapia*). É a diferença que decide a conduta |
| **O ponto de corte ≥ 3** | Sem ele o escore é um número sem leitura. A mensagem descrevia como pontuar e não o que fazer com a pontuação |

Se a instituição usar por protocolo uma variante sem o ajuste de idade, isso é
mudança **nesta especificação** e na `Nrs2002Escala`, com registro aqui — nunca um
ajuste calado no seed.

### 3.8 Gabarito de teste — `nrsGabaritoCompleto`

| Etapa | Resposta | Pontos |
|---|---|---|
| Pré-triagem | "Sim" em ao menos uma | porta aberta |
| Estado nutricional | Moderado — perda acima de 5% em dois meses… | 2 |
| Gravidade da doença | Moderada — cirurgia abdominal de grande porte… | 2 |
| Idade | 81 anos em 07/09/2026 | +1 |
| | **Total** | **5 / 7 · Em risco nutricional** |

Gabaritos de limite, `nrsIdade69NaoPontua` e `nrsIdade70Pontua` — a data de
preenchimento é 07/09/2026:

| Nascimento | Idade | Ajuste |
|---|---|---|
| 08/09/1956 | 69 | **0** |
| 07/09/1956 | 70 | **+1** |

O primeiro é o caso que uma subtração de anos feita sem olhar o dia erraria: em
07/09/2026 quem nasceu em 08/09/1956 ainda **não** fez 70.

---

## 4. Motivos de ausência

Regra do `CLAUDE.md`: *todo caminho que não produz valor tem de produzir motivo*, e
*ramo `else if` sem `else` é traço mudo esperando acontecer*. Cada linha desta
tabela é um `else` explícito na `EscalaNutricional` correspondente:

| Situação | O que o servidor publica |
|---|---|
| Grupo com pergunta sem resposta | `subtotal` nulo · *"Faltam responder: C. Mobilidade, F. Índice de massa corporal."* — **nomeando as perguntas**, porque motivo é do tamanho da coisa que faltou |
| MNA · triagem completa, global incompleta | Triagem **classifica normalmente**; `total` nulo · *"O escore total exige as perguntas G a R."* |
| NRS · pré-triagem toda "não" | `total` nulo, `conclusao` preenchida · *"Nenhum critério da pré-triagem — repetir a triagem semanalmente."* Conclusão, não queixa |
| NRS · pré-triagem incompleta | `total` nulo · *"Responda as quatro perguntas da pré-triagem."* |
| NRS · paciente sem data de nascimento | `total` nulo · *"Paciente sem data de nascimento: o ponto por idade (70 anos ou mais) não pode ser decidido."* |

---

## 5. O escore congelado

`resposta_ficha.pontos` e `resposta_ficha.grupo_escore` são **retrato**, como
`rotulo` e `tipo` já eram: editar os pontos de um modelo não muda o escore de
nenhuma ficha já gravada.

Mas o retrato congela as *entradas*, e duas coisas ainda moveriam o escore de uma
ficha salva:

- **`pessoa.dataNascimento` é editável fora da ficha.** Corrigir a data de
  nascimento de um paciente mudaria, calado, o escore NRS de toda ficha antiga
  dele — o ponto por idade entraria ou sairia.
- **Uma faixa corrigida em Java** reclassificaria prontuário retroativamente.

Por isso `ficha_anamnese.escore_json` guarda o `EscoreDto` inteiro no momento da
gravação, e é ele que a ficha salva devolve. A leitura é tolerante
(`@JsonIgnoreProperties(ignoreUnknown = true)`), pela mesma razão que
`OpcoesJson.paraLista` devolve lista vazia em vez de exceção: uma ficha inteira não
pode ficar impossível de abrir por causa de um campo que o DTO ganhou depois.

As colunas planas `escore_total`, `escore_classificacao` e `escore_tom` existem
porque JSON não se ordena nem se filtra na *native query* da listagem.

---

## 6. Cobertura de teste

| Área | Teste | O que trava |
|---|---|---|
| Aritmética | `mnaGabaritoCompleto` · `nrsGabaritoCompleto` | Os gabaritos de §2.6 e §3.8 |
| Precisão | `mnaMeioPontoSoma` | 0,5 × 4 = 2,0 exatos, com uma casa — trava a escala fixa, não o `double` (ver §2.5) |
| Faixas | `mnaTriagemClassificaSozinha` | Triagem completa e global vazia: triagem classifica, total nulo com motivo |
| Porta | `nrsPortaFechada` | Quatro "não" → conclusão, não queixa de dado faltando |
| Idade | `nrsIdade69NaoPontua` · `nrsIdade70Pontua` | O limite exato, no dia |
| Idade | `nrsSemDataNascimentoNaoConclui` | Total nulo com motivo nomeando a data de nascimento |
| Motivo | `grupoIncompletoNomeiaAsPerguntas` | O motivo contém os rótulos, não só "incompleto" |
| Faixas | `blocoAusenteNaoSomaSozinho` | Bloco que o modelo não tem **não** conta como completo. Foi um defeito real: só a triagem no modelo, e o total saía 9,0 lido pelas faixas de 30 — "Desnutrido" para quem respondeu tudo |
| Retrato | `escoreCongeladoNaFichaSalva` | **Verificado ao contrário**: com o mapper recalculando do catálogo, uma ficha de 20,5 reabre com 19,5. A inversão é feita na pergunta B, e não na A: o gabarito responde a opção do meio de A, e inverter `[0,1,2]` não a moveria — teste que passa com e sem o defeito não trava nada |
| Retrato | `escoreNaoMudaComDataNascimentoCorrigida` | A razão de `escore_json` existir |
| Estado | `endpointEscoreNaoRecusaIncompleto` | Formulário pela metade → 200 com motivo, nunca 400 |
| Guarda | `multiplasOpcoesPontuadoRecusado` · `pontosDeTamanhoDiferenteRecusado` | O CHECK do banco e a frase do service |
| Clone | `clonarPreservaEscore` | A cópia mantém escala, pontos e grupos |
| Seed | `seedCoerenteEscore` | Cardinalidade `pontos` × `opcoes`, e o máximo de cada escala (14+16=30; 3+3+1=7) |
