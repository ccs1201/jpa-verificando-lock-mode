package br.com.ccs.services;

import br.com.ccs.entities.Produto;
import br.com.ccs.exceptions.RepositoryException;
import br.com.ccs.repositories.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {
    private final ProdutoRepository produtoRepository;

    @Transactional(readOnly = true)
    public List<Produto> findByIdIn(List<UUID> ids) {
        log.info("\nBuscando Produtos por IDs");
        try {
            var produtos = produtoRepository.findByIdIn(ids);
            log.info("\nProdutos encontrados por IDs agora estão lockados");

            return produtos;

        } catch (CannotAcquireLockException e) {
            throw new RepositoryException("Erro ao buscar produtos por IDs", e);
        }
    }

    @Transactional
    public void atualizarValorCompraProdutos(BigDecimal novoValorCompra) {
        log.info("\nIniciando Atualização do valor de compra dos produtos");
        var produtos = produtoRepository.findAll();
        produtos.forEach(produto -> {
            produto.setValorCompra(novoValorCompra);
            produtoRepository.saveAndFlush(produto);
            log.info("\nAtualizado valor de compra do produto: {} - {} - {}",
                    produto.getNome(), produto.getDataHoraCriacao(), produto.getDataHoraAlteracao());
        });
    }
}
