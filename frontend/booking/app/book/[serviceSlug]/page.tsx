import Link from "next/link";
import { notFound } from "next/navigation";
import { getService } from "@/lib/api";
import AddOnPicker from "@/components/AddOnPicker";
import Steps from "@/components/Steps";

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
    <div className="flow">
      <Link href={`/services/${service.slug}`} className="back-link">
        ← Back to {service.name}
      </Link>
      <h1 className="flow-title">Book {service.name}</h1>
      <Steps current={1} />
      <AddOnPicker service={service} />
    </div>
  );
}
