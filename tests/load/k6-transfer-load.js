import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

const WALLET_URL = __ENV.WALLET_URL || 'http://localhost:8083';
const TXN_URL = __ENV.TXN_URL || 'http://localhost:8084';
const INTERNAL_TOKEN = __ENV.INTERNAL_TOKEN || 'dev-internal-token';

export function setup() {
  const walletRes = http.post(
    `${WALLET_URL}/api/v1/wallets`,
    JSON.stringify({ currency: 'CDF', type: 'PERSONAL', label: 'Load test' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(walletRes, { 'wallet created': (r) => r.status === 201 || r.status === 200 });

  let walletId = null;
  if (walletRes.status === 201) {
    walletId = JSON.parse(walletRes.body).walletId;
  } else {
    const list = http.get(`${WALLET_URL}/api/v1/wallets`);
    const wallets = JSON.parse(list.body);
    walletId = wallets[0]?.walletId;
  }

  const wallet2Res = http.post(
    `${WALLET_URL}/api/v1/wallets`,
    JSON.stringify({ currency: 'USD', type: 'PERSONAL', label: 'Load test dest' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  let destWalletId = walletId;
  if (wallet2Res.status === 201) {
    destWalletId = JSON.parse(wallet2Res.body).walletId;
  }

  if (walletId) {
    http.post(
      `${WALLET_URL}/internal/wallets/${walletId}/credit`,
      JSON.stringify({
        transactionId: uuidv4(),
        amount: '1000000.00',
        description: 'Load test funding',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'X-Internal-Service-Token': INTERNAL_TOKEN,
        },
      }
    );
  }

  return { sourceWalletId: walletId, destWalletId };
}

export default function (data) {
  if (!data.sourceWalletId || !data.destWalletId) {
    sleep(1);
    return;
  }

  const idempotencyKey = uuidv4();

  const transferRes = http.post(
    `${TXN_URL}/api/v1/transactions/transfer`,
    JSON.stringify({
      sourceWalletId: data.sourceWalletId,
      destinationWalletId: data.destWalletId,
      amount: '100.00',
      currency: 'CDF',
      description: 'k6 load test transfer',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
    }
  );

  check(transferRes, {
    'transfer status 201 or 409': (r) => r.status === 201 || r.status === 409,
    'transfer latency < 500ms': (r) => r.timings.duration < 500,
  });

  const balanceRes = http.get(
    `${WALLET_URL}/api/v1/wallets/${data.sourceWalletId}/balance`
  );
  check(balanceRes, {
    'balance status 200': (r) => r.status === 200,
  });

  sleep(0.1);
}

export function handleSummary(data) {
  return {
  'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, opts) {
  const p99 = data.metrics.http_req_duration?.values?.['p(99)'] || 0;
  const rps = data.metrics.http_reqs?.values?.rate || 0;
  return `\n=== VillageSat Load Test Summary ===\np99 latency: ${p99.toFixed(2)}ms\nRPS: ${rps.toFixed(2)}\n`;
}
