# Cálculos da UTI adulto

> Fatia 6 — **especificação numérica**. Este documento é a fonte de verdade das
> fórmulas do módulo `uti`. Nenhum número aqui foi inferido: tudo vem da planilha
> `Facilita Nutri na UTI - com SA_atualiza (1).xlsx`, com a célula de origem
> citada — ou, quando a planilha é omissa, de literatura primária com a citação
> completa.
>
> Leia junto com [03-arquitetura-backend.md](03-arquitetura-backend.md) §8
> (fórmulas em classe pura), [04-arquitetura-frontend.md](04-arquitetura-frontend.md) §7
> (telas de cálculo em abas) e [09-calculos-pediatria.md](09-calculos-pediatria.md)
> (o mesmo formato, na pediatria).

---

## 1. A fonte

Há **duas planilhas** de nome quase igual na raiz do repositório. A canônica é a
maior:

| | `... com SA_atualiza (1).xlsx` ✅ | `... com SA_atualiza.xlsx` |
|---|---|---|
| Tamanho | 1.175.297 bytes | 342.740 bytes |
| Modificada | **2026-03-24** | 2026-03-23 |
| Salva por | Microsoft Excel (Mac) | LibreOffice 25.8 |
| Autoria | `marcelo kamp` · `s1876` | `marcelo kamp` |
| Abas | 15 | 15 |
| Conteúdo | **superconjunto** | subconjunto |

A menor **não é subconjunto posicional**: várias áreas foram movidas entre as
versões. Usar a canônica, e apenas ela.

15 abas, **1.139 células de fórmula** que colapsam em **~42 funções distintas** —
o resto é a mesma fórmula replicada por linha. Nenhuma aba tem proteção nem
`dataValidation`: **todos os enums do domínio saem das tabelas de referência
transcritas abaixo**, não de metadados do arquivo.

### O que existe fora das células

Duas imagens da aba `Estimativas Antropométricas` carregam conteúdo clínico que
**não está em célula nenhuma** — e que o eroERP, por isso, nunca implementou:

| Âncora | Arquivo | Conteúdo |
|---|---|---|
| `D33` | `xl/media/image3.jpeg` | ajuste da **circunferência da panturrilha** pelo IMC |
| `J33` | `xl/media/image4.png` | ajuste da **circunferência do braço** pelo IMC, + o nível "muito baixo" |

Ambos foram extraídos, lidos e rastreados até a literatura primária. Estão nas
§2.7 e §2.8, e são a diferença clínica mais importante deste documento.

---

## 2. Antropometria

Entradas da aba, com o exemplo em cache: `B2` CB 25 cm · `B3` AJ 53 cm ·
`B4` idade 59 anos · `B5` CP 34 cm · `B6` CC 98 cm · `B7` CA 90 cm ·
`E2` altura 1,68 m.

### 2.1 Altura estimada — Chumlea 1985

Para quem não pode ser medido de pé (acamado, UTI). A partir da **altura do
joelho**.

```
Homem : altura_cm = 64,19 − (0,04 × idade) + (2,02 × AJ)
Mulher: altura_cm = 84,88 − (0,24 × idade) + (1,83 × AJ)
```

*Origem: `Estimativas!B9` / `B10`, referência `C8` "(Chumlea, 85)".*
Confere: AJ 53, idade 59 → **168,89** (H) · **167,71** (M).

### 2.2 Peso estimado — Chumlea 1988

Oito ramos, por **sexo × etnia × faixa de idade**, a partir de AJ e CB:

| Ramo | Equação |
|---|---|
| Homem branco 19–59 | `(AJ × 1,19) + (CB × 3,14) − 86,82` |
| Homem branco ≥60 | `(AJ × 1,10) + (CB × 3,07) − 75,81` |
| Homem negro 19–59 | `(AJ × 1,09) + (CB × 3,14) − 83,72` |
| Homem negro ≥60 | `(AJ × 0,44) + (CB × 2,86) − 39,21` |
| Mulher branca 19–59 | `(AJ × 1,01) + (CB × 2,81) − 66,04` |
| Mulher branca ≥60 | `(AJ × 1,09) + (CB × 2,68) − 65,51` |
| Mulher negra 19–59 | `(AJ × 1,24) + (CB × 2,97) − 82,48` |
| Mulher negra ≥60 | `(AJ × 1,50) + (CB × 2,58) − 84,22` |

*Origem: `Estimativas!B13:B20`, referência `C12` "(Chumlea, 88)".*
Confere, na ordem: **54,75 · 59,24 · 52,55 · 55,61 · 57,74 · 59,26 · 57,49 · 59,78**.

> ⚠️ **Os 60 anos exatos não caem em faixa nenhuma.** Os rótulos da planilha são
> "19-59 ANOS" e ">60 ANOS". Adotamos **≥60 = idoso**, por coerência com o
> Estatuto do Idoso (Lei 10.741/2003). É **convenção nossa**, não leitura da
> fonte, e tem teste explícito em 59, 60 e 61.

> ⚠️ **A fonte primária exata deste conjunto de 8 ramos não foi identificada.** A
> literatura primária de Chumlea 1988 que se encontra usa **quatro** medidas
> (panturrilha, altura do joelho, braço e dobra subescapular); este conjunto usa
> **duas** e é o amplamente reproduzido na literatura brasileira de nutrição. O
> que torna estes números auditáveis é que os valores em cache da planilha
> conferem com as equações dela. **Pendência de documentação, não de cálculo.**

**Etnia é dado da avaliação, não do cadastro.** É o dicotômico de uma equação de
1988 (`BRANCA`/`NEGRA`), coletado por avaliação e usado em nada mais. Gravar
"raça" na tabela de pessoas seria erro de modelagem e ônus de LGPD sem
contrapartida — o eroERP faz isso (`avaliacao_nutricional_uti.raca VARCHAR(10)`,
valor livre).

### 2.3 Peso estimado — Jung 2004

```
Homem : (AJ × 0,928) + (CB × 2,508) − (idade × 0,144) − 42,543
Mulher: (AJ × 0,826) + (CB × 2,116) − (idade × 0,133) − 31,486
```

*Origem: `Estimativas!B24` / `B25`, referência `C23` "(Jung, 2004)".*
Confere: **60,845** (H) · **57,345** (M).

### 2.4 Peso estimado — Rabito 2008

```
peso = (0,5759 × CB) + (0,5263 × CA) + (1,2452 × CP) − (4,8689 × sexo) − 32,9241
       onde sexo: homem = 1, mulher = 2
```

*Origem: `Estimativas!B28` / `B29`, referência `C27` "(Rabito, 2008)".*
Confere: CB 25, CA 90, CP 34 → **66,3083** (H) · **61,4394** (M).

> 📌 **Correção em `docs/03 §12`.** O exemplo de teste de Rabito lá afirma
> `rabito(28; 95; 33; MASCULINO) == 66.05`, e a equação com essas entradas dá
> `69,4223`. É o exemplo que se copia como gabarito. Substituir pelo par
> verificado acima: `rabito(25; 90; 34; MASCULINO) == 66,3083`, célula
> `Estimativas!B28`.

### 2.5 Peso ideal, ajustado e corrigido

