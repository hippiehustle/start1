import WebSocket from "ws";
import type { Env } from "../config/env.js";
import type { Logger } from "pino";

export type CoinbaseMessage = Record<string, unknown> & { type: string };

export type WsHandlers = {
  onMessage: (msg: CoinbaseMessage) => void;
  onOpen?: () => void;
  onClose?: () => void;
};

export class CoinbaseWsClient {
  private ws: WebSocket | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private backoffMs = 1000;
  private productIds: string[] = [];

  constructor(
    private readonly env: Env,
    private readonly log: Logger,
    private readonly handlers: WsHandlers
  ) {}

  connect() {
    this.clearReconnect();
    this.ws = new WebSocket(this.env.COINBASE_WS_URL);
    this.ws.on("open", () => {
      this.log.info("Coinbase WS connected");
      this.backoffMs = 1000;
      this.handlers.onOpen?.();
      if (this.productIds.length > 0) {
        this.subscribe(this.productIds);
      }
    });
    this.ws.on("message", (data) => {
      try {
        const parsed = JSON.parse(data.toString()) as CoinbaseMessage;
        this.handlers.onMessage(parsed);
      } catch (error) {
        this.log.warn({ err: error }, "Failed to parse WS message");
      }
    });
    this.ws.on("close", () => {
      this.log.warn("Coinbase WS disconnected");
      this.handlers.onClose?.();
      this.scheduleReconnect();
    });
    this.ws.on("error", (error) => {
      this.log.error({ err: error }, "Coinbase WS error");
      this.ws?.close();
    });
  }

  isConnected() {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  setProducts(productIds: string[]) {
    this.productIds = [...new Set(productIds)];
    if (this.isConnected()) {
      this.unsubscribeAll();
      this.subscribe(this.productIds);
    }
  }

  subscribe(productIds: string[]) {
    if (!this.ws || !this.isConnected()) return;
    const payload = {
      type: "subscribe",
      product_ids: productIds,
      channels: ["ticker", "matches", "level2"]
    };
    this.ws.send(JSON.stringify(payload));
  }

  unsubscribeAll() {
    if (!this.ws || !this.isConnected()) return;
    const payload = {
      type: "unsubscribe",
      product_ids: this.productIds,
      channels: ["ticker", "matches", "level2"]
    };
    this.ws.send(JSON.stringify(payload));
  }

  close() {
    this.clearReconnect();
    this.ws?.close();
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return;
    const delay = this.backoffMs;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.backoffMs = Math.min(this.backoffMs * 2, 30000);
      this.connect();
    }, delay);
  }

  private clearReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }
}
