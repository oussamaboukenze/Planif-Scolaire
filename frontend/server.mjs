// Planif-Scolaire — frontend
// Sert l'interface (index.html, app.css, app.js) et relaie les appels :
//   /api/**        -> API Gateway (http://localhost:8080)   : toutes les données métier
//   /svc/<nom>/**  -> un service en direct (santé, Eureka)   : page « Services »
// Le navigateur ne parle qu'à http://localhost:4200 : pas de CORS à configurer côté Spring.
// Lancement (Node 18+, aucune dépendance) :  node frontend/server.mjs

import http from "node:http";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join, extname } from "node:path";

const PORT = Number(process.env.FRONT_PORT || 4200);
const GATEWAY = process.env.GATEWAY_URL || "http://localhost:8080";
const here = dirname(fileURLToPath(import.meta.url));

const SERVICES = {
  gateway: "http://localhost:8080",
  referentiel: "http://localhost:8081",
  planning: "http://localhost:8082",
  presences: "http://localhost:8083",
  notifications: "http://localhost:8084",
  utilisateurs: "http://localhost:8085",
  registry: "http://localhost:8761",
  config: "http://localhost:8888",
};

const STATIC = { "/": "index.html", "/index.html": "index.html", "/app.css": "app.css", "/app.js": "app.js" };
const TYPES = { ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8", ".js": "text/javascript; charset=utf-8" };
const DROP = new Set(["host", "connection", "content-length", "origin", "referer", "accept-encoding"]);

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

async function relay(req, res, target, label) {
  const headers = { "x-client-id": "planif-frontend" };
  for (const [k, v] of Object.entries(req.headers)) if (!DROP.has(k)) headers[k] = v;
  const body = ["GET", "HEAD"].includes(req.method) ? undefined : await readBody(req);
  try {
    const r = await fetch(target, { method: req.method, headers, body, redirect: "manual" });
    const out = {};
    r.headers.forEach((v, k) => {
      if (!["content-encoding", "content-length", "transfer-encoding", "connection"].includes(k)) out[k] = v;
    });
    res.writeHead(r.status, out);
    res.end(Buffer.from(await r.arrayBuffer()));
  } catch (e) {
    res.writeHead(502, { "content-type": "application/json" });
    res.end(JSON.stringify({
      status: 502,
      title: "Service injoignable",
      detail: `${label} ne répond pas (${e.cause?.code || e.message}). Vérifiez qu'il est démarré.`,
    }));
  }
}

http
  .createServer(async (req, res) => {
    const url = new URL(req.url, "http://x");
    if (url.pathname.startsWith("/api/")) {
      return relay(req, res, GATEWAY + url.pathname + url.search, `La Gateway (${GATEWAY})`);
    }
    const m = url.pathname.match(/^\/svc\/([a-z]+)(\/.*)?$/);
    if (m) {
      const base = SERVICES[m[1]];
      if (!base) {
        res.writeHead(404, { "content-type": "application/json" });
        return res.end(JSON.stringify({ status: 404, detail: `Service inconnu : ${m[1]}` }));
      }
      return relay(req, res, base + (m[2] || "/") + url.search, `${m[1]} (${base})`);
    }
    const file = STATIC[url.pathname];
    if (file) {
      res.writeHead(200, { "content-type": TYPES[extname(file)], "cache-control": "no-store" });
      return res.end(await readFile(join(here, file)));
    }
    res.writeHead(404);
    res.end();
  })
  .listen(PORT, () => console.log(`Planif-Scolaire : http://localhost:${PORT}`));
