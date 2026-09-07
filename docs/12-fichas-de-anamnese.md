# Fichas de anamnese

> Fatia 12 — **completa**. Primeira tela do menu **Clínica** do eroERP trazida
> para cá.
>
> Leia junto com [01-multitenant.md](01-multitenant.md) §6 (catálogo híbrido) e
> [08-modulo-pessoas.md](08-modulo-pessoas.md) §3.3 (a lista é o estado completo).

---

## 1. O que esta fatia é, e o que ela não é

O sistema sabia calcular dieta e acompanhar evolução, mas não guardava **o que o
paciente contou**: alergia alimentar, intolerância, hábito, aversão, via de
alimentação, objetivo. Isso vivia em papel, fora do sistema, e sumia.

O eroERP tem um menu **Clínica** com sete telas — Consultas, Pacotes, Fichas de
Anamnese, Planos Alimentares, e os auxiliares Templates de Anamnese, Refeições e
Configuração de Consulta. A decisão foi trazer esse menu **uma tela por vez, sob
demanda**, começando pela ficha.

O corte é limpo porque a cadeia de lá é **Template → Ficha → Consulta**: a ficha
não depende de Consultas nem de Pacotes — são eles que a consomem. Trazer a ficha
sozinha não deixa ponta solta.

**Fora de escopo, de propósito:** Consultas, Pacotes, Planos Alimentares,
Refeições, faturamento, WhatsApp — e **emitente**, que não existe neste sistema e
não vai existir. Quem conduziu a anamnese é o **profissional**, uma pessoa com o
tipo de cadastro *Profissional de saúde*, como já fazem `AvaliacaoUti` e
`AvaliacaoPediatrica`.

---

## 2. A ficha é dirigida por modelo

Não há coluna de pergunta. Quem define o que se pergunta é o **modelo de ficha**,
e cada cliente monta os seus — sem migration por pergunta nova.

| Tabela | Escopo | Conteúdo |
|---|---|---|
| `modelo_ficha` | **híbrido** (`tenant_id` anulável) | `nome`, `descricao`, `ativo` |
| `campo_ficha` | via modelo | `modelo_id`, `secao`, `rotulo`, `tipo`, `opcoes`, `ordem`, `obrigatorio`, `ativo` |
| `ficha_anamnese` | **tenant** | `paciente_id`, `profissional_id`, `data_preenchimento`, `modelo_id`, `modelo_nome`, `observacao`, `created_by`, `updated_by` |
| `resposta_ficha` | **tenant** | `ficha_id`, `campo_id`, **o retrato**, `valor` |

Sete tipos de campo, os mesmos do eroERP: `TEXTO`, `TEXTO_LONGO`, `CHECKBOX`,
`DATA`, `NUMERO`, `OPCOES`, `MULTIPLAS_OPCOES`.

### 2.1 `campo_ficha` não tem `tenant_id`, e isso é decisão

O modelo dono **pode ser do sistema**, com `tenant` nulo. Um `tenant_id NOT NULL`
ali seria impossível de preencher, e um anulável seria uma segunda fonte de
verdade para a mesma resposta. O campo é sempre alcançado *através* do modelo, e
o modelo é a guarda de isolamento.

`resposta_ficha` **tem**: é dado clínico de paciente, a ficha dona sempre tem
tenant, e é a redundância deliberada de [docs/01 §5](01-multitenant.md).

---

## 3. O retrato da pergunta — o que sustenta a fatia

No eroERP, `resposta_anamnese` guarda só `campo_id` + `valor`, e a ficha é
desenhada lendo os campos **vivos** do template.

Consequência: **editar o rótulo de uma pergunta reescreve o que fichas antigas
parecem ter perguntado.** Trocar *"Consome álcool?"* por *"Consome álcool
diariamente?"* faz um "Sim" respondido há um ano passar a responder outra
pergunta. Desativar um campo faz a resposta sumir da tela sem sumir do banco.

