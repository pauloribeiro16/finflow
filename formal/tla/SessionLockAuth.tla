---- MODULE SessionLockAuth ----
(*
 * Formal TLA+ model of Finflow's Biometric Session Lock & Sensitive Action Auth.
 *
 * Code covered (sync commit eb8dc12):
 *   - app/src/main/kotlin/com/financas/app/MainActivity.kt (onCreate, onPause, onResume)
 *   - app/src/main/kotlin/com/financas/app/core/security/SessionLockPolicy.kt
 *   - app/src/main/kotlin/com/financas/app/ui/screens/account/BiometricLockGate.kt
 *   - app/src/main/kotlin/com/financas/app/ui/screens/account/PrivacyModeRequest.kt
 *)

VARIABLES
    lifecycle,           \* "cold" | "foreground" | "background"
    sessionLocked,       \* BOOLEAN: MainActivity.sessionLocked
    biometricsEnabled,   \* BOOLEAN: BiometricPreferenceStore.isBiometricsEnabled
    deviceSecure,        \* BOOLEAN: KeyguardManager.isDeviceSecure
    timeoutMode,         \* "immediate" (0ms) | "normal" (30s)
    bgElapsed,           \* "none" | "short" (10s) | "long" (60s, >30s and <=120s) | "negative" (<0)
    externalFlowFlag,    \* BOOLEAN: SessionBackgroundClock.externalFlowExpected
    bgWasExternal,       \* BOOLEAN: ghost — was the actual background trip an external flow?
    lastResumeBypassed,  \* BOOLEAN: ghost — normal long/immediate bg resumed unlocked while lock enabled
    clockSkewBypassed    \* BOOLEAN: ghost — negative clock skew resumed unlocked while lock enabled

vars == <<lifecycle, sessionLocked, biometricsEnabled, deviceSecure, timeoutMode, bgElapsed, externalFlowFlag, bgWasExternal, lastResumeBypassed, clockSkewBypassed>>

Init ==
    /\ lifecycle = "cold"
    /\ sessionLocked = FALSE
    /\ biometricsEnabled = TRUE
    /\ deviceSecure \in {TRUE, FALSE}
    /\ timeoutMode \in {"immediate", "normal"}
    /\ bgElapsed = "none"
    /\ externalFlowFlag = FALSE
    /\ bgWasExternal = FALSE
    /\ lastResumeBypassed = FALSE
    /\ clockSkewBypassed = FALSE

(* MainActivity.onCreate: sessionLocked.value = biometricGate.isBiometricsEnabled() *)
ColdStart ==
    /\ lifecycle = "cold"
    /\ sessionLocked' = biometricsEnabled
    /\ lifecycle' = "foreground"
    /\ UNCHANGED <<biometricsEnabled, deviceSecure, timeoutMode, bgElapsed, externalFlowFlag, bgWasExternal, lastResumeBypassed, clockSkewBypassed>>

(* BiometricLockGate.authenticate when isDeviceSecure == TRUE -> user passes biometric/PIN *)
UnlockWithBiometrics ==
    /\ lifecycle = "foreground"
    /\ sessionLocked = TRUE
    /\ deviceSecure = TRUE
    /\ sessionLocked' = FALSE
    /\ UNCHANGED <<lifecycle, biometricsEnabled, deviceSecure, timeoutMode, bgElapsed, externalFlowFlag, bgWasExternal, lastResumeBypassed, clockSkewBypassed>>

(* BiometricLockGate.authenticate when !isDeviceSecure -> fail-closed: calls onError, stays locked *)
UnlockFallbackWhenDeviceNotSecure ==
    /\ lifecycle = "foreground"
    /\ sessionLocked = TRUE
    /\ deviceSecure = FALSE
    /\ UNCHANGED vars

(* PrivacyModeRequest.expectExternalFlow(context) arms 10s timestamp window *)
ArmExternalFlowFlag ==
    /\ lifecycle = "foreground"
    /\ sessionLocked = FALSE
    /\ externalFlowFlag = FALSE
    /\ externalFlowFlag' = TRUE
    /\ UNCHANGED <<lifecycle, sessionLocked, biometricsEnabled, deviceSecure, timeoutMode, bgElapsed, bgWasExternal, lastResumeBypassed, clockSkewBypassed>>

