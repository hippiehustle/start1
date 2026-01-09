import cron from "node-cron";
import { DateTime } from "luxon";
import type { Logger } from "pino";

export type ScheduleCallback = () => Promise<void>;

const TIMEZONE = "America/Denver";

export function startScheduler(log: Logger, runScan: ScheduleCallback) {
  const schedules = ["0 9 * * *", "0 12 * * *", "0 16 * * *", "0 20 * * *"];

  schedules.forEach((pattern) => {
    cron.schedule(
      pattern,
      async () => {
        const now = DateTime.now().setZone(TIMEZONE);
        log.info({ now: now.toISO() }, "Scheduled scan triggered");
        await runScan();
      },
      { timezone: TIMEZONE }
    );
  });

  log.info("Scheduler started for America/Denver");
}
