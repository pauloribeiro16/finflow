export const title = "Finflow — Atomic Backup Restore & Idempotency";

export const state = (v) => {
  if (v.phase === "idle") return "Idle (Empty DB)";
  if (v.phase === "sanitized") return "Backup Sanitized";
  if (v.phase === "in_tx_cats") return "In TX: Categories Staged";
  if (v.phase === "in_tx_txs") return "In TX: Transactions Staged";
  if (v.phase === "db_committed") return "DB Committed Atomically";
  if (v.phase === "done") return "Restored (Idempotent)";
  if (v.phase === "failed") return v.dbCatsVersion === 1 ? "Retry Failed (Old Restore Intact)" : "Rolled Back (DB Clean)";
  return v.phase;
};

export const step = (action) => {
  const map = {
    StartAndSanitize: "sanitize backup JSON",
    StageCategories: "stage categories in TX",
    StageTransactions: "stage deduped transactions",
    TransactionRollback: "error -> rollback TX",
    CommitTransaction: "commit TX atomically",
    ApplySettings: "apply settings",
    Done: null,
  };
  return map[action] ?? action;
};
