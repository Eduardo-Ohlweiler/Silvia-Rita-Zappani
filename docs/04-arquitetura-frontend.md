# Arquitetura Frontend — React 19 + TypeScript + Tailwind v4

> Leia antes de criar qualquer página, componente, formulário, grid ou integração
> com a API.
>
> Cores, fontes, espaçamentos e componentes visuais: [05-identidade-visual.md](05-identidade-visual.md).

---

## 1. Stack

React 19 · TypeScript · Vite · Tailwind v4 · react-router-dom v7 · axios ·
react-hook-form · zod · react-toastify · `@fontsource` · `vite-plugin-svgr`

Nada além disso sem justificativa no PR. Em particular: **sem** biblioteca de
componentes de terceiros (MUI, Ant, shadcn) — a biblioteca é interna; **sem**
Redux/Zustand/Jotai — Context API dá conta deste porte; **sem** React Query nesta
fase — as telas são Form/List simples e o `useState` + service resolve.

---

## 2. Estrutura

O que existe hoje (fatia 2 concluída):

```
src/
├── assets/
│   ├── brand/             logo-full.svg · logo-mark.svg · logo-simbolo.svg
│   └── icons/index.tsx    ícones em SVG inline, num módulo único
├── components/
│   ├── common/            biblioteca interna T* — §4
│   └── layout/            Layout · Sidebar · TenantSwitcher · TProtected
├── contexts/              AuthContext · ThemeContext
├── hooks/                 useAuth · useTheme · useDebounce
├── pages/
│   ├── auth/              Login
│   ├── Dashboard.tsx      tela inicial
│   ├── usuario/           UsuarioList · UsuarioForm
│   ├── tenant/            TenantList
│   ├── loginlog/          LoginLogList
│   └── perfil/            Perfil
├── routes/                AppRoutes.tsx
├── services/              api.ts · tokenStore.ts + um service por módulo
├── styles/                theme.css
├── types/                 auth · comum · usuario · tenant · loginlog
└── utils/                 format.ts
```

Da fatia 3 em diante entram `pages/paciente/`, `services/pacienteService.ts`,
`types/paciente.ts` — e, quando houver cálculo, `utils/calc-display.ts` e
`styles/print.css`.

Espelha os módulos do backend. Se existe `paciente` lá, existe `pages/paciente`,
`services/pacienteService.ts` e `types/paciente.ts` aqui.

---

## 3. Antes de escrever qualquer coisa

Ordem obrigatória:

1. Procurar **tela semelhante** já implementada
2. Procurar **componente semelhante** na biblioteca interna
3. Procurar **service semelhante**
4. Só então gerar código novo

A implementação nova deve parecer ter sido escrita junto com o resto do sistema.
Não inventar arquitetura alternativa quando já existe uma equivalente.

Tela de referência: **`UsuarioList`/`UsuarioForm`** — é o Form/List completo, com
filtros, `TCombo` de busca no servidor, coluna condicional, paginação, ação de
linha e formulário com `react-hook-form` + `zod`. Copiar dela.

---

## 3.1 Ícones

SVG inline em `src/assets/icons/index.tsx`, num módulo único. **Nenhum
componente importa ícone de outro lugar** — trocar de família é mexer só ali.

Escolhi inline em vez de biblioteca por três razões: só embarca o que se usa,
não há risco de a dependência mudar de API, e o traço fica sob controle — 1.8
para casar com a direção delicada do doc 05. Todos usam `currentColor`, então
herdam a cor do texto nos dois temas.

```tsx
import { IconBusca } from '@/assets/icons'

<IconBusca className="size-4 text-txt-muted" />
```

---

## 4. Biblioteca interna

Componentes em `components/common/`. **Nunca** criar input, select, checkbox,
radio, grid, modal ou container de página novos quando já existe equivalente.

### Existe hoje

| Componente | Papel |
|---|---|
| `TPage` | container de tela: título, subtítulo, ações do topo |
| `TPanel` | agrupamento visual, com título opcional |
| `TEntry` | campo de texto/número, com rótulo, erro, ajuda e sufixo |
| `TSelect` | `<select>` nativo, com opção "todos"/vazio |
| `TCombo` | select **com busca no servidor**, via `/select?nome=`; teclado e clique-fora |
| `TDataGrid` | tabela no desktop, **cartões abaixo de `md`**; skeleton, estado vazio, ação de linha |
| `TDataGridFooter` | paginação e contagem |
| `TButton` | `variant`: primary · secondary · danger · ghost; `size`; `loading` |
| `TBadge` | status com rótulo — `tom`: sucesso · alerta · neutro · info |
| `TThemeToggle` | alterna claro/escuro |

