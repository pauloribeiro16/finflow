# NOTAS

## 2026-11-09 — auditoria de segurança v2.5.2
O audit de 2026-11-01 ficou desatualizado: a app tem agora INTERNET, auto-updater com REQUEST_INSTALL_PACKAGES e o repo é público.
Principal risco: o backup JSON completo, sem cifra, é gravado em Downloads a cada atualização e anula o SQLCipher. Seguem-se o bypass do bloqueio biométrico via relógio do sistema e a chave de assinatura exposta a uma action não fixada no release.yml.
Fonte: `SECURITY_AUDIT_2026-11-09.md` (14 achados, prioridades no fim).

## 2026-11-09 — correções da auditoria de segurança
Corrigidos S1–S9 e S11–S13; S10 aceite; S14 parcial (SQLCipher 4.17.0, pois ≥4.18 exige compileSdk 36+).
Gotchas: `FLAG_SECURE` torna as capturas de ecrã pretas em release (usar `uiautomator dump`); o certificado de release está fixado em `UpdateSignatureCheck.RELEASE_CERT_SHA256` e no `release.yml` — trocar a chave obriga a atualizar ambos; os testes androidTest já não compilavam antes destas alterações.
Fonte: `SECURITY_AUDIT_2026-11-09.md` (secção "Estado das correções").


## 2026-11-09 — v2.6.0: atualização de plataforma
Gradle 9.9, AGP 9.5 (Kotlin incorporado: sem plugin `kotlin-android`, `kotlin { compilerOptions }` em vez de `kotlinOptions`, `resValues = true` obrigatório para `resValue`), Kotlin 2.4.21, KSP 2.3.13 (KSP1 removido), compileSdk 37 / targetSdk 36, SQLCipher 4.20.0, Room 2.8.5, Compose BOM 2026.09.00.
Gotchas: o lint novo do Compose exige `Locale.ROOT` em pesquisas dentro de composables (NonObservableLocale); o primeiro build depois da atualização demora ~20 min (caches frias, lint ×2, R8 ×2).
Não testado no emulador (falta de RAM): validar a atualização 2.5.2 → 2.6.0 num telemóvel antes de publicar.

## 2026-11-09 — novo movimento em 3 passos e deteção de comerciante
O ecrã de novo movimento passou a 3 passos (Valor → Categoria → Detalhes) para o valor ficar visível junto ao teclado; editar ou rever pendente abre nos Detalhes.
"Comerciante desconhecido" vinha de o serviço só ler título/texto/bigText/subText e de faltarem preposições ("de", "à", "at"); agora lê também linhas agrupadas, mensagens e resumo, ignora referências de cartão e usa "Pagamento <app>"/"Recebimento <app>" como fallback, guardando o texto original no pendente.
Fontes: `AddTransactionBottomSheet.kt`, `BankNotificationParser.kt`, `FinancialNotificationListenerService.kt`.

## 2026-11-10 — modo privacidade (ocultação de montantes com máscara)
Adicionado botão de olho (👁️/👁️‍🗨️) na barra superior do Finflow e interruptor em Conta → Segurança para mascarar instantaneamente todos os valores monetários com "•••• €" (mantendo categorias, datas, percentagens e gráficos operacionais).
Implementado de forma cirúrgica em `MoneyCents.formatCents`/`formatPt` com toggle reativo via `UserProfileStore.privacyModeFlow` e `LocalPrivacyMode`, garantindo recomposição atómica em todos os ecrãs sem expor montantes reais em apresentações a terceiros.
Fontes: `MoneyCents.kt`, `UserProfileStore.kt`, `MainScaffold.kt`, `AccountScreen.kt`, `MoneyCentsTest.kt`.

## 2026-11-11 — cobertura integral do modo privacidade em todos os ecrãs e gráficos
O modo privacidade passou a cobrir 100% dos valores monetários em todos os separadores: todas as lentes de Análise (No Quê, Onde, Como, Evolução, Comparar, Calendário, Subscrições), eixos Y e legendas dos gráficos Canvas, ecrãs de Orçamentos/Metas/Investimentos, cartões de património e badges de movimentos.
O `AnalyticsViewModel` passa a observar ativamente `userProfileStore.privacyModeFlow` e o `UserProfileStore` atualiza `MoneyCents.isPrivacyModeEnabled` antes de emitir no fluxo, assegurando consistência reativa atómica sem valores residuais.
Fontes: `AnalyticsViewModel.kt`, `InteractiveChartsCanvas.kt`, `UserProfileStore.kt`, `BudgetsGoalsScreen.kt`, `AnalyticsScreen.kt`.

## 2026-11-11 — estimativa diária Yahoo Finance, catalogação de posições e sanitização XTB
Adicionado suporte a cotações em tempo real via Yahoo Finance (v8 chart API nativa, zero dependências externas) para ativos com ticker e unidades detidas.
Implementada a catalogação completa das posições iniciais (volume, preço médio de compra, capital investido e selo '⚡ 2x Alavancado' para instrumentos como o Nasdaq LQQ), permitindo comparar a posição de entrada com a cotação estimada atual.
Garantida sanitização rigorosa da extração da XTB: zero IDs de conta, zero IDs de ordens/posições e deduplicação de dividendos/juros via hash anónimo SHA-256. Migração Room 7 -> 8 adiciona colunas `ticker` e `units` sem perda de dados.
Fontes: `YahooFinanceQuoteService.kt`, `PortfolioMarketSyncUseCase.kt`, `XtbTickerMapper.kt`, `XtbSyncEngine.kt`, `BudgetsGoalsScreen.kt`, `InvestmentAssetEntity.kt`.


