# Changelog — Finflow (Finanças Pessoais)

Todas as alterações notáveis deste projeto são documentadas neste ficheiro de acordo com os princípios do [Semantic Versioning (SemVer)](https://semver.org/).

Formato de Versão: `MAJOR.MINOR.PATCH`
- **MAJOR**: Alterações de arquitetura, redesigns globais ou quebras de compatibilidade.
- **MINOR**: Novas funcionalidades, novos ecrãs, gráficos ou ferramentas de análise.
- **PATCH**: Correções de bugs, ajustes de sincronização, melhorias de desempenho e usabilidade.

---

## [Por lançar]
### Adicionado
- **Backup cifrado (`.finflow`)**: a exportação completa passa a ser protegida com palavra-passe (AES-256-GCM, chave derivada com PBKDF2). O restauro deteta o formato e pede a palavra-passe; o JSON em claro só sai por opção explícita.
- **Cotações automáticas opcionais**: interruptor em Conta → Versão (desligado por omissão) e aviso antes da primeira consulta ao Yahoo Finance.

### Segurança
- Ligações às APIs (GitHub, Yahoo) através de um cliente único com lista de servidores, HTTPS obrigatório, sem redirecionamentos e com limite de tamanho; tickers validados.
- Cotações: câmbio por moeda (incluindo cotações em pence), sem taxa fixa em caso de falha e com rejeição de variações implausíveis.
- Importação XTB: a deduplicação passa a distinguir instrumentos, por isso dividendos iguais no mesmo dia já não se perdem.
- Restauro de backup valida tickers e unidades.
- Mostrar valores no modo privacidade exige desbloqueio quando o bloqueio está ativo.
- Movimentos pendentes com mais de 30 dias são apagados automaticamente.
- Proteção contra sobreposições de ecrã (tapjacking).
- CI: Dependabot e análise CodeQL.

### Corrigido
- Com o modo privacidade ativo, os formulários de edição (investimentos, alocações, valor do movimento) eram preenchidos com `••••` em vez do número; guardar podia gravar 0.

### Melhorado
- Tradução completa para inglês (cerca de 580 textos, datas e dias da semana).
- Testes de migração da base de dados em todas as versões (1 → 8) e esquema exportado.
- Ecrãs de Conta, Análise e Património divididos em ficheiros mais pequenos.
- CI com lint, cobertura mínima do domínio e compilação dos testes de interface.

---

## [2.7.0] — 2026-11-10 (Minor Release)
### Adicionado
- **Modo Privacidade (Máscara de Valores)**: Botão de olho rápido (👁️/👁️‍🗨️) no cabeçalho e interruptor nas preferências de segurança em Conta para mascarar todos os montantes com `•••• €`. Permite mostrar e demonstrar a app com total segurança, preservando a navegação, percentagens, proporções e gráficos operacionais.
- **Novo fluxo de movimento em 3 passos**: A criação de movimentos passa a ser guiada em Valor → Categoria → Detalhes, com o teclado numérico e o montante em foco imediato.

### Melhorado
- **Deteção de comerciantes em notificações**: Suporte melhorado a linhas agrupadas, mensagens e compras por cartão, com limpeza automática de referências de pagamento.

---

## [2.6.0] — 2026-11-09 (Minor Release)
### Segurança
- **Backups privados**: o backup automático antes de cada atualização passa a ficar no armazenamento privado da app (3 mais recentes) em vez de Downloads. Exportar JSON/CSV abre o seletor de ficheiros e o utilizador escolhe o destino.
- **Bloqueio da app reforçado**: deixa de ser possível contorná-lo alterando a hora do telemóvel; ligar ou desligar o bloqueio exige autenticação; o PIN/padrão do telemóvel funciona como alternativa à biometria.
- **Atualizações verificadas**: só são instaladas atualizações assinadas com o certificado oficial do Finflow, do mesmo pacote, com versão superior e descarregadas do repositório oficial no GitHub.
- **Troca de telemóvel**: os dados deixam de ser copiados para backups do Android; se a chave de encriptação faltar, a app arranca vazia e pede para restaurar um backup em vez de fechar.
- **Captura de notificações**: só apps bancárias e de pagamento reconhecidas pelo nome do pacote (sem palavras genéricas como "bank" ou "banco").
- **Importações e exportações**: limites de tamanho nos ficheiros importados e neutralização de fórmulas no CSV.

### Adicionado
- Opção para desligar a procura automática de atualizações (Conta → Versão).

### Alterado
- Android SDK 37 (compilação) e 36 (alvo); Gradle 9.9, Android Gradle Plugin 9.5, Kotlin 2.4, Room 2.8, Compose BOM 2026.09, SQLCipher 4.20.
- Release minificada com R8 (APK de ~35 MB para ~10 MB).
- Política de privacidade e textos da app atualizados: a app guarda os dados localmente e só contacta o GitHub para procurar atualizações.

---

## [2.5.2] — 2026-11-07 (Patch & Fix)
### Corrigido
- **Alocação de saldo 100% livre e editável**: qualquer quantia pode agora ser inserida no campo de texto de alocação, complementada por atalhos de percentagem (25%, 50%, 75%, Tudo) com feedback em tempo real.
- **Visibilidade perfeita em Modo Claro**: corrigido o contraste no diálogo de alocação de ativos e no painel de destaque (cores de texto invertidas e campos de texto com fundo e contraste adequados).
- **Terminologia PT-PT**: substituído o termo 'Aporte' por 'Reforço' em todos os ecrãs e serviços da aplicação.
- **Notificações moey! com cartão de crédito**: suporte para títulos com pontuação (`moey!`) e limpeza de sufixos de cartão de crédito para extração precisa do comerciante e valor.

---

## [2.5.1] — 2026-11-07 (Patch & Fix)
### Corrigido
- **Captura de notificações alargada**: adicionado reconhecimento de notificações para apps de corretagem e carteiras de investimento (XTB, Trade Republic, DEGIRO, Wise, N26, Openbank) e cartões de refeição (Coverflex, Edenred, Pluxee, Sodexo).
- **Prioridade de atualização no GitHub**: ordem de artefactos no pipeline de release ajustada para garantir que utilizadores com versões anteriores atualizam sempre a aplicação principal (`Finflow-<versão>.apk`) sem confundir com o pacote Demo.

---

## [2.5.0] — 2026-11-06 (Minor Release)
### Adicionado
- **Dois Pilares de Poupança e Investimento**: separação clara entre Reserva Segura (capital 100% garantido em depósitos a prazo e certificados de aforro) e Carteira de Investimentos (crescimento a longo prazo em ETFs, PPRs e ações), visíveis logo abaixo do resumo patrimonial.
- **Painel de Destaque "Saldo por Alocar"**: alerta no topo do separador de Investimentos quando existem transferências de investimento pendentes de destino (ex.: transferências para XTB para 'decidir depois').
- **Alocação Modular Fracionada**: botão de alocação que permite dividir um montante livremente (ex.: transferir 50 € para um ativo e deixar 100 € em espera para alocar mais tarde), com atalhos de preenchimento rápido (Tudo, 50 €, 100 €).
- O ativo temporário de espera deixa de surgir como cartão avulso a poluir a lista de ativos reais.
- Suporte para seleção de ativo de investimento em transferências bancárias (`TRANSFER`), além de despesas.

---

## [2.4.1] — 2026-11-05 (Patch & Fix)
### Corrigido
- **Atualização da app demo**: a demo oferecia o APK da app real (outro pacote e outra assinatura), que nunca instalava por cima. As releases passam a incluir `Finflow-Demo-<versão>.apk` e o atualizador escolhe o APK do seu flavor. O APK de produção deixa de poder ser confundido com o da demo no pipeline.

---

## [2.4.0] — 2026-11-05 (Minor Release)
### Adicionado
- **Investimentos em dois toques**: categorizar um movimento pendente como `Poupança / Investimento` mostra a escolha do destino sem nada pré-selecionado (um ativo ou `Decidir depois (XTB)`, que envia para `XTB – por alocar`) e regista a transferência, sem contar como despesa. Se um ativo tiver `Reforço mensal` definido (ex.: 200 €/mês), esse ativo vem sugerido quando o valor coincide; `Confirmar Todos` só envia automaticamente esses casos, os restantes ficam pendentes.
- **`+ Aporte`** em cada ativo da Carteira para somar dinheiro diretamente (registado como transferência).
- A folha `Novo Movimento` pergunta o ativo quando a categoria é investimento.

---

## [2.3.3] — 2026-11-04 (Patch & Fix)
### Alterado
- **Backup completo**: o ficheiro (esquema v3) passa a incluir as definições da app (nome, moeda, ciclo, tema, idioma, biometria, captura de notificações, etiquetas) e a foto de perfil (reduzida); ao restaurar são aplicadas automaticamente. Backups antigos continuam a importar-se.

---

## [2.3.2] — 2026-11-04 (Patch & Fix)
### Alterado
- **Atualizador mais seguro**: guarda um backup automático em `Downloads` antes de instalar e verifica a assinatura do APK, mostrando uma mensagem clara em vez do erro genérico do Android quando não é compatível.

---

## [2.3.1] — 2026-11-04 (Patch & Fix)
### Alterado
- **Backup em ficheiro**: `Exportar Backup (JSON)` e `Exportar CSV` guardam o ficheiro em `Downloads` (fallback para partilha); o restauro lê o ficheiro pelo seletor.

---

## [2.3.0] — 2026-11-04 (Minor Release)
### Adicionado
- **Transferências para XTB & Optimize**: movimentos pendentes da XTB ou Optimize deixam de contar como despesa e passam a transferências ligadas a um ativo da Carteira.
  - Optimize: pergunta sempre para que ativo foi o dinheiro.
  - XTB: vai por omissão para o ativo 'XTB – por alocar', criado automaticamente; o botão **Alocar** na Carteira passa o saldo para os ativos reais.
  - Apagar ou editar uma transferência ajusta o valor do ativo; o backup (JSON) inclui o novo campo `investmentAssetId`.
### Alterado
- Base de dados migrada para a versão 7 (nova coluna `investmentAssetId` em `transactions`), sem perda de dados.

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
