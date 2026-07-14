package ai.forcedream.sdk;

public record VerifyResult(
        boolean verified,
        String taskId,
        String keyId,
        String algorithm,
        int fieldsSigned,
        boolean trustless,
        String message
) {}
