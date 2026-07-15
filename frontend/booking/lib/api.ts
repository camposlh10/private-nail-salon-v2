// Typed client for the public catalog API, hand-synced with
// backend/src/main/resources/openapi/openapi.yaml (public-catalog tag).

export type PriceType = "FIXED" | "STARTING_AT" | "FREE";

export interface Money {
  amountCents: number;
  currency: string;
}

export interface PublicBusiness {
  name: string;
  slug: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  timezone: string;
  currency: string;
  appointmentStartWindowMinutes: number;
  appointmentStartNotice: string | null;
}

export interface PublicCategory {
  slug: string;
  name: string;
  description: string | null;
}

export interface PublicServiceSummary {
  slug: string;
  name: string;
  categorySlug: string;
  durationMinutes: number;
  priceType: PriceType;
  price: Money;
  imageUrl: string | null;
}

export interface PublicAddOn {
  name: string;
  description: string | null;
  addedDurationMinutes: number;
  price: Money;
}

export interface PublicServiceDetail extends PublicServiceSummary {
  description: string | null;
  addOns: PublicAddOn[];
}

// Server components fetch the backend directly; the browser goes through the
// /api rewrite in next.config.ts (only relevant for <img> URLs).
const API = process.env.BACKEND_URL ?? "http://localhost:8092";

async function fetchJson<T>(path: string): Promise<T | null> {
  try {
    const res = await fetch(`${API}${path}`, { cache: "no-store" });
    if (!res.ok) return null;
    return (await res.json()) as T;
  } catch {
    // Backend down — pages render a friendly fallback instead of crashing.
    return null;
  }
}

export function getBusiness() {
  return fetchJson<PublicBusiness>("/api/v1/public/business");
}

export function getCategories() {
  return fetchJson<PublicCategory[]>("/api/v1/public/categories");
}

export function getServices(category?: string, q?: string) {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  if (q) params.set("q", q);
  const qs = params.size > 0 ? `?${params}` : "";
  return fetchJson<PublicServiceSummary[]>(`/api/v1/public/services${qs}`);
}

export function getService(slug: string) {
  return fetchJson<PublicServiceDetail>(`/api/v1/public/services/${encodeURIComponent(slug)}`);
}

export function formatPrice(priceType: PriceType, price: Money): string {
  if (priceType === "FREE") return "Free";
  const amount = (price.amountCents / 100).toLocaleString("en-US", {
    style: "currency",
    currency: price.currency || "USD",
  });
  return priceType === "STARTING_AT" ? `From ${amount}` : amount;
}

export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} hr` : `${h} hr ${m} min`;
}
