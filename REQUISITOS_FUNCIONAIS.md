# Requisitos Funcionais — App Finanças Pessoais

> **Para quem é este documento:** qualquer pessoa (utilizadores, testadores, novos programadores) que queira perceber **o que a aplicação faz e porquê**, sem precisar de ler código.
>
> **Estado:** reflete o código tal como está em 2026-11-01 (a app não embute nem importa qualquer histórico; a base de dados começa vazia).
> Documentação técnica complementar: [`PLANO_ARQUITETURA.md`](./PLANO_ARQUITETURA.md).

---

## 1. O que é a aplicação

É uma aplicação **Android de finanças pessoais, 100% offline**, escrita em português de Portugal, com tema escuro exclusivo. Permite registar tudo o que se **recebe** e tudo o que se **gasta**, e depois analisar esses dados em gráficos — sempre **em relação ao rendimento do mês** ("isto custou-me X% do que recebo"), e não apenas em valores absolutos.

Princípios base:

- **Não há contas, nem servidores, nem internet:** todos os dados ficam numa base de dados local (SQLite/Room) no próprio telemóvel. Nada sai do aparelho.
- **Todos os valores monetários são guardados em cêntimos** (números inteiros), para nunca haver erros de arredondamento. Exemplo interno: `1250` = 12,50 €.
- **Tudo é formatado à portuguesa:** `1.234,56 €`, percentagens com 1 casa decimal (`24,5%`), meses como `Outubro 2026`.

---

## 2. A ideia central: cada movimento tem 4 dimensões

A app evita a confusão das apps tradicionais (que misturam categoria, local e método de pagamento numa lista só). Cada movimento responde a **4 perguntas em separado**:

| Dimensão | Pergunta | Exemplos |
|---|---|---|
| **Quanto & Quando** | Qual o valor e a data? | 12,50 € · 26 de outubro de 2026 |
| **No Quê** | Em que categoria se enquadra? | Alimentação > Supermercado |
| **Onde** | Em que contexto e em que estabelecimento? | Comércio / Rua · Continente |
| **Como** | Que meio de pagamento e é fixo ou variável? | MB Way · Variável |

### Conjuntos predefinidos (todos editáveis pelo utilizador)

- **Categorias (47 predefinidas):**
  - *Rendimentos:* Rendimento Fixo (Salário Base, Subsídio de Alimentação) e Rendimento Extra (Prémios/Bónus, Subsídio Férias/Natal, Reembolsos, Outros).
  - *Gastos — 8 grupos com subcategorias:* Habitação & Contas, Alimentação, Transportes, Saúde & Bem-Estar, Lazer & Social, Compras & Educação, Férias & Viagens, Financeiro & Outros.
- **Contextos "Onde" (6):** Trabalho · Casa · Saída / Social · Férias / Viagem · Online · Comércio / Rua.
- **Meios de pagamento "Como" (5):** MB Way · Cartão · Débito Direto · Transferência · Dinheiro.
- **Flag Fixo/Variável:** o interruptor "🔁 Fixo" marca despesas e rendimentos recorrentes (renda, ginásio, salário base…). É esta marca que alimenta o gerador automático de mês (ver §4.2) e a análise de rigidez (ver §4.5).

---

## 3. Primeira abertura

Ao abrir a app pela primeira vez acontece uma coisa automática:

1. **Criação das categorias predefinidas** (as 47 acima), se a base de dados estiver vazia.

Resultado: a app começa com a base de dados **vazia de movimentos** — o utilizador adiciona os seus movimentos (manualmente, via registo rápido, captura de notificações ou restauro de backup JSON em Conta). A app não embute nem importa qualquer histórico de dados.

---

## 4. Ecrãs e funcionalidades

A navegação é uma barra inferior com 4 separadores e um botão central `( + )`:

```text
[ 🏠 Início ]  [ 🧾 Movimentos ]  ( ➕ )  [ 📊 Análise ]  [ ⚙️ Conta ]
```

### 4.1 🏠 Início — cockpit do mês

O painel principal. Para o mês selecionado (com seletor `< Mês Ano ▾ >` no topo, que inclui setas e toque no título para abrir a grelha de salto rápido entre anos e meses) mostra:

