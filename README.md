# DIDx Slack Bot

## Getting Started

To use the new [Slack Manifest API](https://api.slack.com/reference/manifests),
we need the "App Configuration Token". To get one, visit the
[Application List Page](https://api.slack.com/apps), and in the "Your App
Configuration Tokens" block, click the enticing "Generate Token" button.

Copy the access token, and export as follows:

```sh
export APP_CONF_TOKEN='xoxe.xoxp-1-...'
```

You can now use the `create-slack-app.sh` script to send [our custom app manifest](resources/slack-manifest.json) to the Slack API endpoint:
```sh
scripts/create-slack-app.sh
```

If successful, you'll be prompted with a link containing the new app id. If it is null, consult the error message.

Visit the <https://api.slack.com/apps/${app_id}/oauth> page, and install the app to your workspace.

You can now open Slack, scroll below Direct Messages to view Apps, and select Manage > Browse Apps. Your new app will appear in the directory. Tada!

## Updating the app

Whenever you want to update the webhook URL

```bash
export WEBHOOK_URL='' # Webhook handler for the slack app
export APP_CONF_TOKEN='' # Can be generated at https://api.slack.com/apps
scripts/update-slack-app.sh
```

## Run locally

### Ngrok

Slack requires https traffic and current server implementation exposes only http
endpoint. This means you can't simply expose the port on your router. Instead,
you can use ngrok to create a http proxy.

```
brew install --cask ngrok

ngrok http 9876 

# public url will be shown in the terminal. Alternatively you can get it with
curl -s localhost:4040/api/tunnels | jq -r '.tunnels[0].public_url'
```

### Prerequisites

```bash
export SLACK_BOT_TOKEN='' # Can be generated at https://api.slack.com/apps/${APP_ID}/oauth
```


## Acknowledgements

Forked from [Krever/scala-slack-bot](https://github.com/Krever/scala-slack-bot)
