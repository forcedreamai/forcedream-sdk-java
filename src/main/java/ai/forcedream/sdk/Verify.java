package ai.forcedream.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Trustlessly verifies a ForceDream proof's Ed25519 signature entirely client-side.
 * ForceDream is never asked whether the proof is valid -- the math decides, locally.
 * Ported field-for-field from the real, published @forcedream/sdk's verify.js buildSignable,
 * which is itself a verbatim port of the server's own logic -- not reconstructed from a
 * description. Uses Java's native Ed25519 support (java.security, built in since Java 15) --
 * confirmed directly against a real Node-signed signature before this was written, no
 * external crypto dependency needed.
 */
final class Verify {
    private Verify() {}

    record Signable(Map<String, Object> fields, int fieldCount) {}

    /** Reconstructs the signable EXACTLY as the server did. Version-aware: proofs with
     * external_cost_hash were signed over 10 fields, older ones over 8. */
    static Signable buildSignable(JsonNode p) {
        boolean hasExt = p.has("external_cost_hash") && !p.get("external_cost_hash").isNull();

        Map<String, Object> base = new TreeMap<>();
        base.put("task_id", textOrNull(p, "task_id"));
        base.put("agent_id", textOrNull(p, "agent_id"));
        base.put("input_hash", textOrNull(p, "input_hash"));
        base.put("output_hash", textOrNull(p, "output_hash"));
        base.put("cost_pence", jsNumberValue(p, "cost_pence"));
        base.put("budget_pence", jsNumberValue(p, "budget_pence"));
        base.put("started_at", jsNumberValue(p, "started_at"));
        base.put("completed_at", jsStringValue(p, "completed_at"));

        if (hasExt) {
            base.put("external_cost_hash", jsStringValue(p, "external_cost_hash"));
            base.put("retrieved_count", p.has("retrieved_count") ? jsNumberValue(p, "retrieved_count") : 0.0);
            return new Signable(base, 10);
        }
        return new Signable(base, 8);
    }

    private static String textOrNull(JsonNode p, String field) {
        return p.has(field) && !p.get(field).isNull() ? p.get(field).asText() : null;
    }

    private static Double jsNumberValue(JsonNode p, String field) {
        if (!p.has(field) || p.get(field).isNull()) return 0.0;
        JsonNode v = p.get(field);
        return v.isTextual() ? Double.parseDouble(v.asText()) : v.asDouble();
    }

    private static String jsStringValue(JsonNode p, String field) {
        if (!p.has(field) || p.get(field).isNull()) return "";
        JsonNode v = p.get(field);
        return v.isTextual() ? v.asText() : Canonical.jsNumber(v.asDouble());
    }

    static VerifyResult verifyProof(String apiBase, String taskId, JsonNode proofInput) throws Exception {
        JsonNode proof;
        if (proofInput != null) {
            proof = proofInput;
        } else {
            if (taskId == null) throw new IllegalArgumentException("Provide task_id or proof");
            JsonNode data = Http.get(apiBase + "/v1/workforce/proof/" + taskId + "/public", null).json();
            if (!data.has("proof") || data.get("proof").isNull()) throw new RuntimeException("proof_not_found");
            proof = data.get("proof");
        }

        JsonNode keyData = Http.get(apiBase + "/v1/workforce/proof/public-key", null).json();
        String keyId = keyData.has("key_id") ? keyData.get("key_id").asText() : null;
        String pem = keyData.has("public_key_pem") ? keyData.get("public_key_pem").asText() : "";

        PublicKey verifyingKey = null;
        try {
            byte[] der = pemToDer(pem);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            verifyingKey = kf.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            // leave null; verification will report false below
        }

        Signable signable = buildSignable(proof);
        String digest = Canonical.sha256Hex(Canonical.wfCanonical(signable.fields()));

        boolean verified = false;
        if (verifyingKey != null && proof.has("signature")) {
            String algorithm = proof.has("algorithm") ? proof.get("algorithm").asText() : null;
            if (algorithm == null || algorithm.equals("Ed25519")) {
                try {
                    byte[] sigBytes = Base64.getDecoder().decode(proof.get("signature").asText());
                    byte[] digestBytes = hexToBytes(digest);
                    Signature sig = Signature.getInstance("Ed25519");
                    sig.initVerify(verifyingKey);
                    sig.update(digestBytes);
                    verified = sig.verify(sigBytes);
                } catch (Exception e) {
                    verified = false;
                }
            }
        }

        String taskIdOut = proof.has("task_id") ? proof.get("task_id").asText() : null;

        return new VerifyResult(
                verified,
                taskIdOut,
                keyId,
                "Ed25519",
                signable.fieldCount(),
                true,
                verified
                        ? "Signature mathematically verified. This proof was signed by ForceDream and has not been altered."
                        : "Signature verification FAILED. The proof was altered or not signed by ForceDream."
        );
    }

    private static byte[] pemToDer(String pem) {
        String body = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
