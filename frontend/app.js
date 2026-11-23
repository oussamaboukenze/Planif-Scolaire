// Planif-Scolaire — interface web (vanilla JS, aucune dépendance)
// Toutes les données passent par /api/** -> Gateway 8080 -> microservices.

const $ = (sel, root = document) => root.querySelector(sel);
const view = $("#view");
const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

/* ===================== API + journal ===================== */
const journal = [];
const ROUTES = [
  [/^\/api\/(classes|enseignants)/, "service-referentiel :8081"],
  [/^\/api\/users/, "service-utilisateurs :8085"],
  [/^\/api\/cours/, "service-planning :8082"],
  [/^\/api\/appels/, "service-presences :8083"],
  [/^\/api\/notifications/, "notification-service :8084"],
  [/^\/svc\/([a-z]+)/, null],
];
function routeOf(path) {
  for (const [re, name] of ROUTES) {
    const m = path.match(re);
    if (m) return name ?? `${m[1]} (direct)`;
  }
  return "";
}

class ApiError extends Error {
  constructor(status, detail) { super(detail); this.status = status; }
}

async function api(method, path, body, headers = {}) {
  const t0 = performance.now();
  const opts = { method, headers: { Accept: "application/json", ...headers } };
  if (body !== undefined) {
    opts.body = JSON.stringify(body);
    opts.headers["Content-Type"] = "application/json";
  }
  let res;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      res = await fetch(path, opts);
    } catch {
      logCall(method, path, 0, performance.now() - t0);
      throw new ApiError(0, "Le serveur du frontend ne répond pas. Lancez node frontend/server.mjs.");
    }
    if (res.status !== 429) break;
    await new Promise((r) => setTimeout(r, 400 * (attempt + 1))); // rate limiter de la Gateway
  }
  logCall(method, path, res.status, performance.now() - t0);
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    const detail = data?.detail || data?.title || (res.status === 429 ? "Trop de requêtes : la Gateway limite à 5 par seconde." : `Erreur HTTP ${res.status}`);
    throw new ApiError(res.status, detail);
  }
  return data;
}

function logCall(method, path, status, ms) {
  journal.unshift({ method, path, status, ms: Math.round(ms), at: new Date() });
  journal.length = Math.min(journal.length, 80);
  $("#journalCount").textContent = journal.length;
  if (!$("#journal").hidden) renderJournal();
}

function renderJournal() {
  $("#journalList").innerHTML = journal.map((j) => `
    <li>
      <span class="m">${j.method}</span>
      <span class="p">${esc(j.path)}<small>${esc(routeOf(j.path))} · ${j.ms} ms</small></span>
      <span class="s s${String(j.status)[0]}">${j.status || "—"}</span>
    </li>`).join("") || `<li class="empty">Aucune requête pour l'instant.</li>`;
}
$("#journalBtn").onclick = () => { $("#journal").hidden = false; renderJournal(); };
$("#journalClose").onclick = () => { $("#journal").hidden = true; };

/* ===================== données ===================== */
const db = { classes: [], enseignants: [], users: [], cours: [], appels: [] };
async function loadAll(...keys) {
  const map = {
    classes: () => api("GET", "/api/classes"),
    enseignants: () => api("GET", "/api/enseignants"),
    users: () => api("GET", "/api/users"),
    cours: () => api("GET", "/api/cours"),
    appels: () => api("GET", "/api/appels"),
  };
  const list = keys.length ? keys : Object.keys(map);
  const results = await Promise.allSettled(list.map((k) => map[k]()));
  const errors = [];
  results.forEach((r, i) => {
    if (r.status === "fulfilled") db[list[i]] = r.value || [];
    else errors.push(`${list[i]} : ${r.reason.message}`);
  });
  if (errors.length) toast(errors.join(" · "), "err");
}
const byId = (arr, id) => arr.find((x) => x.id === id);
const classeNom = (id) => byId(db.classes, id)?.nom ?? `Classe #${id}`;
const ensNom = (id) => byId(db.enseignants, id)?.nom ?? `Enseignant #${id}`;

/* ===================== utilitaires UI ===================== */
function toast(msg, kind = "") {
  const el = document.createElement("div");
  el.className = `toast ${kind}`;
  el.textContent = msg;
  $("#toasts").append(el);
  setTimeout(() => el.remove(), kind === "err" ? 6000 : 3200);
}
const st = (s) => `<span class="st st-${esc(s)}">${esc(label(s))}</span>`;
const LABELS = { PLANIFIE: "Planifié", REALISE: "Réalisé", ANNULE: "Annulé", OUVERT: "Ouverte", CLOTURE: "Clôturée", SENT: "Envoyé", PENDING: "En attente", FAILED: "Échec", ETUDIANT: "Étudiant", ENSEIGNANT: "Enseignant", ADMIN: "Admin", COURSE_SCHEDULED: "Cours planifié", COURSE_CANCELLED: "Cours annulé", ABSENCE_RECORDED: "Absence saisie", UP: "En ligne", DOWN: "Arrêté" };
const label = (s) => LABELS[s] ?? s;
const JOURS = ["dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."];
const fmtDate = (iso) => new Date(iso).toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long" });
const fmtShort = (iso) => new Date(iso).toLocaleDateString("fr-FR", { day: "2-digit", month: "2-digit" });
const fmtTime = (iso) => new Date(iso).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
const options = (arr, val, text, sel) => arr.map((x) => `<option value="${esc(x[val])}" ${String(x[val]) === String(sel) ? "selected" : ""}>${esc(text(x))}</option>`).join("");

