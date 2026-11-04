# Changelog — Finflow (Finanças Pessoais)

Todas as alterações notáveis deste projeto são documentadas neste ficheiro de acordo com os princípios do [Semantic Versioning (SemVer)](https://semver.org/).

Formato de Versão: `MAJOR.MINOR.PATCH`
- **MAJOR**: Alterações de arquitetura, redesigns globais ou quebras de compatibilidade.
- **MINOR**: Novas funcionalidades, novos ecrãs, gráficos ou ferramentas de análise.
- **PATCH**: Correções de bugs, ajustes de sincronização, melhorias de desempenho e usabilidade.

---

## [2.2.2] — 2026-11-04 (Patch & Fix)
### Alterado
- **Assinatura fixa das releases**: o pipeline passa a assinar sempre com a mesma chave (segredos do GitHub) e recusa publicar sem ela, permitindo que cada release atualize a anterior.
### Nota de migração
- Quem tem a v2.2.0 ou v2.2.1 (assinadas com chaves diferentes) deve exportar um backup (Conta → Sistema), instalar a v2.2.2 e importar o backup. As atualizações seguintes funcionam diretamente.

---

## [2.2.1] — 2026-11-04 (Patch & Fix)
### Corrigido
- **Botão de tema** (diurno/noturno) passa a estar disponível em todos os separadores e na Conta.
- **Formatação PT-PT**: percentagens com vírgula decimal em todos os ecrãs e sem `-0,0%`; variação face ao mês anterior no Início calculada com dados reais.
- **Conta**: nome, foto, idioma e aparência refletem sempre o valor guardado; cabeçalho e barra inferior coerentes; iniciais do avatar sem caracteres inválidos.
- **Movimentos**: ícones por categoria-mãe e etiquetas de contexto traduzidas.
- **Início**: média diária correta em meses passados; lista animada usa o estado alvo (erro de lint).
- **Splash** com o fundo `#0B0E14`; sobreposição do texto "Despesa Fixa" com o interruptor.

---

## [2.2.0] — 2026-10-29 (Minor Release)
### Adicionado
- **Transições Direcionais Entre Ecrãs (Material 3 Expressive)**: Transições dinâmicas com deslize horizontal direcional (`slideInHorizontally` + `fadeIn` + `scaleIn` e `slideOutHorizontally` + `fadeOut` + `scaleOut`) entre `Início`, `Movimentos`, `Análise` e `Orçamentos`, além de transição modal suave para o ecrã de `Conta`.
- **Barra de Navegação Tátil & Reativa**:
  - Pílula de destaque animada (`FinflowPrimary` com fading suave de alfa).
  - Ícone com animação de salto elástico (*bouncy spring physics*) ao selecionar cada separador.
  - Botão central `Nova (+)` transformado em FAB flutuante em gradiente com feedback de toque e escala tátil.
- **Sub-Navegação Dinâmica em Análise**: Troca instantânea e fluida entre lentes (`Visão Anual`, `No Quê`, `Onde`, `Como`, `Evolução`, `Comparar`, `Calendário`) com animações direcionais suaves e pílulas de seleção interpoladas.
- **Odómetro Temporal no Seletor de Mês**: Transição de viragem vertical suave (*calendar flip*) no título do mês ao navegar entre períodos (`< >`).
- **Micro-interações em Filtros**: Pílulas de filtro no Início e Movimentos interpolam cores e escala de forma suave e instantânea.

---

## [2.1.1] — 2026-10-29 (Patch & Fix)
### Corrigido
- **Ancoragem de Meses em Movimentos**: O ecrã de `Movimentos` (`TransactionsScreen`) agora abre e ancora exatamente no mês que estava a ser visualizado no `Início`, em vez de ficar preso a Outubro de 2026 (`LocalDate.now()`).
- **Navegação de Meses em Movimentos**: Adicionado seletor de mês (`MonthSelector`) com setas `< >` e diálogo para saltar para qualquer mês entre 2024 e 2027 diretamente no separador Movimentos.
- **Filtro Temporal Dinâmico**: Ao alternar para o filtro `Mês`, o ecrã atualiza de imediato a contagem, total gasto, total recebido e percentagem de peso no rendimento para o mês em foco.

### Adicionado
- **Barra de Navegação Inferior de 5 Pontos**: O separador `Movimentos` passa a ter acesso direto na barra inferior entre `Início` e `(+) Nova`.
- **Expansão In-Place no Início**: Botão `Mostrar mais (N)` / `Mostrar menos` para inspecionar todas as transações do mês diretamente no ecrã inicial.
- **Filtros Rápidos Ativos**: Os botões `Todas`, `Hoje` e `Ontem` no Início filtram dinamicamente a lista de movimentos visíveis.
- **Rastreio de Versões & Patches na App**: Novo ecrã e diálogo no separador `Conta` com histórico completo de lançamentos e notas de patch.

---

## [2.1.0] — 2026-10-28 (Minor Release)
### Adicionado
- **Lente Anual 360°**: Nova vista anual no ecrã de `Análise` com seletor de anos (2024, 2025, 2026), gráfico interativo de barras de 12 meses e cartões de resumo anual.
- **Movimentos Discriminados por Subcategoria**: Na vista mensal e anual, cada categoria expande para mostrar detalhadamente cada compra individual com data, contexto, método de pagamento e montante.
- **Motor de Importação Resiliente**: Suporte a históricos com anotações e referências imprevisíveis (452+ transações preservadas sem omissão de dados).

---

## [2.0.0] — 2026-10-28 (Major Release)
### Modificado
- **Redesign Finflow (Stitch Modern Wealth)**: Migração para a identidade visual inspirada no Stitch (paleta de alto contraste, fundo claro `#FAF8FF` e destaque em índigo vibrante `#3525CD` e esmeralda `#006C49`).
- **Card de Despesas de Alto Impacto**: Exibição da percentagem consumida face ao teto com badge de estado ("Dentro da meta").
- **Métricas de Ritmo Financeiro**: Média diária de consumo, indicador de ritmo sustentável e visualização do ritmo semanal dividido em 4 semanas.

---

## [1.2.0] — 2026-10-27 (Minor Release)
### Adicionado
- **Gestão de Orçamentos por Categoria**: Definição de limites mensais por categoria com barras de progresso proporcionais.
- **Metas de Poupança**: Sistema de objetivos de poupança com cálculo de ritmo e prazo estimado.

---

## [1.1.0] — 2026-10-26 (Minor Release)
### Adicionado
- **Calculadora Numpad Integrada**: Teclado numérico no ecrã de registo de transações com suporte a operações aritméticas diretas (`+`, `-`, `*`, `/`).
- **Preenchimento Automático Inteligente**: Sugestão de contexto (`CASA`, `TRABALHO`, `SAIDA`) e método de pagamento com base no histórico do comerciante.

---

## [1.0.0] — 2026-10-25 (Major Release)
### Adicionado
- **Lançamento Inicial da App**: Arquitetura 100% offline-first construída com Jetpack Compose e SQLite Room.
- **Matemática Financeira Imutável**: Valores monetários rigorosamente modelados em cêntimos (`Long`) para precisão absoluta.
- **Semente de Categorias PT-PT**: Categorização pré-configurada em dois níveis com suporte a despesas fixas e variáveis.
