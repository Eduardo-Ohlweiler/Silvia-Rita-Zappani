/** Espelha o `Page<T>` do Spring Data. */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface Paginacao {
  page?: number
  size?: number
}

export interface SelectOption {
  id: string
  nome: string
}

/** Espelha o `ErroResponseDto` do backend. */
export interface ErroResponse {
  erro: string
  codigo: number
  timestamp: string
  path: string
}
