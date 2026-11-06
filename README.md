# DropSpace

Loja virtual de tecnologia (PCs, notebooks, celulares, consoles e acessórios).
Back-end: Java 21 + Spring Boot + banco H2 (arquivo local). Front-end: HTML/CSS/JS puro.

## Como rodar no VS Code
1. Instale o JDK 21+ e a extensão "Extension Pack for Java" (e "Spring Boot Extension Pack").
2. Abra a pasta `DropSpace` no VS Code.
3. Abra `DropSpaceApplication.java` e clique em **Run**, ou no terminal: `mvn spring-boot:run`
4. Acesse http://localhost:8080

## Banco de dados
Criado automaticamente em `./data/dropspace`. Tabelas: `users`, `product`, `orders`, `order_item`.
Console: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:file:./data/dropspace`, usuário `sa`, senha vazia).

## API
| Método | Rota | Descrição |
|---|---|---|
| GET | /api/products?q=&category= | Lista/busca produtos |
| GET | /api/products/{id} | Detalhe do produto |
| POST | /api/auth/register | Cria conta |
| POST | /api/auth/login | Entra (retorna token) |
| GET | /api/me | Dados do usuário logado |
| POST | /api/orders | Finaliza compra (baixa o estoque) |
| GET | /api/orders | Pedidos do usuário |

Rotas protegidas usam o cabeçalho `Authorization: Bearer <token>`.

## Próximos passos
Pagamento real (Pix/cartão), tela de produto, endereço/frete por CEP, painel admin, Spring Security + JWT, trocar H2 por PostgreSQL.