Este projeto já pagou essa armadilha duas vezes — a composição da fórmula láctea
impressa ao lado de uma oferta calculada com a antiga, e a fórmula *alterada* no
catálogo, que engana mais que a removida. A regra do `CLAUDE.md` é: **retrato
gravado explica o número; catálogo de hoje explica o cálculo de hoje.**

Aqui é pior que nos módulos de cálculo, e por isso a regra é mais dura: uma
resposta de anamnese é **só texto**. Se o rótulo muda, **nada denuncia** — não há
número ao lado para não fechar.

**Portanto:** ao gravar, cada resposta copia da pergunta o `secao`, `rotulo`,
`tipo`, `opcoes`, `ordem` e `obrigatorio`. A ficha salva é desenhada, impressa e
exportada a partir das **suas próprias linhas** — nunca do modelo. O `campo_id`
continua gravado, mas só como rastro.

O `tipo` no retrato é o que mantém o `valor` legível: `valor` é `TEXT` para todo
tipo, e é o tipo gravado que diz se `"true"` é um sim/não, se `"2026-09-06"` é
data e se `["Leite","Ovo"]` é opção múltipla.

### 3.1 A consequência boa: apagar um modelo não esvazia prontuário

`ficha_anamnese.modelo_id` e `resposta_ficha.campo_id` são **anuláveis**, com
`ON DELETE SET NULL`. Some o modelo, a ficha continua legível, imprimível e
exportável, dizendo de qual modelo veio pelo `modelo_nome`.

Sem o retrato, excluir um modelo esvaziaria prontuários — e é por isso que no
eroERP essa exclusão é perigosa e aqui não é.

### 3.2 Dois avisos, e são conversas diferentes

| Aviso | O que diz |
|---|---|
| `modeloRemovido` | o modelo não existe mais, e esta ficha continua inteira |
| `modeloAlterado` | o modelo existe, mas **não é mais este** — alguma pergunta mudou de texto, de tipo, de opções, ou entrou e saiu |

O segundo engana mais, porque o combo continua mostrando o nome certo. A tela
desenha pelo retrato e oferece **"Ver as perguntas de hoje"** — que só acontece
se alguém pedir, e nunca sozinho.

`modeloAlterado` também olha o outro lado: pergunta **nova e ativa** que a ficha
não tem é alteração. Sem isso, acrescentar uma pergunta ao modelo passaria
despercebido e a ficha pareceria completa faltando um dado.

---

## 4. Visibilidade e edição do modelo

Quem decide tudo é uma coluna: `modelo_ficha.tenant_id`.

| | Modelo **do sistema** (`NULL`) | Modelo **do cliente** |
|---|---|---|
| Quem enxerga | **todo tenant** | **só o dono** |
| Editar · inativar · apagar | **não** — 404 | sim |
| Clonar | sim, e a cópia nasce do cliente | sim |
| De onde vem | seed da migration `030` | criado na tela |

**404 e não 403.** É o que `FormulaEnteralService.buscarEditavel` já faz: o par é
`buscarVisivel` (enxerga a do tenant **e** a global) e `buscarEditavel` (só a do
tenant). Responder 403 confirmaria a existência do registro a quem não pode
tocá-lo.

**Clonar é o que torna a imutabilidade suportável.** A cópia nasce com
`tenant_id` preenchido, com as mesmas perguntas, e a partir daí é do cliente. O
nome ganha o sufixo `(cópia)` até ficar livre — recusar com 409 seria correto e
inútil: quem clica em Clonar quer a cópia, não uma conversa sobre nomes.

Na tela, o modelo do sistema abre **somente leitura**: campos desabilitados, sem
Salvar, sem Inativar e sem Excluir, com o aviso e o botão Clonar. Na lista, o
selo *Do sistema* — sem ele, o usuário só descobriria a regra ao tentar salvar, e
um botão que existe e recusa é pior que um botão que não existe.

---

## 5. Os três modelos semeados

Todos de **nutrição**, `tenant_id NULL`, na migration `030`:

