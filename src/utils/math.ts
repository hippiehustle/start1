export function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

export function mean(values: number[]) {
  if (values.length === 0) return 0;
  return values.reduce((a, b) => a + b, 0) / values.length;
}

export function pctChange(current: number, prev: number) {
  if (prev === 0) return 0;
  return ((current - prev) / prev) * 100;
}