Em `components/layout/`: `Layout` (header, faixa de impersonação, conteúdo),
`Sidebar` (fixa em `lg`, drawer abaixo), `TenantSwitcher` (só superadmin) e
`TProtected` (guarda de rota, com `roles` opcional).

### A criar quando a fatia pedir

Não adiantar: componente sem uso real nasce errado. `TDate`/`TDateTime` (paciente),
`TCheckBox`/`TRadio`, `TFieldList` (medidas seriadas), `TWindow` (modal),
`TDropdown`, e o `TResult` — **específico deste sistema**, para exibir resultado de
cálculo com valor, unidade, classificação e referência.

`TRow`/`TSpace`/`TForm` foram descartados: `grid gap-4 sm:grid-cols-2` do Tailwind
e o `<form>` nativo com `handleSubmit` resolvem sem camada intermediária.

### Escolha entre `TSelect` e `TCombo`

**`TSelect` para lista fixa e curta** (nível de acesso, situação, enum);
**`TCombo` quando a lista cresce** (cliente, paciente, fórmula). Um `<select>` com
dezenas de clientes obriga a rolar procurando; o `TCombo` busca no servidor com
debounce, sem carregar tudo para filtrar no navegador.

Todos com props tipadas, `forwardRef` quando o react-hook-form precisa, e estilo
só por tokens.

### Regras de estilo que já custaram bug

- **Token de tema, nunca cor fixa, em estado interativo.** `hover:bg-surface-alt`,
  não `hover:bg-brand-50`: a escala `brand-*` é fixa nos dois temas e ficaria clara
  sobre fundo escuro. Vale para `border-brand-300` também.
- **Botão cheio usa `text-txt-inverse`, nunca `text-white`.** No escuro a primária
  clareia, e branco sobre ela dá 3.28:1 — abaixo do mínimo AA. Ver doc 05.

---

## 5. Padrões de tela

Existem **dois**. Toda tela é um dos dois.

### Form/List — entidades principais

Duas telas separadas: listagem (`/pacientes`) e formulário
(`/pacientes/novo`, `/pacientes/:id`). Muitos registros, busca, filtros, paginação.

Usar em: Paciente, Atendimento, Usuario, Tenant, Formula, Suplemento.

Nomenclatura em português, como no resto do código. As props do `TDataGrid` são as
reais — conferir em `UsuarioList` antes de copiar:

```tsx
export function PacienteList() {
  const navigate = useNavigate()
  const [nome, setNome] = useState('')
  const [ativo, setAtivo] = useState('')
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<Page<PacienteResponse>>()
  const [carregando, setCarregando] = useState(true)

  const nomeBusca = useDebounce(nome)

  const carregar = useCallback(() => {
    setCarregando(true)
    pacienteService
      .getAll({
        nome: nomeBusca || undefined,
        ativo: ativo === '' ? undefined : ativo === 'true',
        page: pagina,
        size: 20,
      })
      .then(setDados)
      .catch(handleApiError)
      .finally(() => setCarregando(false))
  }, [nomeBusca, ativo, pagina])

  useEffect(carregar, [carregar])

  const colunas: Coluna<PacienteResponse>[] = [
    { chave: 'nome', cabecalho: 'Nome', render: (p) => p.nome },
    { chave: 'nascimento', cabecalho: 'Nascimento', secundaria: true,
      render: (p) => formatarData(p.dataNascimento) },
    { chave: 'situacao', cabecalho: 'Situação',
      render: (p) => <TBadge tom={p.ativo ? 'sucesso' : 'neutro'}>
        {p.ativo ? 'Ativo' : 'Inativo'}
      </TBadge> },
  ]

  return (
    <TPage
      title="Pacientes"
      actions={<TButton onClick={() => navigate('/pacientes/novo')}>
        <IconAdicionar className="size-4" />
        Novo paciente
      </TButton>}
    >
      <TPanel>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <TEntry
            label="Nome"
            value={nome}
            suffix={<IconBusca className="size-4" />}
            onChange={(e) => { setNome(e.target.value); setPagina(0) }}
          />
          <TSelect
            label="Situação"
            vazio="Todas"
            opcoes={SITUACAO_OPCOES}
            value={ativo}
            onChange={(e) => { setAtivo(e.target.value); setPagina(0) }}
          />
        </div>
      </TPanel>

      <TDataGrid
        colunas={colunas}
        linhas={dados?.content ?? []}
        chaveDe={(p) => p.id}
        carregando={carregando}
        onLinhaClick={(p) => navigate(`/pacientes/${p.id}`)}
        tituloCartao={(p) => p.nome}
        vazioTitulo="Nenhum paciente encontrado"
        vazioDescricao="Ajuste os filtros ou cadastre o primeiro."
      />

      <TDataGridFooter pagina={dados} onPaginaChange={setPagina} />
    </TPage>
  )
}
```

