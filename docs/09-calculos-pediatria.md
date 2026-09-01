# Cálculos da Pediatria

> Fatia 5 — **especificação numérica**. Este documento é a fonte de verdade das
> fórmulas do módulo `pediatria`. Nenhum número aqui foi inferido: tudo vem da
> planilha `Pediatria.xlsx`, na raiz do repositório, com a célula de origem
> citada.
>
> Leia junto com [03-arquitetura-backend.md](03-arquitetura-backend.md) §8
> (fórmulas em classe pura) e [04-arquitetura-frontend.md](04-arquitetura-frontend.md) §7
> (telas de cálculo).

---

## 1. Por que este documento existe

O `CLAUDE.md` bloqueava as fatias de cálculo com uma frase que continua valendo:

> Nenhuma fórmula nutricional deve ser implementada por inferência: o sistema
> prescreve dieta para paciente de UTI.

Uma fórmula lida de código-fonte alheio não é auditável. Uma fórmula lida de uma
planilha, com a célula citada e um caso de teste conferido, é. Este documento faz
a segunda coisa para a pediatria — e é ele, não o código do eroERP, que a
implementação deve seguir.

**A planilha continua sendo o árbitro.** Divergiu do código, o código está errado.

### Fonte

`Pediatria.xlsx`, 23.331 bytes, três abas:

| Aba | Conteúdo |
|---|---|
| `Planilha1` | entradas, resultados, tabelas de referência e instruções |
| `Percentil_M` | percentis da OMS, masculino, 0 a 60 meses |
| `Percentil_F` | percentis da OMS, feminino, 0 a 60 meses |

Autoria: **Tiago da Silva Martins**. Criada em 2025-08-02, última revisão
2026-06-05.

---

## 2. Entradas

| Entrada | Unidade | Obrigatória | Célula |
|---|---|---|---|
| Sexo | `M` ou `F` | sim | `B2` |
| Idade | meses, **inteiro** | sim | `B3` |
| Peso | kg | sim | `B4` |
| Estatura | cm | não | `B5` |
| Fórmula láctea | — | não | `E2` |
| Volume por tomada | ml | não | `E3` |
| Frequência | **horas de intervalo** | não | `E4` |

⚠️ **"Frequência" é o intervalo entre as tomadas, não a quantidade delas.**
`E4 = 3` significa uma tomada a cada 3 horas, logo 8 tomadas por dia. É o erro de
leitura mais provável de quem implementar esta planilha sem ler esta linha.

---

## 3. IMC

```
IMC = peso_kg / (estatura_cm / 100)²
```

Unidade kg/m². Calculado apenas se **peso > 0 e estatura > 0**; sem estatura, o
IMC e a classificação de IMC para a idade ficam ausentes.

*Origem: `B10` — `=IF(B5>0, B4/((B5/100)^2), "")`*

---

## 4. Estado nutricional — OMS, 0 a 60 meses

### 4.1 A regra de classificação

As três classificações usam a mesma regra de três faixas, comparando o valor
medido com os percentis **P15** e **P85** da tabela do sexo do paciente, na linha
da sua idade em meses:

```
valor <  P15          → faixa baixa
P15 ≤ valor ≤ P85     → faixa adequada
valor >  P85          → faixa alta
```

**Atenção à assimetria: P15 e P85 são ambos inclusivos na faixa adequada.** Só
`> P85` sobe de faixa. Um paciente com peso exatamente igual ao P85 é adequado,
não "acima do peso". Vem direto do `CHOOSE(1 + (valor>=P15) + (valor>P85), …)`
da planilha, e é o tipo de detalhe que se perde ao reimplementar de memória.

| Índice | Valor comparado | Faixa baixa | Faixa adequada | Faixa alta | Célula |
|---|---|---|---|---|---|
| Peso para a idade | peso (kg) | `Baixo peso` | `Peso adequado` | `Acima do peso` | `B8` |
| Estatura para a idade | estatura (cm) | `Baixa estatura` | `Adequada` | `Estatura alta` | `B9` |
| IMC para a idade | IMC | `Magreza` | `IMC adequado` | `Sobrepeso` | `B11` |

Os rótulos são exatamente estes, incluindo a inconsistência de `Adequada` (e não
"Estatura adequada") na linha do meio da estatura.

### 4.2 Faixa de validade

Só há linha de percentil para **idade inteira de 0 a 60 meses**. Fora disso — 61
meses, ou idade fracionária — **não há classificação**. A planilha devolve vazio
pelo `IFERROR`; aqui o resultado vem acompanhado do motivo, para a tela poder
explicar em vez de mostrar um traço mudo.

### 4.3 As tabelas de percentil

Cada aba tem 61 linhas (0 a 60 meses) e, para cada uma das três medidas, cinco
percentis: **P3, P15, P50, P85, P97**.

