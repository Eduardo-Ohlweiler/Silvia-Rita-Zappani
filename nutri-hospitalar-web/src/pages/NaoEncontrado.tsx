import { useNavigate } from 'react-router-dom'
import { TButton, TPanel } from '@/components/common'

/**
 * 404 de dentro do ERP. Renderiza no Layout e **mantém a URL** — redirecionar
 * esconderia o link quebrado de quem precisa consertá-lo.
 */
export function NaoEncontrado() {
  const navigate = useNavigate()

  return (
    <TPanel title="Página não encontrada">
      <p className="text-body text-txt-secondary">
        O endereço que você abriu não existe ou foi movido.
      </p>

      <TButton className="mt-5" onClick={() => navigate('/app')}>
        Ir para o início
      </TButton>
    </TPanel>
  )
}