Três detalhes que não são estilo, são o que faz a tela funcionar:

- **`useCallback` + `useEffect(carregar, [carregar])`.** Mantém uma função só de
  recarga, reaproveitada depois de uma ação de linha.
- **`setPagina(0)` a cada mudança de filtro.** Sem isso, filtrar na página 3 devolve
  uma página vazia e parece que não há resultado.
- **`secundaria: true`** nas colunas que podem sumir primeiro no mobile.

### FormList — entidades auxiliares

Formulário e grid na mesma tela, sem navegação. Poucos registros, CRUD simples.

Usar em: tabelas de apoio (tipos, unidades) — e só nelas.

---

## 6. Formulários

Sempre **react-hook-form + zod**. Nunca um `useState` por campo.

```tsx
const pacienteSchema = z.object({
  nome:           z.string().min(1, 'Informe o nome').max(255),
  dataNascimento: z.coerce.date().max(new Date(), 'Data futura').optional(),
  sexo:           z.nativeEnum(Sexo, { required_error: 'Informe o sexo' }),
  cpf:            z.string().refine(isCpfValido, 'CPF inválido').optional().or(z.literal('')),
  pesoAtual:      z.coerce.number().min(0.5).max(500).optional(),
})

type PacienteFormData = z.infer<typeof pacienteSchema>

export function PacienteForm() {
  const { id } = useParams()
  const navigate = useNavigate()

  const { register, handleSubmit, reset, control, formState: { errors, isSubmitting } } =
    useForm<PacienteFormData>({ resolver: zodResolver(pacienteSchema) })

  useEffect(() => {
    if (id) pacienteService.findById(id).then(p => reset(toFormData(p))).catch(handleApiError)
  }, [id])

  const onSubmit = async (data: PacienteFormData) => {
    try {
      id ? await pacienteService.update(id, data) : await pacienteService.create(data)
      toast.success('Paciente salvo')
      navigate('/pacientes')
    } catch (e) {
      handleApiError(e)
    }
  }

  return (
    <TPage title={id ? 'Editar paciente' : 'Novo paciente'}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
        <TPanel title="Dados pessoais">
          <div className="grid gap-4 sm:grid-cols-2">
            <TEntry label="Nome" autoFocus error={errors.nome?.message} {...register('nome')} />
            <TEntry label="Nascimento" type="date" error={errors.dataNascimento?.message}
                    {...register('dataNascimento')} />
            <TSelect label="Sexo" opcoes={SEXO_OPCOES} {...register('sexo')} />
          </div>
        </TPanel>

        {/* Em mobile o botão principal fica embaixo: column-reverse resolve
            sem duplicar markup */}
        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <TButton type="button" variant="secondary" onClick={() => navigate('/pacientes')}
                   className="sm:w-auto">
            Cancelar
          </TButton>
          <TButton type="submit" loading={isSubmitting}>Salvar</TButton>
        </div>
      </form>
    </TPage>
  )
}
```

`<form>` nativo com `noValidate` — a validação é do zod, e a do navegador só
atrapalharia com mensagens fora do padrão visual. Para campo que não é um `input`
simples (`TCombo`, por exemplo), usar o `Controller` do react-hook-form, como em
`UsuarioForm`.

