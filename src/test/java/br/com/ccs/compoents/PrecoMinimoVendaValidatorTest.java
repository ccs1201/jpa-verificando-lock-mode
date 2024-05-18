package br.com.ccs.compoents;

import br.com.ccs.entities.Produto;
import br.com.ccs.exceptions.PrecoVendaInvalidoException;
import br.com.ccs.repositories.ProdutoRepository;
import br.com.ccs.validators.PrecoMinimoVendaValidator;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
class PrecoMinimoVendaValidatorTest {

    @Inject
    private ProdutoRepository produtoRepository;
    @Inject
    private PrecoMinimoVendaValidator precoMinimoVendaValidator;
    private Produto produto;

    @BeforeEach
    void setUp() {
        produto = Produto.builder()
                .nome("Coca-cola")
                .valorVenda(BigDecimal.valueOf(155.79))
                .valorCompra(BigDecimal.valueOf(100.00))
                .id(UUID.fromString("c8c8c8c8-c8c8-c8c8-c8c8-c8c8c8c8c8c8"))
                .build();
        produtoRepository.saveAndFlush(produto);
    }

    @AfterEach
    void tearDown() {
        produtoRepository.deleteAll();
    }

    @Test
    @Transactional
    void validarValorMinimoVendaQuandoValorVendaMaiorQueMinimo() {
        assertDoesNotThrow(() -> precoMinimoVendaValidator.validarValorMinimoVenda(List.of(produto)));
    }

    @Test
    @Transactional
    void validarValorMinimoVendaQuandoValorVendaMenorQueMinimo() {
        produto.setValorCompra(BigDecimal.valueOf(150.00));
        produtoRepository.saveAndFlush(produto);
        assertThrows(PrecoVendaInvalidoException.class, () -> precoMinimoVendaValidator.validarValorMinimoVenda(List.of(produto)));
    }
}