import Link from "next/link";
import { formatDuration, formatPrice, getBusiness, getCategories, getServices } from "@/lib/api";

export default async function HomePage() {
  const [business, categories, services] = await Promise.all([
    getBusiness(),
    getCategories(),
    getServices(),
  ]);
  if (!business) {
    return <p className="empty">The salon is offline right now — please check back soon.</p>;
  }

  const popular = (services ?? []).slice(0, 6);

  return (
    <>
      <section className="hero">
        <h1>Beautiful nails, booked in a minute.</h1>
        <p>
          Pick a service, choose a time that suits you, and we&apos;ll take care of the rest.
          We can&apos;t wait to pamper you at {business.name}.
        </p>
        <Link href="/services" className="button">
          Book an appointment
        </Link>
      </section>

      {categories && categories.length > 0 && (
        <>
          <div className="section-head">
            <h2>Categories</h2>
          </div>
          <div className="chips">
            {categories.map((c) => (
              <Link key={c.slug} className="pill" href={`/services?category=${encodeURIComponent(c.slug)}`}>
                {c.name}
              </Link>
            ))}
          </div>
        </>
      )}

      {popular.length > 0 && (
        <>
          <div className="section-head">
            <h2>Popular services</h2>
            <Link href="/services">See all →</Link>
          </div>
          <div className="grid">
            {popular.map((s) => (
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
                  <div className="price-row">
                    <span className="price">{formatPrice(s.priceType, s.price)}</span>
                    <span className="tag">Book</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </>
      )}

      {business.appointmentStartNotice && <div className="notice">{business.appointmentStartNotice}</div>}
    </>
  );
}