A validação zod do front **espelha** a Bean Validation do backend, não a substitui.
O backend valida de novo, sempre — o front só evita ida e volta desnecessária.

---

## 7. Telas de cálculo

Específico deste sistema, e a regra mais importante deste documento.

**Nenhuma fórmula nutricional é implementada em TypeScript.** O front envia as
entradas, o backend calcula, o front exibe. Duas implementações da mesma fórmula
divergem — e aqui divergência significa prescrição errada.

Layout em duas colunas:

```tsx
<TPage title="Avaliação antropométrica">
  <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_380px]">

    {/* ENTRADAS */}
    <TPanel title="Medidas">
      <div className="grid gap-4 sm:grid-cols-2">
        <TEntry label="CB"  suffix="cm" type="number" {...register('circunferenciaBraco')} />
        <TEntry label="CP"  suffix="cm" type="number" {...register('circunferenciaPanturrilha')} />
        <TEntry label="AJ"  suffix="cm" type="number" {...register('alturaJoelho')} />
        <TEntry label="CA"  suffix="cm" type="number" {...register('circunferenciaAbdominal')} />
      </div>
    </TPanel>

    {/* RESULTADOS — vindos do backend */}
    <TPanel title="Resultados" className="lg:sticky lg:top-4 self-start">
      <TResult label="Peso estimado (Rabito)" value={r?.pesoRabito}  unit="kg" />
      <TResult label="Altura estimada (Chumlea)" value={r?.alturaChumlea} unit="cm" />
      <TResult label="IMC" value={r?.imc} classificacao={r?.classificacaoImc}
               referencia="OMS 1997" />
      <TResult label="Adequação de CB" value={r?.adequacaoCb} unit="%"
               classificacao={r?.classificacaoCb} referencia="Frisancho — P50" />
    </TPanel>
  </div>
</TPage>
```

Regras:

- Recálculo por **debounce de 500 ms** sobre as entradas válidas — não a cada tecla,
  não só no submit.
- O painel de resultados é `sticky` no desktop e vai para baixo no mobile.
- Cada `TResult` mostra **valor + unidade + classificação + referência**. O
  profissional precisa saber de qual fórmula veio o número; um valor solto na tela
  não é auditável.
- Enquanto recalcula, o valor anterior fica visível com opacidade reduzida — nunca
  sumir ou piscar.
- Entrada faltando: o resultado que depende dela mostra "—", e o `TResult`
  informa quais medidas faltam.
- Toda entrada de medida é `type="number"` com `step` e `min`/`max` coerentes, e
  `suffix` com a unidade.

---

## 8. Serviços

Toda chamada HTTP passa por `services/api.ts`. **Nunca `fetch` direto.**

```ts
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',   // dev cai no proxy do Vite
  timeout: 30_000,
})

api.interceptors.request.use(config => {
  const token = tokenStore.getAccessToken()
  if (token && !ehRotaPublica(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  r => r,
  async (erro: AxiosError<ErroResponse>) => {
    const original = erro.config as RequisicaoRepetivel | undefined

    if (erro.response?.status === 401 && original && !original._jaTentouRenovar &&
        !ehRotaPublica(original.url) && tokenStore.getRefreshToken()) {
      original._jaTentouRenovar = true
      try {
        const { accessToken } = await renovarSessao()
        original.headers.Authorization = `Bearer ${accessToken}`
        return api(original)
      } catch {
        tokenStore.clear()
        aoPerderSessao?.()            // o AuthContext navega para /login
      }
    }
    return Promise.reject(erro)
  },
)
```

### Uma renovação por vez, no aplicativo inteiro

O refresh é de **uso único com rotação**: reapresentar um token já consumido faz o
backend revogar a cadeia toda, tratando como roubo. Duas chamadas concorrentes com
o mesmo token derrubam a sessão do próprio usuário.

```ts
let renovacaoEmCurso: Promise<AuthResponse> | null = null

export function renovarSessao(): Promise<AuthResponse> {
  renovacaoEmCurso ??= executarRenovacao().finally(() => { renovacaoEmCurso = null })
  return renovacaoEmCurso
}
```