**A cópia canônica fica na migration `015-create-percentil-oms.xml`**, semeada a
partir da planilha. Não replicar os 1.830 valores aqui: duas cópias divergem, e
divergência em tabela de referência de crescimento infantil é classificação
errada.

Apenas a classificação usa P15 e P85. **P3, P50 e P97 são carregados mesmo assim**
— são o que uma curva de crescimento na tela vai pedir, e a fonte já os tem.

Linhas de âncora, para conferir que a carga não saiu deslocada:

| Aba | Idade | Peso P15 / P85 | Estatura P15 / P85 | IMC P15 / P85 |
|---|---|---|---|---|
| `Percentil_M` | 0 | 2,9 / 3,9 | 47,9 / 51,8 | 12,2 / 14,8 |
| `Percentil_M` | 12 | 8,6 / 10,8 | 73,3 / 78,2 | 15,5 / 18,3 |
| `Percentil_M` | 60 | 16,0 / 21,1 | 105,2 / 114,8 | 13,9 / 16,7 |
| `Percentil_F` | 0 | 2,8 / 3,7 | 47,2 / 51,1 | 12,1 / 14,7 |
| `Percentil_F` | 8 | 7,0 / 9,0 | 66,3 / 71,2 | 15,4 / 18,5 |
| `Percentil_F` | 60 | 15,7 / 21,3 | 104,5 / 114,4 | 13,8 / 17,0 |

### 4.4 A descontinuidade dos 25 meses

Entre 24 e 25 meses a estatura dá um salto (masculino: P15 de 84,6 para 84,7;
IMC P15 de 14,5 para 14,8). **Não é erro de digitação e não deve ser "corrigido".**
É a emenda das duas curvas da OMS: comprimento deitado até 24 meses, estatura em
pé a partir daí. Transcrever como está é o comportamento correto.

---

## 5. VET — Valor Energético Total

```
VET = (89 × peso_kg − 100) + deposição
```

Unidade kcal/dia. A deposição energética depende da faixa etária:

| Faixa | Deposição |
|---|---|
| 0 a 3 meses | + 175 |
| 4 a 6 meses | + 56 |
| 7 a 12 meses | + 22 |
| 13 a 35 meses | + 20 |
| **acima de 35 meses** | **não coberto** |

Requer peso > 0. Acima de 35 meses não há VET — e o resultado carrega o motivo.

*Origem: `B13`, com a tabela textual em `M14:N18`. Referência: **DRIs, 2002**
(célula `K19`) — é a equação de EER do IOM para lactentes.*

---

## 6. Necessidade proteica

**Valor fixo por faixa etária — não é por quilo de peso.** Unidade g/dia.

| Faixa | g/dia | Implementada |
|---|---|---|
| 0 a 6 meses | 9,1 | **sim** |
| 7 a 12 meses | 11 | **sim** |
| 13 a 36 meses | 13 | **sim** |
| 4 a 8 anos | 19 | não |
| 9 a 13 anos | 34 | não |
| 14 a 18 anos (feminino) | 46 | não |
| 14 a 18 anos (masculino) | 52 | não |

*Origem: `B14`, com a tabela em `M20:N27`. Referência: **DRIs, 2002** — AI para
0–6 meses, RDA nas demais faixas.*

### Por que as quatro últimas faixas não são implementadas

Os valores **estão na planilha**, mas nenhuma fórmula os referencia: `B14` só lê
`N21`, `N22` e `N23`, e devolve vazio acima de 36 meses. A instrução `A37`
confirma que foi deliberado.

Ficam registrados aqui e fora do código. Habilitá-los é decisão clínica de quem
responde pela prescrição — não algo a herdar de uma tabela que a autora
deliberadamente não ligou a nenhuma fórmula.

---

## 7. Dieta láctea prescrita

Todas as saídas dependem da frequência; as demais encadeiam a partir dela.

```
vezesDia       = 24 / frequenciaHoras                        exige frequencia > 0
volumeTotal    = vezesDia × volumePorTomada                  exige volume > 0        (ml/dia)
caloriasTotais = kcalPor100ml × volumeTotal / 100            exige fórmula           (kcal/dia)
proteinaTotal  = protPor100ml × volumeTotal / 100            exige fórmula           (g/dia)
percCalorico   = caloriasTotais / VET × 100                  exige VET > 0           (%)
percProteico   = proteinaTotal / necProteica × 100           exige necessidade > 0   (%)
```

*Origem: `E5` a `E10`.*

O `/100` aparece porque a composição da fórmula é declarada **por 100 ml**.

`vezesDia` pode ser fracionário: uma frequência que não divide 24 (5 horas, por
exemplo) resulta em 4,8 tomadas ao dia. A planilha não arredonda, e nós também
não — arredondar mudaria o volume total e, por consequência, a adequação.

