#!/usr/bin/env bash
set -xeuv

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

manifest="$(cat ${SCRIPT_DIR}/../resources/slack-manifest.json | envsubst)"

function createApp() {
  curl -s -XPOST https://slack.com/api/apps.manifest.create \
    -H "Authorization: Bearer ${APP_CONF_TOKEN}" \
    -d "manifest=$manifest"
}

response=$(createApp)
app_id=$(echo $response | jq -r '.app_id')

echo "Go to https://api.slack.com/apps/${app_id}/oauth and install the app"
