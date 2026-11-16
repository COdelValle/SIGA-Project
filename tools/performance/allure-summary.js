import crypto from 'k6/crypto';

function uuid() {
  const bytes = new Uint8Array(crypto.randomBytes(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function metricValue(data, name, key) {
  const metric = data.metrics?.[name];
  if (!metric || !metric.values) {
    return undefined;
  }
  return metric.values[key];
}

function perfResults(data, scenario) {
  const now = Date.now();
  const measurements = [];
  const add = (id, key, value) => {
    if (typeof value === "number" && Number.isFinite(value)) {
      measurements.push({ id, key, value, start: now, stop: now });
    }
  };

  add(`${scenario}-duration`, "http.req.duration.p95", metricValue(data, "http_req_duration", "p(95)"));
  add(`${scenario}-duration`, "http.req.duration.avg", metricValue(data, "http_req_duration", "avg"));
  add(`${scenario}-duration`, "http.req.duration.max", metricValue(data, "http_req_duration", "max"));
  add(`${scenario}-errors`, "http.req.failed.rate", metricValue(data, "http_req_failed", "rate"));
  add(`${scenario}-throughput`, "http.reqs.rate", metricValue(data, "http_reqs", "rate"));
  add(`${scenario}-iterations`, "iterations.count", metricValue(data, "iterations", "count"));

  return JSON.stringify(measurements, null, 2);
}

function thresholdResults(data, scenario) {
  const results = [];
  for (const [metricName, metric] of Object.entries(data.metrics ?? {})) {
    const thresholds = metric.thresholds ?? {};
    for (const [thresholdName, threshold] of Object.entries(thresholds)) {
      const passed = threshold.ok !== false;
      const name = `${scenario} :: ${metricName} ${thresholdName}`;
      results.push({
        uuid: uuid(),
        historyId: `${scenario}-${metricName}-${thresholdName}`,
        name,
        fullName: name,
        status: passed ? "passed" : "failed",
        statusDetails: passed ? {} : { message: "Threshold k6 incumplido" },
        stage: "finished",
        start: Date.now(),
        stop: Date.now(),
        labels: [
          { name: "layer", value: "performance" },
          { name: "epic", value: "Performance" },
          { name: "feature", value: scenario },
          { name: "story", value: "Thresholds k6" },
          { name: "owner", value: "qa" },
          { name: "tag", value: "performance" },
        ],
        parameters: [],
        steps: [],
        attachments: [],
        links: [],
      });
    }
  }
  return results;
}

export function handleAllureSummary(data, scenario) {
  const resultFiles = thresholdResults(data, scenario);
  const files = {
    "tools/performance/allure-results/performance.json": perfResults(data, scenario),
  };

  for (const result of resultFiles) {
    files[`tools/performance/allure-results/${result.uuid}-result.json`] =
      JSON.stringify(result, null, 2);
  }

  const summary = JSON.stringify(data, null, 2);
  const attachmentName = `${scenario}-k6-summary.json`;
  files[`tools/performance/allure-results/${attachmentName}`] = summary;

  return files;
}