| Modelo | Perguntas | Seções |
|---|---|---|
| Anamnese nutricional adulto | 20 | Saúde · Hábitos alimentares · Objetivo |
| Anamnese nutricional pediátrica | 21 | Gestação e nascimento · Aleitamento e introdução alimentar · Hábitos atuais · Saúde · Objetivo |
| Anamnese nutricional hospitalar | 16 | Via de alimentação · Aceitação e sintomas · Restrições |

O eroERP semeia seis finalidades (Estética, Clínica Geral, Odontologia,
Podologia, Nutrição, Veterinária); cinco delas não têm uso aqui.

**O modelo adulto não pede peso nem altura**, ao contrário do template `NUTRICAO`
de lá: o sistema já os coleta na avaliação, e pedir a mesma medida em dois
lugares produz dois valores divergentes para o mesmo paciente, sem dizer qual
vale. Travado em `ModeloFichaTest.adultoNaoDuplicaMedida`.

**A pediátrica registra quem prestou as informações.** Numa anamnese pediátrica
quem responde quase nunca é o paciente, e isso qualifica todas as respostas
acima.

### 5.1 Nenhum instrumento pontuado entra no seed

NRS-2002, MUST, MNA-SF e STRONGkids são escalas com pontos de corte e validação
publicada — o mesmo tipo de conhecimento que `docs/09` e `docs/10` exigiram
documentar antes de virar código. Se a cliente pedir, entram como fatia própria,
com fonte citada.

Um questionário **descritivo** não tem esse problema: ele registra o que foi
dito, não calcula um escore. É o que justifica semear estes três sem levantar
bibliografia.

---

## 6. Não há coluna de finalidade

No eroERP a ficha escolhe o template por finalidade, e o servidor pega o
**primeiro ativo** daquela finalidade (`templates.get(0)`) — indeterminado quando
há dois. Aqui tudo é nutrição, a coluna seria constante, e o profissional
**escolhe o modelo explicitamente** num combo. O defeito deixa de ser
representável.

---

## 7. Regras que o service impõe

1. Modelo do sistema não se edita, não se inativa e não se apaga — **404**.
2. **Campo obrigatório sem resposta → 400, nomeando o rótulo.** "Campo
   obrigatório" numa ficha de vinte perguntas manda o profissional procurar qual.
3. Valor fora das opções declaradas → 400. Sim/não que não seja `true`/`false` →
   400. Número que não é número, data que não é data → 400.
4. **A lista de perguntas é do servidor**, e o cliente só manda `campoId` e
   `valor`. Aceitar rótulo do front abriria caminho para uma ficha afirmar ter
   perguntado o que o modelo não perguntava.
5. Id de pergunta que não é do modelo → **404**, e não silêncio.
6. O retrato é copiado no `POST` e no `PUT`. Regravar atualiza o retrato — é o
   mesmo encontro sendo corrigido. O que nunca acontece é o retrato mudar **sem**
   alguém regravar.
7. Trocar o modelo da ficha descarta as respostas antigas e recomeça; a tela
   avisa antes.
8. `data_preenchimento` é `@PastOrPresent`, e o front manda `hojeIso()`.
9. Modelo sem pergunta nenhuma → 400. Pergunta de opção com menos de duas opções
   → 400. Opção pendurada em campo de texto → 400. Pergunta repetida na mesma
   seção → 400.
10. **A ordem é a posição na lista**, reatribuída pelo servidor — ordem digitada à
    mão produz duas perguntas com o número 3.

---

## 8. Telas

| Tela | Rota | Menu |
|---|---|---|
| Fichas de anamnese | `/app/clinica/fichas` | **Clínica** |
| Nova / editar ficha | `/app/clinica/fichas/nova` · `/:id` | |
| Modelos de ficha | `/app/clinica/modelos-ficha` | **Cadastros auxiliares** |
| Novo / editar modelo | `/app/clinica/modelos-ficha/novo` · `/:id` | |

O menu **Cadastros auxiliares** nasce com um item só e recebe os auxiliares das
próximas telas de Clínica (Refeições, Configuração de consulta) conforme elas
chegarem — cada uma traz o seu.

