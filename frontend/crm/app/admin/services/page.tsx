"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import {
  AdminCategory,
  AdminService,
  ApiError,
  changeServiceStatus,
  formatCents,
  listCategories,
  listServices,
  ServiceStatus,
} from "@/lib/api";

export default function ServicesPage() {
  const [services, setServices] = useState<AdminService[]>([]);
  const [categories, setCategories] = useState<Map<string, AdminCategory>>(new Map());
  const [statusFilter, setStatusFilter] = useState<ServiceStatus | "">("");
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      const [page, cats] = await Promise.all([
        listServices(0, 100, statusFilter || undefined),
        listCategories(),
      ]);
      setServices(page.content);
      setCategories(new Map(cats.map((c) => [c.id, c])));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load services");
    }
  }, [statusFilter]);

  useEffect(() => {
    reload();
  }, [reload]);

  const toggleStatus = async (service: AdminService) => {
    setError(null);
    try {
      await changeServiceStatus(
        service.id,
        service.status === "ARCHIVED" ? "ACTIVE" : "ARCHIVED",
      );
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    }
  };

  return (
    <>
      <h1>Services</h1>
      {error && <div className="error">{error}</div>}

      <div className="row" style={{ marginBottom: "1rem", alignItems: "center" }}>
        <div style={{ flex: "0 0 220px" }}>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as ServiceStatus | "")}
            aria-label="Filter by status"
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="DRAFT">Draft</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
        <div style={{ flex: 1, textAlign: "right" }}>
          <Link href="/admin/services/new" className="button">
            New service
          </Link>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Category</th>
            <th>Duration</th>
            <th>Price</th>
            <th>Bookable</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {services.map((service) => (
            <tr key={service.id}>
              <td>
                <Link href={`/admin/services/${service.id}`} style={{ fontWeight: 600 }}>
                  {service.name}
                </Link>
                {service.addOns.length > 0 && (
                  <span className="muted"> · {service.addOns.length} add-on(s)</span>
                )}
              </td>
              <td className="muted">{categories.get(service.categoryId)?.name ?? "—"}</td>
              <td>{service.durationMinutes} min</td>
              <td>
                {service.priceType === "FREE"
                  ? "Free"
                  : `${service.priceType === "STARTING_AT" ? "From " : ""}${formatCents(service.priceCents)}`}
              </td>
              <td>{service.onlineBookable ? "Yes" : "No"}</td>
              <td>
                <span className={`badge ${service.status}`}>{service.status}</span>
              </td>
              <td style={{ textAlign: "right" }}>
                <button
                  className={service.status === "ARCHIVED" ? "secondary small" : "danger small"}
                  onClick={() => toggleStatus(service)}
                >
                  {service.status === "ARCHIVED" ? "Activate" : "Archive"}
                </button>
              </td>
            </tr>
          ))}
          {services.length === 0 && (
            <tr>
              <td colSpan={7} className="muted">
                No services yet.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </>
  );
}
