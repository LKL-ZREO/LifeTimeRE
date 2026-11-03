import { createHash, timingSafeEqual } from "node:crypto";
import { createServer } from "node:http";
import {
  OUTPUT_SCHEMA_VERSION,
  PROMPT_VERSION,
  instructions,
  responseSchema,
  validateAnalysisResult
} from "./review-contract.mjs";

const port = numberFromEnvironment("PORT", 8787);
const provider = providerFromEnvironment();
const model = process.env.AI_MODEL?.trim() || (
  provider === "deepseek"
    ? process.env.DEEPSEEK_MODEL?.trim() || "deepseek-flash"
    : process.env.OPENAI_MODEL?.trim() || "gpt-5.4-mini"
);
const providerApiKey = (
  provider === "deepseek" ? process.env.DEEPSEEK_API_KEY : process.env.OPENAI_API_KEY
)?.trim() || "";
const deepSeekBaseUrl = (process.env.DEEPSEEK_BASE_URL?.trim() || "https://api.deepseek.com").replace(/\/$/, "");
const gatewayToken = process.env.GATEWAY_TOKEN?.trim() || "";

if (!gatewayToken) {
  throw new Error("GATEWAY_TOKEN is required.");
}

const server = createServer(async (request, response) => {
  const requestId = cryptoId();
  try {
    if (!authorized(request.headers.authorization)) {
      return sendJson(response, 401, { error: "访问令牌无效。", requestId });
    }

    if (request.method === "GET" && request.url === "/health") {
      return sendJson(response, 200, {
        ok: true,
        providerConfigured: Boolean(providerApiKey),
        provider,
        model: `${provider}/${model}`,
        promptVersion: PROMPT_VERSION,
        schemaVersion: OUTPUT_SCHEMA_VERSION,
        requestId
      });
    }

    if (request.method === "POST" && request.url === "/v1/reviews/analyze") {
      if (!providerApiKey) {
        return sendJson(response, 503, { error: "服务端尚未配置模型密钥。", requestId });
      }
      const payload = await readJson(request);
      validateReviewRequest(payload);
      const analysis = await analyzeWithProvider(payload, requestId);
      try {
        validateAnalysisResult(analysis, payload.body);
      } catch (error) {
        throw serverError(error.message);
      }
      return sendJson(response, 200, {
        ...analysis,
        metadata: {
          model: `${provider}/${model}`,
          promptVersion: PROMPT_VERSION,
          schemaVersion: OUTPUT_SCHEMA_VERSION,
          analyzedAtEpochMillis: Date.now()
        }
      });
    }

    return sendJson(response, 404, { error: "接口不存在。", requestId });
  } catch (error) {
    const status = Number.isInteger(error.status) ? error.status : 500;
    if (status >= 500) console.error(`[${requestId}] ${error.message}`);
    return sendJson(response, status, {
      error: status >= 500 ? "AI 服务暂时不可用，请稍后重试。" : error.message,
      requestId
    });
  }
});

server.listen(port, "0.0.0.0", () => {
  console.log(`LifeTimeRE review gateway listening on port ${port}; provider=${provider}; model=${model}; providerConfigured=${Boolean(providerApiKey)}`);
});

async function analyzeWithProvider(payload, requestId) {
  return provider === "deepseek"
    ? analyzeWithDeepSeek(payload, requestId)
    : analyzeWithOpenAi(payload, requestId);
}

