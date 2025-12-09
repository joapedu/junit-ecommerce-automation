package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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

class FinalizarCompraCenario1Test {

    private CompraService compraService;
    private CarrinhoDeComprasService carrinhoServiceMock;
    private ClienteService clienteServiceMock;
    private FakeEstoqueService estoqueFake;
    private FakePagamentoService pagamentoFake;

    @BeforeEach
    void setUp() {
        carrinhoServiceMock = mock(CarrinhoDeComprasService.class);
        clienteServiceMock = mock(ClienteService.class);
        estoqueFake = new FakeEstoqueService();
        pagamentoFake = new FakePagamentoService();

        compraService = new CompraService(carrinhoServiceMock, clienteServiceMock, estoqueFake, pagamentoFake);
    }

    private void configurarMocks(Long clienteId, Long carrinhoId, CarrinhoDeCompras carrinho) {
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        
        when(clienteServiceMock.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoServiceMock.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);
    }

    private CarrinhoDeCompras criarCarrinhoSimples() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setPreco(new BigDecimal("100.00"));
        produto.setPesoFisico(new BigDecimal("1.00"));
        produto.setFragil(false);

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(1L);

        carrinho.setItens(Arrays.asList(item));
        return carrinho;
    }

    @Test
    void testeCompraSucesso() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples();

        configurarMocks(clienteId, carrinhoId, carrinho);
        estoqueFake.setDisponivel(true);
        estoqueFake.setSucessoBaixa(true);
        pagamentoFake.setAutorizado(true);

        CompraDTO resultado = compraService.finalizarCompra(carrinhoId, clienteId);

        assertTrue(resultado.sucesso());
        assertEquals("Compra finalizada com sucesso.", resultado.mensagem());
        assertTrue(estoqueFake.isVerificarChamado());
        assertEquals(Arrays.asList(1L), estoqueFake.getIdsVerificados());
        assertTrue(pagamentoFake.isAutorizarChamado());
        assertTrue(estoqueFake.isBaixaChamada());
    }

    @Test
    void testeEstoqueIndisponivel() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples();

        configurarMocks(clienteId, carrinhoId, carrinho);
        estoqueFake.setDisponivel(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinhoId, clienteId);
        });

        assertEquals("Itens fora de estoque.", ex.getMessage());
        assertTrue(estoqueFake.isVerificarChamado());
        // Não deve chamar pagamento nem baixa
        assertEquals(false, pagamentoFake.isAutorizarChamado());
        assertEquals(false, estoqueFake.isBaixaChamada());
    }

    @Test
    void testePagamentoNaoAutorizado() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples();

        configurarMocks(clienteId, carrinhoId, carrinho);
        estoqueFake.setDisponivel(true);
        pagamentoFake.setAutorizado(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinhoId, clienteId);
        });

        assertEquals("Pagamento não autorizado.", ex.getMessage());
        assertTrue(estoqueFake.isVerificarChamado());
        assertTrue(pagamentoFake.isAutorizarChamado());
        // Não deve dar baixa
        assertEquals(false, estoqueFake.isBaixaChamada());
    }

    @Test
    void testeErroBaixaEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 1L;
        CarrinhoDeCompras carrinho = criarCarrinhoSimples();

        configurarMocks(clienteId, carrinhoId, carrinho);
        estoqueFake.setDisponivel(true);
        pagamentoFake.setAutorizado(true);
        estoqueFake.setSucessoBaixa(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(carrinhoId, clienteId);
        });

        assertEquals("Erro ao dar baixa no estoque.", ex.getMessage());
        assertTrue(estoqueFake.isVerificarChamado());
        assertTrue(pagamentoFake.isAutorizarChamado());
        assertTrue(estoqueFake.isBaixaChamada());
        assertTrue(pagamentoFake.isCancelarChamado()); // Deve compensar
    }

    // Fakes Implementation
    static class FakeEstoqueService implements IEstoqueExternal {
        private boolean disponivel = true;
        private boolean sucessoBaixa = true;
        private boolean verificarChamado = false;
        private boolean baixaChamada = false;
        private List<Long> idsVerificados;
        private List<Long> qtdsVerificadas;

        public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
        public void setSucessoBaixa(boolean sucessoBaixa) { this.sucessoBaixa = sucessoBaixa; }
        public boolean isVerificarChamado() { return verificarChamado; }
        public boolean isBaixaChamada() { return baixaChamada; }
        public List<Long> getIdsVerificados() { return idsVerificados; }
        public List<Long> getQtdsVerificadas() { return qtdsVerificadas; }

        @Override
        public DisponibilidadeDTO verificarDisponibilidade(List<Long> ids, List<Long> qtds) {
            this.verificarChamado = true;
            this.idsVerificados = ids;
            this.qtdsVerificadas = qtds;
            return new DisponibilidadeDTO(disponivel, ids);
        }

        @Override
        public EstoqueBaixaDTO darBaixa(List<Long> ids, List<Long> qtds) {
            this.baixaChamada = true;
            return new EstoqueBaixaDTO(sucessoBaixa);
        }
    }

    static class FakePagamentoService implements IPagamentoExternal {
        private boolean autorizado = true;
        private boolean autorizarChamado = false;
        private boolean cancelarChamado = false;

        public void setAutorizado(boolean autorizado) { this.autorizado = autorizado; }
        public boolean isAutorizarChamado() { return autorizarChamado; }
        public boolean isCancelarChamado() { return cancelarChamado; }

        @Override
        public PagamentoDTO autorizarPagamento(Long clienteId, Double total) {
            this.autorizarChamado = true;
            return new PagamentoDTO(autorizado, 123L);
        }

        @Override
        public void cancelarPagamento(Long clienteId, Long transacaoId) {
            this.cancelarChamado = true;
        }
    }
}

