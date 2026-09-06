# Acompanhamento diário pediátrico

Especificação da fatia 10. Mesmo rigor de [docs/09](09-calculos-pediatria.md) e
[docs/10](10-calculos-uti-adulto.md): cada número tem origem, unidade e caso de
teste.

---

## 1. O que esta fatia é, e o que ela não é

A pediatria tinha **avaliações soltas**: cada uma um retrato, nenhuma linha entre
elas. A UTI adulto já tinha o dia de acompanhamento e, com ele, o terceiro
painel. Esta fatia dá à pediatria a mesma coisa — **o eixo do tempo**.

**E é só isso: nutrição no tempo.** O `RegistroDiarioUti` tem gasometria, FiO₂,
suporte ventilatório, lactato e PCR, porque a `AvaliacaoUti` tem fase da terapia
crítica, terapia renal e noradrenalina — é um módulo de UTI. A
`AvaliacaoPediatrica` **não tem um único campo de terapia intensiva**: é peso,
estatura, percentil da OMS, VET, DRI e fórmula láctea. Copiar os 29 campos de lá
transformaria a pediatria em módulo de UTI pediátrica por efeito colateral de uma
decisão sobre simetria — e exigiria levantar toda faixa de referência pediátrica
por idade (AAP 2017 para pressão, PALS para hipotensão, laboratório por faixa
etária), que é fatia própria, com fonte própria.

**Simetria aqui é de propósito, não de colunas.**

> Se um dia o uso em UTI pediátrica se confirmar, os campos clínicos entram numa
> fatia à parte, com o documento que eles exigem. A entidade desta fatia não
> impede isso — só não finge que já aconteceu.

## 2. A régua já está no sistema

O que dá valor a esta fatia **não precisa de fonte nova**, e é o ponto que a
distingue: as réguas todas já foram levantadas, conferidas e testadas.

| O que se lê | De onde vem | Já no sistema |
|---|---|---|
| Percentis de peso, estatura e IMC por idade e sexo | OMS, 0 a 60 meses | migration `015`, 61 linhas por sexo, âncoras travadas em teste |
| Regra das três faixas (P15/P85, com a assimetria do `>`) | `docs/09 §4.1` | `CalculoPediatricoCalculator.faixa()` |
| VET por faixa etária | DRIs 2002, `docs/09 §5` | `deposicaoEnergetica` · até 35 meses |
| Necessidade proteica | DRIs 2002, `docs/09 §6` | `necessidadeProteica` · até 36 meses |
| Composição da fórmula láctea, por 100 ml | catálogo, `docs/09 §7` | `FormulaLactea` |

**Nenhuma constante clínica nova entra nesta fatia.** É deliberado: onde a
pediatria precisaria de número que a `Pediatria.xlsx` não traz — velocidade
esperada de ganho de peso em g/dia, percentil de circunferência do braço
(Frisancho), Holliday-Segar — a fatia **não vai**, e esses itens seguem no
backlog com a etiqueta de fonte a levantar.

## 3. A diferença que só existe na pediatria: a idade anda

Na UTI adulto a idade é entrada da avaliação e não muda entre os dias. **Na
pediatria ela é o eixo X das curvas**, e 30 dias de internação de um lactente
atravessam uma linha inteira da tabela da OMS.

Então **cada registro calcula a própria idade**, da data de nascimento do
paciente e da data do registro:

```
idadeMeses(registro) = Period.between(paciente.dataNascimento, registro.data).toTotalMonths()
```

É o mesmo cálculo que `PediatriaDashboardService.idadeAtual` já faz, e a mesma
razão: idade copiada da avaliação envelheceria calada. **Sem data de nascimento
não há idade**, e sem idade não há classificação — com o motivo escrito, nunca
traço mudo.

⚠️ **Consequência a não esquecer:** o registro de uma criança que passa dos 60
meses durante o acompanhamento **deixa de classificar**, e a série ganha um ponto
sem faixa. Isso é informação, não falha, e a tela diz.

## 4. Entradas

O que foi **medido e ofertado** naquele dia. Nada aqui é derivado.

| Campo | Unidade | Obrigatório | Observação |
|---|---|---|---|
| `pessoa` | — | sim | o paciente |
| `avaliacao` | — | **não** | a avaliação que estava valendo. Dá VET, DRI e o retrato da fórmula |
| `data` | dia do calendário | sim | um registro por paciente por dia |
| `pesoKg` | kg | não | a medida do dia, não a da avaliação |
| `estaturaCm` | cm | não | muda devagar; medir todo dia não é esperado |
| `volPrescrito24h` | ml | não | o que foi prescrito |
| `volRecebido24h` | ml | não | o que a criança de fato recebeu |
| `tomadasPrevistas` | contagem | não | quantas tomadas o plano previa |
| `tomadasAceitas` | contagem | não | quantas foram aceitas |
| `observacao` | texto | não | |

