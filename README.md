# Zona GEO

Plataforma que cruza **clima** (calor/frio/ar seco/UV), **ocorrências** (queimada, alagamento…) e **pontos de apoio**
(água, sombra, abrigo, mercados parceiros) para dizer ao usuário **o que fazer** onde ele está.

- **Web** (`/`): página de vendas dos planos e explicação do app.
- **Admin** (`/admin/`): cadastro de pontos, ocorrências, planos e validação de vouchers.
- **Android** (em desenvolvimento): cadastro do usuário, recomendações, mapa de pontos. Contrato em [`docs/API_MOBILE.md`](docs/API_MOBILE.md).

| | Gratuito | Pro |
|---|---|---|
| Clima, ocorrências e recomendações | ✅ | ✅ |
| Mapa de pontos de água/sombra/abrigo/parceiros | ✅ | ✅ |
| Rota com paradas de apoio e alertas no trajeto | — | ✅ |
| Voucher em mercado parceiro (ex.: água) | — | ✅ |

## Requisitos
JDK 21 · Maven 3.6.3+ · Docker Desktop (para o PostgreSQL/PostGIS)

## Como rodar (Windows / VS Code)
1. Copie `.env.example` para `.env` e preencha. **Gere o segredo do JWT** (PowerShell):
   ```powershell
   [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
   ```
   Para usar os dados de demonstração, deixe `ZONAGEO_SEED_DEMO=true`.
2. Suba o banco: `docker compose up -d database`
3. Rode: `mvn spring-boot:run`
4. Abra `http://localhost:8080` (site) e `http://localhost:8080/admin/` (painel).

O Flyway cria as tabelas na primeira execução. O administrador é criado a partir de `ZONAGEO_ADMIN_USER` e
`ZONAGEO_ADMIN_PASSWORD` **somente se ainda não existir** (a senha é guardada com BCrypt).

### Testar a API no VS Code
Instale a extensão **REST Client** e abra [`docs/requests.http`](docs/requests.http): ele percorre cadastro, login,
recomendações, plano Pro, rota e voucher.

### Testes
- `mvn test` roda os testes unitários (motor de recomendações e cálculos geográficos).
- Teste de integração (precisa do banco no ar): `ZONAGEO_TEST_DB=true mvn test`.

## Segurança: o que já existe
Senhas com BCrypt · JWT com expiração · papéis `USER`/`ADMIN` · limite de tentativas de login (5 por 15 min) ·
validação de entrada · erros sem stack trace · cabeçalhos CSP/anti-clickjacking · segredos fora do código e do Git.

> ⚠️ A senha do admin deste projeto de desenvolvimento foi compartilhada em conversa. **Troque-a antes de publicar**
> e use uma senha longa (10+ caracteres). Para mudar, defina outro valor em `ZONAGEO_ADMIN_PASSWORD` em um banco novo
> ou altere direto no banco (o app não sobrescreve admin existente).

## Limitações atuais (leia antes de lançar)
- Não há cobrança: o Pro é liberado manualmente em **Admin → Usuários e planos**.
- O clima vem da Open-Meteo, cujo plano gratuito é **não comercial**. Contrate o plano comercial ou troque o provedor.
- As recomendações são **regras fixas**, não alertas oficiais. Limites em `RecomendacaoEngine`; valide com um especialista.
- A "rota segura" liga pontos cadastrados perto do caminho direto. Não calcula sombra real nem roteia por ruas.
- Os tiles do OpenStreetMap têm política de uso; para produção, use um provedor adequado.
- Os dados de demonstração têm coordenadas fictícias.
- Localização é dado pessoal (LGPD): é preciso política de privacidade e consentimento antes de publicar.
- Não é um sistema de emergência. Ver [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) para a lista completa de pendências.