```
peso ideal        = IMC_alvo × altura_m²        alvo padrão: 22 (H) · 20,8 (M) · e 25
peso ajustado     = (peso_atual − peso_ideal) × 0,33 + peso_ideal
peso p/ amputação = peso − (peso × Σ%membros / 100)
```

*Origem: `E4`/`E5`/`E6` · `E9` · `G29`.*
Confere: altura 1,68 → **62,0928** (IMC 22) · **58,70592** (IMC 20,8) ·
**70,56** (IMC 25). Ajustado (85; 58,71) → **67,3857**. Amputação (65; 1,6 %) →
**63,96**.

**% do peso por segmento amputado — Osterkamp 1995** *(`D16:E24`)*

| Segmento | % | Segmento | % |
|---|---|---|---|
| Mão | 0,7 | Pé | 1,5 |
| Antebraço | 1,6 | Perna | 4,4 |
| Antebraço + mão | 2,3 | Membro inferior | 16,0 |
| Braço | 2,7 | | |
| Membro superior | 5,0 | | |

> ⚠️ **Os segmentos se contêm.** 0,7 + 1,6 = 2,3 e 2,7 + 2,3 = 5,0 na própria
> tabela. Selecionar "Membro superior" **e** "Mão" desconta a mão duas vezes. O
> cálculo deve **recusar** a combinação sobreposta, nomeando os dois segmentos —
> não devolver um peso silenciosamente errado.

### 2.6 IMC e classificação

```
IMC = peso_kg / altura_m²
```

*Origem: `Estimativas!B51`.* Confere: (62; 1,75) → **20,2449**.

**OMS 1997** *(`A43:B49`)* e **OPAS 2002** *(`C43:C47`)*, para idoso:

| Classificação | OMS 1997 | OPAS 2002 (idoso) |
|---|---|---|
| Desnutrição / baixo peso | < 18,5 | **< 23** |
| Eutrofia | 18,5 – 24,9 | **23 – 28** |
| Sobrepeso / excesso de peso | 25 – 29,9 | **28 – 30** |
| Obesidade grau I | 30 – 34,9 | **> 30** (obesidade) |
| Obesidade grau II | 35 – 39,9 | — |
| Obesidade grau III | ≥ 40 | — |

