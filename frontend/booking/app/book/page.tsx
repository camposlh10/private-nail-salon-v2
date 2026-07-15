import Link from "next/link";

// Placeholder until the booking milestone (date/time selection, phone verification).
export default function BookPage() {
  return (
    <>
      <h1>Online booking is almost here</h1>
      <p style={{ margin: "0.75rem 0 1.5rem" }}>
        We&apos;re putting the finishing touches on online booking. In the meantime, call or
        text us to schedule your appointment — we&apos;d love to see you!
      </p>
      <Link href="/services" className="button">
        Back to services
      </Link>
    </>
  );
}