function openDrawer(html) {
  $("#drawer").innerHTML = html;
  $("#drawer").hidden = false;
  $("#drawerBackdrop").hidden = false;
}
function closeDrawer() { $("#drawer").hidden = true; $("#drawerBackdrop").hidden = true; }
$("#drawerBackdrop").onclick = closeDrawer;
document.addEventListener("keydown", (e) => { if (e.key === "Escape") { closeDrawer(); $("#journal").hidden = true; } });

// Formulaire modal générique. fields: [{name,label,type,options,value,required,hint,min,step}]
function form({ title, fields, submit = "Enregistrer", onSubmit, onChange }) {
  const dlg = $("#modal");
  const fieldHtml = (f) => {
    const req = f.required ? "required" : "";
    if (f.type === "select") return `<div class="field"><label for="f_${f.name}">${esc(f.label)}</label><select id="f_${f.name}" name="${f.name}" ${req}>${f.options}</select>${f.hint ? `<span class="hint">${esc(f.hint)}</span>` : ""}</div>`;
    if (f.type === "checkbox") return `<div class="field check"><input type="checkbox" id="f_${f.name}" name="${f.name}" ${f.value ? "checked" : ""}><label for="f_${f.name}">${esc(f.label)}</label></div>`;
    return `<div class="field"><label for="f_${f.name}">${esc(f.label)}</label><input type="${f.type || "text"}" id="f_${f.name}" name="${f.name}" value="${esc(f.value ?? "")}" ${req} ${f.min !== undefined ? `min="${f.min}"` : ""} ${f.step ? `step="${f.step}"` : ""}>${f.hint ? `<span class="hint">${esc(f.hint)}</span>` : ""}</div>`;
  };
  const rows = fields.map((f) => Array.isArray(f) ? `<div class="field-row">${f.map(fieldHtml).join("")}</div>` : fieldHtml(f)).join("");
  dlg.innerHTML = `<form method="dialog" novalidate>
    <h2>${esc(title)}</h2>
    <p class="form-error" hidden></p>
    ${rows}
    <div class="form-actions"><button type="button" class="btn ghost" data-cancel>Annuler</button><button class="btn" type="submit">${esc(submit)}</button></div>
  </form>`;
  const f = $("form", dlg);
  const flat = fields.flat();
  $("[data-cancel]", f).onclick = () => dlg.close();
  if (onChange) f.addEventListener("change", (e) => onChange(e, f));
  f.onsubmit = async (e) => {
    e.preventDefault();
    const values = {};
    for (const fd of flat) {
      const el = f.elements[fd.name];
      values[fd.name] = fd.type === "checkbox" ? el.checked : fd.type === "number" ? Number(el.value) : el.value;
    }
    const btn = $("button[type=submit]", f);
    btn.disabled = true;
    try {
      await onSubmit(values);
      dlg.close();
    } catch (err) {
      const p = $(".form-error", f);
      p.hidden = false;
      p.textContent = `${err.status ? err.status + " · " : ""}${err.message}`;
    } finally { btn.disabled = false; }
  };
  dlg.showModal();
}

async function confirmDo(question, fn) {
  if (!window.confirm(question)) return;
  try { await fn(); } catch (e) { toast(`${e.status} · ${e.message}`, "err"); }
}

/* ===================== pages ===================== */
const pages = {};

/* ---------- Aperçu ---------- */
pages[""] = async () => {
  view.innerHTML = `<div class="page-head"><div><h1>Aperçu</h1><p class="lead">La semaine de l'établissement en un coup d'œil : cours à venir, absences et état des microservices.</p></div></div><p class="muted">Chargement…</p>`;
  await loadAll();
  const now = new Date();
  const counts = (s) => db.cours.filter((c) => c.statut === s).length;
  const absences = db.appels.reduce((n, a) => n + (a.nbAbsences || 0), 0);
  const etudiants = db.users.filter((u) => u.role === "ETUDIANT");
  const next = [...db.cours].filter((c) => c.statut === "PLANIFIE").sort((a, b) => a.dateHeure.localeCompare(b.dateHeure)).slice(0, 6);
  view.innerHTML = `
    <div class="page-head">
      <div><h1>Aperçu</h1><p class="lead">${esc(now.toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long", year: "numeric" }))} · ${db.cours.length} cours au planning</p></div>
      <a class="btn" href="#/planning">Ouvrir l'emploi du temps</a>
    </div>
    <div class="today">
      <section class="sheet">
        <h2>Prochains cours</h2>
        ${next.length ? `<ul class="next-list">${next.map((c) => `
          <li data-cours="${c.id}">
            <span class="date">${new Date(c.dateHeure).getDate()}<small>${JOURS[new Date(c.dateHeure).getDay()]} ${fmtTime(c.dateHeure)}</small></span>
            <span><strong>${esc(c.matiere)}</strong><br><span class="muted">${esc(classeNom(c.classeId))} · ${esc(ensNom(c.enseignantId))} · ${esc(c.salle)}</span></span>
            ${st(c.statut)}
          </li>`).join("")}</ul>` : `<p class="empty">Aucun cours planifié. <a href="#/planning">Planifier un cours</a></p>`}
      </section>
      <section class="sheet" style="padding:0">
        <div class="figures">
          <div class="figure"><b>${db.classes.length}</b><span>classes</span></div>
          <div class="figure"><b>${db.enseignants.length}</b><span>enseignants</span></div>
          <div class="figure"><b>${etudiants.length}</b><span>étudiants</span></div>
          <div class="figure"><b>${counts("PLANIFIE")}</b><span>cours planifiés</span></div>
          <div class="figure"><b>${counts("REALISE")}</b><span>cours réalisés</span></div>
          <div class="figure"><b>${absences}</b><span>absences saisies</span></div>
        </div>
      </section>
    </div>
    <section class="sheet">
      <div class="page-head" style="margin-bottom:10px"><h2 style="margin:0">Microservices</h2><a href="#/services">Détails</a></div>
      <div class="health-strip" id="health">${SVC.map((s) => `<span class="pill"><span class="dot" data-svc="${s.key}"></span>${s.name} <span class="muted">:${s.port}</span></span>`).join("")}</div>
    </section>`;
  view.querySelectorAll("[data-cours]").forEach((li) => (li.onclick = () => showCours(Number(li.dataset.cours))));
  checkHealth();
};

