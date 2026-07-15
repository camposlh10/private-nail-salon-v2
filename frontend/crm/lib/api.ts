// Typed client for the owner CRM API, hand-synced with
// backend/src/main/resources/openapi/openapi.yaml. All requests go through the
// same-origin /api rewrite (next.config.ts), so the SESSION cookie just works.

export type ServiceStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";
export type AddOnStatus = "ACTIVE" | "ARCHIVED";
export type PriceType = "FIXED" | "STARTING_AT" | "FREE";

export interface OwnerMe {
  id: string;
  email: string;
  lastLoginAt: string | null;
}

export interface AdminCategory {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  displayOrder: number;
  status: ServiceStatus;
  version: number;
}

export interface AdminAddOn {
  id: string;
  serviceId: string;
  name: string;
  description: string | null;
  addedDurationMinutes: number;
  priceCents: number;
  displayOrder: number;
  status: AddOnStatus;
  version: number;
}

export interface AdminService {
  id: string;
  categoryId: string;
  name: string;
  slug: string;
  description: string | null;
  durationMinutes: number;
  priceType: PriceType;
  priceCents: number;
  onlineBookable: boolean;
  hiddenFromNewClients: boolean;
  imageId: string | null;
  displayOrder: number;
  status: ServiceStatus;
  version: number;
  addOns: AdminAddOn[];
}

export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ServiceWrite {
  categoryId: string;
  name: string;
  description?: string | null;
  durationMinutes: number;
  priceType: PriceType;
  priceCents: number;
  onlineBookable?: boolean;
  hiddenFromNewClients?: boolean;
  imageId?: string | null;
  version?: number;
}

export interface AddOnWrite {
  name: string;
  description?: string | null;
  addedDurationMinutes?: number;
  priceCents?: number;
  version?: number;
}

export interface MediaResult {
  id: string;
  url: string;
  contentType: string;
  fileSize: number;
  width: number | null;
  height: number | null;
  altText: string | null;
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

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fields?: Record<string, string>,
  ) {
    super(message);
  }
}

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp("(^|; )" + name + "=([^;]*)"));
  return match ? decodeURIComponent(match[2]) : null;
}

async function ensureCsrf(): Promise<string | null> {
  let token = getCookie("XSRF-TOKEN");
  if (!token) {
    await fetch("/api/v1/admin/auth/csrf", { credentials: "include" });
    token = getCookie("XSRF-TOKEN");
  }
  return token;
}

async function api<T>(
  path: string,
  options: { method?: string; body?: unknown; formData?: FormData } = {},
): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = {};
  if (method !== "GET") {
    const token = await ensureCsrf();
    if (token) headers["X-XSRF-TOKEN"] = token;
  }
  let body: BodyInit | undefined;
  if (options.formData) {
    body = options.formData;
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }
  const res = await fetch(path, { method, headers, body, credentials: "include" });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    throw new ApiError(
      res.status,
      data?.code ?? "ERROR",
      data?.detail ?? res.statusText,
      data?.errors,
    );
  }
  return data as T;
}

// --- auth --------------------------------------------------------------------

export const login = (email: string, password: string) =>
  api<OwnerMe>("/api/v1/admin/auth/login", { method: "POST", body: { email, password } });

export const logout = () => api<void>("/api/v1/admin/auth/logout", { method: "POST" });

export const me = () => api<OwnerMe>("/api/v1/admin/auth/me");

// --- categories --------------------------------------------------------------

export const listCategories = () => api<AdminCategory[]>("/api/v1/admin/categories");

export const createCategory = (name: string, description?: string) =>
  api<AdminCategory>("/api/v1/admin/categories", { method: "POST", body: { name, description } });

export const updateCategory = (id: string, name: string, description: string | null, version: number) =>
  api<AdminCategory>(`/api/v1/admin/categories/${id}`, {
    method: "PUT",
    body: { name, description, version },
  });

export const changeCategoryStatus = (id: string, status: ServiceStatus) =>
  api<AdminCategory>(`/api/v1/admin/categories/${id}/status`, {
    method: "PATCH",
    body: { status },
  });

export const reorderCategories = (orderedIds: string[]) =>
  api<void>("/api/v1/admin/categories/order", { method: "PUT", body: { orderedIds } });

// --- services ----------------------------------------------------------------

export const listServices = (page = 0, size = 50, status?: ServiceStatus) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set("status", status);
  return api<PageDto<AdminService>>(`/api/v1/admin/services?${params}`);
};

export const getService = (id: string) => api<AdminService>(`/api/v1/admin/services/${id}`);

export const createService = (body: ServiceWrite) =>
  api<AdminService>("/api/v1/admin/services", { method: "POST", body });

export const updateService = (id: string, body: ServiceWrite) =>
  api<AdminService>(`/api/v1/admin/services/${id}`, { method: "PUT", body });

export const changeServiceStatus = (id: string, status: ServiceStatus) =>
  api<AdminService>(`/api/v1/admin/services/${id}/status`, { method: "PATCH", body: { status } });

// --- add-ons -----------------------------------------------------------------

export const createAddOn = (serviceId: string, body: AddOnWrite) =>
  api<AdminAddOn>(`/api/v1/admin/services/${serviceId}/addons`, { method: "POST", body });

export const updateAddOn = (serviceId: string, addOnId: string, body: AddOnWrite) =>
  api<AdminAddOn>(`/api/v1/admin/services/${serviceId}/addons/${addOnId}`, {
    method: "PUT",
    body,
  });

export const changeAddOnStatus = (serviceId: string, addOnId: string, status: AddOnStatus) =>
  api<AdminAddOn>(`/api/v1/admin/services/${serviceId}/addons/${addOnId}/status`, {
    method: "PATCH",
    body: { status },
  });

// --- media -------------------------------------------------------------------

export const uploadMedia = (file: File, altText?: string) => {
  const formData = new FormData();
  formData.append("file", file);
  if (altText) formData.append("altText", altText);
  return api<MediaResult>("/api/v1/admin/media", { method: "POST", formData });
};

// --- misc --------------------------------------------------------------------

export const publicBusiness = () => api<PublicBusiness>("/api/v1/public/business");

export const formatCents = (cents: number) =>
  (cents / 100).toLocaleString("en-US", { style: "currency", currency: "USD" });
