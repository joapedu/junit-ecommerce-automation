package ecommerce.external.fake;

import java.util.List;

import org.springframework.stereotype.Service;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.external.IEstoqueExternal;

@Service
public class EstoqueSimulado implements IEstoqueExternal
{

	@Override
	public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades)
	{
		return new EstoqueBaixaDTO(true);
	}

	@Override
	public DisponibilidadeDTO verificarDisponibilidade(List<Long> produtosIds, List<Long> produtosQuantidades)
	{
		return new DisponibilidadeDTO(true, produtosIds);
	}
}
