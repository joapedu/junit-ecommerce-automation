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

@DisplayName("Testes de Tabela de Decisão - calcularCustoTotal")
public class CalculoCustoTotalTabelaDecisaoTest
{
	private CompraService service;

	@BeforeEach
	public void setUp()
	{
		service = new CompraService(null, null, null, null);
	}

	@Test
	@DisplayName("TD1: Subtotal≤500, Peso≤5kg, <3itens/tipo, NãoFragil, Bronze, Sudeste")
	public void testRegra1()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("2.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Sem descontos, frete isento").isEqualByComparingTo("200.00");
	}

	@Test
	@DisplayName("TD2: Subtotal>500, Peso>5kg, <3itens/tipo, NãoFragil, Bronze, Sudeste")
	public void testRegra2()
	{
		Produto produto = criarProduto(1L, new BigDecimal("300.00"), new BigDecimal("3.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("10% desconto valor, frete R$2/kg").isEqualByComparingTo("564.00");
	}

	@Test
	@DisplayName("TD3: Subtotal>1000, Peso>10kg, <3itens/tipo, NãoFragil, Bronze, Sudeste")
	public void testRegra3()
	{
		Produto produto = criarProduto(1L, new BigDecimal("600.00"), new BigDecimal("6.0"), false, TipoProduto.LIVRO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("20% desconto valor, frete R$4/kg").isEqualByComparingTo("1020.00");
	}

	@Test
	@DisplayName("TD4: Subtotal≤500, Peso≤5kg, 3-4itens/tipo, NãoFragil, Bronze, Sudeste")
	public void testRegra4()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 3; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("100.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("5% desconto tipo, frete isento").isEqualByComparingTo("285.00");
	}

	@Test
	@DisplayName("TD5: Subtotal≤500, Peso>5kg, <3itens/tipo, Fragil, Bronze, Sudeste")
	public void testRegra5()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("3.0"), true, TipoProduto.ELETRONICO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

		assertThat(total).as("Frete R$2/kg + taxa frágil R$10").isEqualByComparingTo("234.00");
	}

	@Test
	@DisplayName("TD6: Subtotal>500, Peso>5kg, 3-4itens/tipo, NãoFragil, Prata, Sudeste")
	public void testRegra6()
	{
		Produto produto1 = criarProduto(1L, new BigDecimal("200.00"), new BigDecimal("2.0"), false, TipoProduto.LIVRO);
		Produto produto2 = criarProduto(2L, new BigDecimal("200.00"), new BigDecimal("2.0"), false, TipoProduto.LIVRO);
		Produto produto3 = criarProduto(3L, new BigDecimal("200.00"), new BigDecimal("2.0"), false, TipoProduto.LIVRO);
		ItemCompra item1 = criarItemCompra(1L, produto1, 1L);
		ItemCompra item2 = criarItemCompra(2L, produto2, 1L);
		ItemCompra item3 = criarItemCompra(3L, produto3, 1L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item1, item2, item3));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.PRATA);

		BigDecimal subtotal = new BigDecimal("600.00");
		BigDecimal descontoTipo = subtotal.multiply(new BigDecimal("0.05"));
		BigDecimal subtotalAposDescontoTipo = subtotal.subtract(descontoTipo);
		BigDecimal descontoValor = subtotalAposDescontoTipo.multiply(new BigDecimal("0.10"));
		BigDecimal subtotalFinal = subtotalAposDescontoTipo.subtract(descontoValor);
		BigDecimal frete = new BigDecimal("6.0").multiply(new BigDecimal("2.00")).add(new BigDecimal("12.00"));
		BigDecimal freteComDesconto = frete.multiply(new BigDecimal("0.50"));
		BigDecimal esperado = subtotalFinal.add(freteComDesconto);

		assertThat(total).as("5% desconto tipo + 10% desconto valor + 50% desconto frete").isEqualByComparingTo(esperado);
	}

	@Test
	@DisplayName("TD7: Subtotal>1000, Peso>10kg, 5-7itens/tipo, Fragil, Ouro, Sul")
	public void testRegra7()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 5; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("250.00"), new BigDecimal("3.0"), true, TipoProduto.ELETRONICO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUL, TipoCliente.OURO);

		BigDecimal subtotal = new BigDecimal("1250.00");
		BigDecimal descontoTipo = subtotal.multiply(new BigDecimal("0.10"));
		BigDecimal subtotalAposDescontoTipo = subtotal.subtract(descontoTipo);
		BigDecimal descontoValor = subtotalAposDescontoTipo.multiply(new BigDecimal("0.20"));
		BigDecimal esperado = subtotalAposDescontoTipo.subtract(descontoValor);

		assertThat(total).as("10% desconto tipo + 20% desconto valor + frete grátis OURO").isEqualByComparingTo(esperado);
	}

	@Test
	@DisplayName("TD8: Subtotal>500, Peso>50kg, <3itens/tipo, NãoFragil, Bronze, Norte")
	public void testRegra8()
	{
		Produto produto = criarProduto(1L, new BigDecimal("300.00"), new BigDecimal("30.0"), false, TipoProduto.MOVEL);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORTE, TipoCliente.BRONZE);

		BigDecimal subtotal = new BigDecimal("600.00");
		BigDecimal descontoValor = subtotal.multiply(new BigDecimal("0.10"));
		BigDecimal subtotalFinal = subtotal.subtract(descontoValor);
		BigDecimal freteBase = new BigDecimal("60.0").multiply(new BigDecimal("7.00"));
		BigDecimal freteTotalSemRegiao = freteBase.add(new BigDecimal("12.00"));
		BigDecimal freteComRegiao = freteTotalSemRegiao.multiply(new BigDecimal("1.30"));
		BigDecimal esperado = subtotalFinal.add(freteComRegiao);

		assertThat(total).as("10% desconto valor + frete R$7/kg × 1.30 região Norte").isEqualByComparingTo(esperado);
	}

	@Test
	@DisplayName("TD9: Acúmulo de todos os descontos máximos")
	public void testRegra9_TodosDescontosMaximos()
	{
		List<ItemCompra> itens = new ArrayList<>();
		for (int i = 1; i <= 8; i++)
		{
			Produto produto = criarProduto((long) i, new BigDecimal("200.00"), new BigDecimal("1.0"), false, TipoProduto.LIVRO);
			itens.add(criarItemCompra((long) i, produto, 1L));
		}
		CarrinhoDeCompras carrinho = criarCarrinho(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.OURO);

		BigDecimal subtotal = new BigDecimal("1600.00");
		BigDecimal descontoTipo = subtotal.multiply(new BigDecimal("0.15"));
		BigDecimal subtotalAposDescontoTipo = subtotal.subtract(descontoTipo);
		BigDecimal descontoValor = subtotalAposDescontoTipo.multiply(new BigDecimal("0.20"));
		BigDecimal esperado = subtotalAposDescontoTipo.subtract(descontoValor);

		assertThat(total).as("15% desc tipo + 20% desc valor + frete grátis").isEqualByComparingTo(esperado);
	}

	@Test
	@DisplayName("TD10: Frete máximo com todos os acréscimos")
	public void testRegra10_FreteMaximo()
	{
		Produto produto = criarProduto(1L, new BigDecimal("100.00"), new BigDecimal("30.0"), true, TipoProduto.ELETRONICO);
		ItemCompra item = criarItemCompra(1L, produto, 2L);
		CarrinhoDeCompras carrinho = criarCarrinho(List.of(item));

		BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORTE, TipoCliente.BRONZE);

		BigDecimal subtotal = new BigDecimal("200.00");
		BigDecimal peso = new BigDecimal("60.0");
		BigDecimal freteBase = peso.multiply(new BigDecimal("7.00"));
		BigDecimal taxaMinima = new BigDecimal("12.00");
		BigDecimal taxaFragil = new BigDecimal("5.00").multiply(new BigDecimal("2"));
		BigDecimal freteSemRegiao = freteBase.add(taxaMinima).add(taxaFragil);
		BigDecimal freteComRegiao = freteSemRegiao.multiply(new BigDecimal("1.30"));
		BigDecimal esperado = subtotal.add(freteComRegiao);

		assertThat(total).as("Frete R$7/kg + taxa mínima + frágil + região Norte").isEqualByComparingTo(esperado);
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
