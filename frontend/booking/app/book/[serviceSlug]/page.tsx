import Link from "next/link";
import { notFound } from "next/navigation";
import { getService } from "@/lib/api";
import AddOnPicker from "@/components/AddOnPicker";

interface Props {
  params: Promise<{ serviceSlug: string }>;
}

export default async function BookServicePage({ params }: Props) {
  const { serviceSlug } = await params;
  const service = await getService(serviceSlug);
  if (!service) {
    notFound();
  }

  return (
    <>
      <p style={{ marginTop: "1.25rem" }}>
        <Link href={`/services/${service.slug}`} className="meta">
          ← Back to {service.name}
        </Link>
      </p>
      <h1>Book {service.name}</h1>
      <p className="meta" style={{ marginBottom: "1rem" }}>
        Step 1 of 4 — choose your add-ons
      </p>
      <AddOnPicker service={service} />
    </>
  );
}
