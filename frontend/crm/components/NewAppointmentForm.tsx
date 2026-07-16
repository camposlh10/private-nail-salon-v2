"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AdminService,
  ApiError,
  createAppointment,
  getAvailability,
  listServices,
} from "@/lib/api";

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

function todayIso(): string {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

/**
 * Owner-created appointment. Slots come from the same availability endpoint the
 * public site uses, so owner bookings obey identical rules (the backend re-checks
 * regardless — this just avoids offering times that will be rejected).
 */
export default function NewAppointmentForm({ onClose, onCreated }: Props) {
  const [services, setServices] = useState<AdminService[]>([]);
  const [serviceId, setServiceId] = useState("");
  const [addOnIds, setAddOnIds] = useState<Set<string>>(new Set());
  const [date, setDate] = useState(todayIso());
  const [slots, setSlots] = useState<string[]>([]);
  const [slot, setSlot] = useState("");
  const [clientName, setClientName] = useState("");
  const [clientPhone, setClientPhone] = useState("");
  const [clientEmail, setClientEmail] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    listServices(0, 100, "ACTIVE")
      .then((page) => {
        setServices(page.content);
        if (page.content.length > 0) setServiceId(page.content[0].id);
      })
      .catch(() => setError("Failed to load services"));
  }, []);

  const service = useMemo(
    () => services.find((s) => s.id === serviceId),
    [services, serviceId],
  );

  const refreshSlots = useCallback(async () => {
    if (!serviceId || !date) return;
    try {
      const availability = await getAvailability(serviceId, [...addOnIds], date, date);
      const day = availability.days.find((d) => d.date === date);
      setSlots(day?.slots ?? []);
      setSlot("");
    } catch {
      setSlots([]);
    }
  }, [serviceId, date, addOnIds]);

  useEffect(() => {
    void refreshSlots();
  }, [refreshSlots]);

  const toggleAddOn = (id: string) => {
    setAddOnIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitting || !slot) return;
    setSubmitting(true);
    setError(null);
    try {
      await createAppointment({
        serviceId,
        addOnIds: [...addOnIds],
        start: slot,
        clientName,
        clientPhone,
        clientEmail: clientEmail || undefined,
        notes: notes || undefined,
      });
      onCreated();
    } catch (err) {
      setSubmitting(false);
      setError(err instanceof ApiError ? err.message : "Could not create the appointment");
    }
  };

  return (
    <div className="card" style={{ marginTop: "1rem" }}>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h2>New appointment</h2>
        <button className="button secondary" onClick={onClose}>
          Cancel
        </button>
      </div>
      {error && <div className="error">{error}</div>}

      <form onSubmit={submit} className="row" style={{ flexDirection: "column", gap: "0.7rem" }}>
        <label>
          Service
          <select value={serviceId} onChange={(e) => setServiceId(e.target.value)}>
            {services.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.durationMinutes} min)
              </option>
            ))}
          </select>
        </label>

        {service && service.addOns.filter((a) => a.status === "ACTIVE").length > 0 && (
          <fieldset style={{ border: "none" }}>
            <legend className="muted">Add-ons</legend>
            {service.addOns
              .filter((a) => a.status === "ACTIVE")
              .map((a) => (
                <label key={a.id} style={{ display: "block" }}>
                  <input
                    type="checkbox"
                    checked={addOnIds.has(a.id)}
                    onChange={() => toggleAddOn(a.id)}
                  />{" "}
                  {a.name}
                  {a.addedDurationMinutes > 0 && ` (+${a.addedDurationMinutes} min)`}
                </label>
              ))}
          </fieldset>
        )}

        <label>
          Date
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </label>

        <label>
          Time
          <select value={slot} onChange={(e) => setSlot(e.target.value)} required>
            <option value="">{slots.length === 0 ? "No openings this day" : "Pick a time"}</option>
            {slots.map((s) => (
              <option key={s} value={s}>
                {s.slice(11, 16)}
              </option>
            ))}
          </select>
        </label>

        <label>
          Client name
          <input value={clientName} onChange={(e) => setClientName(e.target.value)} required maxLength={200} />
        </label>
        <label>
          Client phone (E.164)
          <input
            value={clientPhone}
            onChange={(e) => setClientPhone(e.target.value)}
            required
            placeholder="+15550100200"
          />
        </label>
        <label>
          Client email (optional)
          <input value={clientEmail} onChange={(e) => setClientEmail(e.target.value)} type="email" />
        </label>
        <label>
          Notes (optional)
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} maxLength={2000} />
        </label>

        <button className="button" disabled={submitting || !slot}>
          {submitting ? "Creating…" : "Create appointment"}
        </button>
      </form>
    </div>
  );
}
