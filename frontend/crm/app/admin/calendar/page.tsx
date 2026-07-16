"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AdminAppointment,
  AdminAppointmentDetail,
  ApiError,
  AppointmentStatus,
  changeAppointmentStatus,
  getAppointment,
  listAppointments,
} from "@/lib/api";
import MiniCalendar from "@/components/MiniCalendar";
import TimeGrid from "@/components/TimeGrid";
import AppointmentDrawer from "@/components/AppointmentDrawer";
import NewAppointmentForm from "@/components/NewAppointmentForm";

type View = "day" | "week";

function isoDate(d: Date): string {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function startOfWeek(d: Date): Date {
  const copy = new Date(d);
  const day = (copy.getDay() + 6) % 7; // Monday = 0
  copy.setDate(copy.getDate() - day);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

function addDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + days);
  return copy;
}

const LEGEND: { status: AppointmentStatus; label: string }[] = [
  { status: "CONFIRMED", label: "Confirmed" },
  { status: "CHECKED_IN", label: "Checked in" },
  { status: "IN_PROGRESS", label: "In progress" },
  { status: "COMPLETED", label: "Completed" },
  { status: "NO_SHOW", label: "No-show" },
];

export default function CalendarPage() {
  const [view, setView] = useState<View>("week");
  const [anchor, setAnchor] = useState(() => new Date());
  const [appointments, setAppointments] = useState<AdminAppointment[]>([]);
  const [selected, setSelected] = useState<AdminAppointmentDetail | null>(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const days = useMemo(() => {
    if (view === "day") return [new Date(anchor)];
    const monday = startOfWeek(anchor);
    return Array.from({ length: 7 }, (_, i) => addDays(monday, i));
  }, [view, anchor]);

  const range = useMemo(
    () => ({ from: isoDate(days[0]), to: isoDate(days[days.length - 1]) }),
    [days],
  );

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

  const transition = async (status: AppointmentStatus) => {
    if (!selected) return;
    setError(null);
    try {
      setSelected(await changeAppointmentStatus(selected.id, status));
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Status change failed");
    }
  };

  const step = view === "day" ? 1 : 7;
  const rangeLabel =
    view === "day"
      ? anchor.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })
      : `${days[0].toLocaleDateString("en-US", { month: "short", day: "numeric" })} – ${days[6].toLocaleDateString(
          "en-US",
          { month: "short", day: "numeric" },
        )}`;

  return (
    <>
      <div className="cal-toolbar">
        <h1>Calendar</h1>
        <button className="icon-btn" onClick={() => setAnchor(addDays(anchor, -step))} aria-label="Previous">
          ‹
        </button>
        <button className="secondary small" onClick={() => setAnchor(new Date())}>
          Today
        </button>
        <button className="icon-btn" onClick={() => setAnchor(addDays(anchor, step))} aria-label="Next">
          ›
        </button>
        <strong style={{ marginLeft: "0.4rem" }}>{rangeLabel}</strong>
        <span className="spacer" />
        <div className="seg">
          <button className={view === "day" ? "on" : ""} onClick={() => setView("day")}>
            Day
          </button>
          <button className={view === "week" ? "on" : ""} onClick={() => setView("week")}>
            Week
          </button>
        </div>
        <button onClick={() => setCreating(true)}>+ New</button>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="cal-layout">
        <div>
          <MiniCalendar selected={anchor} onSelect={(d) => setAnchor(d)} />
          <div className="legend">
            {LEGEND.map((l) => (
              <span key={l.status}>
                <span className={`dot st-${l.status}`} />
                {l.label}
              </span>
            ))}
          </div>
        </div>

        <TimeGrid days={days} appointments={appointments} onOpen={open} selectedId={selected?.id} />
      </div>

      {selected && (
        <AppointmentDrawer
          appointment={selected}
          onClose={() => setSelected(null)}
          onTransition={transition}
        />
      )}

      {creating && (
        <NewAppointmentForm
          defaultDate={isoDate(anchor)}
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
