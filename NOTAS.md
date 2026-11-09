# NOTAS

## 2026-11-09 — auditoria de segurança v2.5.2
O audit de 2026-11-01 ficou desatualizado: a app tem agora INTERNET, auto-updater com REQUEST_INSTALL_PACKAGES e o repo é público.
Principal risco: o backup JSON completo, sem cifra, é gravado em Downloads a cada atualização e anula o SQLCipher. Seguem-se o bypass do bloqueio biométrico via relógio do sistema e a chave de assinatura exposta a uma action não fixada no release.yml.
Fonte: `SECURITY_AUDIT_2026-11-09.md` (14 achados, prioridades no fim).

## 2026-11-09 — correções da auditoria de segurança
Corrigidos S1–S9 e S11–S13; S10 aceite; S14 parcial (SQLCipher 4.17.0, pois ≥4.18 exige compileSdk 36+).
Gotchas: `FLAG_SECURE` torna as capturas de ecrã pretas em release (usar `uiautomator dump`); o certificado de release está fixado em `UpdateSignatureCheck.RELEASE_CERT_SHA256` e no `release.yml` — trocar a chave obriga a atualizar ambos; os testes androidTest já não compilavam antes destas alterações.
Fonte: `SECURITY_AUDIT_2026-11-09.md` (secção "Estado das correções").

