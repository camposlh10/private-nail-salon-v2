"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  BookingApiError,
  createSlotHold,
  formatDay,
  formatDuration,
  formatSlot,
  getAvailability,
  type AvailabilityDay,
} from "@/lib/api";
import { loadBooking, saveBooking, type BookingState } from "@/lib/bookingState";

const RANGE_DAYS = 14;

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

/**
 * Step 2: pick a start time. The list comes exclusively from the backend
 * availability endpoint — this page never decides on its own whether a time is
 * valid, it only renders what the server said and immediately re-asks on conflict.
 */
export default function DateTimePage() {
  const router = useRouter();
  const [booking, setBooking] = useState<BookingState | null>(null);
  const [days, setDays] = useState<AvailabilityDay[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [holding, setHolding] = useState<string | null>(null);

  const refresh = useCallback(async (b: BookingState) => {
    const from = new Date();
    const to = new Date(from.getTime() + (RANGE_DAYS - 1) * 86_400_000);
    try {
      const availability = await getAvailability(b.serviceId, b.addOnIds, isoDate(from), isoDate(to));
      setDays(availability.days);
    } catch {
      setError("We couldn't load available times. Please try again.");
    }
  }, []);

  useEffect(() => {
    const state = loadBooking();
    if (!state) {
      router.replace("/services");
      return;
    }
    setBooking(state);
    void refresh(state);
  }, [router, refresh]);

  const pick = async (slot: string) => {
    if (!booking || holding) return;
    setHolding(slot);
    setError(null);
    try {
      const hold = await createSlotHold(booking.serviceId, booking.addOnIds, slot);
      saveBooking({
        ...booking,
        holdId: hold.id,
        start: hold.start,
        end: hold.end,
        holdExpiresAt: hold.expiresAt,
        idempotencyKey: crypto.randomUUID(),
      });
      router.push("/book/details");
    } catch (err) {
      setHolding(null);
      if (err instanceof BookingApiError && err.status === 409) {
        setError("Sorry, that time was just taken — here are the latest openings.");
        void refresh(booking);
      } else {
        setError("Something went wrong reserving that time. Please try again.");
      }
    }
  };

  if (!booking) return null;

  const openDays = (days ?? []).filter((d) => d.slots.length > 0);

  return (
    <>
      <p style={{ marginTop: "1.25rem" }}>
        <Link href={`/book/${booking.serviceSlug}`} className="meta">
          ← Back to add-ons
        </Link>
      </p>
      <h1>Pick a time</h1>
      <p className="meta" style={{ marginBottom: "1rem" }}>
        Step 2 of 4 — {booking.serviceName}
        {booking.addOnNames.length > 0 && <> + {booking.addOnNames.join(", ")}</>} (
        {formatDuration(booking.durationMinutes)})
      </p>

      {error && <div className="notice">{error}</div>}

      {days === null ? (
        <p className="meta">Loading available times…</p>
      ) : openDays.length === 0 ? (
        <p className="meta">
          No openings in the next {RANGE_DAYS} days. Please check back soon or contact us directly.
        </p>
      ) : (
        openDays.map((day) => (
          <div key={day.date} className="slot-day">
            <h2>{formatDay(day.date)}</h2>
            <div className="slot-grid">
              {day.slots.map((slot) => (
                <button
                  key={slot}
                  className="slot"
                  disabled={holding !== null}
                  onClick={() => pick(slot)}
                >
                  {holding === slot ? "Reserving…" : formatSlot(slot)}
                </button>
              ))}
            </div>
          </div>
        ))
      )}
    </>
  );
}
