# Contrato da API para o aplicativo Android

Base URL em desenvolvimento: `http://10.0.2.2:8080` no emulador Android (alias do `localhost` do seu PC).
Em aparelho físico, use o IP do PC na rede local e libere `usesCleartextTraffic` **apenas no build debug**.
Em produção, use sempre HTTPS.

Autenticação: `Authorization: Bearer <token>` em tudo que está sob `/api/app/**`.
Erros vêm em JSON com o campo `detail` (RFC 9457). Em 401, volte para a tela de login.

## Cadastro e login

`POST /api/auth/registro`
```json
{ "nome": "Maria", "email": "maria@exemplo.com", "senha": "minimo-8-caracteres" }
```
`POST /api/auth/login`
```json
{ "login": "maria@exemplo.com", "senha": "minimo-8-caracteres" }
```
Resposta (ambos):
```json
{
  "token": "eyJ...", "tipo": "Bearer", "expiraEm": "2026-10-12T22:00:00Z",
  "usuario": { "id": 7, "nome": "Maria", "login": "maria@exemplo.com", "papel": "USER", "plano": "FREE", "planoExpiraEm": null }
}
```
Não envie o cabeçalho `Authorization` nas chamadas de login/registro (um token vencido causaria 401).
O token vale 8 h. Guarde em `EncryptedSharedPreferences`/Keystore, nunca em texto puro.

## Tela principal: `GET /api/app/localidade?lat=&lon=&raio=`
`raio` em metros (padrão 3000, máx. 10000). Resposta:
```json
{
  "latitude": -15.79, "longitude": -47.88, "raioM": 3000, "plano": "FREE",
  "clima": { "temperaturaC": 33.1, "sensacaoC": 38.2, "umidade": 24.0, "indiceUv": 9.0, "momento": "...", "fonte": "Open-Meteo" },
  "nivelTermico": "MUITO_QUENTE", "nivelRotulo": "Muito quente",
  "recomendacoes": [ { "categoria": "calor", "prioridade": "ATENCAO", "texto": "..." } ],
  "ocorrencias": [ { "id": 1, "titulo": "...", "categoria": "Queimada", "severidade": "Alto", "status": "Aberto", "distanciaM": 1520.4, "latitude": 0, "longitude": 0 } ],
  "pontos": [ { "id": 3, "nome": "...", "tipo": "PONTO_AGUA", "distanciaM": 350.2, "gratuito": true, "sombra": false, "voucher": null, "latitude": 0, "longitude": 0 } ],
  "aviso": "Informações de apoio..."
}
```
- `clima` e `nivelTermico` podem ser `null` se o provedor de clima estiver fora do ar. Trate isso na UI.
- `prioridade`: `INFO`, `ATENCAO`, `ALERTA` (já vem ordenado do mais para o menos importante).
- `tipo` do ponto: `PONTO_AGUA`, `DESCANSO_SOMBRA`, `ABRIGO_FRIO`, `MERCADO_PARCEIRO`.
- Mostre o campo `aviso` em algum lugar fixo da tela.

## Plano Pro
| Recurso | Endpoint | No Free |
|---|---|---|
| Rota com paradas | `GET /api/app/rotas/segura?origemLat&origemLon&destinoLat&destinoLon` | 403 |
| Gerar voucher | `POST /api/app/vouchers` `{ "pontoId", "latitude", "longitude" }` | 403 |
| Meus vouchers | `GET /api/app/vouchers` | lista vazia |

Regras do voucher: só em `MERCADO_PARCEIRO` com `voucher` preenchido; o usuário precisa estar a até 500 m do local
(enviar a posição atual); 1 por dia; vale 120 min. Se já houver um ativo para o mesmo local, a API devolve o mesmo.
Status: `ATIVO`, `RESGATADO`, `EXPIRADO`. Mostre o `codigo` em letras grandes para o caixa conferir.

A rota devolve `paradas` (ordenadas ao longo do caminho), `alertas` (ocorrências perto do trajeto) e `linkMapa`
(URL do Google Maps com as paradas). Abra com `Intent(Intent.ACTION_VIEW, Uri.parse(linkMapa))`.

## Exemplo mínimo em Kotlin (Retrofit + kotlinx.serialization)
```kotlin
@Serializable data class LoginRequest(val login: String, val senha: String)
@Serializable data class UsuarioDto(val id: Long, val nome: String, val login: String, val papel: String, val plano: String)
@Serializable data class TokenResposta(val token: String, val tipo: String, val expiraEm: String, val usuario: UsuarioDto)

interface ZonaGeoApi {
    @POST("api/auth/login") suspend fun login(@Body req: LoginRequest): TokenResposta
    @GET("api/app/localidade") suspend fun localidade(
        @Query("lat") lat: Double, @Query("lon") lon: Double, @Query("raio") raio: Int? = null
    ): LocalidadeResposta
}
// Interceptor do OkHttp: adiciona "Authorization: Bearer $token" só em /api/app/**
```
Permissões: `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`, pedidas em tempo de execução, com explicação do motivo.

## Fora do escopo desta versão
Cobrança do Pro (Google Play Billing), refresh token, push de alertas e portal do parceiro para conferir vouchers
(hoje a conferência é feita pelo painel admin).
