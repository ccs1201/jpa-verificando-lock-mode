package br.com.ccs.services;

import br.com.ccs.validators.PrecoMinimoVendaValidator;
import br.com.ccs.entities.ItemPedido;
import br.com.ccs.entities.Produto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemPedidoService {

    private final PrecoMinimoVendaValidator precoMinimoVendaValidator;

    public void validarItensPedido(List<ItemPedido> itens) {
        precoMinimoVendaValidator.validarValorMinimoVenda(toListProdutos(itens));
    }

    private static List<Produto> toListProdutos(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::getProduto)
                .toList();
    }
}
