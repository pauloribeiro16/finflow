---- MODULE BackupAtomicRestore ----
(*
 * Formal TLA+ model of Finflow's Atomic Backup Restore & Idempotency.
 *
 * Code covered:
 *   - app/src/main/kotlin/com/financas/app/ui/screens/account/AccountViewModel.kt (importJson)
 *   - app/src/main/kotlin/com/financas/app/data/repository/BackupSanitizer.kt
 *   - app/src/main/kotlin/com/financas/app/data/repository/BackupSerializer.kt (restoreBackup)
 *   - app/src/main/kotlin/com/financas/app/data/repository/SettingsBackupManager.kt
 *)

EXTENDS Integers

VARIABLES
    phase,             \* "idle" | "sanitized" | "in_tx_cats" | "in_tx_txs" | "db_committed" | "done" | "failed"
    dbCatsVersion,     \* 0 (old) | 1 (restored)
    dbTxsCount,        \* 0 (old) | 1 (restored, deduplicated)
    stagedCatsVersion, \* 0 | 1 (uncommitted inside Room withTransaction)
    stagedTxsCount,    \* 0 | 1 (uncommitted inside Room withTransaction)
    settingsApplied,   \* BOOLEAN
    restoreAttempts    \* 0..2 (checks repeated idempotent restores)

vars == <<phase, dbCatsVersion, dbTxsCount, stagedCatsVersion, stagedTxsCount, settingsApplied, restoreAttempts>>

Init ==
    /\ phase = "idle"
    /\ dbCatsVersion = 0
    /\ dbTxsCount = 0
    /\ stagedCatsVersion = 0
    /\ stagedTxsCount = 0
    /\ settingsApplied = FALSE
    /\ restoreAttempts = 0

(* BackupSanitizer.sanitize succeeds *)
StartAndSanitize ==
    /\ phase \in {"idle", "done", "failed"}
    /\ restoreAttempts < 2
    /\ phase' = "sanitized"
    /\ stagedCatsVersion' = dbCatsVersion
    /\ stagedTxsCount' = dbTxsCount
    /\ restoreAttempts' = restoreAttempts + 1
    /\ UNCHANGED <<dbCatsVersion, dbTxsCount, settingsApplied>>

(* Inside runInTransaction: insert categories *)
StageCategories ==
    /\ phase = "sanitized"
    /\ stagedCatsVersion' = 1
    /\ phase' = "in_tx_cats"
    /\ UNCHANGED <<dbCatsVersion, dbTxsCount, stagedTxsCount, settingsApplied, restoreAttempts>>

(* Inside runInTransaction: insert deduplicated transactions (idempotent: max 1) *)
StageTransactions ==
    /\ phase = "in_tx_cats"
    /\ stagedTxsCount' = 1
    /\ phase' = "in_tx_txs"
    /\ UNCHANGED <<dbCatsVersion, dbTxsCount, stagedCatsVersion, settingsApplied, restoreAttempts>>

(* Exception mid-transaction -> Room rolls back staged writes; DB unchanged *)
TransactionRollback ==
    /\ phase \in {"in_tx_cats", "in_tx_txs"}
    /\ stagedCatsVersion' = dbCatsVersion
    /\ stagedTxsCount' = dbTxsCount
    /\ phase' = "failed"
    /\ UNCHANGED <<dbCatsVersion, dbTxsCount, settingsApplied, restoreAttempts>>

(* Room commits transaction atomically *)
CommitTransaction ==
    /\ phase = "in_tx_txs"
    /\ dbCatsVersion' = stagedCatsVersion
    /\ dbTxsCount' = stagedTxsCount
    /\ phase' = "db_committed"
    /\ UNCHANGED <<stagedCatsVersion, stagedTxsCount, settingsApplied, restoreAttempts>>

(* SettingsBackupManager.apply runs ONLY after DB transaction commits *)
ApplySettings ==
    /\ phase = "db_committed"
    /\ settingsApplied' = TRUE
    /\ phase' = "done"
    /\ UNCHANGED <<dbCatsVersion, dbTxsCount, stagedCatsVersion, stagedTxsCount, restoreAttempts>>

Done ==
    /\ phase \in {"done", "failed"}
    /\ restoreAttempts = 2
    /\ UNCHANGED vars

Next ==
    \/ StartAndSanitize
    \/ StageCategories
    \/ StageTransactions
    \/ TransactionRollback
    \/ CommitTransaction
    \/ ApplySettings
    \/ Done

Spec == Init /\ [][Next]_vars

(* --- Safety & Atomicity Invariants --- *)

(* 1. Committed DB is never left half-restored (categories updated without transactions or vice versa) *)
NoPartialDbState ==
    dbCatsVersion = dbTxsCount

(* 2. Settings are never applied if DB has not been committed *)
NoSettingsWithoutDbCommit ==
    settingsApplied => (dbCatsVersion = 1 /\ dbTxsCount = 1)

(* 3. Repeated restores never duplicate transactions *)
IdempotentTransactionCount ==
    dbTxsCount <= 1

====
