## 2024-12-12 - Committed Binary Key File
**Vulnerability:** Found `NCScoreKey` (binary file) in the repository root.
**Learning:** The project seems to commit potential keystores or private keys directly to the repo. This puts signing keys or encryption keys at risk if the repo is ever shared or compromised.
**Prevention:** Keys should be in `.gitignore`. Use a secure vault or environment variables for CI/CD.

## 2025-05-18 - Exposed File Provider Paths
**Vulnerability:** The `provider_paths.xml` file configures `FileProvider` to share the entire internal and external storage directories.
**Learning:** Using `.` as the path in `FileProvider` exposes more than necessary. While `FileProvider` mechanism itself requires granting permissions, defining broad roots increases the attack surface if a path traversal vulnerability exists elsewhere or if `grantUriPermissions` is misused.
**Prevention:** Specify specific subdirectories (e.g., `scores/`) in `provider_paths.xml` instead of the root `.`.