- **Banner "Virar do Mês"** (aparece só quando o mês selecionado não tem movimentos fixos e o mês anterior tem): *"Gerar Salário e {N} fixos do mês anterior?"* com botão **Gerar**. Um toque copia o salário base e todas as despesas fixas do mês anterior para o novo mês, mantendo o dia do mês original (ajustado se o mês for mais curto). É seguro repetir: movimentos fixos já existentes não são duplicados.
- **Cartão principal (herói):**
  - **Recebido Total** — o "100%" de toda a app — decomposto em Base (fixos) e Extras;
  - **Total Gasto** em destaque, com a percentagem *"X% do que recebes"*;
  - **Poupança Líquida** (recebido − gasto) com a **taxa de poupança** (o rótulo fica verde quando a taxa atinge ≥ 20%);
  - **Barra de Esforço tripartida:** Fixos | Variáveis | Poupado, cada segmento com % do rendimento e valor. Num mês em que se gastou mais do que se recebeu, o segmento "Poupado" aparece a zero.
- **"Onde vai o dinheiro"** — top 3 categorias de gasto com barra proporcional e % do rendimento.
- **"Distribuição por Contexto"** — top 4 contextos com valores.
- **"Últimos Movimentos"** — os 5 mais recentes, com link **"Ver todos"** para a aba Movimentos.
- **Estado vazio:** *"Sem movimentos neste mês. Toca no (+) para registar em menos de 3s."*

Cada linha de movimento pode ser **apagada com um toque no ícone do caixote** (eliminação imediata, sem confirmação — ver §9).

### 4.2 🧾 Movimentos — histórico com pesquisa e filtros

- **Pesquisa em tempo real** por estabelecimento/nota, nome de categoria, contexto ou método de pagamento (ex.: escrever "Continente" mostra só esses movimentos).
- **Cartão de soma dinâmica:** *"N movimentos · Total: Y €"* + **"Peso no Recebido"** (o total filtrado em % do rendimento do período) — mostra sempre o impacto do filtro ativo.
- **Filtros em cápsulas:**
  - Período: **Mês Atual** (predefinido) · Ano · Todos;
  - Fluxo: Todos · Fixos · Variáveis · Receitas;
  - Valor: Todos · ≤ 10 € · 10–50 € · ≥ 50 €;
  - Contexto (Trabalho, Casa, Saída, Férias, Online, Comércio) e Método (MB Way, Cartão, Débito Direto, Transferência, Dinheiro) — um segundo toque desativa o filtro.
- **Lista agrupada por data** com cabeçalhos especiais para Hoje e Ontem, e edição/remoção ao toque.
- **Estado vazio:** *"Nenhum movimento encontrado para estes filtros."*

### 4.3 ( ➕ ) Registo ultra-rápido (abaixo de 3 segundos)

Uma folha modal única para registar gasto ou receita:

- **Alternador "💸 Gasto | 💰 Receita"** no topo.
- **Teclado de calculadora próprio** (escuro, sem invocar o teclado do sistema): dígitos, vírgula decimal, tecla `00`, apagar, `C` (limpar), `+` para **somar valores inline** (ex.: `12,50 + 4,00` → 16,50 €) e tecla verde `✓` para gravar. Limites: máximo 2 casas decimais; `00` comporta-se de forma inteligente conforme já existirem decimais.
- **Sugestões rápidas com auto-preenchimento:** chips com os últimos 10 estabelecimentos usados. Ao tocar num (ex.: "Continente"), a app preenche automaticamente **categoria, contexto, método de pagamento e Fixo/Variável** com base no último movimento desse estabelecimento — a "memória associativa" que torna o registo quase instantâneo.
- **Seletores de 1 toque:** Categoria Principal → Subcategoria; Contexto "Onde" (predefinição: Comércio / Rua); Método "Como" (predefinição: MB Way); interruptor **"Movimento Fixo / Recorrente"**.
- **Regras de gravação:**
  - Valor 0 **não grava** (a folha continua aberta, silenciosamente);
  - A **data é escolhível com atalhos e dropdowns diretos:**
    - Chips rápidos de 1 toque: **Hoje**, **Ontem**, **Anteontem** na folha de registo;
    - Toque na linha **Data** abre o diálogo de seleção direta por **dropdowns** (Dia 1..31 com ajuste automático, Mês Janeiro..Dezembro, Ano 2024..2027), eliminando a necessidade de navegar mês a mês com setas;
    - Opção no diálogo para alternar para o calendário gráfico tradicional se o utilizador desejar;
    - A data escolhida determina o dia (`epochDay`) e o mês (`yearMonth`) a que o movimento pertence;
  - Sem nota nem categoria escolhida, o movimento fica na subcategoria de recurso *"Outros / Imprevistos"* e o título mostra o nome da categoria (ou "Sem nota").
