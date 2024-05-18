package br.com.ccs.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
public class ItemPedido {

    @Id
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produto produto;
    private Integer quantidade;
    private BigDecimal valorUnitario;

    public void setProduto(Produto produto) {
        this.produto = produto;
        this.valorUnitario = produto.getValorVenda();
    }
}
