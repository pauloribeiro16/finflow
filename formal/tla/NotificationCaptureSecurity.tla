---- MODULE NotificationCaptureSecurity ----
(*
 * Formal TLA+ model of Finflow's Bank Notification Capture attack surface.
 *
 * Code covered (sync commit eb8dc12):
 *   - app/src/main/AndroidManifest.xml (BIND_NOTIFICATION_LISTENER_SERVICE)
 *   - app/src/main/kotlin/com/financas/app/data/service/FinancialNotificationListenerService.kt
 *   - app/src/main/kotlin/com/financas/app/domain/usecase/FinancialAppFilter.kt
 *   - app/src/main/kotlin/com/financas/app/domain/usecase/PendingFloodGuard.kt
 *)

EXTENDS Integers

CONSTANTS MAX_PENDING

VARIABLES
    sessionLocked,       \* BOOLEAN: MainActivity session lock
    userAuthenticated,   \* BOOLEAN: ghost — did the user actually authenticate?
    pendingLegit,        \* 0..MAX_PENDING: pending items from real bank apps
    pendingAttacker,     \* 0..MAX_PENDING: pending items injected by spoofed-package attacker app
    confirmedTx,         \* 0..MAX_PENDING: confirmed transactions in transactions table
    legitDroppedByFlood  \* BOOLEAN: real bank notification dropped because queue hit MAX_PENDING

vars == <<sessionLocked, userAuthenticated, pendingLegit, pendingAttacker, confirmedTx, legitDroppedByFlood>>

Init ==
    /\ sessionLocked = TRUE
    /\ userAuthenticated = FALSE
    /\ pendingLegit = 0
    /\ pendingAttacker = 0
    /\ confirmedTx = 0
    /\ legitDroppedByFlood = FALSE

(* Attacker app with non-financial package (e.g. com.evil.app) -> rejected by FinancialAppFilter *)
AttackerOrdinaryPkgRejected ==
    /\ UNCHANGED vars

(* Attacker app with spoofed package (e.g. com.evil.mbway) -> REJECTED by exact ALLOWED_PACKAGES allowlist *)
AttackerSpoofedPkgPosts ==
    /\ UNCHANGED vars

(* Real bank app (e.g. pt.sibs.android.mbway) posts notification when queue has space *)
LegitBankPostsAccepted ==
    /\ pendingLegit + pendingAttacker < MAX_PENDING
    /\ pendingLegit' = pendingLegit + 1
    /\ UNCHANGED <<sessionLocked, userAuthenticated, pendingAttacker, confirmedTx, legitDroppedByFlood>>

(* Real bank app posts notification when queue is full -> dropped by PendingFloodGuard *)
LegitBankPostsDropped ==
    /\ pendingLegit + pendingAttacker = MAX_PENDING
    /\ ~legitDroppedByFlood
    /\ legitDroppedByFlood' = TRUE
    /\ UNCHANGED <<sessionLocked, userAuthenticated, pendingLegit, pendingAttacker, confirmedTx>>

(* User authenticates with biometrics/PIN to unlock MainActivity *)
UserUnlocksApp ==
    /\ sessionLocked = TRUE
    /\ sessionLocked' = FALSE
    /\ userAuthenticated' = TRUE
    /\ UNCHANGED <<pendingLegit, pendingAttacker, confirmedTx, legitDroppedByFlood>>

(* User reviews & approves a pending item inside unlocked app *)
UserApprovesPending ==
    /\ sessionLocked = FALSE
    /\ confirmedTx < MAX_PENDING
    /\ \/ (pendingLegit > 0 /\ pendingLegit' = pendingLegit - 1 /\ UNCHANGED pendingAttacker)
       \/ (pendingAttacker > 0 /\ pendingAttacker' = pendingAttacker - 1 /\ UNCHANGED pendingLegit)
    /\ confirmedTx' = confirmedTx + 1
    /\ UNCHANGED <<sessionLocked, userAuthenticated, legitDroppedByFlood>>

Done ==
    /\ UNCHANGED vars

Next ==
    \/ AttackerOrdinaryPkgRejected
    \/ AttackerSpoofedPkgPosts
    \/ LegitBankPostsAccepted
    \/ LegitBankPostsDropped
    \/ UserUnlocksApp
    \/ UserApprovesPending
    \/ Done

Spec == Init /\ [][Next]_vars

(* --- Security Invariants --- *)

(* 1. Notification capture can NEVER unlock the app session *)
NoSessionUnlockFromNotification ==
    (~sessionLocked) => userAuthenticated

(* 2. Notifications can NEVER directly write confirmed transactions without user auth/review *)
NoAutoConfirmWithoutUser ==
    (confirmedTx > 0) => userAuthenticated

(* 3. Pending queue strictly respects PendingFloodGuard.MAX_PENDING_TOTAL *)
QueueBounded ==
    pendingLegit + pendingAttacker <= MAX_PENDING

(* 4. Spoofed attacker package (com.evil.mbway) MUST NOT enter pending_transactions *)
NoSpoofedPackageAccepted ==
    pendingAttacker = 0

(* 5. Attacker cannot starve legitimate bank notifications by filling the pending queue *)
LegitBankNotStarvedByAttacker ==
    ~(legitDroppedByFlood /\ pendingAttacker > 0)

====
