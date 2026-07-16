"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AdminAppointment,
  AdminAppointmentDetail,
  ApiError,
  APPOINTMENT_TRANSITIONS,
  AppointmentStatus,
  changeAppointmentStatus,
  formatCents,
  getAppointment,
  listAppointments,
} from "@/lib/api";
import NewAppointmentForm from "@/components/NewAppointmentForm";

type View = "day" | "week";

function isoDate(d: Date): string {
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function startOfWeek(d: Date): Date {
  const copy = new Date(d);
  const day = (copy.getDay() + 6) % 7; // Monday = 0
  copy.setDate(copy.getDate() - day);
  return copy;
}

function addDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + days);
  return copy;
}

const STATUS_LABELS: Record<AppointmentStatus, string> = {
  CONFIRMED: "Confirmed",
  CHECKED_IN: "Checked in",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED_BY_CLIENT: "Cancelled (client)",
  CANCELLED_BY_OWNER: "Cancelled (owner)",
  NO_SHOW: "No-show",
};

export default function CalendarPage() {
  const [view, setView] = useState<View>("week");
  const [anchor, setAnchor] = useState(() => new Date());
  const [appointments, setAppointments] = useState<AdminAppointment[]>([]);
  const [selected, setSelected] = useState<AdminAppointmentDetail | null>(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const range = useMemo(() => {
    if (view === "day") {
      return { from: isoDate(anchor), to: isoDate(anchor) };
    }
    const monday = startOfWeek(anchor);
    return { from: isoDate(monday), to: isoDate(addDays(monday, 6)) };
  }, [view, anchor]);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setAppointments(await listAppointments(range.from, range.to));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load appointments");
    }
  }, [range]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const open = async (id: string) => {
    try {
      setSelected(await getAppointment(id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load appointment");
    }
  };

  const transition = async (id: string, status: AppointmentStatus) => {
    setError(null);
    try {
      setSelected(await changeAppointmentStatus(id, status));
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Status change failed");
    }
  };

  // Group by the salon-local date, which is the date part of the ISO string
  // (the backend serializes starts with the business timezone offset).
  const days = useMemo(() => {
    const map = new Map<string, AdminAppointment[]>();
    const dayCount = view === "day" ? 1 : 7;
    const first = view === "day" ? anchor : startOfWeek(anchor);
    for (let i = 0; i < dayCount; i++) {
      map.set(isoDate(addDays(first, i)), []);
    }
    for (const a of appointments) {
      const key = a.start.slice(0, 10);
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(a);
    }
    return [...map.entries()];
  }, [appointments, view, anchor]);

  const step = view === "day" ? 1 : 7;

  return (
    <>
      <div className="row" style={{ justifyContent: "space-between", alignItems: "center" }}>
        <h1>Calendar</h1>
        <button className="button" onClick={() => setCreating(true)}>
          New appointment
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="row" style={{ alignItems: "center", gap: "0.5rem", margin: "0.75rem 0" }}>
        <button className="button secondary" onClick={() => setAnchor(addDays(anchor, -step))}>
          ←
        </button>
        <button className="button secondary" onClick={() => setAnchor(new Date())}>
          Today
        </button>
        <button className="button secondary" onClick={() => setAnchor(addDays(anchor, step))}>
          →
        </button>
        <strong style={{ marginLeft: "0.5rem" }}>
          {range.from === range.to ? range.from : `${range.from} → ${range.to}`}
        </strong>
        <span style={{ flex: 1 }} />
        <select value={view} onChange={(e) => setView(e.target.value as View)}>
          <option value="day">Day</option>
          <option value="week">Week</option>
        </select>
      </div>

      <div className={view === "week" ? "calendar-week" : undefined}>
        {days.map(([date, list]) => (
          <div key={date} className="card calendar-day">
            <div className="muted" style={{ marginBottom: "0.5rem", fontWeight: 600 }}>
              {new Date(`${date}T12:00:00`).toLocaleDateString("en-US", {
                weekday: "short",
                month: "short",
                day: "numeric",
              })}
            </div>
            {list.length === 0 && <div className="muted">—</div>}
            {list.map((a) => (
              <button key={a.id} className="calendar-item" onClick={() => open(a.id)}>
                <div style={{ fontWeight: 600 }}>
                  {a.start.slice(11, 16)}–{a.end.slice(11, 16)} {a.serviceName}
                </div>
                <div className="muted">
                  {a.client?.name ?? "?"} · {STATUS_LABELS[a.status]}
                </div>
              </button>
            ))}
          </div>
        ))}
      </div>

      {selected && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <h2>
              {selected.serviceName} — {selected.start.slice(0, 10)} {selected.start.slice(11, 16)}
            </h2>
            <button className="button secondary" onClick={() => setSelected(null)}>
              Close
            </button>
          </div>
          <p className="muted">
            {STATUS_LABELS[selected.status]} · {formatCents(selected.totalCents)}
            {selected.actualStart && <> · started {selected.actualStart.slice(11, 16)}</>}
            {selected.actualEnd && <> · finished {selected.actualEnd.slice(11, 16)}</>}
          </p>

          {selected.client && (
            <p style={{ margin: "0.5rem 0" }}>
              <strong>{selected.client.name}</strong> · {selected.client.phone}
              {selected.client.email && <> · {selected.client.email}</>}
            </p>
          )}
          {selected.notes && <p className="muted">Notes: {selected.notes}</p>}

          <h3 style={{ margin: "0.75rem 0 0.25rem" }}>Items</h3>
          {selected.items.map((item, i) => (
            <div key={i} className="row" style={{ justifyContent: "space-between" }}>
              <span>
                {item.name}
                {item.durationMinutes > 0 && <span className="muted"> ({item.durationMinutes} min)</span>}
              </span>
              <span>{formatCents(item.priceCents)}</span>
            </div>
          ))}

          {APPOINTMENT_TRANSITIONS[selected.status].length > 0 && (
            <div className="row" style={{ gap: "0.5rem", marginTop: "0.75rem", flexWrap: "wrap" }}>
              {APPOINTMENT_TRANSITIONS[selected.status].map((next) => (
                <button key={next} className="button secondary" onClick={() => transition(selected.id, next)}>
                  {STATUS_LABELS[next]}
                </button>
              ))}
            </div>
          )}

          <h3 style={{ margin: "0.75rem 0 0.25rem" }}>History</h3>
          {selected.events.map((event, i) => (
            <div key={i} className="muted" style={{ fontSize: "0.85rem" }}>
              {event.occurredAt.slice(0, 16).replace("T", " ")} · {event.eventType}
              {event.detail && <> — {event.detail}</>} ({event.actor})
            </div>
          ))}
        </div>
      )}

      {creating && (
        <NewAppointmentForm
          onClose={() => setCreating(false)}
          onCreated={async () => {
            setCreating(false);
            await reload();
          }}
        />
      )}
    </>
  );
}
