# Junit - E-commerce - Automation

## Descrição do Projeto

Este projeto implementa e testa a funcionalidade de **cálculo do custo total** de uma compra em um sistema de e-commerce. O método `calcularCustoTotal` aplica diversas regras de negócio envolvendo descontos, cálculo de frete, e benefícios por nível de cliente.

## Estrutura do Projeto

```
src/
├── main/java/ecommerce/
│   ├── entity/              # Entidades do domínio
│   ├── service/             # Camada de serviço (lógica de negócio)
│   ├── repository/          # Camada de persistência
│   └── controller/          # Camada de controle (REST API)
└── test/java/ecommerce/service/
    ├── CalculoCustoTotalParticoesTest.java      # Testes de Partições de Domínio
    ├── CalculoCustoTotalLimitesTest.java        # Testes de Valores Limites
    ├── CalculoCustoTotalTabelaDecisaoTest.java  # Testes de Tabela de Decisão
    ├── CalculoCustoTotalRobustezTest.java       # Testes de Robustez/Validação
    └── CalculoCustoTotalCoberturaTest.java      # Testes de Cobertura de Arestas
```

## Como Executar o Projeto

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6 ou superior

### Compilar o Projeto
```bash
mvn clean compile
```

### Executar os Testes
```bash
mvn test
```

### Verificar a Cobertura de Testes
```bash
mvn verify
```

Após a execução, o relatório de cobertura estará disponível em:
```
target/site/jacoco/index.html
```

Abra este arquivo em um navegador para visualizar a cobertura detalhada.

## Regras de Negócio Implementadas

### 1. Cálculo do Subtotal
- Soma de `preço_unitário × quantidade` de todos os itens do carrinho

### 2. Desconto por Múltiplos Itens de Mesmo Tipo
- 3 a 4 itens do mesmo tipo → 5% de desconto
- 5 a 7 itens do mesmo tipo → 10% de desconto
- 8 ou mais itens do mesmo tipo → 15% de desconto

### 3. Desconto por Valor de Carrinho
- Subtotal > R$ 1000,00 → 20% de desconto
- Subtotal > R$ 500,00 (≤ R$ 1000) → 10% de desconto
- Caso contrário → sem desconto

**Nota:** Os descontos são cumulativos (desconto por tipo é aplicado antes do desconto por valor).

### 4. Cálculo do Frete

#### 4.1. Peso Tributável
```
peso_tributável = max(peso_físico, peso_cúbico)
peso_cúbico = (C × L × A) / 6000
```

#### 4.2. Faixas de Peso
| Faixa | Condição | Valor por kg |
|-------|----------|--------------|
| A | 0,00 ≤ peso ≤ 5,00 | Isento (R$ 0,00) |
| B | 5,00 < peso ≤ 10,00 | R$ 2,00/kg |
| C | 10,00 < peso ≤ 50,00 | R$ 4,00/kg |
| D | peso > 50,00 | R$ 7,00/kg |

- Se não isento, adiciona-se **taxa mínima de R$ 12,00**
- Produtos frágeis: **R$ 5,00 × quantidade** (taxa de manuseio especial)

#### 4.3. Multiplicador Regional
| Região | Multiplicador |
|--------|---------------|
| Sudeste | 1,00 |
| Sul | 1,05 |
| Nordeste | 1,10 |
| Centro-Oeste | 1,20 |
| Norte | 1,30 |

### 5. Benefício de Nível do Cliente (aplicado ao frete)
- **OURO:** 100% de desconto no frete
- **PRATA:** 50% de desconto no frete
- **BRONZE:** paga o frete integral

### 6. Total da Compra
```
Total = (Subtotal com desconto) + (Frete após desconto de nível)
```

O resultado é arredondado para duas casas decimais (HALF_UP).

---

## Documentação dos Casos de Teste

### Testes de Partições de Domínio (15 testes)

