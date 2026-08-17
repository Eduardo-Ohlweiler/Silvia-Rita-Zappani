import './landing.css'
import { MetaPixel } from './components/MetaPixel'
import { useDocumentMeta } from './hooks/useDocumentMeta'
import { Beneficios } from './sections/Beneficios'
import { Depoimentos } from './sections/Depoimentos'
import { EstruturaSessao } from './sections/EstruturaSessao'
import { Footer } from './sections/Footer'
import { Hero } from './sections/Hero'
import { OfertaFaq } from './sections/OfertaFaq'
import { Qualificacao } from './sections/Qualificacao'
import { SobreProfissional } from './sections/SobreProfissional'
import { Transformacao } from './sections/Transformacao'

export function Landing() {
  useDocumentMeta({
    titulo: 'Silvia Zappani — Segurança clínica hospitalar para nutricionistas',
    descricao:
      'Sessão Estratégica gratuita com Silvia Zappani: descubra o que você precisa para atuar com segurança no ambiente hospitalar.',
  })

  return (
    <div className="landing">
      <MetaPixel />
      <Hero />
      <Qualificacao />
      <SobreProfissional />
      <EstruturaSessao />
      <Transformacao />
      <Beneficios />
      <Depoimentos />
      <OfertaFaq />
      <Footer />
    </div>
  )
}