- **Editar um movimento** — o ícone de lápis em cada linha (Início e Movimentos) abre a mesma folha em modo de edição ("A editar movimento"): todos os campos vêm preenchidos e o toque em ✓ **atualiza** o registo existente (não cria um duplicado). Ao abrir o `( + )` normal, a folha começa sempre limpa.
- **Apagar com confirmação** — o caixote abre um diálogo "Apagar movimento?" / "Apagar subcategoria?" com o aviso *"Esta ação é definitiva e não pode ser anulada."* — nada se apaga sem confirmar.

### 4.4 📊 Análise — 6 vistas interativas

Cabeçalho comum: seletor `< Mês Ano >`, o **interruptor global `[ € | % do que recebo ]`**, que converte instantaneamente todos os valores do ecrã entre euros e percentagens do rendimento do mês (com rendimento 0, as percentagens mostram `0,0% do recebido`), e o botão **partilhar relatório** (ícone de partilha), que gera um resumo textual do mês selecionado e o envia para qualquer app (ou para a área de transferência).

| Vista | O que mostra | Interação |
|---|---|---|
| **No Quê** | Anel (donut) com os grupos de categorias + lista ordenada por peso | Tocar numa fatia destaca-a e mostra as subcategorias; tocar de novo ou no centro limpa. Os cartões expandem para listar subcategorias com % e nº de movimentos; gastos lançados diretamente no grupo aparecem como "{Grupo} (Geral)" |
| **Onde** | Barras horizontais por contexto + **ranking Top 10 de estabelecimentos** | No ranking: nº de idas e **valor médio por ida** — onde se deixa realmente mais dinheiro |
| **Como** | **Rigidez:** Fixos vs. Variáveis vs. Poupança (barra + 3 cartões com % do rendimento) + breakdown por meio de pagamento | Leitura; mostra quanta margem de manobra resta |
| **Evolução** | Gráfico de barras Recebido vs. Gasto para os 12 meses do ano + **linha tracejada da média mensal de gastos**; por baixo: **"Evolução de {Categoria}"** (chips das top 8 categorias do ano + gráfico de linha com média da categoria e desvio do mês), **"Taxa de Poupança"** (gráfico de linha com média e linha de referência "Meta 20%"), **"Comparação Anual"** (barras agrupadas mês a mês por ano com legenda de totais) e **"Totais por Ano"** (Recebido/Gasto/Poupança/Taxa de cada ano) | Tocar/arrastar sobre os meses abre o cartão "Detalhes de {Mês}" e move o cursor nos gráficos de linha; tocar num chip troca a categoria em análise |
| **Comparar** | Deltas do mês selecionado face a uma **base** à escolha — `Mês Anterior`, `Média Mensal` (médias não diluídas do ano) ou `Ano Anterior` — para Gasto, Recebido e Poupança (valor + %, ícone de tendência colorido), seguido das listas **"Maiores Subidas"** e **"Maiores Descidas"** (top 5 categorias com roll-up para o pai) | Tocar numa cápsula de base recalcula a comparação; se a base não tiver movimentos, mostra "Sem dados para comparar com esta base." |
| **Calendário** | Grelha do mês (Seg–Dom) com cada dia pintado por **intensidade do gasto** (vermelho proporcional), dia atual assinalado e toque num dia | Tocar num dia abre o cartão "Movimentos de {dia}" com todos os movimentos desse dia |

Regras importantes: a **média mensal de gastos só conta meses com atividade** (meses a zero não diluem a média); as comparações com base "Mês Anterior" e "Ano Anterior" lidam com a viragem do ano; uma subida de **gasto** aparece em coral e uma descida em verde (recebido/poupança ao contrário).

### 4.5 ⚙️ Conta — organização, dados e segurança

