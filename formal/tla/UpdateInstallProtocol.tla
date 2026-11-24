---- MODULE UpdateInstallProtocol ----
(*
 * Formal TLA+ model of Finflow's OTA APK Update Download & Signature Verification.
 *
 * Code covered (sync commit eb8dc12):
 *   - app/src/main/kotlin/com/financas/app/data/updater/AppUpdateManager.kt
 *   - app/src/main/kotlin/com/financas/app/domain/usecase/UpdateSignatureCheck.kt
 *)

EXTENDS Integers

CONSTANTS MAX_REDIRECTS

VARIABLES
    state,          \* "idle" | "following_redirects" | "ready_to_install" | "installed" | "error"
    urlAllowed,     \* BOOLEAN: UpdateUrlPolicy.isAllowedReleaseUrl
    redirects,      \* 0..MAX_REDIRECTS+1
    apkOnDisk,      \* BOOLEAN
    samePackage,    \* BOOLEAN: candidatePackage == installedPackage
    higherVersion,  \* BOOLEAN: candidateVersionCode > installedVersionCode
    officialCert,   \* BOOLEAN: UpdateSignatureCheck.isOfficialRelease
    compatibleCert, \* BOOLEAN: UpdateSignatureCheck.isCompatible
    contactedBadHost \* BOOLEAN: ghost — did we ever connect to an unallowed host?

vars == <<state, urlAllowed, redirects, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

Init ==
    /\ state = "idle"
    /\ urlAllowed \in {TRUE, FALSE}
    /\ redirects = 0
    /\ apkOnDisk = FALSE
    /\ samePackage \in {TRUE, FALSE}
    /\ higherVersion \in {TRUE, FALSE}
    /\ officialCert \in {TRUE, FALSE}
    /\ compatibleCert \in {TRUE, FALSE}
    /\ contactedBadHost = FALSE

(* AppUpdateManager.downloadUpdate checks UpdateUrlPolicy.isAllowedReleaseUrl first *)
RejectInitialUrl ==
    /\ state = "idle"
    /\ ~urlAllowed
    /\ state' = "error"
    /\ UNCHANGED <<urlAllowed, redirects, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

StartAllowedDownload ==
    /\ state = "idle"
    /\ urlAllowed
    /\ state' = "following_redirects"
    /\ UNCHANGED <<urlAllowed, redirects, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

(* openFollowingAllowedRedirects: allowed redirect within MAX_REDIRECTS *)
FollowAllowedRedirect ==
    /\ state = "following_redirects"
    /\ redirects < MAX_REDIRECTS
    /\ redirects' = redirects + 1
    /\ UNCHANGED <<state, urlAllowed, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

(* openFollowingAllowedRedirects: disallowed redirect host -> throws before connecting *)
RejectDisallowedRedirect ==
    /\ state = "following_redirects"
    /\ state' = "error"
    /\ UNCHANGED <<urlAllowed, redirects, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

(* openFollowingAllowedRedirects: too many redirects -> throws *)
RejectTooManyRedirects ==
    /\ state = "following_redirects"
    /\ redirects = MAX_REDIRECTS
    /\ state' = "error"
    /\ UNCHANGED <<urlAllowed, redirects, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

(* Download completes and writes APK to cacheDir/updates *)
FinishDownload ==
    /\ state = "following_redirects"
    /\ apkOnDisk' = TRUE
    /\ state' = "ready_to_install"
    /\ UNCHANGED <<urlAllowed, redirects, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

(* AppUpdateManager.installApk -> verifyCandidate returns OK -> launches installIntent *)
VerifyAndInstallOk ==
    /\ state = "ready_to_install"
    /\ apkOnDisk = TRUE
    /\ samePackage /\ higherVersion /\ officialCert /\ compatibleCert
    /\ state' = "installed"
    /\ UNCHANGED <<urlAllowed, redirects, apkOnDisk, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

(* AppUpdateManager.installApk -> verifyCandidate fails -> apkFile.delete() + Error *)
VerifyFailedAndDelete ==
    /\ state = "ready_to_install"
    /\ apkOnDisk = TRUE
    /\ ~(samePackage /\ higherVersion /\ officialCert /\ compatibleCert)
    /\ apkOnDisk' = FALSE
    /\ state' = "error"
    /\ UNCHANGED <<urlAllowed, redirects, samePackage, higherVersion, officialCert, compatibleCert, contactedBadHost>>

Done ==
    /\ state \in {"installed", "error"}
    /\ UNCHANGED vars

Next ==
    \/ RejectInitialUrl
    \/ StartAllowedDownload
    \/ FollowAllowedRedirect
    \/ RejectDisallowedRedirect
    \/ RejectTooManyRedirects
    \/ FinishDownload
    \/ VerifyAndInstallOk
    \/ VerifyFailedAndDelete
    \/ Done

Spec == Init /\ [][Next]_vars

(* --- Security Invariants --- *)

OnlyAllowlistedNetworkTargets ==
    ~contactedBadHost

NoUntrustedOrDowngradeInstall ==
    (state = "installed") =>
        (urlAllowed /\ samePackage /\ higherVersion /\ officialCert /\ compatibleCert)

RejectedApkDeleted ==
    (state = "error") => ~apkOnDisk

RedirectLimitRespected ==
    redirects <= MAX_REDIRECTS

====
