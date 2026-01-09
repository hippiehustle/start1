# Kaos EV Crypto Scanner Backend

Production-ready backend for the Coinbase live-data “EV Crypto Scanner” system. It streams public Coinbase market data, caches it in Upstash Redis, runs the EV scanner, and exposes an API for a native Android app + widget.

## Features
- Coinbase WebSocket subscriptions for `ticker`, `matches`, and `level2`.
- REST fallback for products and candles.
- Upstash Redis (REST) for live caching and scan outputs.
- Fastify API with API key protection for POST routes.
- Luxon + node-cron scheduler for America/Denver scan times.
- FCM push notifications on BUY or SETUP FORMING (>=70 readiness).
- Unit tests for scanner domain logic.

## Environment Variables
| Variable | Description | Default |
| --- | --- | --- |
| NODE_ENV | Environment | production |
| PORT | HTTP port | 8080 |
| API_KEY | API key for protected routes | (required) |
| UPSTASH_REDIS_REST_URL | Upstash REST URL | (required) |
| UPSTASH_REDIS_REST_TOKEN | Upstash REST token | (required) |
| FIREBASE_SERVICE_ACCOUNT_JSON | Firebase Admin SDK service account JSON | (required) |
| COINBASE_WS_URL | Coinbase WS URL | wss://ws-feed.exchange.coinbase.com |
| COINBASE_REST_BASE | Coinbase REST base | https://api.exchange.coinbase.com |
| LIQ_SPREAD_BPS_MAX | Max spread bps | 50 |
| LIQ_DEPTH_USD_MIN | Min depth USD | 50000 |
| TRACK_TOP_N | Top N by volume | 80 |

## Setup
```bash
npm install
```

Create a `.env` file or export env vars:
```bash
export API_KEY=your-key
export UPSTASH_REDIS_REST_URL=https://...
export UPSTASH_REDIS_REST_TOKEN=...
export FIREBASE_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
```

## Running Locally
```bash
npm run dev
```

## Tests
```bash
npm test
```

## Deploy to Fly.io
1. Install Fly CLI: https://fly.io/docs/hands-on/install-flyctl/
2. Create the app and set secrets:
```bash
fly apps create kaos-ev-scanner-backend
fly secrets set API_KEY=... UPSTASH_REDIS_REST_URL=... UPSTASH_REDIS_REST_TOKEN=... FIREBASE_SERVICE_ACCOUNT_JSON=...
fly deploy
```

## Upstash Redis Setup
1. Create an Upstash Redis database.
2. Copy the REST URL and REST token into Fly secrets or `.env`.

## API
### Public
- `GET /health`
- `GET /scan/latest`

### Protected (X-API-KEY)
- `POST /scan/run`
- `POST /device/register` `{ token: string }`
- `POST /device/unregister` `{ token: string }`

## Android Integration
1. Register token:
```bash
curl -X POST $BASE_URL/device/register \
  -H "X-API-KEY: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"token":"<fcm-token>"}'
```
2. Fetch latest scan:
```bash
curl $BASE_URL/scan/latest
```

## Scheduler
Scheduled scans run at 09:00, 12:00, 16:00, and 20:00 America/Denver.

## Notes
- Coinbase market data uses public endpoints only; no Coinbase API keys required.
- Device tokens are stored in Redis under `devices:tokens`.
