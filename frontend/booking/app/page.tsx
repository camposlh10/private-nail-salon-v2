import Link from "next/link";
import { getBusiness } from "@/lib/api";

export default async function HomePage() {
  const business = await getBusiness();
  if (!business) {
    return <p className="empty">The salon is offline right now — please check back soon.</p>;
  }
  return (
    <>
      <h1>Welcome to {business.name}</h1>
      <p>
        Explore our services and book your next visit. We can&apos;t wait to pamper you.
      </p>
      {business.appointmentStartNotice && (
        <div className="notice">{business.appointmentStartNotice}</div>
      )}
      <p style={{ margin: "1.5rem 0 2.5rem" }}>
        <Link href="/services" className="button">
          Browse services
        </Link>
      </p>
    </>
  );
}
