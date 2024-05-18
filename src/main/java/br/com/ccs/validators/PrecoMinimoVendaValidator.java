package br.com.ccs.validators;

import br.com.ccs.entities.Produto;
import br.com.ccs.exceptions.LockModeException;
import br.com.ccs.exceptions.PrecoVendaInvalidoException;
import br.com.ccs.services.ProdutoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrecoMinimoVendaValidator {

    private static final float PERCENTUAL_MINIMO_VENDA = 1.5F;
    private final ProdutoService produtoService;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarValorMinimoVenda(List<Produto> produtos) {
        log.info("\nIniciando validações do valor mínimo de venda dos produtos.");
        var idsProdutos = produtos.stream().map(Produto::getId).toList();

        var produtosLockMap = produtoService.findByIdIn(idsProdutos)
                .stream()
                .collect(Collectors.toMap(Produto::getId, Function.identity()));

        produtos.forEach(produto -> validarValorMinimoVenda(produto, produtosLockMap.get(produto.getId())));
    }

    private void validarValorMinimoVenda(Produto produtoVenda, Produto produtoLock) {
        checkLockMode(produtoLock);
        var valorMinimoVenda = calcularValorMinimoVenda(produtoLock);

        if (valorMinimoVenda.compareTo(produtoVenda.getValorVenda()) > 0) {
            lancarException(produtoVenda, produtoLock, valorMinimoVenda);
        }

        log.info("\nValor mínimo de venda validado com sucesso para o produto: "
                .concat(produtoVenda.getNome())
                .concat("\nValor Venda: ").concat(produtoVenda.getValorVenda().toString())
                .concat("\nValor Compra: ").concat(produtoLock.getValorCompra().toString())
        );
    }

    private BigDecimal calcularValorMinimoVenda(Produto produto) {
        return produto.getValorCompra().multiply(BigDecimal.valueOf(PERCENTUAL_MINIMO_VENDA));
    }

    private void checkLockMode(Produto produto) {
        var lockModeType = entityManager.getLockMode(produto);

        if (LockModeType.PESSIMISTIC_WRITE.compareTo(lockModeType) != 0) {
            throw new LockModeException("\nO LockModeType desta operação deveria ser igual a " + LockModeType.PESSIMISTIC_WRITE);
        }

        log.info("\nLockModeType validado com sucesso para o produto: " + produto.getNome());
    }

    private static void lancarException(Produto produtoVenda, Produto produtoLock, BigDecimal valorMinimoVenda) {
        throw new PrecoVendaInvalidoException(
                "\nPreço de venda menor que o permitido para o produto: "
                        .concat(produtoVenda.getNome())
                        .concat("\nValor Venda: ").concat(produtoVenda.getValorVenda().toString())
                        .concat("\nValor Compra: ").concat(produtoLock.getValorCompra().toString())
                        .concat("\nValor Mínimo Venda: ").concat(valorMinimoVenda.toString())
                        .concat("\nData e Hora da atualização: ")
                        .concat(Objects.isNull(produtoLock.getDataHoraAlteracao()) ? "Não Alterado" : produtoLock.getDataHoraAlteracao().toString())
        );
    }
}
