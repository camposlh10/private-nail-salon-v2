"use client";

import { useEffect, useState } from "react";
import { PublicBusiness, publicBusiness } from "@/lib/api";

export default function BusinessSettingsPage() {
  const [business, setBusiness] = useState<PublicBusiness | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    publicBusiness()
      .then(setBusiness)
      .catch(() => setError("Failed to load business profile"));
  }, []);

  if (error) return <div className="error">{error}</div>;
  if (!business) return <p className="muted">Loading…</p>;

  return (
    <>
      <h1>Business settings</h1>
      <div className="card">
        <div className="row">
          <div>
            <label>Name</label>
            <p>{business.name}</p>
          </div>
          <div>
            <label>Timezone</label>
            <p>{business.timezone}</p>
          </div>
          <div>
            <label>Currency</label>
            <p>{business.currency}</p>
          </div>
        </div>
        <div className="row">
          <div>
            <label>Phone</label>
            <p>{business.phone ?? "—"}</p>
          </div>
          <div>
            <label>Email</label>
            <p>{business.email ?? "—"}</p>
          </div>
        </div>
        <label>Address</label>
        <p>{business.address ?? "—"}</p>
        <label>Appointment start notice ({business.appointmentStartWindowMinutes} min window)</label>
        <p>{business.appointmentStartNotice ?? "—"}</p>
      </div>
      <p className="muted">
        Editing the business profile from the CRM arrives in a later milestone — for now these
        values are managed directly in the database.
      </p>
    </>
  );
}
