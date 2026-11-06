# 🛒 DropSpace

**Sua loja virtual de tecnologia!**

O DropSpace é uma plataforma de comércio eletrônico desenvolvida para a venda de produtos de tecnologia, incluindo computadores, notebooks, celulares, consoles e acessórios.

O projeto busca proporcionar uma experiência de compra simples e intuitiva, permitindo que os usuários naveguem pelo catálogo, pesquisem produtos, adicionem itens ao carrinho e realizem pedidos online.

## 🚀 Tecnologias utilizadas

### Backend
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- Maven

### Frontend
- HTML5
- CSS3
- JavaScript puro (Vanilla JS)

## 📋 Funcionalidades

- [x] Catálogo de produtos.
- [x] Busca por nome e marca.
- [x] Filtro por categorias.
- [x] Cadastro de usuários.
- [x] Login e autenticação por token.
- [x] Carrinho de compras.
- [x] Finalização de pedidos.
- [x] Validação de estoque.
- [x] Histórico de pedidos.
- [ ] Integração com pagamento Pix e cartão.
- [ ] Cálculo de frete por CEP.
- [ ] Painel administrativo.
- [ ] Página individual de produtos.
- [ ] Integração com PostgreSQL.

## 💻 Como executar o projeto no VS Code

### 1. Pré-requisitos

Antes de iniciar, instale:

- JDK 21 ou superior.
- Visual Studio Code.
- Extensão Extension Pack for Java.
- Extensão Spring Boot Extension Pack.
- Maven (caso não utilize o Maven Wrapper do projeto).

### 2. Abrir o projeto

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

- **Loja virtual:** http://localhost:8080
- **Console H2:** http://localhost:8080/h2-console

## 🗄️ Banco de dados

O projeto utiliza o banco de dados **H2**, configurado para armazenamento em arquivo local.

O banco é criado automaticamente durante a inicialização da aplicação.

**Configurações:**

| Propriedade | Valor |
|---|---|
| Banco | H2 Database |
| Tipo | Arquivo local |
| Caminho | `./data/dropspace` |
| JDBC URL | `jdbc:h2:file:./data/dropspace` |
| Usuário | `sa` |
| Senha | Vazia |

**Principais tabelas:**
- `users`: dados dos usuários.
- `product`: produtos cadastrados.
- `orders`: pedidos realizados.
- `order_item`: itens pertencentes aos pedidos.

### Acessar o console H2

1. Inicie a aplicação.
2. Acesse http://localhost:8080/h2-console.
3. Informe os dados de conexão apresentados acima.
4. Clique em Connect.

## 🔌 API REST

A aplicação disponibiliza endpoints REST para comunicação entre o frontend e o backend.

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/products?q=&category=` | Lista e pesquisa produtos. |
| GET | `/api/products/{id}` | Consulta detalhes de um produto. |
| POST | `/api/auth/register` | Realiza cadastro de usuário. |
| POST | `/api/auth/login` | Realiza login e retorna token. |
| GET | `/api/me` | Consulta dados do usuário autenticado. |
| POST | `/api/orders` | Finaliza pedido e atualiza estoque. |
| GET | `/api/orders` | Lista pedidos do usuário autenticado. |

### Autenticação

As rotas protegidas utilizam autenticação por token.

Para acessar esses endpoints, envie o cabeçalho:

```http
Authorization: Bearer <token>
```

O token é obtido após o cadastro ou login.

## 📁 Estrutura do projeto

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

*Estrutura ilustrativa dos principais diretórios e arquivos.*

## 🔮 Próximas melhorias

O projeto possui possibilidades de evolução, incluindo:

- Integração com gateways de pagamento (Pix e cartão).
- Desenvolvimento de página individual para cada produto.
- Cadastro de endereço e cálculo de frete por CEP.
- Painel administrativo para gerenciamento de produtos e pedidos.
- Implementação do Spring Security com JWT.
- Migração do banco H2 para PostgreSQL.
- Melhorias de segurança, testes e desempenho.

## 👥 Equipe de desenvolvimento

Projeto desenvolvido por estudantes do curso de Análise e Desenvolvimento de Sistemas (ADS).

| Integrante | Responsabilidade |
|---|---|
| Thiago de Deus Freire | Gestão do projeto e versionamento no GitHub. |
| Ismael Sampaio Barros | Desenvolvimento e arquitetura da aplicação. |
| Fernando Ferreira | Desenvolvimento e arquitetura da aplicação. |

**Instituição:** Faculdade de Tecnologia e Inovação Senac-DF.

## 🔗 Repositório

[GitHub — Thiago061/DropSpace](https://github.com/Thiago061/DropSpace)

---

<p align="center">
  **DropSpace — Seu espaço para descobrir, explorar e comprar.** 🛒
</p>
