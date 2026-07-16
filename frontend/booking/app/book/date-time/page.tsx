"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  BookingApiError,
  createSlotHold,
  formatDuration,
  formatSlot,
  getAvailability,
  type AvailabilityDay,
} from "@/lib/api";
import { loadBooking, saveBooking, type BookingState } from "@/lib/bookingState";
import Steps from "@/components/Steps";

const RANGE_DAYS = 14;

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function dayChipParts(dateIso: string): { dow: string; num: string } {
  const d = new Date(`${dateIso}T12:00:00`);
  return {
    dow: d.toLocaleDateString("en-US", { weekday: "short" }),
    num: String(d.getDate()),
  };
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
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [holding, setHolding] = useState<string | null>(null);

  const refresh = useCallback(async (b: BookingState) => {
    const from = new Date();
    const to = new Date(from.getTime() + (RANGE_DAYS - 1) * 86_400_000);
    try {
      const availability = await getAvailability(b.serviceId, b.addOnIds, isoDate(from), isoDate(to));
      setDays(availability.days);
      setSelectedDate((current) => {
        if (current && availability.days.some((d) => d.date === current && d.slots.length > 0)) {
          return current;
        }
        return availability.days.find((d) => d.slots.length > 0)?.date ?? null;
      });
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

  const selectedDay = useMemo(
    () => (days ?? []).find((d) => d.date === selectedDate) ?? null,
    [days, selectedDate],
  );

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

  const anyOpen = (days ?? []).some((d) => d.slots.length > 0);

  return (
    <div className="flow">
      <Link href={`/book/${booking.serviceSlug}`} className="back-link">
        ← Back to add-ons
      </Link>
      <h1 className="flow-title">Pick a time</h1>
      <Steps current={2} />

      <div className="panel">
        <div className="booking-recap">
          <span className="icon">💅</span>
          <span>
            <strong>{booking.serviceName}</strong>
            {booking.addOnNames.length > 0 && <> + {booking.addOnNames.join(", ")}</>}
            <span className="meta" style={{ display: "block" }}>
              {formatDuration(booking.durationMinutes)}
            </span>
          </span>
        </div>

        {error && <div className="notice warn">{error}</div>}

        {days === null ? (
          <p className="meta">Loading available times…</p>
        ) : !anyOpen ? (
          <p className="meta">
            No openings in the next {RANGE_DAYS} days. Please check back soon or contact us directly.
          </p>
        ) : (
          <>
            <div className="day-strip">
              {days.map((day) => {
                const parts = dayChipParts(day.date);
                return (
                  <button
                    key={day.date}
                    className={`day-chip${day.date === selectedDate ? " active" : ""}`}
                    disabled={day.slots.length === 0}
                    onClick={() => setSelectedDate(day.date)}
                  >
                    <span className="dow">{parts.dow}</span>
                    <span className="num">{parts.num}</span>
                  </button>
                );
              })}
            </div>

            {selectedDay && (
              <>
                <h2 style={{ margin: "0.6rem 0 0.2rem" }}>
                  {new Date(`${selectedDay.date}T12:00:00`).toLocaleDateString("en-US", {
                    weekday: "long",
                    month: "long",
                    day: "numeric",
                  })}
                </h2>
                <div className="slot-grid">
                  {selectedDay.slots.map((slot) => (
                    <button
                      key={slot}
                      className="slot"
                      disabled={holding !== null}
                      onClick={() => pick(slot)}
                    >
                      {holding === slot ? "…" : formatSlot(slot)}
                    </button>
                  ))}
                </div>
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
