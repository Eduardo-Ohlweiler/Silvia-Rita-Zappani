import { api } from './api'
import type { SelectOption } from '@/types/comum'

export interface CidadeSelect {
  id: string
  nome: string
  estadoSigla: string
}

/**
 * Estados e municípios do IBGE — referência global, igual para todo cliente.
 * São mais de cinco mil cidades: a busca é sempre no servidor.
 */
export const localidadeService = {
  estados: () => api.get<SelectOption[]>('/estados/select').then((r) => r.data),

  cidades: (nome?: string, estadoId?: string) =>
    api
      .get<CidadeSelect[]>('/cidades/select', { params: { nome, estadoId } })
      .then((r) => r.data),

  /** Para o `TCombo`, que espera `{ id, nome }`. A UF entra no rótulo. */
  cidadesParaCombo: (nome?: string, estadoId?: string) =>
    localidadeService
      .cidades(nome, estadoId)
      .then((cidades) =>
        cidades.map((c) => ({ id: c.id, nome: `${c.nome} — ${c.estadoSigla}` })),
      ),
}
