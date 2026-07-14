package ai.forcedream.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SignupResponse(
        @JsonProperty("api_key") String apiKey,
        @JsonProperty("user_id") String userId,
        @JsonProperty("live_key") String liveKey,
        @JsonProperty("trial_balance_pence") long trialBalancePence,
        @JsonProperty("trial_balance_gbp") String trialBalanceGbp,
        @JsonProperty("referral_code") String referralCode
) {}
