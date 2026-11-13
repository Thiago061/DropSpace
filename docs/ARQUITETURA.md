# Arquitetura do Zona GEO

## Visão geral
```
 Android (Kotlin) ──┐                       ┌── PostgreSQL 16 + PostGIS (Flyway)
                    ├─ HTTPS/JSON + JWT ──► Spring Boot 3.5 (Java 21) ──┤
 Site + Admin (web) ┘                       └── Open-Meteo (clima atual, com cache)
```
- **Site público** (`/`): página de vendas e explicação. Os pontos de apoio **não** aparecem no site, só no app.
- **Admin** (`/admin/`): login, cadastro de pontos de apoio, ocorrências, planos de usuários e conferência de vouchers.
- **API do app** (`/api/app/**`): recomendações, rota e vouchers. Exige login.
- **Admin API** (`/api/admin/**`, `/api/ocorrencias/**`, `/api/localizacoes/**`, `/api/medicoes/**`, `/api/dashboard/**`): só papel `ADMIN`.

## Organização do código
```
src/main/java/br/com/zonageo
├── config/        propriedades tipadas, dados iniciais
├── security/      Spring Security, JWT, limite de tentativas de login
├── controller/
│   ├── publico/   login/cadastro, planos, saúde (sem autenticação)
│   ├── app/       endpoints do aplicativo Android (usuário logado)
│   └── admin/     um controller por assunto: pontos, ofertas, vouchers, usuários, lugares, ocorrências...
├── service/       regras de negócio (controllers só recebem e devolvem)
├── repository/    acesso ao banco (JPA + consultas PostGIS)
├── model/         entidades          dto/  contratos de entrada/saída
src/main/resources/static
├── index.html, css/site.css, js/site.js        página de vendas
└── admin/index.html, css/app.css, vendor/leaflet
    js/admin/  api.js · auth.js · dados.js · seletor-local.js · util.js · main.js
               paginas/ painel · pontos · ocorrencias · vouchers · usuarios
```

## Pacotes (`br.com.zonageo`)
| Pacote | Responsabilidade |
|---|---|
| `config` | Propriedades tipadas, criação do admin inicial/dados demo |
| `security` | Spring Security stateless, JWT (HS256), BCrypt, limite de tentativas de login |
| `controller` | Camada HTTP (fina) |
| `service` | Regras de negócio: auth, clima, localidade, rota, voucher |
| `service.RecomendacaoEngine` | Motor de regras **puro** (sem Spring), com testes unitários |
| `geo` | Cálculos geográficos puros (haversine, projeção ponto-trecho) |
| `model` / `repository` | JPA; consultas espaciais nativas com PostGIS (`ST_DWithin`) |
| `dto` | Contratos de entrada/saída (records validados com Bean Validation) |
| `exception` | Erros padronizados (RFC 9457) sem vazar detalhes internos |

## Decisões e por que
- **JWT stateless** com claim `papel`; o usuário e o plano são relidos do banco a cada requisição (mudanças de plano valem na hora).
- **Segredos só no ambiente** (`.env` fora do Git). A aplicação **não sobe** sem `ZONAGEO_JWT_SECRET` forte.
- **Admin inicial** vem do ambiente e é gravado com BCrypt; nunca sobrescreve um admin existente.
- **Recomendação por regras**, não por IA generativa: é previsível, auditável e barata. Os limites ficam em constantes no motor.
  O mesmo contrato de entrada serve para plugar um LLM depois, mantendo as regras como rede de segurança.
- **Rota Pro = paradas de apoio perto do caminho direto**, não roteamento por ruas nem medição real de sombra.
- **Voucher**: código aleatório (SecureRandom), validade curta, 1 por dia, só perto do local, resgate atômico no banco
  (`UPDATE ... WHERE status='ATIVO' AND expira_em > agora`) para impedir uso duplo, e trava da linha do usuário na emissão.
- **Pontos nunca são apagados**, só desativados, para preservar o histórico dos vouchers.

## Pendências conhecidas (próximos passos)
1. **Pagamento do Pro** (Google Play Billing no app e/ou Pix/cartão no site). Hoje o admin libera o plano manualmente.
2. **Portal do parceiro** (login próprio para o caixa validar vouchers sem ser admin).
3. **Provedor de clima comercial**: o plano gratuito da Open-Meteo é para uso não comercial.
4. **Refresh token** e revogação de sessões; limite de login em Redis se houver mais de uma instância.
5. **HTTPS e proxy reverso** em produção (`server.forward-headers-strategy=framework` atrás de proxy).
6. **Controllers legados** (`/api/localizacoes`, `/api/medicoes`, `/api/ocorrencias`) foram reorganizados, mas ainda recebem entidades JPA diretamente; migrar para DTOs.
7. **Testes de integração** com Testcontainers (PostGIS) cobrindo segurança e consultas espaciais.
8. **Lógica de "IA" para criar ocorrências automaticamente** a partir de clima + notificações push de alerta.
9. Política de privacidade/LGPD (localização é dado pessoal) antes de publicar o app.
