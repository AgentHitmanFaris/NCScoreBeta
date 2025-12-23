## 2025-12-22 - User Enumeration via Error Messages
**Vulnerability:** The `LoginActivity` was leaking specific authentication failures (e.g., "User not found" vs. "Wrong password") via Toast messages. This allows attackers to perform user enumeration and harvest valid email addresses.
**Learning:** Security error messages must be generic ("Invalid email or password") while retaining detailed logging internally for debugging.
**Prevention:** Implemented generic user-facing error messages in `LoginActivity` and added internal logging via `AppLogger`. Added client-side input validation to catch common errors before they reach the server.
