# Módulo de Pessoas

> Fatia 3 — **completa**. Porte do módulo `pessoa` do eroERP, onde o
> `cliente_id` de lá é o `tenant_id` daqui.
>
> Leia junto com [01-multitenant.md](01-multitenant.md) §6.1 e
> [07-banco-liquibase.md](07-banco-liquibase.md).

---

## 1. O que é uma pessoa aqui

Uma tabela só para **pessoa física e jurídica**, classificada por um ou mais
**tipos de cadastro**. Paciente não é uma tabela: é um tipo de cadastro de uma
pessoa. Quem hoje é responsável de um paciente pode amanhã ser paciente também,
sem recadastro e sem duplicar CPF.

| Tipo de cadastro | Para quê |
|---|---|
| Paciente | quem recebe a dieta |
| Responsável | acompanhante, familiar, responsável legal |
| Profissional de saúde | médico, nutricionista, enfermeiro citado no atendimento |
| Fornecedor | quem entrega fórmula e suplemento |
| Outros | o que não couber acima |

Os campos de PF e PJ convivem na mesma tabela, anuláveis, com a coerência
garantida no service. Duas tabelas quase iguais exigiriam pendurar a mesma
agenda de contatos em ambas — foi a decisão do eroERP e ela se sustenta aqui.

**A CNH não veio.** Número, categoria e validade existem no eroERP porque lá há
módulo de motorista. Em nutrição hospitalar não têm uso, e campo sem uso em
cadastro de saúde é dado pessoal guardado à toa.

---

## 2. Tabelas

| Tabela | Escopo | Conteúdo |
|---|---|---|
| `pessoa` | **tenant** | `nome`, `tipo_pessoa`, PF (`data_nascimento`, `cpf`, `rg`), PJ (`cnpj`, `inscricao_estadual`, `inscricao_municipal`, `nome_fantasia`, `razao_social`), `observacao`, `ativo`, `created_by`, `updated_by` |
| `pessoa_tipo_cadastro` | via pessoa | N:N, PK composta |
| `telefone` | **tenant** | `pessoa_id`, `tipo_telefone_id`, `codigo_pais`, `numero`, `observacao`, `principal` |
| `email` | **tenant** | `pessoa_id`, `tipo_email_id`, `email`, `observacao`, `principal` |
| `rede_social` | **tenant** | `pessoa_id`, `tipo_rede_social_id`, `usuario`, `url`, `observacao` |
| `endereco` | **tenant** | `pessoa_id`, `tipo_endereco_id`, `cidade_id`, `cep`, `rua`, `numero`, `bairro`, `complemento`, `principal` |
| `pessoa_vinculo` | **tenant** | `pessoa_origem_id`, `pessoa_destino_id`, `tipo`, `observacao` |
| `tipo_cadastro` · `tipo_telefone` · `tipo_email` · `tipo_rede_social` · `tipo_endereco` | **global** | `nome`, `ativo` — sem `tenant_id`, ver [01-multitenant §6.1](01-multitenant.md) |
| `estado` · `cidade` | **global** | 27 UFs e os municípios do IBGE, com `codigo_ibge` |

Constraints que importam:

```text
ck_pessoa_tipo             CHECK tipo_pessoa IN ('PESSOA_FISICA','PESSOA_JURIDICA')
uk_pessoa_tenant_cpf       UNIQUE (tenant_id, cpf)  WHERE cpf  IS NOT NULL
uk_pessoa_tenant_cnpj      UNIQUE (tenant_id, cnpj) WHERE cnpj IS NOT NULL
idx_pessoa_tenant_nome     (tenant_id, nome)
uk_pessoa_vinculo_par      UNIQUE (tenant_id, pessoa_origem_id, pessoa_destino_id)
ck_pessoa_vinculo_distintas  CHECK pessoa_origem_id <> pessoa_destino_id
uk_cidade_codigo_ibge      UNIQUE (codigo_ibge)
```

**Unicidade de documento é por tenant.** Duas clínicas podem atender a mesma
pessoa, e cada uma tem o seu cadastro. É o oposto de `usuario.email`, único no
sistema inteiro por ser credencial de login (docs/02 §2).

**Índice único parcial.** Sem o `WHERE`, o índice carregaria à toa todo cadastro
sem documento — que é a maioria num pronto-socorro.

Telefone, e-mail e rede social carregam `tenant_id` **além** do `pessoa_id`. A
redundância é deliberada (01-multitenant §5): é o que permite recusar um contato
de outro cliente sem carregar a pessoa antes.

---

## 3. Regras de negócio

### 3.1 Documento

Chega com máscara ou sem; o service reduz a dígitos antes de qualquer coisa.
Sem isso, `529.982.247-25` e `52998224725` conviveriam como se fossem pessoas
diferentes, e o índice único não veria o duplicado.

| Documento | Validação |
|---|---|
| CPF, CNPJ | **dígito verificador**, algoritmo completo — 400 se falhar |
| RG | faixa de 6 a 10 caracteres (mantém o `X`) |
| Inscrição estadual e municipal | faixa de 5 a 12 dígitos |

