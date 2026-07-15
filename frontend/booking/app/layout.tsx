import type { Metadata } from "next";
import Link from "next/link";
import { getBusiness } from "@/lib/api";
import "./globals.css";

export const metadata: Metadata = {
  title: "Private Nail Studio",
  description: "Book your next nail appointment",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const business = await getBusiness();
  const name = business?.name ?? "Private Nail Studio";
  return (
    <html lang="en">
      <body>
        <header className="site-header">
          <div className="container">
            <Link href="/" className="brand">
              {name.split(" ")[0]} <span>{name.split(" ").slice(1).join(" ")}</span>
            </Link>
            <nav>
              <Link className="pill" href="/services">
                Services
              </Link>
            </nav>
          </div>
        </header>
        <main className="container">{children}</main>
        <footer className="footer">
          {business?.address && <div>{business.address}</div>}
          {business?.phone && <div>{business.phone}</div>}
        </footer>
      </body>
    </html>
  );
}
