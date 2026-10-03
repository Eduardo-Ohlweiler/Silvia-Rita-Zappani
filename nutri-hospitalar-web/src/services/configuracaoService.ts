import axios from 'axios'
import { api } from './api'
import type { Aparencia, PaletaSistema } from '@/types/configuracao'

export const configuracaoService = {
  /**
   * `axios.get` puro, sem os interceptores de `api`: a rota é pública e a tela
   * de login a chama antes de existir sessão. E não pode levar o bearer — o
   * filtro de JWT responde 401 a token vencido mesmo em rota pública, e o 401
   * dispararia uma renovação de sessão por causa de uma cor.
   */
  aparencia: () =>
    axios
      .get<Aparencia>(`${api.defaults.baseURL}/configuracoes/aparencia`, { timeout: 10_000 })
      .then((r) => r.data),

  atualizarAparencia: (paleta: PaletaSistema) =>
    api.put<Aparencia>('/configuracoes/aparencia', { paleta }).then((r) => r.data),
}
