# Personal details app

Spring Boot 4.1 (Java 21) app with a single-page form and a REST API for creating, reading,
updating and deleting personal details. Records are kept **in the HTTP session** (a
session-scoped bean), so there is no database. Each browser sees only its own records.

| Method | Path                | Purpose                 |
|--------|---------------------|-------------------------|
| GET    | `/api/people`       | List this session's people |
| GET    | `/api/people/{id}`  | Get one                 |
| POST   | `/api/people`       | Create (201 + Location) |
| PUT    | `/api/people/{id}`  | Update                  |
| DELETE | `/api/people/{id}`  | Delete (204)            |
| GET    | `/actuator/health`  | Liveness                |
| GET    | `/actuator/info`    | Deployed version (`build.version`, `app.commit`) |

## Run locally

    mvn spring-boot:run          # http://localhost:8080
    mvn verify                   # tests

Locally `/actuator/info` shows `"commit": "unknown"`. On Render it shows the deployed git SHA.

## Hosting: Render free web service (Docker)

1. Push this repo to GitLab (public or private).
2. In Render: **New > Blueprint**, connect GitLab, pick the repo. `render.yaml` creates the
   service (free plan, Singapore region, auto-deploy off).
   If you create the service by hand instead: runtime Docker, plan Free, Auto-Deploy **Off**,
   health check path `/actuator/health`, env var `SESSION_COOKIE_SECURE=true`.
3. In Render, open the service **Settings** and copy the **Deploy Hook** URL. It looks like
   `https://api.render.com/deploy/srv-XXXX?key=YYYY`.
4. In GitLab: **Settings > CI/CD > Variables**, add:
   - `APP_URL` = `https://<your-service>.onrender.com`
   - `RENDER_SERVICE_ID` = `srv-XXXX`
   - `RENDER_DEPLOY_KEY` = `YYYY` (mask it; if GitLab refuses to mask, leave it unmasked and keep the repo private)

## How deployments work

Pushing code only runs tests. A deploy happens when a pipeline on `main` has `DEPLOY=true`:

1. `test` runs `mvn verify`.
2. `deploy_production` calls the Render deploy hook with `ref=<commit SHA>`.
3. `verify_deployment` polls `/actuator/info` until `app.commit` equals that SHA, then checks
   `/actuator/health` is `UP`. If the live app never reports the SHA within 20 minutes the job fails.

You can start it from GitLab (**Build > Pipelines > Run pipeline**, choose `main`, set `DEPLOY=true`)
or with the GitLab MCP (below).

**Roll back:** run a pipeline on `main` with `DEPLOY=true` and `DEPLOY_REF=<older commit SHA>`.
Tests are skipped for rollbacks so a broken `main` cannot block one.

## Controlling deployments with the GitLab MCP

GitLab's official MCP server lives at `https://gitlab.com/api/v4/mcp` (or
`https://<your-host>/api/v4/mcp`). Connect it from an MCP client, for example Claude Code:

    claude mcp add --transport http GitLab https://gitlab.com/api/v4/mcp

and complete the OAuth login. Then ask in plain language (tool names may differ slightly by GitLab version):

- "Create a pipeline on `main` in `<group>/<project>` with variable `DEPLOY=true`."  (deploy latest main)
- "Show the jobs in pipeline 123."  then  "Show the log for the verify_deployment job."
- "Create a pipeline on `main` with `DEPLOY=true` and `DEPLOY_REF=<sha>`."  (roll back)
- "Retry pipeline 123."  (if the deploy or verify job failed)
- "List the latest pipelines for the project."

The pipeline's `verify_deployment` job is the source of truth. It only goes green when
`/actuator/info` reports the commit you asked for. You can also check by hand:

    curl -s https://<your-service>.onrender.com/actuator/info

## Free-tier caveats

- Render free web services sleep after 15 minutes idle and take about a minute to wake up, with
  512 MB RAM and 0.1 CPU. Expect a slow first request.
- Sessions live in memory, so they are lost when the service sleeps, restarts or is redeployed.
  That is expected for this app (no database), but it means a deploy clears everyone's records.
