#!/usr/bin/env node
// Semeia um cliente de demonstração pela API REST, para gravar os vídeos de
// treinamento. Tudo passa pelos mesmos endpoints que as telas usam: o servidor
// calcula, classifica e grava os retratos como faria com uma digitação real.
//
//   SUPERADMIN_EMAIL=… SUPERADMIN_PASSWORD=… node demo/semear-demo.mjs
//   node demo/semear-demo.mjs --atualizar
//
// A primeira forma cria o cliente "Hospital Demonstração", a nutricionista que
// entra nele e seis meses de histórico. A segunda completa os dias que faltam
// até hoje nas internações em curso — os dados são relativos à data em que o
// script rodou, e sem isso a ronda da tela inicial esvazia em uma semana.
//
// Login tem limite de 5 por minuto por IP: o script faz no máximo três.

import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const API = process.env.API_URL ?? 'http://localhost:8080'
const AQUI = dirname(fileURLToPath(import.meta.url))
const ESTADO = join(AQUI, '.estado-demo.json')

// Credencial de demonstração — só existe neste ambiente local de testes.
export const DEMO = {
  nome: 'Ana Paula Martins',
  email: 'nutricionista@demo.local',
  senha: 'Demo@Nutri2026',
  cliente: 'Hospital Demonstração',
}

// ── HTTP ────────────────────────────────────────────────────────────────────

let token = null