**Por que o vínculo com a avaliação é opcional:** criança que internou de
madrugada tem dia antes de avaliação, e exigir o vínculo travaria o uso à beira
do leito — a mesma razão de `RegistroDiarioUti`. Quando existe, é ele que dá as
metas e a composição da fórmula.

**Por que `ON DELETE RESTRICT` na avaliação:** apagar uma avaliação com dias
vinculados é recusado, e o service diz quantos. Um cascade levaria o
acompanhamento embora sem avisar — decisão idêntica à da UTI.

## 5. Derivados — calculados na leitura, nunca colunas

Guardar o que se pode calcular é criar duas versões que um dia divergem. No
eroERP o `% recebido` é campo digitável ao lado de um calculado, e os dois vão
para o banco.

| Derivado | Fórmula | Depende de |
|---|---|---|
| `idadeMeses` | §3 | data de nascimento |
| `prescritoDeReferencia` | o da avaliação quando positivo, senão o digitado | as duas colunas |
| `percentualRecebido` | `recebido / prescritoDeReferencia × 100` | o campo acima, e nenhum outro |
| `caloriasRecebidas` | `volRecebido × kcalPor100ml / 100` | fórmula da avaliação |
| `proteinaRecebida` | `volRecebido × proteinaPor100ml / 100` | idem |
| `caloriasPorKg` | `caloriasRecebidas / peso` | peso do dia |
| `proteinaPorKg` | `proteinaRecebida / peso` | idem |
| `adequacaoCalorica` | `caloriasRecebidas / VET × 100` | VET da avaliação |
| `adequacaoProteica` | `proteinaRecebida / proteinaNecessidade × 100` | DRI da avaliação |
| `aceitacaoTomadas` | `tomadasAceitas / tomadasPrevistas × 100` | as duas contagens |
| `imc` | `peso / (estatura/100)²` | peso e estatura do dia |
| `classifPesoIdade` · `classifEstaturaIdade` · `classifImcIdade` | regra das três faixas, `docs/09 §4.1` | idade, sexo e a linha da OMS |

**O prescrito preferido é o da avaliação**, não o digitado no dia: é o que estava
de fato prescrito, e não depende de alguém repetir o número certo. Sem avaliação
vinculada, cai no digitado — e a tela diz contra o quê comparou. Mesma regra de
`AcompanhamentoCalculator.prescritoDeReferencia`, na UTI — escrita duas vezes de
propósito, porque os módulos não se importam (`§5.1`).

**O número escolhido é publicado**, e não só a procedência. Enquanto ele ficava
dentro do cálculo, quem exibisse prescrito ao lado de percentual tinha de
adivinhar a regra — e sete telas adivinharam errado, mostrando o digitado ao
lado de uma conta feita com outro número. Quem mostra o par mostra este campo.

### 5.1 O que se reusa, e o que não dá para reusar

`AcompanhamentoCalculator` da UTI é quase todo idade-agnóstico e é reaproveitado:
`percentualRecebido` e a ideia de derivar na leitura. **Duas coisas não passam:**

- **`caloriasRecebidas` e `proteinaRecebida` da UTI dividem por 1000**, porque o
  catálogo enteral é declarado **por litro** (`docs/10`, defeito 1). A fórmula
  láctea é **por 100 ml** (`docs/09 §7`) — divide por **100**. Reusar a função da
  UTI daria **dez vezes** o valor, num número que vira adequação calórica.
- **`diuresePorQuiloHora` e `mediaIngestaoOral` não se aplicam**: diurese é
  variável de UTI, e "seis refeições" não é como um lactente em fórmula come — a
  unidade é a **tomada**, como `docs/09 §7` já modela a dieta.

### 5.2 Escala e arredondamento

Como em `docs/09 §10`: `HALF_UP`, escala declarada. **4 casas** para os valores
(`NUMERIC(12,4)`), **2 casas** para percentuais (`NUMERIC(6,2)`) — e as colunas
das entradas seguem a mesma escala das da avaliação, para que peso gravado aqui e
lá sejam comparáveis sem conversão.

## 6. Faixas de validade

Herdadas de `docs/09 §8`, e a tela precisa dizê-las com todas as letras:

