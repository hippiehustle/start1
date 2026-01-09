export class RollingVolume {
  private buckets: { ts: number; value: number }[] = [];
  constructor(private readonly windowMs: number) {}

  add(value: number, ts: number) {
    this.buckets.push({ ts, value });
    this.trim(ts);
  }

  sum(ts: number) {
    this.trim(ts);
    return this.buckets.reduce((acc, cur) => acc + cur.value, 0);
  }

  private trim(now: number) {
    const cutoff = now - this.windowMs;
    while (this.buckets.length > 0 && this.buckets[0].ts < cutoff) {
      this.buckets.shift();
    }
  }
}
