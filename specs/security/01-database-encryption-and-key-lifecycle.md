# Spec 01 — Database Encryption & Key Lifecycle

## Objectivo
Garantir que toda a base de dados Room (`financas_app_db`) é cifrada em repouso com SQLCipher (chave aleatória de 256 bits protegida por Android Keystore AES-256-GCM), que bases legadas em texto claro são migradas sem perda de dados nem resíduos em disco, e que chaves irrecuperáveis disparam quarentena segura com aviso ao utilizador.

## Ficheiros Cobertos
- `app/src/main/kotlin/com/financas/app/core/security/DatabaseKeyManager.kt`
- `app/src/main/kotlin/com/financas/app/core/security/DatabaseEncryptionMigrator.kt`
- `app/src/main/kotlin/com/financas/app/core/security/DatabaseRecovery.kt`
- `app/src/main/kotlin/com/financas/app/di/AppContainer.kt` (`DefaultAppContainer.database`)

## Requisitos Funcionais e de Segurança (EARS)
1. **Geração e proteção da chave:** WHEN `database.key` não existe, THE SYSTEM SHALL gerar 32 bytes via `SecureRandom`, cifrá-los com a chave mestra `finflow_db_master` (AES-256-GCM, Android Keystore) e gravar `Base64(iv[12] || ciphertext[32] || tag[16])` (60 bytes decodificados).
2. **Validação estrita do blob:** WHEN `database.key` existe, THE SYSTEM SHALL validar o formato Base64 e o tamanho exato de 60 bytes antes de decifrar; se falhar formato, chave mestra ausente ou `AEADBadTagException`, THE SYSTEM SHALL lançar `UnrecoverableDatabaseKeyException`.
3. **Recuperação por quarentena:** WHEN `UnrecoverableDatabaseKeyException` ocorre no arranque, THE SYSTEM SHALL isolar a BD (`db`, `-wal`, `-shm`, `-journal`) e `database.key` com sufixo `.unrecoverable-<timestamp>`, marcar `database_recovery_notice = true` de forma síncrona (`commit()`), e gerar nova passphrase para arranque limpo.
4. **Migração plaintext → SQLCipher:** WHEN `financas_app_db` tem cabeçalho `"SQLite format 3\u0000"`, THE SYSTEM SHALL renomear `db`, `-wal` e `-shm` para `*.plaintext*`, exportar todos os dados + pendentes, importar na BD cifrada, verificar contagens de categorias, movimentos e pendentes, e apagar todos os ficheiros `.plaintext*`.
5. **Retoma após crash:** WHEN o processo falha durante a migração e reinicia com `*.plaintext` presente, THE SYSTEM SHALL retomar `completeMigration` sem perder dados legados.

## Acceptance Criteria
- [ ] GIVEN `database.key` válido e chave mestra presente WHEN `getOrCreatePassphrase()` é chamado THEN devolve os mesmos 32 bytes determinísticos.
- [ ] GIVEN `database.key` corrompido ou chave mestra apagada WHEN `DefaultAppContainer.database` inicializa THEN os ficheiros anteriores são renomeados para `*.unrecoverable-*`, `DatabaseRecovery.hasPendingNotice()` devolve `true` e a app arranca sem crash.
- [ ] GIVEN uma BD SQLite em texto claro WHEN `migrateIfNeeded()` termina THEN `isPlaintextDatabase(dbFile)` é `false`, todos os registos existem na BD cifrada e nenhum ficheiro `*.plaintext*` resta em disco.
- [ ] GIVEN um blob Base64 com tamanho != 60 bytes WHEN `DatabaseKeyManager.isValidBlobFormat()` é chamado THEN devolve `false`.

## Invariantes Formais (`formal/tla/DbEncryptionLifecycle.tla`)
- `NoPlaintextLeftAfterReady`: Quando a app está pronta (`ready`), nenhum ficheiro `.plaintext` existe em disco e a BD ativa não está em plaintext.
- `KeyMatchesEncryptedDb`: Quando a app abre a BD cifrada (`ready`), a chave em memória corresponde à chave com que o ficheiro da BD foi cifrado.
- `NoSilentDataLoss`: Dados do utilizador nunca desaparecem silenciosamente sem estarem na BD cifrada ativa, no ficheiro `.plaintext`, ou em quarentena com `recoveryNotice = TRUE`.
- `RecoveryNoticeOnQuarantine`: Se a BD foi colocada em quarentena, `recoveryNotice = TRUE`.

## Fora de Âmbito
- `setUserAuthenticationRequired(true)` na chave Keystore (aceite como exceção documentada S10 para permitir captura de notificações bancárias em segundo plano).
- Ataques com acesso root / debug ativo no dispositivo.

## Riscos e Questões Abertas
- **Falta de `database.key` com BD cifrada existente:** Se `database.key` desaparecer isoladamente enquanto `financas_app_db` cifrada ainda existe, `getOrCreatePassphrase()` gera uma chave nova sem chamar `DatabaseRecovery.quarantine()`, deixando a BD cifrada inabrível. (Verificado no modelo TLA+ `DbEncryptionLifecycle.tla`.)
- **Crash parcial durante `preserveLegacyFiles` ou `importIntoEncryptedDatabase`:** Avaliado formalmente no modelo TLA+.
