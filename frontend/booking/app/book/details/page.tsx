"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { startPhoneVerification, BookingApiError } from "@/lib/api";
import { loadBooking, saveBooking, type BookingState } from "@/lib/bookingState";
import HoldCountdown from "@/components/HoldCountdown";

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
    <>
      <p style={{ marginTop: "1.25rem" }}>
        <Link href="/book/date-time" className="meta">
          ← Pick a different time
        </Link>
      </p>
      <h1>Your details</h1>
      <p className="meta" style={{ marginBottom: "1rem" }}>
        Step 3 of 4 — {booking.serviceName}
        {booking.start && <> · {new Date(booking.start).toLocaleString()}</>}
      </p>
      {booking.holdExpiresAt && <HoldCountdown expiresAt={booking.holdExpiresAt} />}

      {error && <div className="notice">{error}</div>}

      <form className="detail booking-form" onSubmit={submit}>
        <label>
          Name
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={200} />
        </label>
        <label>
          Mobile phone (we&apos;ll text you a code)
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
        <button className="button" disabled={submitting}>
          {submitting ? "Sending code…" : "Text me a verification code"}
        </button>
      </form>
    </>
  );
}
