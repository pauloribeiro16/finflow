---- MODULE PortfolioMarketSync ----
(*
 * Formal TLA+ model of Finflow's Yahoo Finance Market Sync & Integrity Guards.
 *
 * Code covered:
 *   - app/src/main/kotlin/com/financas/app/domain/usecase/PortfolioMarketSyncUseCase.kt
 *   - app/src/main/kotlin/com/financas/app/data/network/YahooFinanceQuoteService.kt
 *   - app/src/main/kotlin/com/financas/app/data/network/SafeHttp.kt
 *)

VARIABLES
    phase,          \* "idle" | "fetching" | "evaluating" | "updated" | "rejected"
    userOptIn,      \* BOOLEAN: auto-sync enabled or explicit manual tap
    tickerValid,    \* BOOLEAN: TickerPolicy.normalize(ticker) != null
    quoteFinite,    \* BOOLEAN: quote.price.isFinite() && quote.price > 0
    fxOk,           \* BOOLEAN: fetchEurRate(currency) != null
    jumpPlausible,  \* BOOLEAN: !isImplausibleJump (within [1/3x, 3x])
    httpCalled,     \* BOOLEAN: ghost — was SafeHttp called?
    valueOverwritten \* BOOLEAN: ghost — was asset.currentValueCents updated?

vars == <<phase, userOptIn, tickerValid, quoteFinite, fxOk, jumpPlausible, httpCalled, valueOverwritten>>

Init ==
    /\ phase = "idle"
    /\ userOptIn \in {TRUE, FALSE}
    /\ tickerValid \in {TRUE, FALSE}
    /\ quoteFinite \in {TRUE, FALSE}
    /\ fxOk \in {TRUE, FALSE}
    /\ jumpPlausible \in {TRUE, FALSE}
    /\ httpCalled = FALSE
    /\ valueOverwritten = FALSE

(* No network call if user did not opt in or ticker fails TickerPolicy *)
SkipSync ==
    /\ phase = "idle"
    /\ (~userOptIn \/ ~tickerValid)
    /\ phase' = "rejected"
    /\ UNCHANGED <<userOptIn, tickerValid, quoteFinite, fxOk, jumpPlausible, httpCalled, valueOverwritten>>

(* SafeHttp fetches quote for valid ticker with explicit opt-in *)
FetchQuote ==
    /\ phase = "idle"
    /\ userOptIn /\ tickerValid
    /\ httpCalled' = TRUE
    /\ phase' = "evaluating"
    /\ UNCHANGED <<userOptIn, tickerValid, quoteFinite, fxOk, jumpPlausible, valueOverwritten>>

(* Quote invalid, FX failed, or >3x / <1/3x jump -> do NOT overwrite asset value *)
RejectBadQuoteOrFx ==
    /\ phase = "evaluating"
    /\ (~quoteFinite \/ ~fxOk \/ ~jumpPlausible)
    /\ phase' = "rejected"
    /\ UNCHANGED <<userOptIn, tickerValid, quoteFinite, fxOk, jumpPlausible, httpCalled, valueOverwritten>>

(* All checks pass -> persist updated currentValueCents *)
PersistUpdatedValue ==
    /\ phase = "evaluating"
    /\ quoteFinite /\ fxOk /\ jumpPlausible
    /\ valueOverwritten' = TRUE
    /\ phase' = "updated"
    /\ UNCHANGED <<userOptIn, tickerValid, quoteFinite, fxOk, jumpPlausible, httpCalled>>

Done ==
    /\ phase \in {"updated", "rejected"}
    /\ UNCHANGED vars

Next ==
    \/ SkipSync
    \/ FetchQuote
    \/ RejectBadQuoteOrFx
    \/ PersistUpdatedValue
    \/ Done

Spec == Init /\ [][Next]_vars

(* --- Security & Data-Integrity Invariants --- *)

NoNetworkWithoutOptInAndValidTicker ==
    httpCalled => (userOptIn /\ tickerValid)

NoCorruptedValueOnBadQuoteFxOrAnomaly ==
    valueOverwritten => (userOptIn /\ tickerValid /\ quoteFinite /\ fxOk /\ jumpPlausible)

====
