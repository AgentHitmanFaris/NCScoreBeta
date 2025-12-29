## 2025-12-11 - Privilege Escalation in Firestore Rules
**Vulnerability:** The Firestore security rules for the `users` collection allowed authenticated users to write any field to their own document (`allow write: if request.auth.uid == userId`). This allowed a malicious user to craft a request setting `isPremiumUser: true` or `isAdmin: true`, granting themselves unauthorized access to premium content or admin capabilities.
**Learning:** Broad `write` permissions are dangerous. Always validate *what* is being written, not just *who* is writing it.
**Prevention:** Use granular `create` and `update` rules. validate that sensitive fields (like roles or subscription status) are either absent from the request or match the existing values (immutable) unless the user has specific administrative privileges.

## 2025-12-11 - Exposed Google Services Configuration
**Vulnerability:** The `google-services.json` file, which contains the project's Firebase API key and configuration, was committed to the Git repository. While the API key itself should be restricted in the Google Cloud Console, exposing this file publicly (or to all repo clones) increases the attack surface and risk of quota theft or project impersonation.
**Learning:** Configuration files often slip into repositories because they are needed for the build but are not standard "secrets" like a private key. Developers often forget to ignore them.
**Prevention:** Always check `.gitignore` before committing new project setup files. Provide a `.json.example` template for required configuration files instead of the actual file.