> ✅ **A OPAS 2002 tem quatro faixas, e a planilha está certa.** O eroERP
> implementou só dois cortes (`<23` · `≤28` · resto) e **perde a distinção entre
> excesso de peso e obesidade no idoso**. A classificação da OPAS, derivada do
> estudo SABE (sete países da América Latina e Caribe), é de quatro faixas
> exatamente como a planilha tabula. Implementar as quatro.
>
> Fontes: [Rev. Bras. Geriatr. Gerontol. — pontos de corte de IMC em idosos](https://www.scielo.br/j/rbgg/a/qXgVGH3dqzw6cK36qM4RBCD/?format=html&lang=pt)
> · [Pontos de corte do IMC para classificar o estado nutricional em idosos](https://www.redalyc.org/journal/4979/497950365004/html/)

> ⚠️ **As fronteiras da planilha excluem o valor exato.** Os rótulos são
> "Sobrepeso >25 <30" e "Obesidade Grau I >30 a 34,9": IMC 25,0 e 30,0 exatos não
> caem em faixa nenhuma. Implementamos **cada faixa fechada à esquerda**
> (`[18,5; 25)`, `[25; 30)`, `[30; 35)`, `[35; 40)`, `[40; ∞)`), com teste nas 9
> fronteiras.

### 2.7 Perda de peso — Blackburn 1977

```
% perda de peso = (peso_usual − peso_atual) × 100 / peso_usual
```

*Origem: `Estimativas!B32`.* Confere: (68 usual; 60 atual) → **11,7647 %**.

Classificação por **janela de tempo** *(`A36:C40`)*:

| Janela | Perda moderada | Perda grave |
|---|---|---|
| 1 semana | 1 – 2 % | ≥ 2 % |
| 1 mês | < 5 % | ≥ 5 % |
| 3 meses | < 7,5 % | ≥ 7,5 % |
| 6 meses | < 10 % | ≥ 10 % |

A janela é **escolha do usuário**, não constante. O eroERP a fixa em "1 mês"
apesar de implementar as quatro (`tabelas.ts:77-91`) — aqui é campo da avaliação,
e a referência do resultado diz qual janela e qual limite foram usados.

### 2.8 Adequação da circunferência do braço (CB)

```
% adequação = CB_medida / CB_P50 × 100
```

*Origem: `Estimativas!J25`.* Confere: (30; 32,3) → **92,8793 %** → *Eutrofia*.

**Percentil 50 de CB, por sexo e idade** *(`I12:K21`)* — o P50 é **buscado na
tabela**, não digitado. Na planilha ele é digitado em `J24`, o que é o defeito 19.

| Faixa de idade | P50 homens | P50 mulheres |
|---|---|---|
| 18 – 18,9 anos | 30,0 | 26,7 |
| 19 – 19,9 anos | 30,5 | 26,8 |
| 20 – 29,9 anos | 31,8 | 28,1 |
| 30 – 39,9 anos | 32,3 | 30,3 |
| 40 – 49,9 anos | 33,0 | 31,4 |
| 50 – 59,9 anos | 32,6 | 31,9 |
| 60 – 69,9 anos | 32,0 | 31,4 |
| 70 – 79,9 anos | 30,6 | 29,9 |
| 80 – 90,9 anos | 28,9 | 27,8 |

Fora de 18–90,9 anos: **sem adequação, com o motivo dito**. O eroERP extrapola em
silêncio usando a linha do extremo (`tabelas.ts:30-33`).

**Classificação da % de adequação** *(`N12:O17`)*:

| Faixa | Classificação |
|---|---|
| < 70 % | Desnutrição grave |
| 70 – 80 % | Desnutrição moderada |
| 80 – 90 % | Desnutrição leve |
| 90 – 110 % | Eutrofia |
| 110 – 120 % | Sobrepeso |
| > 120 % | Obesidade |

### 2.9 Depleção muscular — e o ajuste pelo IMC

Esta é a seção que **só existe nas imagens** da planilha. As células trazem
apenas os cortes; a figura mostra que a medida deve ser **ajustada pelo IMC antes
de ser comparada ao corte** — passo que o eroERP não faz, e que inverte o
resultado no paciente obeso.

#### Circunferência da panturrilha (CP)

**Passo 1 — ajustar a CP pelo IMC** *(imagem `image3.jpeg`, âncora `D33`)*:

| IMC | Adulto saudável | Idoso / população clínica com provável perda de peso ou massa |
|---|---|---|
| < 18,5 | **+ 4 cm** | usar a CP original |
| 18,5 – 24,9 | usar a CP original | usar a CP original |
| 25 – 29,9 | **− 3 cm** | **− 3 cm** |
| 30 – 39,9 | **− 7 cm** | **− 7 cm** |
| ≥ 40 | **− 12 cm** | **− 12 cm** |

**Passo 2 — comparar a CP ajustada ao corte por sexo** *(`I28:J30`)*:

| Sexo | Corte |
|---|---|
| Homem | < 34 cm → indicativo de depleção muscular |
| Mulher | < 33 cm → indicativo de depleção muscular |

*Cortes: `(Barbosa-Silva, 2016)`, célula `K28`.*
*Ajuste: Gonzalez MC, Mehrnezhad A, Razaviarab N, Barbosa-Silva TG, Heymsfield SB.
"Measuring calf circumference: a practical tool to predict skeletal muscle mass
via adjustment with BMI." **Am J Clin Nutr** 2021;113(6):1398-9. DOI
[10.1093/ajcn/nqab107](https://doi.org/10.1093/ajcn/nqab107).*

> **Por que isso importa.** Um paciente com IMC 32 e CP medida de 36 cm sai
> "adequado" no eroERP (36 > 34). Com o ajuste de −7 cm da sua faixa, a CP
> ajustada é 29 cm → **depleção muscular**. O ajuste existe justamente porque a
> gordura subcutânea infla a circunferência e o desempenho diagnóstico da CP cai
> em IMC ≥ 30.

#### Circunferência do braço (CB)

**Passo 1 — ajustar a CB pelo IMC** *(imagem `image4.png`, âncora `J33`)*. Aqui o
ajuste **depende do sexo**:

| IMC | Adulto saudável | Idoso / população clínica |
|---|---|---|
| < 18,5 | **+2 cm** (M) · **+3 cm** (H) | usar a CB original |
| 18,5 – 24,9 | usar a CB original | usar a CB original |
| 25 – 29,9 | **−2 cm** (M) · **−3 cm** (H) | **−2 cm** (M) · **−3 cm** (H) |
| 30 – 39,9 | **−6 cm** (M) · **−7 cm** (H) | **−6 cm** (M) · **−7 cm** (H) |
| ≥ 40 | **−9 cm** (M) · **−10 cm** (H) | **−9 cm** (M) · **−10 cm** (H) |

**Passo 2 — comparar a CB ajustada aos cortes**, que têm **dois níveis**:

| Sexo | Baixa | Muito baixa |
|---|---|---|
| Homem | < 28 cm | **< 26 cm** |
| Mulher | < 25 cm | **< 23 cm** |

As células da planilha (`L23:M25`) trazem só o nível "baixa" (H <28, M <25). O
nível **"muito baixa"** existe apenas na imagem.

*Fonte: Costa-Pereira JP, Prado CM, Heymsfield SB, Fayh APT, Menezes-Júnior LAA,
Cabral PC, Diniz AS, Gonzalez MC. "Arm circumference as a marker of muscle mass:
cutoff values from NHANES 1999–2006." **Am J Clin Nutr** 2025;122(6):1809-18.*

> As duas figuras são do mesmo grupo de pesquisa — Gonzalez MC é autora sênior nas
> duas, e Barbosa-Silva TG (a citada pela planilha para os cortes de CP) é coautora
> da de 2021. A de CB é de **2025**, o que explica a planilha ter sido modificada
> em março de 2026.

#### As duas colunas — e por que a planilha está certa

As colunas divergem em **uma única faixa, IMC < 18,5**, e são idênticas nas outras
quatro. Isso foi conferido contra as fontes primárias, e **não é defeito de
transcrição: são duas recomendações publicadas que discordam nessa faixa e só
nessa.**

**A coluna do adulto saudável é a fonte primária.** Gonzalez et al. 2021, Tabelas
5 e 6, traz o fator para IMC < 18,5 como **+4,3 cm** (masculino) e **+3,9 cm**
(feminino), ambos arredondados para **+4,0** — e o texto do artigo é literal:

> *"The adjusted CC is easily obtained by adding 4 cm (BMI <18.5) or subtracting
> 3, 7, or 12 cm (BMI 25–29, 30–39, and ≥40, respectively)."*

**A coluna clínica é literatura posterior.** A orientação do **GLIM** para o
critério fenotípico de massa muscular não incorpora o +4, e a literatura que a
aplica é explícita ao dizer por quê: no IMC < 18,5 o ajuste

> *"is not applicable in catabolic disease conditions with suspicion of weight loss
> or muscle wasting and not recommended on the guidance for assessment of the
> muscle mass phenotypic criterion in GLIM."*

A razão é direta. Somar centímetro **aproxima a medida do corte por cima** e faz o
depletado parecer adequado. No paciente magro por constituição isso corrige um viés
real; no paciente magro *porque perdeu massa*, mascara o diagnóstico — e é
falso-negativo na exata população em que ele custa mais.

#### Como a escolha entra no sistema — campo, não tela

O critério do GLIM é **"suspeita de perda de peso ou de massa"**: atributo *do
paciente*, avaliado à beira do leito, um por um. Não é modo de trabalho, e por isso
não é menu.

Telas ou menus separados foram considerados e recusados por três motivos
verificáveis:

1. **Menu é navegação; isto é dado clínico.** Descobrir no meio da avaliação que o
   paciente perde peso há seis meses exigiria sair, entrar por outra porta e
   redigitar tudo.
2. **Nenhuma das portas seria "a da UTI".** O politraumatizado magro de 22 anos sem
   perda de peso e o idoso caquético convivem na mesma unidade. A UTI precisa das
   duas colunas.
3. **As colunas são idênticas em IMC ≥ 18,5** — a maioria dos pacientes. Dois itens
   de menu que devolvem resultado igual na maior parte dos casos e divergem em
   silêncio só na faixa do falso-negativo ensinam o usuário a não olhar para a
   diferença onde ela importa.

O desenho adotado usa o dado que o sistema **já tem**: `peso_usual` e a janela da
perda são entradas da mesma aba (§2.7).

| Aspecto | Decisão |
|---|---|
| Campo | `populacao_referencia` na aba Antropometria |
| Valores | `POPULACAO_CLINICA` (padrão) · `ADULTO_SAUDAVEL` |
| Visibilidade | **só aparece quando IMC < 18,5** — fora dessa faixa as colunas são iguais, e exibir controle que não muda nada é mentir sobre a interface |
| Automatismo | havendo perda significativa por Blackburn (§2.7), assume `POPULACAO_CLINICA` **com o motivo escrito na tela**; a troca manual é permitida e fica registrada |
| Saída | o resultado **declara a coluna usada**, como todo valor da cascata declara a sua origem (§2.5) |
| Persistência | gravado na avaliação — prontuário registra o critério, não só o número |

> **Fora de escopo, e por isso não é este campo.** Um módulo de **adulto saudável /
> ambulatorial** é pedido legítimo, mas difere em muito mais que esta linha:
> as necessidades viriam de equação preditiva (Harris-Benedict, Mifflin-St Jeor ou
> DRIs) e não de kcal/kg, e não há dieta enteral, hidratação por regra de UTI nem
> vasopressor. A planilha da UTI **não tem nenhuma equação preditiva** (§3), então
> esse módulo precisa de especificação própria antes de existir. Não se deriva
> desta tabela.

---

## 3. Necessidades nutricionais

Todas por **kcal/kg e g/kg**. **Não há equação preditiva de gasto energético** na
planilha — nem Harris-Benedict, nem Mifflin-St Jeor, nem Ireton-Jones.

### 3.1 Por fase da terapia *(`A1:C10`)*

| | Fase aguda | Reabilitação |
|---|---|---|
| Energia | **15 – 20** kcal/kg | **25 – 30** kcal/kg |
| Proteína | **1,2 – 1,5** g/kg | **1,5 – 2,0** g/kg |

*Origem: `B4`/`B5`/`C4`/`C5` e `B7`/`B8`/`C7`/`C8`.*
Confere, peso 68: **1020 · 1360 · 1700 · 2040** kcal · **81,6 · 102 · 102 · 136** g.

O rótulo da proteína em reabilitação é `>1,5` (`C6`), e as fórmulas calculam
`1,5 × peso` e `2,0 × peso` — ou seja, a faixa implementada é **1,5 a 2,0**.

### 3.2 Terapia renal substitutiva *(`A9:B10`)*

| Modalidade | Proteína |
|---|---|
| Hemodiálise intermitente | **1,8** g/kg |
| Hemodiálise contínua | **2,0** g/kg |

Confere, peso 68: **122,4** e **136** g.

### 3.3 Obesidade *(`A13:C17`)*

| | IMC > 30 | IMC > 40 |
|---|---|---|
| Energia | **11 – 14** kcal/kg de **peso atual** | **22 – 25** kcal/kg de **peso ideal** |
| Proteína | **2,0** g/kg de peso ideal | **2,5** g/kg de peso ideal (rótulo diz IMC > 50) |

*Origem: `B15`/`B16` (peso atual, `F2`), `C15`/`C16` (peso ideal, `F3`),
`B17`/`C17` (peso ideal).*
Confere, peso 68 e ideal 82: **748 · 952 · 1804 · 2050** kcal · **164 · 205** g.

> ⚠️ **A base do peso muda entre as linhas, e é o erro mais fácil de cometer.**
> Energia em IMC > 30 usa **peso atual**; energia em IMC > 40 e toda a proteína
> usam **peso ideal**. Na planilha isso são duas células (`F2` e `F3`) que o
> usuário confunde. O cálculo deve receber os dois pesos separados e **escolher
> internamente** — o chamador não pode ter essa liberdade.

> ⚠️ **Contradição de rótulo:** o cabeçalho da coluna C diz `IMC>40` (`C13`) e o
> rodapé diz `IMC >50` (`C18`). A leitura que usa os dois: energia de 22–25 kcal/kg
> a partir de IMC > 40; proteína de 2,5 g/kg a partir de IMC > 50 (2,0 antes
> disso). Registrada como interpretação, com teste nas fronteiras 30, 40 e 50.

### 3.4 Personalizado *(`F5:G9`)*

```
kcal total = kcal/kg desejado × peso
PTN total  = g/kg desejado  × peso
```

Confere: 35 kcal/kg e 1,3 g/kg com peso 68 → **2380** kcal e **88,4** g.

---

## 4. Dieta enteral

Dez fórmulas paramétricas, replicadas por dieta nas abas
`Cálculos dietas_Contínuo` (52 linhas) e `_Intermitente` (50 linhas).

```
VT             = volume × tempo                            ml/dia
kcal           = densidade × VT                            kcal/dia
PTN            = ptn_por_litro × VT / 1000                 g/dia
kcal/kg        = kcal / peso
PTN/kg         = PTN  / peso
% VCT          = kcal × 100 / meta_energetica
% PTN          = PTN  × 100 / meta_proteica
volume pleno   = (meta_energetica / densidade) / tempo      ml/h ou ml/horário
PTN no pleno   = (volume_pleno × tempo × ptn_por_litro) / 1000
PTN suplementar= meta_proteica − PTN
```

**`tempo` é horas de infusão (contínuo) ou número de horários (intermitente).**
A planilha anota `UTI = 22H` (`R1` das duas abas) — 22 h de infusão, 2 h de pausa.

> **O modo não muda a matemática.** As duas abas têm as mesmas dez fórmulas; o
> modo só troca o rótulo (`ml/h` vs `ml/horário`, `horas` vs `nº de horários`) e o
> padrão de tempo. Uma classe, não duas — e o teste que prova isso é o mesmo
> cálculo nos dois modos, com o mesmo VT, dando o mesmo resultado.

### 4.1 Progressão *(`Contínuo!P15:V18`)*

25 % · 50 % · 75 % · 100 % da meta nos dias 1 a 4.

```
kcal_do_dia = meta_energetica × pct / 100
volume      = kcal_do_dia / densidade / tempo
```

Confere, meta 1360 e 22 h, a 1,5 kcal/ml: **10,3030 · 20,6061 · 30,9091 · 41,2121** ml/h.
Confere, meta 1800 e 6 horários, a 1,5 kcal/ml: **50 · 100 · 150 · 200** ml/horário.

### 4.2 Módulo proteico *(`Contínuo!Q21:V24`)*

Para cobrir a lacuna de proteína. A nota da planilha (`V21`) diz:
**"iniciar o módulo proteína a partir do 4º dia"**.

```
gramas_de_produto = lacuna_g × medida_g / ptn_por_medida_g
kcal_adicionadas  = gramas_de_produto / medida_g × kcal_por_medida
```

Confere, lacuna de 45 g com Nutren Just Protein (medida 15 g, 13 g PTN, 52 kcal):
`45 × 15 / 13 = 51,9231 g` = 3,4615 medidas × 52 = **180,0 kcal**, idêntico ao
`lacuna × 4` da planilha.

> ⚠️ **A fórmula de kcal da planilha está errada para o módulo com carboidrato.**
> `Contínuo!U24` faz `lacuna×4 + gramas×9,66/20` — o segundo termo são **gramas de
> carboidrato somadas a um total de kcal**, sem multiplicar por 4. Para o
> Nutridrink Protein (medida 20 g, 6 g PTN, 9,66 g CHO, 62,64 kcal) a planilha
> devolve **252,45** onde o cálculo correto pela composição dá
> `150 g = 7,5 medidas × 62,64 = ` **469,8 kcal**.
>
> Os dois caminhos concordam nos módulos **sem** carboidrato e discordam no único
> **com** — é a prova aritmética do defeito. Calcular a kcal lendo
> `kcal_por_medida` do catálogo, nunca recompondo macro por macro.

---

## 5. Hidratação

```
necessidade mínima = peso × 25   ml/dia
necessidade ideal  = peso × 30   ml/dia
água na dieta      = volume_dieta × % de água / 100
água extra         = necessidade − água na dieta
distribuição       = água extra / n     para n ∈ {4, 5, 6, 8}
```

*Origem: `Hidratação!D5`/`D6` · `D10:D13` · `D17:I21`. Referência `D4`:
`25 a 30ml/Kg/dia`. Nota `E15`: "dividir este volume mínimo ou ideal no nº de
vezes que deseja ofertar".*

Confere, peso 72 e dieta 1600 ml: necessidade **1800** / **2160**; água na dieta
**1120 · 1200 · 1280 · 1360**; extra ideal **1040 · 960 · 880 · 800**; 6× do ideal
**173,3333 · 160 · 146,6667 · 133,3333**.

### 5.1 O % de água — propriedade do produto, não da densidade

A planilha tabula quatro densidades exatas *(`G5:H8`)*:

| Dieta | % de água |
|---|---|
| 2,0 kcal/ml | 70 |
| 1,5 kcal/ml | 75 |
| 1,2 kcal/ml | 80 |
| 1,0 kcal/ml | 85 |

> ⚠️ **12 das 54 fórmulas do catálogo têm densidade intermediária** (1,12 · 1,14 ·
> 1,21 · 1,23 · 1,24 · 1,3 · 1,31 · 1,33) e **não têm linha nesta tabela**.
> Aplicar uma função escada seria inferência nossa.
>
> **O teor de água livre é propriedade do produto e vem no rótulo — não se deriva
> da densidade.** A literatura mede 1,0 → 84 % · 1,2 → 82 % · 1,5 → 76 % ·
> 2,0 → 70 %, que não coincide com os números redondos da planilha justamente
> porque ela usa aproximação por faixa.
>
> **Decisão:** `agua_livre_perc` é campo de `formula_enteral`, alimentado pelo
> rótulo de cada produto. Faltando o dado, o cálculo cai na tabela por densidade
> acima e **diz que caiu** ("estimada pela densidade" vs "água livre do rótulo").
> As densidades intermediárias deixam de ser um problema: nunca precisaram da
> escada.
>
> Fontes: [Free Water in Tube Feeding Formulas](https://www.visualveggies.com/free-water-in-tube-feeding-formulas/)
> · [Enteral Formula Selection, Univ. of Virginia](https://med.virginia.edu/ginutrition/wp-content/uploads/sites/199/2015/11/MaloneArticle-June-05.pdf)

---

## 6. Ferramentas clínicas

### 6.1 Noradrenalina *(`Cálculos!C3`, `C4`, `J6`)*

Uma fórmula, não duas:

```
concentração (mcg/ml) = n_ampolas × 4 mg × 1000 / volume_do_soro_ml
dose (mcg/kg/min)     = vazão_ml_h × concentração / peso / 60
```

Os "simples 32" e "concentrada 64" da planilha são **presets** desta fórmula:
2 ampolas em 250 ml = 32 mcg/ml; 4 ampolas em 250 ml = 64 mcg/ml. Notas da
planilha: `D1` "considerando diluição em 250ml soro", `D3` "2 ampolas",
`D4` "4 ampolas".

Confere, peso 70: 25 ml/h simples → **0,190476**; 25 ml/h concentrada →
**0,380952**; 4 ampolas em 234 ml a 20 ml/h → **0,325600**.
**As duas assinaturas devem concordar** — é o teste que torna os 32/64 auditáveis.

O exemplo do segundo bloco usa soro de **234 ml** (`J5`), que é 250 − 4 × 4 ml:
o volume **final** da bolsa, já com as ampolas.

### 6.2 Balanço nitrogenado *(`Cálculos!C11:B13`)*

```
N ingerido  = PTN_ingerida_24h / 6,25
N excretado = ureia_urinaria_24h / 2,14 + 4
balanço     = N ingerido − N excretado          > 0 = anabolismo
```

Confere: PTN 95 g e ureia 40 g → **15,2** · **22,6916** · **−7,4916**.
O `+4` são as perdas insensíveis.

### 6.3 Calorias do propofol *(`Cálculos!B18`)*

```
kcal/dia = vazão_ml_h × horas × 1,1
```

1,1 kcal/ml é a emulsão lipídica do propofol a 1 %. Confere: 20 ml/h × 24 h →
**528** kcal/dia. As **24 h** são constante escondida na planilha; aqui viram
campo com esse padrão.

---

## 7. Dieta artesanal — sistema aberto

Composição dos insumos *(`TNE Sistema aberto!A2:E5`)*, **por medida**:

| Insumo (medida) | kcal | CHO g | PTN g | LIP g |
|---|---|---|---|---|
| Trophic basic pó (1 medida = 7,8 g) | 33,93 | 4,68 | 1,24 | 1,09 |
| Carbodex (1 medida = 10 g) | 40 | 10 | 0 | 0 |
| Albumix power (1 medidor = 20 g) | 70 | 1,5 | 16 | 0 |
| Óleo de soja (1 colher de sopa = 13 ml) | 108 | 0 | 0 | 12 |

```
doses sugeridas  = ((VET − LIP_do_óleo × 9) / kcal_por_medida_da_base) × 0,85
CHO/PTN/LIP      = Σ (doses do insumo × composição do insumo)
kcal base        = PTN×4 + (CHO_albumix + CHO_trophic)×4 + LIP×9
kcal total       = kcal base + doses_carbodex × 40
kcal/kg · PTN/kg = totais / peso
% CHO/PTN/LIP    = macro × fator / VET × 100
água             = 28,5 × doses_da_base + 100 + 100 × doses_carbodex
embalagens/mês   = (doses/dia × g_por_medida / g_por_embalagem) × 31
receita por dose = cada insumo / nº de administrações
```

*Origem: `B9` · `C9:E11` · `B12` · `B14` · `F9`/`G9` · `H9:J9` · `B15` ·
`B17:B20` · `B24:B27`.*

Confere, VET 2000 · peso 75 · óleo 0,5 colher · Carbodex 3 · Albumix 2:
doses **48,7504** · CHO **228,1517** · PTN **60,4505** · LIP **53,1379** ·
totais **261,1517** / **92,4505** / **59,1379** · kcal base **1826,6498** ·
**kcal total 1946,6498** · kcal/kg **25,9553** · PTN/kg **1,2327** ·
% **52,2303** / **18,4901** / **26,6121** · água **1789,3855** · por dose **447,3464**.

Embalagens: Trophic 800 g · Carbodex 500 g · Albumix 500 g · óleo 900 ml, × **31 dias**.

> ✅ **Verificação de fechamento energético:** `261,1517×4 + 92,4505×4 + 59,1379×9
> = 1946,6498` — idêntico ao `kcal total` da planilha. Os dois caminhos coincidem,
> o que valida a primitiva `kcal = CHO×4 + PTN×4 + LIP×9` (Atwater) contra a
> fonte. **`kcalDeMacros` deve ser a única porta pela qual um grama vira caloria
> no sistema** — é o que torna o defeito do §4.2 inexprimível.

> ⚠️ **Os percentuais somam 97,33 %, não 100 %.** A planilha divide cada macro
> pelo **VET desejado** (2000), não pela kcal efetivamente ofertada (1946,65).
> Não é erro de conta, é escolha de denominador — mas o profissional que vê
> 52,2 + 18,5 + 26,6 acha que 2,7 % desapareceram. Expor **os dois**, nomeados:
> `% sobre o ofertado` (soma 100 por construção) e `% sobre o VET` (o número da
> planilha).

> ⚠️ **Dois defeitos no óleo.** `B20` calcula as embalagens com `10` ml onde a
> medida é declarada 13 ml. E `B27` é a constante `13/4`, ignorando a entrada
> `B13` que todas as outras fórmulas usam — a receita por dose do óleo não muda
> quando se muda a quantidade de óleo.

---

## 8. Catálogos

### 8.1 Fórmulas enterais — `Dietas consulta`, 54 produtos → 53 no catálogo

Colunas: densidade (kcal/ml) · PTN · CHO · LIP · fibras (g/L) · potássio (mg/L) ·
osmolaridade (mOsm/L). **Tabela de consulta pura: zero fórmulas, e nenhuma célula
de nenhuma aba a referencia** — densidade e PTN estão duplicadas manualmente nas
abas de cálculo.

> ⚠️ **Dois produtos têm composição por embalagem de 500 ml, não por litro:**
> `Fresubin 2kcal HP (500ml)` e `Fresubin 2kcal HP Fibre(500ml)`, células `A28` e
> `A29` da aba. A aba `Intermitente` divide a proteína desses por **500**
> (`G28`, `G29`); a aba `Contínuo` divide por **1000** — e **subestima a proteína
> em 50 %**.
>
> *(Conferido célula a célula na aba: são esses dois e mais nenhum. Uma versão
> anterior deste documento falava em três, incluindo um `Novasource 2.0 (500ml)`
> que não existe no catálogo.)*
>
> **Decisão: normalizar tudo para "por litro" no catálogo.** Fresubin 2kcal HP
> passa de 50 g/500 ml para **100 g/L** — coerente com o produto real (10 g de
> proteína por 100 ml). **E o sufixo `(500ml)` sai do nome** (migration `022`):
> tamanho de embalagem não é composição, e mantê-lo ao lado de um número por
> litro reintroduz pela porta do rótulo a leitura que a normalização veio matar. Com isso a ramificação `/500` vs `/1000` deixa de existir,
> e um `CHECK` de fechamento energético (±12 % entre a densidade declarada e a
> soma dos macros por Atwater) torna o erro não representável.

> **São 53 linhas no seed, não 54:** `Isosource 1.5` aparece duas vezes na
> planilha, com valores idênticos (ver *Outras inconsistências*, abaixo). O
> índice único parcial por nome recusaria a segunda de qualquer forma.

#### O teste de fechamento energético, e o que ele encontrou

Toda linha do catálogo é conferida por **Atwater** contra a densidade declarada:

```
CHO × 4 + PTN × 4 + LIP × 9  ≈  densidade × 1000     (tolerância ±12 %)
```

A tolerância é de **12 %**, e não é generosidade: rótulo de fórmula enteral
arredonda macro, e o fator de Atwater é ele próprio aproximação. O que a faixa
pega são os erros de **ordem de grandeza** — densidade trocada, fator 10 num
macro, composição por embalagem lançada como litro.

**50 dos 54 produtos fecham**, a maioria dentro de ±3 %. E `Fresubin 2kcal HP`
fecha em **exatamente +0,0 %** depois da normalização por litro — prova
aritmética de que "por litro" é o certo: antes da normalização ele estaria em
−50 %.

**Quatro reprovaram, e os quatro foram pesquisados na ficha do fabricante:**

| Produto | Planilha | Fecha | Corrigido para | Fecha | Fonte |
|---|---|---|---|---|---|
| `Diben HP` | densidade **1,0** | +45,4 % | densidade **1,5** | −3,1 % | é o *Diben 1.5 kcal HP*, 75 g PTN/L — a densidade foi transcrita errada ([Fresenius Kabi](https://www.humanaalimentar.com.br/dieta-enteral-por-sonda/dieta-para-situacoes-especificas/diben-1-5-kcal-hp-easy-bag-1-000ml-fresenius-kabi)) |
| `Nutrison Advanced Peptisorb` | LIP **170** g/L | +141,0 % | LIP **17** g/L | +3,3 % | 1,7 g/100 ml = 17 g/L — **erro de fator 10** ([Nutricia](https://nutricia.com.au/adult/wp-content/uploads/sites/7/2021/02/Nutrison-Advanced-Peptisorb-Factsheet-AUS-July-2020.pdf)) |
| `Survimed OPD` | CHO **90** g/L | −20,8 % | CHO **142** g/L | **0,0 %** | distribuição declarada 18 % PTN · 57 % CHO · 25 % LIP em 1000 kcal/L ([Fresenius Kabi](https://www.fresenius-kabi.com/br/produtos/survimed-opd)) |
| `Peptimax pó` | PTN 36 · CHO 126 · LIP **100** · K **8360** | +54,8 % | PTN 45 · CHO 155 · LIP 22 · K **nulo** | −0,2 % | distribuição declarada 18 % PTN · 62 % CHO · 20 % LIP ([Prodiet](https://www.humanaalimentar.com.br/dieta-enteral-por-sonda/dieta-para-situacoes-especificas/peptimax-400g-prodiet)) |

> ⚠️ **`Peptimax pó` é o único cuja correção não é uma leitura direta.** A planilha
> tem **dois** valores impossíveis na mesma linha (LIP 100 g/L a 1,0 kcal/ml, e
> potássio 8360 mg/L — uma ordem de grandeza acima de todos os outros), o que torna
> a linha inteira suspeita. Os três macros foram derivados da distribuição
> percentual que o fabricante declara, e o potássio foi deixado **nulo** — melhor
> ausente e dito do que absurdo e silencioso. **Pendente de ficha técnica.**

O `CHECK` de fechamento na tabela é o que impede qualquer um desses quatro erros de
voltar por digitação futura.

#### Outras inconsistências do catálogo

A conferir no seed, mas sem impacto de cálculo: `Isosource 1.5` aparece
**duplicado** (linhas 19 e 24, valores idênticos); `Nutro Premium PreFibra 1.5` e
`Nutro Premium 1.5` têm potássio `"-"` (texto, não número → nulo);
`Nutrison Soya Fiber` está sem osmolaridade; `Nutro Premium 1.5` tem PTN **61** na
aba de cálculo e **75** no catálogo (usar o catálogo, que é a tabela de
composição); e vários nomes têm **espaço à direita** (`'Impact '`,
`'Isosource 1.5 '`, `'Nutri enteral Soya Fiber '`) — aplicar `trim` no seed, senão
qualquer junção por nome quebra.

### 8.2 Suplementos — aba `Suplementos`, 23 produtos + 3 módulos proteicos

Colunas: porção (g) · kcal · PTN · CHO · açúcar · LIP · sódio · potássio ·
fósforo · ferro · fibras · osmolaridade. Legenda `P6`: **`NI = Não informado`**.

**Módulos proteicos** *(`A30:E33`)* — são as linhas que as abas de cálculo
referenciam (`Suplementos!C31`, `C32`, `C33`):

| Módulo | medida (g) | PTN g | CHO g | kcal |
|---|---|---|---|---|
| Nutren Just Protein | 15 | 13 | 0 | 52 |
| Fresubin Protein Powder | 12 | 10,4 | 0 | 41,6 |
| Nutridrink Protein | 20 | 6 | 9,66 | 62,64 |

> A tabela dos 23 suplementos orais **não alimenta cálculo nenhum** na planilha —
> é consulta. No eroERP idem: o cadastro de suplementos é um CRUD órfão e os
> módulos proteicos estão *hardcoded* em `calculoDietaEnteral.ts:16-20`. Aqui os
> módulos e os insumos da dieta artesanal passam a ser **linhas do catálogo com
> flag**, e a sugestão itera o catálogo — cadastrar um produto novo faz ele
> aparecer na sugestão.

---

## 9. Faixas de validade e limites declarados

| Item | Limite | Origem |
|---|---|---|
| Infusão contínua em UTI | 22 h/dia | `Contínuo!R1` |
| Progressão da dieta | 25 → 50 → 75 → 100 % nos dias 1 a 4 | `Contínuo!P15:P18` |
| Módulo proteico | a partir do 4º dia | `Contínuo!V21` |
| Água extra | fracionável em 4×, 5×, 6× ou 8× ao dia | `Hidratação!F16:I16` |
| Necessidade hídrica | 25 a 30 ml/kg/dia | `Hidratação!D4` |
| Percentil 50 de CB | 18 a 90,9 anos | `Estimativas!I13:I21` |
| Chumlea 1988 | 19–59 e ≥60 anos (convenção nossa nos 60) | `Estimativas!A13:A20` |
| Adequação de CB | < 70 % a > 120 % | `Estimativas!N12:O17` |
| Perda de peso | janelas de 1 semana, 1 mês, 3 e 6 meses | `Estimativas!A37:A40` |

---

## 10. Casos canônicos

Gabaritos para os testes, todos conferidos contra os valores em cache da planilha,
com a célula no comentário do teste.

| Domínio | Entradas | Esperado |
|---|---|---|
| **Antropometria** | CB 25 · AJ 53 · idade 59 · CP 34 · CA 90 · altura 1,68 | Chumlea 85: `168,89` H / `167,71` M · Chumlea 88 (8 ramos): `54,75` `59,24` `52,55` `55,61` `57,74` `59,26` `57,49` `59,78` · Jung: `60,845` / `57,345` · Rabito: `66,3083` / `61,4394` · peso ideal: `62,0928` / `58,70592` / `70,56` · ajustado(85; 58,71): `67,3857` · amputação(65; 1,6 %): `63,96` |
| | peso 62 · altura 1,75 | IMC `20,2449` |
| | usual 68 · atual 60 | perda `11,7647 %` → grave em 1 mês |
| | CB 30 · P50 32,3 | adequação `92,8793 %` → Eutrofia |
| **Necessidades** | peso 68 · ideal 82 | `1020` `1360` `1700` `2040` kcal · `81,6` `102` `102` `136` g · HD `122,4` / `136` · obeso `748` `952` `1804` `2050` · `164` / `205` · personalizado 35 e 1,3 → `2380` / `88,4` |
| **Dieta contínua** | peso 68 · VCT 1360 · PTN 102 · 22 h · Peptamen Intense (1,0 · 92 g/L) · 62 ml/h | VT `1364` · kcal `1364` · PTN `125,488` · kcal/kg `20,0588` · PTN/kg `1,8454` · %VCT `100,2941` · %PTN `123,0275` · pleno `61,8182` · PTN no pleno `125,12` |
| **Dieta intermitente** | peso 65 · VCT 1800 · PTN 80 · 6 horários · Novasource Senior (1,24 · 65) · 133 ml | VT `798` · PTN `51,87` · pleno `241,9355` · PTN no pleno `94,3548` |
| **Progressão** | meta 1360 · 22 h · 1,5 kcal/ml | `10,3030` `20,6061` `30,9091` `41,2121` ml/h |
| **Módulo proteico** | lacuna 45 g · Nutren Just Protein | `51,9231` g → `180,0` kcal |
| **Hidratação** | peso 72 · dieta 1600 ml | `1800` / `2160` · água `1120` `1200` `1280` `1360` · extra ideal `1040` `960` `880` `800` · 6× `173,3333` `160` `146,6667` `133,3333` |
| **Noradrenalina** | peso 70 | 25 ml/h simples `0,190476` · concentrada `0,380952` · 4 amp/234 ml a 20 ml/h `0,325600` — **e as duas assinaturas concordam** |
| **Balanço N** | PTN 95 g · ureia 40 g | `15,2` · `22,6916` · `−7,4916` |
| **Propofol** | 20 ml/h · 24 h | `528` kcal/dia |
| **Artesanal** | VET 2000 · peso 75 · óleo 0,5 · Carbodex 3 · Albumix 2 | doses `48,7504` · totais `261,1517` / `92,4505` / `59,1379` · kcal base `1826,6498` · **kcal total `1946,6498`, e `kcalDeMacros` fecha no mesmo número** · água `1789,3855` |

---

## 11. Os defeitos da planilha — não replicar

Cada um ganha um teste que verifica que **o nosso número é diferente do dela**.
Nenhum desses testes confere o valor da planilha; é o único formato que impede a
regressão.

| # | Célula | Defeito | Efeito |
|---|---|---|---|
| 1 | `Contínuo!G50:G52` | divide a PTN das apresentações de 500 ml por 1000 (a aba Intermitente divide por 500) | **proteína −50 %** |
| 2 | `Dietas consulta` `A28`, `A29` | composição de 2 produtos por embalagem de 500 ml, não por litro | idem |
| 3 | `Contínuo!J` linhas 2,3,5-11,13,22 | %VCT dividido pela PTN total em vez do VCT | percentual absurdo |
| 4 | `Contínuo!H52`, `I52` | kcal/kg e PTN/kg divididos pela **altura** | |
| 5 | `Intermitente!H5`, `H6` | kcal/kg dividido pelo VCT e pela PTN | |
| 6 | `Contínuo!E38`, `E52` | VT = volume × célula **vazia** → VT sempre 0 | dieta zerada |
| 7 | `Contínuo!F`,`G` (42 de 52) | literal `0` no lugar da fórmula | |
| 8 | `Intermitente!N11` | PTN suplementar usa célula vazia como meta | |
| 9 | `Hidratação!D21`, `E21` | auto-referenciam `Hidratação!B15`, célula vazia da própria aba; deveriam ler `'TNE Sistema aberto'!B15` | água extra da dieta artesanal não desconta nada |
| 10 | `Contínuo!U24` | kcal do módulo soma **gramas** de CHO a um total de **kcal** | `252,45` onde o certo é `469,8` |
| 11 | `TNE SA!B20` | embalagens de óleo com 10 ml onde a medida é 13 ml | |
| 12 | `TNE SA!B27` | receita de óleo por dose é a constante `13/4` | ignora a entrada |
| 13 | `TNE SA!H9:J9` | % de macros sobre o VET desejado somam 97,33 % sem dizer | |
| 14 | — | nada liga `Necessidades!B5` a `Contínuo!P4`: a meta é **redigitada** em cada aba | |
| 15 | — | o peso do paciente está em **8 células independentes** (85 · 60 · 62 · 68 · 68 · 72 · 65 · 75 · 70) e a altura em **4** (1,68 · 1,75 · 1,72 · 1,60) | |
| 16 | `Estimativas!A13:A20` | Chumlea 88 rotula "19-59" e ">60": os 60 exatos não caem em faixa | |
| 17 | `Estimativas!B43:B49` | "Sobrepeso >25 <30" e "Obesidade I >30 a 34,9" excluem 25,0 e 30,0 exatos | |
| 18 | `Necessidades!C13` vs `C18` | cabeçalho diz `IMC>40`, rodapé diz `IMC >50` | |
| 19 | `Estimativas!J24` | o P50 de CB é **digitado**, com a tabela `I12:K21` ao lado sem ser consultada | |
| 20 | — | o peso estimado de `Estimativas` **não alimenta aba nenhuma** | |
| 21 | `Hidratação!G5:H8` | a tabela de % de água só tem 4 densidades exatas; 12 das 54 fórmulas não têm linha | ver §5.1 |
| 22 | `Prescr x Inf!E7:E452` | 446 células gravadas com `#DIV/0!` | fora desta fatia |

**Defeitos 14 e 15 são o problema estrutural**, não erros de fórmula: a planilha
não tem cascata. É o que a tipagem de `PesoDeTrabalho`, `Altura`,
`MetaEnergetica`, `MetaProteica` e `VolumeDieta` resolve — nenhuma etapa aceita um
`BigDecimal` de peso, e o resultado exibe a origem por escrito.

**Sobre arredondamento:** os `.ts` do eroERP operam em `number` (IEEE-754) e não
arredondam em lugar nenhum — zero ocorrências de `toFixed`/`Math.round` em
`calculo/*.ts`. O float cheio é enviado e cai em `NUMERIC(10,2)`, onde o Postgres
arredonda com a regra que quiser: **o arredondamento de uma prescrição de UTI é
acidental**. Aqui é `BigDecimal`, `MathContext.DECIMAL64` nas contas
intermediárias, e arredondamento uma vez na saída com a escala declarada.

---

## 12. Referências

**Da planilha, citadas nas células:**

- **Chumlea et al., 1985** — altura estimada pela altura do joelho *(`Estimativas!C8`)*
- **Chumlea et al., 1988** — peso estimado por AJ e CB, 8 ramos *(`C12`)*
- **Jung, 2004** — peso estimado *(`C23`)*
- **Rabito et al., 2008** — peso estimado *(`C27`)*
- **Osterkamp, 1995** — % do peso por membro amputado *(`D25`)*
- **Barbosa-Silva, 2016** — cortes de circunferência da panturrilha *(`K28`)*
- **Blackburn, 1977** — classificação da perda de peso *(`A41`)*
- **OMS, 1997** e **OPAS, 2002** — classificação de IMC *(`B43`, `C43`)*

**Pesquisadas para completar o que a planilha não traz em célula:**

- **Gonzalez MC, Mehrnezhad A, Razaviarab N, Barbosa-Silva TG, Heymsfield SB.**
  "Calf circumference: cutoff values from the NHANES 1999–2006." *Am J Clin Nutr*
  2021;113(6):1679-87. DOI
  [10.1093/ajcn/nqab029](https://doi.org/10.1093/ajcn/nqab029) · PMID 33742191 —
  **o ajuste da CP pelo IMC (§2.9), Tabelas 5 e 6.** Fatores derivados: +4,3 cm
  (M) e +3,9 cm (F) em IMC < 18,5, arredondados para +4,0; −3, −7 e −12 nas faixas
  superiores. É a fonte da **coluna "adulto saudável"**.
- **Barazzoni R, Jensen GL, Correia MITD, Gonzalez MC, Higashiguchi T, Shi HP, et al.**
  "Guidance for assessment of the muscle mass phenotypic criterion for the Global
  Leadership Initiative on Malnutrition (GLIM) diagnosis of malnutrition."
  *Clin Nutr* 2022;41(6):1425-33. DOI
  [10.1016/j.clnu.2022.02.001](https://doi.org/10.1016/j.clnu.2022.02.001) — é a
  fonte da **coluna "população clínica"**: a orientação não incorpora o +4, e a
  literatura que a aplica registra que em IMC < 18,5 o ajuste *"is not applicable
  in catabolic disease conditions with suspicion of weight loss or muscle wasting"*.
  **Gonzalez MC é autora nos dois lados** — a divergência entre as colunas é
  deliberada, não contradição entre grupos.
- **Costa-Pereira JP, Prado CM, Heymsfield SB, Fayh APT, Menezes-Júnior LAA,
  Cabral PC, Diniz AS, Gonzalez MC.** "Arm circumference as a marker of muscle
  mass: cutoff values from NHANES 1999–2006." *Am J Clin Nutr* 2025;122(6):1809-18
  · PMID 41015191 — o ajuste da CB pelo IMC e o nível "muito baixa" (§2.9).
  Fatores conferidos contra o resumo: −3 (H) / −2 (M), −7 / −6, −10 / −9; cortes
  28/25 cm (baixa) e 26/23 cm (muito baixa).
- Classificação de IMC da OPAS 2002, quatro faixas:
  [Rev. Bras. Geriatr. Gerontol.](https://www.scielo.br/j/rbgg/a/qXgVGH3dqzw6cK36qM4RBCD/?format=html&lang=pt)
  · [Pontos de corte do IMC em idosos](https://www.redalyc.org/journal/4979/497950365004/html/) (§2.6)
- Teor de água livre de fórmulas enterais:
  [Free Water in Tube Feeding Formulas](https://www.visualveggies.com/free-water-in-tube-feeding-formulas/)
  · [Enteral Formula Selection, Univ. of Virginia](https://med.virginia.edu/ginutrition/wp-content/uploads/sites/199/2015/11/MaloneArticle-June-05.pdf) (§5.1)
- **Estatuto do Idoso**, Lei 10.741/2003 — a convenção de ≥60 anos (§2.2)

**Autoria da planilha:** `marcelo kamp`, criada em 2022-07-04, última modificação
2026-03-24. Caminho original do arquivo indica autoria clínica de
`nutriandressaoliveira` ("tabela facilita nutri").

---

## 13. O que fica pendente

1. **A fonte primária do Chumlea 1988 de 8 ramos** (§2.2) — os números são
   auditáveis contra a planilha, mas a publicação exata não foi identificada.
2. ~~**Adulto saudável vs população clínica** no ajuste de CB e CP (§2.9).~~
   **Resolvido pela pesquisa:** não é defeito da planilha — são duas
   recomendações publicadas (Gonzalez 2021 e a orientação GLIM 2022) que
   discordam só na faixa IMC < 18,5. Vira o campo `populacao_referencia`, visível
   apenas nessa faixa, com padrão clínico deduzido da perda de peso. Ver §2.9.
3. **Faixas de referência de laboratório** (K, Na, Mg, lactato, pH, pCO₂, HCO₃,
   PCR, glicemia) — não estão na planilha e só são necessárias para as bandas dos
   gráficos do painel diário. Pesquisar com citação quando a fatia dos painéis
   chegar.
4. **`agua_livre_perc` está vazio nas 53 fórmulas do catálogo.** A coluna existe
   e o cálculo a prefere quando preenchida (§5.1), mas o seed não trouxe o dado:
   ele vem do **rótulo de cada produto**, e inventá-lo por aproximação seria
   exatamente a inferência que este documento recusa. Consequência hoje: as 41
   fórmulas de densidade exata caem na escada da planilha — dizendo que
   estimaram — e as **12 de densidade intermediária ficam sem cálculo de água**,
   com o motivo na tela. É preenchimento de cadastro, não de código: a tela de
   fórmula enteral tem o campo, e a nutricionista o preenche com o rótulo na mão.
5. **As abas fora do escopo desta fatia:** `Controle Ingestão` (média de aceitação
   por refeição), `Prescr x Inf` (prescrito × infundido), `Acomp` e `Paciente`
   (formulários em branco, 1.087 células sem fórmula) e `Siglário` (35 siglas).
