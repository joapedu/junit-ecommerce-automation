package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService
{

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal)
	{
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId)
	{
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel())
		{
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado())
		{
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso())
		{
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	} 

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente)
	{
		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty())
		{
			throw new IllegalArgumentException("Carrinho não pode ser nulo ou vazio");
		}
		if (regiao == null)
		{
			throw new IllegalArgumentException("Região não pode ser nula");
		}
		if (tipoCliente == null)
		{
			throw new IllegalArgumentException("Tipo de cliente não pode ser nulo");
		}

		BigDecimal subtotal = calcularSubtotal(carrinho);
		BigDecimal subtotalComDesconto = aplicarDescontos(carrinho, subtotal);
		BigDecimal frete = calcularFrete(carrinho, regiao, tipoCliente);
		BigDecimal total = subtotalComDesconto.add(frete);

		return total.setScale(2, java.math.RoundingMode.HALF_UP);
	}

	private BigDecimal calcularSubtotal(CarrinhoDeCompras carrinho)
	{
		BigDecimal subtotal = BigDecimal.ZERO;
		for (var item : carrinho.getItens())
		{
			if (item.getQuantidade() == null || item.getQuantidade() <= 0)
			{
				throw new IllegalArgumentException("Quantidade deve ser maior que zero");
			}
			if (item.getProduto() == null)
			{
				throw new IllegalArgumentException("Produto não pode ser nulo");
			}
			if (item.getProduto().getPreco() == null || item.getProduto().getPreco().compareTo(BigDecimal.ZERO) < 0)
			{
				throw new IllegalArgumentException("Preço não pode ser nulo ou negativo");
			}
			BigDecimal precoItem = item.getProduto().getPreco().multiply(new BigDecimal(item.getQuantidade()));
			subtotal = subtotal.add(precoItem);
		}
		return subtotal;
	}

	private BigDecimal aplicarDescontos(CarrinhoDeCompras carrinho, BigDecimal subtotal)
	{
		BigDecimal subtotalComDescontoTipo = aplicarDescontoPorTipo(carrinho, subtotal);
		BigDecimal subtotalComDesconto = aplicarDescontoPorValor(subtotalComDescontoTipo);
		return subtotalComDesconto;
	}

	private BigDecimal aplicarDescontoPorTipo(CarrinhoDeCompras carrinho, BigDecimal subtotal)
	{
		var itensPorTipo = new java.util.HashMap<ecommerce.entity.TipoProduto, java.util.List<ecommerce.entity.ItemCompra>>();
		for (var item : carrinho.getItens())
		{
			ecommerce.entity.TipoProduto tipo = item.getProduto().getTipo();
			itensPorTipo.computeIfAbsent(tipo, k -> new java.util.ArrayList<>()).add(item);
		}

		BigDecimal descontoTotal = BigDecimal.ZERO;
		for (var entrada : itensPorTipo.entrySet())
		{
			int quantidadeItens = entrada.getValue().size();
			BigDecimal percentualDesconto = BigDecimal.ZERO;

			if (quantidadeItens >= 8)
			{
				percentualDesconto = new BigDecimal("0.15");
			}
			else if (quantidadeItens >= 5)
			{
				percentualDesconto = new BigDecimal("0.10");
			}
			else if (quantidadeItens >= 3)
			{
				percentualDesconto = new BigDecimal("0.05");
			}

			if (percentualDesconto.compareTo(BigDecimal.ZERO) > 0)
			{
				BigDecimal subtotalTipo = BigDecimal.ZERO;
				for (var item : entrada.getValue())
				{
					subtotalTipo = subtotalTipo.add(item.getProduto().getPreco().multiply(new BigDecimal(item.getQuantidade())));
				}
				BigDecimal desconto = subtotalTipo.multiply(percentualDesconto);
				descontoTotal = descontoTotal.add(desconto);
			}
		}

		return subtotal.subtract(descontoTotal);
	}

	private BigDecimal aplicarDescontoPorValor(BigDecimal subtotal)
	{
		BigDecimal percentualDesconto = BigDecimal.ZERO;

		if (subtotal.compareTo(new BigDecimal("1000.00")) > 0)
		{
			percentualDesconto = new BigDecimal("0.20");
		}
		else if (subtotal.compareTo(new BigDecimal("500.00")) > 0)
		{
			percentualDesconto = new BigDecimal("0.10");
		}

		BigDecimal desconto = subtotal.multiply(percentualDesconto);
		return subtotal.subtract(desconto);
	}

	private BigDecimal calcularFrete(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente)
	{
		BigDecimal pesoTotal = calcularPesoTotal(carrinho);
		BigDecimal freteBase = calcularFreteBase(pesoTotal);
		BigDecimal taxaFrageis = calcularTaxaFrageis(carrinho);
		BigDecimal taxaMinima = freteBase.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("12.00") : BigDecimal.ZERO;
		BigDecimal multiplicadorRegiao = obterMultiplicadorRegiao(regiao);

		BigDecimal freteSemDesconto = freteBase.add(taxaMinima).add(taxaFrageis).multiply(multiplicadorRegiao);
		BigDecimal freteComDesconto = aplicarDescontoFrete(freteSemDesconto, tipoCliente);

		return freteComDesconto;
	}

	private BigDecimal calcularPesoTotal(CarrinhoDeCompras carrinho)
	{
		BigDecimal pesoTotal = BigDecimal.ZERO;
		for (var item : carrinho.getItens())
		{
			BigDecimal pesoTributavel = calcularPesoTributavel(item.getProduto());
			pesoTotal = pesoTotal.add(pesoTributavel.multiply(new BigDecimal(item.getQuantidade())));
		}
		return pesoTotal;
	}

	private BigDecimal calcularPesoTributavel(ecommerce.entity.Produto produto)
	{
		if (produto.getPesoFisico() == null || produto.getPesoFisico().compareTo(BigDecimal.ZERO) < 0)
		{
			throw new IllegalArgumentException("Peso físico não pode ser nulo ou negativo");
		}

		BigDecimal pesoFisico = produto.getPesoFisico();
		BigDecimal pesoCubico = calcularPesoCubico(produto);

		return pesoFisico.max(pesoCubico);
	}

	private BigDecimal calcularPesoCubico(ecommerce.entity.Produto produto)
	{
		if (produto.getComprimento() == null || produto.getLargura() == null || produto.getAltura() == null)
		{
			return BigDecimal.ZERO;
		}
		if (produto.getComprimento().compareTo(BigDecimal.ZERO) < 0 || 
		    produto.getLargura().compareTo(BigDecimal.ZERO) < 0 || 
		    produto.getAltura().compareTo(BigDecimal.ZERO) < 0)
		{
			throw new IllegalArgumentException("Dimensões não podem ser negativas");
		}

		BigDecimal volume = produto.getComprimento()
				.multiply(produto.getLargura())
				.multiply(produto.getAltura());
		BigDecimal pesoCubico = volume.divide(new BigDecimal("6000"), 2, java.math.RoundingMode.HALF_UP);
		return pesoCubico;
	}

	private BigDecimal calcularFreteBase(BigDecimal pesoTotal)
	{
		BigDecimal valorPorKg = BigDecimal.ZERO;

		if (pesoTotal.compareTo(new BigDecimal("50.00")) > 0)
		{
			valorPorKg = new BigDecimal("7.00");
		}
		else if (pesoTotal.compareTo(new BigDecimal("10.00")) > 0)
		{
			valorPorKg = new BigDecimal("4.00");
		}
		else if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0)
		{
			valorPorKg = new BigDecimal("2.00");
		}

		return valorPorKg.multiply(pesoTotal);
	}

	private BigDecimal calcularTaxaFrageis(CarrinhoDeCompras carrinho)
	{
		BigDecimal taxaTotal = BigDecimal.ZERO;
		for (var item : carrinho.getItens())
		{
			if (item.getProduto().isFragil() != null && item.getProduto().isFragil())
			{
				BigDecimal taxa = new BigDecimal("5.00").multiply(new BigDecimal(item.getQuantidade()));
				taxaTotal = taxaTotal.add(taxa);
			}
		}
		return taxaTotal;
	}

	private BigDecimal obterMultiplicadorRegiao(Regiao regiao)
	{
		switch (regiao)
		{
			case SUDESTE:
				return new BigDecimal("1.00");
			case SUL:
				return new BigDecimal("1.05");
			case NORDESTE:
				return new BigDecimal("1.10");
			case CENTRO_OESTE:
				return new BigDecimal("1.20");
			case NORTE:
				return new BigDecimal("1.30");
			default:
				throw new IllegalArgumentException("Região inválida");
		}
	}

	private BigDecimal aplicarDescontoFrete(BigDecimal frete, TipoCliente tipoCliente)
	{
		switch (tipoCliente)
		{
			case OURO:
				return BigDecimal.ZERO;
			case PRATA:
				return frete.multiply(new BigDecimal("0.50"));
			case BRONZE:
				return frete;
			default:
				throw new IllegalArgumentException("Tipo de cliente inválido");
		}
	}
}
