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
      <p style={{ marginTop: "1.25rem" }}>
        <Link href="/services" className="meta">
          ← Back to services
        </Link>
      </p>
      <div className="detail">
        {service.imageUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={service.imageUrl} alt={service.name} />
        )}
        <h1>{service.name}</h1>
        <p className="meta">
          {formatDuration(service.durationMinutes)} · {formatPrice(service.priceType, service.price)}
        </p>
        {service.description && <p style={{ margin: "0.75rem 0" }}>{service.description}</p>}

        {service.addOns.length > 0 && (
          <>
            <h2>Add-ons</h2>
            {service.addOns.map((a) => (
              <div key={a.name} className="addon">
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

        <p style={{ marginTop: "1.25rem" }}>
          <Link href={`/book/${encodeURIComponent(service.slug)}`} className="button">
            Book this service
          </Link>
        </p>
      </div>
    </>
  );
}
