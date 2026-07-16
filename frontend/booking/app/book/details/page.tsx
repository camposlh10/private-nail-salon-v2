"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { startPhoneVerification, BookingApiError } from "@/lib/api";
import { loadBooking, saveBooking, type BookingState } from "@/lib/bookingState";
import HoldCountdown from "@/components/HoldCountdown";
import Steps from "@/components/Steps";

/** Step 3: contact details; submitting sends the verification code. */
export default function DetailsPage() {
  const router = useRouter();
  const [booking, setBooking] = useState<BookingState | null>(null);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const state = loadBooking();
    if (!state?.holdId) {
      router.replace("/services");
      return;
    }
    setBooking(state);
    setName(state.clientName ?? "");
    setPhone(state.phone ?? "");
    setEmail(state.clientEmail ?? "");
    setNotes(state.notes ?? "");
  }, [router]);

  if (!booking) return null;

  const start = booking.start ? new Date(booking.start) : null;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      await startPhoneVerification(phone);
      saveBooking({
        ...booking,
        clientName: name,
        phone,
        clientEmail: email || undefined,
        notes: notes || undefined,
      });
      router.push("/book/verify");
    } catch (err) {
      setSubmitting(false);
      setError(
        err instanceof BookingApiError && err.status === 400
          ? "Please enter your phone in international format, e.g. +1 555 010 0200."
          : "We couldn't send the verification code. Please try again.",
      );
    }
  };

  return (
    <div className="flow">
      <Link href="/book/date-time" className="back-link">
        ← Pick a different time
      </Link>
      <h1 className="flow-title">Your details</h1>
      <Steps current={3} />

      <div className="panel">
        <div className="booking-recap">
          <span className="icon">🗓️</span>
          <span>
            <strong>{booking.serviceName}</strong>
            {start && (
              <span className="meta" style={{ display: "block" }}>
                {start.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })}{" "}
                at {start.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })}
              </span>
            )}
          </span>
        </div>
        {booking.holdExpiresAt && <HoldCountdown expiresAt={booking.holdExpiresAt} />}

        {error && <div className="notice warn">{error}</div>}

        <form className="booking-form" onSubmit={submit}>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={200} />
          </label>
          <label>
            Mobile phone — we&apos;ll text you a code
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
              placeholder="+1 555 010 0200"
              inputMode="tel"
            />
          </label>
          <label>
            Email (optional, for your confirmation)
            <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" maxLength={320} />
          </label>
          <label>
            Notes (optional)
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={2000} rows={3} />
          </label>
          <button className="button block" disabled={submitting}>
            {submitting ? "Sending code…" : "Text me a verification code"}
          </button>
        </form>
      </div>
    </div>
  );
}
