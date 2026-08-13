import { useEffect, useState } from 'react'

/** Evita uma chamada à API a cada tecla nos campos de busca. */
export function useDebounce<T>(valor: T, atraso = 400): T {
  const [adiado, setAdiado] = useState(valor)

  useEffect(() => {
    const id = setTimeout(() => setAdiado(valor), atraso)
    return () => clearTimeout(id)
  }, [valor, atraso])

  return adiado
}
