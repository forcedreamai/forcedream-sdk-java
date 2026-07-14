package ai.forcedream.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported precisely from @forcedream/mcp-server's search_agents.ts (via the real, published
 * @forcedream/sdk's agents.js). Real, load-bearing fact confirmed directly from that source,
 * not assumed: the server has no working server-side capability/query filter on
 * /v1/agents/list -- filtering must happen client-side, after fetching the full list. Also
 * merges in real reliability data from the separate /v1/agents/reliability endpoint, exactly
 * as the proven implementation does.
 */
final class Agents {
    private Agents() {}

    static JsonNode searchAgentsFiltered(String apiBase, String capability, String query) throws Exception {
        JsonNode data = Http.get(apiBase + "/v1/agents/list", null).json();
        JsonNode relData;
        try {
            relData = Http.get(apiBase + "/v1/agents/reliability", null).json();
        } catch (Exception e) {
            relData = null;
        }

        ArrayNode agents = Http.MAPPER.createArrayNode();
        if (data.has("agents") && data.get("agents").isArray()) {
            for (JsonNode a : data.get("agents")) agents.add(a);
        }

        Map<String, JsonNode> reliabilityBySlug = new HashMap<>();
        if (relData != null && relData.has("agents") && relData.get("agents").isArray()) {
            for (JsonNode ra : relData.get("agents")) {
                if (ra.has("agent_slug") && ra.has("reliability")) {
                    reliabilityBySlug.put(ra.get("agent_slug").asText(), ra.get("reliability"));
                }
            }
        }

        ArrayNode filtered = Http.MAPPER.createArrayNode();
        for (JsonNode a : agents) {
            boolean capMatch = capability == null || matchesCapability(a, capability);
            boolean queryMatch = query == null || matchesQuery(a, query);
            if (capMatch && queryMatch) filtered.add(a);
        }

        ArrayNode enriched = Http.MAPPER.createArrayNode();
        for (JsonNode a : filtered) {
            ObjectNode obj = a.deepCopy();
            String slug = obj.has("slug") ? obj.get("slug").asText() : null;
            JsonNode health = slug != null ? reliabilityBySlug.get(slug) : null;
            obj.set("health", health != null ? health : Http.MAPPER.nullNode());
            enriched.add(obj);
        }

        ObjectNode result = Http.MAPPER.createObjectNode();
        result.put("count", enriched.size());
        result.set("agents", enriched);
        result.put("note", enriched.isEmpty()
                ? "No agents matched. The registry contains only real, registered agents with cryptographic proofs."
                : "Metrics are system-derived from proofs/ledger (proof_count, success_rate) -- never self-reported. Health (success_rate, avg_latency_ms, sample_size) is honestly null where no real reliability data exists yet.");
        return result;
    }

    private static boolean matchesCapability(JsonNode a, String capability) {
        String capLower = capability.toLowerCase();
        if (!a.has("capabilities") || !a.get("capabilities").isArray()) return false;
        for (JsonNode c : a.get("capabilities")) {
            if (c.asText().toLowerCase().equals(capLower)) return true;
        }
        return false;
    }

    private static boolean matchesQuery(JsonNode a, String query) {
        String qLower = query.toLowerCase();
        String slug = (a.has("slug") && !a.get("slug").isNull()) ? a.get("slug").asText() : "";
        String name = (a.has("name") && !a.get("name").isNull()) ? a.get("name").asText() : "";
        if (slug.toLowerCase().contains(qLower) || name.toLowerCase().contains(qLower)) return true;
        if (a.has("capabilities") && a.get("capabilities").isArray()) {
            for (JsonNode c : a.get("capabilities")) {
                if (c.asText().toLowerCase().contains(qLower)) return true;
            }
        }
        return false;
    }
}
