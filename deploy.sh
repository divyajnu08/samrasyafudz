#!/usr/bin/env bash
#
# deploy.sh — one-command deployment of the Samrasya FUDZ stack to GCP + Firebase.
#
# Pipeline (in dependency order):
#   1. Build + push a Docker image for each backend service to Artifact Registry.
#   2. Deploy each to Cloud Run with the correct env vars / Secret Manager refs.
#   3. Capture each Cloud Run URL and feed it to the dependent service that needs it.
#   4. Build + deploy the frontend to Firebase Hosting.
#   5. Run a smoke test against the api-gateway and print a success/failure summary.
#   6. Grant roles/run.invoker to allUsers on the api-gateway (required after a fresh deploy).
#
# Requirements before running:
#   - gcloud CLI authenticated and pointing at the right project (gcloud auth login,
#     gcloud config set project <project-id>)
#   - firebase CLI authenticated (firebase login)
#   - Artifact Registry repo + Cloud Run APIs enabled
#   - Secrets already in Secret Manager (see check_secrets below)
#
# Usage:  ./deploy.sh
###############################################################################

set -euo pipefail

# ---------------------------------------------------------------- CONFIG --
# EDIT THESE for your environment
PROJECT_ID="${PROJECT_ID:-}"                                   # GCP project id
REGION="${REGION:-asia-south1}"                                # Cloud Run region
AR_REPO="${AR_REPO:-samrasya-services}"                        # Artifact Registry repo name
CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE:-}"                   # full path or name e.g. project:region:instance
DB_USER="${DB_USER:-postgres}"
ARTIFACT_REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}"

# Firestore/Google Maps keys for the frontend (baked at build time)
FRONTEND_DIR="samrasyafudz-frontend"
VITE_GOOGLE_MAPS_API_KEY="${VITE_GOOGLE_MAPS_API_KEY:-}"

# Shared across all backend services (must be identical everywhere)
JWT_SECRET="${JWT_SECRET:-}"

# Existing secret names inside Secret Manager (create these yourself)
SECRET_JWT="jwt-secret"
SECRET_DB_PASSWORD="db-password"

# Service -> deployed name on Cloud Run
declare -A SERVICES=(
  [user-service]="user-service"
  [product-service]="product-service"
  [order-service]="order-service"
  [api-gateway]="api-gateway"
)

# ----------------------------------------------------------------- helper --
info()  { printf '\n\033[1;36m==> %s\033[0m\n' "$1"; }
ok()    { printf '\033[0;32m    %s\033[0m\n' "$1"; }
die()   { printf '\033[0;31mFATAL: %s\033[0m\n' "$1" >&2; exit 1; }

# Track per-step status for the final summary. `fail` is NOT wired to set -e;
# set -e aborts on the first real error, which is what the issue wants.
STEP_LOG=()
run_step() {
    local name="$1"; shift
    info "$name"
    if ! "$@"; then
        STEP_LOG+=("FAIL  $name")
        die "$name failed — aborting to avoid a half-deployed, inconsistent state."
    fi
    STEP_LOG+=("OK    $name")
    ok "$name succeeded"
}

# ------------------------------------------------------------------ checks --
preflight() {
    info "Preflight checks"
    [[ -n "$PROJECT_ID" ]] || die "PROJECT_ID is not set. Export it or edit deploy.sh"
    command -v gcloud >/dev/null || die "gcloud CLI not found. Install it and re-run."
    command -v firebase >/dev/null || die "firebase CLI not found. Install it and re-run."
    command -v docker >/dev/null || die "docker not found. Install it and re-run."
    gcloud config get-value project 2>/dev/null | grep -q "$PROJECT_ID" \
        || die "gcloud is not on project $PROJECT_ID. Run: gcloud config set project $PROJECT_ID"
    ok "gcloud on project $PROJECT_ID"

    # Fail loudly & early if required secrets aren't in Secret Manager,
    # rather than deploying and only failing inside a running container.
    for secret in "$SECRET_JWT" "$SECRET_DB_PASSWORD"; do
        if ! gcloud secrets describe "$secret" --project="$PROJECT_ID" >/dev/null 2>&1; then
            die "Secret '$secret' missing from Secret Manager. Create it before deploying."
        fi
        ok "secret $secret present"
    done
    [[ -n "$JWT_SECRET" ]] && ok "JWT_SECRET env override will be used"
    ok "preflight passed"
}

