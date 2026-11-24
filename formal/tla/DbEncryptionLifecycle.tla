---- MODULE DbEncryptionLifecycle ----
(*
 * Formal TLA+ model of Finflow's SQLCipher database encryption & key lifecycle.
 *
 * Code covered (sync commit eb8dc12):
 *   - app/src/main/kotlin/com/financas/app/di/AppContainer.kt (DefaultAppContainer.database)
 *   - app/src/main/kotlin/com/financas/app/core/security/DatabaseKeyManager.kt
 *   - app/src/main/kotlin/com/financas/app/core/security/DatabaseEncryptionMigrator.kt
 *   - app/src/main/kotlin/com/financas/app/core/security/DatabaseRecovery.kt
 *)

VARIABLES
    phase,          \* "init" | "key_ready" | "migrating_preserved" | "migrating_imported" | "ready"
    dbFile,         \* "none" | "plaintext" | "encrypted_k1" | "encrypted_k2"
    legacyFile,     \* "none" | "plaintext"
    keyFile,        \* "none" | "valid_k1" | "valid_k2" | "corrupted"
    keystoreKey,    \* "present" | "missing"
    memPassphrase,  \* "none" | "k1" | "k2"
    quarantined,    \* BOOLEAN: DatabaseRecovery.quarantine was invoked
    recoveryNotice, \* BOOLEAN: DatabaseRecovery.hasPendingNotice
    hadUserData     \* BOOLEAN: ghost variable, TRUE if user had existing data at boot

vars == <<phase, dbFile, legacyFile, keyFile, keystoreKey, memPassphrase, quarantined, recoveryNotice, hadUserData>>

Init ==
    /\ phase = "init"
    /\ legacyFile = "none"
    /\ memPassphrase = "none"
    /\ quarantined = FALSE
    /\ recoveryNotice = FALSE
    /\ \/ (* Fresh install *)
          (dbFile = "none" /\ keyFile = "none" /\ keystoreKey = "present" /\ hadUserData = FALSE)
       \/ (* Legacy plaintext upgrade *)
          (dbFile = "plaintext" /\ keyFile = "none" /\ keystoreKey = "present" /\ hadUserData = TRUE)
       \/ (* Normal encrypted boot *)
          (dbFile = "encrypted_k1" /\ keyFile = "valid_k1" /\ keystoreKey = "present" /\ hadUserData = TRUE)
       \/ (* Corrupted database.key blob *)
          (dbFile = "encrypted_k1" /\ keyFile = "corrupted" /\ keystoreKey = "present" /\ hadUserData = TRUE)
       \/ (* Android Keystore master key wiped after device transfer *)
          (dbFile = "encrypted_k1" /\ keyFile = "valid_k1" /\ keystoreKey = "missing" /\ hadUserData = TRUE)
       \/ (* database.key deleted/missing while encrypted DB remains on disk *)
          (dbFile = "encrypted_k1" /\ keyFile = "none" /\ keystoreKey = "present" /\ hadUserData = TRUE)

(* DatabaseKeyManager.getOrCreatePassphrase -> keyFile.exists() -> unwrap() succeeds *)
UnwrapExistingKey ==
    /\ phase = "init"
    /\ keyFile \in {"valid_k1", "valid_k2"}
    /\ keystoreKey = "present"
    /\ memPassphrase' = IF keyFile = "valid_k1" THEN "k1" ELSE "k2"
    /\ phase' = "key_ready"
    /\ UNCHANGED <<dbFile, legacyFile, keyFile, keystoreKey, quarantined, recoveryNotice, hadUserData>>

(* DatabaseKeyManager.getOrCreatePassphrase -> !keyFile.exists() when no encrypted DB is on disk *)
CreateNewKey ==
    /\ phase = "init"
    /\ keyFile = "none"
    /\ dbFile \in {"none", "plaintext"}
    /\ keyFile' = "valid_k2"
    /\ keystoreKey' = "present"
    /\ memPassphrase' = "k2"
    /\ phase' = "key_ready"
    /\ UNCHANGED <<dbFile, legacyFile, quarantined, recoveryNotice, hadUserData>>

