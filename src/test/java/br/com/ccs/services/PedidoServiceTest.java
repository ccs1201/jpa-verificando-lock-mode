package br.com.ccs.services;

import br.com.ccs.entities.ItemPedido;
import br.com.ccs.entities.Pedido;
import br.com.ccs.entities.Produto;
import br.com.ccs.exceptions.PrecoVendaInvalidoException;
import br.com.ccs.exceptions.RepositoryException;
import br.com.ccs.repositories.PedidoRepository;
import br.com.ccs.repositories.ProdutoRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class PedidoServiceTest {

    @Inject
    private PedidoService pedidoService;
    @Inject
    private ProdutoRepository produtoRepository;
    @Inject
    private ProdutoService produtoService;
    @Inject
    private PedidoRepository pedidoRepository;

    @AfterEach
    void tearDown() {
        pedidoRepository.deleteAll();
    }

    /**
     * Neste cenário queremos testar uma situação de concorrência onde,
     * há uma tentativa de atualizar o preço de compra durante um processo
     * de validação do valor mínimo de venda.
     * <p>
     * Nossa regra de negócio define que, iniciada uma validação
     * os produtos devem ficar bloqueados para atualizações até o fim
     * da validação.
     */
    @Test
    void testQuandoValorCompraAlteradoDuranteExecucaoSucesso() {

        log.info("\nTeste >>>> Iniciando validação antes da atualização do valor compra\n");

        var futures = new CompletableFuture[2];
        var pedido = Pedido.builder()
                .id(UUID.randomUUID())
                .build();

        pedido.setItens(criarItensPedido(10, pedido));

        var novoValorCompra = BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_EVEN);

        futures[0] = CompletableFuture.runAsync(() -> assertDoesNotThrow(() -> pedidoService.save(pedido)));
        futures[1] = CompletableFuture.runAsync(() -> {
            try {
                /*
                Vamos forçar uma pausa de 5ms para dar chance
                desta thread entrar e simular a concorrência
                ao tentar atualizar o valor de compra dos produtos,
                durante a execução de validações do valor mínimo de venda.
                 */
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            produtoService.atualizarValorCompraProdutos(novoValorCompra);
        });

        CompletableFuture.allOf(futures).join();

        var produtosPrecoAtualizado = produtoRepository.findAll();

        produtosPrecoAtualizado.forEach(p -> {
            log.info("\nExecutando assertEqual do valor de compra para o produto: {}", p.getNome());
            assertEquals(novoValorCompra, p.getValorCompra());
        });

        log.info("\nFim Teste >>>> Iniciando validação antes da atualização do valor compra\n");
    }

    /**
     * Neste cenário queremos testar uma situação de concorrência onde,
     * há uma tentativa de atualizar o preço de compra anterior um há um
     * processo de validação do valor mínimo de venda.
     * <p>
     * Nossa regra de negócio define que, iniciada uma validação
     * os produtos devem ficar bloqueados para atualizações até o fim
     * da validação.
     */
    @Test
    void testQuandoValorCompraAlteradoAntesDaExecucao() {
        log.info("\nTeste >>>> Iniciando atualização do valor compra antes da Validação\n");

        var futures = new CompletableFuture[2];
        var pedido = Pedido.builder()
                .id(UUID.randomUUID())
                .build();

        pedido.setItens(criarItensPedido(10, pedido));

        var novoValorCompra = BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_EVEN);

        //inicia a atualização do valor de compra dos produtos
        futures[0] = CompletableFuture.runAsync(() -> {
                    produtoService.atualizarValorCompraProdutos(novoValorCompra);
                    var produtosPrecoAtualizado = produtoRepository.findAll();

                    produtosPrecoAtualizado.forEach(p -> {
                        log.info("\nExecutando assertEqual do valor de compra para o produto: {}", p.getNome());
                        assertEquals(novoValorCompra, p.getValorCompra());
                    });
                },
                Executors.newVirtualThreadPerTaskExecutor());

        //inicia validação do valor mínimo de venda dos produtos
        futures[1] = CompletableFuture.runAsync(() -> {
                    try {
                        /*
                        Atraso para simular o cenário onde a
                        atualização de preços começou antes da validação
                        do valor de venda mínimo.
                         */
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    assertThrows(PrecoVendaInvalidoException.class, () -> pedidoService.save(pedido));
                },
                Executors.newVirtualThreadPerTaskExecutor());

        CompletableFuture.allOf(futures).join();

        log.info("\nFim Teste >>>> Iniciando atualização do valor compra antes da Validação\n");
    }

    /**
     * Neste cenário queremos testar uma situação de concorrência onde,
     * há duas tentativas de atualizar o preço de compra anteriores um há um
     * processo de validação do valor mínimo de venda.
     * <p>
     * Nossa regra de negócio define que, iniciada uma validação
     * os produtos devem ficar bloqueados para atualizações até o fim
     * da validação.
     */
    @Test
    void testQuandoAletrandoValorCompraDuasThreadsSimultaneas() {
        log.info("\nTeste >>>> Iniciando duas atualizações do valor compra antes da Validação\n");

        var futures = new CompletableFuture[3];
        var pedido = Pedido.builder()
                .id(UUID.randomUUID())
                .build();
        pedido.setItens(criarItensPedido(100, pedido));

        //inicia a atualização do valor de compra dos produtos
        futures[0] = CompletableFuture.runAsync(() -> {
                    var novoValorCompra = BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_EVEN);
                    produtoService.atualizarValorCompraProdutos(novoValorCompra);
                    var produtosPrecoAtualizado = produtoRepository.findAll();

                    produtosPrecoAtualizado.forEach(p -> {
                        log.info("\nExecutando assertEqual do valor de compra para o produto: {}", p.getNome());
                        assertEquals(novoValorCompra, p.getValorCompra());
                    });
                },
                Executors.newVirtualThreadPerTaskExecutor());

        //inicia outra atualização do valor de compra dos produtos
        futures[2] = CompletableFuture.runAsync(() -> {
                    var novoValorCompra = BigDecimal.valueOf(300.00).setScale(2, RoundingMode.HALF_EVEN);
                    produtoService.atualizarValorCompraProdutos(novoValorCompra);
                    var produtosPrecoAtualizado = produtoRepository.findAll();

                    produtosPrecoAtualizado.forEach(p -> {
                        log.info("\nExecutando assertEqual do valor de compra para o produto: {}", p.getNome());
                        assertEquals(novoValorCompra, p.getValorCompra());
                    });
                },
                Executors.newVirtualThreadPerTaskExecutor());

        //inicia validação do valor mínimo de venda dos produtos
        futures[1] = CompletableFuture.runAsync(() -> {
                    try {
                        /*
                        Atraso para simular o cenário onde a
                        atualização de preços começou antes da validação
                        do valor de venda mínimo.
                         */
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    assertThrows(RepositoryException.class, () -> pedidoService.save(pedido));
                },
                Executors.newVirtualThreadPerTaskExecutor());

        CompletableFuture.allOf(futures).join();
        log.info("\nFim Teste >>>> Iniciando duas atualizações do valor compra antes da Validação \n");

    }

    private List<ItemPedido> criarItensPedido(int qtd, Pedido pedido) {
        var itens = new LinkedList<ItemPedido>();

        for (var i = 1; i <= qtd; i++) {
            itens.add(
                    ItemPedido.builder()
                            .id(UUID.randomUUID())
                            .produto(
                                    Produto.builder()
                                            .id(UUID.randomUUID())
                                            .nome("Produto " + i)
                                            .valorVenda(BigDecimal.valueOf(155.79D * i).setScale(2, RoundingMode.HALF_EVEN))
                                            .valorCompra(BigDecimal.valueOf(100.00))
                                            .build()
                            )
                            .quantidade(i)
                            .pedido(pedido)
                            .build()
            );
            produtoRepository.saveAndFlush(itens.get(i - 1).getProduto());
        }
        log.info("\nItens Pedido criados com sucesso, produtos cadastrados!");

        return itens;
    }
}