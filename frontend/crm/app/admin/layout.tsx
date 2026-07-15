"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError, logout, me } from "@/lib/api";

/**
 * Guards every /admin page except /admin/login: verifies the session on mount and
 * bounces to the login page on 401.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const isLogin = pathname === "/admin/login";
  const [ready, setReady] = useState(isLogin);

  useEffect(() => {
    if (isLogin) return;
    let cancelled = false;
    me()
      .then(() => !cancelled && setReady(true))
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
        } else {
          setReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [isLogin, pathname, router]);

  if (isLogin) {
    return <>{children}</>;
  }
  if (!ready) {
    return <p style={{ padding: "2rem" }}>Loading…</p>;
  }

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      router.replace("/admin/login");
    }
  };

  return (
    <div className="shell">
      <aside className="sidebar">
        <Link href="/admin/services" className="brand">
          Salon CRM
        </Link>
        <nav>
          <Link href="/admin/services">Services</Link>
          <Link href="/admin/categories">Categories</Link>
          <Link href="/admin/settings/business">Settings</Link>
        </nav>
        <button className="logout" onClick={handleLogout}>
          Sign out
        </button>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
