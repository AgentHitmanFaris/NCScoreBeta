## 2024-05-24 - Path Traversal in Internal Activity
**Vulnerability:** The `PdfViewerActivity` accepted a file path via the `PDF_FILE` intent extra and opened it without validation. Although the activity is not exported (preventing external apps from launching it directly), it blindly trusted the input, allowing potential arbitrary file read if an internal component was compromised or if the activity's export status changed.
**Learning:** Even internal components should treat intent extras as untrusted input. "Defense in Depth" requires validating boundaries between components. Relying solely on `android:exported="false"` is fragile.
**Prevention:** Implemented `SecurityUtils.isSafeFilePath(file, rootDir)` to enforce that files opened by `PdfViewerActivity` must reside within the application's external storage directory (`getExternalFilesDir(null)`).

## 2024-05-24 - SSRF via Redirects
**Vulnerability:** `HttpURLConnection` follows redirects by default (or when explicitly enabled) without validating the target URL. An attacker could supply a safe URL (passing initial checks) that redirects to an internal/private IP (SSRF), bypassing DNS rebinding protections.
**Learning:** Initial validation is insufficient if the client follows redirects blindly. "Time of Check Time of Use" (TOCTOU) applies to the entire redirect chain.
**Prevention:** Implemented `SecurityUtils.openSafeConnection` to manually handle redirects, validating `isSafeUrlWithDnsCheck` at every hop before following.