| Bloco | Idade máxima | O que acontece depois |
|---|---|---|
| Classificação pela OMS | 60 meses | ponto na série **sem faixa**, com motivo |
| Adequação calórica | 35 meses (VET) | adequação ausente, com motivo |
| Adequação proteica | 36 meses (DRI) | idem |

**O descompasso 35/36 é da fonte e está preservado** — uniformizar seria alterar
prescrição por conta própria.

## 7. Caso de teste canônico

Mesma criança do caso canônico de `docs/09 §9`, agora com um dia de
acompanhamento. **F, nascida de forma que no dia do registro tenha 8 meses.**

**Avaliação vinculada:** peso 9 kg · sem estatura · NAN 2 (73,8 kcal e
1,65 g/100 ml) · 110 ml a cada 3 h → VET `723` kcal/dia · proteína `11` g/dia ·
volume total `880` ml/dia.

**Registro do dia:** peso `9,2` kg · volume recebido `800` ml · 8 tomadas
previstas · 7 aceitas.

| Derivado | Esperado | Conferência |
|---|---|---|
| Idade no dia | `8` meses | do nascimento à data do registro |
| % recebido | `90,91` % | 800 / **880** × 100 — o prescrito da **avaliação** |
| Calorias recebidas | `590,4` kcal | 73,8 × 800 / 100 |
| Proteína recebida | `13,2` g | 1,65 × 800 / 100 |
| kcal/kg | `64,1739` | 590,4 / 9,2 |
| g/kg | `1,4348` | 13,2 / 9,2 |
| Adequação calórica | `81,66` % | 590,4 / 723 × 100 |
| Adequação proteica | `120` % | 13,2 / 11 × 100 |
| Aceitação das tomadas | `87,5` % | 7 / 8 × 100 |
| Peso para a idade | `Acima do peso` | 9,2 contra P15 7,0 e P85 9,0 → passa do P85 |

⚠️ **Atenção à última linha:** 9,2 kg **passa** do P85 de 9,0, então a
classificação é `Acima do peso` — e não `adequada`, como na avaliação de 9,0 kg
exatos. **É o caso que prova que o registro classifica com o peso DO DIA**, e não
repete a classificação gravada na avaliação. Se este teste devolver "adequado", o
registro está copiando a avaliação em vez de calcular.

### Casos de borda a cobrir junto

| Caso | Esperado |
|---|---|
| Sem avaliação vinculada | % recebido cai no prescrito digitado, e a tela diz contra o quê comparou |
| Sem avaliação e sem prescrito digitado | % recebido ausente, com motivo |
| Sem data de nascimento | sem idade, sem classificação, **com motivo** |
| Criança que passa de 60 meses no meio da série | ponto sem faixa, com motivo |
| Idade entre 36 e 60 meses | classifica, **sem** adequação calórica nem proteica |
| Sem peso no dia | sem kcal/kg nem g/kg, com motivo; % recebido continua saindo |
| Volume recebido sem fórmula na avaliação | sem calorias nem proteína, com motivo |
| Dois registros do mesmo paciente no mesmo dia | recusado — 409 |
| `tomadasAceitas` > `tomadasPrevistas` | recusado na validação: aceitar mais do que se previu é erro de digitação |

## 8. Implementação

Segue `docs/03` e o molde de `RegistroDiarioUti`, com uma diferença de pacote:
o calculador vive em **`pediatria/calculo/`**, para reusar `faixa()` de
`CalculoPediatricoCalculator`, que é package-private — a régua da OMS é a mesma,
e uma segunda cópia dela divergiria.

- **Cálculo no servidor.** A tela não recalcula nada.
- **Classe pura** para os derivados: sem estado, sem Spring, sem banco. A linha
  da OMS chega resolvida, como em `CalculoPediatricoCalculator`.
- **Motivo em toda ausência.** Todo caminho que não produz valor produz motivo —
  a lição do `else if` sem `else`, registrada no `CLAUDE.md`.
- **Isolamento por tenant** em toda consulta, `findByIdAndTenantId`.
- **Nada de dado clínico em log**: só id e tenant.

### O que fica gravado

As **entradas**, e só elas. Nenhum derivado é coluna — todos saem na leitura,
sobre a avaliação vinculada e a tabela da OMS. É o oposto da avaliação, que grava
os resultados: lá o número é **prescrição registrada** e não pode mudar quando a
régua mudar; aqui é **leitura de acompanhamento**, e recalcular sobre a régua de
hoje é o correto.
