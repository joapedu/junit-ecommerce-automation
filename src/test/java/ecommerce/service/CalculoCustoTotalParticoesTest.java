package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

@DisplayName("Testes de Partições de Domínio - calcularCustoTotal")
public class CalculoCustoTotalParticoesTest
{
	private CompraService service;

	@BeforeEach
	public void setUp()
	{
		service = new CompraService(null, null, null, null);
	}

	@Test
	@DisplayName("P1: Peso total entre 0 e 5 kg (faixa isenta)")
	public void testPesoFaixaA_Isenta()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("2.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 4kg, frete isento").isEqualByComparingTo("200.00");
	}

	@Test
	@DisplayName("P2: Peso total entre 5.01 e 10 kg (R$ 2/kg)")
	public void testPesoFaixaB_2ReaisPorKg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("3.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 6kg, R$2/kg + R$12").isEqualByComparingTo("224.00");
	}

	@Test
	@DisplayName("P3: Peso total entre 10.01 e 50 kg (R$ 4/kg)")
	public void testPesoFaixaC_4ReaisPorKg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 12kg, R$4/kg + R$12").isEqualByComparingTo("260.00");
	}

	@Test
	@DisplayName("P4: Peso total acima de 50 kg (R$ 7/kg)")
	public void testPesoFaixaD_7ReaisPorKg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("30.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 60kg, R$7/kg + R$12").isEqualByComparingTo("632.00");
	}

	@Test
	@DisplayName("P5: Subtotal até R$ 500 (sem desconto por valor)")
	public void testSubtotalAte500_SemDesconto()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 4L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$400, sem desconto por valor").isEqualByComparingTo("400.00");
	}

	@Test
	@DisplayName("P6: Subtotal entre R$ 500.01 e R$ 1000 (10% desconto)")
	public void testSubtotalEntre500E1000_Desconto10Porcento()
	{
		Produto produto = criarProduto(1L, new BigDecimal("150.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 5L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$750, 10% desconto").isEqualByComparingTo("675.00");
	}

	@Test
	@DisplayName("P7: Subtotal acima de R$ 1000 (20% desconto)")
	public void testSubtotalAcima1000_Desconto20Porcento()
	{
		Produto produto = criarProduto(1L, new BigDecimal("300.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 4L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$1200, 20% desconto").isEqualByComparingTo("960.00");
	}

	@Test
	@DisplayName("P8: 3 a 4 itens de mesmo tipo (5% desconto)")
	public void test3A4ItensMesmoTipo_Desconto5Porcento()
	{
		Produto produto1 = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		Produto produto2 = criarProduto(2L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		Produto produto3 = criarProduto(3L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item1 = criarItemCompra(1L, produto1, 1L);
		ItemCompra item2 = criarItemCompra(2L, produto2, 1L);
		ItemCompra item3 = criarItemCompra(3L, produto3, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item1, item2, item3));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("3 itens LIVRO, 5% desconto no tipo").isEqualByComparingTo("285.00");
	}

	@Test
	@DisplayName("P9: 5 a 7 itens de mesmo tipo (10% desconto)")
	public void test5A7ItensMesmoTipo_Desconto10Porcento()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 5; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("5 itens LIVRO, 10% desconto no tipo").isEqualByComparingTo("450.00");
	}

	@Test
	@DisplayName("P10: 8 ou mais itens de mesmo tipo (15% desconto)")
	public void test8OuMaisItensMesmoTipo_Desconto15Porcento()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 8; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("8 itens LIVRO, 15% desconto tipo + 10% desconto valor + frete").isEqualByComparingTo("640.00");
	}

	@Test
	@DisplayName("P11: Cliente tipo PRATA (50% desconto no frete)")
	public void testClientePrata_Desconto50PorcentoFrete()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.PRATA);

		assertThat(total).as("Cliente PRATA, 50% desconto no frete").isEqualByComparingTo("112.00");
	}

	@Test
	@DisplayName("P12: Cliente tipo OURO (100% desconto no frete)")
	public void testClienteOuro_FreteGratis()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.OURO);

		assertThat(total).as("Cliente OURO, frete grátis").isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("P13: Região NORDESTE (multiplicador 1.10)")
	public void testRegiaoNordeste_Multiplicador110()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Região NORDESTE, multiplicador 1.10").isEqualByComparingTo("126.40");
	}

	@Test
	@DisplayName("P14: Região NORTE (multiplicador 1.30)")
	public void testRegiaoNorte_Multiplicador130()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORTE, TipoCliente.BRONZE);

		assertThat(total).as("Região NORTE, multiplicador 1.30").isEqualByComparingTo("131.20");
	}

	@Test
	@DisplayName("P15: Produto frágil (R$ 5 por unidade)")
	public void testProdutoFragil_TaxaDe5ReaisPorUnidade()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), true, TipoProduto.ELETRONICO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Produto frágil, 2 unidades, frete com taxa frágil + taxa mínima").isEqualByComparingTo("270.00");
	}

	private Produto criarProduto(Long id, BigDecimal preco, BigDecimal peso, Boolean fragil, TipoProduto tipo)
	{
		return new Produto(id, "Produto " + id, "Descrição", preco, peso, 
			new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("10.0"), fragil, tipo);
	}

	private ItemCompra criarItemCompra(Long id, Produto produto, Long quantidade)
	{
		return new ItemCompra(id, produto, quantidade);
	}

	private CarrinhoDeCompras criarCarrinho(List<ItemCompra> itens)
	{
		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		carrinho.setItens(itens);
		return carrinho;
	}
}
