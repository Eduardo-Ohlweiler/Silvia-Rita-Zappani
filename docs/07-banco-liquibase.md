# Banco de Dados — PostgreSQL 16 + Liquibase XML

> Leia antes de criar entidade, tabela, índice, constraint, foreign key ou
> migration.

---

## 1. Regras de base

- **PostgreSQL 16.** Sem abstração para outro banco.
- **Liquibase XML.** Nada de Flyway, nada de SQL solto, nada de YAML/JSON.
- **`ddl-auto: none`.** O schema é do Liquibase, não do Hibernate.
- **Nunca alterar banco manualmente.** Nem em desenvolvimento — o que não está no
  changelog não existe.
- **Nunca editar changeSet já executado.** O checksum falha e a migration quebra
  em quem já rodou. Correção é changeSet novo.

---

## 2. Estrutura de changelogs

```
src/main/resources/db/changelog/
├── changelog-master.xml
└── changes/
    ├── 001-enable-extensions.xml
    ├── 002-create-tenant.xml
    ├── 003-create-usuario.xml
    ├── 004-create-login-log.xml
    ├── 005-create-refresh-token.xml
    ├── 006-create-paciente.xml
    └── ...
```

Nomenclatura: `NNN-verbo-alvo.xml`, sequencial, três dígitos, kebab-case.
Verbos: `create`, `add`, `alter`, `drop`, `insert`, `enable`.

```xml
<!-- changelog-master.xml -->
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <include file="db/changelog/changes/001-enable-extensions.xml"/>
    <include file="db/changelog/changes/002-create-tenant.xml"/>
    <!-- ... -->
</databaseChangeLog>
```

### ChangeSet

```xml
<changeSet id="009-create-paciente" author="eduardo">
    ...
    <rollback>
        <dropTable tableName="paciente"/>
    </rollback>
</changeSet>
```

- `id` único e igual ao nome do arquivo (`NNN-descricao`)
- `author` obrigatório
- `rollback` sempre que não for inferido automaticamente (`sql`, `insert`, `update`)
- Um assunto por changeSet. Vários changeSets por arquivo é aceitável quando são
  do mesmo assunto (tabela + índices + uniques).

---

## 3. Primeira migration — extensões

```xml
<changeSet id="001-enable-extensions" author="eduardo">
    <sql>CREATE EXTENSION IF NOT EXISTS "pgcrypto";</sql>
    <sql>CREATE EXTENSION IF NOT EXISTS "unaccent";</sql>
    <rollback>
        <sql>DROP EXTENSION IF EXISTS "unaccent";</sql>
        <sql>DROP EXTENSION IF EXISTS "pgcrypto";</sql>
    </rollback>
</changeSet>
```

`pgcrypto` fornece `gen_random_uuid()`; `unaccent` viabiliza busca sem acento —
essencial num sistema em português.

---

## 4. Anatomia de uma tabela de negócio

```xml
<changeSet id="009-create-paciente" author="eduardo">

    <createTable tableName="paciente">

        <!-- PK -->
        <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
            <constraints primaryKey="true" primaryKeyName="pk_paciente"/>
        </column>

        <!-- TENANT — obrigatório em toda tabela de negócio -->
        <column name="tenant_id" type="UUID">
            <constraints nullable="false"
                         foreignKeyName="fk_paciente_tenant"
                         references="tenant(id)"/>
        </column>

        <!-- Negócio -->
        <column name="nome" type="VARCHAR(255)">
            <constraints nullable="false"/>
        </column>
        <column name="data_nascimento" type="DATE"/>
        <column name="sexo" type="VARCHAR(20)">
            <constraints nullable="false"/>
        </column>
        <column name="cpf" type="VARCHAR(14)"/>
        <column name="peso_atual" type="NUMERIC(8,3)"/>
        <column name="ativo" type="BOOLEAN" defaultValueBoolean="true">
            <constraints nullable="false"/>
        </column>

        <!-- Auditoria -->
        <column name="created_by" type="UUID">
            <constraints foreignKeyName="fk_paciente_created_by" references="usuario(id)"/>
        </column>
        <column name="updated_by" type="UUID">
            <constraints foreignKeyName="fk_paciente_updated_by" references="usuario(id)"/>
        </column>
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"/>
    </createTable>

    <!-- Índice do tenant — obrigatório -->
    <createIndex tableName="paciente" indexName="idx_paciente_tenant_id">
        <column name="tenant_id"/>
    </createIndex>

    <!-- Índice composto de busca — tenant SEMPRE primeiro -->
    <createIndex tableName="paciente" indexName="idx_paciente_tenant_nome">
        <column name="tenant_id"/>
        <column name="nome"/>
    </createIndex>

    <!-- Unique SEMPRE composta com o tenant -->
    <addUniqueConstraint tableName="paciente"
                         columnNames="tenant_id, cpf"
                         constraintName="uk_paciente_tenant_cpf"/>

    <!-- Enum validado também no banco -->
    <sql>
        ALTER TABLE paciente ADD CONSTRAINT ck_paciente_sexo
        CHECK (sexo IN ('MASCULINO','FEMININO'));
    </sql>

    <rollback>
        <dropTable tableName="paciente"/>
    </rollback>
</changeSet>
```

