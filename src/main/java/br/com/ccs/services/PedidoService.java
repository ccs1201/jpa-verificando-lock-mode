package br.com.ccs.services;

import br.com.ccs.entities.Pedido;
import br.com.ccs.repositories.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoService itemPedidoService;

    @Transactional
    void save(Pedido pedido) {
        itemPedidoService.validarItensPedido(pedido.getItens());
        pedidoRepository.save(pedido);
    }
}