# -------------------------------------------------------------- image urls --
image_url() { # $1 = gradle module dir name (e.g. user-service)
    echo "${ARTIFACT_REGISTRY}/${SERVICES[$1]}"
}

# --------------------------------------------------------------- build push --
# $1 = module dir  (e.g. user-service)
build_and_push() {
    local svc="$1"
    local img; img="$(image_url "$svc")"
    info "Building + pushing $svc"
    docker build -t "$img:latest" -f "$svc/Dockerfile" .
    docker push "$img:latest"
    ok "$svc image pushed: $img:latest"
}

# Shows the --set-secrets flags for one service (maps Spring props to Secret Manager).
# DB-backed services pull SPRING_DATASOURCE_PASSWORD from Secret Manager.
# JWT_SECRET is passed as a plain env var so it can be overridden locally, but can
# optionally be sourced from Secret Manager too by uncommenting the line below.
secrets_flags() {
    local svc="$1"
    local flags=( )
    if [[ -n "$(db_name_for "$svc")" ]]; then
        flags+=( "SPRING_DATASOURCE_PASSWORD=projects/${PROJECT_ID}/secrets/${SECRET_DB_PASSWORD}:latest" )
    fi
    # Uncomment to source the JWT secret from Secret Manager instead of the JWT_SECRET env var:
    # flags+=( "JWT_SECRET=projects/${PROJECT_ID}/secrets/${SECRET_JWT}:latest" )
    [[ "${#flags[@]}" -gt 0 ]] && printf -- '--set-secrets=%s ' "$(IFS=,; echo "${flags[*]}")"
}

# ---------------------------------------------------------------- deploy one --
# Deploys a single service and records its Cloud Run URL in SERVICE_URLS.
# $1 = service name (user-service | product-service | order-service | api-gateway)
declare -A SERVICE_URLS=()

# The database name each service owns. E.g. user-service -> usersdb.
# (api-gateway has no database of its own.)
db_name_for() {
    case "$1" in
        user-service)    echo "usersdb"    ;;
        product-service) echo "productdb"  ;;
        order-service)   echo "ordersdb"   ;;
        *)               echo ""           ;;
    esac
}

deploy_service() {
    local svc="$1"
    local img; img="$(image_url "$svc")"
    info "Deploying $svc to Cloud Run"

    local envs=()
    envs+=( "SPRING_PROFILES_ACTIVE=cloud" )

    # All DB-backed services get the Cloud SQL connection string + secrets.
    # The socket factory connects without a dedicated host/port; the instance is
    # attached via --set-cloudsql-instances below.
    local db; db="$(db_name_for "$svc")"
    if [[ -n "$db" ]]; then
        envs+=( "SPRING_DATASOURCE_USERNAME=${DB_USER}" )
        envs+=( "SPRING_DATASOURCE_URL=jdbc:postgresql:///${db}?cloudSqlInstance=${CLOUD_SQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory" )
    fi

    # JWT secret for every service that validates tokens.
    if [[ -n "$JWT_SECRET" ]]; then
        envs+=( "JWT_SECRET=${JWT_SECRET}" )
    fi

    # Service-to-service URLs (only services that call others need these).
    case "$svc" in
        order-service)
            envs+=( "USER_SERVICE_URL=${SERVICE_URLS[user-service]}" )
            envs+=( "PRODUCT_SERVICE_URL=${SERVICE_URLS[product-service]}" )
            ;;
        api-gateway)
            envs+=( "USER_SERVICE_URL=${SERVICE_URLS[user-service]}" )
            envs+=( "PRODUCT_SERVICE_URL=${SERVICE_URLS[product-service]}" )
            envs+=( "ORDER_SERVICE_URL=${SERVICE_URLS[order-service]}" )
            ;;
    esac

    # Cloud Run's --set-env-vars takes ONE comma-separated list.
    local env_arg; env_arg=$(IFS=,; echo "${envs[*]}")
    local secflags; secflags="$(secrets_flags "$svc")"

    gcloud run deploy "$svc" \
        --image "$img:latest" \
        --platform managed \
        --region "$REGION" \
        --project "$PROJECT_ID" \
        --allow-unauthenticated \
        --set-env-vars "$env_arg" \
        ${secflags} \
        --set-cloudsql-instances "$CLOUD_SQL_INSTANCE" \
        --memory 512Mi \
        --min-instances 0 \
        --max-instances 2 \
        --timeout 300 \
        --quiet

    # Capture the URL programmatically so the next service can consume it.
    local url
    url=$(gcloud run services describe "$svc" \
        --region "$REGION" --project "$PROJECT_ID" \
        --format='value(status.url)')
    SERVICE_URLS["$svc"]="$url"
    ok "$svc -> $url"
}

