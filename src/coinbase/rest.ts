import { request } from "undici";
import type { Env } from "../config/env.js";
import { normalizeCandles, type Candle } from "../utils/candles.js";

export type Product = {
  id: string;
  base_currency: string;
  quote_currency: string;
  status: string;
  status_message: string | null;
  trading_disabled: boolean;
  online: boolean;
  auction_mode: boolean;
  cancel_only: boolean;
  limit_only: boolean;
  post_only: boolean;
};

export type ProductTicker = {
  price: string;
  volume: string;
  open_24h: string;
};

export class CoinbaseRestClient {
  constructor(private readonly env: Env) {}

  private async get<T>(path: string): Promise<T> {
    const url = new URL(path, this.env.COINBASE_REST_BASE);
    const res = await request(url, {
      headers: { "User-Agent": "kaos-ev-scanner" }
    });
    if (res.statusCode >= 400) {
      const body = await res.body.text();
      throw new Error(`Coinbase REST error ${res.statusCode}: ${body}`);
    }
    return (await res.body.json()) as T;
  }

  async listProducts(): Promise<Product[]> {
    return this.get<Product[]>("/products");
  }

  async getTicker(productId: string): Promise<ProductTicker> {
    return this.get<ProductTicker>(`/products/${productId}/ticker`);
  }

  async getCandles(productId: string, granularity: number): Promise<Candle[]> {
    const candles = await this.get<number[][]>(
      `/products/${productId}/candles?granularity=${granularity}`
    );
    return normalizeCandles(candles);
  }
}