RG, IE e IM não têm regra nacional — variam por estado e por município. Validar
tamanho pega o dedo escorregado sem recusar dado legítimo.

Já cadastrado no tenant → **409**. Formato inválido → **400**.

### 3.2 Coerência entre tipo e campos

Campo do tipo errado é **recusado**, não ignorado: CNPJ digitado numa pessoa
física quase sempre significa que quem preencheu escolheu o tipo errado, e
salvar em silêncio esconderia o engano.

No `PUT`, trocar de tipo **limpa os campos do tipo anterior**. Sem isso um CPF
ficaria pendurado numa pessoa jurídica, e o índice único ainda o consideraria
ocupado.

### 3.3 Contatos — a lista é o estado completo

O `PessoaCreateDto` e o `PessoaUpdateDto` levam as listas inteiras, e o
`ContatoService` as sincroniza:

- item **com `id`** → atualiza
- item **sem `id`** → cria
- item que **não veio** → é removido
- lista **ausente ou vazia** → apaga todos daquele tipo

É o contrato do eroERP, e é o que faz o formulário inteiro caber num único PUT.

Um `id` que não seja daquela pessoa naquele tenant devolve **404** — a busca é
no mapa dos contatos já carregados, e não no banco, justamente para que mandar
o id de outra pessoa não altere nada.

**Principal é exclusivo.** Dois marcados → 400. Nenhum marcado → o primeiro da
lista assume: pessoa sem telefone principal é pessoa que ninguém sabe como
chamar. Rede social não tem principal — o que identifica cada linha é o tipo.

### 3.4 Endereço — a cidade vem do IBGE

`estado` e `cidade` são semeadas na migration `011`: 27 UFs e os municípios do
IBGE, cada um com o seu `codigo_ibge`. O endereço aponta para `cidade_id`, e não
guarda o nome — "Porto Alegre" escrito de cinco jeitos inviabilizaria qualquer
relatório por região.

O seed veio do eroERP convertido. Lá as cidades apontam para um `estado_id`
numérico que aqui não existe, e a ligação é refeita pelo **próprio código do
IBGE**: os dois primeiros dígitos do código de um município são o código da UF,
então `codigo_ibge / 100000` devolve o estado. Um `INSERT … SELECT` resolve as
mais de cinco mil linhas de uma vez.

São **5.571 municípios**, um a mais que os 5.570 oficiais — a lista de origem
tem 142 registros em MT contra 141 do IBGE. Ficou como está: é a lista que o
eroERP usa em produção, uma linha a mais numa tabela de referência não afeta a
escolha do endereço, e corrigir às cegas correria o risco de apagar município
legítimo.

A busca (`GET /cidades/select`) é sempre no servidor, com `unaccent` e
`LIMIT 100`. `estadoId` restringe à UF, que é como se acha "Bom Jesus" — existe
em nove estados. O CEP é gravado só com dígitos, como os demais documentos.

### 3.5 Vínculo — uma linha serve os dois cadastros

Cadastrar "Maria é responsável de José" faz **José aparecer como dependente no
cadastro de Maria**, sem segunda linha e sem digitar de novo.

Para isso a orientação é **canônica**: `pessoa_origem` é sempre a de menor id, e
o `tipo` está gravado nessa orientação. Quem lê pelo lado do destino recebe o
`TipoVinculo.inverso()` — responsável ↔ dependente; cônjuge e familiar são
simétricos. Sem a orientação canônica, o mesmo par entraria duas vezes invertido
e a unique não veria a duplicata.

Consequências que valem saber:

- **Remover por um cadastro remove do outro.** É uma relação, não uma anotação
  particular.
- **Editar o tipo de um lado muda o que o outro vê** — o registro continua
  canônico porque o service grava o inverso quando a edição vem do destino.
- Reenviar o mesmo par **sem `id`** não é duplicata: o vínculo antigo sai e o
  novo entra, porque a lista é o estado completo. Duplicata é a mesma pessoa
  duas vezes no mesmo envio, e essa é recusada com 400.
- Vincular a pessoa a ela mesma → 400. Vincular a alguém de outro tenant → 404.

O `sincronizar` dá `flush()` entre as remoções e as inserções: trocar um vínculo
por outro no mesmo PUT, sem isso, pode ter a ordem invertida pelo Hibernate e
esbarrar na unique.

### 3.6 Inativar, nunca apagar

`PATCH /pessoas/{id}/ativo`. Cadastro de paciente entra em histórico clínico;
apagar destruiria o rastro de quem foi atendido.

---

## 4. Endpoints

Todos exigem apenas `isAuthenticated()` — **é módulo de negócio**, não área
administrativa: `ADMIN` e `USER` operam. O isolamento vem do tenant efetivo do
token, não da role.

