import type { Candle } from "./candles.js";

export function findSupportResistance(candles: Candle[]) {
  const closes = candles.map((c) => c.close);
  const min = Math.min(...closes);
  const max = Math.max(...closes);
  const mid = (min + max) / 2;
  return { support: min, resistance: max, midpoint: mid };
}

export function isNear(value: number, target: number, pct: number) {
  if (target === 0) return false;
  return Math.abs((value - target) / target) * 100 <= pct;
}
