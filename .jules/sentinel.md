## 2024-12-12 - Committed Binary Key File
**Vulnerability:** Found `NCScoreKey` (binary file) in the repository root.
**Learning:** The project seems to commit potential keystores or private keys directly to the repo. This puts signing keys or encryption keys at risk if the repo is ever shared or compromised.
**Prevention:** Keys should be in `.gitignore`. Use a secure vault or environment variables for CI/CD.
