import { api } from './api'
import type { SelectOption } from '@/types/comum'

const buscar = (rota: string) => api.get<SelectOption[]>(rota).then((r) => r.data)

/**
 * Listas de referência do cadastro de pessoas. São globais — não passam pelo
 * tenant — e vêm do seed da migration; não há tela de manutenção.
 */
export const catalogoService = {
  tiposCadastro: () => buscar('/tipos-cadastro/select'),
  tiposTelefone: () => buscar('/tipos-telefone/select'),
  tiposEmail: () => buscar('/tipos-email/select'),
  tiposRedeSocial: () => buscar('/tipos-rede-social/select'),
}
