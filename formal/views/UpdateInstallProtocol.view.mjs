export const title = "Finflow — OTA APK Update Verification";

export const state = (v) => {
  if (v.state === "idle") return "Update Requested";
  if (v.state === "following_redirects") return v.redirects === 0 ? "Downloading (Direct)" : "Downloading (Redirected)";
  if (v.state === "ready_to_install") return "APK Downloaded (Verifying)";
  if (v.state === "installed") return "Verified & Install Intent Sent";
  if (v.state === "error") return "Blocked & APK Deleted";
  return v.state;
};

export const step = (action) => {
  const map = {
    RejectInitialUrl: "URL rejected",
    StartAllowedDownload: "start HTTPS download",
    FollowAllowedRedirect: "follow allowed redirect",
    RejectDisallowedRedirect: "disallowed redirect",
    RejectTooManyRedirects: "redirect limit exceeded",
    FinishDownload: "download completes",
    VerifyAndInstallOk: "sig + upgrade OK",
    VerifyFailedAndDelete: "sig/upgrade failed -> delete APK",
    Done: null,
  };
  return map[action] ?? action;
};
