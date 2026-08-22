import type { SVGProps } from 'react'

/**
 * Ícones em SVG inline, num módulo único.
 *
 * Nenhum componente importa ícone de outro lugar — trocar de família é mexer
 * só aqui. Traço de 1.8 para casar com a direção delicada do doc 05, e
 * `currentColor` para herdar a cor do texto nos dois temas.
 */
type IconProps = SVGProps<SVGSVGElement>

function Icon({ children, ...props }: IconProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      {...props}
    >
      {children}
    </svg>
  )
}

export const IconDashboard = (p: IconProps) => (
  <Icon {...p}>
    <rect x="3" y="3" width="7" height="9" rx="2" />
    <rect x="14" y="3" width="7" height="5" rx="2" />
    <rect x="14" y="12" width="7" height="9" rx="2" />
    <rect x="3" y="16" width="7" height="5" rx="2" />
  </Icon>
)

export const IconUsuarios = (p: IconProps) => (
  <Icon {...p}>
    <path d="M16 19v-1.5a3.5 3.5 0 0 0-3.5-3.5h-5A3.5 3.5 0 0 0 4 17.5V19" />
    <circle cx="10" cy="7.5" r="3.5" />
    <path d="M20 19v-1.5a3.5 3.5 0 0 0-2.6-3.4M15.4 4.6a3.5 3.5 0 0 1 0 5.8" />
  </Icon>
)

export const IconTenants = (p: IconProps) => (
  <Icon {...p}>
    <path d="M3 21h18M5 21V6a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v15M13 21V10h5a1 1 0 0 1 1 1v10" />
    <path d="M8 9h2M8 13h2M8 17h2M16 14h1M16 17h1" />
  </Icon>
)

export const IconLog = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3.5 2" />
  </Icon>
)

export const IconPaciente = (p: IconProps) => (
  <Icon {...p}>
    <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </Icon>
)

/** Mamadeira — a pediatria é reconhecida por ela em qualquer menu. */
export const IconPediatria = (p: IconProps) => (
  <Icon {...p}>
    <path d="M10 2h4M9.5 5h5M8 9h8v9a4 4 0 0 1-4 4 4 4 0 0 1-4-4V9Z" />
    <path d="M11.5 2v3" />
    <path d="M8 13h8" />
  </Icon>
)

/**
 * Terapia nutricional: frasco de dieta com o equipo. É o objeto da beira do
 * leito, e distingue a UTI adulto da mamadeira da pediatria.
 */
export const IconTerapiaNutricional = (p: IconProps) => (
  <Icon {...p}>
    <path d="M9 2h6v4l-1 1v6a2 2 0 0 1-2 2h0a2 2 0 0 1-2-2V7L9 6V2Z" />
    <path d="M12 15v3a3 3 0 0 0 3 3h2" />
    <path d="M10 9h4" />
  </Icon>
)

export const IconPerfil = IconPaciente

export const IconSair = (p: IconProps) => (
  <Icon {...p}>
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
  </Icon>
)

export const IconMenu = (p: IconProps) => (
  <Icon {...p}>
    <path d="M3 6h18M3 12h18M3 18h18" />
  </Icon>
)

export const IconFechar = (p: IconProps) => (
  <Icon {...p}>
    <path d="M18 6 6 18M6 6l12 12" />
  </Icon>
)

export const IconBusca = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </Icon>
)

export const IconAdicionar = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 5v14M5 12h14" />
  </Icon>
)

export const IconAnterior = (p: IconProps) => (
  <Icon {...p}>
    <path d="m15 18-6-6 6-6" />
  </Icon>
)

export const IconProximo = (p: IconProps) => (
  <Icon {...p}>
    <path d="m9 18 6-6-6-6" />
  </Icon>
)

export const IconSeta = (p: IconProps) => (
  <Icon {...p}>
    <path d="m6 9 6 6 6-6" />
  </Icon>
)

export const IconCheck = (p: IconProps) => (
  <Icon {...p}>
    <path d="M20 6 9 17l-5-5" />
  </Icon>
)

export const IconBloqueado = (p: IconProps) => (
  <Icon {...p}>
    <rect x="4" y="10" width="16" height="11" rx="2" />
    <path d="M8 10V7a4 4 0 1 1 8 0v3" />
  </Icon>
)

export const IconDesbloquear = (p: IconProps) => (
  <Icon {...p}>
    <rect x="4" y="10" width="16" height="11" rx="2" />
    <path d="M8 10V7a4 4 0 0 1 7.5-2" />
  </Icon>
)

export const IconAlerta = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 9v4M12 17h.01" />
    <path d="M10.3 3.9 2.4 17a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
  </Icon>
)

export const IconVazio = (p: IconProps) => (
  <Icon {...p}>
    <path d="M3 7.5 12 3l9 4.5v9L12 21l-9-4.5v-9Z" />
    <path d="m3 7.5 9 4.5 9-4.5M12 12v9" />
  </Icon>
)
