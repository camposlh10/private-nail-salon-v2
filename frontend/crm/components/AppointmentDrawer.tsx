"use client";

import {
  AdminAppointmentDetail,
  APPOINTMENT_TRANSITIONS,
  AppointmentStatus,
  formatCents,
} from "@/lib/api";

interface Props {
  appointment: AdminAppointmentDetail;
  onClose: () => void;
  onTransition: (status: AppointmentStatus) => void;
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

export default function AppointmentDrawer({ appointment, onClose, onTransition }: Props) {
  const day = new Date(`${appointment.start.slice(0, 10)}T12:00:00`).toLocaleDateString("en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
  });

  return (
    <>
      <div className="drawer-backdrop" onClick={onClose} />
      <aside className="drawer" role="dialog" aria-label="Appointment details">
        <button className="icon-btn close" onClick={onClose} aria-label="Close">
          ✕
        </button>
        <span className={`badge`} style={{ background: "var(--rose-100)", color: "var(--rose-700)" }}>
          {STATUS_LABELS[appointment.status]}
        </span>
        <h2 style={{ marginTop: "0.5rem" }}>{appointment.serviceName}</h2>
        <p className="muted">
          {day} · {appointment.start.slice(11, 16)}–{appointment.end.slice(11, 16)}
        </p>

        {appointment.client && (
          <div className="section">
            <strong>{appointment.client.name}</strong>
            <div className="muted">{appointment.client.phone}</div>
            {appointment.client.email && <div className="muted">{appointment.client.email}</div>}
          </div>
        )}

        {appointment.notes && (
          <div className="section">
            <div className="muted" style={{ marginBottom: "0.25rem" }}>
              Notes
            </div>
            {appointment.notes}
          </div>
        )}

        <div className="section">
          <div className="muted" style={{ marginBottom: "0.4rem" }}>
            Items
          </div>
          {appointment.items.map((item, i) => (
            <div key={i} className="line">
              <span>
                {item.name}
                {item.durationMinutes > 0 && (
                  <span className="muted"> · {item.durationMinutes} min</span>
                )}
              </span>
              <span>{formatCents(item.priceCents)}</span>
            </div>
          ))}
          <div className="line" style={{ fontWeight: 700, borderTop: "1px solid var(--border)", marginTop: "0.3rem", paddingTop: "0.4rem" }}>
            <span>Total</span>
            <span>{formatCents(appointment.totalCents)}</span>
          </div>
        </div>

        {(appointment.actualStart || appointment.actualEnd) && (
          <div className="section muted">
            {appointment.actualStart && <>Started {appointment.actualStart.slice(11, 16)} </>}
            {appointment.actualEnd && <>· Finished {appointment.actualEnd.slice(11, 16)}</>}
          </div>
        )}

        {APPOINTMENT_TRANSITIONS[appointment.status].length > 0 && (
          <div className="section">
            <div className="muted" style={{ marginBottom: "0.4rem" }}>
              Update status
            </div>
            <div className="actions" style={{ marginTop: 0 }}>
              {APPOINTMENT_TRANSITIONS[appointment.status].map((next) => (
                <button key={next} className="secondary small" onClick={() => onTransition(next)}>
                  {STATUS_LABELS[next]}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="section">
          <div className="muted" style={{ marginBottom: "0.4rem" }}>
            History
          </div>
          {appointment.events.map((event, i) => (
            <div key={i} className="muted" style={{ fontSize: "0.8rem", marginBottom: "0.2rem" }}>
              {event.occurredAt.slice(0, 16).replace("T", " ")} · {event.eventType}
              {event.detail && <> — {event.detail}</>}
            </div>
          ))}
        </div>
      </aside>
    </>
  );
}
