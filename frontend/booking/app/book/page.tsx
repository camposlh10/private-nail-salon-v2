import { redirect } from "next/navigation";

interface Props {
  searchParams: Promise<{ service?: string }>;
}

// Entry point kept for old links: /book?service=x forwards into the real flow.
export default async function BookPage({ searchParams }: Props) {
  const { service } = await searchParams;
  redirect(service ? `/book/${encodeURIComponent(service)}` : "/services");
}
