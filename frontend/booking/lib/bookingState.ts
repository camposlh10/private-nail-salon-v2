// The in-progress booking, carried across the /book/* pages in sessionStorage.
// The backend is the only authority on availability and pricing — everything here
// is display/state plumbing, re-validated server-side at every step.

export interface BookingState {
  serviceId: string;
  serviceSlug: string;
  serviceName: string;
  addOnIds: string[];
  addOnNames: string[];
  durationMinutes: number;
  totalCents: number;
  currency: string;
  // set once a slot is held
  holdId?: string;
  start?: string;
  end?: string;
  holdExpiresAt?: string;
  // set on the details step
  clientName?: string;
  clientEmail?: string;
  phone?: string;
  notes?: string;
  // one per booking attempt so browser retries can't double-book
  idempotencyKey?: string;
}

const KEY = "nail-booking";
const CONFIRMED_KEY = "nail-booking-confirmed";

export function loadBooking(): BookingState | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as BookingState) : null;
  } catch {
    return null;
  }
}

export function saveBooking(state: BookingState) {
  sessionStorage.setItem(KEY, JSON.stringify(state));
}

export function clearBooking() {
  sessionStorage.removeItem(KEY);
}

export function saveConfirmation(confirmation: unknown) {
  sessionStorage.setItem(CONFIRMED_KEY, JSON.stringify(confirmation));
}

export function loadConfirmation<T>(): T | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = sessionStorage.getItem(CONFIRMED_KEY);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}
