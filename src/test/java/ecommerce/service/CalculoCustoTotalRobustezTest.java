package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

@DisplayName("Testes de Robustez e Validação - calcularCustoTotal")
public class CalculoCustoTotalRobustezTest
{
	private CompraService service;

	@BeforeEach
	public void setUp()
	{
		service = new CompraService(null, null, null, null);
	}

	@Test
	@DisplayName("R1: Carrinho nulo deve lançar exceção")
	public void testCarrinhoNulo()
	{
		assertThatThrownBy(() -> service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Carrinho não pode ser nulo");
	}

	@Test
	@DisplayName("R2: Carrinho com lista de itens vazia deve lançar exceção")
	public void testCarrinhoComItensVazios()
	{
		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		carrinho.setItens(new ArrayList<>());

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Carrinho não pode ser nulo ou vazio");
	}

	@Test
	@DisplayName("R3: Região nula deve lançar exceção")
	public void testRegiaoNula()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, null, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Região não pode ser nula");
	}

	@Test
	@DisplayName("R4: Tipo de cliente nulo deve lançar exceção")
	public void testTipoClienteNulo()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Tipo de cliente não pode ser nulo");
	}

	@Test
	@DisplayName("R5: Quantidade zero deve lançar exceção")
	public void testQuantidadeZero()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 0L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Quantidade deve ser maior que zero");
	}

	@Test
	@DisplayName("R6: Quantidade negativa deve lançar exceção")
	public void testQuantidadeNegativa()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, -1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Quantidade deve ser maior que zero");
	}

	@Test
	@DisplayName("R7: Produto nulo deve lançar exceção")
	public void testProdutoNulo()
	{
		ItemCompra item = new ItemCompra(1L, null, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Produto não pode ser nulo");
	}

	@Test
	@DisplayName("R8: Preço negativo deve lançar exceção")
	public void testPrecoNegativo()
	{
		Produto produto = criarProduto(1L, new BigDecimal("-100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Preço não pode ser nulo ou negativo");
	}

	@Test
	@DisplayName("R9: Peso físico negativo deve lançar exceção")
	public void testPesoFisicoNegativo()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("-1.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Peso físico não pode ser nulo ou negativo");
	}

	@Test
	@DisplayName("R10: Dimensões negativas devem lançar exceção")
	public void testDimensoesNegativas()
	{
		Produto produto = new Produto(1L, "Produto", "Desc", new BigDecimal("100.00"), new BigDecimal("1.0"), 
			new BigDecimal("-10.0"), new BigDecimal("10.0"), new BigDecimal("10.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Dimensões não podem ser negativas");
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
