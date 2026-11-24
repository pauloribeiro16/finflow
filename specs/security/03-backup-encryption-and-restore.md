# Spec 03 — Backup Encryption, Restore & Local Files

## Objectivo
Garantir confidencialidade e integridade autenticada dos backups exportados (`.finflow`) e ficheiros locais sensíveis (`.enc`), impor força mínima de palavra-passe, validar e sanitizar backups importados e assegurar restauro atómico.

## Ficheiros Cobertos
- `app/src/main/kotlin/com/financas/app/data/repository/BackupCrypto.kt`
- `app/src/main/kotlin/com/financas/app/data/repository/BackupPasswordPolicy.kt`
- `app/src/main/kotlin/com/financas/app/data/repository/BackupSanitizer.kt`
- `app/src/main/kotlin/com/financas/app/data/repository/BackupSerializer.kt`
- `app/src/main/kotlin/com/financas/app/data/repository/SettingsBackupManager.kt`
- `app/src/main/kotlin/com/financas/app/core/security/LocalFileCipher.kt`
- `app/src/main/kotlin/com/financas/app/ui/screens/account/AccountViewModel.kt` (`importJson`)

## Requisitos Funcionais e de Segurança (EARS)
1. **Cifra de backups `.finflow`:** WHEN um backup cifrado é exportado, THE SYSTEM SHALL derivar uma chave AES-256 via `PBKDF2WithHmacSHA256` (600.000 iterações por omissão, salt aleatório de 16 bytes), cifrar com `AES/GCM/NoPadding` (IV aleatório de 12 bytes, tag de 128 bits) e autenticar `AAD = "finflow-encrypted-backup:1:PBKDF2WithHmacSHA256:<iter>"`.
2. **Decifra estrita:** WHEN um envelope `.finflow` é decifrado, THE SYSTEM SHALL validar `format`, `v == 1`, `kdf`, `iter ∈ [100_000, 5_000_000]`, `salt.size == 16`, `iv.size == 12`, e verificar a tag GCM com AAD; falha de tag SHALL lançar `WrongBackupPasswordException` e limpar a `PBEKeySpec` em `finally`.
3. **Política de palavra-passe:** WHEN o utilizador cria um backup cifrado na UI, THE SYSTEM SHALL exigir `BackupPasswordPolicy.isAcceptable(password)` (mínimo 10 caracteres e ≥ 50 bits efetivos após penalizar repetições, sequências e fragmentos comuns). `BackupCrypto.encrypt` SHALL rejeitar palavras-passe com menos de 8 caracteres.
4. **Cifra de ficheiros locais (`LocalFileCipher`):** WHEN um ficheiro privado local (ex. backup pré-update) é cifrado, THE SYSTEM SHALL gerar uma DEK aleatória de 32 bytes, cifrá-la com a chave Keystore `finflow_local_files_master` (AES-256-GCM), cifrar os dados com a DEK e `AAD = "FFLF1"`, gravar `FFLF1 || wrapIv[12] || wrappedKey[48] || dataIv[12] || ciphertext+tag`, e zerar `dataKey` em `finally`.
5. **Restauro sanitizado e atómico:** WHEN um JSON de backup é importado em `AccountViewModel.importJson`, THE SYSTEM SHALL executar `BackupSanitizer.sanitize` (rejeitando/corrigindo valores inválidos, `units` não finitos, tickers inválidos) e aplicar o restauro dentro de `runInTransaction` para evitar estado parcial na BD em caso de erro.
6. **Proteção contra CSV Formula Injection:** WHEN movimentos são exportados em CSV, THE SYSTEM SHALL prefixar células iniciadas por `=`, `+`, `-`, `@`, `\t`, `\r` para neutralizar fórmulas em folhas de cálculo.

## Acceptance Criteria
- [ ] GIVEN um texto e palavra-passe válida WHEN `BackupCrypto.encrypt` seguido de `decrypt` executa THEN recupera o texto exato; duas cifras do mesmo texto produzem `salt`, `iv` e `data` distintos.
- [ ] GIVEN um envelope `.finflow` válido WHEN qualquer byte de `data`, `iv`, `salt` ou o campo `iter` é adulterado THEN `decrypt` falha (`WrongBackupPasswordException` ou `IllegalArgumentException`).
- [ ] GIVEN um payload cifrado por `LocalFileCipher.encrypt` WHEN qualquer byte do cabeçalho, `wrapIv`, `wrappedKey`, `dataIv` ou `ciphertext` é invertido THEN `decrypt` lança `GeneralSecurityException`.
- [ ] GIVEN uma palavra-passe curta (<10 chars) ou com fragmentos comuns (`password1234`, `finflow2026`) WHEN `BackupPasswordPolicy.isAcceptable` é chamado THEN devolve `false`.

## Fora de Âmbito
- Recuperação de backups `.finflow` sem a palavra-passe (impossível por desenho).

## Riscos e Questões Abertas
- **Desalinhamento de constante mínima:** `BackupCrypto.MIN_PASSWORD_LENGTH = 8` enquanto `BackupPasswordPolicy.MIN_LENGTH = 10` (a UI impõe 10, a API criptográfica aceita 8 para compatibilidade com backups antigos).
- **Chave derivada em `BackupCrypto.deriveKey`:** `keyBytes` é passado a `SecretKeySpec` (que copia o array internamente), sem `keyBytes.fill(0)` após criação do `SecretKeySpec`.