### Composição das fórmulas lácteas *(tabela `M1:O12`)*

Semeadas como **fórmulas globais do sistema** na migration
`016-create-formula-lactea.xml`. Cada tenant pode cadastrar as suas, mas não
editar estas.

| Fórmula | kcal / 100 ml | Proteína g / 100 ml |
|---|---|---|
| NAN 1 | 74,2 | 1,38 |
| NAN 2 | 73,8 | 1,65 |
| NAN SL | 74,4 | 1,61 |
| NEOCATE LCD | 74,8 | 2,13 |
| NEOCATE ADV | 100 | 2,8 |
| FORTINI | 150 | 3,34 |
| INFATRINE | 100,8 | 2,6 |
| PREGOMIN | 66 | 1,8 |
| NESTOGENO 1 | 67 | 1,4 |
| NESTOGENO 2 | 67 | 1,5 |

Na planilha a fórmula é **texto digitado à mão**, casado por `VLOOKUP` exato:
qualquer diferença de grafia cai no `IFERROR` e zera o cálculo da dieta em
silêncio. Aqui a fórmula é uma FK escolhida em combo — o erro deixa de existir.

### 7.1 O que o cadastro de fórmula láctea recusa

A fórmula **enteral** declara os três macros e fecha por Atwater — é essa conta
que denuncia composição por embalagem ou fator 10 (`docs/10 §11`). A fórmula
**láctea** declara só energia e proteína: não há o que fechar, e por isso ela
passou muito tempo aceitando qualquer número, num campo que prescreve mamadeira.

Duas guardas substituem o fechamento. **Nenhuma delas é conformidade
regulatória** — são guardas de erro de digitação, calibradas para que todo
produto real passe.

**A faixa óbvia estava errada, e a pesquisa a derrubou.** A norma de fórmula
infantil põe a densidade entre 60 e 70 kcal/100 ml — e essa faixa recusaria
**três das dez fórmulas que o próprio sistema semeia**: FORTINI (150),
INFATRINE (100,8) e NEOCATE ADV (100). O catálogo não é de fórmula de partida:
é de tudo que a criança recebe por via oral ou sonda, e 1,5 kcal/ml é conduta,
não erro.

| Guarda | Faixa | O que pega | Procedência |
|---|---|---|---|
| Densidade energética | **20 a 250 kcal/100 ml** | a composição da **lata de pó** (≈ 500 kcal/100 g) lançada como reconstituída; o fator 10 para baixo; campos trocados | o produto enteral líquido mais denso descrito chega a 2,4 kcal/ml — acima de 1,5 a densidade só sobe engordando muito a fração lipídica, e passado isso não é líquido |
| Razão proteína/energia | **1,0 a 6,0 g/100 kcal** | o **ponto decimal deslocado** na proteína (um fator 10 cai perto de 21) | Codex Alimentarius **CXS 72-1981** fixa 1,8 a 3,0 para fórmula infantil; a faixa é alargada para caber produto pediátrico especializado, fora do escopo da norma |

**Por que a razão, e não a densidade, é a régua principal:** ela é *invariante de
escala*. Vale igual para a fórmula de partida a 67 kcal/100 ml e para o
suplemento a 150. As dez fórmulas semeadas caem **todas** dentro da faixa
estreita do Codex — de 1,86 (NAN 1) a 2,85 (NEOCATE LCD), **incluindo os três
hipercalóricos que a faixa de densidade teria reprovado**:

```text
NAN 1 1,86 · NESTOGENO 1 2,09 · NAN SL 2,16 · FORTINI 2,23
NAN 2 2,24 · NESTOGENO 2 2,24 · INFATRINE 2,58 · PREGOMIN 2,73
NEOCATE ADV 2,80 · NEOCATE LCD 2,85
```

**As duas guardas são necessárias, e nenhuma basta sozinha.** A composição do pó
tem razão proteica *correta* — só a densidade a pega. O fator 10 na proteína não
muda a densidade — só a razão o pega.

**Proteína zero passa.** Módulo puro de carboidrato existe em dieta metabólica
pediátrica; recusá-lo transformaria cadastro legítimo em cadastro impossível —
o mesmo critério que a fórmula enteral usa com macro ausente.

**A mensagem nomeia a causa provável**, não diz "valor inválido": *"500 kcal por
100 ml é densidade de pó, não de fórmula pronta … confira se o rótulo lido é o
da lata"*. Recusa que não ensina manda o usuário conferir dois campos no escuro.

`CatalogoPediatriaTest` trava as duas faixas contra o seed: **se alguém apertar a
guarda, o próprio catálogo do sistema reprova** — antes de reprovar o cadastro
da nutricionista.

---

## 8. Faixas de validade, reunidas

