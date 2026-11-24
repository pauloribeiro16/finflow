# Spec 05 — Notification Capture Security

## Objectivo
Garantir que a captura automática de notificações bancárias (`FinancialNotificationListenerService`) não permite que uma app maliciosa ou notificação forjada entre na aplicação, desbloqueie a sessão, injete movimentos confirmados, execute código/intents arbitrários ou bloqueie notificações bancárias legítimas.

## Ficheiros Cobertos
- `app/src/main/AndroidManifest.xml` (`FinancialNotificationListenerService`, `BIND_NOTIFICATION_LISTENER_SERVICE`)
- `app/src/main/kotlin/com/financas/app/data/service/FinancialNotificationListenerService.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/FinancialAppFilter.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/BankNotificationParser.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/PendingFloodGuard.kt`
- `app/src/main/kotlin/com/financas/app/domain/usecase/PendingRetention.kt`

## Requisitos de Segurança (EARS)
1. **Isolamento IPC do serviço:** THE SYSTEM SHALL proteger `FinancialNotificationListenerService` com `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` (permissão `signature` reservada ao `system_server`).
2. **Sem bypass de autenticação nem execução de intents:** WHEN uma notificação é recebida em `onNotificationPosted`, THE SYSTEM SHALL ignorar `contentIntent`/`actions`, SHALL NOT alterar `sessionLocked`, e SHALL NOT inserir diretamente na tabela `transactions` (apenas em `pending_transactions`, exigindo revisão humana).
3. **Filtragem de origem:** WHEN `onNotificationPosted` recebe uma notificação, THE SYSTEM SHALL rejeitar notificações da própria app, notificações `FLAG_ONGOING_EVENT`, notificações quando `capture_enabled == false`, e pacotes fora do filtro financeiro.
4. **Controlo de flood e retenção:** WHEN uma notificação parsed é processada, THE SYSTEM SHALL purgar pendentes com mais de 30 dias (`PendingRetention`), SHALL limitar a fila a `MAX_PENDING_TOTAL = 500` e `MAX_PER_SOURCE_PER_HOUR = 30` (`PendingFloodGuard`), e SHALL deduplicar contra pendentes e movimentos do dia.
5. **Limites de entrada e segurança numérica:** WHEN `BankNotificationParser` analisa texto de notificação não confiável, THE SYSTEM SHALL validar `amountCents > 0` sem overflow de `Long` e truncar notas armazenadas (`MAX_ORIGINAL_TEXT_CHARS = 300`, `MAX_PACKAGE_CHARS = 120`).

## Acceptance Criteria
- [ ] GIVEN uma notificação de uma app maliciosa WHEN `onNotificationPosted` executa THEN `sessionLocked` permanece inalterado e nenhum registo é inserido em `transactions`.
- [ ] GIVEN 500 pendentes na fila ou 30 da mesma origem na última hora WHEN chega nova notificação THEN `PendingFloodGuard.shouldAccept` devolve `false`.
- [ ] GIVEN uma notificação com montante > `Long.MAX_VALUE / 100` WHEN `BankNotificationParser.extractAmountCents` executa THEN não sofre overflow silencioso para valores negativos ou truncados.

## Invariantes Formais (`formal/tla/NotificationCaptureSecurity.tla`)
- `NoSessionUnlockFromNotification`: Uma notificação nunca desbloqueia a sessão.
- `NoDirectTransactionWithoutUserReview`: Uma notificação nunca cria um movimento confirmado sem revisão do utilizador.
- `NoSpoofedPackageAccepted`: Uma app atacante com package name forjado (ex. `com.evil.mbway`) não deve ser aceite como banco legítimo. *(Achado real — ver relatório TLC.)*
- `QueueBounded`: `pendingCount <= MAX_PENDING_TOTAL`.

## Fora de Âmbito
- Proteção contra apps com privilégios root ou acessibilidade total no dispositivo.

## Riscos e Questões Abertas (Achados Reais)
- **Spoofing de package name em `FinancialAppFilter`:** Como o filtro aceita qualquer package que contenha um segmento/prefixo/sufixo de marca (`com.evil.mbway`, `org.fake.revolut`), uma app maliciosa instalada pode injetar pendentes falsos e, em volume, esgotar `MAX_PENDING_TOTAL` (starvation de bancos reais).
- **Entrada não truncada antes de regex em `BankNotificationParser`:** O texto bruto só é truncado para `originalTextNote` depois do `parse()`.
- **Overflow em `parseStringToCents`:** `euros * 100L + cents` não verifica overflow para sequências numéricas gigantes.