(* AppContainer quarantines encrypted DB when keyFile is missing, corrupted, or Keystore key missing *)
QuarantineAndCreateNewKey ==
    /\ phase = "init"
    /\ \/ (keyFile = "none" /\ dbFile \in {"encrypted_k1", "encrypted_k2"})
       \/ keyFile = "corrupted"
       \/ (keyFile \in {"valid_k1", "valid_k2"} /\ keystoreKey = "missing")
    /\ dbFile' = "none"
    /\ keyFile' = "valid_k2"
    /\ keystoreKey' = "present"
    /\ memPassphrase' = "k2"
    /\ quarantined' = TRUE
    /\ recoveryNotice' = TRUE
    /\ phase' = "key_ready"
    /\ UNCHANGED <<legacyFile, hadUserData>>

(* DatabaseEncryptionMigrator.preserveLegacyFiles: renames dbFile -> dbFile.plaintext *)
PreserveLegacyPlaintext ==
    /\ phase = "key_ready"
    /\ legacyFile = "none"
    /\ dbFile = "plaintext"
    /\ legacyFile' = "plaintext"
    /\ dbFile' = "none"
    /\ phase' = "migrating_preserved"
    /\ UNCHANGED <<keyFile, keystoreKey, memPassphrase, quarantined, recoveryNotice, hadUserData>>

(* DatabaseEncryptionMigrator.exportLegacyPlaintext + importIntoEncryptedDatabase *)
ImportIntoEncryptedDb ==
    /\ (phase = "migrating_preserved" \/ (phase = "key_ready" /\ legacyFile = "plaintext"))
    /\ dbFile' = IF memPassphrase = "k1" THEN "encrypted_k1" ELSE "encrypted_k2"
    /\ phase' = "migrating_imported"
    /\ UNCHANGED <<legacyFile, keyFile, keystoreKey, memPassphrase, quarantined, recoveryNotice, hadUserData>>

(* DatabaseEncryptionMigrator.deleteLegacyFiles -> AppDatabase.getInstance *)
DeleteLegacyAndOpen ==
    /\ phase = "migrating_imported"
    /\ legacyFile' = "none"
    /\ phase' = "ready"
    /\ UNCHANGED <<dbFile, keyFile, keystoreKey, memPassphrase, quarantined, recoveryNotice, hadUserData>>

(* No plaintext migration needed -> AppDatabase.getInstance opens (or creates) encrypted DB *)
OpenEncryptedDatabase ==
    /\ phase = "key_ready"
    /\ legacyFile = "none"
    /\ dbFile /= "plaintext"
    /\ dbFile' = IF dbFile = "none"
                 THEN (IF memPassphrase = "k1" THEN "encrypted_k1" ELSE "encrypted_k2")
                 ELSE dbFile
    /\ phase' = "ready"
    /\ UNCHANGED <<legacyFile, keyFile, keystoreKey, memPassphrase, quarantined, recoveryNotice, hadUserData>>

(* Process killed mid-migration; next app launch restarts at phase="init" *)
CrashDuringMigration ==
    /\ phase \in {"migrating_preserved", "migrating_imported"}
    /\ phase' = "init"
    /\ memPassphrase' = "none"
    /\ UNCHANGED <<dbFile, legacyFile, keyFile, keystoreKey, quarantined, recoveryNotice, hadUserData>>

Done ==
    /\ phase = "ready"
    /\ UNCHANGED vars

Next ==
    \/ UnwrapExistingKey
    \/ CreateNewKey
    \/ QuarantineAndCreateNewKey
    \/ PreserveLegacyPlaintext
    \/ ImportIntoEncryptedDb
    \/ DeleteLegacyAndOpen
    \/ OpenEncryptedDatabase
    \/ CrashDuringMigration
    \/ Done

Spec == Init /\ [][Next]_vars

(* --- Security & Safety Invariants --- *)

(* 1. Once ready, no plaintext database or .plaintext residue remains on disk *)
NoPlaintextLeftAfterReady ==
    (phase = "ready") => (legacyFile = "none" /\ dbFile /= "plaintext")

(* 2. Once ready, the in-memory SQLCipher passphrase MUST match the key that encrypted dbFile *)
KeyMatchesEncryptedDb ==
    (phase = "ready") =>
        ((memPassphrase = "k1" /\ dbFile = "encrypted_k1")
         \/ (memPassphrase = "k2" /\ dbFile = "encrypted_k2"))

(* 3. Any quarantine must set the user-facing recovery notice *)
RecoveryNoticeOnQuarantine ==
    quarantined => recoveryNotice

(* 4. Existing user data is never silently lost without either encrypted DB or quarantine notice *)
NoSilentDataLoss ==
    (phase = "ready" /\ hadUserData) =>
        (dbFile \in {"encrypted_k1", "encrypted_k2"} \/ (quarantined /\ recoveryNotice))

====
