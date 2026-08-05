package ai.forcedream.sdk.examples;

import ai.forcedream.sdk.ForceDream;
import ai.forcedream.sdk.VerifyResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the shared cross-SDK conformance suite against a local mock server.
 * Start forcedream-sdk-conformance/harness/mock_server.py first.
 */
public final class Conformance {
    public static void main(String[] args) {
        String apiBase = "http://127.0.0.1:8787";

        Map<String, Boolean> cases = new LinkedHashMap<>();
        cases.put("conf_a_real_batched", true);
        cases.put("conf_b_real_batched", true);
        cases.put("conf_c_bad_signature", false);
        cases.put("conf_d_bad_payload", false);
        cases.put("conf_e_bad_algorithm", false);
        cases.put("conf_f_siblings_wrong_root", false);
        cases.put("conf_g_missing_root", false);

        ForceDream fd = new ForceDream(null, apiBase);
        int passed = 0, failed = 0, errored = 0;

        for (Map.Entry<String, Boolean> e : cases.entrySet()) {
            try {
                VerifyResult r = fd.verifyByTaskId(e.getKey());
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
        if (failed > 0 || errored > 0) System.exit(1);
    }
}
