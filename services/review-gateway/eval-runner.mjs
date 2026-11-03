import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import {
  ATTRIBUTES,
  OUTPUT_SCHEMA_VERSION,
  PROMPT_VERSION,
  validateAnalysisResult
} from "./review-contract.mjs";

const args = process.argv.slice(2);
const live = args.includes("--live");
const outputIndex = args.indexOf("--output");
const outputPath = outputIndex >= 0 ? args[outputIndex + 1] : "";
if (outputIndex >= 0 && !outputPath) throw new Error("--output requires a file path.");

const datasetPath = fileURLToPath(new URL("./evals/daily-review-cases.json", import.meta.url));
const dataset = JSON.parse(await readFile(datasetPath, "utf8"));
validateDataset(dataset);

let report;
if (live) {
  report = await runLive(dataset);
} else {
  report = {
    mode: "static",
    datasetVersion: dataset.datasetVersion,
    promptVersion: PROMPT_VERSION,
    schemaVersion: OUTPUT_SCHEMA_VERSION,
    totalCases: dataset.cases.length,
    expectedActions: dataset.cases.reduce((sum, item) => sum + item.expectedActions.length, 0),
    zeroActionCases: dataset.cases.filter(item => item.expectedActions.length === 0).length,
    passed: true,
    message: "Dataset structure and evidence anchors are valid. Use --live to evaluate the configured gateway."
  };
}

const serialized = `${JSON.stringify(report, null, 2)}\n`;
process.stdout.write(serialized);
if (outputPath) await writeFile(outputPath, serialized, "utf8");
if (!report.passed) process.exitCode = 1;

async function runLive(currentDataset) {
  const gatewayUrl = (process.env.REVIEW_GATEWAY_URL || "http://127.0.0.1:8787").replace(/\/$/, "");
  const token = process.env.GATEWAY_TOKEN?.trim() || "";
  if (!token) throw new Error("GATEWAY_TOKEN is required for live evaluation.");

  const caseReports = [];
  for (const [index, item] of currentDataset.cases.entries()) {
    try {
      const response = await fetch(`${gatewayUrl}/v1/reviews/analyze`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
          entryId: `eval_${item.id}`,
          revision: 1,
          localDate: "2026-11-01",
          body: item.body
        }),
        signal: AbortSignal.timeout(60_000)
      });
      const raw = await response.text();
      if (!response.ok) throw new Error(`Gateway returned ${response.status}: ${raw.slice(0, 300)}`);
      const result = JSON.parse(raw);
      validateAnalysisResult(result, item.body);
      caseReports.push(gradeCase(item, result));
    } catch (error) {
      caseReports.push({ id: item.id, passed: false, error: error.message });
    }
    process.stderr.write(`[${index + 1}/${currentDataset.cases.length}] ${item.id}\n`);
  }

  const successful = caseReports.filter(item => item.passed).length;
  const totals = caseReports.reduce((value, item) => ({
    missingActions: value.missingActions + (item.metrics?.missingActions || 0),
    falsePositiveActions: value.falsePositiveActions + (item.metrics?.falsePositiveActions || 0),
    wrongAttributes: value.wrongAttributes + (item.metrics?.wrongAttributes || 0),
    wrongIntensity: value.wrongIntensity + (item.metrics?.wrongIntensity || 0),
    invalidMetadata: value.invalidMetadata + (item.metrics?.invalidMetadata || 0)
  }), { missingActions: 0, falsePositiveActions: 0, wrongAttributes: 0, wrongIntensity: 0, invalidMetadata: 0 });

  return {
    mode: "live",
    datasetVersion: currentDataset.datasetVersion,
    promptVersion: PROMPT_VERSION,
    schemaVersion: OUTPUT_SCHEMA_VERSION,
    totalCases: caseReports.length,
    passedCases: successful,
    passRate: caseReports.length ? successful / caseReports.length : 0,
    metrics: totals,
    passed: successful === caseReports.length,
    cases: caseReports
  };
}

function gradeCase(item, result) {
  const unused = new Set(result.actions.map((_, index) => index));
  let missingActions = 0;
  let wrongAttributes = 0;
  let wrongIntensity = 0;

  for (const expected of item.expectedActions) {
    const matchIndex = [...unused].find(index => result.actions[index].quote.includes(expected.anchor));
    if (matchIndex === undefined) {
      missingActions += 1;
      continue;
    }
    unused.delete(matchIndex);
    const actual = result.actions[matchIndex];
    if (actual.attribute !== expected.attribute) wrongAttributes += 1;
    if (!expected.allowedIntensity.includes(actual.intensity)) wrongIntensity += 1;
  }

  const falsePositiveActions = unused.size + Math.max(0, result.actions.length - item.maxActions - unused.size);
  const metadata = result.metadata || {};
  const invalidMetadata = Number(
    metadata.promptVersion !== PROMPT_VERSION ||
    metadata.schemaVersion !== OUTPUT_SCHEMA_VERSION ||
    typeof metadata.model !== "string" || !metadata.model ||
    !Number.isInteger(metadata.analyzedAtEpochMillis) || metadata.analyzedAtEpochMillis <= 0
  );
  const metrics = { missingActions, falsePositiveActions, wrongAttributes, wrongIntensity, invalidMetadata };
  return {
    id: item.id,
    passed: Object.values(metrics).every(value => value === 0),
    metrics,
    actualActions: result.actions.map(action => ({
      attribute: action.attribute,
      intensity: action.intensity,
      quote: action.quote
    }))
  };
}

function validateDataset(value) {
  if (!value || typeof value.datasetVersion !== "string" || !Array.isArray(value.cases) || !value.cases.length) {
    throw new Error("Eval dataset is missing required fields.");
  }
  const ids = new Set();
  for (const item of value.cases) {
    if (!item.id || ids.has(item.id)) throw new Error(`Duplicate or missing case id: ${item.id}`);
    ids.add(item.id);
    if (typeof item.body !== "string" || !item.body) throw new Error(`${item.id}: body is required.`);
    if (!Array.isArray(item.expectedActions)) throw new Error(`${item.id}: expectedActions must be an array.`);
    if (!Number.isInteger(item.maxActions) || item.maxActions < item.expectedActions.length || item.maxActions > 8) {
      throw new Error(`${item.id}: maxActions is invalid.`);
    }
    for (const expected of item.expectedActions) {
      if (!expected.anchor || !item.body.includes(expected.anchor)) throw new Error(`${item.id}: evidence anchor is not in body.`);
      if (!ATTRIBUTES.includes(expected.attribute)) throw new Error(`${item.id}: unknown attribute.`);
      if (!Array.isArray(expected.allowedIntensity) || !expected.allowedIntensity.length ||
          expected.allowedIntensity.some(intensity => ![1, 2, 3].includes(intensity))) {
        throw new Error(`${item.id}: invalid allowedIntensity.`);
      }
    }
  }
}
