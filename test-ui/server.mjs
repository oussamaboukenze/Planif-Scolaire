// Interface de test Planif-Scolaire.
// Sert la page web et relaie les appels vers les services : le navigateur ne parle
// qu'a une seule origine (http://localhost:3000), donc aucun CORS a configurer
// et aucune modification du backend.
// Lancement : node test-ui/server.mjs   (Node 18+ , aucune dependance)

import http from "node:http";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const PORT = Number(process.env.TEST_UI_PORT || 3000);
const here = dirname(fileURLToPath(import.meta.url));

// Nom utilise dans l'URL /proxy/<nom>/...  ->  service cible
const TARGETS = {
  referentiel: "http://localhost:8081",
  planning: "http://localhost:8082",
  presences: "http://localhost:8083",
  notifications: "http://localhost:8084",
  gateway: "http://localhost:8080",
  registry: "http://localhost:8761",
  config: "http://localhost:8888",
  mailpit: "http://localhost:8025",
};

const HOP_HEADERS = new Set(["host", "connection", "content-length", "origin", "referer", "accept-encoding"]);

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

async function proxy(req, res, name, rest) {
  const base = TARGETS[name];
  if (!base) {
    res.writeHead(404, { "content-type": "application/json" });
    return res.end(JSON.stringify({ error: `Service inconnu : ${name}` }));
  }
  const headers = {};
  for (const [k, v] of Object.entries(req.headers)) if (!HOP_HEADERS.has(k)) headers[k] = v;
  const body = ["GET", "HEAD"].includes(req.method) ? undefined : await readBody(req);
  try {
    const r = await fetch(base + rest, { method: req.method, headers, body, redirect: "manual" });
    const out = {};
    r.headers.forEach((v, k) => {
      if (!["content-encoding", "content-length", "transfer-encoding", "connection"].includes(k)) out[k] = v;
    });
    res.writeHead(r.status, out);
    res.end(Buffer.from(await r.arrayBuffer()));
  } catch (e) {
    res.writeHead(502, { "content-type": "application/json" });
    res.end(JSON.stringify({ status: 502, title: "Service injoignable", detail: `${name} (${base}) ne repond pas : ${e.cause?.code || e.message}` }));
  }
}

http
  .createServer(async (req, res) => {
    const url = new URL(req.url, "http://x");
    const m = url.pathname.match(/^\/proxy\/([a-z]+)(\/.*)?$/);
    if (m) return proxy(req, res, m[1], (m[2] || "/") + url.search);
    if (url.pathname === "/" || url.pathname === "/index.html") {
      res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
      return res.end(await readFile(join(here, "index.html")));
    }
    res.writeHead(404);
    res.end();
  })
  .listen(PORT, () => console.log(`Interface de test : http://localhost:${PORT}`));
