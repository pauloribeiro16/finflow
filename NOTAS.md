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