/* ---------- Emploi du temps ---------- */
let weekStart = null;
let filtreClasse = "";
const mondayOf = (d) => { const x = new Date(d); x.setHours(0, 0, 0, 0); const day = (x.getDay() + 6) % 7; x.setDate(x.getDate() - day); return x; };
const HOUR_START = 8, HOUR_END = 19, DUREE = 2; // le modèle n'a pas de durée : on affiche 2 h

pages.planning = async () => {
  view.innerHTML = `<div class="page-head"><div><h1>Emploi du temps</h1></div></div><p class="muted">Chargement…</p>`;
  await loadAll("cours", "classes", "enseignants", "appels");
  if (!weekStart) {
    const upcoming = [...db.cours].filter((c) => new Date(c.dateHeure) >= new Date()).sort((a, b) => a.dateHeure.localeCompare(b.dateHeure))[0];
    weekStart = mondayOf(upcoming ? upcoming.dateHeure : new Date());
  }
  drawPlanning();
};

function drawPlanning() {
  const days = [...Array(6)].map((_, i) => { const d = new Date(weekStart); d.setDate(d.getDate() + i); return d; });
  const end = new Date(weekStart); end.setDate(end.getDate() + 7);
  const inWeek = db.cours.filter((c) => { const d = new Date(c.dateHeure); return d >= weekStart && d < end && (!filtreClasse || String(c.classeId) === filtreClasse); });
  const todayKey = new Date().toDateString();
  const absByCours = Object.fromEntries(db.appels.map((a) => [a.coursId, a.nbAbsences]));
  const h = 64;
  const hours = [...Array(HOUR_END - HOUR_START)].map((_, i) => `<div>${String(HOUR_START + i).padStart(2, "0")}:00</div>`).join("");
  view.innerHTML = `
    <div class="page-head">
      <div><h1>Emploi du temps</h1><p class="lead">${inWeek.length} cours cette semaine. Cliquez sur un cours pour voir sa classe, son enseignant et ses absences.</p></div>
      <div class="toolbar">
        <select id="filtreClasse" aria-label="Filtrer par classe"><option value="">Toutes les classes</option>${options(db.classes, "id", (c) => c.nom, filtreClasse)}</select>
        <button class="btn" id="newCours">Planifier un cours</button>
      </div>
    </div>
    <div class="toolbar" style="margin-bottom:12px">
      <div class="week-nav">
        <button class="btn ghost small" id="prevW" aria-label="Semaine précédente">‹</button>
        <strong>Semaine du ${weekStart.toLocaleDateString("fr-FR", { day: "numeric", month: "long" })}</strong>
        <button class="btn ghost small" id="nextW" aria-label="Semaine suivante">›</button>
      </div>
      <button class="btn ghost small" id="thisW">Aujourd'hui</button>
    </div>
    <div class="timetable" style="--h:${h}px">
      <div class="tt-head"></div>
      ${days.map((d) => `<div class="tt-head ${d.toDateString() === todayKey ? "today-col" : ""}">${JOURS[d.getDay()]}<small>${d.getDate()}/${d.getMonth() + 1}</small></div>`).join("")}
      <div class="tt-hours">${hours}</div>
      ${days.map((d) => {
        const lessons = inWeek.filter((c) => new Date(c.dateHeure).toDateString() === d.toDateString());
        return `<div class="tt-day ${d.toDateString() === todayKey ? "today-col" : ""}" style="height:${(HOUR_END - HOUR_START) * h}px">
          ${lessons.map((c) => {
            const t = new Date(c.dateHeure);
            const top = Math.max(0, (t.getHours() + t.getMinutes() / 60 - HOUR_START) * h);
            const abs = absByCours[c.id];
            return `<button class="lesson ${c.statut}" style="top:${top + 2}px;height:${DUREE * h - 4}px" data-cours="${c.id}" title="${esc(c.matiere)} — ${esc(label(c.statut))}">
              ${abs ? `<span class="abs" title="${abs} absence(s)">${abs}</span>` : ""}
              <strong>${esc(c.matiere)}</strong>
              <span class="meta">${fmtTime(c.dateHeure)} · ${esc(c.salle)}<br>${esc(classeNom(c.classeId))} · ${esc(ensNom(c.enseignantId))}</span>
            </button>`;
          }).join("")}
        </div>`;
      }).join("")}
    </div>
    <div class="legend">${st("PLANIFIE")} ${st("REALISE")} ${st("ANNULE")} <span>Pastille rouge : nombre d'absences. Les cours sont affichés sur 2 h.</span></div>`;
  $("#prevW").onclick = () => { weekStart.setDate(weekStart.getDate() - 7); drawPlanning(); };
  $("#nextW").onclick = () => { weekStart.setDate(weekStart.getDate() + 7); drawPlanning(); };
  $("#thisW").onclick = () => { weekStart = mondayOf(new Date()); drawPlanning(); };
  $("#filtreClasse").onchange = (e) => { filtreClasse = e.target.value; drawPlanning(); };
  $("#newCours").onclick = () => newCours();
  view.querySelectorAll("[data-cours]").forEach((b) => (b.onclick = () => showCours(Number(b.dataset.cours))));
}