| Método | Rota | Observação |
|---|---|---|
| `GET` | `/pessoas` | paginado; filtros `nome`, `documento`, `tipoPessoa`, `ativo`, `tipoCadastroId` |
| `GET` | `/pessoas/select` | ativas, com documento para desempatar homônimo |
| `GET` | `/pessoas/{id}` | traz os contatos junto |
| `POST` `PUT` | `/pessoas`, `/pessoas/{id}` | |
| `PATCH` | `/pessoas/{id}/ativo` | |
| `GET` | `/tipos-cadastro/select` e os outros quatro | catálogos, somente leitura |
| `GET` | `/estados/select`, `/cidades/select` | IBGE; a de cidade aceita `nome` e `estadoId` |

O filtro `documento` procura em CPF **e** CNPJ, com ou sem máscara: quem digita
não sabe de antemão qual dos dois a pessoa tem. O de `nome` também alcança o
nome fantasia, e usa `unaccent` — "jose" acha "José".

---

## 5. Telas

| Tela | Rota | Quem vê |
|---|---|---|
| Pessoas | `/pessoas` | todo autenticado |
| Nova / editar | `/pessoas/nova` · `/pessoas/:id` | todo autenticado |

**Lista.** Nome (com o fantasia embaixo), CPF/CNPJ formatado, tipos de cadastro
como selos, telefone principal e situação. Filtra por nome, documento, tipo de
pessoa, tipo de cadastro e situação — e já abre em "Ativa", que é o que se quer
ver 95% das vezes.

**Formulário.** O painel de identificação **troca de campos** conforme PF/PJ: o
eroERP mostra os dois painéis sempre, e aqui só aparece o do tipo escolhido —
menos ruído à beira do leito. Os campos do tipo não escolhido nem são enviados:
o backend os recusaria com 400 e a mensagem confundiria quem só trocou o tipo.

Tipos de cadastro são caixas de seleção (são cinco). Telefones, e-mails e redes
sociais são listas que se somam e se removem (`useFieldArray`), com rádio
"Principal" nas duas primeiras.

**Endereço não cabe em linha** — são sete campos. Cada um é um bloco com
borda dentro do painel, com grid interno que vira coluna única no celular. O
eroERP resolve com grid mais modal de edição; aqui o bloco tem o mesmo peso
visual sem a navegação a mais. A cidade usa `TCombo`, buscando no servidor.

**Vínculo cabe em linha:** `TCombo` de pessoa (com `ignorarId`, para não
oferecer a própria), select de tipo e observação. Ao lado do combo, um botão
abre o **cadastro rápido** (`PessoaRapidaModal`): nome, tipo, documento e
telefone, pelo mesmo `POST /pessoas` do cadastro completo — herda as
validações e não abre caminho paralelo de escrita. Ao salvar, a pessoa criada
já entra selecionada na linha que abriu o modal.

O `TModal` nasceu aqui e é genérico: fecha por Esc e por clique fora, trava a
rolagem de trás e, no celular, ocupa a tela — modal centralizado em 360px
vira caixa apertada com o teclado por cima.

Máscara de CPF, CNPJ e telefone é aplicada **enquanto se digita**, e desfeita
no envio — a API recebe e devolve só dígitos.

---

## 6. O que está coberto por teste

`PessoaTest` e `EnderecoVinculoTest` — 35 testes:

| Área | O que prova |
|---|---|
| Cadastro | PF completa com contatos; PJ com CNPJ; sem tipo de cadastro é 400 |
| Documento | dígito verificador; duplicado no tenant é 409; **o mesmo CPF em outro tenant passa**; campo do tipo errado é 400; troca de tipo limpa o anterior |
| Contatos | dois principais é 400; nenhum marcado promove o primeiro; a lista substitui o estado; lista ausente apaga |
| Isolamento | pessoa de outro tenant é 404, e não aparece na listagem |
| Acesso | `USER` cria e lê — é módulo de negócio |
| Consulta | `unaccent` no nome, documento parcial e com máscara, filtro por tipo de cadastro, inativar, select |
| Catálogos | vêm semeados e são iguais para todo cliente |
| IBGE | 27 UFs e mais de 5.500 municípios; busca ignora acento e a UF desempata nome repetido |
| Endereço | grava a cidade do IBGE e devolve a UF; dois principais é 400; cidade inexistente é 404; o ausente no update some |
| Vínculo | **a reciprocidade** (responsável de um lado, dependente do outro); cônjuge simétrico; remover de um lado remove do outro; editar pelo destino grava o inverso; consigo mesma, par repetido e cross-tenant recusados; troca no mesmo PUT não esbarra na unique |

---

## 7. Fora do escopo, de propósito

- **Busca de CEP** (ViaCEP e afins). O endereço é digitado e a cidade escolhida
  no combo. O sistema é usado à beira do leito, onde a rede falha — e um
  formulário que depende de serviço externo para ser preenchido trava justamente
  onde não pode. Entra se a cliente sentir falta.
- **Manutenção de cidade e estado.** São referência do IBGE, não cadastro do
  cliente: a API só expõe `/select`.
- **`audit_log` de operações de negócio.** Entra como fatia própria, agora que o
  cadastro está fechado.
