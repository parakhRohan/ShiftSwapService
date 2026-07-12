# Deployment Guide

This document describes how to build, publish, deploy, and roll back the Shift Swap Service in production.

## Overview

The release flow is:

1. Run Maven tests.
2. Build the Docker image.
3. Push the image to the container registry.
4. Deploy to Kubernetes with Helm.
5. Switch traffic using blue/green deployment.

## Build and Test

Run the unit and integration test suite before packaging:

```bash
mvn test
```

To build the application artifact:

```bash
mvn -DskipTests package
```

## Docker Image

The repository contains a multi-stage `Dockerfile`.

Build locally:

```bash
docker build -t shift-swap-service:local .
```

## Container Registry

The GitHub Actions workflow expects these repository secrets:

- `REGISTRY_URL`
- `IMAGE_NAME`
- `REGISTRY_USERNAME`
- `REGISTRY_PASSWORD`

On pushes to `main`, the workflow builds and pushes:

- a commit SHA tag
- a `latest` tag on the default branch

## Helm Deployment

The Helm chart is located at `helm/shift-swap-service`.

Important values:

- `image.repository`: registry path for the image
- `blueGreen.activeColor`: the active traffic target, `blue` or `green`
- `blueGreen.blue.imageTag`: image tag for blue
- `blueGreen.green.imageTag`: image tag for green
- `env.appSeedDataEnabled`: keep `false` in production

Example deploy:

```bash
helm upgrade --install shift-swap-service ./helm/shift-swap-service \
  --set image.repository=registry.example.com/shift-swap-service \
  --set blueGreen.blue.imageTag=1.0.0 \
  --set blueGreen.green.imageTag=1.0.1 \
  --set blueGreen.activeColor=green
```

## Blue/Green Flow

The chart deploys two separate Deployments:

- `shift-swap-service-blue`
- `shift-swap-service-green`

The Service routes traffic to the Deployment whose `color` label matches `blueGreen.activeColor`.

Recommended release steps:

1. Deploy the new image to the inactive color.
2. Verify readiness and smoke test the inactive color.
3. Switch `blueGreen.activeColor` to the new color.
4. Monitor health, logs, and business metrics.

## Rollback Plan

Rollback is a selector switch, not a full rebuild.

If the new release is unhealthy:

1. Change `blueGreen.activeColor` back to the previous color.
2. Re-run `helm upgrade`.
3. If needed, redeploy the previous image tag to the previously active color.
4. Confirm the service is healthy before closing the incident.

If the failure is caused by a schema issue or incompatible database change:

1. Stop traffic to the new version.
2. Restore the last known good database backup.
3. Redeploy the previous application version.
4. Verify the app against readiness and smoke tests before re-enabling traffic.

## Production Notes

- Keep seeding disabled in production.
- Use a real external database, not H2.
- Prefer additive Flyway migrations.
- Keep readiness and liveness probes enabled.
- Store image tags immutably for each release.