Interceptor e boot do `AuthContext` compartilham essa promessa. São dois cenários
reais, não hipóteses: várias requisições tomando 401 juntas, e o `StrictMode`, que
em desenvolvimento monta o efeito de boot **duas vezes** — as duas leriam o mesmo
token do `localStorage`. Foi assim que a troca de tenant passou a jogar o
superadmin na tela de login.

A renovação usa `axios.post` puro, e não a instância `api`: passar pelos
interceptores dela criaria recursão no 401.

**Redirecionar para o login é do `AuthContext`, não do `api.ts`.**
`window.location.assign` recarrega a aplicação e descarta o token em memória —
o callback registrado navega pelo router, sem reload.

Um service por módulo, tipado ponta a ponta:

```ts
export const pacienteService = {
  getAll: (params: PacienteFiltros & Paginacao) =>
    api.get<Page<PacienteResponse>>('/pacientes', { params }).then(r => r.data),

  findById: (id: string) =>
    api.get<PacienteResponse>(`/pacientes/${id}`).then(r => r.data),

  select: (nome?: string) =>
    api.get<SelectOption[]>('/pacientes/select', { params: { nome } }).then(r => r.data),

  create: (dto: PacienteCreate) =>
    api.post<PacienteResponse>('/pacientes', dto).then(r => r.data),

  update: (id: string, dto: PacienteUpdate) =>
    api.put<PacienteResponse>(`/pacientes/${id}`, dto).then(r => r.data),
}
```

### Tratamento de erro

Um helper único, usado em todo `catch`:

```ts
export function handleApiError(error: unknown): void {
  if (axios.isAxiosError<ErroResponse>(error)) {
    const status = error.response?.status
    if (status === 401) return                                   // o interceptor cuida
    if (status === 403) return toast.error('Você não tem permissão para esta ação')
    if (status === 404) return toast.error('Registro não encontrado')
    return toast.error(error.response?.data?.erro ?? 'Falha na comunicação com o servidor')
  }
  toast.error('Erro inesperado')
}
```

**Nunca `alert()`.** Sempre `toast.success` / `toast.error` / `toast.warning`.

---

## 9. Autenticação e autorização visual

`AuthContext` guarda usuário, tenant, roles e o flag `impersonating`.
O access token fica **em memória**; o refresh token em `localStorage`.

A sessão é um objeto só, derivado do token — nunca montado a partir de campos
soltos vindos do formulário:

```ts
interface Sessao {
  usuarioId: string
  nome: string
  email: string
  role: Role                  // SUPERADMIN | ADMIN | USER
  tenantId: string
  tenantNome: string
  impersonating: boolean      // superadmin dentro de outro tenant
}
```

`useAuth()` só devolve o contexto — quem decide é o `TProtected` e o próprio JSX,
comparando `sessao.role`. Com nível único não há cálculo de roles efetivas para
fazer:

```ts
export function useAuth() {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth precisa estar dentro de <AuthProvider>')
  return contexto
}
```

Rotas protegidas por `TProtected`, nunca com verificação dentro da página:

```tsx
<Route element={<TProtected />}>
  <Route element={<Layout />}>
    <Route path="/pacientes"     element={<PacienteList />} />
    <Route path="/pacientes/:id" element={<PacienteForm />} />
  </Route>
</Route>

<Route element={<TProtected roles={['SUPERADMIN']} />}>
  <Route path="/tenants" element={<TenantList />} />
</Route>
```

### O front não autoriza

Esconder um menu ou um botão é **conveniência visual**. A autorização real está no
`@PreAuthorize` do backend. Nunca supor que uma rota escondida está protegida.

```tsx
{sessao.role === 'SUPERADMIN' && <TButton onClick={novo}>Novo usuário</TButton>}
```

### Seletor de tenant e faixa de impersonação

No header, só para `SUPERADMIN`: um seletor alimentado por `/tenants/select` que
chama `switchTenant` e troca o access token.

> **Nunca recarregar a página ao trocar de tenant.** O access token vive em
> memória; um reload o descarta, o boot restaura a sessão pelo refresh token —
> que pertence ao tenant de origem — e o usuário volta de onde saiu. Não é
> preciso: o `AuthContext` já atualizou a sessão, e as listas refazem a consulta
> porque dependem de `sessao.tenantId`.

Quando `impersonating`, o `Layout` mostra a faixa no topo, em `--color-warning`:

> **Você está navegando como o tenant "Clínica X".** [Sair do tenant]

