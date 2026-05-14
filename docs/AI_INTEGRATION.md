# AI Integration Notes

## Provider abstraction

The backend defines an `AiProvider` interface:

```java
public interface AiProvider {
    AiResponse chat(AiRequest request);
}
```

Two implementations are registered as Spring beans:

| Bean                  | Active when `AI_PROVIDER=` |
|-----------------------|----------------------------|
| `OpenRouterAiProvider`| `openrouter`               |
| `FakeAiProvider`      | `fake` (default)           |

The active provider is selected at startup via `@ConditionalOnProperty`.
This allows development and tests to run without an OpenRouter API key.

## OpenRouter configuration

All settings are externalized through environment variables:

| Variable                   | Default                        | Description                        |
|----------------------------|--------------------------------|------------------------------------|
| `OPENROUTER_API_KEY`       | (required when provider=openrouter) | Secret key, never logged      |
| `OPENROUTER_BASE_URL`      | `https://openrouter.ai/api/v1` | Base URL for the API               |
| `OPENROUTER_MODEL`         | `openai/gpt-3.5-turbo`         | Model identifier                   |
| `OPENROUTER_MAX_TOKENS`    | `2048`                         | Maximum tokens in response         |
| `OPENROUTER_TEMPERATURE`   | `0.7`                          | Sampling temperature               |
| `OPENROUTER_TIMEOUT_SECONDS` | `30`                         | HTTP request timeout               |

## HTTP client

`OpenRouterAiProvider` uses Spring's `RestClient` (Spring 6.1+) configured with:
- `Authorization: Bearer ${OPENROUTER_API_KEY}` header (set once in the bean factory, never in logs)
- `HTTP-Referer` and `X-Title` headers as recommended by OpenRouter
- connect and read timeouts from configuration
- structured logging of request start, elapsed time, model used, token counts — without the API key or message content at INFO level (DEBUG logs full request for local dev only)

## Fake provider

`FakeAiProvider` returns deterministic canned responses based on the prompt type.
It is suitable for:
- local development without API key
- unit tests
- CI pipelines

The fake provider logs `[FAKE AI]` on every call so it is obvious when it is active.

## Prompt templates

Prompt templates live in `backend/src/main/resources/prompts/`.
Each template is a plain text file with a version suffix, e.g.:

```
prompts/
  ticket-summary-v1.txt
  suggested-reply-v1.txt
  chat-system-v1.txt
```

Templates use `{placeholder}` syntax. The `PromptBuilder` service loads the template by name, substitutes placeholders, and returns the final prompt string.

Versioning strategy:
- increment the version suffix when semantics change (v1 -> v2)
- keep old versions in the repository for audit purposes
- the active version is a constant in `PromptBuilder`

## Conversation context

Each `AiSession` is linked to one ticket and one user.
When sending a message, the service:
1. Loads the system prompt (injected once as the first message).
2. Loads the last N messages from the session (configurable, default 20).
3. Appends the new user message.
4. Calls the AI provider.
5. Persists the assistant response as an `AiMessage`.

Token counts from the OpenRouter response are stored on `AiMessage` for cost tracking.

## AI-generated content rules

- AI responses are **never** automatically applied to a ticket.
- Applying a summary or suggested reply requires an **explicit user action** (a separate API call).
- Applied summaries are stored as `AI_SUMMARY` ticket notes with the generating model and timestamp.
- Status changes remain exclusively under human control.

## Error handling

If the OpenRouter call fails or times out:
- The backend returns HTTP `503` with a problem detail body.
- The failed message is persisted with `errorFlag=true` and `errorMessage` set.
- The frontend shows a recoverable error banner and allows the user to retry.
- No partial or corrupted state is written to the ticket.
