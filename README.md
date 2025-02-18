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
  Consulta pedidos com base em seu status de acordo com a etapa do pedido são eles "RECEIVED", "IN_PREPARATION", "READY", "FINISHED" 


---

## 3. Base de Dados

Os dados são armazenados utilizando o **Redis**, onde:

- **Key**: `orderId`
- **Value**: Todas as informações adicionais referentes ao pedido.

---

## 4. Desenho de Solução

O microsserviço de produção está sempre recebendo eventos através da fila RabbitMQ de *CONFIRMED_ORDER_QUEUE* do microsserviço de pedido.

Conforme os status de pedido são atualizados são produzidos eventos na fila *UPDATED_ORDER_QUEUE* para atualizar o status do pedido na base de pedido mantendo a consistência de dados a cada atualização. 

A persistência de dados é realizada através do Redis que mantêm como key para recuperação de informações do pedido o orderId.

![Desenho de Solução Microsserviços](./assets/TechChallenge-Modulo4%20-%20Desenho%20de%20Solução.png)

---

## 5. Como Executar o Projeto Localmente

Para executar o projeto localmente, siga os passos abaixo:

1. Acesse a raiz do projeto.
2. Execute o comando:

   ```bash
   docker compose up --build

   ```

   3.Após a conclusão do build acesse o `localhost:8083/swagger.ui.html` e realize o teste nos endpoints disponiveis 

## 6. Execução de Testes e Cobertura 

Está configurada um arquivo de workflow do git com a execução de testes unitários e um cenário de execução BDD ( teste integrado ) 

O gerenciador do projeto é o maven e comando utilizado para realizar a execução de testes é : 

```bash
   mvn clean verify 

```

Abaixo seguem as evidências da cobertura total de testes unitários ( considerando as classes de service, repository e controller. Não foram incluídas classes de configuração na cobertura por não conter lógica e regras de negócio) 

1. Execução de Testes Unitários :

![Execução de Testes Unitários](./assets/Testes%20Executados%20MVN%20Clean%20Package%20.png)

2. Execução do Cenário BDD : 

![Execução do Cenário BDD](./assets/Scenario%20BDD%20Executado.png)

3. Cobertura total de Testes Unitários : 

![Cobertura de Testes Unitários](./assets/Cobertura%20Total%20de%20Testes.png)

4. Build com sucesso da Action no Github : 

![Build da Action  Executado com Sucesso](./assets/Visão%20do%20Build%20Executado.png)

Segue o link da action para conferência : (https://github.com/rinaldomedeiros/techchallenge-producao/actions/runs/13383120500/job/37375048880)
