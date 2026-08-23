import { Suspense, lazy } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from '@/components/layout/Layout'
import { TProtected } from '@/components/layout/TProtected'
import { Dashboard } from '@/pages/Dashboard'
import { NaoEncontrado } from '@/pages/NaoEncontrado'
import { NaoEncontradoPublico } from '@/pages/NaoEncontradoPublico'
import { Login } from '@/pages/auth/Login'
import { LoginLogList } from '@/pages/loginlog/LoginLogList'
import { AvaliacaoPediatricaForm } from '@/pages/pediatria/AvaliacaoPediatricaForm'
import { AvaliacaoPediatricaList } from '@/pages/pediatria/AvaliacaoPediatricaList'
import { FormulaLacteaForm } from '@/pages/pediatria/FormulaLacteaForm'
import { FormulaLacteaList } from '@/pages/pediatria/FormulaLacteaList'
import { PediatriaCalculadora } from '@/pages/pediatria/PediatriaCalculadora'
import { PediatriaDashboard } from '@/pages/pediatria/PediatriaDashboard'
import { PediatriaPacienteDashboard } from '@/pages/pediatria/PediatriaPacienteDashboard'
import { FormulaEnteralForm } from '@/pages/uti/FormulaEnteralForm'
import { FormulaEnteralList } from '@/pages/uti/FormulaEnteralList'
import { ProdutoNutricionalForm } from '@/pages/uti/ProdutoNutricionalForm'
import { ProdutoNutricionalList } from '@/pages/uti/ProdutoNutricionalList'
import { AvaliacaoUtiForm } from '@/pages/uti/AvaliacaoUtiForm'
import { AvaliacaoUtiList } from '@/pages/uti/AvaliacaoUtiList'
import { FerramentasClinicas } from '@/pages/uti/FerramentasClinicas'
import { UtiCalculadora } from '@/pages/uti/UtiCalculadora'
import { Perfil } from '@/pages/perfil/Perfil'
import { PessoaForm } from '@/pages/pessoa/PessoaForm'
import { PessoaList } from '@/pages/pessoa/PessoaList'
import { TenantForm } from '@/pages/tenant/TenantForm'
import { TenantList } from '@/pages/tenant/TenantList'
import { UsuarioForm } from '@/pages/usuario/UsuarioForm'
import { UsuarioList } from '@/pages/usuario/UsuarioList'

// Lazy: página pública, com fontes e scripts próprios (Panda Video, Meta
// Pixel) — não faz sentido pesar o bundle inicial do ERP autenticado com isso.
const Landing = lazy(() => import('@/pages/landing/Landing').then((m) => ({ default: m.Landing })))

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        {/* A raiz é a landing de captação: o domínio é o da marca. */}
        <Route
          path="/"
          element={
            <Suspense fallback={null}>
              <Landing />
            </Suspense>
          }
        />
        {/* A landing morava aqui. Anúncio e link em bio continuam valendo. */}
        <Route path="/conheca" element={<Navigate to="/" replace />} />

        {/* Pública. Não há cadastro: o acesso é provisionado pelo superadmin. */}
        <Route path="/app/login" element={<Login />} />
        <Route path="/login" element={<Navigate to="/app/login" replace />} />

        {/*
          Todo o ERP vive sob /app, fora do namespace de marketing. Assim o
          robots.txt bloqueia o sistema inteiro numa linha e a raiz fica livre
          para páginas novas de captação sem risco de colidir com rota interna.
        */}
        <Route path="/app" element={<TProtected />}>
          <Route element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="perfil" element={<Perfil />} />

            {/* Módulo de negócio: ADMIN e USER operam, não só o superadmin. */}
            <Route path="pessoas" element={<PessoaList />} />
            <Route path="pessoas/nova" element={<PessoaForm />} />
            <Route path="pessoas/:id" element={<PessoaForm />} />

            <Route path="pediatria/painel-paciente" element={<PediatriaPacienteDashboard />} />
            <Route path="pediatria/dashboard" element={<PediatriaDashboard />} />
            <Route path="pediatria/calculadora" element={<PediatriaCalculadora />} />
            <Route path="pediatria/avaliacoes" element={<AvaliacaoPediatricaList />} />
            <Route path="pediatria/avaliacoes/nova" element={<AvaliacaoPediatricaForm />} />
            <Route path="pediatria/avaliacoes/:id" element={<AvaliacaoPediatricaForm />} />
            <Route path="pediatria/formulas-lacteas" element={<FormulaLacteaList />} />
            <Route path="pediatria/formulas-lacteas/nova" element={<FormulaLacteaForm />} />
            <Route path="pediatria/formulas-lacteas/:id" element={<FormulaLacteaForm />} />

            {/* Rota literal antes de /:id — o Spring e o router preferem o literal */}
            <Route path="uti/calculadora" element={<UtiCalculadora />} />
            <Route path="uti/ferramentas" element={<FerramentasClinicas />} />
            <Route path="uti/avaliacoes" element={<AvaliacaoUtiList />} />
            <Route path="uti/avaliacoes/nova" element={<AvaliacaoUtiForm />} />
            <Route path="uti/avaliacoes/:id" element={<AvaliacaoUtiForm />} />
            <Route path="uti/formulas-enterais" element={<FormulaEnteralList />} />
            <Route path="uti/formulas-enterais/nova" element={<FormulaEnteralForm />} />
            <Route path="uti/formulas-enterais/:id" element={<FormulaEnteralForm />} />
            <Route path="uti/produtos" element={<ProdutoNutricionalList />} />
            <Route path="uti/produtos/novo" element={<ProdutoNutricionalForm />} />
            <Route path="uti/produtos/:id" element={<ProdutoNutricionalForm />} />

            {/*
              Área administrativa. O guard aqui é conveniência de navegação —
              a autorização real é o @PreAuthorize do backend.
            */}
            <Route element={<TProtected roles={['SUPERADMIN']} />}>
              <Route path="usuarios" element={<UsuarioList />} />
              <Route path="usuarios/novo" element={<UsuarioForm />} />
              <Route path="usuarios/:id" element={<UsuarioForm />} />
              <Route path="tenants" element={<TenantList />} />
              <Route path="tenants/:id" element={<TenantForm />} />
              <Route path="log-acesso" element={<LoginLogList />} />
            </Route>

            <Route path="*" element={<NaoEncontrado />} />
          </Route>
        </Route>

        {/*
          404 público — não redirecionar para "/". A landing monta o Meta Pixel,
          então mandar todo erro de URL para lá transforma link quebrado em
          PageView de tráfego pago. E como o SPA responde 200 em qualquer
          caminho, o Google indexaria o lixo como duplicata da home.
        */}
        <Route path="*" element={<NaoEncontradoPublico />} />
      </Routes>
    </BrowserRouter>
  )
}
