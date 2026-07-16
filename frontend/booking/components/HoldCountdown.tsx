"use client";

import { useEffect, useState } from "react";

/** Shows how long the reserved time is held; the server enforces the real expiry. */
export default function HoldCountdown({ expiresAt }: { expiresAt: string }) {
  const [secondsLeft, setSecondsLeft] = useState(() =>
    Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000)),
  );

  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsLeft(Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000)));
    }, 1000);
    return () => clearInterval(timer);
  }, [expiresAt]);

  if (secondsLeft <= 0) {
    return <div className="notice">Your hold has expired — please pick a time again.</div>;
  }
  const m = Math.floor(secondsLeft / 60);
  const s = secondsLeft % 60;
  return (
    <p className="meta">
      We&apos;re holding this time for {m}:{s.toString().padStart(2, "0")} minutes.
    </p>
  );
}
