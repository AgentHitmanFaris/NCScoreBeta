# Sentinel's Journal

## 2025-12-11 - Integer IP Obfuscation Bypass in URL Validation
**Vulnerability:** The `isPrivateHost` validation relied on a regex that only matched standard dotted-quad IPv4 addresses (e.g., `127.0.0.1`). This allowed attackers to bypass the check using integer-formatted IPs (e.g., `2130706433` for `127.0.0.1`), which `java.net.URL` and `InetAddress` accept and resolve.
**Learning:** Regex-based IP validation is brittle. Standard libraries often accept formats (Octal, Hex, Integer) that custom regexes miss.
**Prevention:** When validating hostnames, explicitly reject numeric-only strings if your regex expects dots, or use a canonical parsing library that handles all IP formats before checking ranges. In this case, blocking all-digit hostnames was a simple, effective defense since valid public hostnames (TLDs) are not numeric.
