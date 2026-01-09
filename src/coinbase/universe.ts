import type { Logger } from "pino";
import type { CoinbaseRestClient } from "./rest.js";

export type UniverseSnapshot = {
  productIds: string[];
  volumes: Record<string, number>;
  lastUpdated: number;
};

export class ProductUniverse {
  private snapshot: UniverseSnapshot = {
    productIds: [],
    volumes: {},
    lastUpdated: 0
  };

  constructor(
    private readonly rest: CoinbaseRestClient,
    private readonly log: Logger,
    private readonly topN: number
  ) {}

  async refresh(): Promise<UniverseSnapshot> {
    const products = await this.rest.listProducts();
    const tradable = products.filter(
      (p) =>
        p.id.endsWith("-USD") &&
        !p.trading_disabled &&
        p.status === "online"
    );

    const volumes: Record<string, number> = {};
    for (const product of tradable) {
      try {
        const ticker = await this.rest.getTicker(product.id);
        const price = Number(ticker.price);
        const vol = Number(ticker.volume);
        volumes[product.id] = price * vol;
      } catch (error) {
        this.log.warn({ err: error, product: product.id }, "Ticker fetch failed");
      }
    }

    const sorted = Object.entries(volumes)
      .sort((a, b) => b[1] - a[1])
      .map(([id]) => id);

    const alwaysInclude = ["BTC-USD", "ETH-USD"];
    const top = sorted.filter((id) => !alwaysInclude.includes(id)).slice(0, this.topN);
    const productIds = [...alwaysInclude, ...top];

    this.snapshot = {
      productIds,
      volumes,
      lastUpdated: Date.now()
    };

    this.log.info({ count: productIds.length }, "Universe refreshed");
    return this.snapshot;
  }

  getSnapshot() {
    return this.snapshot;
  }
}
