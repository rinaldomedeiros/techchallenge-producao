# TechChallenge Produção

Este repositório contém o microsserviço responsável por atualizar o status dos pedidos conforme a preparação é realizada pela cozinha.

O microsserviço foi desenvolvido em java utilizando o framework Springboot e tem como dependência o Redis e o RabbitMQ.

---

## 1. Objetivo do Microsserviço

O objetivo deste microsserviço é atualizar o status do pedido conforme a preparação é realizada pela cozinha. Para isso, ele:

- Consome eventos de pedidos a partir da fila `ORDER_CONFIRMED_QUEUE`.
- Após atualizar o status do pedido, publica um evento na fila `UPDATED_ORDER_QUEUE`.

---

## 2.Endpoints

O microsserviço disponibiliza os seguintes endpoints:

- **PUT `/order-production/orders/{orderId}/status`**  
  Atualiza o status de um pedido específico.

- **GET `/order-production/orders/{orderId}`**  
  Retorna as informações de um pedido específico.

- **GET `/order-production/orders/status?`**  
  Consulta pedidos com base em seu status (parâmetros opcionais podem ser utilizados para filtrar a consulta).

---

## 3. Base de Dados

Os dados são armazenados utilizando o **Redis**, onde:

- **Key**: `orderId`
- **Value**: Todas as informações adicionais referentes ao pedido.

---

## 4. Desenho de Solução



---

## 5. Como Executar o Projeto Localmente

Para executar o projeto localmente, siga os passos abaixo:

1. Acesse a raiz do projeto.
2. Execute o comando:

   ```bash
   docker compose up --build

## 6. Execução de Testes e Cobertura 

Está configurada um arquivo de workflow do git com a execução de testes unitários e um cenário de execução BDD ( teste integrado ) 

O gerenciador do projeto é o maven e comando utilizado para realizar a execução de testes é : 

```bash
   mvn clean verify 

Abaixo seguem as evidências da cobertura total de testes unitários ( considerando as classes de service, repository e controller ) 