| ID | Critério | Entrada | Resultado Esperado |
|----|----------|---------|-------------------|
| P1 | Peso: 0-5kg (isenta) | Peso=4kg | Frete=R$0 |
| P2 | Peso: 5.01-10kg | Peso=6kg | Frete=R$2/kg+R$12 |
| P3 | Peso: 10.01-50kg | Peso=12kg | Frete=R$4/kg+R$12 |
| P4 | Peso: >50kg | Peso=60kg | Frete=R$7/kg+R$12 |
| P5 | Subtotal: ≤R$500 | Subtotal=R$400 | Sem desconto valor |
| P6 | Subtotal: R$500-R$1000 | Subtotal=R$750 | 10% desconto |
| P7 | Subtotal: >R$1000 | Subtotal=R$1200 | 20% desconto |
| P8 | Itens mesmo tipo: 3-4 | 3 itens LIVRO | 5% desconto |
| P9 | Itens mesmo tipo: 5-7 | 5 itens LIVRO | 10% desconto |
| P10 | Itens mesmo tipo: ≥8 | 8 itens LIVRO | 15% desconto |
| P11 | Cliente: PRATA | TipoCliente.PRATA | 50% desconto frete |
| P12 | Cliente: OURO | TipoCliente.OURO | Frete grátis |
| P13 | Região: NORDESTE | Regiao.NORDESTE | Multiplicador 1.10 |
| P14 | Região: NORTE | Regiao.NORTE | Multiplicador 1.30 |
| P15 | Produto: Frágil | fragil=true, qtd=2 | Taxa=R$10 |

### Testes de Valores Limites (13 testes)

| ID | Critério | Valor Limite | Resultado Esperado |
|----|----------|--------------|--------------------|
| L1 | Peso máximo faixa A | 5kg | Isento |
| L2 | Peso mínimo faixa B | 5.01kg | R$2/kg+R$12 |
| L3 | Peso máximo faixa B | 10kg | R$2/kg+R$12 |
| L4 | Peso mínimo faixa C | 10.01kg | R$4/kg+R$12 |
| L5 | Peso máximo faixa C | 50kg | R$4/kg+R$12 |
| L6 | Peso mínimo faixa D | 50.01kg | R$7/kg+R$12 |
| L7 | Subtotal limite sem desconto | R$500.00 | Sem desconto |
| L8 | Subtotal início 10% | R$500.01 | 10% desconto |
| L9 | Subtotal limite 10% | R$1000.00 | 10% desconto |
| L10 | Subtotal início 20% | R$1000.01 | 20% desconto |
| L11 | Itens mesmo tipo início 5% | 3 itens | 5% desconto |
| L12 | Itens mesmo tipo início 10% | 5 itens | 10% desconto |
| L13 | Itens mesmo tipo início 15% | 8 itens | 15% desconto |

### Testes de Tabela de Decisão (10 testes)

| ID | Subtotal | Peso | Itens/Tipo | Frágil | Cliente | Região | Resultado |
|----|----------|------|------------|--------|---------|--------|-----------|
| TD1 | ≤500 | ≤5kg | <3 | Não | Bronze | Sudeste | Sem descontos, frete isento |
| TD2 | >500 | >5kg | <3 | Não | Bronze | Sudeste | 10% desc valor, frete R$2/kg |
| TD3 | >1000 | >10kg | <3 | Não | Bronze | Sudeste | 20% desc valor, frete R$4/kg |
| TD4 | ≤500 | ≤5kg | 3-4 | Não | Bronze | Sudeste | 5% desc tipo |
| TD5 | ≤500 | >5kg | <3 | Sim | Bronze | Sudeste | Taxa frágil R$5/un |
| TD6 | >500 | >5kg | 3-4 | Não | Prata | Sudeste | Descontos acumulados + 50% frete |
| TD7 | >1000 | >10kg | 5-7 | Sim | Ouro | Sul | Todos descontos + frete grátis |
| TD8 | >500 | >50kg | <3 | Não | Bronze | Norte | 10% desc + frete máximo × 1.30 |
| TD9 | >1000 | ≤5kg | ≥8 | Não | Ouro | Sudeste | Todos descontos máximos |
| TD10 | ≤500 | >50kg | <3 | Sim | Bronze | Norte | Frete máximo com todos acréscimos |

