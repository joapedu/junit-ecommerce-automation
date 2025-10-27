package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
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

@DisplayName("Testes de Cobertura de Arestas e Branch Coverage - calcularCustoTotal")
public class CalculoCustoTotalCoberturaTest
{
	private CompraService service;

	@BeforeEach
	public void setUp()
	{
		service = new CompraService(null, null, null, null);
	}

	@Test
	@DisplayName("C1: Carrinho nulo - branch de validação inicial")
	public void testCarrinhoNulo_BranchValidacao()
	{
		assertThatThrownBy(() -> service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C2: Itens nulos - branch de validação inicial")
	public void testItensNulos_BranchValidacao()
	{
		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		carrinho.setItens(null);
		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C3: Itens vazios - branch de validação inicial")
	public void testItensVazios_BranchValidacao()
	{
		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		carrinho.setItens(new ArrayList<>());
		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C4: Região nula - branch de validação")
	public void testRegiaoNula_BranchValidacao()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, null, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C5: Tipo cliente nulo - branch de validação")
	public void testTipoClienteNulo_BranchValidacao()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C6: Quantidade <= 0 - branch de validação no calcularSubtotal")
	public void testQuantidadeInvalida_BranchValidacao()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 0L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C7: Produto nulo - branch de validação")
	public void testProdutoNulo_BranchValidacao()
	{
		ItemCompra item = new ItemCompra(1L, null, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C8: Preço negativo - branch de validação")
	public void testPrecoNegativo_BranchValidacao()
	{
		Produto produto = criarProduto(1L, new BigDecimal("-100.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C9: quantidadeItens < 3 - sem desconto por tipo")
	public void testMenosDe3Itens_SemDescontoTipo()
	{
		Produto produto1 = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
		Produto produto2 = criarProduto(2L, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
		ItemCompra item1 = criarItemCompra(1L, produto1, 1L);
		ItemCompra item2 = criarItemCompra(2L, produto2, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item1, item2));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("2 itens, sem desconto").isEqualByComparingTo("200.00");
	}

	@Test
	@DisplayName("C10: quantidadeItens >= 8 - 15% desconto")
	public void test8OuMaisItens_15PorcentoDesconto()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 8; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("8 itens, 15% desconto").isEqualByComparingTo("640.00");
	}

	@Test
	@DisplayName("C11: quantidadeItens >= 5 (mas < 8) - 10% desconto")
	public void test5A7Itens_10PorcentoDesconto()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 5; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("5 itens, 10% desconto").isEqualByComparingTo("450.00");
	}

	@Test
	@DisplayName("C12: quantidadeItens >= 3 (mas < 5) - 5% desconto")
	public void test3A4Itens_5PorcentoDesconto()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 3; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("3 itens, 5% desconto").isEqualByComparingTo("285.00");
	}

	@Test
	@DisplayName("C13: percentualDesconto > 0 - aplica desconto por tipo")
	public void testPercentualDescontoMaiorQueZero_AplicaDesconto()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 3; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Desconto aplicado").isLessThan(new BigDecimal("300.00"));
	}

	@Test
	@DisplayName("C14: subtotal > 1000 - 20% desconto")
	public void testSubtotalAcima1000_20PorcentoDesconto()
	{
		Produto produto = criarProduto(1L, new BigDecimal("1200.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal > 1000, 20% desconto").isEqualByComparingTo("960.00");
	}

	@Test
	@DisplayName("C15: subtotal > 500 (mas <= 1000) - 10% desconto")
	public void testSubtotalEntre500E1000_10PorcentoDesconto()
	{
		Produto produto = criarProduto(1L, new BigDecimal("600.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal > 500, 10% desconto").isEqualByComparingTo("540.00");
	}

	@Test
	@DisplayName("C16: subtotal <= 500 - sem desconto por valor")
	public void testSubtotalAte500_SemDescontoValor()
	{
		Produto produto = criarProduto(1L, new BigDecimal("400.00"), new BigDecimal("1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Subtotal <= 500, sem desconto").isEqualByComparingTo("400.00");
	}

	@Test
	@DisplayName("C17: pesoTotal > 50 - R$ 7/kg")
	public void testPesoAcima50_7ReaisPorKg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("60.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso > 50, R$7/kg").isEqualByComparingTo("532.00");
	}

	@Test
	@DisplayName("C18: pesoTotal > 10 (mas <= 50) - R$ 4/kg")
	public void testPesoEntre10E50_4ReaisPorKg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("20.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso > 10, R$4/kg").isEqualByComparingTo("192.00");
	}

	@Test
	@DisplayName("C19: pesoTotal > 5 (mas <= 10) - R$ 2/kg")
	public void testPesoEntre5E10_2ReaisPorKg()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("8.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso > 5, R$2/kg").isEqualByComparingTo("128.00");
	}

	@Test
	@DisplayName("C20: pesoTotal <= 5 - frete isento")
	public void testPesoAte5_Isento()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("3.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Peso <= 5, isento").isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("C21: freteBase > 0 - adiciona taxa mínima")
	public void testFreteBaseMaiorQueZero_AdicionaTaxaMinima()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Frete base > 0, com taxa mínima").isEqualByComparingTo("124.00");
	}

	@Test
	@DisplayName("C22: freteBase == 0 - não adiciona taxa mínima")
	public void testFreteBaseZero_SemTaxaMinima()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("3.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Frete base = 0, sem taxa mínima").isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("C23: produto frágil - adiciona taxa de R$ 5")
	public void testProdutoFragil_AdicionaTaxa()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), true);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Produto frágil, taxa adicional").isEqualByComparingTo("270.00");
	}

