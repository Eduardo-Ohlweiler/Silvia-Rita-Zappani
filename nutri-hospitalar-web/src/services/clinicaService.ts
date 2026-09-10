import { api } from './api'
import type { Page, SelectOption } from '@/types/comum'
import type {
  FichaAnamneseCreate,
  FichaAnamneseFiltros,
  FichaAnamneseLista,
  FichaAnamneseResponse,
  FichaAnamneseUpdate,
  ModeloFichaCreate,
  ModeloFichaFiltros,
  ModeloFichaResponse,
  ModeloFichaSelect,
  ModeloFichaUpdate,
  Escore,
  EscoreRequest,
} from '@/types/clinica'

/**
 * Módulo de negócio: o escopo é sempre o tenant efetivo do token.
 *
 * <p>Os modelos são catálogo híbrido — os do sistema aparecem para todo cliente
 * e recusam escrita com **404**; os criados pelo cliente só existem para ele.
 */
export const modeloFichaService = {
  getAll: (params: ModeloFichaFiltros) =>
    api.get<Page<ModeloFichaResponse>>('/modelos-ficha', { params }).then((r) => r.data),

  findById: (id: string) =>
    api.get<ModeloFichaResponse>(`/modelos-ficha/${id}`).then((r) => r.data),

  select: (termo?: string) =>
    api
      .get<ModeloFichaSelect[]>('/modelos-ficha/select', { params: { termo } })
      .then((r) => r.data),

  /**
   * O `TCombo` fala em `{ id, nome }`; aqui o modelo ganha a origem no rótulo.
   *
   * <p>Quem escolhe precisa distinguir o que veio pronto do que a própria
   * clínica montou — dois modelos podem ter nomes parecidos, e a diferença entre
   * eles é quem pode editá-los.
   */
  selectParaCombo: (termo?: string): Promise<SelectOption[]> =>
    modeloFichaService.select(termo).then((modelos) =>
      modelos.map((m) => ({
        id: m.id,
        nome: m.doSistema ? `${m.nome} · do sistema` : m.nome,
      })),
    ),

  create: (dto: ModeloFichaCreate) =>
    api.post<ModeloFichaResponse>('/modelos-ficha', dto).then((r) => r.data),

  update: (id: string, dto: ModeloFichaUpdate) =>
    api.put<ModeloFichaResponse>(`/modelos-ficha/${id}`, dto).then((r) => r.data),

  /** Cria a cópia editável do cliente a partir de qualquer modelo visível. */
  clonar: (id: string) =>
    api.post<ModeloFichaResponse>(`/modelos-ficha/${id}/clonar`).then((r) => r.data),

  alterarAtivo: (id: string, ativo: boolean) =>
    api
      .patch<ModeloFichaResponse>(`/modelos-ficha/${id}/ativo`, null, { params: { ativo } })
      .then((r) => r.data),

  /** Não danifica ficha nenhuma: elas guardam o retrato das perguntas. */
  delete: (id: string) => api.delete<void>(`/modelos-ficha/${id}`).then((r) => r.data),
}

export const fichaAnamneseService = {
  getAll: (params: FichaAnamneseFiltros) =>
    api.get<Page<FichaAnamneseLista>>('/fichas-anamnese', { params }).then((r) => r.data),

  findById: (id: string) =>
    api.get<FichaAnamneseResponse>(`/fichas-anamnese/${id}`).then((r) => r.data),

  create: (dto: FichaAnamneseCreate) =>
    api.post<FichaAnamneseResponse>('/fichas-anamnese', dto).then((r) => r.data),

  update: (id: string, dto: FichaAnamneseUpdate) =>
    api.put<FichaAnamneseResponse>(`/fichas-anamnese/${id}`, dto).then((r) => r.data),

  delete: (id: string) => api.delete<void>(`/fichas-anamnese/${id}`).then((r) => r.data),

  /**
   * O escore de um formulário ainda não salvo.
   *
   * <p>**204 quando o modelo não aplica escala** — vira `undefined`, e a tela
   * simplesmente não desenha o painel.
   *
   * <p>Este endpoint não devolve 400 por formulário incompleto: incompleto é
   * estado, e vem 200 com `total` nulo e o motivo escrito. Por isso a tela pode
   * chamá-lo a cada pausa de digitação sem empilhar toast vermelho.
   */
  escore: (dto: EscoreRequest) =>
    api
      .post<Escore | ''>('/fichas-anamnese/escore', dto)
      .then((r) => (r.status === 204 || !r.data ? undefined : (r.data as Escore))),
}