(* MainActivity.onPause during an actual external flow (within EXTERNAL_FLOW_ARM_WINDOW_MS) *)
PauseForExternalFlow ==
    /\ lifecycle = "foreground"
    /\ sessionLocked = FALSE
    /\ externalFlowFlag = TRUE
    /\ lifecycle' = "background"
    /\ bgWasExternal' = TRUE
    /\ bgElapsed' = "none"
    /\ UNCHANGED <<sessionLocked, biometricsEnabled, deviceSecure, timeoutMode, externalFlowFlag, lastResumeBypassed, clockSkewBypassed>>

(* MainActivity.onPause when user leaves app normally (armed TTL expired -> pausedForExternalFlow = FALSE) *)
PauseNormally ==
    /\ lifecycle = "foreground"
    /\ sessionLocked = FALSE
    /\ lifecycle' = "background"
    /\ bgWasExternal' = FALSE
    /\ externalFlowFlag' = FALSE
    /\ bgElapsed' = "none"
    /\ UNCHANGED <<sessionLocked, biometricsEnabled, deviceSecure, timeoutMode, lastResumeBypassed, clockSkewBypassed>>

(* Time elapses while in background *)
BackgroundTimeElapses ==
    /\ lifecycle = "background"
    /\ bgElapsed = "none"
    /\ bgElapsed' \in {"short", "long", "negative"}
    /\ UNCHANGED <<lifecycle, sessionLocked, biometricsEnabled, deviceSecure, timeoutMode, externalFlowFlag, bgWasExternal, lastResumeBypassed, clockSkewBypassed>>

(* SessionLockPolicy.shouldLock exact boolean formula *)
ShouldLockNow ==
    IF ~biometricsEnabled THEN FALSE
    ELSE IF bgElapsed = "negative" THEN TRUE
    ELSE IF externalFlowFlag THEN FALSE  \* "long" is 60s <= 120s EXTERNAL_FLOW_GRACE_MS
    ELSE (timeoutMode = "immediate" \/ bgElapsed = "long")

(* MainActivity.onResume evaluates SessionLockPolicy.shouldLock and clears externalFlowExpected *)
ResumeForeground ==
    /\ lifecycle = "background"
    /\ bgElapsed /= "none"
    /\ LET lockedNow == ShouldLockNow
           shouldHaveLocked == biometricsEnabled /\ ~bgWasExternal /\
                               (bgElapsed = "negative" \/ bgElapsed = "long" \/ timeoutMode = "immediate")
       IN /\ sessionLocked' = lockedNow
          /\ lastResumeBypassed' = (shouldHaveLocked /\ ~lockedNow)
          /\ clockSkewBypassed' = (biometricsEnabled /\ bgElapsed = "negative" /\ ~lockedNow)
    /\ externalFlowFlag' = FALSE
    /\ bgElapsed' = "none"
    /\ bgWasExternal' = FALSE
    /\ lifecycle' = "foreground"
    /\ UNCHANGED <<biometricsEnabled, deviceSecure, timeoutMode>>

Done ==
    /\ lifecycle = "foreground"
    /\ UNCHANGED vars

Next ==
    \/ ColdStart
    \/ UnlockWithBiometrics
    \/ UnlockFallbackWhenDeviceNotSecure
    \/ ArmExternalFlowFlag
    \/ PauseForExternalFlow
    \/ PauseNormally
    \/ BackgroundTimeElapses
    \/ ResumeForeground
    \/ Done

Spec == Init /\ [][Next]_vars

(* --- Security Invariants --- *)

(* 1. Negative clock skew ALWAYS locks when biometricsEnabled = TRUE *)
ClockSkewAlwaysLocks ==
    ~clockSkewBypassed

(* 2. Normal (non-external) background exceeding timeout MUST lock on resume *)
NormalTimeoutAlwaysLocks ==
    ~lastResumeBypassed

(* 3. If biometricsEnabled = TRUE, session cannot be unlocked when device has no screen lock *)
NoFailOpenWhenDeviceUnsecured ==
    (lifecycle = "foreground" /\ biometricsEnabled /\ ~deviceSecure) => sessionLocked

====
