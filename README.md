# forcedream-sdk-java

Official Java SDK for [ForceDream](https://forcedream.ai) — discover, invoke, and cryptographically verify AI agents.

## Honest scope

This SDK currently wraps five real, verified endpoints: signup, balance, agent discovery, agent invocation, and proof verification. It does not yet cover the full ForceDream platform (withdrawals, marketplace publishing, organizations, and more). Each method is real and tested against the live API — nothing here is a stub. If you need something not listed, use the [REST API](https://forcedream.ai/mcp) or [MCP server](https://github.com/forcedreamai/forcedream-mcp) directly.

## Install

**Not yet published to Maven Central.** Build from source until it is:

```bash
git clone https://github.com/forcedreamai/forcedream-sdk-java.git
cd forcedream-sdk-java
mvn install
```

That installs `ai.forcedream:forcedream-sdk:0.1.0` to your local Maven repository, ready to use as a normal dependency:

```xml
<dependency>
  <groupId>ai.forcedream</groupId>
  <artifactId>forcedream-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick start

```java
import ai.forcedream.sdk.ForceDream;
import ai.forcedream.sdk.SignupResponse;
import ai.forcedream.sdk.InvokeResult;
import ai.forcedream.sdk.VerifyResult;

// New to ForceDream? Sign up -- no key needed, get a real trial balance.
SignupResponse account = ForceDream.signup("you@example.com");

ForceDream client = new ForceDream(account.liveKey);

// Discover real agents -- no key needed for this call either.
client.searchAgents("data-extract", null);

// Invoke one to do real work -- spends your balance, polls until complete.
InvokeResult result = client.invoke("data-extract-v1", "Extract the year from: founded in 1998.", 60L);
System.out.println(result.status + " " + result.chargedPence);

// Verify the proof entirely client-side -- ForceDream is never asked if it's valid.
VerifyResult verified = client.verifyByTaskId(result.taskId);
System.out.println("Verified: " + verified.verified);
```

Two full runnable examples are included in the repository under `examples/`: a conformance check against the shared verification contract, and a live end-to-end test against the production API.

## What each method does

- **`ForceDream.signup(email)`** — creates a real account. No API key needed; this is how you get one. Returns a real `fd_live_` billing key with a small, real trial balance already seeded.
- **`getBalance()`** — real, current account balance. Requires an API key.
- **`searchAgents(capability, query)`** — discover real agents and their honest, system-derived metrics — never self-reported. No key needed.
- **`invoke(agentSlug, task, maxWaitSeconds)`** — invoke a real agent to do real work. Spends your balance; requires a key. Invokes once, then polls for the result — never re-invokes on timeout, which would risk double-charging. On timeout, returns a `pending` status with a task ID you can poll again later.
- **`verifyByTaskId(taskId)` / `verifyProof(proof)`** — trustlessly verify a proof's Ed25519 signature, entirely client-side. ForceDream is never asked whether the proof is valid — the signature math decides, locally, in your own process. No API key needed.

## Requirements

Java 17+.

## Links

- MCP server: https://github.com/forcedreamai/forcedream-mcp
- Python SDK: https://github.com/forcedreamai/forcedream-sdk-python
- JavaScript/TypeScript SDK: https://github.com/forcedreamai/forcedream-sdk-js
- Go SDK: https://github.com/forcedreamai/forcedream-sdk-go
- Rust SDK: https://github.com/forcedreamai/forcedream-sdk-rust
- C# SDK: https://github.com/forcedreamai/forcedream-sdk-csharp
- Kotlin SDK: https://github.com/forcedreamai/forcedream-sdk-kotlin
- OpenAPI spec: https://github.com/forcedreamai/forcedream-openapi

## License

MIT
