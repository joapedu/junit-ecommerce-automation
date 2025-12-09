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
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService {

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;
		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		// Simplified call - no longer needs region or client type
		BigDecimal custoTotal = calcularCustoTotal(carrinho);

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho) {
		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			throw new IllegalArgumentException("Carrinho invalido");
		}

		// 1. Subtotal
		BigDecimal subtotal = BigDecimal.ZERO;
		for (var item : carrinho.getItens()) {
			BigDecimal preco = item.getProduto().getPreco();
			BigDecimal qtd = new BigDecimal(item.getQuantidade());
			subtotal = subtotal.add(preco.multiply(qtd));
		}

		// 2. Desconto
		BigDecimal desconto = BigDecimal.ZERO;
		if (subtotal.compareTo(new BigDecimal("1000.00")) >= 0) {
			desconto = subtotal.multiply(new BigDecimal("0.20"));
		} else if (subtotal.compareTo(new BigDecimal("500.00")) >= 0) {
			desconto = subtotal.multiply(new BigDecimal("0.10"));
		}
		
		BigDecimal subtotalComDesconto = subtotal.subtract(desconto);

		// 3. Frete
		BigDecimal pesoTotal = BigDecimal.ZERO;
		BigDecimal taxaFrageis = BigDecimal.ZERO;

		for (var item : carrinho.getItens()) {
			BigDecimal peso = item.getProduto().getPesoFisico();
			BigDecimal qtd = new BigDecimal(item.getQuantidade());
			pesoTotal = pesoTotal.add(peso.multiply(qtd));

			if (Boolean.TRUE.equals(item.getProduto().isFragil())) {
				taxaFrageis = taxaFrageis.add(new BigDecimal("5.00").multiply(qtd));
			}
		}

		BigDecimal valorFrete = BigDecimal.ZERO;
		if (pesoTotal.compareTo(new BigDecimal("50.00")) > 0) {
			valorFrete = pesoTotal.multiply(new BigDecimal("7.00"));
		} else if (pesoTotal.compareTo(new BigDecimal("10.00")) > 0) {
			valorFrete = pesoTotal.multiply(new BigDecimal("4.00"));
		} else if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0) {
			valorFrete = pesoTotal.multiply(new BigDecimal("2.00"));
		}
		// Se for <= 5kg, frete é 0 (já inicializado)

		valorFrete = valorFrete.add(taxaFrageis);

		// 4. Total final
		BigDecimal total = subtotalComDesconto.add(valorFrete);

		return total.setScale(2, java.math.RoundingMode.HALF_UP);
	}
}
