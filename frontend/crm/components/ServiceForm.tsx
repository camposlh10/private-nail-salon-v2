"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import {
  AdminCategory,
  AdminService,
  ApiError,
  changeAddOnStatus,
  createAddOn,
  createService,
  formatCents,
  listCategories,
  PriceType,
  updateService,
  uploadMedia,
} from "@/lib/api";

interface Props {
  service?: AdminService;
  onSaved?: (service: AdminService) => void;
}

/** Create/edit form for a service, including image upload and (in edit mode) add-ons. */
export default function ServiceForm({ service, onSaved }: Props) {
  const router = useRouter();
  const editing = Boolean(service);

  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [name, setName] = useState(service?.name ?? "");
  const [categoryId, setCategoryId] = useState(service?.categoryId ?? "");
  const [description, setDescription] = useState(service?.description ?? "");
  const [durationMinutes, setDurationMinutes] = useState(service?.durationMinutes ?? 60);
  const [priceType, setPriceType] = useState<PriceType>(service?.priceType ?? "FIXED");
  const [priceDollars, setPriceDollars] = useState(
    service ? (service.priceCents / 100).toFixed(2) : "50.00",
  );
  const [onlineBookable, setOnlineBookable] = useState(service?.onlineBookable ?? true);
  const [hidden, setHidden] = useState(service?.hiddenFromNewClients ?? false);
  const [imageId, setImageId] = useState<string | null>(service?.imageId ?? null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Add-on editor state (edit mode only)
  const [addOnName, setAddOnName] = useState("");
  const [addOnMinutes, setAddOnMinutes] = useState(0);
  const [addOnDollars, setAddOnDollars] = useState("0.00");

  useEffect(() => {
    listCategories()
      .then((cats) => {
        setCategories(cats.filter((c) => c.status !== "ARCHIVED"));
        if (!service && cats.length > 0) {
          setCategoryId((current) => current || cats[0].id);
        }
      })
      .catch(() => setError("Failed to load categories"));
  }, [service]);

  const handleImage = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const media = await uploadMedia(file, name || undefined);
      setImageId(media.id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const priceCents = priceType === "FREE" ? 0 : Math.round(parseFloat(priceDollars || "0") * 100);
    const body = {
      categoryId,
      name: name.trim(),
      description: description.trim() || null,
      durationMinutes,
      priceType,
      priceCents,
      onlineBookable,
      hiddenFromNewClients: hidden,
      imageId,
      version: service?.version,
    };
    try {
      const saved = editing ? await updateService(service!.id, body) : await createService(body);
      if (onSaved) {
        onSaved(saved);
      } else {
        router.push("/admin/services");
      }
    } catch (err) {
      if (err instanceof ApiError) {
        const fieldErrors = err.fields ? " — " + Object.values(err.fields).join(", ") : "";
        setError(err.message + fieldErrors);
      } else {
        setError("Something went wrong");
      }
      setBusy(false);
    }
  };

  const handleAddAddOn = async (event: FormEvent) => {
    event.preventDefault();
    if (!service || !addOnName.trim()) return;
    setError(null);
    try {
      const created = await createAddOn(service.id, {
        name: addOnName.trim(),
        addedDurationMinutes: addOnMinutes,
        priceCents: Math.round(parseFloat(addOnDollars || "0") * 100),
      });
      onSaved?.({ ...service, addOns: [...service.addOns, created] });
      setAddOnName("");
      setAddOnMinutes(0);
      setAddOnDollars("0.00");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add add-on");
    }
  };

  const toggleAddOn = async (addOnId: string, currentStatus: string) => {
    if (!service) return;
    try {
      const updated = await changeAddOnStatus(
        service.id,
        addOnId,
        currentStatus === "ARCHIVED" ? "ACTIVE" : "ARCHIVED",
      );
      onSaved?.({
        ...service,
        addOns: service.addOns.map((a) => (a.id === addOnId ? updated : a)),
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update add-on");
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <div className="error">{error}</div>}

      <div className="card">
        <div className="row">
          <div>
            <label htmlFor="svc-name">Name</label>
            <input id="svc-name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div>
            <label htmlFor="svc-category">Category</label>
            <select
              id="svc-category"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              required
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <label htmlFor="svc-description">Description</label>
        <textarea
          id="svc-description"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <div className="row">
          <div>
            <label htmlFor="svc-duration">Duration (minutes)</label>
            <input
              id="svc-duration"
              type="number"
              min={15}
              step={15}
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(parseInt(e.target.value, 10) || 0)}
              required
            />
          </div>
          <div>
            <label htmlFor="svc-price-type">Price type</label>
            <select
              id="svc-price-type"
              value={priceType}
              onChange={(e) => setPriceType(e.target.value as PriceType)}
            >
              <option value="FIXED">Fixed</option>
              <option value="STARTING_AT">Starting at</option>
              <option value="FREE">Free</option>
            </select>
          </div>
          {priceType !== "FREE" && (
            <div>
              <label htmlFor="svc-price">Price (USD)</label>
              <input
                id="svc-price"
                type="number"
                min="0.01"
                step="0.01"
                value={priceDollars}
                onChange={(e) => setPriceDollars(e.target.value)}
                required
              />
            </div>
          )}
        </div>

        <label style={{ marginTop: "1rem" }}>
          <input
            type="checkbox"
            checked={onlineBookable}
            onChange={(e) => setOnlineBookable(e.target.checked)}
          />
          Bookable online
        </label>
        <label>
          <input type="checkbox" checked={hidden} onChange={(e) => setHidden(e.target.checked)} />
          Hidden from new clients
        </label>

        <label htmlFor="svc-image">Image (JPEG/PNG, max 5MB)</label>
        <input
          id="svc-image"
          type="file"
          accept="image/jpeg,image/png"
          onChange={(e) => handleImage(e.target.files?.[0])}
        />
        {uploading && <p className="muted">Uploading…</p>}
        {imageId && !uploading && (
          <p style={{ marginTop: "0.5rem" }}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img className="thumb" src={`/api/v1/public/media/${imageId}`} alt="Service" />
          </p>
        )}
      </div>

      {editing && service && (
        <div className="card">
          <h2 style={{ marginTop: 0 }}>Add-ons</h2>
          {service.addOns.length === 0 && <p className="muted">No add-ons yet.</p>}
          {service.addOns.map((addOn) => (
            <div key={addOn.id} className="row" style={{ alignItems: "center", padding: "0.35rem 0" }}>
              <div>
                <strong>{addOn.name}</strong>{" "}
                <span className="muted">
                  {addOn.addedDurationMinutes > 0 && `+${addOn.addedDurationMinutes} min · `}
                  {addOn.priceCents > 0 ? `+${formatCents(addOn.priceCents)}` : "no extra cost"}
                </span>{" "}
                <span className={`badge ${addOn.status}`}>{addOn.status}</span>
              </div>
              <div style={{ flex: "0 0 auto" }}>
                <button
                  type="button"
                  className={addOn.status === "ARCHIVED" ? "secondary small" : "danger small"}
                  onClick={() => toggleAddOn(addOn.id, addOn.status)}
                >
                  {addOn.status === "ARCHIVED" ? "Activate" : "Archive"}
                </button>
              </div>
            </div>
          ))}

          <div className="row" style={{ alignItems: "flex-end", marginTop: "0.75rem" }}>
            <div>
              <label htmlFor="addon-name">New add-on name</label>
              <input
                id="addon-name"
                value={addOnName}
                onChange={(e) => setAddOnName(e.target.value)}
                placeholder="e.g. Nail art"
              />
            </div>
            <div>
              <label htmlFor="addon-minutes">Added minutes</label>
              <input
                id="addon-minutes"
                type="number"
                min={0}
                step={15}
                value={addOnMinutes}
                onChange={(e) => setAddOnMinutes(parseInt(e.target.value, 10) || 0)}
              />
            </div>
            <div>
              <label htmlFor="addon-price">Added price (USD)</label>
              <input
                id="addon-price"
                type="number"
                min={0}
                step="0.01"
                value={addOnDollars}
                onChange={(e) => setAddOnDollars(e.target.value)}
              />
            </div>
            <div style={{ flex: "0 0 auto" }}>
              <button type="button" className="secondary" onClick={handleAddAddOn}>
                Add
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="actions">
        <button type="submit" disabled={busy || uploading}>
          {busy ? "Saving…" : editing ? "Save changes" : "Create service"}
        </button>
        <button type="button" className="secondary" onClick={() => router.push("/admin/services")}>
          Cancel
        </button>
      </div>
    </form>
  );
}