**Dois componentes nasceram aqui**, da lista *"a criar quando a fatia pedir"* de
[docs/04 §4](04-arquitetura-frontend.md): `TCheckBox` e `TRadio`.

### 8.1 O sim/não tem três estados

Uma caixa desmarcada não distingue *"o paciente disse que não"* de *"ninguém
perguntou"* — e num prontuário essa é a diferença que importa. Por isso
`CHECKBOX` é um `TRadio` de **Sim · Não · Não informado**, e não uma caixa de
seleção. É a mesma família do traço mudo: ausência não é valor.

### 8.2 A folha impressa

`DocumentoFichaAnamnese` monta uma `Folha` com uma `Secao` por seção da ficha, e
**pergunta sem resposta não é omitida**: um prontuário que esconde o que não foi
perguntado mente por omissão.

`LinhasDeValor` ganhou a variante **`prosa`**. A coluna de valor nasceu para
"72,5 kg" — 22 % da largura, alinhada à direita —, e numa ficha de anamnese, onde
toda resposta é texto, isso quebrava *"Amendoim e frutos do mar"* em duas linhas e
deixava um terço da folha vazio. **Só apareceu no PDF**: `build`, `lint` e os 424
testes passavam.

---

## 9. O que está coberto por teste

`ModeloFichaTest` e `FichaAnamneseTest` — 42 testes:

| Área | O que prova |
|---|---|
| Carga da migration | os três entram, todos do sistema; toda pergunta de opção tem opções; o adulto não pede peso nem altura; os três aparecem para os dois tenants |
| Imutabilidade | editar, inativar e apagar um do sistema é 404 — um teste por verbo; ler funciona e a resposta diz `doSistema` |
| Clonar | a cópia é do tenant, tem as mesmas perguntas e aceita ser alterada; dois clones não esbarram na unique; clonar o de outro tenant é 404 |
| Isolamento | o modelo de um tenant não aparece para o outro na listagem, no `/select` nem por id; o mesmo nome convive em dois clientes e colide dentro de um |
| Perguntas | a ordem é reatribuída; a lista é o estado completo; opção sem opções, opção em campo de texto, pergunta repetida e modelo vazio são 400; id de pergunta alheia é 404 |
| Gravar e reabrir | `USER` opera; o retrato completo volta na resposta; sim/não em branco reabre em branco; a lista conta respondidas de total |
| **Retrato** | **editar o rótulo no modelo não muda o que a ficha antiga perguntou**; desativar a pergunta não some com a resposta; pergunta nova conta como alterado; **apagar o modelo não esvazia a ficha**; regravar atualiza o retrato; trocar o modelo recomeça |
| Recusas | obrigatória sem resposta nomeia a pergunta; opção fora da lista; sim/não com texto; data futura; pergunta de outro modelo |
| Isolamento da ficha | paciente, ficha e modelo de outro tenant são 404; apagar a alheia é 404 e a própria some |

**O teste do retrato foi verificado ao contrário.** Reintroduzindo o defeito do
eroERP — desenhar a partir do campo vivo —, `rotuloAntigoPermanece` ficou
vermelho com exatamente o sintoma de lá:

```
JSON path "$.respostas[0].rotulo"
  expected:<Tem alergia alimentar?> but was:<Tem alergia alimentar GRAVE?>
```

Um teste que passa antes e depois da correção não trava nada.

---

## 10. O que falta do menu Clínica

Entram **sob demanda**, uma por vez. A ordem de dependência de lá:

| Tela | Depende de |
|---|---|
| Planos alimentares | cadastro de Refeições (auxiliar) |
| Consultas | Configuração de consulta, produtos/serviços, financeiro |
| Pacotes | Consultas, contas a receber |

Consultas e Pacotes trazem faturamento e estoque atrás de si — não são "mais uma
tela", são um módulo financeiro. Planos alimentares é o próximo corte limpo, se a
cliente pedir.
