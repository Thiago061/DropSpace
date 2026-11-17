# DropSpace

Loja virtual de tecnologia. Java 17 + Spring Boot 3.3.5 + H2 (arquivo local) no back-end; HTML/CSS/JS puro no front-end.

## Rodar no VS Code
1. JDK 17+ e a extensão "Extension Pack for Java".
2. **Apague a pasta `data/`** se já rodou uma versão anterior (o banco mudou e será recriado).
3. Rode `DropSpaceApplication.java` (Run) ou `mvn spring-boot:run` **a partir da pasta do projeto**. Abra http://localhost:8080

## Acesso administrativo
As credenciais ficam em `config/local.properties` (arquivo **ignorado pelo Git**, nunca no código):
```
dropspace.admin.username=Adm123
dropspace.admin.password=123Adm
```
Também funcionam variáveis de ambiente `DROPSPACE_ADMIN_USERNAME` e `DROPSPACE_ADMIN_PASSWORD`. No banco só fica o hash BCrypt.
Mudar a senha no arquivo e reiniciar troca a senha do admin. Sem essas configurações, não existe administrador.
O cadastro público **sempre** cria clientes; ninguém vira admin pelo site. Entre em `conta.html` com `Adm123`.

## Páginas
| Página | Função |
|---|---|
| `index.html` | Loja, busca e categorias (categoria ativa destacada em laranja, via URL) |
| `videos.html` | Aprenda a montar seu PC |
| `carrinho.html` | Carrinho + frete por CEP + finalizar compra |
| `cadastro.html` | Passo 1: nome, sobrenome, CPF, CEP, e-mail e senha |
| `interesses.html` | Passo 2: interesses (múltipla escolha, Continuar ou Pular) |
| `conta.html` | Entrar (e-mail ou usuário), meus pedidos |
| `admin.html` | Resumo, produtos (cadastrar/editar/excluir, foto), categorias, estoque e movimentações |

HTML fica nas páginas: `partials/header.html`, `partials/footer.html` e `<template>` em cada página. O JavaScript só preenche dados.

## Regras
- Senha de cliente: 6 a 8 caracteres, BCrypt. Campo com mostrar/ocultar e copiar/colar bloqueado.
- CPF validado (dígitos verificadores) no navegador e no servidor; CPF e e-mail são únicos. CEP consultado no ViaCEP.
- Sessão de 8 h; 5 tentativas erradas de login bloqueiam por 5 min. Rotas `/api/admin/**` exigem perfil ADMIN no servidor.
- Interesses = categorias do banco; a loja mostra primeiro os produtos dos interesses do cliente logado.
- Estoque só muda por movimentação registrada (histórico) e vendas usam bloqueio otimista.

## API
Públicas: `GET /api/products`, `/api/products/{id}`, `/api/categories`, `/api/videos`, `/api/address/{cep}`, `/api/shipping?cep=&subtotal=`.
Conta: `POST /api/auth/register|login|logout`, `GET /api/me`, `PUT /api/me/interests`. Pedidos: `POST|GET /api/orders`.
Admin: `GET /api/admin/summary`; `GET|POST /products`, `PUT|DELETE /products/{id}`, `POST /products/{id}/stock`, `POST /products/{id}/image`, `GET /movements`; `POST /categories`, `PUT|DELETE /categories/{slug}`.
Console do banco: http://localhost:8080/h2-console (`jdbc:h2:file:./data/dropspace`, usuário `sa`).

## Próximos passos
Pagamento real (Pix/cartão), recuperação de senha, e-mail de confirmação, PostgreSQL e HTTPS em produção.
