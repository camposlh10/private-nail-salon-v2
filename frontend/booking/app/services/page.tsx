import Link from "next/link";
import { formatDuration, formatPrice, getBusiness, getCategories, getServices } from "@/lib/api";

interface Props {
  searchParams: Promise<{ category?: string; q?: string }>;
}

export default async function ServicesPage({ searchParams }: Props) {
  const { category, q } = await searchParams;
  const [business, categories, services] = await Promise.all([
    getBusiness(),
    getCategories(),
    getServices(category, q),
  ]);

  if (!business || !categories || !services) {
    return <p className="empty">The salon is offline right now — please check back soon.</p>;
  }

  return (
    <>
      <h1>Our services</h1>
      {business.appointmentStartNotice && (
        <div className="notice">{business.appointmentStartNotice}</div>
      )}

      <div className="filters">
        <Link className={`pill${!category ? " active" : ""}`} href="/services">
          All
        </Link>
        {categories.map((c) => (
          <Link
            key={c.slug}
            className={`pill${category === c.slug ? " active" : ""}`}
            href={`/services?category=${encodeURIComponent(c.slug)}`}
          >
            {c.name}
          </Link>
        ))}
        <form className="search" action="/services" method="get">
          {category && <input type="hidden" name="category" value={category} />}
          <input type="search" name="q" placeholder="Search services…" defaultValue={q ?? ""} />
          <button type="submit">Search</button>
        </form>
      </div>

      {services.length === 0 ? (
        <p className="empty">No services match your search.</p>
      ) : (
        <div className="grid">
          {services.map((s) => (
            <Link key={s.slug} href={`/services/${encodeURIComponent(s.slug)}`} className="card">
              {s.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={s.imageUrl} alt={s.name} />
              ) : (
                <div className="placeholder" />
              )}
              <div className="card-body">
                <div className="card-title">{s.name}</div>
                <div className="meta">{formatDuration(s.durationMinutes)}</div>
                <div className="price">{formatPrice(s.priceType, s.price)}</div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
