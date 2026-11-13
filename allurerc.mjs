import { defineConfig } from "allure";

const branch = process.env.ALLURE_BRANCH ?? process.env.GITHUB_REF_NAME ?? "local";
const run = process.env.GITHUB_RUN_NUMBER ?? process.env.ALLURE_RUN ?? "local";
const commit = process.env.GITHUB_SHA ?? process.env.ALLURE_COMMIT ?? "local";

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
      },
    },
  },
});
