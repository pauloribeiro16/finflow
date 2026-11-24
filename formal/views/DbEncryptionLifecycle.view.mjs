export const title = "Finflow — Database Encryption & Key Lifecycle";

export const state = (v) => {
  if (v.phase === "init") {
    if (v.dbFile === "plaintext") return "Boot: Plaintext DB";
    if (v.dbFile === "none") return "Boot: Fresh Install";
    if (v.keyFile === "none") return "Boot: Encrypted DB, Key Missing";
    if (v.keyFile === "corrupted" || v.keystoreKey === "missing") return "Boot: Unrecoverable Key";
    return "Boot: Encrypted DB OK";
  }
  if (v.phase === "key_ready") {
    if (v.dbFile === "encrypted_k1" && v.memPassphrase === "k2") return "Key Mismatch Ready";
    if (v.quarantined) return "Quarantined + New Key";
    return "Key Ready";
  }
  if (v.phase === "migrating_preserved") return "Migrating: Legacy Preserved";
  if (v.phase === "migrating_imported") return "Migrating: Encrypted Imported";
  if (v.phase === "ready") {
    if (v.dbFile === "encrypted_k1" && v.memPassphrase === "k2") return "Ready (Key Mismatch Crash)";
    if (v.quarantined) return "Ready (Empty + Recovery Notice)";
    return "Ready (Encrypted DB Open)";
  }
  return v.phase;
};

export const step = (action) => {
  const map = {
    UnwrapExistingKey: "unwrap existing key",
    CreateNewKey: "generate new key",
    QuarantineAndCreateNewKey: "quarantine DB & create key",
    PreserveLegacyPlaintext: "rename DB to .plaintext",
    ImportIntoEncryptedDb: "import into SQLCipher DB",
    DeleteLegacyAndOpen: "delete .plaintext & open",
    OpenEncryptedDatabase: "open encrypted DB",
    CrashDuringMigration: "process crashes",
    Done: null,
  };
  return map[action] ?? action;
};