- **Categorias & Contextos:** criar subcategorias (herdam a cor e o tipo do grupo pai; marcáveis como "Fixo"), eliminá-las (com confirmação), e criar novos contextos "Onde" (normalizados para MAIÚSCULAS_COM_UNDERSCORE) — **guardados no dispositivo**, pelo que sobrevivem ao fecho da app e aparecem também na folha de registo.
- **Orçamentos & Metas:**
  - **Orçamentos** — um limite mensal em € por categoria (ex.: Alimentação 400 €/mês). A app mostra o gasto, o restante e o estado: **OK** (<80%), **Atenção** (≥80%) e **Excedido** (>100%). Um orçamento numa categoria pai abrange as suas subcategorias.
  - **Metas de poupança** — nome, valor alvo e prazo opcional; a app mostra o acumulado, a % concluída e a previsão de meses para completar (com selo "No prazo" / "Atrasado"). Podem ser arquivadas.
- **Cópias de segurança & portabilidade:**
  - **Exportar JSON** — cópia completa e atómica (categorias + movimentos + **orçamentos + metas**, esquema v2), partilhada via folha de partilha do Android (se não houver destino, copia para a área de transferência);
  - **Exportar CSV** — compatível com Excel em PT-PT (separador `;`, colunas: Data; Mês; Tipo; Valor €; Categoria; Subcategoria; Onde; Estabelecimento; Como; Fixo) — apenas movimentos;
  - **Restaurar Backup (JSON)** — escolher ficheiro ou colar texto; importação **idempotente** (duplicados são ignorados e reportados: *"Backup restaurado: N novos movimentos, M categorias sincronizadas, B orçamentos e G metas sincronizados"*). Backups antigos (v1) também são aceites;
- **Segurança:** interruptor **"Bloqueio Biométrico"** (desligado por defeito — a app abre em 0 segundos). Ativo, apresenta o diálogo biométrico do sistema ao abrir; falhar ou cancelar mantém o ecrã de bloqueio com o botão "Desbloquear".
- **Sobre:** destaca "100% Offline-First" e a garantia de que os registos nunca saem do telemóvel.

### 4.6 🏠 Início — cartões de planeamento

Para além do cockpit mensal (§4.1), o Início mostra (cada cartão só aparece quando tem dados):

- **"Orçamentos do Mês"** — até 5 orçamentos ordenados por % usado, com barra verde/âmbar/vermelha, valor gasto e limite.
- **"A Este Ritmo"** — a média diária de gasto e a **projeção de fecho do mês** (média × dias do mês), com a taxa de poupança projetada.
- **"Metas"** — cada meta com barra de progresso, acumulado, prazo e previsão de meses para completar.

---

## 5. Regras de cálculo (a matemática da app)

Toda a matemática financeira vive em classes Kotlin puras e testadas, sempre em cêntimos:

| Cálculo | Fórmula | Proteção |
|---|---|---|
| % do rendimento | `parte ÷ recebido × 100` | Devolve 0% quando o rendimento é 0 (nunca divide por zero) |
| Taxa de poupança | `(recebido − gasto) ÷ recebido × 100` | 0% com rendimento 0 |
| Rendimento Base | movimentos de rendimento marcados como fixos (ou das categorias Salário Base / Subsídio de Alimentação) | Extras = Total − Base |
| Gastos Fixos / Variáveis | soma dos marcados como fixo / restantes | — |
| Média mensal de gastos | média dos meses com gastos > 0 | Meses sem atividade são excluídos |
| Desvio vs. média | `(gasto_atual − média) ÷ média × 100` | Só existe quando há média |
| % dos gastos | `parte ÷ total_gasto × 100` | 0% sem gastos |
| Delta vs. base (Comparar) | `(atual − base) ÷ base × 100` | Percentagem `null` quando a base é 0 (mostra só a diferença em cêntimos) |
| Média de categoria (tendência) | média dos meses do ano com gastos > 0 nessa categoria | Meses a zero não diluem a média da categoria |
| Média da taxa de poupança | média das taxas dos meses com atividade (recebido ou gasto > 0) | Meses sem atividade são excluídos |
| Gasto num orçamento | soma das despesas da categoria (subs incluídas quando o orçamento é do pai) | O total agregado conta cada movimento **uma só vez**, mesmo que esteja coberto por dois orçamentos |
| Estado do orçamento | OK < 80% · Atenção 80–100% · Excedido > 100% | Limite 0 → 0% e sem divisão por zero |
| Progresso de meta | poupança acumulada (recebido − gasto) desde o início da meta ÷ alvo | Alvo 0 → 0%; projeção nula se a média mensal for ≤ 0 |
| Projeção de fecho do mês | (gasto do mês ÷ dias já decorridos) × dias do mês | Mês vazio → zeros; sem NaN |
| Gasto por dia (calendário) | soma das despesas de cada dia | Receitas não contam como gasto |

