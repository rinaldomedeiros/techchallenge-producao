Feature: Atualização de Status do Pedido e Envio para a Fila

  Como sistema de produção de pedidos
  Quero que, quando o status de um pedido for atualizado,
  O pedido seja enviado para a fila de Pedidos Atualizados e possa ser consultado com o status atualizado

  Scenario: Atualização de status de um pedido
  Given que recebo um pedido com status "RECEBIDO"
  When atualizo o status do pedido para "EM_PREPARACAO"
  Then o pedido é enviado para a fila de "UPDATED_ORDER_QUEUE"
  And ao consultar o pedido, o status deve ser "EM_PREPARACAO"
