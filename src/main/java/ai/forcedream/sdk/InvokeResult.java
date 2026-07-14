package ai.forcedream.sdk;

import com.fasterxml.jackson.databind.JsonNode;

public record InvokeResult(
        String status,
        String agent,
        String taskId,
        JsonNode output,
        Long chargedPence,
        String proofId,
        String message,
        String error
) {}
