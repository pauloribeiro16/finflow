export const title = "Finflow — Biometric Session Lock";

export const state = (v) => {
  if (v.lifecycle === "cold") return "App Cold Start";
  if (v.lifecycle === "background") {
    if (v.bgWasExternal) return "Background (External Flow)";
    return v.externalFlowFlag ? "Background (Normal + Stale Flag)" : "Background (Normal)";
  }
  if (v.sessionLocked) return "Foreground: Locked";
  if (v.lastResumeBypassed) return "Foreground: Unlocked (Timeout Bypassed)";
  if (v.biometricsEnabled && !v.deviceSecure) return "Foreground: Unlocked (No Device PIN)";
  return v.externalFlowFlag ? "Foreground: Unlocked (External Armed)" : "Foreground: Unlocked";
};

export const step = (action, from, to) => {
  if (action === "Done") return null;
  if (action === "ColdStart") return "launch app";
  if (action === "UnlockWithBiometrics") return "unlock with biometric/PIN";
  if (action === "UnlockFallbackWhenDeviceNotSecure") return "bypass lock (no device PIN)";
  if (action === "ArmExternalFlowFlag") return "arm external-flow grace";
  if (action === "PauseForExternalFlow") return "open external picker";
  if (action === "PauseNormally") return "leave app normally";
  if (action === "BackgroundTimeElapses") return `${to.bgElapsed} time passes`;
  if (action === "ResumeForeground") return `resume (${from.bgElapsed})`;
  return action;
};
