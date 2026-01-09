export type RegimeState = "FALLING" | "BASING" | "TRENDING";

export type ScanState = "BUY" | "SETUP_FORMING" | "NO_TRADE";

export type ScanResult = {
  state: ScanState;
  output: string;
  productId?: string;
  readinessScore?: number;
  reasons?: string[];
  timestamp: number;
};

export type ProductMetrics = {
  productId: string;
  lastPrice: number;
  change24hPct: number;
  vol24hUsd: number;
  rollVol5mUsd: number;
  rollVol15mUsd: number;
  rollVol1hUsd: number;
  spreadBps: number;
  depthUsdTop: number;
  candles1h: number[][];
  candles1d: number[][];
};