Esse é o gabarito. Toda tabela de negócio tem: PK UUID com default no banco,
`tenant_id` com FK, colunas de negócio, auditoria completa, índice de tenant,
índices compostos de busca, uniques compostas e checks de enum.

---

## 5. Chave primária

```xml
<column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
    <constraints primaryKey="true" primaryKeyName="pk_paciente"/>
</column>
```

O `defaultValueComputed` vai **desde a primeira migration**, mesmo com o Hibernate
usando `GenerationType.UUID`. Assim o banco funciona sozinho em seed, script de
manutenção e carga direta.

---

## 6. Multi-tenant no banco

| Regra | Como |
|---|---|
| Coluna | `tenant_id UUID NOT NULL` em toda tabela de negócio |
| FK | `fk_<tabela>_tenant` referenciando `tenant(id)` |
| Índice | `idx_<tabela>_tenant_id` — **obrigatório**, é o filtro de toda query |
| Unique | sempre composta: `uk_<tabela>_tenant_<campo>` (exceto credencial de login — ver abaixo) |
| Índice de busca | `tenant_id` como **primeira** coluna |

Tabelas **sem** `tenant_id`: `tenant`, `role`, `databasechangelog`,
`databasechangeloglock`.

Tabelas com `tenant_id` **nullable** (catálogo híbrido — `NULL` = global):
`formula_enteral`, `suplemento`. Nesses casos o PostgreSQL trata `NULL` como
distinto em unique, então o catálogo global e o de cada tenant convivem com o
mesmo nome.

### Por que a unique é sempre composta

Dois tenants precisam poder ter o mesmo CPF de paciente, o mesmo nome de fórmula,
o mesmo código de leito. Unique global em campo de negócio faz um cliente
atrapalhar o outro.

### A exceção: credencial de login

`usuario.email` é **único globalmente** (`uk_usuario_email`). Não é campo de
negócio, é a credencial de autenticação: o login acontece antes de o tenant ser
conhecido e busca só por e-mail. Com o mesmo e-mail em dois tenants,
`findByEmailIgnoreCase` encontraria duas linhas e o login quebraria.

A regra vale para campo de identidade/credencial, não abre precedente para campo
de negócio. Ver a migration `006-usuario-email-unico-global.xml`, que documenta o
raciocínio no próprio changeSet.

---

## 7. Tipos de coluna

| Grandeza | Tipo | Exemplo |
|---|---|---|
| Chave e FK | `UUID` | `id`, `tenant_id`, `paciente_id` |
| Texto curto | `VARCHAR(n)` — **sempre com tamanho** | `VARCHAR(255)` |
| Texto longo | `TEXT` | observação, evolução |
| Medida antropométrica (cm, kg) | `NUMERIC(8,3)` | `peso_atual`, `circunferencia_braco` |
| Resultado de cálculo (kcal, g, mL) | `NUMERIC(12,4)` | `valor_calorico_total` |
| Percentual | `NUMERIC(6,2)` | `percentual_adequacao` |
| Contagem | `INTEGER` | `numero_horas` |
| Booleano | `BOOLEAN` com `defaultValueBoolean` | `ativo` |
| Enum | `VARCHAR(n)` + `CHECK` | `sexo`, `tipo_logout` |
| Data | `DATE` | `data_nascimento` |
| Data e hora | `TIMESTAMP WITH TIME ZONE` | `created_at`, `data_login` |
| IP | `VARCHAR(45)` | `endereco_ip` (cabe IPv6) |
| JSON | `JSONB` | só quando o dado é realmente sem esquema |

**Nunca** `DOUBLE PRECISION`, `REAL` ou `FLOAT` em medida, dosagem ou valor
calórico. O sistema prescreve dieta; erro de ponto flutuante aqui tem consequência
clínica. Sempre `NUMERIC`.

