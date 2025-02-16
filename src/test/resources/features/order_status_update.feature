Feature: Atualização de Status do Pedido e Envio para a Fila

  Como sistema de produção de pedidos
  Quero que, quando o status de um pedido for atualizado,
  O pedido seja enviado para a fila de Pedidos Atualizados e possa ser consultado com o status atualizado

  Scenario: Atualização de status de um pedido
  Given que recebo um pedido com status "RECEIVED"
  When atualizo o status do pedido para "IN_PREPARATION"
  Then o pedido é enviado para a fila de pedidos atualizados
  And ao consultar o pedido, o status deve ser "IN_PREPARATION"