function newCours() {
  const ens0 = db.enseignants[0];
  const d = new Date(weekStart); d.setDate(d.getDate() + 1);
  form({
    title: "Planifier un cours",
    submit: "Planifier",
    fields: [
      { name: "classeId", label: "Classe", type: "select", required: true, options: options(db.classes, "id", (c) => `${c.nom} (${c.niveau})`) },
      { name: "enseignantId", label: "Enseignant", type: "select", required: true, options: options(db.enseignants, "id", (e) => `${e.nom} — ${e.matiere}`) },
      { name: "matiere", label: "Matière", required: true, value: ens0?.matiere ?? "", hint: "Doit correspondre à la matière de l'enseignant (vérifié par le service planning via OpenFeign)." },
      [{ name: "date", label: "Date", type: "date", required: true, value: d.toISOString().slice(0, 10) },
       { name: "heure", label: "Heure", type: "time", required: true, value: "09:00" }],
      { name: "salle", label: "Salle", required: true, value: "Salle 10" },
    ],
    onChange: (e, f) => {
      if (e.target.name === "enseignantId") f.elements.matiere.value = byId(db.enseignants, Number(e.target.value))?.matiere ?? "";
    },
    onSubmit: async (v) => {
      const body = { classeId: Number(v.classeId), enseignantId: Number(v.enseignantId), matiere: v.matiere, dateHeure: `${v.date}T${v.heure}:00`, salle: v.salle };
      const created = await api("POST", "/api/cours", body, { idempotencyKey: crypto.randomUUID() });
      toast(`Cours planifié. Un email part vers l'enseignant (Mailpit).`, "ok");
      weekStart = mondayOf(created.dateHeure);
      await loadAll("cours", "appels");
      drawPlanning();
    },
  });
}

async function showCours(id) {
  openDrawer(`<p class="muted">Chargement du cours #${id}…</p>`);
  let d;
  try { d = await api("GET", `/api/cours/${id}/detail`); }
  catch (e) { openDrawer(`<button class="icon" onclick="this.closest('.drawer').hidden=true;document.getElementById('drawerBackdrop').hidden=true">×</button><p class="form-error">${e.status} · ${esc(e.message)}</p>`); return; }
  const abs = d.absences || [];
  openDrawer(`
    <div style="display:flex;justify-content:space-between;align-items:start"><div>${st(d.statut)}<h2>${esc(d.matiere)}</h2><p class="muted" style="margin:0">${esc(fmtDate(d.dateHeure))} à ${fmtTime(d.dateHeure)} · ${esc(d.salle)}</p></div><button class="icon" data-close aria-label="Fermer">×</button></div>
    <p class="source">Vue enrichie : service-planning a appelé service-referentiel et service-presences (OpenFeign).</p>
    <dl class="kv">
      <dt>Classe</dt><dd>${esc(d.classe?.nom)} · ${esc(d.classe?.niveau)} · ${esc(d.classe?.effectifMax)} places</dd>
      <dt>Enseignant</dt><dd>${esc(d.enseignant?.nom)}<br><span class="muted">${esc(d.enseignant?.email || "pas d'email")}</span></dd>
      <dt>Absences signalées</dt><dd>${d.nbAbsencesSignalees}</dd>
    </dl>
    <h2 style="font-size:18px">Absences (${abs.length})</h2>
    ${abs.length ? `<table><thead><tr><th>Étudiant</th><th>Motif</th><th>Justifiée</th></tr></thead><tbody>${abs.map((a) => `<tr><td>${esc(a.etudiantNom)}</td><td>${esc(a.motif)}</td><td>${a.justifiee ? "Oui" : "Non"}</td></tr>`).join("")}</tbody></table>` : `<p class="muted">Aucune absence${d.statut === "PLANIFIE" ? " : le cours n'a pas encore eu lieu" : ""}.</p>`}
    <div class="actions-row">
      ${d.statut === "PLANIFIE" ? `<button class="btn ok" data-patch="REALISE">Marquer comme réalisé</button><button class="btn danger" data-patch="ANNULE">Annuler le cours</button>` : ""}
      <a class="btn ghost" href="#/presences?cours=${d.id}">Feuille d'appel</a>
      <a class="btn ghost" href="#/notifications?cours=${d.id}">Emails envoyés</a>
    </div>`);
  $("[data-close]", $("#drawer")).onclick = closeDrawer;
  $("#drawer").querySelectorAll("a").forEach((a) => a.addEventListener("click", closeDrawer));
  $("#drawer").querySelectorAll("[data-patch]").forEach((b) => (b.onclick = async () => {
    try {
      await api("PATCH", `/api/cours/${id}/statut`, { statut: b.dataset.patch });
      toast(b.dataset.patch === "ANNULE" ? "Cours annulé : la feuille d'appel est clôturée et l'enseignant est prévenu." : "Cours marqué comme réalisé : la saisie des absences est ouverte.", "ok");
      await loadAll("cours", "appels");
      if (location.hash.startsWith("#/planning")) drawPlanning();
      showCours(id);
    } catch (e) { toast(`${e.status} · ${e.message}`, "err"); }
  }));
}