A planilha declara os próprios limites na célula `A37`:

> "O VET pode ser calculado automaticamente para crianças até 35 meses. A
> proteína pode ser calculada para crianças até 36 meses. O estado nutricional
> pode ser calculado para crianças até 60 meses."

| Bloco | Idade máxima |
|---|---|
| Estado nutricional (OMS) | 60 meses |
| VET | 35 meses |
| Necessidade proteica | 36 meses |

**O descompasso entre 35 e 36 é da fonte** e está preservado. Uma criança de 36
meses recebe necessidade proteica mas não VET. Uniformizar seria alterar a
prescrição por conta própria.

Consequência prática: entre 37 e 60 meses o sistema classifica o estado
nutricional mas não calcula necessidades. A tela deve dizer isso com todas as
letras — é informação clínica, não falha.

---

## 9. Caso de teste canônico

Conferido contra os valores em cache da própria planilha. **Este caso é
obrigatório na suíte** (`CalculoPediatricoTest`).

**Entrada:** sexo `F` · idade `8` meses · peso `9` kg · sem estatura ·
fórmula `NAN 2` (73,8 kcal e 1,65 g/100 ml) · volume `110` ml · frequência `3` h.

| Saída | Esperado | Conferência |
|---|---|---|
| IMC | ausente | sem estatura |
| Peso para a idade | `Peso adequado` | P15 = 7,0 · P85 = 9,0 → 9 ≥ 7 e **não** > 9 |
| Estatura para a idade | ausente | sem estatura |
| IMC para a idade | ausente | sem IMC |
| VET | `723` kcal/dia | (89 × 9 − 100) + 22 |
| Proteína | `11` g/dia | faixa 7 a 12 meses |
| Vezes ao dia | `8` | 24 / 3 |
| Volume total | `880` ml/dia | 8 × 110 |
| Calorias totais | `649,44` kcal/dia | 73,8 × 880 / 100 |
| % calórico | `89,83` % | 649,44 / 723 × 100 = 89,8257261410788, **arredondado a 2 casas** |
| Proteína total | `14,52` g/dia | 1,65 × 880 / 100 |
| % proteico | `132` % | 14,52 / 11 × 100 |

**Sobre a escala dos percentuais.** A planilha guarda o percentual com toda a
cauda de ponto flutuante; o sistema o devolve com **2 casas**, porque é assim que
a coluna é declarada (`NUMERIC(6,2)`, migration `017`) e porque §10 manda
arredondar com escala declarada. Os demais números da tabela saem com 4 casas
(`NUMERIC(12,4)`). Conferir a API contra a cauda inteira acusaria divergência
onde há só arredondamento — o valor exato fica registrado acima, na coluna da
conferência.

O caso de peso `9` contra P85 `9,0` não é coincidência: ele fixa a inclusividade
do P85 descrita na §4.1. Se a implementação trocar `>` por `>=`, este teste
quebra — que é exatamente o que se quer dele.

### Casos de borda a cobrir junto

| Caso | Esperado |
|---|---|
| Idade 60 meses | classifica |
| Idade 61 meses | sem estado nutricional, **com motivo** |
| Idade 35 meses | tem VET |
| Idade 36 meses | sem VET, **com** proteína |
| Idade 37 meses | sem VET e sem proteína |
| Valor exatamente em P15 | faixa adequada |
| Valor exatamente em P85 | faixa adequada |
| Frequência 5 h | 4,8 tomadas — não arredondar |
| Frequência 0 ou ausente | dieta inteira ausente, sem divisão por zero |

---

## 10. Implementação

`BigDecimal` do início ao fim, `RoundingMode.HALF_UP` com escala declarada.
Classe pura, `final`, construtor privado, sem Spring, sem acesso a banco —
[03-arquitetura-backend.md](03-arquitetura-backend.md) §8.

Cada saída ausente vem acompanhada do **motivo**, para a tela explicar em vez de
mostrar traço mudo — ver [04-arquitetura-frontend.md](04-arquitetura-frontend.md) §7.

**O cálculo é do servidor, sempre.** `POST /pediatria/avaliacoes/calcular` atende
à tela sem persistir; `POST`/`PUT` recalculam e gravam **o resultado do próprio
cálculo**, nunca o que o cliente enviou. No eroERP toda a matemática vivia no
browser e o backend gravava o que recebesse — um cliente adulterado gravava
`imc: 999` no prontuário.

### O que fica gravado

A avaliação guarda as entradas, os resultados **e um retrato da fórmula usada**
(nome, kcal e proteína por 100 ml). Editar ou desativar a fórmula depois não
altera avaliação nenhuma já feita, e abrir uma avaliação salva **não recalcula**:
mostra o que foi gravado. Prontuário registra o que se decidiu na hora, com os
dados que se tinha na hora.
