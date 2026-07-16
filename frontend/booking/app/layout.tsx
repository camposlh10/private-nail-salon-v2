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
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        {/* eslint-disable-next-line @next/next/no-page-custom-font */}
        <link
          href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <header className="site-header">
          <div className="container">
            <Link href="/" className="brand">
              <span className="brand-mark">✦</span>
              {name.split(" ")[0]} <span>{name.split(" ").slice(1).join(" ")}</span>
            </Link>
            <nav className="site-nav">
              <Link className="pill" href="/services">
                Services
              </Link>
              <Link className="pill active" href="/services">
                Book now
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
