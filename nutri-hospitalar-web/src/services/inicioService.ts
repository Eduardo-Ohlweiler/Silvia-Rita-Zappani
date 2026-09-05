import { api } from './api'
import type { PainelInicial } from '@/types/inicio'

/** A tela inicial: uma chamada só, que serve os dois módulos. */
export const inicioService = {
  painel: () => api.get<PainelInicial>('/painel-inicial').then((r) => r.data),
}