/* ---------- Feuilles d'appel ---------- */
let appelSel = null;
pages.presences = async (params) => {
  view.innerHTML = `<div class="page-head"><div><h1>Feuilles d'appel</h1></div></div><p class="muted">Chargement…</p>`;
  await loadAll("appels", "cours", "classes", "users", "enseignants");
  if (params.get("cours")) appelSel = db.appels.find((a) => a.coursId === Number(params.get("cours")))?.id ?? null;
  drawPresences();
};

async function drawPresences() {
  const coursOf = (a) => byId(db.cours, a.coursId);
  const rows = [...db.appels].sort((a, b) => (coursOf(b)?.dateHeure || "").localeCompare(coursOf(a)?.dateHeure || ""));
  view.innerHTML = `
    <div class="page-head"><div><h1>Feuilles d'appel</h1><p class="lead">Une feuille est créée automatiquement pour chaque cours planifié (événement Kafka). On ne peut saisir une absence que sur un cours réalisé.</p></div></div>
    <div class="grid-2">
      <section class="sheet">
        <h2>Feuilles (${rows.length})</h2>
        ${rows.length ? `<table><thead><tr><th>Cours</th><th>Classe</th><th>Date</th><th>Feuille</th><th>Abs.</th></tr></thead><tbody>
          ${rows.map((a) => { const c = coursOf(a); return `<tr class="clickable ${a.id === appelSel ? "selected" : ""}" data-appel="${a.id}">
            <td>${esc(c?.matiere ?? "Cours #" + a.coursId)} ${c ? st(c.statut) : ""}</td><td>${esc(classeNom(a.classeId))}</td><td>${fmtShort(a.date)}</td><td>${st(a.statut)}</td><td>${a.nbAbsences}</td></tr>`; }).join("")}
        </tbody></table>` : `<p class="empty">Aucune feuille. Planifiez un cours : sa feuille arrive en 1 à 2 secondes.</p>`}
      </section>
      <section class="sheet" id="appelPanel"><p class="muted">Choisissez une feuille pour voir et saisir les absences.</p></section>
    </div>`;
  view.querySelectorAll("[data-appel]").forEach((tr) => (tr.onclick = () => { appelSel = Number(tr.dataset.appel); drawPresences(); }));
  if (appelSel) drawAppel(appelSel);
}

