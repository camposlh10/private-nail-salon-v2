"use client";

import { use, useEffect, useState } from "react";
import { AdminService, ApiError, getService } from "@/lib/api";
import ServiceForm from "@/components/ServiceForm";

interface Props {
  params: Promise<{ id: string }>;
}

export default function EditServicePage({ params }: Props) {
  const { id } = use(params);
  const [service, setService] = useState<AdminService | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getService(id)
      .then(setService)
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "Failed to load the service"),
      );
  }, [id]);

  if (error) return <div className="error">{error}</div>;
  if (!service) return <p className="muted">Loading…</p>;

  return (
    <>
      <h1>
        Edit service <span className="muted">/{service.slug}</span>
      </h1>
      <ServiceForm key={service.version} service={service} onSaved={setService} />
    </>
  );
}
