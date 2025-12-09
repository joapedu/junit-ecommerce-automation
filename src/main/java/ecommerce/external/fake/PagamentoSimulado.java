package ecommerce.external.fake;

import org.springframework.stereotype.Service;

import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IPagamentoExternal;

@Service
public class PagamentoSimulado implements IPagamentoExternal
{

	@Override
	public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal)
	{
		return new PagamentoDTO(true, System.currentTimeMillis());
	}

	@Override
	public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId)
	{
		// Simula cancelamento
		System.out.println("Pagamento cancelado: " + pagamentoTransacaoId);
	}
}
