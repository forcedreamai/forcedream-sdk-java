package ai.forcedream.sdk.examples;

import ai.forcedream.sdk.*;

public class LiveTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Real signup ===");
        String email = "java-sdk-test-" + System.currentTimeMillis() + "@example.com";
        SignupResponse signup = ForceDream.signup(email);
        System.out.println("Signed up: user_id=" + signup.userId() + ", trial_balance=" + signup.trialBalanceGbp());

        ForceDream client = new ForceDream(signup.liveKey());

        System.out.println();
        System.out.println("=== search_agents (client-side filtered) ===");
        var search = client.searchAgents("data:extraction", null);
        System.out.println(search.toPrettyString());

        System.out.println();
        System.out.println("=== invoke (real agent, real charge) ===");
        InvokeResult result = client.invoke("data-extract-v1", "Extract the year and location from: Founded in 2011 in Tokyo, Japan.", 60L);
        System.out.println(result);

        if ("completed".equals(result.status()) && result.taskId() != null) {
            System.out.println();
            System.out.println("=== verify (real Ed25519 proof) ===");
            VerifyResult verifyResult = client.verifyByTaskId(result.taskId());
            System.out.println(verifyResult);
        }
    }
}