	@Test
	@DisplayName("C24: produto não frágil - sem taxa adicional")
	public void testProdutoNaoFragil_SemTaxa()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Produto não frágil").isEqualByComparingTo("124.00");
	}

	@Test
	@DisplayName("C25: Cliente OURO - frete zero")
	public void testClienteOuro_FreteZero()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("10.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.OURO);

		assertThat(total).as("Cliente OURO, frete zero").isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("C26: Cliente PRATA - 50% desconto frete")
	public void testClientePrata_50PorcentoDesconto()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("10.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.PRATA);

		assertThat(total).as("Cliente PRATA, 50% desconto frete").isEqualByComparingTo("116.00");
	}

	@Test
	@DisplayName("C27: Cliente BRONZE - frete integral")
	public void testClienteBronze_FreteIntegral()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("10.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Cliente BRONZE, frete integral").isEqualByComparingTo("132.00");
	}

	@Test
	@DisplayName("C28: Todas as regiões - SUDESTE")
	public void testRegiaoSudeste()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Região SUDESTE").isEqualByComparingTo("124.00");
	}

	@Test
	@DisplayName("C29: Todas as regiões - SUL")
	public void testRegiaoSul()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUL, TipoCliente.BRONZE);

		assertThat(total).as("Região SUL").isEqualByComparingTo("125.20");
	}

	@Test
	@DisplayName("C30: Todas as regiões - NORDESTE")
	public void testRegiaoNordeste()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Região NORDESTE").isEqualByComparingTo("126.40");
	}

	@Test
	@DisplayName("C31: Todas as regiões - CENTRO_OESTE")
	public void testRegiaoCentroOeste()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.CENTRO_OESTE, TipoCliente.BRONZE);

		assertThat(total).as("Região CENTRO_OESTE").isEqualByComparingTo("128.80");
	}

	@Test
	@DisplayName("C32: Todas as regiões - NORTE")
	public void testRegiaoNorte()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("6.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORTE, TipoCliente.BRONZE);

		assertThat(total).as("Região NORTE").isEqualByComparingTo("131.20");
	}

	@Test
	@DisplayName("C33: Peso físico > peso cúbico")
	public void testPesoFisicoMaiorQueCubico()
	{
		Produto produto = new Produto(1L, "Produto", "Desc", new BigDecimal("100.00"), 
			new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("10.0"), 
			new BigDecimal("10.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Usa peso físico").isEqualByComparingTo("132.00");
	}

	@Test
	@DisplayName("C34: Peso cúbico > peso físico")
	public void testPesoCubicoMaiorQueFisico()
	{
		Produto produto = new Produto(1L, "Produto", "Desc", new BigDecimal("100.00"), 
			new BigDecimal("1.0"), new BigDecimal("50.0"), new BigDecimal("50.0"), 
			new BigDecimal("50.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Usa peso cúbico").isEqualByComparingTo("195.32");
	}

	@Test
	@DisplayName("C35: Dimensões nulas - retorna peso cúbico zero")
	public void testDimensoesNulas_PesoCubicoZero()
	{
		Produto produto = new Produto(1L, "Produto", "Desc", new BigDecimal("100.00"), 
			new BigDecimal("5.0"), null, null, null, false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Dimensões nulas, peso cúbico = 0").isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("C36: Dimensões negativas - lança exceção")
	public void testDimensoesNegativas_LancaExcecao()
	{
		Produto produto = new Produto(1L, "Produto", "Desc", new BigDecimal("100.00"), 
			new BigDecimal("1.0"), new BigDecimal("-10.0"), new BigDecimal("10.0"), 
			new BigDecimal("10.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("C37: Peso físico negativo - lança exceção")
	public void testPesoFisicoNegativo_LancaExcecao()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("-1.0"), false);
		ItemCompra item = criarItemCompra(1L, produto, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private Produto criarProduto(Long id, BigDecimal preco, BigDecimal peso, Boolean fragil)
	{
		return new Produto(id, "Produto " + id, "Descrição", preco, peso, 
			new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("10.0"), fragil, TipoProduto.LIVRO);
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