**Exemplo prático:** recebido 1.500,00 €, gasto 400,00 € → poupança líquida 1.100,00 €, taxa de poupança 73,3%, gasto igual a 26,7% do rendimento. Se a média mensal fosse 500 €, o desvio seria −20,0% ("abaixo da média").

A classificação de categorias faz **roll-up automático**: uma transação registada numa subcategoria (ex.: Supermercado) conta para a sua categoria pai (Alimentação) nos totais e gráficos.

---

## 6. Segurança, privacidade e backup

- **Offline-first absoluto:** sem contas remotas, sem telemetria, sem permissões de rede para os dados; a base de dados Room vive no armazenamento do telemóvel.
- **Captura Local de Movimentos & Fila de Pendentes (MB WAY, Moey, Carteira, Bancos):**
  - O serviço `FinancialNotificationListenerService` corre 100% offline no telemóvel (sem permissão `INTERNET`) e extrai montantes e comerciantes de notificações locais.
  - **Zero gravação silenciosa:** todos os movimentos detetados entram na tabela `pending_transactions` e surgem no banner "Movimentos Pendentes (N)" no Início. Nada entra na contabilidade oficial sem validação humana.
  - **Desduplicação Multi-App (Debounce 3 min):** compras notificadas em simultâneo por duas apps (ex.: Google Wallet NFC + Moey, ou MB WAY + Moey) são unificadas; o alerta duplicado é descartado.
  - **Proteção de Despesas Fixas:** se o valor e entidade corresponderem a uma despesa fixa já registada no mês (ex.: ginásio, telecomunicações, renda), a app sinaliza com `[ 📌 Corresponde a despesa fixa deste mês ]`, prevenindo contagem dupla de encargos fixos.
  - **Edição & Validação:** na folha de revisão de pendentes, o utilizador pode alterar a subcategoria e o contexto com 1 toque antes de confirmar, abrir a edição completa, ou descartar.
- **Reembolsos MB WAY & Divisão de Despesas (Custo Líquido Real):**
  - Ao registar uma receita (ex.: dinheiro recebido por MB WAY de amigos por jantar ou compras partilhadas), o utilizador pode associá-la como reembolso de uma despesa pré-existente via `linkedTransactionId`.
  - **Zero inflação da taxa de poupança:** o reembolso não é somado aos rendimentos de trabalho/poupança. Em vez disso, abate diretamente ao gasto da categoria e ao total de despesas do mês (`max(0L, original - reembolsos)`).
  - **Visualização Clara:** os movimentos reembolsados exibem no feed o montante bruto, o total devolvido e o custo líquido real (`Reembolsado: 40,00 € · Custo Real: 20,00 €`).
- **Backup atómico:** a exportação produz um único documento completo; a importação valida a versão do esquema (v1) e não altera nada em caso de erro.
- **Idempotência em todas as importações:** restaurar o mesmo backup duas vezes não duplica nada (a 2.ª importação reporta todos os registos como duplicados ignorados).
- **Biometria opcional:** desligada por defeito para maximizar a velocidade de abertura; ativável em Conta.

---

## 7. Validação executada (testes)

Em 2026-11-01 foi corrida a suite unitária completa (`./gradlew :app:testDebugUnitTest`) e a compilação do APK de debug. **Resultado: 463 testes em 43 classes — 0 falhas, 0 ignorados.** Cada funcionalidade teve contrato próprio e auditoria de código independente com prova de regressão e garantia de segurança.

