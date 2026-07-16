"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { formatCents, formatDuration, type PublicServiceDetail } from "@/lib/api";
import { saveBooking } from "@/lib/bookingState";

export default function AddOnPicker({ service }: { service: PublicServiceDetail }) {
  const router = useRouter();
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const totals = useMemo(() => {
    const chosen = service.addOns.filter((a) => selected.has(a.id));
    return {
      duration: service.durationMinutes + chosen.reduce((s, a) => s + a.addedDurationMinutes, 0),
      cents: service.price.amountCents + chosen.reduce((s, a) => s + a.price.amountCents, 0),
    };
  }, [service, selected]);

  const toggle = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const proceed = () => {
    const chosen = service.addOns.filter((a) => selected.has(a.id));
    saveBooking({
      serviceId: service.id,
      serviceSlug: service.slug,
      serviceName: service.name,
      addOnIds: chosen.map((a) => a.id),
      addOnNames: chosen.map((a) => a.name),
      durationMinutes: totals.duration,
      totalCents: totals.cents,
      currency: service.price.currency || "USD",
    });
    router.push("/book/date-time");
  };

  return (
    <div className="detail">
      {service.addOns.length === 0 ? (
        <p className="meta">No add-ons for this service — continue to pick a time.</p>
      ) : (
        service.addOns.map((a) => (
          <label key={a.id} className="addon" style={{ cursor: "pointer" }}>
            <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
              <input
                type="checkbox"
                checked={selected.has(a.id)}
                onChange={() => toggle(a.id)}
              />
              <div>
                <div className="card-title">{a.name}</div>
                {a.description && <div className="meta">{a.description}</div>}
                {a.addedDurationMinutes > 0 && (
                  <div className="meta">+{formatDuration(a.addedDurationMinutes)}</div>
                )}
              </div>
            </div>
            <div className="price">
              {a.price.amountCents > 0 ? formatCents(a.price.amountCents, a.price.currency) : "Included"}
            </div>
          </label>
        ))
      )}

      <div className="booking-summary">
        <span>
          Total: <strong>{formatDuration(totals.duration)}</strong>
          {totals.cents > 0 && (
            <>
              {" · "}
              <strong>{formatCents(totals.cents, service.price.currency)}</strong>
              {service.priceType === "STARTING_AT" && <span className="meta"> (starting at)</span>}
            </>
          )}
        </span>
        <button className="button" onClick={proceed}>
          Choose date &amp; time
        </button>
      </div>
    </div>
  );
}
