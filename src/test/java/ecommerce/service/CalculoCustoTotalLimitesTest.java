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

@DisplayName("Testes de Valores Limites - calcularCustoTotal")
public class CalculoCustoTotalLimitesTest
{
	private CompraService service;

	@BeforeEach
	public void setUp()
	{
		service = new CompraService(null, null, null, null);
	}

	@Test
	@DisplayName("L1: Peso exatamente 5 kg (limite superior faixa A)")
	public void testPesoExatamente5Kg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("5.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 5kg, ainda isento").isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("L2: Peso 5.01 kg (limite inferior faixa B)")
	public void testPeso501Kg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("5.01"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 5.01kg, R$2/kg").isEqualByComparingTo("122.02");
	}

	@Test
	@DisplayName("L3: Peso exatamente 10 kg (limite superior faixa B)")
	public void testPesoExatamente10Kg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("10.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 10kg, R$2/kg").isEqualByComparingTo("132.00");
	}

	@Test
	@DisplayName("L4: Peso 10.01 kg (limite inferior faixa C)")
	public void testPeso1001Kg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("10.01"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 10.01kg, R$4/kg").isEqualByComparingTo("152.04");
	}

	@Test
	@DisplayName("L5: Peso exatamente 50 kg (limite superior faixa C)")
	public void testPesoExatamente50Kg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("50.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 50kg, R$4/kg").isEqualByComparingTo("312.00");
	}

	@Test
	@DisplayName("L6: Peso 50.01 kg (limite inferior faixa D)")
	public void testPeso5001Kg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("50.01"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso 50.01kg, R$7/kg").isEqualByComparingTo("462.07");
	}

	@Test
	@DisplayName("L7: Subtotal exatamente R$ 500.00 (limite sem desconto)")
	public void testSubtotalExatamente500()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 5L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$500, sem desconto por valor").isEqualByComparingTo("500.00");
	}

	@Test
	@DisplayName("L8: Subtotal R$ 500.01 (limite inferior 10% desconto)")
	public void testSubtotal50001()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.002"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 5L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$500.01, 10% desconto").isEqualByComparingTo("450.01");
	}

	@Test
	@DisplayName("L9: Subtotal exatamente R$ 1000.00 (limite 10% desconto)")
	public void testSubtotalExatamente1000()
	{
		Produto produto = criarProduto(1L, new BigDecimal("200.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 5L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$1000, 10% desconto").isEqualByComparingTo("900.00");
	}

	@Test
	@DisplayName("L10: Subtotal R$ 1000.01 (limite inferior 20% desconto)")
	public void testSubtotal100001()
	{
		Produto produto = criarProduto(1L, new BigDecimal("200.002"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 5L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal R$1000.01, 20% desconto").isEqualByComparingTo("800.01");
	}

	@Test
	@DisplayName("L11: Exatamente 3 itens de mesmo tipo (limite inferior 5% desconto)")
	public void testExatamente3ItensMesmoTipo()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 3; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("3 itens LIVRO, 5% desconto").isEqualByComparingTo("285.00");
	}

	@Test
	@DisplayName("L12: Exatamente 5 itens de mesmo tipo (limite inferior 10% desconto)")
	public void testExatamente5ItensMesmoTipo()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 5; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("5 itens LIVRO, 10% desconto").isEqualByComparingTo("450.00");
	}

	@Test
	@DisplayName("L13: Exatamente 8 itens de mesmo tipo (limite inferior 15% desconto)")
	public void testExatamente8ItensMesmoTipo()
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
