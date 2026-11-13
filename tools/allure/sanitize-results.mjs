import { readdir, readFile, stat, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";

const roots = process.argv.slice(2);
const targets = roots.length > 0 ? roots : ["allure-results"];

const secretKey = /password|passwd|secret|token|authorization|api[-_]?key|client[-_]?secret/i;
const bearer = /Bearer\s+[A-Za-z0-9._~+/=-]+/g;
const jwt = /eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g;

let redactedFiles = 0;

function sanitizeValue(value, key = "") {
  if (typeof value === "string") {
    if (secretKey.test(key)) {
      return "[REDACTED]";
    }
    return value.replace(bearer, "Bearer [REDACTED]").replace(jwt, "[REDACTED_JWT]");
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeValue(item, key));
  }
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([childKey, childValue]) => [
        childKey,
        sanitizeValue(childValue, childKey),
      ]),
    );
  }
  return value;
}

async function sanitizeFile(path) {
  const info = await stat(path);
  if (!info.isFile() || !path.endsWith(".json")) {
    return;
  }

  const original = await readFile(path, "utf8");
  let parsed;
  try {
    parsed = JSON.parse(original);
  } catch {
    return;
  }

  const sanitized = JSON.stringify(sanitizeValue(parsed));
  if (sanitized !== original) {
    await writeFile(path, sanitized, "utf8");
    redactedFiles += 1;
  }
}

async function walk(path) {
  const info = await stat(path);
  if (info.isDirectory()) {
    for (const entry of await readdir(path)) {
      await walk(join(path, entry));
    }
    return;
  }
  await sanitizeFile(path);
}

for (const target of targets) {
  await walk(resolve(target));
}

console.log(`Allure results sanitized. Files changed: ${redactedFiles}`);
