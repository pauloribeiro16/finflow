export const title = "Finflow — Notification Capture Attack Surface";

export const state = (v) => {
  if (v.legitDroppedByFlood) return "Queue Full: Real Bank Dropped";
  if (v.confirmedTx > 0) return "User Approved Transaction";
  if (!v.sessionLocked) return "App Unlocked by User";
  if (v.pendingAttacker > 0) return `Locked + ${v.pendingAttacker} Spoofed Pending`;
  if (v.pendingLegit > 0) return "Locked + Legit Pending";
  return "Locked (Empty Queue)";
};

export const step = (action) => {
  const map = {
    AttackerOrdinaryPkgRejected: null,
    AttackerSpoofedPkgPosts: "com.evil.mbway posts fake notif",
    LegitBankPostsAccepted: "real bank posts notif",
    LegitBankPostsDropped: "real bank notif dropped (full)",
    UserUnlocksApp: "user unlocks with biometrics",
    UserApprovesPending: "user approves pending",
    Done: null,
  };
  return map[action] ?? action;
};