async function drawAppel(id) {
  const panel = $("#appelPanel");
  const a = byId(db.appels, id);
  if (!a) return;
  const c = byId(db.cours, a.coursId);
  let abs = [];
  try { abs = await api("GET", `/api/appels/${id}/absences`); } catch (e) { toast(e.message, "err"); }
  const etudiants = db.users.filter((u) => u.role === "ETUDIANT");
  const ouvert = a.statut === "OUVERT" && c?.statut === "REALISE";
  panel.innerHTML = `
    <h2>${esc(c?.matiere ?? "Cours #" + a.coursId)}</h2>
    <p class="muted" style="margin-top:-6px">${esc(classeNom(a.classeId))} · ${c ? esc(fmtDate(c.dateHeure)) : ""} · ${st(a.statut)}</p>
    ${abs.length ? `<table><thead><tr><th>Étudiant</th><th>Motif</th><th>Justifiée</th></tr></thead><tbody>${abs.map((x) => `<tr><td>${esc(x.etudiantNom)}${x.etudiantId ? ` <span class="muted">#${x.etudiantId}</span>` : ""}</td><td>${esc(x.motif)}</td><td>${x.justifiee ? "Oui" : "Non"}</td></tr>`).join("")}</tbody></table>` : `<p class="muted">Aucune absence sur cette feuille.</p>`}
    <h2 style="font-size:18px;margin-top:18px">Saisir une absence</h2>
    ${ouvert ? "" : `<p class="muted">${a.statut === "CLOTURE" ? "Feuille clôturée : le cours a été annulé." : "Le cours n'est pas encore réalisé. Vous pouvez quand même essayer pour voir le refus 409."}</p>`}
    <form id="absForm" class="stack" style="gap:10px">
      <div class="field" style="margin:0"><label for="absEt">Étudiant</label><select id="absEt">${options(etudiants, "id", (u) => `${u.nom}${u.actif ? "" : " (inactif)"}`)}</select><span class="hint">Vérifié dans service-utilisateurs : il doit exister et être actif.</span></div>
      <div class="field-row"><div class="field" style="margin:0"><label for="absMotif">Motif</label><input id="absMotif" type="text" value="Maladie"></div><div class="field check" style="margin:22px 0 0"><input type="checkbox" id="absJ" checked><label for="absJ">Justifiée</label></div></div>
      <div><button class="btn" type="submit">Enregistrer l'absence</button></div>
    </form>`;
  $("#absForm").onsubmit = async (e) => {
    e.preventDefault();
    try {
      await api("POST", `/api/appels/${id}/absences`, { etudiantId: Number($("#absEt").value), motif: $("#absMotif").value, justifiee: $("#absJ").checked });
      toast("Absence enregistrée. Le compteur du cours se met à jour via Kafka.", "ok");
      await loadAll("appels");
      drawPresences();
    } catch (err) { toast(`${err.status} · ${err.message}`, "err"); }
  };
}

/* ---------- Classes et enseignants ---------- */
pages.referentiel = async () => {
  view.innerHTML = `<div class="page-head"><div><h1>Classes et enseignants</h1></div></div><p class="muted">Chargement…</p>`;
  await loadAll("classes", "enseignants");
  view.innerHTML = `
    <div class="page-head"><div><h1>Classes et enseignants</h1><p class="lead">Le référentiel de l'établissement (service-referentiel). Un cours ne garde que l'identifiant de sa classe et de son enseignant.</p></div></div>
    <div class="stack">
      <section class="sheet">
        <div class="page-head" style="margin-bottom:8px"><h2 style="margin:0">Classes (${db.classes.length})</h2><button class="btn" id="addClasse">Ajouter une classe</button></div>
        <table><thead><tr><th>#</th><th>Nom</th><th>Niveau</th><th>Effectif max</th><th></th></tr></thead><tbody>
          ${db.classes.map((c) => `<tr><td class="muted">${c.id}</td><td>${esc(c.nom)}</td><td>${esc(c.niveau)}</td><td>${c.effectifMax}</td>
            <td class="actions"><button class="btn ghost small" data-edit-classe="${c.id}">Modifier</button><button class="btn danger small" data-del-classe="${c.id}">Supprimer</button></td></tr>`).join("")}
        </tbody></table>
      </section>
      <section class="sheet">
        <div class="page-head" style="margin-bottom:8px"><h2 style="margin:0">Enseignants (${db.enseignants.length})</h2><button class="btn" id="addEns">Ajouter un enseignant</button></div>
        <table><thead><tr><th>#</th><th>Nom</th><th>Email</th><th>Matière</th><th>Charge max</th><th></th></tr></thead><tbody>
          ${db.enseignants.map((e) => `<tr><td class="muted">${e.id}</td><td>${esc(e.nom)}</td><td>${esc(e.email || "—")}</td><td>${esc(e.matiere)}</td><td>${e.chargeMaxHeures} h</td>
            <td class="actions"><button class="btn ghost small" data-edit-ens="${e.id}">Modifier</button><button class="btn danger small" data-del-ens="${e.id}">Supprimer</button></td></tr>`).join("")}
        </tbody></table>
      </section>
    </div>`;
  const classeForm = (c) => form({
    title: c ? "Modifier la classe" : "Ajouter une classe",
    fields: [{ name: "nom", label: "Nom", required: true, value: c?.nom }, [{ name: "niveau", label: "Niveau", required: true, value: c?.niveau ?? "5IIR" }, { name: "effectifMax", label: "Effectif max", type: "number", min: 1, value: c?.effectifMax ?? 30 }]],
    onSubmit: async (v) => { await api(c ? "PUT" : "POST", c ? `/api/classes/${c.id}` : "/api/classes", v); toast(c ? "Classe modifiée." : "Classe ajoutée.", "ok"); pages.referentiel(); },
  });
  const ensForm = (e) => form({
    title: e ? "Modifier l'enseignant" : "Ajouter un enseignant",
    fields: [{ name: "nom", label: "Nom", required: true, value: e?.nom }, { name: "email", label: "Email", type: "email", value: e?.email, hint: "Les emails de planification lui seront envoyés." }, [{ name: "matiere", label: "Matière", required: true, value: e?.matiere }, { name: "chargeMaxHeures", label: "Charge max (h)", type: "number", min: 1, value: e?.chargeMaxHeures ?? 20 }]],
    onSubmit: async (v) => { if (!v.email) v.email = null; await api(e ? "PUT" : "POST", e ? `/api/enseignants/${e.id}` : "/api/enseignants", v); toast(e ? "Enseignant modifié." : "Enseignant ajouté.", "ok"); pages.referentiel(); },
  });
  $("#addClasse").onclick = () => classeForm();
  $("#addEns").onclick = () => ensForm();
  view.querySelectorAll("[data-edit-classe]").forEach((b) => (b.onclick = () => classeForm(byId(db.classes, Number(b.dataset.editClasse)))));
  view.querySelectorAll("[data-edit-ens]").forEach((b) => (b.onclick = () => ensForm(byId(db.enseignants, Number(b.dataset.editEns)))));
  view.querySelectorAll("[data-del-classe]").forEach((b) => (b.onclick = () => confirmDo("Supprimer cette classe ?", async () => { await api("DELETE", `/api/classes/${b.dataset.delClasse}`); toast("Classe supprimée.", "ok"); pages.referentiel(); })));
  view.querySelectorAll("[data-del-ens]").forEach((b) => (b.onclick = () => confirmDo("Supprimer cet enseignant ?", async () => { await api("DELETE", `/api/enseignants/${b.dataset.delEns}`); toast("Enseignant supprimé.", "ok"); pages.referentiel(); })));
};

/* ---------- Étudiants ---------- */
pages.etudiants = async () => {
  view.innerHTML = `<div class="page-head"><div><h1>Étudiants</h1></div></div><p class="muted">Chargement…</p>`;
  await loadAll("users");
  view.innerHTML = `
    <div class="page-head"><div><h1>Étudiants</h1><p class="lead">Les utilisateurs de service-utilisateurs. Un étudiant inactif ne peut pas recevoir d'absence.</p></div><button class="btn" id="addUser">Ajouter un utilisateur</button></div>
    <section class="sheet">
      <table><thead><tr><th>#</th><th>Nom</th><th>Email</th><th>Rôle</th><th>Statut</th><th></th></tr></thead><tbody>
        ${db.users.map((u) => `<tr><td class="muted">${u.id}</td><td>${esc(u.nom)}</td><td>${esc(u.email)}</td><td>${esc(label(u.role))}</td><td>${u.actif ? `<span class="st st-UP">Actif</span>` : `<span class="st st-CLOTURE">Inactif</span>`}</td>
          <td class="actions"><button class="btn ghost small" data-edit="${u.id}">Modifier</button><button class="btn danger small" data-del="${u.id}">Supprimer</button></td></tr>`).join("")}
      </tbody></table>
    </section>`;
  const roles = [{ v: "ETUDIANT" }, { v: "ENSEIGNANT" }, { v: "ADMIN" }];
  const userForm = (u) => form({
    title: u ? "Modifier l'utilisateur" : "Ajouter un utilisateur",
    fields: [{ name: "nom", label: "Nom", required: true, value: u?.nom }, { name: "email", label: "Email", type: "email", required: true, value: u?.email }, { name: "role", label: "Rôle", type: "select", options: options(roles, "v", (r) => label(r.v), u?.role ?? "ETUDIANT") }, { name: "actif", label: "Compte actif", type: "checkbox", value: u ? u.actif : true }],
    onSubmit: async (v) => { await api(u ? "PUT" : "POST", u ? `/api/users/${u.id}` : "/api/users", v); toast(u ? "Utilisateur modifié." : "Utilisateur ajouté.", "ok"); pages.etudiants(); },
  });
  $("#addUser").onclick = () => userForm();
  view.querySelectorAll("[data-edit]").forEach((b) => (b.onclick = () => userForm(byId(db.users, Number(b.dataset.edit)))));
  view.querySelectorAll("[data-del]").forEach((b) => (b.onclick = () => confirmDo("Supprimer cet utilisateur ?", async () => { await api("DELETE", `/api/users/${b.dataset.del}`); toast("Utilisateur supprimé.", "ok"); pages.etudiants(); })));
};

/* ---------- Emails envoyés ---------- */
pages.notifications = async (params) => {
  view.innerHTML = `<div class="page-head"><div><h1>Emails envoyés</h1></div></div><p class="muted">Chargement…</p>`;
  await loadAll("cours", "classes", "enseignants");
  const sorted = [...db.cours].sort((a, b) => b.dateHeure.localeCompare(a.dateHeure));
  const sel = params.get("cours") || sorted[0]?.id || "";
  view.innerHTML = `
    <div class="page-head"><div><h1>Emails envoyés</h1><p class="lead">notification-service consomme les événements Kafka (cours planifié, annulé, absence) et envoie un email. Les emails arrivent dans <a href="http://localhost:8025" target="_blank" rel="noopener">Mailpit</a>.</p></div>
      <select id="notifCours" aria-label="Cours">${options(sorted, "id", (c) => `${fmtShort(c.dateHeure)} · ${c.matiere} · ${classeNom(c.classeId)}`, sel)}</select></div>
    <section class="sheet" id="notifList"><p class="muted">Chargement…</p></section>`;
  const load = async (id) => {
    const box = $("#notifList");
    if (!id) { box.innerHTML = `<p class="empty">Aucun cours.</p>`; return; }
    try {
      const list = await api("GET", `/api/notifications/cours/${id}`);
      const c = byId(db.cours, Number(id));
      box.innerHTML = `<h2>${esc(c?.matiere)} · destinataire : ${esc(byId(db.enseignants, c?.enseignantId)?.email || "adresse par défaut")}</h2>` + (list.length ? `<table><thead><tr><th>Événement</th><th>Statut</th><th>Créé</th><th>Envoyé</th><th>Erreur</th></tr></thead><tbody>
        ${list.map((n) => `<tr><td>${esc(label(n.eventType))}</td><td>${st(n.status)}</td><td>${n.createdAt ? new Date(n.createdAt).toLocaleString("fr-FR") : ""}</td><td>${n.sentAt ? new Date(n.sentAt).toLocaleString("fr-FR") : "—"}</td><td class="muted">${esc(n.lastError || "")}</td></tr>`).join("")}</tbody></table>`
        : `<p class="empty">Aucun email pour ce cours. Les cours de démonstration (ids 1001+) ont été insérés directement en base, sans événement : planifiez un nouveau cours pour voir un email.</p>`);
    } catch (e) { box.innerHTML = `<p class="form-error">${e.status} · ${esc(e.message)}</p>`; }
  };
  $("#notifCours").onchange = (e) => load(e.target.value);
  load(sel);
};

/* ---------- Services ---------- */
const SVC = [
  { key: "config", name: "config-server", port: 8888, health: "/svc/config/service-planning/default", role: "Fournit la configuration (config-repo) au démarrage de chaque service." },
  { key: "registry", name: "Eureka", port: 8761, health: "/svc/registry/actuator/health", role: "Annuaire : chaque service s'y enregistre, la Gateway et Feign y cherchent les adresses." },
  { key: "gateway", name: "Gateway", port: 8080, health: "/svc/gateway/actuator/health", role: "Point d'entrée unique : routes /api/**, load balancing (lb://), limite de 5 requêtes/s." },
  { key: "referentiel", name: "referentiel", port: 8081, health: "/svc/referentiel/actuator/health", role: "Classes et enseignants." },
  { key: "planning", name: "planning", port: 8082, health: "/svc/planning/actuator/health", role: "Cours : vérifie classe et enseignant (Feign), conflits de créneau, publie les événements Kafka." },
  { key: "presences", name: "presences", port: 8083, health: "/svc/presences/actuator/health", role: "Feuilles d'appel et absences : vérifie le cours et l'étudiant (Feign)." },
  { key: "notifications", name: "notification", port: 8084, health: "/svc/notifications/actuator/health", role: "Consomme Kafka et envoie les emails (Mailpit)." },
  { key: "utilisateurs", name: "utilisateurs", port: 8085, health: "/svc/utilisateurs/actuator/health", role: "Étudiants et autres utilisateurs." },
];

async function healthOf(s) {
  try { const r = await api("GET", s.health); return r?.status === "UP" || s.key === "config" ? "UP" : "DOWN"; }
  catch (e) { return e.status === 404 ? "UP" : "DOWN"; } // registry/config sans actuator : 404 = démarré
}
async function checkHealth() {
  await Promise.all(SVC.map(async (s) => {
    const state = await healthOf(s);
    const dot = document.querySelector(`[data-svc="${s.key}"]`);
    if (dot) dot.className = `dot ${state === "UP" ? "up" : "down"}`;
    const cell = document.querySelector(`[data-state="${s.key}"]`);
    if (cell) cell.innerHTML = st(state);
  }));
}

pages.services = async () => {
  view.innerHTML = `
    <div class="page-head"><div><h1>Services</h1><p class="lead">Les 8 applications Spring Boot du projet. Cette interface appelle uniquement la Gateway ; l'état ci-dessous est lu directement sur chaque service.</p></div><button class="btn ghost" id="refresh">Actualiser</button></div>
    <div class="stack">
      <section class="sheet">
        <table class="svc-table"><thead><tr><th>Application</th><th>Port</th><th>État</th><th>Rôle</th></tr></thead><tbody>
          ${SVC.map((s) => `<tr><td>${s.name}</td><td>${s.port}</td><td data-state="${s.key}"><span class="st">…</span></td><td class="muted">${esc(s.role)}</td></tr>`).join("")}
        </tbody></table>
      </section>
      <div class="grid-2">
        <section class="sheet"><h2>Enregistrés dans Eureka</h2><div id="eureka"><p class="muted">Chargement…</p></div></section>
        <section class="sheet"><h2>Routes de la Gateway</h2>
          <table><thead><tr><th>Chemin</th><th>Service</th></tr></thead><tbody>
            <tr><td><span class="route">/api/classes/**</span> <span class="route">/api/enseignants/**</span></td><td>lb://service-referentiel</td></tr>
            <tr><td><span class="route">/api/users/**</span></td><td>lb://service-utilisateurs</td></tr>
            <tr><td><span class="route">/api/cours/**</span></td><td>lb://service-planning</td></tr>
            <tr><td><span class="route">/api/appels/**</span></td><td>lb://service-presences</td></tr>
            <tr><td><span class="route">/api/notifications/**</span></td><td>lb://notification-service</td></tr>
            <tr><td><span class="route">/&lt;service&gt;/**</span></td><td>route dynamique (Eureka)</td></tr>
          </tbody></table>
          <p class="muted" style="font-size:13px;margin-bottom:0">Liens : <a href="http://localhost:8761" target="_blank" rel="noopener">Eureka</a> · <a href="http://localhost:8025" target="_blank" rel="noopener">Mailpit</a> · <a href="http://localhost:8888/service-planning/default" target="_blank" rel="noopener">Config de planning</a></p>
        </section>
      </div>
    </div>`;
  $("#refresh").onclick = () => pages.services();
  checkHealth();
  try {
    const r = await api("GET", "/svc/registry/eureka/apps");
    const apps = r?.applications?.application || [];
    $("#eureka").innerHTML = apps.length ? `<table><thead><tr><th>Nom</th><th>Instances</th><th>État</th></tr></thead><tbody>${apps.map((a) => `<tr><td>${esc(a.name)}</td><td>${a.instance.map((i) => esc(i.instanceId || i.hostName)).join("<br>")}</td><td>${a.instance.map((i) => st(i.status)).join("<br>")}</td></tr>`).join("")}</tbody></table>` : `<p class="empty">Aucun service enregistré.</p>`;
  } catch (e) { $("#eureka").innerHTML = `<p class="form-error">${e.status} · ${esc(e.message)}</p>`; }
};

/* ===================== routeur ===================== */
async function router() {
  const [route, query] = location.hash.replace(/^#\/?/, "").split("?");
  const page = pages[route] ? route : "";
  document.querySelectorAll(".side nav a").forEach((a) => {
    if (a.dataset.route === page) a.setAttribute("aria-current", "page");
    else a.removeAttribute("aria-current");
  });
  closeDrawer();
  try { await pages[page](new URLSearchParams(query || "")); }
  catch (e) { view.innerHTML = `<h1>Oups</h1><p class="form-error">${esc(e.message)}</p>`; }
  view.focus({ preventScroll: true });
}
window.addEventListener("hashchange", router);
router();
