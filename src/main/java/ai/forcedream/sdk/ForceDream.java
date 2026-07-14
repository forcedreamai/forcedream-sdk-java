package ai.forcedream.sdk;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * A real, honestly-scoped client for the ForceDream API. Wraps only endpoints verified
 * working directly against the live, production API -- not the full platform surface.
 * See the README for exactly what is and isn't covered yet.
 */
public final class ForceDream {
    private final String apiKey;
    private final String apiBase;

    public ForceDream(String apiKey) {
        this(apiKey, "https://api.forcedream.ai");
    }

    public ForceDream(String apiKey, String apiBase) {
        this.apiKey = apiKey;
        this.apiBase = apiBase;
    }

    /** Create a new ForceDream account. No API key needed -- this is how you get one.
     * Returns a real fd_live_ billing key with a small, real trial balance already seeded. */
    public static SignupResponse signup(String email) throws Exception {
        return signup(email, "https://api.forcedream.ai");
    }

    public static SignupResponse signup(String email, String apiBase) throws Exception {
        Http.Result res = Http.post(apiBase + "/api/signup", Map.of("email", email), null);
        return Http.MAPPER.treeToValue(res.json(), SignupResponse.class);
    }

    /** Real, current account balance. Requires an API key. */
    public JsonNode getBalance() throws Exception {
        if (apiKey == null) throw new IllegalStateException("getBalance() requires an apiKey");
        return Http.get(apiBase + "/v1/account/balance", apiKey).json();
    }

    /** Discover real ForceDream agents and their honest, system-derived metrics. No key
     * needed -- every field here is computed from real proofs and ledger entries, never
     * self-reported. Filtering happens client-side (the server has no working server-side
     * filter for this). */
    public JsonNode searchAgents(String capability, String query) throws Exception {
        return Agents.searchAgentsFiltered(apiBase, capability, query);
    }

    /** Invoke a real ForceDream agent to do real work. Spends your balance -- requires an
     * API key. Invokes once, then polls (bounded by maxWaitSeconds) for the result -- never
     * re-invokes on timeout, which would double-charge. On timeout, returns status:
     * "pending" with a task_id you can poll again later. Honest declines and failed charges
     * cost nothing. */
    public InvokeResult invoke(String agentSlug, String task, Long maxWaitSeconds) {
        if (apiKey == null) throw new IllegalStateException("invoke() requires an apiKey (it spends your balance)");
        return Invoke.invokeAgentPolling(apiBase, apiKey, agentSlug, task, maxWaitSeconds);
    }

    /** Trustlessly verify a proof's Ed25519 signature, entirely client-side. ForceDream is
     * never asked whether the proof is valid -- the signature math decides, locally, in
     * your own process. No API key needed. */
    public VerifyResult verifyByTaskId(String taskId) throws Exception {
        return Verify.verifyProof(apiBase, taskId, null);
    }

    public VerifyResult verifyProof(JsonNode proof) throws Exception {
        return Verify.verifyProof(apiBase, null, proof);
    }
}
