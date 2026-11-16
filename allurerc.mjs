import { existsSync, readFileSync } from "node:fs";
import { defaultChartsConfig, defineConfig } from "allure";

const branch = process.env.ALLURE_BRANCH ?? process.env.GITHUB_REF_NAME ?? "local";
const run = process.env.GITHUB_RUN_NUMBER ?? process.env.ALLURE_RUN ?? "local";
const commit = process.env.GITHUB_SHA ?? process.env.ALLURE_COMMIT ?? "local";

let coverage = {};
const coveragePath = "tools/coverage/coverage-summary.json";
if (existsSync(coveragePath)) {
  try {
    coverage = JSON.parse(readFileSync(coveragePath, "utf8"));
  } catch {
    coverage = {};
  }
}

const backendCoverage = coverage.backend?.business?.lines;
const frontendCoverage = coverage.frontend?.business?.lines;

export default defineConfig({
  name: "SIGA - Reporte Centralizado",
  output: "allure-report",
  historyPath:
    process.env.ALLURE_HISTORY_PATH ?? `./.allure-history/${branch}.jsonl`,
  appendHistory: true,
  historyLimit: 50,
  variables: {
    Repositorio: "COdelValle/SIGA-Project",
    Rama: branch,
    Commit: commit.slice(0, 12),
    Ejecucion: run,
    Entorno: process.env.ALLURE_ENV ?? "ci",
    "Cobertura backend": backendCoverage === undefined ? "sin datos" : `${backendCoverage}%`,
    "Cobertura frontend": frontendCoverage === undefined ? "sin datos" : `${frontendCoverage}%`,
  },
  defaultLabels: {
    owner: "SIGA",
    severity: "normal",
  },
  categories: {
    rules: [
      {
        name: "Infraestructura",
        id: "infraestructura",
        matchers: {
          message: /ECONNREFUSED|timeout|Docker|Testcontainers|Connection refused/i,
        },
        groupBy: ["layer", "owner"],
        expand: true,
      },
      {
        name: "Producto",
        id: "producto",
        matchers: {
          statuses: ["failed"],
        },
        groupBy: [{ label: "epic" }, { label: "feature" }],
        expand: true,
      },
      {
        name: "Inestables",
        id: "inestables",
        matchers: {
          flaky: true,
        },
        groupBy: ["owner", { label: "feature" }],
        expand: true,
      },
    ],
  },
  plugins: {
    awesome: {
      options: {
        reportName: "SIGA - Pruebas",
        groupBy: ["layer", "epic", "feature", "story"],
        charts: [
          ...defaultChartsConfig.filter((chart) => chart.type !== "testingPyramid"),
          {
            type: "testingPyramid",
            title: "Piramide de pruebas",
            layers: [
              "unit",
              "component",
              "integration",
              "contract",
              "e2e",
              "security",
              "performance",
              "infra",
            ],
          },
        ],
      },
    },
  },
});