| Classe de teste | Testes | O que valida |
|---|---|---|
| `SeedIntegrityTest` | 3 | Integridade do seed predefinido: hierarquia sem órfãos (2 níveis) e tipos consistentes, regressão dos flags Fixo com ids únicos e ordenação sequencial, nenhum campo monetário em Float/Double |
| `Phase8And9IntegrityTest` | 10 | Entidades novas (montantes Long, valores positivos, ids únicos), JSON v2 (versão, budgets, goals), compatibilidade v1, chaves malformadas → defaults seguros, payload partido → exceção tipada, coerência `yearMonth`/`epochDay`, id determinístico de metas |
| `BackupSerializerIntegrityTest` | 13 | Round-trip JSON campo a campo, CSV com escapes e formato PT-PT, JSON malformado, defaults seguros, restauro misto idempotente, regras de duplicado |
| `BackupRoundTripFidelityTest` | 9 | Round-trip v2 completo (categorias + movimentos + orçamentos + metas), restauro em DAOs vazios, idempotência, CSV só de movimentos, escapes especiais (`;`, `"`, acentos, €) |
| `BackupSerializerTest` | 5 | Exportação/importação JSON e CSV com escapes |
| `DefaultSeedTest` | 4 | Categorias e contextos predefinidos |
| `MoneyCentsTest` | 6 | Formatação PT-PT e conversões (arredondamento, negativos, valores grandes, percentagens) |
| `MonthlyAnalyticsCalculatorTest` | 10 | % do rendimento, taxa de poupança, médias, desvios, roll-up de categorias, classificação Base vs Extras, guards de rendimento zero |
| `ComparisonCalculatorTest` | 5 | Deltas vs. base (cêntimos + %), guard de base zero, roll-up sub→pai, ordenação por delta, filtro só-despesas, listas vazias |
| `TrendCalculatorTest` | 6 | Séries de 12 slots com zero-fill, média não diluída por categoria, série de taxa de poupança com atividade, agregados multi-anuais com top 3 categorias, entradas vazias |
| `BudgetProgressCalculatorTest` | 17 | Gasto por orçamento com roll-up pai↔sub, estados nos limites (79,9/80/100/100,1), limites 0 e negativos, agregado sem contagem dupla (inclusive transações sem id), ordenação, categoria desconhecida |
| `GoalProgressCalculatorTest` | 15 | Acumulado com meses negativos, projeção nula sem média, `onTrack` com/sem prazo, meses restantes, meta já atingida, truncamento da média |
| `MonthProjectionCalculatorTest` | 13 | Média diária × dias do mês, dia 1, dia intermédio, mês passado/futuro, mês vazio, rendimento zero, taxa negativa |
| `CalendarAggregatorTest` | 14 | Todos os dias do mês com epochDay exato, ano bissexto, dias sem gasto a zero, ordenação, `isToday` injetado, receitas não contam como gasto, ignorância de dias fora do mês |
| `MonthlyReportGeneratorTest` | 15 | Relatório PT-PT determinístico com todos os blocos, truncamento top-N (3/3/5), blocos de orçamentos/metas presentes/ausentes, valores negativos, ordenação estável |
| `RecurringMonthGeneratorTest` | 6 | Geração de fixos sem duplicados, clamping de dias (31 jan → 28 fev, anos bissextos), chave de merchant case/espaços, data forçada |
| `SmartPrefillEngineTest` | 7 | Auto-preenchimento por estabelecimento (case-insensitive, desempates, trim) e merchants recentes (distintos, limite 10) |
| `CalculatorNumpadEvaluatorTest` | 14 | Regras do teclado calculadora (vírgula, `00`, `+`, truncamento decimal, soma grande, limpar/apagar) |
| `HomeViewModelTest` | 19 | Cockpit do mês, banner "Virar do Mês" (3 estados), geração com dia clampado, navegação entre anos, cartões de orçamentos/projeção/metas, mês vazio |
| `TransactionsViewModelTest` | 21 | Pesquisa nas 4 dimensões, filtros período/fluxo/contexto/método/valor com toggle, filtros combinados, splits de gasto/recebido, cartão de soma, ordenação, apagar |
| `AddTransactionViewModelTest` | 20 | Gravação, guarda de valor 0, expressões somadas, trim, fallback de categoria, Fixo, sugestões recentes, display ao digitar, contextos personalizados |
| `EditFlowFidelityTest` | 12 | Edição preserva campos não editados, alterar só o valor, DAO sem duplicação, valor 0 bloqueia em edição, `startNewEntry` limpa o id, receitas e fixos |
| `DateSelectionTest` | 8 | Data escolhida define `yearMonth`/`epochDay`, mês e ano anteriores, dia 1 e último dia, ano bissexto, 12 meses do ano |
| `AnalyticsViewModelTest` | 26 | 6 vistas, média só sobre meses com atividade, categorias desconhecidas, as 3 bases de comparação, tendências com chips estáveis, taxa de poupança, totais por ano, calendário, relatório construído no clique, subscrições no uiState |
| `CollectorLifecycleTest` | 8 | Número de coletores ativos estável após várias mudanças de mês (prova por mutação: remover a correção faz os testes falhar) |
| `AccountViewModelTest` | 18 | Criar/eliminar subcategorias com mensagens exatas, contextos persistidos e normalizados, exportar/importar com mensagens, biometria |
| `AccountBudgetsGoalsFlowTest` | 13 | CRUD de orçamentos (id determinístico, substituição) e metas (prazo, arquivo), mensagens exatas, progresso da meta, ciclo exportar→importar |
| `ContextTagsPersistenceTest` | 9 | Store de contextos, arranque com tags persistidas, adicionar/persistir/recarregar, normalização, tags visíveis e selecionáveis no registo |
| `DateDropdownUtilsTest` | 4 | Utilitários de dropdown de data: dias no mês, clamping de dia (bissexto/não bissexto), conversão bidirecional epochDay e texto PT-PT |
| `BankNotificationParserTest` | 10 | Extração de montante em cêntimos (Long), comerciante, método de pagamento e app de origem de notificações de MB WAY, Moey, Google Wallet e bancos |
| `TransactionDeduplicatorTest` | 5 | Debounce de 3 minutos para apps concorrentes simultâneas (ex.: MB WAY + Moey), desduplicação no mesmo dia e correspondência com despesas fixas do mês |
| `PendingTransactionsFlowTest` | 6 | Ciclo de vida da Fila de Pendentes: carregar para UI state, confirmar com 1 toque, editar categoria/contexto antes de confirmar, descartar e confirmar todos |
| `SubscriptionDetectorTest` | 10 | Deteção de subscrições ativas no mês visualizado, cálculo de custo anualizado (`Long`), deteção de subida de preço vs mês anterior, descarte de canceladas e filtragem de despesas sem comerciante |
| `NetExpenseCalculatorTest` | 6 | Cálculo de custo líquido pós-reembolso, reembolsos parciais e totais, múltiplos reembolsos, guardas de zero, não inflação de rendimentos nem distorção da taxa de poupança |

