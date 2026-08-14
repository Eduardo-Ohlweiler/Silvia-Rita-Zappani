import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from '@/components/layout/Layout'
import { TProtected } from '@/components/layout/TProtected'
import { Dashboard } from '@/pages/Dashboard'
import { Login } from '@/pages/auth/Login'
import { LoginLogList } from '@/pages/loginlog/LoginLogList'
import { Perfil } from '@/pages/perfil/Perfil'
import { PessoaForm } from '@/pages/pessoa/PessoaForm'
import { PessoaList } from '@/pages/pessoa/PessoaList'
import { TenantForm } from '@/pages/tenant/TenantForm'
import { TenantList } from '@/pages/tenant/TenantList'
import { UsuarioForm } from '@/pages/usuario/UsuarioForm'
import { UsuarioList } from '@/pages/usuario/UsuarioList'

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Pública. Não há cadastro: o acesso é provisionado pelo superadmin. */}
        <Route path="/login" element={<Login />} />

        <Route element={<TProtected />}>
          <Route element={<Layout />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/perfil" element={<Perfil />} />

            {/* Módulo de negócio: ADMIN e USER operam, não só o superadmin. */}
            <Route path="/pessoas" element={<PessoaList />} />
            <Route path="/pessoas/nova" element={<PessoaForm />} />
            <Route path="/pessoas/:id" element={<PessoaForm />} />

            {/*
              Área administrativa. O guard aqui é conveniência de navegação —
              a autorização real é o @PreAuthorize do backend.
            */}
            <Route element={<TProtected roles={['SUPERADMIN']} />}>
              <Route path="/usuarios" element={<UsuarioList />} />
              <Route path="/usuarios/novo" element={<UsuarioForm />} />
              <Route path="/usuarios/:id" element={<UsuarioForm />} />
              <Route path="/tenants" element={<TenantList />} />
              <Route path="/tenants/:id" element={<TenantForm />} />
              <Route path="/log-acesso" element={<LoginLogList />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
