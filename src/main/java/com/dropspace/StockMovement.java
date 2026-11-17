package com.dropspace;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Histórico de estoque: toda entrada, saída, venda ou ajuste fica registrado (rastreabilidade). */
@Entity
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId;
    public String productName;
    public String type;        // ENTRADA, SAIDA, AJUSTE, VENDA
    public int quantity;       // variação (negativa = saiu)
    public int balanceAfter;   // saldo depois da movimentação
    public String note;
    public Long userId;
    public LocalDateTime createdAt = LocalDateTime.now();

    static StockMovement of(Product p, String type, int delta, String note, Long userId) {
        StockMovement m = new StockMovement();
        m.productId = p.id; m.productName = p.name; m.type = type; m.quantity = delta;
        m.balanceAfter = p.stock; m.note = note; m.userId = userId;
        return m;
    }
}