async function api(metodo, caminho, corpo, tk = token) {
  const r = await fetch(API + caminho, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(tk ? { Authorization: `Bearer ${tk}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  const texto = await r.text()
  const json = texto ? JSON.parse(texto) : null
  if (!r.ok) {
    const e = new Error(`${metodo} ${caminho} → ${r.status}: ${json?.erro ?? texto}`)
    e.status = r.status
    throw e
  }
  return json
}

const get = (c) => api('GET', c)
const post = (c, b) => api('POST', c, b)

async function login(email, senha) {
  return api('POST', '/auth/login', { email, senha }, null)
}

// ── Datas: dia do calendário, nunca instante (ver CLAUDE.md) ────────────────

const HOJE = new Date().toLocaleDateString('sv-SE')

function somarDias(iso, n) {
  const d = new Date(iso + 'T00:00:00')
  d.setDate(d.getDate() + n)
  return d.toLocaleDateString('sv-SE')
}

function diasEntre(a, b) {
  return Math.round((new Date(b + 'T00:00:00') - new Date(a + 'T00:00:00')) / 86400000)
}

function mesesCompletos(nasc, em) {
  const [y1, m1, d1] = nasc.split('-').map(Number)
  const [y2, m2, d2] = em.split('-').map(Number)
  return (y2 - y1) * 12 + (m2 - m1) - (d2 < d1 ? 1 : 0)
}

function anosCompletos(nasc, em) {
  return Math.floor(mesesCompletos(nasc, em) / 12)
}

// ── Aleatório determinístico: rodar duas vezes dá os mesmos números ────────

function prng(semente) {
  let a = semente >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const sementeDe = (s) => [...s].reduce((h, c) => (Math.imul(h, 31) + c.charCodeAt(0)) >>> 0, 7)
const r1 = (x) => Math.round(x * 10) / 10
const r2 = (x) => Math.round(x * 100) / 100
const r3 = (x) => Math.round(x * 1000) / 1000
const lerp = (a, b, t) => a + (b - a) * Math.max(0, Math.min(1, t))

// ── Documentos válidos ──────────────────────────────────────────────────────

function cpf(semente) {
  const rnd = prng(semente)
  const d = Array.from({ length: 9 }, () => Math.floor(rnd() * 10))
  if (d.every((x) => x === d[0])) d[8] = (d[8] + 1) % 10
  for (const t of [9, 10]) {
    let soma = 0
    for (let i = 0; i < t; i++) soma += d[i] * (t + 1 - i)
    let resto = (soma * 10) % 11
    if (resto === 10) resto = 0
    d.push(resto)
  }
  return d.join('')
}

function rg(semente) {
  const rnd = prng(semente * 7 + 3)
  return Array.from({ length: 10 }, () => Math.floor(rnd() * 10)).join('').replace(/^0/, '1')
}

function celular(semente) {
  const rnd = prng(semente * 13 + 5)
  return '519' + Array.from({ length: 8 }, () => Math.floor(rnd() * 10)).join('')
}

function emailDe(nome) {
  const partes = nome
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .split(' ')
    .filter((p) => !['de', 'da', 'do', 'dos', 'das'].includes(p))
  return `${partes[0]}.${partes[partes.length - 1]}@example.com`
}

// ── Curvas de referência (mediana OMS) para pesos e estaturas plausíveis ───

const MEDIANA = {
  MASCULINO: {
    peso: { 0: 3.3, 1: 4.5, 2: 5.6, 3: 6.4, 4: 7.0, 5: 7.5, 6: 7.9, 7: 8.3, 8: 8.6, 9: 8.9, 10: 9.2, 11: 9.4, 12: 9.6, 15: 10.3, 18: 10.9, 21: 11.5, 24: 12.2, 30: 13.3, 36: 14.3, 42: 15.3, 48: 16.3, 54: 17.3, 60: 18.3 },
    est: { 0: 49.9, 1: 54.7, 2: 58.4, 3: 61.4, 4: 63.9, 5: 65.9, 6: 67.6, 7: 69.2, 8: 70.6, 9: 72.0, 10: 73.3, 11: 74.5, 12: 75.7, 15: 79.1, 18: 82.3, 21: 85.1, 24: 87.1, 30: 91.9, 36: 96.1, 42: 99.9, 48: 103.3, 54: 106.7, 60: 110.0 },
  },
  FEMININO: {
    peso: { 0: 3.2, 1: 4.2, 2: 5.1, 3: 5.8, 4: 6.4, 5: 6.9, 6: 7.3, 7: 7.6, 8: 7.9, 9: 8.2, 10: 8.5, 11: 8.7, 12: 8.9, 15: 9.6, 18: 10.2, 21: 10.9, 24: 11.5, 30: 12.7, 36: 13.9, 42: 15.0, 48: 16.1, 54: 17.2, 60: 18.2 },
    est: { 0: 49.1, 1: 53.7, 2: 57.1, 3: 59.8, 4: 62.1, 5: 64.0, 6: 65.7, 7: 67.3, 8: 68.7, 9: 70.1, 10: 71.5, 11: 72.8, 12: 74.0, 15: 77.5, 18: 80.7, 21: 83.7, 24: 85.7, 30: 90.7, 36: 95.1, 42: 99.0, 48: 102.7, 54: 106.2, 60: 109.4 },
  },
}

function mediana(sexo, tipo, idadeMeses) {
  const tabela = MEDIANA[sexo][tipo]
  const xs = Object.keys(tabela).map(Number)
  for (let i = 0; i < xs.length - 1; i++) {
    if (idadeMeses <= xs[i + 1]) {
      const t = (idadeMeses - xs[i]) / (xs[i + 1] - xs[i])
      return lerp(tabela[xs[i]], tabela[xs[i + 1]], t)
    }
  }
  return tabela[60]
}

const idadeFracionada = (nasc, em) => diasEntre(nasc, em) / 30.4375

// ── As pessoas ──────────────────────────────────────────────────────────────

const ENDERECOS = [
  ['Porto Alegre', '90035-003', 'Rua Ramiro Barcelos', '1850', 'Bom Fim'],
  ['Porto Alegre', '90619-900', 'Avenida Ipiranga', '6681', 'Partenon'],
  ['Porto Alegre', '90570-020', 'Rua Padre Chagas', '342', 'Moinhos de Vento'],
  ['Porto Alegre', '91350-200', 'Avenida Assis Brasil', '3940', 'São Sebastião'],
  ['Canoas', '92010-000', 'Avenida Guilherme Schell', '5340', 'Centro'],
  ['Canoas', '92425-020', 'Rua Boqueirão', '1102', 'Estância Velha'],
  ['Novo Hamburgo', '93510-010', 'Rua Júlio de Castilhos', '415', 'Centro'],
  ['São Leopoldo', '93010-190', 'Rua Independência', '780', 'Centro'],
  ['Porto Alegre', '90880-480', 'Avenida Teresópolis', '2550', 'Teresópolis'],
  ['Gravataí', '94010-000', 'Avenida José Loureiro da Silva', '1630', 'Centro'],
]

const PROFISSIONAIS = [
  { chave: 'ana', nome: 'Ana Paula Martins', nasc: '1988-03-14', sexo: 'FEMININO', obs: 'Nutricionista clínica — CRN-2 12345' },
  { chave: 'rodrigo', nome: 'Rodrigo Mendes Teixeira', nasc: '1985-11-02', sexo: 'MASCULINO', obs: 'Nutricionista da UTI adulto — CRN-2 23456' },
  { chave: 'camila', nome: 'Camila Duarte Ribeiro', nasc: '1996-07-21', sexo: 'FEMININO', obs: 'Nutricionista residente em pediatria — CRN-2 34567' },
]

const RESPONSAVEIS = [
  { chave: 'juliana', nome: 'Juliana Fernandes Costa', nasc: '1993-05-08', sexo: 'FEMININO' },
  { chave: 'patricia', nome: 'Patrícia Oliveira Santos', nasc: '1998-01-27', sexo: 'FEMININO' },
  { chave: 'marcos', nome: 'Marcos Rocha Lima', nasc: '1989-09-30', sexo: 'MASCULINO' },
  { chave: 'fernanda', nome: 'Fernanda Pereira Gomes', nasc: '1995-12-12', sexo: 'FEMININO' },
  { chave: 'carla', nome: 'Carla Almeida Barros', nasc: '1990-04-03', sexo: 'FEMININO' },
  { chave: 'renata', nome: 'Renata Carvalho Nunes', nasc: '1997-08-19', sexo: 'FEMININO' },
]

// Cada criança: avaliações com o fator sobre a mediana (1 = mediana), a dieta,
// e o acompanhamento diário, quando houver.
const CRIANCAS = [
  {
    chave: 'laura', nome: 'Laura Fernandes Costa', nasc: '2026-01-20', sexo: 'FEMININO', resp: 'juliana',
    obs: 'Acompanhada desde os 4 meses.',
    avaliacoes: [
      { data: '2026-06-10', fp: 1.02, fe: 1.0, formula: 'NAN 1', vol: 150, freq: 4, prof: 'camila', obs: 'Aleitamento misto. Mãe retornou ao trabalho; complementa com fórmula.' },
      { data: '2026-08-12', fp: 1.03, fe: 1.0, formula: 'NAN 2', vol: 200, freq: 6, prof: 'camila', obs: 'Iniciou a introdução alimentar aos 6 meses. Boa aceitação de papas.' },
      { data: '2026-09-20', fp: 1.15, fe: 1.0, formula: 'NAN 2', vol: 210, freq: 6, prof: 'ana', obs: 'Internada por bronquiolite. Ganho de peso acima do esperado no último mês — rever volume ofertado.' },
    ],
    diario: { dias: 12, ganhoG: 12, adesao: [0.82, 0.98], tomadas: [0.75, 1] },
  },
  {
    chave: 'miguel', nome: 'Miguel Oliveira Santos', nasc: '2026-07-05', sexo: 'MASCULINO', resp: 'patricia',
    obs: 'Prematuro de 34 semanas. Alergia à proteína do leite de vaca (APLV).',
    avaliacoes: [
      { data: '2026-08-20', fp: 0.8, fe: 0.97, formula: 'PREGOMIN', vol: 80, freq: 3, prof: 'camila', obs: 'APLV confirmada. Iniciada fórmula extensamente hidrolisada.' },
      { data: '2026-09-18', fp: 0.8, fe: 0.97, formula: 'PREGOMIN', vol: 100, freq: 3, prof: 'camila', obs: 'Reinternação por diarreia. Manter fórmula e acompanhar ganho ponderal diário.' },
    ],
    diario: { dias: 14, ganhoG: 28, adesao: [0.85, 1], tomadas: [0.75, 1] },
  },
  {
    chave: 'helena', nome: 'Helena Rocha Lima', nasc: '2025-10-15', sexo: 'FEMININO', resp: 'marcos',
    obs: 'Cardiopatia congênita (CIV), aguarda correção cirúrgica.',
    avaliacoes: [
      { data: '2026-05-05', fp: 0.83, fe: 0.98, formula: 'INFATRINE', vol: 110, freq: 4, prof: 'ana', obs: 'Restrição hídrica pela cardiopatia. Fórmula hipercalórica para recuperar o peso.' },
      { data: '2026-09-16', fp: 0.84, fe: 0.98, formula: 'INFATRINE', vol: 180, freq: 6, prof: 'ana', obs: 'Pré-operatório. Cansaço às mamadas; avaliar sonda se a aceitação cair.' },
    ],
    diario: { dias: 15, ganhoG: 15, adesao: [0.7, 0.92], tomadas: [0.5, 1], pendenteHoje: true },
  },
  {
    chave: 'arthur', nome: 'Arthur Pereira Gomes', nasc: '2024-08-10', sexo: 'MASCULINO', resp: 'fernanda',
    obs: 'Desnutrição após internações por pneumonia de repetição.',
    avaliacoes: [
      { data: '2026-04-15', fp: 0.84, fe: 0.97, formula: 'FORTINI', vol: 200, freq: 8, prof: 'camila', obs: 'Desnutrição moderada. Suplementação com fórmula pediátrica hipercalórica.' },
      { data: '2026-07-20', fp: 0.9, fe: 0.97, formula: 'FORTINI', vol: 200, freq: 8, prof: 'camila', obs: 'Recuperação ponderal. Manter suplementação mais 60 dias.' },
    ],
  },
  {
    chave: 'sofia', nome: 'Sofia Almeida Barros', nasc: '2022-09-20', sexo: 'FEMININO', resp: 'carla',
    obs: 'Excesso de peso. Acompanhamento ambulatorial.',
    avaliacoes: [
      { data: '2026-05-12', fp: 1.22, fe: 1.01, prof: 'ana', obs: 'Consumo elevado de ultraprocessados e bebidas açucaradas. Orientada a família.' },
      { data: '2026-09-30', fp: 1.2, fe: 1.01, prof: 'ana', obs: 'Manteve o peso com o crescimento. Reforçar plano alimentar.' },
    ],
  },
  {
    chave: 'davi', nome: 'Davi Carvalho Nunes', nasc: '2025-12-01', sexo: 'MASCULINO', resp: 'renata',
    obs: 'APLV com sintomas gastrointestinais.',
    avaliacoes: [
      { data: '2026-04-20', fp: 1.0, fe: 1.0, formula: 'NEOCATE LCD', vol: 150, freq: 4, prof: 'camila', obs: 'Fórmula de aminoácidos após falha da extensamente hidrolisada.' },
      { data: '2026-07-01', fp: 1.01, fe: 1.0, formula: 'NEOCATE LCD', vol: 200, freq: 6, prof: 'camila', obs: 'Introdução alimentar sem leite e derivados. Boa evolução.' },
    ],
  },
  {
    chave: 'valentina', nome: 'Valentina Souza Moreira', nasc: '2025-03-10', sexo: 'FEMININO',
    avaliacoes: [
      { data: '2026-06-18', fp: 1.05, fe: 1.01, formula: 'NESTOGENO 2', vol: 200, freq: 8, prof: 'ana', obs: 'Consulta de rotina. Alimentação da família, fórmula pela manhã, à tarde e à noite.' },
      { data: '2026-09-10', fp: 1.06, fe: 1.01, formula: 'NESTOGENO 2', vol: 200, freq: 12, prof: 'ana', obs: 'Reduzida a fórmula para duas tomadas.' },
    ],
  },
  {
    chave: 'bernardo', nome: 'Bernardo Lima Araújo', nasc: '2026-04-02', sexo: 'MASCULINO',
    avaliacoes: [
      { data: '2026-05-02', fp: 1.0, fe: 1.0, formula: 'NAN 1', vol: 90, freq: 3, prof: 'camila', obs: 'Hipogalactia materna. Fórmula de partida em complemento.' },
      { data: '2026-07-03', fp: 0.99, fe: 1.0, formula: 'NAN 1', vol: 150, freq: 4, prof: 'camila', obs: 'Crescimento adequado.' },
    ],
  },
  {
    chave: 'alice', nome: 'Alice Ribeiro Martins', nasc: '2024-11-25', sexo: 'FEMININO',
    avaliacoes: [
      { data: '2026-08-25', fp: 0.95, fe: 0.99, formula: 'NAN SL', vol: 200, freq: 8, prof: 'ana', obs: 'Diarreia persistente após gastroenterite. Fórmula sem lactose por 4 semanas.' },
    ],
  },
]

// Adultos de UTI. `aval` leva o corpo de /uti/calculo; a vazão é resolvida
// pela meta energética que o servidor devolve, como faria a nutricionista.
const ADULTOS = [
  {
    chave: 'jose', nome: 'José Carlos Silveira', nasc: '1954-03-11', sexo: 'MASCULINO',
    obs: 'Pneumonia comunitária grave com sepse. Hipertenso.',
    avaliacoes: [
      { data: '2026-09-12', prof: 'rodrigo', formula: 'Fresubin HP Energy', modulo: 'Nutren Just Protein',
        calc: { etnia: 'BRANCA', alturaCm: 170, pesoAtualKg: 78, pesoUsualKg: 82, janelaPerda: 'UM_MES', alturaJoelhoCm: 52, circBracoCm: 28, circPanturrilhaCm: 33, circAbdominalCm: 102, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Admissão na UTI em ventilação mecânica. Dieta enteral por SNE com progressão em 4 dias.' },
      { data: '2026-09-22', prof: 'rodrigo', formula: 'Fresubin HP Energy', modulo: 'Nutren Just Protein',
        calc: { etnia: 'BRANCA', alturaCm: 170, pesoAtualKg: 75.5, pesoUsualKg: 82, janelaPerda: 'UM_MES', alturaJoelhoCm: 52, circBracoCm: 27, circPanturrilhaCm: 32, circAbdominalCm: 100, fase: 'REABILITACAO', terapiaRenal: 'NENHUMA' },
        obs: 'Extubado. Fase de reabilitação: metas plenas. Iniciada dieta oral pastosa em transição.' },
    ],
    internacao: { inicio: '2026-09-13', fim: null, hojeRegistrado: true, perfil: 'sepse', adesao: [0.86, 0.97], oralDesde: 15 },
  },
  {
    chave: 'maria', nome: 'Maria de Lourdes Becker', nasc: '1945-05-20', sexo: 'FEMININO',
    obs: 'AVC isquêmico com disfagia. Diabética.',
    avaliacoes: [
      { data: '2026-09-20', prof: 'ana', formula: 'Novasource Senior', modulo: 'Fresubin Protein Powder',
        calc: { etnia: 'BRANCA', alturaCm: 158, pesoAtualKg: 52, pesoUsualKg: 60, janelaPerda: 'TRES_MESES', alturaJoelhoCm: 48, circBracoCm: 22, circPanturrilhaCm: 29, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Disfagia grave após AVC. Dieta por SNE. Perda de 13 % do peso em 3 meses.' },
    ],
    internacao: { inicio: '2026-09-21', fim: null, hojeRegistrado: false, perfil: 'avc', adesao: [0.48, 0.72] },
  },
  {
    chave: 'antonio', nome: 'Antônio Marcos Vieira', nasc: '1968-02-02', sexo: 'MASCULINO',
    obs: 'Pancreatite aguda grave. Obesidade grau III.',
    avaliacoes: [
      { data: '2026-09-25', prof: 'rodrigo', formula: 'Peptamen Intense',
        calc: { etnia: 'BRANCA', alturaCm: 175, pesoAtualKg: 138, pesoUsualKg: 140, janelaPerda: 'UM_MES', circBracoCm: 40, circPanturrilhaCm: 45, circAbdominalCm: 142, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Obeso (IMC 45). Estratégia hipocalórica e hiperproteica. Fórmula peptídica por sonda nasojejunal.' },
    ],
    internacao: { inicio: '2026-09-26', fim: null, hojeRegistrado: true, perfil: 'pancreatite', adesao: [0.85, 0.97] },
  },
  {
    chave: 'rosangela', nome: 'Rosângela Pires Machado', nasc: '1962-06-30', sexo: 'FEMININO',
    obs: 'Choque séptico de foco urinário com lesão renal aguda.',
    avaliacoes: [
      { data: '2026-09-22', prof: 'ana', formula: 'Fresubin HP Energy', modulo: 'Nutren Just Protein',
        calc: { etnia: 'BRANCA', alturaCm: 162, pesoAtualKg: 70, pesoUsualKg: 67, janelaPerda: 'UM_MES', circBracoCm: 29, circPanturrilhaCm: 34, fase: 'AGUDA', terapiaRenal: 'HEMODIALISE_CONTINUA' },
        obs: 'Em hemodiálise contínua (CVVHDF). Necessidade proteica de 2,0 g/kg. Peso com edema.' },
    ],
    internacao: { inicio: '2026-09-23', fim: null, hojeRegistrado: false, perfil: 'renal', adesao: [0.8, 0.95] },
  },
  {
    chave: 'claudia', nome: 'Cláudia Regina Fontoura', nasc: '1971-11-22', sexo: 'FEMININO',
    obs: 'Cetoacidose diabética. Diabetes tipo 2.',
    avaliacoes: [
      { data: '2026-10-01', prof: 'rodrigo', formula: 'Diben HP',
        calc: { etnia: 'BRANCA', alturaCm: 160, pesoAtualKg: 88, pesoUsualKg: 90, janelaPerda: 'TRES_MESES', circBracoCm: 34, circPanturrilhaCm: 38, circAbdominalCm: 112, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Admissão de ontem. Fórmula para controle glicêmico, início a 25 % da meta.' },
    ],
    internacao: { inicio: '2026-10-02', fim: null, hojeRegistrado: true, perfil: 'diabetes', adesao: [0.9, 1] },
  },
  {
    chave: 'paulo', nome: 'Paulo Roberto Nascimento', nasc: '1959-01-15', sexo: 'MASCULINO',
    obs: 'Insuficiência cardíaca descompensada. Acamado.',
    avaliacoes: [
      { data: '2026-07-08', prof: 'rodrigo', formula: 'Fresubin Original',
        calc: { etnia: 'NEGRA', alturaJoelhoCm: 55, circBracoCm: 30, circPanturrilhaCm: 34, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Sem condições de pesar nem medir. Peso e altura estimados pela altura do joelho e pelas circunferências.' },
    ],
    internacao: { inicio: '2026-07-09', fim: '2026-07-20', encerramento: { em: '2026-07-21', motivo: 'ALTA_HOSPITALAR', obs: 'Alta para a enfermaria da cardiologia.' }, perfil: 'cardio', adesao: [0.82, 0.96] },
  },
  {
    chave: 'luiz', nome: 'Luiz Fernando Kraemer', nasc: '1981-04-07', sexo: 'MASCULINO',
    obs: 'Politrauma por acidente de moto. Amputação transtibial direita.',
    avaliacoes: [
      { data: '2026-05-10', prof: 'rodrigo', formula: 'Impact 1.5',
        calc: { etnia: 'BRANCA', alturaCm: 180, pesoAtualKg: 82, pesoUsualKg: 86, janelaPerda: 'UMA_SEMANA', circBracoCm: 31, circPanturrilhaCm: 36, segmentosAmputados: ['PERNA'], fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Pós-operatório de amputação de perna direita. Fórmula imunomoduladora no trauma.' },
      { data: '2026-05-20', prof: 'rodrigo', formula: 'Fresubin HP Energy', modulo: 'Nutren Just Protein',
        calc: { etnia: 'BRANCA', alturaCm: 180, pesoAtualKg: 79, pesoUsualKg: 86, janelaPerda: 'UM_MES', circBracoCm: 30, circPanturrilhaCm: 35, segmentosAmputados: ['PERNA'], fase: 'REABILITACAO', terapiaRenal: 'NENHUMA' },
        obs: 'Reabilitação. Metas plenas. Fisioterapia motora iniciada.' },
    ],
    internacao: { inicio: '2026-05-11', fim: '2026-05-28', encerramento: { em: '2026-05-29', motivo: 'TRANSFERENCIA', obs: 'Transferido para hospital de reabilitação.' }, perfil: 'trauma', adesao: [0.85, 0.98], oralDesde: 13 },
  },
  {
    chave: 'terezinha', nome: 'Terezinha Castro Lopes', nasc: '1942-08-03', sexo: 'FEMININO',
    obs: 'Neoplasia gástrica avançada. Diabetes tipo 2.',
    avaliacoes: [
      { data: '2026-06-02', prof: 'ana', formula: 'Novasource GC',
        calc: { etnia: 'BRANCA', alturaCm: 155, pesoAtualKg: 48, pesoUsualKg: 55, janelaPerda: 'SEIS_MESES', circBracoCm: 21, circPanturrilhaCm: 28, fase: 'AGUDA', terapiaRenal: 'NENHUMA', reguaImcIdoso: 'OPAS_2002' },
        obs: 'Caquexia oncológica. Família ciente do prognóstico. Dieta enteral de conforto.' },
    ],
    internacao: { inicio: '2026-06-03', fim: '2026-06-12', encerramento: { em: '2026-06-13', motivo: 'OBITO', obs: 'Óbito em cuidados paliativos.' }, perfil: 'oncologico', adesao: [0.55, 0.85] },
  },
  {
    chave: 'eduarda', nome: 'Eduarda Sampaio Reis', nasc: '1994-02-18', sexo: 'FEMININO',
    obs: 'Pós-operatório de colectomia por doença de Crohn.',
    avaliacoes: [
      { data: '2026-04-14', prof: 'rodrigo', formula: 'Nutrison Advanced Peptisorb',
        calc: { etnia: 'BRANCA', alturaCm: 165, pesoAtualKg: 61, pesoUsualKg: 64, janelaPerda: 'UM_MES', circBracoCm: 26, circPanturrilhaCm: 33, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Pós-operatório imediato. Fórmula oligomérica por sonda nasoentérica.' },
    ],
    internacao: { inicio: '2026-04-15', fim: '2026-04-24', encerramento: { em: '2026-04-25', motivo: 'ALTA_HOSPITALAR', obs: 'Alta da UTI com dieta oral.' }, perfil: 'cirurgico', adesao: [0.85, 0.98], oralDesde: 8 },
  },
  {
    chave: 'sergio', nome: 'Sérgio Augusto Pinheiro', nasc: '1950-09-09', sexo: 'MASCULINO',
    obs: 'DPOC exacerbada com insuficiência respiratória.',
    avaliacoes: [
      { data: '2026-08-05', prof: 'ana', formula: 'Fresubin Original',
        calc: { etnia: 'BRANCA', alturaCm: 168, pesoAtualKg: 59, pesoUsualKg: 66, janelaPerda: 'TRES_MESES', circBracoCm: 24, circPanturrilhaCm: 31, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Desnutrido. Ventilação mecânica. Atenção à síndrome de realimentação.' },
      { data: '2026-08-15', prof: 'ana', formula: 'Fresubin HP Energy', intermitente: 6, modulo: 'Nutren Just Protein',
        calc: { etnia: 'BRANCA', alturaCm: 168, pesoAtualKg: 58, pesoUsualKg: 66, janelaPerda: 'TRES_MESES', circBracoCm: 24, circPanturrilhaCm: 31, fase: 'REABILITACAO', terapiaRenal: 'NENHUMA' },
        obs: 'Traqueostomizado. Dieta intermitente em 6 horários para treino de desmame.' },
    ],
    internacao: { inicio: '2026-08-06', fim: '2026-08-24', encerramento: { em: '2026-08-25', motivo: 'ALTA_HOSPITALAR', obs: 'Alta para a enfermaria da pneumologia.' }, perfil: 'dpoc', adesao: [0.8, 0.95], oralDesde: 16 },
  },
  {
    chave: 'ivo', nome: 'Ivo Hoffmann', nasc: '1955-03-10', sexo: 'MASCULINO',
    obs: 'Pós-operatório de revascularização do miocárdio.',
    avaliacoes: [
      { data: '2026-04-28', prof: 'rodrigo', formula: 'Isosource 1.5', intermitente: 6,
        calc: { etnia: 'BRANCA', alturaCm: 172, pesoAtualKg: 70, pesoUsualKg: 71, janelaPerda: 'UM_MES', circBracoCm: 29, circPanturrilhaCm: 35, fase: 'AGUDA', terapiaRenal: 'NENHUMA' },
        obs: 'Dieta intermitente por SNE até retorno da deglutição.' },
    ],
    internacao: { inicio: '2026-04-29', fim: '2026-05-07', encerramento: { em: '2026-05-08', motivo: 'ALTA_HOSPITALAR', obs: 'Alta da UTI.' }, perfil: 'cirurgico', adesao: [0.88, 1], oralDesde: 6 },
  },
  {
    chave: 'neusa', nome: 'Neusa Terezinha Wolff', nasc: '1947-02-14', sexo: 'FEMININO',
    obs: 'Fratura de fêmur. Hipotireoidismo.',
    avaliacoes: [
      { data: '2026-08-28', prof: 'ana', formula: 'Fresubin Original Fibre',
        calc: { etnia: 'BRANCA', alturaCm: 152, pesoAtualKg: 63, pesoUsualKg: 64, janelaPerda: 'TRES_MESES', circBracoCm: 27, circPanturrilhaCm: 32, fase: 'REABILITACAO', terapiaRenal: 'NENHUMA', kcalPorKgAlvo: 25 },
        obs: 'Pós-operatório de artroplastia. Complementação enteral noturna.' },
    ],
    internacao: { inicio: '2026-08-29', fim: '2026-09-08', encerramento: { em: '2026-09-09', motivo: 'ALTA_HOSPITALAR', obs: 'Alta hospitalar com suplemento oral.' }, perfil: 'ortopedico', adesao: [0.85, 1], oralDesde: 4 },
  },
]

// ── Perfis de evolução clínica dos dias de UTI ──────────────────────────────
// [início, fim] de cada exame ao longo da internação; o dia interpola entre os
// dois, com um ruído pequeno. Ventilação: dia a partir do qual vale cada suporte.

const PERFIS = {
  sepse: { pcr: [22, 3.5], lac: [4.2, 1.1], k: [3.3, 4.2], na: [134, 139], mg: [1.6, 2.0], ph: [7.29, 7.41], pco2: [47, 40], hco3: [19, 25], hgt: [195, 145], bal: [1800, -400], diu: [0.5, 1.2], pas: [92, 128], pad: [52, 76],
    vent: [[1, 'VENTILACAO_MECANICA', 50], [5, 'VENTILACAO_MECANICA', 35], [8, 'VNI', 40], [10, 'CATETER_NASAL', null], [15, 'AR_AMBIENTE', null]] },
  avc: { pcr: [6, 2.5], lac: [1.4, 1.0], k: [3.9, 4.1], na: [148, 141], mg: [1.9, 2.0], ph: [7.4, 7.42], pco2: [38, 39], hco3: [24, 25], hgt: [210, 165], bal: [600, 100], diu: [0.8, 1.0], pas: [168, 138], pad: [92, 80],
    vent: [[1, 'CATETER_NASAL', null]] },
  pancreatite: { pcr: [28, 9], lac: [3.1, 1.2], k: [3.6, 4.0], na: [136, 140], mg: [1.5, 1.9], ph: [7.32, 7.4], pco2: [42, 40], hco3: [20, 24], hgt: [230, 160], bal: [2500, 300], diu: [0.6, 1.1], pas: [100, 132], pad: [58, 78],
    vent: [[1, 'MASCARA', null], [4, 'CATETER_NASAL', null]] },
  renal: { pcr: [19, 6], lac: [4.8, 1.5], k: [5.8, 4.4], na: [133, 138], mg: [2.4, 2.1], ph: [7.24, 7.37], pco2: [36, 39], hco3: [15, 22], hgt: [170, 150], bal: [1200, -900], diu: [0.15, 0.3], pas: [88, 120], pad: [48, 70],
    vent: [[1, 'VENTILACAO_MECANICA', 45], [6, 'VENTILACAO_MECANICA', 30]] },
  diabetes: { pcr: [4, 2], lac: [2.6, 1.2], k: [3.2, 4.1], na: [131, 138], mg: [1.6, 1.9], ph: [7.18, 7.38], pco2: [28, 38], hco3: [11, 22], hgt: [320, 170], bal: [2200, 400], diu: [1.8, 1.0], pas: [104, 126], pad: [60, 74],
    vent: [[1, 'AR_AMBIENTE', null]] },
  cardio: { pcr: [3, 1.5], lac: [2.2, 1.0], k: [3.7, 4.3], na: [132, 137], mg: [1.7, 2.0], ph: [7.36, 7.41], pco2: [44, 40], hco3: [24, 26], hgt: [140, 120], bal: [-200, -800], diu: [1.2, 0.9], pas: [102, 118], pad: [64, 72],
    vent: [[1, 'VNI', 40], [4, 'CATETER_NASAL', null], [9, 'AR_AMBIENTE', null]] },
  trauma: { pcr: [24, 4], lac: [3.6, 1.0], k: [3.5, 4.2], na: [138, 140], mg: [1.6, 2.0], ph: [7.33, 7.42], pco2: [44, 39], hco3: [21, 25], hgt: [180, 125], bal: [2000, -300], diu: [0.7, 1.3], pas: [96, 130], pad: [55, 80],
    vent: [[1, 'VENTILACAO_MECANICA', 40], [6, 'VNI', 35], [8, 'CATETER_NASAL', null], [12, 'AR_AMBIENTE', null]] },
  oncologico: { pcr: [14, 21], lac: [1.8, 3.4], k: [3.6, 3.4], na: [136, 132], mg: [1.7, 1.6], ph: [7.38, 7.31], pco2: [40, 44], hco3: [23, 20], hgt: [175, 190], bal: [300, 900], diu: [0.7, 0.3], pas: [110, 86], pad: [66, 48],
    vent: [[1, 'CATETER_NASAL', null], [7, 'MASCARA', null]] },
  cirurgico: { pcr: [16, 3], lac: [2.4, 1.0], k: [3.6, 4.1], na: [137, 140], mg: [1.6, 2.0], ph: [7.36, 7.41], pco2: [41, 39], hco3: [22, 25], hgt: [165, 120], bal: [1500, -200], diu: [0.8, 1.2], pas: [104, 124], pad: [62, 76],
    vent: [[1, 'VENTILACAO_MECANICA', 40], [2, 'CATETER_NASAL', null], [5, 'AR_AMBIENTE', null]] },
  dpoc: { pcr: [12, 2.5], lac: [1.9, 1.0], k: [3.4, 4.0], na: [135, 139], mg: [1.6, 1.9], ph: [7.27, 7.39], pco2: [68, 48], hco3: [30, 28], hgt: [150, 125], bal: [900, -300], diu: [0.8, 1.1], pas: [110, 130], pad: [64, 78],
    vent: [[1, 'VENTILACAO_MECANICA', 40], [7, 'TRAQUEOSTOMIA', 35], [13, 'TRAQUEOSTOMIA', 28], [16, 'CATETER_NASAL', null]] },
  ortopedico: { pcr: [9, 2], lac: [1.6, 1.0], k: [3.8, 4.1], na: [136, 139], mg: [1.8, 2.0], ph: [7.39, 7.41], pco2: [40, 40], hco3: [24, 25], hgt: [130, 115], bal: [800, 0], diu: [0.9, 1.1], pas: [124, 132], pad: [72, 78],
    vent: [[1, 'CATETER_NASAL', null], [3, 'AR_AMBIENTE', null]] },
}

const OBS_DIA_RUIM = [
  'Dieta pausada por 6 h para tomografia.',
  'Vômito após a troca de decúbito; dieta suspensa por 4 h.',
  'Sonda deslocada, repassada às 14 h.',
  'Jejum para procedimento (traqueostomia).',
  'Resíduo gástrico elevado; vazão reduzida pela metade por 8 h.',
  'Distensão abdominal; dieta suspensa até avaliação médica.',
]

function diaDeUti(paciente, aval, i, totalDias, rnd) {
  const p = PERFIS[paciente.internacao.perfil]
  const t = totalDias > 1 ? (i - 1) / Math.min(totalDias - 1, 14) : 0
  const ruido = (amp) => (rnd() - 0.5) * 2 * amp
  const v = ([a, b], amp, casas = 1) => {
    const x = lerp(a, b, t) + ruido(amp)
    return casas === 2 ? r2(x) : casas === 0 ? Math.round(x) : r1(x)
  }

  // Progressão 25/50/75/100 % nos quatro primeiros dias da internação.
  const prog = i <= 4 && !aval.reavaliacao ? [0.25, 0.5, 0.75, 1][i - 1] : 1
  const prescrito = Math.round(aval.volumeTotal * prog)
  let [aMin, aMax] = paciente.internacao.adesao
  let obs = null
  let adesao = aMin + rnd() * (aMax - aMin)
  if (rnd() < 0.12) {
    adesao *= 0.55
    obs = OBS_DIA_RUIM[Math.floor(rnd() * OBS_DIA_RUIM.length)]
  }
  const recebido = Math.round(prescrito * Math.min(adesao, 1))

  let vent = p.vent[0]
  for (const fase of p.vent) if (i >= fase[0]) vent = fase
  const fio2 = vent[2] != null ? Math.round(vent[2] + ruido(3)) : null

  const peso = aval.calc.pesoAtualKg ?? aval.pesoTrabalho
  const diurese = Math.max(0, Math.round(peso * 24 * v(p.diu, 0.1, 2)))
  const gasometria = vent[1] === 'VENTILACAO_MECANICA' || vent[1] === 'TRAQUEOSTOMIA' || i % 3 === 1
  const dia = {
    dieta: `${aval.formula} · ${paciente.internacao.perfil === 'pancreatite' ? 'SNJ' : 'SNE'} · ${aval.descricao}`,
    volPrescrito24h: prescrito,
    volRecebido24h: recebido,
    k: v(p.k, 0.2),
    na: v(p.na, 1.5, 0),
    hgt: v(p.hgt, 18, 0),
    mg: i % 2 === 1 ? v(p.mg, 0.1) : null,
    pcr: i % 2 === 1 ? Math.max(0.2, v(p.pcr, 1.2)) : null,
    lactato: gasometria ? Math.max(0.5, v(p.lac, 0.2)) : null,
    ph: gasometria ? v(p.ph, 0.015, 2) : null,
    pco2: gasometria ? v(p.pco2, 2, 0) : null,
    hco3: gasometria ? v(p.hco3, 1) : null,
    suporteVentilatorio: vent[1],
    fio2Perc: fio2,
    paSistolica: v(p.pas, 6, 0),
    paDiastolica: v(p.pad, 4, 0),
    balancoHidricoMl: Math.round(lerp(p.bal[0], p.bal[1], t) + ruido(250)),
    diureseMl: diurese,
    evacuacao: i <= 3 ? 'Ausente' : ['Ausente', '1x pastosa', '1x formada', '2x pastosa'][Math.floor(rnd() * 4)],
    observacao: obs,
  }
  const oralDesde = paciente.internacao.oralDesde
  if (oralDesde && i >= oralDesde) {
    const base = Math.min(100, 25 + (i - oralDesde) * 12)
    const ref = () => Math.min(100, Math.max(0, Math.round((base + ruido(20)) / 25) * 25))
    Object.assign(dia, { cafeManha: ref(), lancheManha: ref(), almoco: ref(), lancheTarde: ref(), jantar: ref(), ceia: ref() })
  }
  return dia
}

function diaPediatrico(crianca, aval, i, rnd) {
  const d = crianca.diario
  const ruido = (amp) => (rnd() - 0.5) * 2 * amp
  const peso = r3(aval.peso + (d.ganhoG * i) / 1000 + ruido(0.02))
  const vezes = Math.round(24 / aval.freq)
  const prescrito = aval.vol * vezes
  const adesao = d.adesao[0] + rnd() * (d.adesao[1] - d.adesao[0])
  const aceitas = Math.min(vezes, Math.max(0, Math.round(vezes * (d.tomadas[0] + rnd() * (d.tomadas[1] - d.tomadas[0])))))
  const obs = rnd() < 0.25
    ? ['Regurgitação após a mamada da tarde.', 'Recusou a tomada da madrugada.', 'Aceitou bem todas as tomadas.', 'Sonolenta, mamou devagar.', 'Evacuou 3 vezes, fezes pastosas.'][Math.floor(rnd() * 5)]
    : null
  const dataDia = somarDias(aval.data, i)
  return {
    pesoKg: peso,
    estaturaCm: i % 7 === 0 ? r1(mediana(crianca.sexo, 'est', idadeFracionada(crianca.nasc, dataDia)) * aval.fe) : null,
    volPrescrito24h: prescrito,
    volRecebido24h: Math.round(prescrito * adesao),
    tomadasPrevistas: vezes,
    tomadasAceitas: aceitas,
    observacao: obs,
  }
}

// ── Fichas de anamnese ──────────────────────────────────────────────────────

const FICHAS = [
  // NRS-2002: [IMC<20,5, perdeu peso, comeu menos, doença grave], estado, gravidade
  { modelo: 'NRS', paciente: 'jose', data: '2026-09-12', prof: 'rodrigo', nrs: [false, true, true, true, 2, 3], obs: 'Triagem na admissão da UTI.' },
  { modelo: 'NRS', paciente: 'maria', data: '2026-09-20', prof: 'ana', nrs: [false, true, true, true, 3, 2], obs: 'Perda de 13 % em 3 meses e disfagia.' },
  { modelo: 'NRS', paciente: 'antonio', data: '2026-09-25', prof: 'rodrigo', nrs: [false, false, true, true, 1, 3] },
  { modelo: 'NRS', paciente: 'rosangela', data: '2026-09-22', prof: 'ana', nrs: [false, false, true, true, 1, 3] },
  { modelo: 'NRS', paciente: 'claudia', data: '2026-10-01', prof: 'rodrigo', nrs: [false, false, true, true, 1, 2] },
  { modelo: 'NRS', paciente: 'neusa', data: '2026-09-08', prof: 'ana', nrs: [false, false, false, false], obs: 'Reavaliação antes da alta: sem critério de risco.' },
  // MNA: as letras A..R, na ordem das opções (0 = primeira opção)
  { modelo: 'MNA', paciente: 'maria', data: '2026-09-21', prof: 'ana', mna: [1, 0, 0, 0, 2, 1, 0, 0, 1, 2, 1, 1, 1, 0, 1, 0, 1, 0], obs: 'Informações prestadas pela filha.' },
  { modelo: 'MNA', paciente: 'sergio', data: '2026-08-16', prof: 'ana', mna: [1, 2, 0, 0, 2, 0, 1, 0, 1, 2, 1, 1, 1, 1, 1, 0, 0, 0] },
  { modelo: 'MNA', paciente: 'neusa', data: '2026-09-02', prof: 'ana', mna: [2, 3, 1, 0, 2, 3, 1, 0, 1, 2, 2, 1, 1, 2, 2, 2, 2, 1] },
  { modelo: 'MNA', paciente: 'ivo', data: '2026-05-01', prof: 'rodrigo', mna: [2, 3, 2, 0, 2, 3, 1, 0, 1, 2, 2, 1, 2, 2, 2, 2, 2, 1] },
  { modelo: 'HOSP', paciente: 'jose', data: '2026-09-23', prof: 'rodrigo', resp: {
    1: 'Oral e enteral', 2: 'false', 4: 'Fresubin HP Energy por SNE a 69 ml/h + dieta pastosa', 5: 'Parcial (50 a 75%)',
    6: 'false', 7: 'false', 8: 'false', 9: 'true', 10: 'true', 11: 'true', 12: 'Constipação há 3 dias, em uso de lactulose.',
    13: 'false', 15: 'Nenhuma.', 16: 'Fonoaudiologia liberou dieta pastosa homogênea. Manter sonda até aceitação acima de 75 %.' } },
  { modelo: 'HOSP', paciente: 'maria', data: '2026-09-21', prof: 'ana', resp: {
    1: 'Enteral por sonda', 2: 'false', 4: 'Novasource Senior por SNE em bomba de infusão', 5: 'Não aceita',
    6: 'true', 7: 'true', 8: 'false', 9: 'false', 10: 'true', 11: 'true', 13: 'false',
    15: 'Dieta para diabetes.', 16: 'Disfagia grave após AVC; via oral suspensa pela fonoaudiologia.' } },
  { modelo: 'HOSP', paciente: 'rosangela', data: '2026-09-23', prof: 'ana', resp: {
    1: 'Enteral por sonda', 2: 'false', 4: 'Fresubin HP Energy + Nutren Just Protein', 5: 'Boa (acima de 75%)',
    6: 'false', 7: 'false', 8: 'false', 9: 'true', 10: 'false', 11: 'false', 13: 'true', 14: 'Camarão',
    16: 'Em hemodiálise contínua; balanço hídrico controlado pela nefrologia.' } },
  { modelo: 'ADULTO', paciente: 'paulo', data: '2026-07-28', prof: 'rodrigo', resp: {
    1: 'false', 3: 'false', 5: 'true', 6: 'Insuficiência cardíaca, hipertensão arterial', 7: 'true', 8: 'Furosemida, carvedilol, enalapril, espironolactona',
    9: 'false', 11: '4', 12: '1', 13: 'false', 14: 'true', 15: 'true', 16: 'Restrição de sódio e de líquidos',
    17: 'Não come peixe.', 18: 'Sedentário', 19: 'Constipado', 20: 'Manter o peso seco, reduzir sódio e adequar a ingestão proteica após a alta.' } },
  { modelo: 'ADULTO', paciente: 'eduarda', data: '2026-05-06', prof: 'rodrigo', resp: {
    1: 'false', 3: 'true', 4: 'Lactose', 5: 'true', 6: 'Doença de Crohn', 7: 'true', 8: 'Azatioprina',
    9: 'true', 10: 'Vitamina D e ferro', 11: '6', 12: '2', 13: 'false', 14: 'false', 15: 'true', 16: 'Pobre em resíduos nas primeiras semanas',
    17: 'Folhosos crus e grãos integrais causam desconforto.', 18: 'Levemente ativo', 19: 'Diarreico', 20: 'Recuperar o peso usual e manter a doença em remissão.' } },
  { modelo: 'PED', paciente: 'laura', data: '2026-06-10', prof: 'camila', resp: {
    1: '39', 2: '3250', 3: 'Vaginal', 5: 'true', 6: '4', 7: 'true', 8: 'NAN 1', 10: 'false', 11: '7',
    14: 'false', 15: 'Normal', 16: 'false', 18: 'false', 20: 'Juliana (mãe)', 21: 'Acompanhar o crescimento na transição para a introdução alimentar.' } },
  { modelo: 'PED', paciente: 'miguel', data: '2026-08-20', prof: 'camila', resp: {
    1: '34', 2: '2180', 3: 'Cesárea', 4: 'Pré-eclâmpsia; UTI neonatal por 12 dias.', 5: 'true', 6: '1', 7: 'true', 8: 'PREGOMIN', 11: '8',
    15: 'Diarreico', 16: 'true', 17: 'Proteína do leite de vaca', 18: 'true', 19: 'Vitamina D', 20: 'Patrícia (mãe)', 21: 'Recuperar o ganho de peso com fórmula extensamente hidrolisada.' } },
  { modelo: 'PED', paciente: 'arthur', data: '2026-04-15', prof: 'camila', resp: {
    1: '38', 2: '2900', 3: 'Vaginal', 5: 'true', 6: '6', 7: 'true', 8: 'FORTINI', 9: '6', 10: 'true', 11: '5', 12: 'true', 13: 'Carnes e verduras',
    14: 'true', 15: 'Constipado', 16: 'false', 18: 'true', 19: 'Sulfato ferroso', 20: 'Fernanda (mãe)', 21: 'Recuperação ponderal após internações por pneumonia.' } },
  { modelo: 'PED', paciente: 'sofia', data: '2026-05-12', prof: 'ana', resp: {
    1: '40', 2: '3600', 3: 'Cesárea', 5: 'true', 6: '12', 7: 'false', 9: '6', 10: 'false', 11: '6', 12: 'true', 13: 'Legumes e frutas',
    14: 'true', 15: 'Normal', 16: 'false', 18: 'false', 20: 'Carla (mãe)', 21: 'Adequar o ganho de peso ao crescimento e reduzir ultraprocessados.' } },
]

// ── Execução ────────────────────────────────────────────────────────────────

const catalogo = {}

async function carregarCatalogos() {
  const porNome = (lista) => Object.fromEntries(lista.map((x) => [x.nome, x]))
  catalogo.tipoCadastro = porNome(await get('/tipos-cadastro/select'))
  catalogo.tipoTelefone = porNome(await get('/tipos-telefone/select'))
  catalogo.tipoEmail = porNome(await get('/tipos-email/select'))
  catalogo.tipoEndereco = porNome(await get('/tipos-endereco/select'))
  catalogo.lactea = porNome(await get('/formulas-lacteas/select'))
  catalogo.enteral = porNome(await get('/formulas-enterais/select'))
  catalogo.modulo = porNome(await get('/produtos-nutricionais/modulos-proteicos'))
  catalogo.cidade = {}
  for (const nome of new Set(ENDERECOS.map((e) => e[0]))) {
    const achadas = await get(`/cidades/select?nome=${encodeURIComponent(nome)}`)
    const rs = achadas.find((c) => c.nome === nome && c.estadoSigla === 'RS')
    if (!rs) throw new Error(`Cidade não encontrada: ${nome}/RS`)
    catalogo.cidade[nome] = rs.id
  }
  catalogo.modelo = {}
  for (const m of await get('/modelos-ficha/select')) {
    const chave = m.escoreCodigo === 'MNA' ? 'MNA' : m.escoreCodigo === 'NRS_2002' ? 'NRS'
      : m.nome === 'Anamnese nutricional adulto' ? 'ADULTO'
      : m.nome === 'Anamnese nutricional hospitalar' ? 'HOSP'
      : m.nome === 'Anamnese nutricional pediátrica' ? 'PED' : null
    if (chave) catalogo.modelo[chave] = await get(`/modelos-ficha/${m.id}`)
  }
  for (const k of ['MNA', 'NRS', 'ADULTO', 'HOSP', 'PED'])
    if (!catalogo.modelo[k]) throw new Error(`Modelo de ficha não encontrado: ${k}`)
}

const pessoas = {}
let seq = 1

async function criarPessoa(p, tipos, extra = {}) {
  const n = seq++
  const [cidade, cep, rua, numero, bairro] = ENDERECOS[n % ENDERECOS.length]
  const corpo = {
    nome: p.nome,
    tipoPessoa: 'PESSOA_FISICA',
    cpf: cpf(sementeDe(p.nome)),
    rg: tipos.includes('Paciente') && anosCompletos(p.nasc, HOJE) < 12 ? null : rg(sementeDe(p.nome)),
    dataNascimento: p.nasc,
    sexo: p.sexo,
    observacao: p.obs ?? null,
    tiposCadastroIds: tipos.map((t) => catalogo.tipoCadastro[t].id),
    telefones: [{ tipoTelefoneId: catalogo.tipoTelefone['Celular'].id, numero: celular(sementeDe(p.nome)), principal: true }],
    emails: anosCompletos(p.nasc, HOJE) >= 16 ? [{ tipoEmailId: catalogo.tipoEmail['Particular'].id, email: emailDe(p.nome), principal: true }] : [],
    enderecos: [{ tipoEnderecoId: catalogo.tipoEndereco['Residencial'].id, cidadeId: catalogo.cidade[cidade], cep, rua, numero, bairro, principal: true }],
    ...extra,
  }
  const criada = await post('/pessoas', corpo)
  pessoas[p.chave] = criada.id
  return criada
}

async function semearPessoas() {
  for (const p of PROFISSIONAIS) await criarPessoa(p, ['Profissional de saúde'])
  for (const p of RESPONSAVEIS) await criarPessoa(p, ['Responsável'])
  for (const c of CRIANCAS) {
    const vinculos = c.resp ? [{ pessoaId: pessoas[c.resp], tipo: 'RESPONSAVEL', observacao: 'Responsável legal' }] : []
    await criarPessoa(c, ['Paciente'], { vinculos })
  }
  for (const a of ADULTOS) await criarPessoa(a, ['Paciente'])
}

const estado = { ativos: { uti: [], pediatria: [] } }

async function semearPediatria() {
  let avaliacoes = 0, dias = 0
  for (const c of CRIANCAS) {
    let ultima = null
    for (const av of c.avaliacoes) {
      const idade = idadeFracionada(c.nasc, av.data)
      const peso = r3(mediana(c.sexo, 'peso', idade) * av.fp)
      const estatura = r1(mediana(c.sexo, 'est', idade) * av.fe)
      const salva = await post('/pediatria/avaliacoes', {
        pacienteId: pessoas[c.chave],
        profissionalId: pessoas[av.prof],
        dataAvaliacao: av.data,
        sexo: c.sexo,
        idadeMeses: mesesCompletos(c.nasc, av.data),
        peso,
        estatura,
        formulaLacteaId: av.formula ? catalogo.lactea[av.formula].id : null,
        volumeMl: av.formula ? av.vol : null,
        frequenciaHoras: av.formula ? av.freq : null,
        observacao: av.obs,
      })
      avaliacoes++
      ultima = { ...av, id: salva.id, peso }
    }
    if (c.diario) {
      // O dia i é a data da avaliação + i. Quem fica "pendente" para hoje para um dia antes.
      const alvo = Math.min(c.diario.dias, diasEntre(ultima.data, HOJE) - (c.diario.pendenteHoje ? 1 : 0))
      dias += await diasPediatricos(c, ultima, 1, alvo)
      estado.ativos.pediatria.push({ chave: c.chave, aval: ultima, ultimoIndice: alvo })
    }
  }
  return { avaliacoes, dias }
}

async function diasPediatricos(c, aval, de, ate) {
  const rnd = prng(sementeDe(c.chave))
  for (let i = 1; i < de; i++) diaPediatrico(c, aval, i, rnd) // mantém a sequência do aleatório
  let n = 0
  for (let i = de; i <= ate; i++) {
    const dia = diaPediatrico(c, aval, i, rnd)
    await post('/pediatria/registros-diarios', { pessoaId: pessoas[c.chave], avaliacaoId: aval.id, data: somarDias(aval.data, i), ...dia })
    n++
  }
  return n
}

async function semearUti() {
  let avaliacoes = 0, dias = 0, encerradas = 0
  for (const a of ADULTOS) {
    const salvas = []
    for (const [k, av] of a.avaliacoes.entries()) {
      const idadeAnos = anosCompletos(a.nasc, av.data)
      const base = { sexo: a.sexo, idadeAnos, ...av.calc }
      const previa = await post('/uti/calculo', base)
      const meta = previa.necessidades?.metaEnergetica
      if (!meta) throw new Error(`Sem meta energética para ${a.nome}: ${previa.necessidades?.motivo}`)
      const formula = catalogo.enteral[av.formula]
      if (!formula) throw new Error(`Fórmula enteral não encontrada: ${av.formula}`)
      const volume = meta / formula.densidadeKcalMl
      const dieta = av.intermitente
        ? { modoInfusao: 'INTERMITENTE', volumePorTempo: Math.round(volume / av.intermitente / 5) * 5, tempo: av.intermitente }
        : { modoInfusao: 'CONTINUA', volumePorTempo: Math.round(volume / 22), tempo: 22 }
      const calculo = {
        ...base,
        formulaEnteralId: formula.id,
        ...dieta,
        moduloProteicoId: av.modulo ? catalogo.modulo[av.modulo].id : null,
      }
      const salva = await post('/uti/avaliacoes', { pacienteId: pessoas[a.chave], profissionalId: pessoas[av.prof], dataAvaliacao: av.data, calculo, observacao: av.obs })
      avaliacoes++
      salvas.push({
        id: salva.id,
        data: av.data,
        formula: av.formula,
        calc: av.calc,
        reavaliacao: k > 0,
        pesoTrabalho: previa.antropometria?.pesoDeTrabalhoKg,
        volumeTotal: av.intermitente ? dieta.volumePorTempo * dieta.tempo : dieta.volumePorTempo * 22,
        descricao: av.intermitente ? `${dieta.volumePorTempo} ml × ${dieta.tempo} horários` : `${dieta.volumePorTempo} ml/h`,
      })
    }

    const int = a.internacao
    const fimPlano = int.fim ?? (int.hojeRegistrado ? HOJE : somarDias(HOJE, -1))
    dias += await diasDeUti(a, salvas, int.inicio, fimPlano)
    if (int.encerramento) {
      const ultima = salvas[salvas.length - 1]
      await api('PATCH', `/uti/avaliacoes/${ultima.id}/encerramento`, { encerradoEm: int.encerramento.em, motivo: int.encerramento.motivo, observacao: int.encerramento.obs })
      encerradas++
    } else {
      estado.ativos.uti.push({ chave: a.chave, salvas, ultimoDia: fimPlano, pendenteHoje: !int.hojeRegistrado })
    }
  }
  return { avaliacoes, dias, encerradas }
}

// A avaliação de referência de cada dia é a mais recente até ele.
async function diasDeUti(a, salvas, de, ate, desde = de) {
  const int = a.internacao
  const total = diasEntre(int.inicio, int.fim ?? HOJE) + 1
  const rnd = prng(sementeDe(a.chave))
  let n = 0
  for (let data = int.inicio; data <= ate; data = somarDias(data, 1)) {
    const i = diasEntre(int.inicio, data) + 1
    const aval = [...salvas].reverse().find((s) => s.data < data) ?? salvas[0]
    const dia = diaDeUti(a, aval, i, total, rnd)
    if (data < desde) continue
    await post('/uti/registros-diarios', { pessoaId: pessoas[a.chave], avaliacaoId: aval.id, data, ...dia })
    n++
  }
  return n
}

async function semearFichas() {
  let n = 0
  for (const f of FICHAS) {
    const modelo = catalogo.modelo[f.modelo]
    const campos = [...modelo.campos].sort((x, y) => x.ordem - y.ordem)
    let respostas
    if (f.nrs) {
      respostas = f.nrs.slice(0, 4).map((v, i) => ({ campoId: campos[i].id, valor: String(v) }))
      if (f.nrs.length > 4) {
        respostas.push({ campoId: campos[4].id, valor: campos[4].opcoes[f.nrs[4]] })
        respostas.push({ campoId: campos[5].id, valor: campos[5].opcoes[f.nrs[5]] })
      }
    } else if (f.mna) {
      respostas = f.mna.map((op, i) => ({ campoId: campos[i].id, valor: campos[i].opcoes[op] }))
    } else {
      respostas = Object.entries(f.resp).map(([ordem, valor]) => ({ campoId: campos.find((c) => c.ordem === Number(ordem)).id, valor }))
    }
    await post('/fichas-anamnese', {
      pacienteId: pessoas[f.paciente],
      profissionalId: pessoas[f.prof],
      dataPreenchimento: f.data,
      modeloId: modelo.id,
      respostas,
      observacao: f.obs ?? null,
    })
    n++
  }
  return n
}

// ── Modo --atualizar: completa os dias até hoje ─────────────────────────────

async function atualizar() {
  if (!existsSync(ESTADO)) throw new Error('Não achei demo/.estado-demo.json — rode o script sem --atualizar primeiro.')
  const salvo = JSON.parse(readFileSync(ESTADO, 'utf8'))
  token = (await login(DEMO.email, DEMO.senha)).accessToken
  Object.assign(pessoas, salvo.pessoas)
  let uti = 0, ped = 0
  for (const at of salvo.ativos.uti) {
    const a = ADULTOS.find((x) => x.chave === at.chave)
    const ate = at.pendenteHoje ? somarDias(HOJE, -1) : HOJE
    if (ate > at.ultimoDia) {
      uti += await diasDeUti(a, at.salvas, a.internacao.inicio, ate, somarDias(at.ultimoDia, 1))
      at.ultimoDia = ate
    }
  }
  for (const at of salvo.ativos.pediatria) {
    const c = CRIANCAS.find((x) => x.chave === at.chave)
    const alvo = diasEntre(at.aval.data, HOJE) + (c.diario.pendenteHoje ? -1 : 0)
    if (alvo > at.ultimoIndice) {
      ped += await diasPediatricos(c, at.aval, at.ultimoIndice + 1, alvo)
      at.ultimoIndice = alvo
    }
  }
  writeFileSync(ESTADO, JSON.stringify(salvo, null, 2))
  console.log(`Atualizado até ${HOJE}: ${uti} dias de UTI e ${ped} dias de pediatria acrescentados.`)
}

async function semear() {
  const email = process.env.SUPERADMIN_EMAIL
  const senha = process.env.SUPERADMIN_PASSWORD
  if (!email || !senha) throw new Error('Defina SUPERADMIN_EMAIL e SUPERADMIN_PASSWORD no ambiente.')

  // Já existe? Então não duplica.
  try {
    token = (await login(DEMO.email, DEMO.senha)).accessToken
    const pagina = await get('/pessoas?size=1')
    if (pagina.totalElements > 0) {
      console.log(`O cliente de demonstração já tem ${pagina.totalElements} pessoas. Nada feito.`)
      console.log('Para completar os dias até hoje: node demo/semear-demo.mjs --atualizar')
      return
    }
  } catch (e) {
    if (e.status !== 401) throw e
    const su = await login(email, senha)
    const usuario = await api('POST', '/usuarios', { nome: DEMO.nome, email: DEMO.email, senha: DEMO.senha, telefone: '51999990000', periodoAcesso: 'INDETERMINADO' }, su.accessToken)
    await api('PUT', `/tenants/${usuario.tenantId}`, { nome: DEMO.cliente }, su.accessToken)
    token = (await login(DEMO.email, DEMO.senha)).accessToken
    console.log(`Cliente "${DEMO.cliente}" e usuária ${DEMO.email} criados.`)
  }

  await carregarCatalogos()
  await semearPessoas()
  console.log(`Pessoas: ${Object.keys(pessoas).length}`)
  const ped = await semearPediatria()
  console.log(`Pediatria: ${ped.avaliacoes} avaliações, ${ped.dias} dias de acompanhamento`)
  const uti = await semearUti()
  console.log(`UTI: ${uti.avaliacoes} avaliações, ${uti.dias} dias de acompanhamento, ${uti.encerradas} internações encerradas`)
  const fichas = await semearFichas()
  console.log(`Fichas de anamnese: ${fichas}`)

  writeFileSync(ESTADO, JSON.stringify({ pessoas, ...estado }, null, 2))
  console.log(`\nPronto. Entre em http://localhost:5173/app/login com ${DEMO.email} (senha em demo/README.md).`)
}

// ── Modo --simular: roda os cálculos sem gravar nada ───────────────────────

async function simular() {
  token = (await login(process.env.SUPERADMIN_EMAIL, process.env.SUPERADMIN_PASSWORD)).accessToken
  await carregarCatalogos()
  for (const c of CRIANCAS) for (const av of c.avaliacoes) {
    const idade = idadeFracionada(c.nasc, av.data)
    const r = await post('/pediatria/avaliacoes/calcular', {
      sexo: c.sexo, idadeMeses: mesesCompletos(c.nasc, av.data),
      peso: r3(mediana(c.sexo, 'peso', idade) * av.fp), estatura: r1(mediana(c.sexo, 'est', idade) * av.fe),
      formulaLacteaId: av.formula ? catalogo.lactea[av.formula].id : null,
      volumeMl: av.formula ? av.vol : null, frequenciaHoras: av.formula ? av.freq : null,
    })
    console.log('PED', c.chave, av.data, JSON.stringify(r).slice(0, 900))
  }
  for (const a of ADULTOS) for (const av of a.avaliacoes) {
    const base = { sexo: a.sexo, idadeAnos: anosCompletos(a.nasc, av.data), ...av.calc }
    const previa = await post('/uti/calculo', base)
    const f = catalogo.enteral[av.formula]
    const vol = previa.necessidades.metaEnergetica / f.densidadeKcalMl
    const dieta = av.intermitente
      ? { modoInfusao: 'INTERMITENTE', volumePorTempo: Math.round(vol / av.intermitente / 5) * 5, tempo: av.intermitente }
      : { modoInfusao: 'CONTINUA', volumePorTempo: Math.round(vol / 22), tempo: 22 }
    const r = await post('/uti/calculo', { ...base, formulaEnteralId: f.id, ...dieta, moduloProteicoId: av.modulo ? catalogo.modulo[av.modulo].id : null })
    const an = r.antropometria, ne = r.necessidades, di = r.dieta
    console.log('UTI', a.chave, av.data, `idade ${base.idadeAnos} peso ${an.pesoDeTrabalhoKg} (${an.pesoDeTrabalhoOrigem}) IMC ${an.imc} ${an.classificacaoImcOms?.descricao ?? JSON.stringify(an.classificacaoImcOms)} idoso ${JSON.stringify(an.classificacaoImcIdoso)} perda ${an.percentualPerdaPeso}`,
      `| meta ${ne.metaEnergetica} kcal ${ne.metaProteica} g obeso ${ne.obeso} | ${dieta.volumePorTempo}x${dieta.tempo} → ${di.volumeTotalMl} ml ${di.caloriasOfertadas} kcal ${di.proteinaOfertada} g ${di.percentualDoVct}% ${di.percentualDaProteina}% módulo ${di.moduloMedidas ?? '-'} ${di.motivoModulo ?? ''}`)
  }
  for (const f of FICHAS.filter((x) => x.nrs || x.mna)) {
    const modelo = catalogo.modelo[f.modelo]
    const campos = [...modelo.campos].sort((x, y) => x.ordem - y.ordem)
    const respostas = f.nrs
      ? [...f.nrs.slice(0, 4).map((v, i) => ({ campoId: campos[i].id, valor: String(v) })),
         ...(f.nrs.length > 4 ? [{ campoId: campos[4].id, valor: campos[4].opcoes[f.nrs[4]] }, { campoId: campos[5].id, valor: campos[5].opcoes[f.nrs[5]] }] : [])]
      : f.mna.map((op, i) => ({ campoId: campos[i].id, valor: campos[i].opcoes[op] }))
    const e = await post('/fichas-anamnese/escore', { modeloId: modelo.id, dataPreenchimento: f.data, respostas })
    console.log('ESC', f.modelo, f.paciente, e.total, e.classificacao?.descricao ?? JSON.stringify(e.classificacao), e.conclusao ?? e.motivoAusencia)
  }
}

const principal = process.argv.includes('--atualizar') ? atualizar : process.argv.includes('--simular') ? simular : semear
principal().catch((e) => {
  console.error('ERRO:', e.message)
  process.exit(1)
})
