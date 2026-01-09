export type Candle = {
  time: number;
  low: number;
  high: number;
  open: number;
  close: number;
  volume: number;
};

export function normalizeCandles(raw: number[][]): Candle[] {
  return raw
    .map(([time, low, high, open, close, volume]) => ({
      time,
      low,
      high,
      open,
      close,
      volume
    }))
    .sort((a, b) => a.time - b.time);
}
