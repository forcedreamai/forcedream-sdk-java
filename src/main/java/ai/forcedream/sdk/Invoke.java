package ai.forcedream.sdk;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Ported precisely from @forcedream/mcp-server's invoke_agent.ts (via the real, published
 * @forcedream/sdk's invoke.js) -- exact endpoints, exact polling interval ramp (starts
 * 2500ms, +1000ms per attempt, capped at 6000ms), exact status handling. Invokes ONCE; never
 * re-invokes on timeout (would double-charge) -- returns a pollable task_id instead. Not
 * reconstructed from a description -- read directly from the real, working source before
 * writing this.
 */
final class Invoke {
    private Invoke() {}

    static InvokeResult invokeAgentPolling(String apiBase, String apiKey, String agentSlug, String task, Long maxWaitSeconds) {
        String slug = agentSlug;
        long maxWaitMs = Math.max(5, Math.min(120, maxWaitSeconds != null ? maxWaitSeconds : 60)) * 1000L;
        String encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8);

        try {
            Http.Result inv = Http.post(apiBase + "/v1/agents/" + encodedSlug + "/invoke", Map.of("task", task), apiKey);

            if (inv.status() == 401) {
                return new InvokeResult("error", slug, null, null, null, null, "Invalid API key (401).", "invalid_key");
            }

            JsonNode invJson = inv.json();
            if (!invJson.has("task_id") || invJson.get("task_id").isNull()) {
                String errMsg = invJson.has("error") ? invJson.get("error").asText()
                        : invJson.has("note") ? invJson.get("note").asText() : "no task_id";
                return new InvokeResult("error", slug, null, null, null, null,
                        "Invoke failed (HTTP " + inv.status() + "): " + errMsg, "invoke_failed");
            }

            String taskId = invJson.get("task_id").asText();
            long start = System.currentTimeMillis();
            long intervalMs = 2500;

            while (System.currentTimeMillis() - start < maxWaitMs) {
                Thread.sleep(intervalMs);

                String encodedTaskId = URLEncoder.encode(taskId, StandardCharsets.UTF_8);
                Http.Result poll = Http.get(apiBase + "/v1/agents/" + encodedSlug + "/result/" + encodedTaskId, apiKey);
                JsonNode d = poll.json();

                String status = d.has("status") ? d.get("status").asText()
                        : d.has("outcome") ? d.get("outcome").asText() : "";
                boolean okTrue = d.has("ok") && d.get("ok").asBoolean(false);

                if (status.equals("completed") || status.equals("succeeded") || okTrue) {
                    JsonNode output = d.get("output");
                    boolean isInsufficient = (d.has("outcome") && "insufficient".equals(d.get("outcome").asText()))
                            || (output != null && output.has("confidence") && "insufficient".equals(output.get("confidence").asText()));

                    if (isInsufficient) {
                        return new InvokeResult("insufficient", slug, taskId, output, 0L, null,
                                "Agent returned insufficient evidence and declined rather than fabricate. Charged nothing.", null);
                    }

                    Long charged = d.has("charged_pence") ? d.get("charged_pence").asLong() : null;
                    String proofId = d.has("proof_id") ? d.get("proof_id").asText() : taskId;
                    return new InvokeResult("completed", slug, taskId, output, charged, proofId,
                            "Completed. Charged " + (charged != null ? charged : 0) + "p. Cryptographically proven (proof_id " + proofId + ").", null);
                }

                if (status.equals("insufficient")) {
                    return new InvokeResult("insufficient", slug, taskId, d.get("output"), 0L, null,
                            "Agent declined (insufficient evidence). Charged nothing.", null);
                }

                if (status.equals("charge_failed")) {
                    String reason = d.has("reason") ? d.get("reason").asText() : "insufficient_balance";
                    return new InvokeResult("error", slug, taskId, null, 0L, null,
                            "Charge failed: " + reason + ". Nothing charged or delivered. Top up and retry.", "charge_failed");
                }

                if (status.equals("failed") || status.equals("dead_letter")) {
                    String reason = d.has("reason") ? d.get("reason").asText()
                            : d.has("last_error") ? d.get("last_error").asText() : "unknown";
                    return new InvokeResult("error", slug, taskId, null, null, null,
                            "Task " + status + ": " + reason, status);
                }

                intervalMs = Math.min(intervalMs + 1000, 6000);
            }

            return new InvokeResult("pending", slug, taskId, null, null, null,
                    "Still processing after " + (maxWaitMs / 1000) + "s. Not re-invoked (would double-charge). Poll the result later with this task_id.", null);

        } catch (Exception e) {
            return new InvokeResult("error", slug, null, null, null, null, "Invoke request failed: " + e.getMessage(), "request_failed");
        }
    }
}
