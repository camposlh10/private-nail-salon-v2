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
  id: string;
  slug: string;
  name: string;
  categorySlug: string;
  durationMinutes: number;
  priceType: PriceType;
  price: Money;
  imageUrl: string | null;
}

export interface PublicAddOn {
  id: string;
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

// --- booking flow (client-side; same-origin /api rewrite carries cookies) ---------

export interface AvailabilityDay {
  date: string;
  slots: string[]; // RFC 3339 with the salon's timezone offset
}

export interface AvailabilityResponse {
  serviceId: string;
  durationMinutes: number;
  timezone: string;
  days: AvailabilityDay[];
}

export interface SlotHold {
  id: string;
  serviceId: string;
  addOnIds: string[];
  start: string;
  end: string;
  expiresAt: string;
}

export interface BookedItem {
  itemType: "SERVICE" | "ADD_ON";
  name: string;
  durationMinutes: number;
  priceCents: number;
}

export interface BookedAppointment {
  id: string;
  status: string;
  start: string;
  end: string;
  timezone: string;
  serviceName: string;
  items: BookedItem[];
  totalCents: number;
  currency: string;
}

export class BookingApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

async function clientApi<T>(
  path: string,
  options: { method?: string; body?: unknown; headers?: Record<string, string> } = {},
): Promise<T> {
  const headers: Record<string, string> = { ...(options.headers ?? {}) };
  let body: BodyInit | undefined;
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }
  const res = await fetch(path, {
    method: options.method ?? "GET",
    headers,
    body,
    credentials: "include",
  });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    throw new BookingApiError(res.status, data?.code ?? "ERROR", data?.detail ?? res.statusText);
  }
  return data as T;
}

export function getAvailability(serviceId: string, addOnIds: string[], from: string, to: string) {
  const params = new URLSearchParams({ serviceId, from, to });
  if (addOnIds.length > 0) params.set("addOnIds", addOnIds.join(","));
  return clientApi<AvailabilityResponse>(`/api/v1/public/availability?${params}`);
}

export function createSlotHold(serviceId: string, addOnIds: string[], start: string) {
  return clientApi<SlotHold>("/api/v1/public/slot-holds", {
    method: "POST",
    body: { serviceId, addOnIds, start },
  });
}

export function releaseSlotHold(id: string) {
  return clientApi<void>(`/api/v1/public/slot-holds/${id}`, { method: "DELETE" });
}

export function startPhoneVerification(phone: string) {
  return clientApi<void>("/api/v1/public/auth/phone/start", { method: "POST", body: { phone } });
}

export function checkPhoneVerification(phone: string, code: string, slotHoldId?: string) {
  return clientApi<{ expiresAt: string }>("/api/v1/public/auth/phone/check", {
    method: "POST",
    body: { phone, code, slotHoldId },
  });
}

export function confirmAppointment(
  body: { slotHoldId: string; clientName: string; clientEmail?: string; notes?: string },
  idempotencyKey: string,
) {
  return clientApi<BookedAppointment>("/api/v1/public/appointments", {
    method: "POST",
    body,
    headers: { "Idempotency-Key": idempotencyKey },
  });
}

export function formatCents(cents: number, currency = "USD"): string {
  return (cents / 100).toLocaleString("en-US", { style: "currency", currency });
}

export function formatSlot(iso: string): string {
  return new Date(iso).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });
}

export function formatDay(dateIso: string): string {
  // Anchor to noon so the label never shifts a day in the viewer's timezone.
  return new Date(`${dateIso}T12:00:00`).toLocaleDateString("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
  });
}
