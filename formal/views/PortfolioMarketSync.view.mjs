export const title = "Finflow — Portfolio Market Sync Guards";

export const state = (v) => {
  if (v.phase === "idle") return "Sync Requested";
  if (v.phase === "evaluating") return "Quote Fetched (Validating)";
  if (v.phase === "updated") return "Asset Value Updated";
  if (v.phase === "rejected") return v.httpCalled ? "Rejected (Value Preserved)" : "Skipped (No Network Call)";
  return v.phase;
};

export const step = (action) => {
  const map = {
    SkipSync: "no opt-in or invalid ticker",
    FetchQuote: "fetch via SafeHttp",
    RejectBadQuoteOrFx: "FX fail / anomaly -> keep value",
    PersistUpdatedValue: "all checks OK -> update",
    Done: null,
  };
  return map[action] ?? action;
};
