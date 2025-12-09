package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;

class CalculoCustoTotalTest {

    private CompraService compraService;

    @BeforeEach
    void setUp() {
        compraService = new CompraService(null, null, null, null);
    }

    private ItemCompra criarItem(BigDecimal preco, BigDecimal peso, Long quantidade, boolean fragil) {
        Produto produto = new Produto();
        produto.setPreco(preco);
        produto.setPesoFisico(peso);
        produto.setFragil(fragil);
        
        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        
        return item;
    }

    private CarrinhoDeCompras criarCarrinho(ItemCompra... itens) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        List<ItemCompra> listaItens = new ArrayList<>();
        for (ItemCompra item : itens) {
            listaItens.add(item);
        }
        carrinho.setItens(listaItens);
        return carrinho;
    }

    @Test
    void testeCarrinhoNulo() {
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(null));
    }

    @Test
    void testeCarrinhoItensNulo() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(null);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(carrinho));
    }

    @Test
    void testeCarrinhoVazio() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(new ArrayList<>());
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(carrinho));
    }

    @Test
    void testeSemDescontoSemFrete() {
        // Subtotal < 500 (400), Peso <= 5kg (4kg)
        ItemCompra item = criarItem(new BigDecimal("400.00"), new BigDecimal("4.00"), 1L, false);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);
        // Subtotal: 400
        // Desconto: 0
        // Frete: 0 (<= 5kg)
        // Total: 400
        assertEquals(new BigDecimal("400.00"), total);
    }

    @Test
    void testeSemDescontoComFreteFaixaB() {
        // Subtotal < 500 (100), Peso > 5kg e <= 10kg (6kg)
        ItemCompra item = criarItem(new BigDecimal("100.00"), new BigDecimal("6.00"), 1L, false);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);
        // Subtotal: 100
        // Desconto: 0
        // Frete: 6 * 2 = 12
        // Total: 112
        assertEquals(new BigDecimal("112.00"), total);
    }

    @Test
    void testeDesconto10PorcentoComFreteFaixaC() {
        // Subtotal >= 500 e < 1000 (500), Peso > 10kg e <= 50kg (20kg)
        ItemCompra item = criarItem(new BigDecimal("500.00"), new BigDecimal("20.00"), 1L, false);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);
        // Subtotal: 500
        // Desconto: 500 * 0.10 = 50. SubtotalComDesconto = 450
        // Frete: 20 * 4 = 80
        // Total: 450 + 80 = 530
        assertEquals(new BigDecimal("530.00"), total);
    }

    @Test
    void testeDesconto20PorcentoComFreteFaixaD() {
        // Subtotal >= 1000 (1000), Peso > 50kg (60kg)
        ItemCompra item = criarItem(new BigDecimal("1000.00"), new BigDecimal("60.00"), 1L, false);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);
        // Subtotal: 1000
        // Desconto: 1000 * 0.20 = 200. SubtotalComDesconto = 800
        // Frete: 60 * 7 = 420
        // Total: 800 + 420 = 1220
        assertEquals(new BigDecimal("1220.00"), total);
    }

    @Test
    void testeTaxaFragil() {
        // Subtotal 100, Peso 2kg (isento base), 2 itens frageis
        ItemCompra item = criarItem(new BigDecimal("50.00"), new BigDecimal("1.00"), 2L, true);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);
        // Subtotal: 100
        // Desconto: 0
        // Frete Base: 2kg * 0 = 0
        // Taxa Fragil: 2 * 5.00 = 10.00
        // Total: 110.00
        assertEquals(new BigDecimal("110.00"), total);
    }
    
    @Test
    void testeLimitesDesconto() {
        // Teste limite < 500 (499.99)
        ItemCompra item1 = criarItem(new BigDecimal("499.99"), new BigDecimal("1.00"), 1L, false);
        assertEquals(new BigDecimal("499.99"), compraService.calcularCustoTotal(criarCarrinho(item1)));

        // Teste limite >= 500 (500.00) -> 10%
        ItemCompra item2 = criarItem(new BigDecimal("500.00"), new BigDecimal("1.00"), 1L, false);
        // 500 - 50 = 450
        assertEquals(new BigDecimal("450.00"), compraService.calcularCustoTotal(criarCarrinho(item2)));

        // Teste limite < 1000 (999.99) -> 10%
        ItemCompra item3 = criarItem(new BigDecimal("999.99"), new BigDecimal("1.00"), 1L, false);
        // 999.99 - 99.999 = 899.991 -> 900.00 (arredondamento no final?) 
        // Logic: 999.99 * 0.10 = 99.999. Sub = 899.991. Total = 899.991. Scale 2 half-up = 899.99
        assertEquals(new BigDecimal("899.99"), compraService.calcularCustoTotal(criarCarrinho(item3)));

        // Teste limite >= 1000 (1000.00) -> 20%
        ItemCompra item4 = criarItem(new BigDecimal("1000.00"), new BigDecimal("1.00"), 1L, false);
        // 1000 - 200 = 800
        assertEquals(new BigDecimal("800.00"), compraService.calcularCustoTotal(criarCarrinho(item4)));
    }

    @Test
    void testeLimitesFrete() {
        // Peso 5.00 (Faixa A - isento)
        ItemCompra item1 = criarItem(new BigDecimal("10.00"), new BigDecimal("5.00"), 1L, false);
        assertEquals(new BigDecimal("10.00"), compraService.calcularCustoTotal(criarCarrinho(item1)));

        // Peso 5.01 (Faixa B) -> 5.01 * 2 = 10.02
        ItemCompra item2 = criarItem(new BigDecimal("10.00"), new BigDecimal("5.01"), 1L, false);
        // 10 + 10.02 = 20.02
        assertEquals(new BigDecimal("20.02"), compraService.calcularCustoTotal(criarCarrinho(item2)));

        // Peso 10.00 (Faixa B) -> 10.00 * 2 = 20.00
        ItemCompra item3 = criarItem(new BigDecimal("10.00"), new BigDecimal("10.00"), 1L, false);
        // 10 + 20 = 30
        assertEquals(new BigDecimal("30.00"), compraService.calcularCustoTotal(criarCarrinho(item3)));

        // Peso 10.01 (Faixa C) -> 10.01 * 4 = 40.04
        ItemCompra item4 = criarItem(new BigDecimal("10.00"), new BigDecimal("10.01"), 1L, false);
        // 10 + 40.04 = 50.04
        assertEquals(new BigDecimal("50.04"), compraService.calcularCustoTotal(criarCarrinho(item4)));

        // Peso 50.00 (Faixa C) -> 50.00 * 4 = 200.00
        ItemCompra item5 = criarItem(new BigDecimal("10.00"), new BigDecimal("50.00"), 1L, false);
        // 10 + 200 = 210
        assertEquals(new BigDecimal("210.00"), compraService.calcularCustoTotal(criarCarrinho(item5)));

        // Peso 50.01 (Faixa D) -> 50.01 * 7 = 350.07
        ItemCompra item6 = criarItem(new BigDecimal("10.00"), new BigDecimal("50.01"), 1L, false);
        // 10 + 350.07 = 360.07
        assertEquals(new BigDecimal("360.07"), compraService.calcularCustoTotal(criarCarrinho(item6)));
    }
}

