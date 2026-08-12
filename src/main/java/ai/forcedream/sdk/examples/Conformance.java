package ai.forcedream.sdk.examples;

import ai.forcedream.sdk.ForceDream;
import ai.forcedream.sdk.VerifyResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the shared cross-SDK conformance suite against a local mock server.
 * Start forcedream-sdk-conformance/harness/mock_server.py first.
 */
public final class Conformance {
    public static void main(String[] args) {
        String apiBase = "http://127.0.0.1:8787";

        // Cases come from the server, never a literal here. A hardcoded list is a
        // snapshot that silently drifts: when the contract gained conf_h and conf_i,
        // every hardcoded harness kept running seven cases and reporting green --
        // validating fixes without ever testing them.
        Map<String, Boolean> cases = new java.util.TreeMap<>();
        try {
            java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder()
                                    .uri(java.net.URI.create(apiBase + "/conformance/cases")).build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            JsonNode root = new ObjectMapper().readTree(resp.body());
            root.fields().forEachRemaining(f ->
                    cases.put(f.getKey(), f.getValue().get("expected").asBoolean()));
        } catch (Exception ex) {
            System.err.println("Could not fetch the contract: " + ex.getMessage());
            System.err.println("Start harness/mock_server.py in the conformance repo first.");
            System.exit(2);
        }
        if (cases.isEmpty()) {
            System.err.println("INCONCLUSIVE: the server returned no cases.");
            System.exit(2);
        }

        ForceDream fd = new ForceDream(null, apiBase);
        int passed = 0, failed = 0, errored = 0, verifiedTrue = 0;

        for (Map.Entry<String, Boolean> e : cases.entrySet()) {
            try {
                VerifyResult r = fd.verifyByTaskId(e.getKey());
                if (r.verified()) verifiedTrue++;
                if (r.verified() == e.getValue()) {
                    System.out.printf("  PASS  %-32s verified=%s%n", e.getKey(), r.verified());
                    passed++;
                } else {
                    System.out.printf("  FAIL  %-32s expected=%s got=%s%n",
                            e.getKey(), e.getValue(), r.verified());
                    failed++;
                }
            } catch (Exception ex) {
                System.out.printf("  ERROR %-32s %s: %s%n",
                        e.getKey(), ex.getClass().getSimpleName(), ex.getMessage());
                errored++;
            }
        }

        System.out.printf("%n%d/%d passed, %d failed, %d threw%n",
                passed, cases.size(), failed, errored);
        // Most cases expect false, so an unreachable server or an implementation that
        // rejects everything would otherwise report a green partial pass.
        if (verifiedTrue == 0) {
            System.out.println("INCONCLUSIVE: no case produced a genuine verified=true.");
            System.exit(2);
        }
        if (failed > 0 || errored > 0) System.exit(1);
    }
}