Adicionalmente: `./gradlew :app:assembleDebug` compila o APK de debug sem erros (463 testes, 0 falhas) e `./gradlew :app:lintDebug` não reporta erros. A instalação num dispositivo/emulador faz-se com `./gradlew :app:installDebug`.

---

## 8. Formatação e convenções

- **Moeda:** `1.234,56 €` (ponto nos milhares, vírgula decimal).
- **Percentagens:** `24,5%` (1 casa decimal, sempre).
- **Meses:** `Outubro 2026`; datas nos cabeçalhos: `Hoje · 26 de outubro`, `Ontem · 25 de outubro`, ou dia da semana completo.
- **Cores:** verde = receitas/poupança, coral/vermelho = gastos, azul = ações e seleção.
- **Modo escuro exclusivo** (fundo `#0B0E14`, superfícies `#151A24`).
- **Gráficos desenhados à mão** em Compose `Canvas` — nenhuma biblioteca externa de gráficos.

---

## 9. Comportamentos a conhecer (limitações atuais)

Estes comportamentos são intencionais ou limitações conhecidas — convém sabê-los antes de usar/testar:

1. **Valor 0 no registo não grava e não avisa** — a folha de registo simplesmente permanece aberta.
2. **Média mensal usa apenas meses com gastos** — um mês novo sem dados não "puxa" a média para baixo.
3. **A taxa de poupança só fica verde a partir de 20%** — abaixo disso o rótulo fica na cor neutra (não é um erro).
4. **Gastos lançados diretamente no grupo pai** aparecem na Análise como subcategoria "{Grupo} (Geral)" — é intencional, para não perder valores.
5. **A app não avisa quando se ultrapassa um orçamento** — mostra o estado (Atenção/Excedido) mas não gera notificações.
6. **Um backup JSON com campos malformados é restaurado com valores por omissão** (ex.: um limite ilegível fica a 0) em vez de ser recusado — evita perder todo o resto do backup.
7. **Os orçamentos e metas não entram no CSV** — esse formato continua a ser só de movimentos (o JSON é o backup completo).
8. **Sem desfazer** — apagar é definitivo, embora passe por um diálogo de confirmação.
