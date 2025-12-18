## 2024-12-12 - Committed Binary Key File
**Vulnerability:** Found `NCScoreKey` (binary file) in the repository root.
**Learning:** The project seems to commit potential keystores or private keys directly to the repo. This puts signing keys or encryption keys at risk if the repo is ever shared or compromised.
**Prevention:** Keys should be in `.gitignore`. Use a secure vault or environment variables for CI/CD.

## 2025-05-18 - Exposed File Provider Paths
**Vulnerability:** The `provider_paths.xml` file configures `FileProvider` to share the entire internal and external storage directories.
**Learning:** Using `.` as the path in `FileProvider` exposes more than necessary. While `FileProvider` mechanism itself requires granting permissions, defining broad roots increases the attack surface if a path traversal vulnerability exists elsewhere or if `grantUriPermissions` is misused.
**Prevention:** Specify specific subdirectories (e.g., `scores/`) in `provider_paths.xml` instead of the root `.`.

## 2025-12-12 - SSRF Protection Implementation
**Vulnerability:** Server-Side Request Forgery (SSRF) / Local Network Scanning
**Learning:** Checking for `https` protocol is insufficient for security. `PdfViewerActivity` allows users (or deep links) to load arbitrary URLs. If a malicious actor supplies a URL like `https://192.168.1.1`, the application could be used to scan the user's local network, even if it only returns connection errors (Oracle).
**Prevention:** Implemented strict host validation in `SecurityUtils.isSecureUrl`.
- Blocked Localhost (`localhost`, `127.0.0.1`)
- Blocked Private IP ranges (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`)
- Blocked Link-Local (`169.254.x.x`)
- Note: This is a regex-based block to avoid `NetworkOnMainThreadException` from DNS lookups. It prevents direct IP access but not DNS rebinding or domains pointing to private IPs (which would require async DNS resolution). Ideally, DNS resolution should happen on a background thread before connection.
