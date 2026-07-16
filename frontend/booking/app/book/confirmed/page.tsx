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
      <>
        <h1>No booking in progress</h1>
        <p style={{ margin: "0.75rem 0 1.5rem" }}>Start by choosing a service.</p>
        <Link href="/services" className="button">
          Browse services
        </Link>
      </>
    );
  }

  const start = new Date(appointment.start);

  return (
    <>
      <h1>You&apos;re booked! 💅</h1>
      <p style={{ margin: "0.75rem 0 1.25rem" }}>
        We sent a confirmation by text{appointment.items.length > 0 ? " (and email if you gave one)" : ""}.
      </p>

      <div className="detail">
        <h2>{appointment.serviceName}</h2>
        <p className="meta" style={{ marginBottom: "0.75rem" }}>
          {start.toLocaleDateString("en-US", {
            weekday: "long",
            month: "long",
            day: "numeric",
            year: "numeric",
          })}{" "}
          at{" "}
          {start.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })}
        </p>
        {appointment.items.map((item, i) => (
          <div key={i} className="addon">
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
        <div className="booking-summary">
          <span>
            Total: <strong>{formatCents(appointment.totalCents, appointment.currency)}</strong>
          </span>
          <Link href="/services" className="button">
            Done
          </Link>
        </div>
      </div>
    </>
  );
}