async function analyzeWithOpenAi(payload, requestId) {
  const apiResponse = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${providerApiKey}`,
      "X-Client-Request-Id": requestId
    },
    body: JSON.stringify({
      model,
      store: false,
      instructions,
      input: `旅志日期：${payload.localDate}\n修订号：${payload.revision}\n旅志正文：\n${payload.body}`,
      max_output_tokens: 1200,
      text: {
        format: {
          type: "json_schema",
          name: "lifetimere_journal_review",
          strict: true,
          schema: responseSchema
        }
      }
    }),
    signal: AbortSignal.timeout(60_000)
  });

  if (!apiResponse.ok) {
    const detail = await apiResponse.text();
    throw serverError(`OpenAI returned ${apiResponse.status}: ${detail.slice(0, 500)}`);
  }
  const responseBody = await apiResponse.json();
  if (responseBody.status === "incomplete") {
    throw serverError(`OpenAI response was incomplete: ${responseBody.incomplete_details?.reason || "unknown reason"}.`);
  }
  const outputContent = responseBody.output?.flatMap(item => item.content || []) || [];
  const refusal = outputContent.find(content => content.type === "refusal")?.refusal;
  if (refusal) throw serverError(`OpenAI refused the review request: ${String(refusal).slice(0, 300)}`);
  const outputText = outputContent.find(content => content.type === "output_text")?.text;
  if (!outputText) throw serverError("OpenAI response did not contain output_text.");
  try {
    return JSON.parse(outputText);
  } catch {
    throw serverError("OpenAI structured output was not valid JSON.");
  }
}

async function analyzeWithDeepSeek(payload, requestId) {
  const jsonInstructions = `${instructions}\n\n你必须只输出 JSON 对象，不要使用 Markdown 代码块。对象形状示例：{"summary":"一到两句中文事实概括","actions":[{"id":"action_1","attribute":"KNOWLEDGE","intensity":1,"quote":"旅志中的连续原文","explanation":"对应属性和强度的简短理由"}]}。没有行动时 actions 必须是空数组。`;
  const apiResponse = await fetch(`${deepSeekBaseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${providerApiKey}`,
      "X-Client-Request-Id": requestId
    },
    body: JSON.stringify({
      model,
      messages: [
        { role: "system", content: jsonInstructions },
        {
          role: "user",
          content: `旅志日期：${payload.localDate}\n修订号：${payload.revision}\n旅志正文：\n${payload.body}`
        }
      ],
      response_format: { type: "json_object" },
      temperature: 0,
      reasoning_effort: "none",
      max_tokens: 2000,
      stream: false
    }),
    signal: AbortSignal.timeout(60_000)
  });

  if (!apiResponse.ok) {
    const detail = await apiResponse.text();
    throw serverError(`DeepSeek returned ${apiResponse.status}: ${detail.slice(0, 500)}`);
  }
  const responseBody = await apiResponse.json();
  const finishReason = responseBody.choices?.[0]?.finish_reason;
  if (finishReason === "length") throw serverError("DeepSeek response was truncated.");
  const outputText = responseBody.choices?.[0]?.message?.content;
  if (typeof outputText !== "string" || !outputText.trim()) {
    throw serverError("DeepSeek response did not contain JSON content.");
  }
  try {
    return JSON.parse(outputText);
  } catch {
    throw serverError("DeepSeek JSON output was not valid JSON.");
  }
}

function validateReviewRequest(payload) {
  if (!payload || typeof payload !== "object") throw clientError("请求正文无效。")
  if (typeof payload.body !== "string" || !payload.body.trim() || payload.body.length > 5000) {
    throw clientError("旅志正文长度无效。")
  }
  if (typeof payload.localDate !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(payload.localDate)) {
    throw clientError("旅志日期无效。")
  }
  if (!Number.isInteger(payload.revision) || payload.revision < 1) throw clientError("修订号无效。")
}

function authorized(header) {
  const supplied = header?.startsWith("Bearer ") ? header.slice(7) : "";
  const expectedDigest = createHash("sha256").update(gatewayToken).digest();
  const suppliedDigest = createHash("sha256").update(supplied).digest();
  return timingSafeEqual(expectedDigest, suppliedDigest);
}

async function readJson(request) {
  let text = "";
  for await (const chunk of request) {
    text += chunk;
    if (Buffer.byteLength(text) > 20_000) throw clientError("请求过大。", 413);
  }
  try {
    return JSON.parse(text);
  } catch {
    throw clientError("请求正文不是有效 JSON。")
  }
}

function sendJson(response, status, value) {
  const body = JSON.stringify(value);
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Cache-Control": "no-store"
  });
  response.end(body);
}

function clientError(message, status = 400) {
  return Object.assign(new Error(message), { status });
}

function serverError(message) {
  return Object.assign(new Error(message), { status: 502 });
}

function cryptoId() {
  return globalThis.crypto.randomUUID();
}

function numberFromEnvironment(name, fallback) {
  const value = Number.parseInt(process.env[name] || "", 10);
  return Number.isInteger(value) && value > 0 ? value : fallback;
}

function providerFromEnvironment() {
  const value = process.env.AI_PROVIDER?.trim().toLowerCase() || "openai";
  if (!new Set(["openai", "deepseek"]).has(value)) {
    throw new Error("AI_PROVIDER must be openai or deepseek.");
  }
  return value;
}
