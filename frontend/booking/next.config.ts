import type { NextConfig } from "next";

const backend = process.env.BACKEND_URL ?? "http://localhost:8092";

const nextConfig: NextConfig = {
  // Proxy API calls (including public media images) to the Spring backend so the
  // browser only ever talks to this origin.
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