**Sempre `TIMESTAMP WITH TIME ZONE`**, nunca `TIMESTAMP` puro. O `Instant` do Java
mapeia para `timestamptz` e o fuso fica explícito.

**Enum como `VARCHAR` + `CHECK`**, nunca tipo `ENUM` do PostgreSQL (alterar exige
DDL desconfortável) e nunca `SMALLINT` ordinal (renomear um valor no Java
silenciosamente corrompe os dados).

---

## 8. Nomenclatura

`snake_case` em tudo. Tabela no **singular**.

```
paciente · atendimento · avaliacao_antropometrica · formula_enteral · login_log
```

Não: `pacientes`, `Paciente`, `tbPaciente`.

Tabela de junção N:N: `<a>_<b>`, em ordem alfabética ou pela relação natural.

```
atendimento_suplemento · prescricao_formula
```

O acesso **não** tem tabela de junção: o nível é um enum em `usuario.role`, com
`CHECK` — a decisão de simplificar está no doc 02.

### Convenções de nome de objeto

| Objeto | Padrão | Exemplo |
|---|---|---|
| Primary key | `pk_<tabela>` | `pk_paciente` |
| Foreign key | `fk_<tabela>_<campo sem _id>` | `fk_paciente_tenant` |
| Índice | `idx_<tabela>_<campos>` | `idx_paciente_tenant_nome` |
| Unique | `uk_<tabela>_<campos>` | `uk_paciente_tenant_cpf` |
| Check | `ck_<tabela>_<campo>` | `ck_paciente_sexo` |

**Toda** constraint e **todo** índice têm nome explícito. Nome gerado pelo banco
é impossível de referenciar numa migration futura.

---

## 9. Índices

Criar índice para:

1. `tenant_id` — sempre, em toda tabela de negócio
2. Toda FK — o PostgreSQL **não** indexa FK automaticamente, e sem índice o
   `DELETE` no pai faz varredura sequencial no filho
3. Campo usado em filtro de listagem — composto, com `tenant_id` primeiro
4. Campo de ordenação padrão da listagem

```xml
<createIndex tableName="atendimento" indexName="idx_atendimento_tenant_data">
    <column name="tenant_id"/>
    <column name="data_atendimento" descending="true"/>
</createIndex>
```

Busca textual sem acento, quando a listagem exigir:

```xml
<sql>
    CREATE INDEX idx_paciente_nome_unaccent
    ON paciente USING gin (unaccent(lower(nome)) gin_trgm_ops);
</sql>
```

(exige a extensão `pg_trgm`; adicionar só quando o volume justificar)

Não indexar por reflexo. Índice custa escrita e espaço — criar quando há consulta
que o use.

---

## 10. Foreign keys e exclusão

```xml
<column name="tenant_id" type="UUID">
    <constraints nullable="false"
                 foreignKeyName="fk_atendimento_tenant"
                 references="tenant(id)"/>
</column>
```

Para agregados com filhos que não fazem sentido sozinhos:

```xml
<addForeignKeyConstraint
        baseTableName="avaliacao_antropometrica"  baseColumnNames="atendimento_id"
        referencedTableName="atendimento"          referencedColumnNames="id"
        constraintName="fk_avaliacao_atendimento"
        onDelete="CASCADE"/>
```

`ON DELETE CASCADE` só dentro do agregado (atendimento → avaliação → medidas).
**Nunca** em FK para `tenant` ou `usuario` — apagar um tenant não deve varrer a
base em silêncio; isso é operação deliberada, com backup e confirmação.

### Exclusão física × lógica

Padrão é **exclusão física**. Soft delete só onde houver regra de negócio ou
exigência de auditoria — e aí a coluna é `ativo BOOLEAN`, não `deleted_at`.

Dados clínicos (paciente, atendimento, prescrição) são **desativados**, nunca
apagados: fazem parte do histórico assistencial. Exclusão real só via pedido de
titular sob LGPD, em rotina própria e registrada em `audit_log`.

---

## 11. Seeds

Não há catálogo de roles em tabela — o nível de acesso é um enum na coluna
`usuario.role`, validado por `CHECK`. Um seed de dado de sistema, quando houver,
entra por migration com `rollback` declarado:

```xml
<changeSet id="020-insert-tipos-dieta" author="eduardo">
    <insert tableName="tipo_dieta">
        <column name="id" valueComputed="gen_random_uuid()"/>
        <column name="nome" value="Contínua"/>
        <column name="created_at" valueComputed="now()"/>
    </insert>
    <rollback>
        <delete tableName="tipo_dieta">
            <where>nome = 'Contínua'</where>
        </delete>
    </rollback>
</changeSet>
```