Sem essa faixa é fácil o superadmin agir no tenant errado achando que está no seu.

---

## 10. TypeScript

- **Zero `any`.** `unknown` + narrowing quando o tipo é realmente desconhecido.
- Um arquivo de tipos por módulo, espelhando os DTOs do backend:

```ts
export interface PacienteResponse {
  id: string
  nome: string
  dataNascimento: string | null      // ISO — converter só na exibição
  sexo: Sexo
  cpf: string | null
  pesoAtual: number | null
  ativo: boolean
  createdAt: string
}

export type PacienteCreate = Omit<PacienteResponse, 'id' | 'ativo' | 'createdAt'>
export type PacienteUpdate = PacienteCreate
```

- Enums do backend viram `enum` ou union de string literais, e os rótulos ficam
  num mapa único: `const SEXO_LABEL: Record<Sexo, string>`.
- Tipos genéricos compartilhados em `types/common.ts`: `Page<T>`, `SelectOption`,
  `Paginacao`, `ErroResponse`.
- Data e hora trafegam como string ISO e são formatadas só na exibição
  (`utils/format.ts`). Nada de `Date` no estado.
- Valores numéricos de cálculo chegam como `number` do JSON, mas são **exibidos**
  com a casa decimal que o backend definiu — o front não arredonda por conta.

---

## 11. Estado e componentização

- `useState` / `useEffect` / `useMemo` / `useCallback` conforme a necessidade real.
  Sem `useMemo` decorativo.
- Nada de estado duplicado: se veio da API, tem uma fonte só.
- Extrair componente quando: aparece mais de uma vez, tem lógica reutilizável, ou
  passa de ~120 linhas.
- Lógica de negócio não mora em componente. Formatação vai para `utils/`,
  chamada de API para `services/`, estado compartilhado para `contexts/`.

---

## 12. Responsividade e acessibilidade

Funciona em desktop, tablet e mobile, sem quebrar:

| Faixa | Comportamento |
|---|---|
| ≥1280px | sidebar expandida, grid com todas as colunas, painel de resultado lateral |
| 768–1279px | sidebar recolhida em ícones, colunas secundárias ocultas |
| <768px | sidebar em *drawer*, grid vira lista de cartões, painel de resultado abaixo |

Acessibilidade: `<label>` associado a todo campo, `aria-invalid` e
`aria-describedby` no erro, foco visível (nunca `outline: none` sem substituto),
navegação completa por teclado, e nenhuma informação transmitida só por cor.

---

## 13. Tailwind

Tailwind é para **layout e espaçamento**. Cor, fonte e raio vêm dos tokens de
[05-identidade-visual.md](05-identidade-visual.md).

```tsx
// CERTO
<div className="flex items-center gap-3 rounded-md bg-surface p-4">

// ERRADO — hex solto
<div className="flex items-center gap-3 rounded-md bg-[#FFFFFF] p-4">
```

Classe utilitária repetida em três lugares vira componente ou `@apply` num
componente da biblioteca — não um quarto `className` copiado.

---

## 14. O que nunca fazer

- `fetch()` direto
- `alert()`
- `any` sem justificativa
- Criar input, select, grid ou modal quando já existe na biblioteca
- Implementar autenticação dentro de página
- **Reimplementar fórmula de cálculo em TypeScript**
- Hex de cor solto no JSX ou no CSS de componente
- Tela fora dos padrões Form/List ou FormList
- Ignorar tratamento de erro da API
- Estado de formulário grande em `useState` por campo
- Confiar em role do front para proteger dado
- Arredondar resultado de cálculo no front

---

## 15. Checklist de tela

- [ ] Padrão Form/List ou FormList
- [ ] Componentes da biblioteca interna
- [ ] react-hook-form + zod
- [ ] Chamadas via `services/`, erro tratado com `handleApiError`
- [ ] Feedback por `toast`
- [ ] Tipagem completa, zero `any`
- [ ] Tokens de cor e tipografia respeitados, tema claro e escuro
- [ ] Foco de teclado visível, campos rotulados
- [ ] Funciona em desktop, tablet e mobile
- [ ] Nenhum cálculo nutricional no front
- [ ] Menus e botões condicionados por `sessao.role` (conveniência, não segurança)
