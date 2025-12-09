package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

class FinalizarCompraCenario2Test {

    private CompraService compraService;
    private FakeCarrinhoService carrinhoServiceFake;
    private FakeClienteService clienteServiceFake;
    private IEstoqueExternal estoqueMock;
    private IPagamentoExternal pagamentoMock;

    @BeforeEach
    void setUp() {
        estoqueMock = mock(IEstoqueExternal.class);
        pagamentoMock = mock(IPagamentoExternal.class);
        
        carrinhoServiceFake = new FakeCarrinhoService();
        clienteServiceFake = new FakeClienteService();

        compraService = new CompraService(carrinhoServiceFake, clienteServiceFake, estoqueMock, pagamentoMock);
    }

    private CarrinhoDeCompras criarCarrinhoSimples(Long carrinhoId) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setId(carrinhoId);
        
        Produto produto = new Produto();
        produto.setId(10L);
        produto.setPreco(new BigDecimal("100.00"));
        produto.setPesoFisico(new BigDecimal("1.00"));
        produto.setFragil(false);

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(2L);

        carrinho.setItens(Arrays.asList(item));
        return carrinho;
    }

    private Cliente criarCliente(Long id, String nome) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome(nome);
        return c;
    }

    @Test
    void testeCompraSucesso() {
        Long clienteId = 1L;
        Long carrinhoId = 2L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples(carrinhoId);

        clienteServiceFake.setCliente(criarCliente(clienteId, "Teste"));
        carrinhoServiceFake.setCarrinho(carrinho);

        List<Long> idsEsperados = Arrays.asList(10L);
        List<Long> qtdsEsperadas = Arrays.asList(2L);
        
        when(estoqueMock.verificarDisponibilidade(eq(idsEsperados), eq(qtdsEsperadas)))
            .thenReturn(new DisponibilidadeDTO(true, idsEsperados));
        when(pagamentoMock.autorizarPagamento(anyLong(), anyDouble()))
            .thenReturn(new PagamentoDTO(true, 999L));
        when(estoqueMock.darBaixa(eq(idsEsperados), eq(qtdsEsperadas)))
            .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO resultado = compraService.finalizarCompra(carrinhoId, clienteId);

        assertTrue(resultado.sucesso());
        assertEquals("Compra finalizada com sucesso.", resultado.mensagem());
        
        verify(estoqueMock).verificarDisponibilidade(eq(idsEsperados), eq(qtdsEsperadas));
        verify(pagamentoMock).autorizarPagamento(eq(clienteId), anyDouble());
        verify(estoqueMock).darBaixa(eq(idsEsperados), eq(qtdsEsperadas));
    }

    @Test
    void testeEstoqueIndisponivel() {
        Long clienteId = 1L;
        Long carrinhoId = 2L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples(carrinhoId);

        clienteServiceFake.setCliente(criarCliente(clienteId, "Teste"));
        carrinhoServiceFake.setCarrinho(carrinho);

        when(estoqueMock.verificarDisponibilidade(eq(Arrays.asList(10L)), eq(Arrays.asList(2L))))
            .thenReturn(new DisponibilidadeDTO(false, Arrays.asList(10L)));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinhoId, clienteId);
        });

        assertEquals("Itens fora de estoque.", ex.getMessage());
        
        verify(estoqueMock).verificarDisponibilidade(eq(Arrays.asList(10L)), eq(Arrays.asList(2L)));
        verify(pagamentoMock, never()).autorizarPagamento(anyLong(), anyDouble());
    }
    
    @Test
    void testePagamentoRecusado() {
        Long clienteId = 1L;
        Long carrinhoId = 2L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples(carrinhoId);

        clienteServiceFake.setCliente(criarCliente(clienteId, "Teste"));
        carrinhoServiceFake.setCarrinho(carrinho);

        when(estoqueMock.verificarDisponibilidade(eq(Arrays.asList(10L)), eq(Arrays.asList(2L))))
            .thenReturn(new DisponibilidadeDTO(true, Arrays.asList(10L)));
        when(pagamentoMock.autorizarPagamento(anyLong(), anyDouble()))
            .thenReturn(new PagamentoDTO(false, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinhoId, clienteId);
        });

        assertEquals("Pagamento não autorizado.", ex.getMessage());
        
        verify(pagamentoMock).autorizarPagamento(anyLong(), anyDouble());
        verify(estoqueMock, never()).darBaixa(any(), any());
    }
    
    @Test
    void testeErroBaixaEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 2L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples(carrinhoId);

        clienteServiceFake.setCliente(criarCliente(clienteId, "Teste"));
        carrinhoServiceFake.setCarrinho(carrinho);

        when(estoqueMock.verificarDisponibilidade(eq(Arrays.asList(10L)), eq(Arrays.asList(2L))))
            .thenReturn(new DisponibilidadeDTO(true, Arrays.asList(10L)));
        Long transacaoId = 888L;
        when(pagamentoMock.autorizarPagamento(anyLong(), anyDouble()))
            .thenReturn(new PagamentoDTO(true, transacaoId));
        when(estoqueMock.darBaixa(eq(Arrays.asList(10L)), eq(Arrays.asList(2L))))
            .thenReturn(new EstoqueBaixaDTO(false));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinhoId, clienteId);
        });

        assertEquals("Erro ao dar baixa no estoque.", ex.getMessage());
        
        verify(estoqueMock).darBaixa(eq(Arrays.asList(10L)), eq(Arrays.asList(2L)));
        verify(pagamentoMock).cancelarPagamento(clienteId, transacaoId);
    }

    static class FakeCarrinhoService extends CarrinhoDeComprasService {
        private CarrinhoDeCompras carrinho;

        public FakeCarrinhoService() {
            super(null);
        }

        public void setCarrinho(CarrinhoDeCompras c) {
            this.carrinho = c;
        }

        @Override
        public CarrinhoDeCompras buscarPorCarrinhoIdEClienteId(Long carrinhoId, Cliente cliente) {
            if (carrinho != null && carrinho.getId().equals(carrinhoId)) {
                return carrinho;
            }
            throw new IllegalArgumentException("Carrinho não encontrado fake");
        }
    }

    static class FakeClienteService extends ClienteService {
        private Cliente cliente;

        public FakeClienteService() {
            super(null);
        }

        public void setCliente(Cliente c) {
            this.cliente = c;
        }

        @Override
        public Cliente buscarPorId(Long id) {
            if (cliente != null && cliente.getId().equals(id)) {
                return cliente;
            }
            throw new IllegalArgumentException("Cliente não encontrado fake");
        }
    }
}

