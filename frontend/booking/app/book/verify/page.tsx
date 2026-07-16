"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BookingApiError,
  checkPhoneVerification,
  confirmAppointment,
  startPhoneVerification,
} from "@/lib/api";
import {
  clearBooking,
  loadBooking,
  saveConfirmation,
  type BookingState,
} from "@/lib/bookingState";
import HoldCountdown from "@/components/HoldCountdown";

/**
 * Step 4: verify the code, then immediately confirm the appointment. The
 * Idempotency-Key generated when the slot was held makes a retry of this step safe.
 */
export default function VerifyPage() {
  const router = useRouter();
  const [booking, setBooking] = useState<BookingState | null>(null);
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [resent, setResent] = useState(false);

  useEffect(() => {
    const state = loadBooking();
    if (!state?.holdId || !state.phone || !state.clientName) {
      router.replace("/services");
      return;
    }
    setBooking(state);
  }, [router]);

  if (!booking) return null;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitting || !booking.holdId || !booking.idempotencyKey) return;
    setSubmitting(true);
    setError(null);
    try {
      await checkPhoneVerification(booking.phone!, code.trim(), booking.holdId);
    } catch {
      setSubmitting(false);
      setError("That code didn't match (or expired). Check the code and try again.");
      return;
    }
    try {
      const appointment = await confirmAppointment(
        {
          slotHoldId: booking.holdId,
          clientName: booking.clientName!,
          clientEmail: booking.clientEmail,
          notes: booking.notes,
        },
        booking.idempotencyKey,
      );
      saveConfirmation(appointment);
      clearBooking();
      router.push("/book/confirmed");
    } catch (err) {
      setSubmitting(false);
      if (err instanceof BookingApiError && err.status === 409) {
        setError("Your hold expired and the time is no longer available. Please pick a new time.");
      } else if (err instanceof BookingApiError && err.status === 401) {
        setError("Verification expired — request a new code below.");
      } else {
        setError("We couldn't confirm your booking. Please try again.");
      }
    }
  };

  const resend = async () => {
    try {
      await startPhoneVerification(booking.phone!);
      setResent(true);
    } catch {
      setError("Couldn't resend the code. Please go back and check your phone number.");
    }
  };

  return (
    <>
      <p style={{ marginTop: "1.25rem" }}>
        <Link href="/book/details" className="meta">
          ← Back to your details
        </Link>
      </p>
      <h1>Verify your phone</h1>
      <p className="meta" style={{ marginBottom: "1rem" }}>
        Step 4 of 4 — we texted a 6-digit code to {booking.phone}
      </p>
      {booking.holdExpiresAt && <HoldCountdown expiresAt={booking.holdExpiresAt} />}

      {error && <div className="notice">{error}</div>}
      {resent && <p className="meta">A new code is on its way.</p>}

      <form className="detail booking-form" onSubmit={submit}>
        <label>
          Verification code
          <input
            value={code}
            onChange={(e) => setCode(e.target.value)}
            required
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={10}
            style={{ letterSpacing: "0.3em", fontSize: "1.2rem" }}
          />
        </label>
        <button className="button" disabled={submitting}>
          {submitting ? "Confirming…" : "Verify & confirm booking"}
        </button>
        <button type="button" className="linklike" onClick={resend}>
          Resend code
        </button>
      </form>
    </>
  );
}
