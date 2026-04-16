# AI PR Review Assistant Demo Data

### 1) Sample Bitbucket Webhook Payload
```json
{
  "eventKey": "pullrequest:created",
  "actor": { "display_name": "Senior Developer" },
  "repository": {
    "slug": "payment-service",
    "full_name": "org/payment-service",
    "workspace": { "slug": "my-workspace" }
  },
  "pullrequest": {
    "id": 42,
    "title": "Refactor: Add retry mechanism to PayPal client",
    "source": {
      "branch": { "name": "feature/retry-logic" },
      "commit": { "hash": "a1b2c3d4e5f6" }
    },
    "destination": {
      "branch": { "name": "main" },
      "commit": { "hash": "f6e5d4c3b2a1" }
    }
  }
}
```

### 2) Sample PR Diff (Java)
```diff
--- a/src/main/java/com/example/PayPalClient.java
+++ b/src/main/java/com/example/PayPalClient.java
@@ -10,5 +10,12 @@
     public void processPayment(Order order) {
-        api.call(order);
+        for (int i = 0; i < 3; i++) {
+            try {
+                api.call(order);
+                break;
+            } catch (Exception e) {
+                log.error("Failed to call PayPal", e);
+            }
+        }
     }
```

### 3) Sample AI JSON Response
```json
{
  "summary": "Added retry logic for PayPal API calls.",
  "severity": "MEDIUM",
  "suggestions": [
    "Consider using an exponential backoff strategy instead of a simple for-loop retry.",
    "The exception handling is too broad; catch specific API exceptions instead of `Exception`.",
    "Inject the max retry count from configuration instead of hardcoding `3`."
  ]
}
```

### 4) Sample PR Comment Output
### 🤖 AI PR Review Assistant

**Summary:** Added retry logic for PayPal API calls.

**Severity:** `MEDIUM`

**Suggestions:**
- Consider using an exponential backoff strategy instead of a simple for-loop retry.
- The exception handling is too broad; catch specific API exceptions instead of `Exception`.
- Inject the max retry count from configuration instead of hardcoding `3`.

---
*Disclaimer: AI-generated review. Please verify before merging.*

### 5) Sample Logs
```text
2026-04-10 10:00:00.123 [b2f4-5a6d] [nio-8080-exec-1] INFO  WebhookController - Received Bitbucket webhook event: pullrequest:created for PR #42
2026-04-10 10:00:00.125 [b2f4-5a6d] [task-1] INFO  ReviewCopilotCoreService - Starting PR Review workflow for: payment-service:42:a1b2c3d4e5f6
2026-04-10 10:00:00.456 [b2f4-5a6d] [task-1] INFO  BitbucketClient - Fetching diff for PR #42 in my-workspace/payment-service
2026-04-10 10:00:01.234 [b2f4-5a6d] [task-1] INFO  LlmClient - Sending review prompt to LLM (Model: gpt-4-turbo)
2026-04-10 10:00:04.567 [b2f4-5a6d] [task-1] INFO  LlmClient - LLM Review completed in 3333ms
2026-04-10 10:00:04.890 [b2f4-5a6d] [task-1] INFO  BitbucketClient - Posting comment to PR #42
2026-04-10 10:00:05.123 [b2f4-5a6d] [task-1] INFO  ReviewCopilotCoreService - Successfully completed PR review for: payment-service:42:a1b2c3d4e5f6
```