Catálogos globais volumosos (fórmulas enterais, suplementos, tabelas de percentil
da OMS) entram por `<loadData>` a partir de CSV versionado:

```xml
<loadData tableName="formula_enteral"
          file="db/data/formula-enteral.csv"
          separator=";" encoding="UTF-8"/>
```

Os CSV ficam em `src/main/resources/db/data/` e são a fonte auditável do catálogo.
Percentis da OMS e tabelas de P50 de CB **nunca** são digitados à mão no XML.

Tenant raiz e superadmin **não** entram por migration — vêm do `DataSeeder`, com
credenciais por variável de ambiente (ver [02-modulo-usuarios.md §9](02-modulo-usuarios.md)).
Credencial em migration é credencial no repositório.

---

## 12. Alterações em tabela existente

Nunca editar o changeSet original. Criar um novo:

```xml
<changeSet id="042-add-etnia-paciente" author="eduardo">
    <addColumn tableName="paciente">
        <column name="etnia" type="VARCHAR(20)"/>
    </addColumn>
    <sql>
        ALTER TABLE paciente ADD CONSTRAINT ck_paciente_etnia
        CHECK (etnia IS NULL OR etnia IN ('BRANCA','NEGRA','OUTRA'));
    </sql>
    <rollback>
        <sql>ALTER TABLE paciente DROP CONSTRAINT ck_paciente_etnia;</sql>
        <dropColumn tableName="paciente" columnName="etnia"/>
    </rollback>
</changeSet>
```

Coluna `NOT NULL` em tabela com dados: três changeSets — adicionar nullable,
preencher, depois aplicar a restrição. Em um passo só, a migration falha em
produção e passa em desenvolvimento.

---

## 13. Banco e aplicação em sincronia

Ao criar ou alterar Entity, Repository, DTO ou Mapper, conferir que o Liquibase
tem estrutura correspondente:

- Campo `nullable = false` na entidade → `nullable="false"` na coluna
- `@Column(length = 255)` → `VARCHAR(255)`
- `BigDecimal precision/scale` → `NUMERIC(p,s)` com os mesmos valores
- Enum Java → `CHECK` com exatamente os mesmos valores
- Unique de negócio → `addUniqueConstraint` composta com `tenant_id`

Os testes de integração pegam a divergência: rodam contra o PostgreSQL local
`nutridb_test`, com o Liquibase recriando o schema e as queries sendo executadas
de verdade. H2 não serviria — não tem `unaccent`, trata `NULL` em unique de outro
jeito e aceita SQL que o Postgres rejeita.

---

## 14. O que nunca fazer

- Editar changeSet já executado
- Alterar banco fora do Liquibase
- `ddl-auto` diferente de `none`
- Tabela de negócio sem `tenant_id`
- Tabela sem auditoria (`created_at`, `updated_at`, `created_by`, `updated_by`)
- FK, índice, unique ou check sem nome explícito
- Unique de campo de negócio sem `tenant_id`
- Tabela multi-tenant sem índice em `tenant_id`
- FK sem índice
- `VARCHAR` sem tamanho
- `DOUBLE`/`REAL`/`FLOAT` em medida, dosagem ou valor calórico
- `TIMESTAMP` sem time zone
- Tipo `ENUM` nativo ou enum como ordinal
- `camelCase` ou plural em nome de tabela ou coluna
- `ON DELETE CASCADE` em FK para `tenant` ou `usuario`
- Credencial em migration
- Migration em SQL solto quando o Liquibase XML resolve

---

## 15. Checklist de migration

- [ ] Liquibase XML em `changes/NNN-descricao.xml`, incluído no master
- [ ] `id` único igual ao nome do arquivo, `author` preenchido
- [ ] `rollback` declarado quando não inferido
- [ ] PK `UUID` com `gen_random_uuid()`
- [ ] `tenant_id UUID NOT NULL` + FK (ou nullable, se catálogo híbrido)
- [ ] Auditoria completa
- [ ] `idx_<tabela>_tenant_id` criado
- [ ] Índice em toda FK
- [ ] Índices de busca com `tenant_id` primeiro
- [ ] Uniques compostas com `tenant_id`
- [ ] `CHECK` para cada enum
- [ ] `snake_case`, tabela no singular
- [ ] Todo objeto com nome explícito
- [ ] `NUMERIC` para tudo que é medida ou dosagem
- [ ] `TIMESTAMP WITH TIME ZONE` em data-hora
- [ ] Entidade JPA em sincronia com a estrutura
- [ ] Roles do módulo semeadas
