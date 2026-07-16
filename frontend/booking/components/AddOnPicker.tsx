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
    <div className="panel">
      <h2 style={{ marginBottom: "0.8rem" }}>
        {service.addOns.length === 0 ? "No add-ons for this service" : "Make it extra special"}
      </h2>
      {service.addOns.length === 0 ? (
        <p className="meta">Continue to pick a time that suits you.</p>
      ) : (
        service.addOns.map((a) => (
          <label key={a.id} className={`addon-card${selected.has(a.id) ? " selected" : ""}`}>
            <span className="addon-main">
              <input type="checkbox" checked={selected.has(a.id)} onChange={() => toggle(a.id)} />
              <span>
                <span className="card-title" style={{ display: "block" }}>
                  {a.name}
                </span>
                {a.description && <span className="meta">{a.description}</span>}
                {a.addedDurationMinutes > 0 && (
                  <span className="meta" style={{ display: "block" }}>
                    +{formatDuration(a.addedDurationMinutes)}
                  </span>
                )}
              </span>
            </span>
            <span className="price">
              {a.price.amountCents > 0 ? formatCents(a.price.amountCents, a.price.currency) : "Included"}
            </span>
          </label>
        ))
      )}

      <div className="summary-bar">
        <div className="totals">
          <span className="label">{formatDuration(totals.duration)}</span>
          <span className="value">
            {totals.cents > 0 ? formatCents(totals.cents, service.price.currency) : "Free"}
            {service.priceType === "STARTING_AT" && totals.cents > 0 ? "+" : ""}
          </span>
        </div>
        <button className="button" onClick={proceed}>
          Choose date &amp; time →
        </button>
      </div>
    </div>
  );
}
