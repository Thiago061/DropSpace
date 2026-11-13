<<<<<<< HEAD
# DropSpace

Loja virtual de tecnologia (PCs, notebooks, celulares, consoles e acessórios).
Back-end: Java 17 + Spring Boot + banco H2 (arquivo local). Front-end: HTML/CSS/JS puro.

## Como rodar no VS Code
1. Instale o JDK 17+ e a extensão "Extension Pack for Java" (e "Spring Boot Extension Pack").
2. Abra a pasta `DropSpace` no VS Code.
3. Abra `DropSpaceApplication.java` e clique em **Run**, ou no terminal: `mvn spring-boot:run`
4. Acesse http://localhost:8080

## Banco de dados
Criado automaticamente em `./data/dropspace`. Tabelas: `users`, `product`, `orders`, `order_item`, `video`.
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
| GET | /api/videos | Vídeos da seção "Aprenda" |

Rotas protegidas usam o cabeçalho `Authorization: Bearer <token>`.

## Próximos passos
Pagamento real (Pix/cartão), tela de produto, endereço/frete por CEP, painel admin, Spring Security + JWT, trocar H2 por PostgreSQL.

## Vídeos (seção Aprenda)
Os vídeos ficam na tabela `video` (cadastro inicial em `DropSpaceApplication.java`). Para trocar título/resumo, edite lá ou direto no H2 Console. Os vídeos são só incorporados do YouTube (modo `youtube-nocookie`, carregados no clique) e creditados ao canal.
=======
﻿## 🛒 DropSpace
Sua loja virtual de tecnologia!

O DropSpace é uma plataforma de comércio eletrônico desenvolvida para a venda de produtos de tecnologia, incluindo computadores, notebooks, celulares, consoles e acessórios.

O projeto busca proporcionar uma experiência de compra simples e intuitiva, permitindo que os usuários naveguem pelo catálogo, pesquisem produtos, adicionem itens ao carrinho e realizem pedidos online.

🚀 Tecnologias utilizadas

Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- Maven

Frontend

- HTML5
- CSS3
- JavaScript puro (Vanilla JS)

📋 Funcionalidades

- Catálogo de produtos.
- Busca por nome e marca.
- Filtro por categorias.
- Cadastro de usuários.
- Login e autenticação por token.
- Carrinho de compras.
- Finalização de pedidos.
- Validação de estoque.
- Histórico de pedidos.
- Integração com pagamento Pix e cartão.
- Cálculo de frete por CEP.
- Painel administrativo.
- Página individual de produtos.
- Integração com PostgreSQL.

💻 Como executar o projeto no VS Code

1. Pré-requisitos
Antes de iniciar, instale:

- JDK 21 ou superior.
- Visual Studio Code.
- Extensão Extension Pack for Java.
- Extensão Spring Boot Extension Pack.
- Maven (caso não utilize o Maven Wrapper do projeto).

2. Abrir o projeto
Clone o repositório:

```bash
git clone https://github.com/Thiago061/DropSpace.git
```

Entre na pasta:

```bash
cd DropSpace
```

Abra a pasta no VS Code:

```bash
code .
```

### 3. Executar a aplicação
Localize o arquivo `DropSpaceApplication.java` e clique em **Run**.

Outra opção é executar pelo terminal:

```bash
mvn spring-boot:run
```

### 4. Acessar a aplicação
Após iniciar o servidor, acesse:

- **Loja virtual:** [http://localhost:8080](http://localhost:8080/)
- **Console H2:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

🗄️ Banco de dados
O projeto utiliza o banco de dados **H2**, configurado para armazenamento em arquivo local.

O banco é criado automaticamente durante a inicialização da aplicação.

Configurações:

| Campo | Valor |
|---|---|
| Tipo | Arquivo local |
| Caminho | `./data/dropspace` |
| JDBC URL | `jdbc:h2:file:./data/dropspace` |
| Usuário | `sa` |
| Senha | vazia |

Principais tabelas:

- `users`: dados dos usuários.
- `product`: produtos cadastrados.
- `orders`: pedidos realizados.
- `order_item`: itens pertencentes aos pedidos.

### Acessar o console H2

1. Inicie a aplicação.
2. Acesse [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
3. Informe os dados de conexão apresentados acima.
4. Clique em Connect.

🔌 API REST
A aplicação disponibiliza endpoints REST para comunicação entre o frontend e o backend.

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/products?q=&category=` | Lista e pesquisa produtos |
| GET | `/api/products/{id}` | Consulta detalhes de um produto |
| POST | `/api/auth/register` | Realiza cadastro de usuário |
| POST | `/api/auth/login` | Realiza login e retorna token |
| GET | `/api/me` | Consulta dados do usuário autenticado |
| POST | `/api/orders` | Finaliza pedido e atualiza estoque |
| GET | `/api/orders` | Lista pedidos do usuário autenticado |

Autenticação
As rotas protegidas utilizam autenticação por token.

Para acessar esses endpoints, envie o cabeçalho:

```http
Authorization: Bearer <token>
```

O token é obtido após o cadastro ou login.

📁 Estrutura do projeto

```text
DropSpace/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── .../
│       │       ├── controller/
│       │       ├── model/
│       │       ├── repository/
│       │       ├── service/
│       │       └── DropSpaceApplication.java
│       │
│       └── resources/
│           ├── static/
│           │   ├── index.html
│           │   ├── style.css
│           │   └── app.js
│           │
│           └── application.properties
│
├── data/
├── pom.xml
└── README.md
```

Estrutura ilustrativa dos principais diretórios e arquivos.

🔮 Próximas melhorias
O projeto possui possibilidades de evolução, incluindo:

- Integração com gateways de pagamento (Pix e cartão).
- Desenvolvimento de página individual para cada produto.
- Cadastro de endereço e cálculo de frete por CEP.
- Painel administrativo para gerenciamento de produtos e pedidos.
- Implementação do Spring Security com JWT.
- Migração do banco H2 para PostgreSQL.
- Melhorias de segurança, testes e desempenho.

👥 Equipe de desenvolvimento
Projeto desenvolvido por estudantes do curso de Análise e Desenvolvimento de Sistemas (ADS).

| Integrante | Responsabilidade |
|---|---|
| Thiago de Deus Freire | Gestão do projeto e versionamento no GitHub. |
| Ismael Sampaio Barros | Desenvolvimento e arquitetura da aplicação. |
| Fernando Ferreira | Desenvolvimento e arquitetura da aplicação. |

nstituição: Faculdade de Tecnologia e Inovação Senac-DF.

🔗 Repositório
[GitHub — Thiago061/DropSpace](https://github.com/Thiago061/DropSpace)

---

<p align="center">
  <strong>DropSpace — Seu espaço para descobrir, explorar e comprar.</strong> 🛒
</p>
>>>>>>> c72c07fe08313c6df2b2e54c9bbf3e5ffeed1440
