import type { NextConfig } from "next";

const backend = process.env.BACKEND_URL ?? "http://localhost:8092";

const nextConfig: NextConfig = {
  // Proxy all API traffic to the Spring backend. Requests stay same-origin in the
  // browser, so the session cookie and CSRF flow need no cross-origin handling.
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
