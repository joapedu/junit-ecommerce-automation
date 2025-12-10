# Testes de Mutação e Dublês

**Autores:** João Eduardo e Bruno Augusto

Este projeto implementa testes de unidade, testes com dublês (Mocks e Fakes) e testes de mutação para um sistema de e-commerce simplificado.

## Tecnologias Utilizadas
- Java 17
- Spring Boot
- JUnit 5
- Mockito
- PITEST (Mutation Testing)
- JaCoCo (Code Coverage)

## Instruções de Execução

Para compilar o projeto:
```bash
mvn clean compile
```

## Como Rodar os Testes

Para executar todos os testes automatizados:
```bash
mvn test
```

Os testes estão divididos em:
- `CalculoCustoTotalTest`: Testes unitários para o método de cálculo de custo (100% cobertura de branches).
- `FinalizarCompraCenario1Test`: Testes do fluxo de finalização de compra usando Fakes manuais para serviços externos e Mocks para serviços internos.
- `FinalizarCompraCenario2Test`: Testes do fluxo de finalização de compra usando Mocks (Mockito) para serviços externos e Fakes para serviços internos.

## Relatório de Cobertura (JaCoCo)

O relatório de cobertura de código é gerado automaticamente após a execução dos testes.
Para visualizar:
1. Execute `mvn test` ou `mvn verify`
2. Abra o arquivo `target/site/jacoco/index.html` no navegador.

## Relatório de Mutação (PITEST)

Para executar a análise de mutação e verificar a qualidade dos testes:

```bash
mvn pitest:mutationCoverage
```
ou para focar apenas na classe principal:
```bash
mvn pitest:mutationCoverage -DtargetClasses=ecommerce.service.CompraService
```

### Interpretando o Relatório
O relatório será gerado em `target/pit-reports/YYYYMMDDHHMM/index.html`.
Abra este arquivo no navegador para ver os detalhes dos mutantes gerados e se foram mortos (Killed) ou sobreviveram (Survived).

**Meta Atingida:** 100% de mutantes mortos na classe `CompraService`.

### Estratégias Utilizadas
- **Análise de Limites:** Testes cobrindo exatamente os limites de faixas de desconto e frete.
- **Verificação de Argumentos:** Nos testes com Mocks e Fakes, validamos se os objetos passados para os métodos (como IDs e quantidades) correspondem exatamente ao esperado, garantindo que mutações que alteram argumentos ou retornos de streams sejam detectadas.
- **Cobertura de Branches:** Testes desenhados para exercitar todos os caminhos `if/else` do código.
