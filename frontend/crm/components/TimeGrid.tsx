"use client";

import { useMemo } from "react";
import type { AdminAppointment, AppointmentStatus } from "@/lib/api";

interface Props {
  days: Date[];
  appointments: AdminAppointment[];
  onOpen: (id: string) => void;
  selectedId?: string | null;
}

const STATUS_ABBR: Record<AppointmentStatus, string> = {
  CONFIRMED: "Confirmed",
  CHECKED_IN: "Checked in",
  IN_PROGRESS: "In progress",
  COMPLETED: "Done",
  CANCELLED_BY_CLIENT: "Cancelled",
  CANCELLED_BY_OWNER: "Cancelled",
  NO_SHOW: "No-show",
};

const HOUR_PX = 64;

function isoDate(d: Date): string {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

/** Minutes since midnight from the "HH:MM" portion of a business-local ISO string. */
function minutesOfDay(iso: string): number {
  const [h, m] = iso.slice(11, 16).split(":").map(Number);
  return h * 60 + m;
}

/**
 * Day/week time grid: hours run down the gutter, each day is a column, and every
 * appointment is an absolutely-positioned block sized by its start/end. The backend
 * serializes starts with the salon's timezone offset, so the ISO date and time
 * substrings are already salon-local — no client TZ math needed.
 */
export default function TimeGrid({ days, appointments, onOpen, selectedId }: Props) {
  // Bound the visible window to the appointments present (default 8:00–20:00).
  const { startHour, endHour } = useMemo(() => {
    let min = 8 * 60;
    let max = 20 * 60;
    for (const a of appointments) {
      min = Math.min(min, minutesOfDay(a.start));
      max = Math.max(max, minutesOfDay(a.end));
    }
    return { startHour: Math.floor(min / 60), endHour: Math.ceil(max / 60) };
  }, [appointments]);

  const hours = [];
  for (let h = startHour; h <= endHour; h++) hours.push(h);
  const bodyHeight = (endHour - startHour) * HOUR_PX;

  const byDay = useMemo(() => {
    const map = new Map<string, AdminAppointment[]>();
    for (const d of days) map.set(isoDate(d), []);
    for (const a of appointments) {
      const key = a.start.slice(0, 10);
      if (map.has(key)) map.get(key)!.push(a);
    }
    return map;
  }, [days, appointments]);

  const todayIso = isoDate(new Date());
  const cols = `var(--gutter-px) repeat(${days.length}, 1fr)`;

  return (
    <div className="timegrid">
      <div className="timegrid-header" style={{ gridTemplateColumns: cols }}>
        <div />
        {days.map((d) => {
          const iso = isoDate(d);
          return (
            <div key={iso} className={`day-head${iso === todayIso ? " today" : ""}`}>
              {d.toLocaleDateString("en-US", { weekday: "short" })}
              <span className="num">{d.getDate()}</span>
            </div>
          );
        })}
      </div>

      <div className="timegrid-scroll">
        <div
          className="timegrid-body"
          style={{ gridTemplateColumns: cols, height: bodyHeight, "--hour-px": `${HOUR_PX}px` } as React.CSSProperties}
        >
          <div className="hour-gutter">
            {hours.map((h) => (
              <span key={h} className="hour-label" style={{ top: (h - startHour) * HOUR_PX }}>
                {h === 0 ? "12a" : h < 12 ? `${h}a` : h === 12 ? "12p" : `${h - 12}p`}
              </span>
            ))}
          </div>

          {days.map((d) => {
            const iso = isoDate(d);
            const list = byDay.get(iso) ?? [];
            return (
              <div key={iso} className="day-col" style={{ backgroundPositionY: 0 }}>
                {list.map((a) => {
                  const top = (minutesOfDay(a.start) - startHour * 60) * (HOUR_PX / 60);
                  const height = Math.max(
                    22,
                    (minutesOfDay(a.end) - minutesOfDay(a.start)) * (HOUR_PX / 60) - 2,
                  );
                  return (
                    <button
                      key={a.id}
                      className={`appt-block st-${a.status}`}
                      style={{
                        top,
                        height,
                        outline: selectedId === a.id ? "2px solid var(--rose-600)" : undefined,
                      }}
                      onClick={() => onOpen(a.id)}
                      title={`${a.serviceName} — ${a.client?.name ?? ""}`}
                    >
                      <span className="t">{a.start.slice(11, 16)}</span>
                      <span>{a.serviceName}</span>
                      <span className="c">
                        {a.client?.name ?? "—"} · {STATUS_ABBR[a.status]}
                      </span>
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