# ------------------------------------------------------------ API gateway role --
grant_gateway_invoker() {
    local svc="api-gateway"
    info "Granting roles/run.invoker to allUsers on $svc (prevents 403 after fresh deploy)"
    gcloud run services add-iam-policy-binding "$svc" \
        --region "$REGION" --project "$PROJECT_ID" \
        --member="allUsers" \
        --role="roles/run.invoker" \
        --quiet >/dev/null
    ok "invoker role granted"
}

# --------------------------------------------------------------- frontend --
deploy_frontend() {
    info "Building frontend"
    ( cd "$FRONTEND_DIR" && \
      VITE_GOOGLE_MAPS_API_KEY="$VITE_GOOGLE_MAPS_API_KEY" \
      VITE_API_URL="${SERVICE_URLS[api-gateway]}" \
      npm run build )

    info "Deploying frontend to Firebase Hosting"
    ( cd "$FRONTEND_DIR" && firebase deploy --only hosting )
    ok "frontend deployed"
}

# -------------------------------------------------------------- smoke test --
smoke_test() {
    info "Running smoke test"
    local gateway="${SERVICE_URLS[api-gateway]}"
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 30 "${gateway}/api/categories")
    if [[ "$code" == "200" ]]; then
        ok "smoke test passed: ${gateway}/api/categories -> HTTP $code"
    else
        die "smoke test failed: ${gateway}/api/categories -> HTTP $code (expected 200)"
    fi
}

# ------------------------------------------------------------------ summary --
summary() {
    printf '\n\033[1;35m========== DEPLOYMENT SUMMARY ==========\033[0m\n'
    local line
    local rc=0
    for line in "${STEP_LOG[@]}"; do
        printf '  %s\n' "$line"
        [[ "$line" == FAIL* ]] && rc=1
    done
    [[ -n "${SERVICE_URLS[api-gateway]:-}" ]] && printf '  Gateway URL: %s\n' "${SERVICE_URLS[api-gateway]}"
    printf '\033[1;35m========================================\033[0m\n'
    [[ "$rc" -eq 0 ]] && printf '\033[1;32mDeployment succeeded.\033[0m\n' || printf '\033[1;31mDeployment finished with failures.\033[0m\n'
}

# ------------------------------------------------------------------- main --
main() {
    preflight

    # 1) user + product have no dependencies — build & deploy first
    run_step "Build+push: user-service"    build_and_push user-service
    run_step "Build+push: product-service" build_and_push product-service
    run_step "Deploy: user-service"        deploy_service user-service
    run_step "Deploy: product-service"     deploy_service product-service

    # 2) order-service depends on user + product URLs
    run_step "Build+push: order-service"   build_and_push order-service
    run_step "Deploy: order-service"       deploy_service order-service

    # 3) api-gateway depends on all service URLs — deploy last
    run_step "Build+push: api-gateway"     build_and_push api-gateway
    run_step "Deploy: api-gateway"         deploy_service api-gateway
    run_step "Grant gateway invoker role"  grant_gateway_invoker

    # 4) frontend
    run_step "Deploy frontend (Firebase)"  deploy_frontend

    # 5) smoke test
    run_step "Smoke test" smoke_test

    summary
}

main "$@"