### Testes de Robustez e Validação (10 testes)

| ID | Entrada Inválida | Exceção Esperada | Mensagem |
|----|------------------|------------------|----------|
| R1 | Carrinho nulo | IllegalArgumentException | "Carrinho não pode ser nulo" |
| R2 | Itens vazios | IllegalArgumentException | "Carrinho não pode ser nulo ou vazio" |
| R3 | Região nula | IllegalArgumentException | "Região não pode ser nula" |
| R4 | TipoCliente nulo | IllegalArgumentException | "Tipo de cliente não pode ser nulo" |
| R5 | Quantidade = 0 | IllegalArgumentException | "Quantidade deve ser maior que zero" |
| R6 | Quantidade < 0 | IllegalArgumentException | "Quantidade deve ser maior que zero" |
| R7 | Produto nulo | IllegalArgumentException | "Produto não pode ser nulo" |
| R8 | Preço < 0 | IllegalArgumentException | "Preço não pode ser nulo ou negativo" |
| R9 | Peso físico < 0 | IllegalArgumentException | "Peso físico não pode ser nulo ou negativo" |
| R10 | Dimensões < 0 | IllegalArgumentException | "Dimensões não podem ser negativas" |

### Testes de Cobertura de Arestas (37 testes)

Os testes de cobertura garantem 100% de cobertura de arestas (branch coverage) do método `calcularCustoTotal` e seus métodos auxiliares, cobrindo:
- Todas as validações de entrada
- Todas as faixas de peso (A, B, C, D)
- Todas as faixas de desconto por tipo (0%, 5%, 10%, 15%)
- Todas as faixas de desconto por valor (0%, 10%, 20%)
- Todos os tipos de cliente (BRONZE, PRATA, OURO)
- Todas as regiões (SUDESTE, SUL, NORDESTE, CENTRO_OESTE, NORTE)
- Condições de produtos frágeis e não frágeis
- Cálculo de peso tributável (físico vs cúbico)

---

## Análise de Cobertura de Código

### Grafo de Fluxo de Controle (CFG) - Método `calcularCustoTotal`

```
Nó 1: Início
  ↓
Nó 2: if (carrinho == null || itens == null || itens.isEmpty())
  ↓ (true)                        ↓ (false)
Nó 3: throw Exception           Nó 4: if (regiao == null)
                                  ↓ (true)          ↓ (false)
                                Nó 5: throw       Nó 6: if (tipoCliente == null)
                                                    ↓ (true)        ↓ (false)
                                                  Nó 7: throw     Nó 8: calcularSubtotal()
                                                                    ↓
                                                                  Nó 9: aplicarDescontos()
                                                                    ↓
                                                                  Nó 10: calcularFrete()
                                                                    ↓
                                                                  Nó 11: total.setScale()
                                                                    ↓
                                                                  Nó 12: return total
```

### Complexidade Ciclomática

A complexidade ciclomática V(G) pode ser calculada por:
- **V(G) = número de decisões + 1**

#### Método `calcularCustoTotal` (simplificado):
- **Decisões:** 3 validações principais (carrinho, região, tipoCliente)
- **V(G) = 3 + 1 = 4**

#### Método `aplicarDescontoPorTipo`:
- **Decisões:** 3 if/else-if (8, 5, 3 itens) + 1 if (percentual > 0)
- **V(G) = 4 + 1 = 5**

#### Método `aplicarDescontoPorValor`:
- **Decisões:** 2 if/else-if (>1000, >500)
- **V(G) = 2 + 1 = 3**

#### Método `calcularFreteBase`:
- **Decisões:** 3 if/else-if (>50, >10, >5)
- **V(G) = 3 + 1 = 4**

