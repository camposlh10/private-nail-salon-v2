"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { formatCents, formatDuration, type BookedAppointment } from "@/lib/api";
import { loadConfirmation } from "@/lib/bookingState";

export default function ConfirmedPage() {
  const [appointment, setAppointment] = useState<BookedAppointment | null | undefined>(undefined);

  useEffect(() => {
    setAppointment(loadConfirmation<BookedAppointment>());
  }, []);

  if (appointment === undefined) return null;

  if (appointment === null) {
    return (
      <div className="flow">
        <div className="success-head">
          <h1>No booking in progress</h1>
          <p style={{ margin: "0.75rem 0 1.5rem" }}>Start by choosing a service.</p>
          <Link href="/services" className="button">
            Browse services
          </Link>
        </div>
      </div>
    );
  }

  const start = new Date(appointment.start);

  return (
    <div className="flow">
      <div className="success-head">
        <div className="success-check">✓</div>
        <h1>You&apos;re booked!</h1>
        <p className="meta" style={{ marginTop: "0.4rem" }}>
          A confirmation is on its way by text (and email if you gave one).
        </p>
      </div>

      <div className="panel">
        <div className="booking-recap">
          <span className="icon">🗓️</span>
          <span>
            <strong>{appointment.serviceName}</strong>
            <span className="meta" style={{ display: "block" }}>
              {start.toLocaleDateString("en-US", {
                weekday: "long",
                month: "long",
                day: "numeric",
                year: "numeric",
              })}{" "}
              at {start.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })}
            </span>
          </span>
        </div>

        {appointment.items.map((item, i) => (
          <div key={i} className="line-item">
            <div>
              <div className="card-title">{item.name}</div>
              {item.durationMinutes > 0 && (
                <div className="meta">{formatDuration(item.durationMinutes)}</div>
              )}
            </div>
            <div className="price">
              {item.priceCents > 0 ? formatCents(item.priceCents, appointment.currency) : "—"}
            </div>
          </div>
        ))}

        <div className="summary-bar">
          <div className="totals">
            <span className="label">Total</span>
            <span className="value">{formatCents(appointment.totalCents, appointment.currency)}</span>
          </div>
          <Link href="/services" className="button ghost">
            Done
          </Link>
        </div>
      </div>
    </div>
  );
}
