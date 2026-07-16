import Link from "next/link";
import { notFound } from "next/navigation";
import { formatDuration, formatPrice, getBusiness, getService } from "@/lib/api";

interface Props {
  params: Promise<{ slug: string }>;
}

export default async function ServiceDetailPage({ params }: Props) {
  const { slug } = await params;
  const [business, service] = await Promise.all([getBusiness(), getService(slug)]);
  if (!service) {
    notFound();
  }

  return (
    <>
      <Link href="/services" className="back-link">
        ← All services
      </Link>
      <div className="panel">
        {service.imageUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={service.imageUrl} alt={service.name} />
        )}
        <h1>{service.name}</h1>
        <div className="chips" style={{ margin: "0.6rem 0 0.2rem" }}>
          <span className="tag">{formatDuration(service.durationMinutes)}</span>
          <span className="tag">{formatPrice(service.priceType, service.price)}</span>
          {service.addOns.length > 0 && <span className="tag">{service.addOns.length} add-ons</span>}
        </div>
        {service.description && <p style={{ margin: "0.9rem 0" }}>{service.description}</p>}

        {service.addOns.length > 0 && (
          <>
            <h2 style={{ margin: "1.1rem 0 0.5rem" }}>Available add-ons</h2>
            {service.addOns.map((a) => (
              <div key={a.id} className="line-item">
                <div>
                  <div className="card-title">{a.name}</div>
                  {a.description && <div className="meta">{a.description}</div>}
                  {a.addedDurationMinutes > 0 && (
                    <div className="meta">+{formatDuration(a.addedDurationMinutes)}</div>
                  )}
                </div>
                <div className="price">
                  {a.price.amountCents > 0 ? formatPrice("FIXED", a.price) : "Included"}
                </div>
              </div>
            ))}
          </>
        )}

        {business?.appointmentStartNotice && (
          <div className="notice">{business.appointmentStartNotice}</div>
        )}

        <div className="summary-bar">
          <div className="totals">
            <span className="label">From</span>
            <span className="value">{formatPrice(service.priceType, service.price)}</span>
          </div>
          <Link href={`/book/${encodeURIComponent(service.slug)}`} className="button">
            Book this service
          </Link>
        </div>
      </div>
    </>
  );
}