#### Método `aplicarDescontoFrete`:
- **Decisões:** 3 switch cases
- **V(G) = 3 + 1 = 4**

#### Método `obterMultiplicadorRegiao`:
- **Decisões:** 5 switch cases
- **V(G) = 5 + 1 = 6**

### Complexidade Total Estimada
**V(G)_total ≈ 26 caminhos independentes**

Nossos testes cobrem **85 cenários** distribuídos entre:
- 15 testes de partições
- 13 testes de limites
- 10 testes de tabela de decisão
- 10 testes de robustez
- 37 testes de cobertura de arestas

Isso garante cobertura extensiva e ultrapassa o número mínimo de caminhos independentes.

---

## Análise MC/DC (Modified Condition/Decision Coverage)

### Decisão Complexa Analisada: `aplicarDescontoPorTipo`

```java
if (quantidadeItens >= 8) {
    percentualDesconto = 0.15;
} else if (quantidadeItens >= 5) {
    percentualDesconto = 0.10;
} else if (quantidadeItens >= 3) {
    percentualDesconto = 0.05;
}
```

Esta decisão composta pode ser analisada como:

**Condição A:** `quantidadeItens >= 8`  
**Condição B:** `quantidadeItens >= 5 && quantidadeItens < 8`  
**Condição C:** `quantidadeItens >= 3 && quantidadeItens < 5`

### Tabela MC/DC

| Caso | quantidadeItens | A (≥8) | B (≥5 e <8) | C (≥3 e <5) | Desconto | Teste |
|------|----------------|--------|-------------|-------------|----------|-------|
| TC1 | 2 | F | F | F | 0% | C9, P8 (implícito) |
| TC2 | 3 | F | F | T | 5% | C12, P8, L11 |
| TC3 | 5 | F | T | F | 10% | C11, P9, L12 |
| TC4 | 8 | T | F | F | 15% | C10, P10, L13 |

**Análise de Independência:**
- Para testar A: TC3 (A=F) vs TC4 (A=T) - B e C são falsos quando A é verdadeiro
- Para testar B: TC2 (B=F) vs TC3 (B=T) - A é falso em ambos
- Para testar C: TC1 (C=F) vs TC2 (C=T) - A e B são falsos em ambos

Cada condição individual foi testada de forma independente, demonstrando que influencia o resultado da decisão.

---

## Análise de Cobertura Alcançada

### Métricas Esperadas
- **Cobertura de Linhas:** ≥ 95%
- **Cobertura de Branches (Arestas):** 100%
- **Cobertura de Métodos:** 100%
- **Cobertura MC/DC:** Decisões complexas cobertas

### Verificação
Execute `mvn verify` e abra `target/site/jacoco/index.html` para ver:
- Cobertura detalhada por classe
- Linhas cobertas vs. não cobertas
- Branches cobertos vs. não cobertos

---

## Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.1.1**
- **JUnit 5** (Jupiter)
- **AssertJ** (assertions fluentes)
- **Jacoco** (cobertura de código)
- **Maven** (gerenciamento de dependências)

---

## Autores

**[João Eduardo](https://github.com/joapedu) e [Bruno Augusto](https://github.com/goisbrunoaugusto)**

---

## Observações Finais

Este projeto demonstra a aplicação completa de técnicas de teste de software:

1. **Testes Funcionais (Caixa Preta):**
   - Partições de Domínio (15 testes)
   - Valores Limites (13 testes)
   - Tabela de Decisão (10 testes)

2. **Testes Estruturais (Caixa Branca):**
   - 100% Cobertura de Arestas (37 testes)
   - Análise MC/DC
   - Complexidade Ciclomática

3. **Testes de Robustez:**
   - Validação de entradas inválidas (10 testes)
   - Tratamento de exceções
   - Casos extremos

**Total: 85 casos de teste** garantindo cobertura completa e robustez da funcionalidade de cálculo de custo total.

Todos os testes utilizam nomenclatura descritiva, mensagens de falha informativas e seguem as boas práticas recomendadas para testes automatizados.
