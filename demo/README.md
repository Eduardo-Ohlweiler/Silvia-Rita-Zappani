# Demonstração — dados e roteiros de vídeo

Material para gravar os vídeos de treinamento do sistema (tudo menos a área
administrativa).

| Arquivo | O que é |
|---|---|
| `Roteiros-de-video.pdf` | 13 roteiros: o que clicar, o que digitar, o que falar e o que mostrar |
| `roteiros/roteiros.html` | fonte do PDF; regere com `node demo/roteiros/gerar-pdf.mjs` |
| `semear-demo.mjs` | cria o cliente de demonstração com seis meses de histórico, pela API |

## Acesso de demonstração

Credencial **só deste ambiente local de testes**:

| | |
|---|---|
| Endereço | http://localhost:5173/app/login |
| E-mail | `nutricionista@demo.local` |
| Senha | `Demo@Nutri2026` |
| Cliente | Hospital Demonstração — usuária Ana Paula Martins, nível ADMIN |

## Semear (uma vez por banco)

Com a API no ar (`./run-dev.sh`):

```bash
SUPERADMIN_EMAIL=… SUPERADMIN_PASSWORD=… node demo/semear-demo.mjs
```

Cria a usuária e o cliente e, dentro dele:

- **30 pessoas**: 3 profissionais, 9 crianças e 6 responsáveis com vínculo, 12
  adultos de UTI. Todas com CPF válido, endereço e telefone.
- **18 avaliações pediátricas** e **41 dias de acompanhamento**, em 3 crianças.
- **15 avaliações de UTI** e **137 dias de acompanhamento**: 5 internações em
  curso e 7 encerradas (altas, óbito, transferência).
- **19 fichas de anamnese**: NRS-2002, MNA, adulto, hospitalar e pediátrica.

Rodar de novo não duplica nada: se o cliente já tem pessoas, o script para.
`--simular` roda só os cálculos, sem gravar.

## No dia de gravar: `--atualizar`

As datas são relativas ao dia em que o script rodou. Para a ronda da tela
inicial não esvaziar:

```bash
node demo/semear-demo.mjs --atualizar
```

Isso completa os dias que faltam até hoje nas internações em curso. Maria de
Lourdes, Rosângela e Helena ficam com **hoje pendente**, de propósito. O estado
fica em `demo/.estado-demo.json`, que não vai para o git.

## Cliente "Ensaio dos Roteiros"

Os números dos roteiros foram conferidos fazendo cada vídeo pela API num cliente
separado (`ensaio@demo.local`). Ele não interfere na demonstração. Pode ser
desativado em **Administração › Tenants**.
