(() => {
  "use strict";

  const state = {
    csrf: null,
    csrfHeader: "X-CSRF-TOKEN",
    user: null,
    data: { clienti: [], autisti: [], mezzi: [], compagnie: [], officine: [] },
    customerResults: [],
    errors: {},
    currentView: "dashboard",
    searchTimer: null,
    missions: [],
    driverMission: null,
    driverDetails: {},
    maps: new Map(),
    alerts: [],
    alertTimer: null,
    driverPollTimer: null,
    driverPollPending: false,
    driverSignature: "",
    deadlineTimer: null,
    deadlineRefreshAt: 0,
    deadlineRefreshPending: false,
    dashboardTimer: null,
    dashboardFailed: false,
    missionFilter: null
  };

  const ALERT_POLL_MS = 15000;
  const DRIVER_POLL_MS = 15000;
  const DASHBOARD_POLL_MS = 30000;
  // Ordine di lettura del grafico: dagli stati che richiedono un intervento a quelli finali.
  const STATUS_ORDER = ["verifica_polizza", "gestione_manuale", "nuova", "proposta", "accettata", "non_accettata", "scaduta", "eseguita", "annullata"];
  const CRITICAL_STATES = ["verifica_polizza", "gestione_manuale"];
  const CATEGORIES = {
    critiche: { label: "Critiche", stati: CRITICAL_STATES, supporto: true },
    verificaPolizza: { label: "Verifica polizza", stati: ["verifica_polizza"] },
    rifiutateTreAutisti: { label: "Rifiutate da 3 autisti", stati: ["gestione_manuale"] },
    supportoAperto: { label: "Supporto aperto", stati: [], supporto: true },
    daAssegnare: { label: "Da assegnare", stati: ["nuova"] },
    pending: { label: "Pending", stati: ["proposta", "accettata"] },
    ko: { label: "KO", stati: ["non_accettata", "scaduta"] },
    ok: { label: "OK", stati: ["eseguita"] },
    annullate: { label: "Annullate", stati: ["annullata"] }
  };
  const CATEGORY_TILES = ["critiche", "daAssegnare", "rifiutateTreAutisti", "ko", "ok", "pending", "annullate"];
  const CRITICAL_MOTIVES = {
    VERIFICA_POLIZZA: "Verifica polizza",
    TRE_RIFIUTI: "Rifiutata da 3 autisti",
    SUPPORTO_AUTISTA: "Supporto autista"
  };
  const ERROR_TEXTS = {
    invalid_request: "Controlla i dati inseriti e riprova.",
    not_found: "Elemento non trovato: aggiorna la pagina.",
    conflict: "L'operazione è in conflitto con lo stato attuale dei dati. Aggiorna e riprova.",
    address_not_found: "Indirizzo non trovato.",
    rate_limited: "Troppe richieste. Attendi un minuto e riprova.",
    service_unavailable: "Servizio esterno non disponibile: riprova più tardi.",
    internal_error: "Errore interno del server. Riprova più tardi."
  };
  const PROBLEM_LABELS = {
    RUOTA_CON_SCORTA: "Ruota forata con ruota di scorta",
    RUOTA_SENZA_SCORTA: "Ruota forata senza ruota di scorta",
    BATTERIA_SCARICA: "Batteria scarica",
    GUASTO_MOTORE: "Guasto motore",
    INCIDENTE: "Incidente"
  };
  const ROUTE_POINTS = {
    base: { label: "Deposito", symbol: "D", css: "marker-base" },
    sinistro: { label: "Luogo di fermo", symbol: "F", css: "marker-sinistro" },
    officina: { label: "Officina", symbol: "O", css: "marker-officina" }
  };
  const PROPOSABLE_STATES = ["nuova", "non_accettata", "scaduta", "gestione_manuale"];
  const alertStorageKey = (user) => `fornitori.avvisiLetti.${user?.id ?? "x"}`;

  const viewConfig = {
    clienti: {
      title: "Clienti",
      description: "Cerca per nome o targa e consulta i dati assicurativi.",
      columns: ["Cliente", "Contatti", "Veicolo", "Compagnia", "Polizza", "Città"],
      searchable: true
    },
    autisti: {
      title: "Autisti",
      description: "Elenco autisti e disponibilità operativa.",
      columns: ["Autista", "Telefono", "Zona", "Mezzo", "Disponibilità"],
      searchable: true
    },
    mezzi: {
      title: "Mezzi",
      description: "Veicoli e sedi operative registrati.",
      columns: ["Targa", "Tipologia", "Portata", "Base", "Indirizzo"],
      searchable: true
    },
    compagnie: {
      title: "Compagnie assicurative",
      description: "Recapiti delle compagnie presenti in anagrafica.",
      columns: ["Compagnia", "Telefono centrale", "Email sinistri"],
      searchable: true
    },
    officine: {
      title: "Officine",
      description: "Officine e località registrate.",
      columns: ["Officina", "Indirizzo", "Città"],
      searchable: true
    }
  };

  const $ = (selector) => document.querySelector(selector);
  const loginView = $("#loginView");
  const appView = $("#appView");
  const publicView = $("#publicView");

  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
    if (options.mutating) {
      if (!state.csrf) await loadCsrf();
      headers.set(state.csrfHeader, state.csrf);
    }

    let response;
    try {
      response = await fetch(path, {
        ...options,
        headers,
        credentials: "same-origin"
      });
    } catch (cause) {
      const error = new Error("Impossibile raggiungere il server. Verifica la connessione e riprova.");
      error.cause = cause;
      throw error;
    }

    if (response.status === 204) return null;
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      const error = new Error(errorMessage(response.status, payload));
      error.status = response.status;
      if (response.status === 403 && state.user && options.mutating) {
        // A lost session can fail CSRF first. Never replay a mutation.
        state.csrf = null;
        try { await request("/api/v1/auth/me"); } catch (ignored) { /* request handles 401 */ }
      }
      if (response.status === 401 && state.user) {
        state.csrf = null;
        showLogin("La sessione è scaduta. Accedi di nuovo.");
      }
      throw error;
    }
    return payload;
  }

  function errorMessage(status, payload) {
    if (status === 401) return "Sessione scaduta o credenziali non valide. Accedi di nuovo.";
    if (status === 403) return "Non hai i permessi necessari per questa operazione.";
    if (payload && typeof payload === "object") {
      if (payload.message) return payload.message;
      if (ERROR_TEXTS[payload.error]) return ERROR_TEXTS[payload.error];
    }
    return `Richiesta non riuscita (errore ${status}). Riprova.`;
  }

  async function loadCsrf() {
    const result = await request("/api/v1/auth/csrf");
    state.csrf = result.token;
    state.csrfHeader = result.headerName || "X-CSRF-TOKEN";
  }

  function setBusy(button, busy) {
    button.disabled = busy;
    button.querySelector(".button-label")?.classList.toggle("d-none", busy);
    button.querySelector(".spinner-border")?.classList.toggle("d-none", !busy);
  }

  function showLogin(message = "") {
    stopDriverPolling();
    stopDashboardPolling();
    state.user = null;
    state.csrf = null;
    publicView.classList.add("d-none");
    appView.classList.add("d-none");
    loginView.classList.remove("d-none");
    $("#loginAlert").textContent = message;
    $("#loginAlert").classList.toggle("d-none", !message);
    $("#loginEmail").focus();
  }

  function showApp(user) {
    state.user = user;
    publicView.classList.add("d-none");
    loginView.classList.add("d-none");
    appView.classList.remove("d-none");
    const displayName = user.nome || user.email || "Utente";
    $("#userName").textContent = displayName;
    $("#userAvatar").textContent = displayName.trim().charAt(0).toUpperCase();
    const manager = user.ruolo === "responsabile";
    $("#workspaceLabel").textContent = manager ? "Centrale operativa" : "Area autista";
    document.querySelectorAll("[data-manager-only]").forEach((item) => item.classList.toggle("d-none", !manager));
    document.querySelectorAll("[data-driver-only]").forEach((item) => item.classList.toggle("d-none", manager));
    if (manager) {
      stopDriverPolling();
      state.currentView = "dashboard";
      void loadRegistries();
      startAlertPolling();
    } else {
      stopAlertPolling();
      state.currentView = "autista";
      startDriverPolling();
    }
    goToView(state.currentView);
  }

  function showPublic() {
    stopAlertPolling();
    stopDriverPolling();
    stopDashboardPolling();
    state.user = null;
    state.csrf = null;
    loginView.classList.add("d-none");
    appView.classList.add("d-none");
    publicView.classList.remove("d-none");
  }

  function showPageAlert(message, type = "danger") {
    const alert = $("#pageAlert");
    alert.className = `alert alert-${type}`;
    alert.textContent = message;
  }

  function clearPageAlert() {
    $("#pageAlert").classList.add("d-none");
    $("#pageAlert").textContent = "";
  }

  async function loadRegistries() {
    clearPageAlert();
    const endpoints = {
      clienti: "/api/v1/clienti",
      autisti: "/api/v1/autisti",
      mezzi: "/api/v1/mezzi",
      compagnie: "/api/v1/compagnie",
      officine: "/api/v1/officine"
    };
    const results = await Promise.all(Object.entries(endpoints).map(async ([key, url]) => {
      try {
        return [key, await request(url), null];
      } catch (error) {
        return [key, [], error];
      }
    }));

    const errors = [];
    for (const [key, data, error] of results) {
      state.data[key] = data;
      if (key === "clienti") state.customerResults = data;
      state.errors[key] = error;
      if (error) errors.push(`${viewConfig[key]?.title || key}: ${error.message}`);
    }

    renderCounts();
    renderOverview();
    renderCompanies();
    if (state.currentView === "missioni") renderMissionList();
    else if (viewConfig[state.currentView]) renderRegistry(state.currentView);
    if (errors.length) showPageAlert(`Alcune anagrafiche non sono state caricate. ${errors.join(" ")}`);
  }

  function renderCounts() {
    $("#customerCount").textContent = state.errors.clienti ? "—" : state.data.clienti.length;
    $("#driverCount").textContent = state.errors.autisti ? "—" : state.data.autisti.length;
    $("#vehicleCount").textContent = state.errors.mezzi ? "—" : state.data.mezzi.length;
    $("#workshopCount").textContent = state.errors.officine ? "—" : state.data.officine.length;
  }

  function renderOverview() {
    const resources = [
      ["Clienti", "clienti"],
      ["Autisti", "autisti"],
      ["Mezzi", "mezzi"],
      ["Compagnie assicurative", "compagnie"],
      ["Officine", "officine"]
    ];
    $("#overviewResources").innerHTML = resources.map(([label, key]) => {
      const count = state.data[key].length;
      const value = state.errors[key]
        ? '<span class="resource-error">Non disponibile</span>'
        : `<span class="resource-value">${count} ${count === 1 ? "elemento" : "elementi"}</span>`;
      return `<div class="resource-row"><span>${label}</span>${value}</div>`;
    }).join("");
  }

  function renderCompanies() {
    const select = $("#customerCompany");
    const selected = select.value;
    select.innerHTML = '<option value="">Seleziona compagnia</option>';
    for (const company of state.data.compagnie) {
      const option = document.createElement("option");
      option.value = company.id;
      option.textContent = company.nome;
      select.append(option);
    }
    if (selected) select.value = selected;
    $("#addCustomerButton").classList.toggle("d-none", state.data.compagnie.length === 0);
    const customer = $("#missionCustomer");
    const selectedCustomer = customer.value;
    customer.innerHTML = '<option value="">Seleziona cliente</option>';
    for (const item of state.data.clienti) {
      const option = document.createElement("option");
      option.value = item.id;
      option.textContent = `${fullName(item)} · ${item.targa}`;
      customer.append(option);
    }
    if (selectedCustomer) customer.value = selectedCustomer;
    const workshop = $("#missionWorkshop");
    const selectedWorkshop = workshop.value;
    workshop.innerHTML = '<option value="">Seleziona officina</option>';
    for (const item of state.data.officine) {
      const option = document.createElement("option");
      option.value = item.id;
      option.textContent = `${item.nome} · ${item.citta}`;
      workshop.append(option);
    }
    if (selectedWorkshop) workshop.value = selectedWorkshop;
  }

  /** Una sola riga con i recapiti utili; il resto dell'anagrafica si apre su richiesta. */
  function missionCustomerRow(mission) {
    const registry = state.data.clienti.find((item) => item.id === mission.clienteId);
    const name = fullName({ nome: mission.clienteNome, cognome: mission.clienteCognome }) || fullName(registry);
    const phone = mission.clienteTelefono || registry?.telefono;
    const plate = mission.targaDichiarata || registry?.targa;
    const parts = [
      name
        ? `<strong>${safe(name)}</strong>`
        : `<span class="data-unavailable">${mission.clienteId ? `Cliente #${safe(mission.clienteId)}` : "Cliente da identificare"}</span>`,
      telLink(phone),
      plate ? `Targa <strong>${safe(plate)}</strong>` : "",
      mission.telefonoSulPosto ? `Sul posto ${telLink(mission.telefonoSulPosto)}` : ""
    ].filter(Boolean).join('<span class="customer-sep"> · </span>');
    const details = registry
      ? `<details class="customer-details"><summary>Dettagli cliente</summary><dl>
          <div><dt>Veicolo</dt><dd>${safe(registry.modelloVeicolo)}</dd></div>
          <div><dt>Compagnia</dt><dd>${safe(registry.compagniaNome)}</dd></div>
          <div><dt>Polizza</dt><dd>${safe(registry.numeroPolizza)}, scade ${safe(formatDate(registry.scadenzaPolizza))}</dd></div>
          <div><dt>Città</dt><dd>${safe(registry.cittaResidenza)}</dd></div>
        </dl></details>`
      : "";
    return `<div class="customer-row"><div class="customer-line">${parts}</div>${details}</div>`;
  }

  function missionWorkshopMarkup(mission, useRegistry = false) {
    if (!mission.officinaId) {
      return `<div><span class="field-label">Officina</span><strong>${mission.tipoIntervento === "traino"
        ? "Non indicata"
        : "Non prevista"}</strong></div>`;
    }
    const workshop = useRegistry
      ? state.data.officine.find((item) => item.id === mission.officinaId)
      : mission.officinaNome ? { nome: mission.officinaNome, indirizzo: mission.officinaIndirizzo, citta: mission.officinaCitta } : null;
    const detail = workshop
      ? [workshop.indirizzo, workshop.citta].filter(Boolean).map(safe).join(", ")
      : useRegistry && state.errors.officine
        ? "Anagrafica officine non disponibile"
        : "Dettaglio non fornito dall'API missioni";
    return `<div><span class="field-label">Officina</span><strong>${safe(workshop?.nome || `Officina #${mission.officinaId}`)}</strong>${detail ? `<span class="${workshop ? "" : "data-unavailable"}">${detail}</span>` : ""}</div>`;
  }

  function driverCustomerMarkup(mission) {
    const customer = state.driverDetails[mission.id]?.cliente;
    if (!customer) {
      return `<div><span class="field-label">Cliente</span><strong>Cliente #${safe(mission.clienteId)}</strong><span class="data-unavailable">Dettagli cliente non disponibili</span></div>`;
    }
    if (!customer.nome) {
      const model = customer.modelloVeicolo ? `<span>${safe(customer.modelloVeicolo)}</span>` : "";
      return `<div><span class="field-label">Cliente</span><span class="data-unavailable">Nome e recapito visibili dopo l'accettazione</span>${model}</div>`;
    }
    const vehicle = [customer.targa, customer.modelloVeicolo].filter(Boolean).map(safe).join(" · ");
    return `<div><span class="field-label">Cliente</span><strong>${safe(customer.nome)}</strong><a href="tel:${safe(customer.telefono)}">${safe(customer.telefono)}</a><span>${vehicle}</span></div>`;
  }

  function driverWorkshopMarkup(mission) {
    const workshop = state.driverDetails[mission.id]?.officina;
    if (!workshop) return missionWorkshopMarkup(mission);
    const address = [workshop.indirizzo, workshop.citta].filter(Boolean).map(safe).join(", ");
    return `<div><span class="field-label">Officina</span><strong>${safe(workshop.nome)}</strong><span>${address}</span></div>`;
  }

  function driverEstimateMarkup(mission) {
    return `<div><span class="field-label">Guadagno stimato</span><strong data-estimate="${safe(mission.id)}">${safe(estimateText(state.driverDetails[mission.id]))}</strong></div>`;
  }

  function estimateText(detail) {
    return detail?.guadagnoStimato != null ? formatEuro(detail.guadagnoStimato) : "Calcolo in corso…";
  }

  function missionMapMarkup(missionId, hidden = false) {
    return `<div class="mission-map-area${hidden ? " d-none" : ""}" data-map-area="${safe(missionId)}"><p class="small text-secondary mb-2" data-map-status>Caricamento mappa…</p><div class="route-km d-none" data-map-km></div><div class="mission-map d-none" data-map></div><div class="route-legend d-none" data-map-legend></div><ol class="mission-map-points small d-none" data-map-points></ol></div>`;
  }

  function routeTotalKm(route) {
    return route?.kmTotali ?? route?.km ?? route?.kmStimati ?? null;
  }

  function formatKm(value) {
    return `${new Intl.NumberFormat("it-IT", { maximumFractionDigits: 2 }).format(Number(value))} km`;
  }

  function routeKmMarkup(route) {
    const total = routeTotalKm(route);
    const rows = [
      ["Andata", route.kmAndata],
      ["Traino", Number(route.kmTraino) > 0 ? route.kmTraino : null],
      ["Ritorno", route.kmRitorno]
    ].filter(([, value]) => value != null);
    return rows.map(([label, value]) => `<span>${label} <strong>${safe(formatKm(value))}</strong></span>`).join("")
      + (total != null ? `<span class="route-km-total">Totali <strong>${safe(formatKm(total))}</strong></span>` : "");
  }

  function routeMarkerIcon(type, shift) {
    const point = ROUTE_POINTS[type] || { symbol: "?", css: "" };
    return L.divIcon({
      className: `route-marker ${point.css}`,
      html: `<span>${point.symbol}</span>`,
      iconSize: [28, 28],
      // Deposito e officina possono coincidere: il secondo segnaposto si sposta di lato.
      iconAnchor: [14 - shift, 14],
      popupAnchor: [shift, -14]
    });
  }

  function removeMaps() {
    state.maps.forEach((map) => map.remove());
    state.maps.clear();
  }

  function mapAreaFor(missionId) {
    // Dopo un cambio utente la pagina dell'altro ruolo resta nel DOM con le stesse missioni.
    const page = state.user?.ruolo === "autista" ? "#driverPage" : "#missionPage";
    return $(page)?.querySelector(`[data-map-area="${CSS.escape(String(missionId))}"]`);
  }

  async function showMissionMap(missionId, driverId) {
    const area = mapAreaFor(missionId);
    if (!area) return null;
    const status = area.querySelector("[data-map-status]");
    status.classList.remove("text-danger");
    status.textContent = "Caricamento mappa…";
    const params = new URLSearchParams({ missionId });
    if (driverId) params.set("driverId", driverId);
    try {
      // Dopo l'hardening del backend anche questa GET richiede il token CSRF.
      const route = await request(`/api/v1/maps/route?${params}`, { mutating: true });
      const current = mapAreaFor(missionId);
      if (!current) return route;
      drawMissionMap(missionId, current, route);
      return route;
    } catch (error) {
      const current = mapAreaFor(missionId);
      if (current) {
        const message = current.querySelector("[data-map-status]");
        message.classList.add("text-danger");
        message.textContent = ({
          422: "Indirizzo non trovato: la mappa non è disponibile per questa missione.",
          503: "Servizio mappe non disponibile: riprova più tardi. I chilometri si possono comunque inserire."
        })[error.status] || error.message;
      }
      return null;
    }
  }

  function drawMissionMap(missionId, area, route) {
    const status = area.querySelector("[data-map-status]");
    if (!window.L) {
      status.classList.add("text-danger");
      status.textContent = "Mappa non caricata: controlla la connessione.";
      return;
    }
    const container = area.querySelector("[data-map]");
    const pointsList = area.querySelector("[data-map-points]");
    container.classList.remove("d-none");
    pointsList.classList.remove("d-none");
    state.maps.get(missionId)?.remove();
    const map = L.map(container);
    state.maps.set(missionId, map);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);
    pointsList.replaceChildren();
    const taken = new Map();
    const types = new Set();
    const coordinates = route.punti.map((point) => {
      const name = ROUTE_POINTS[point.tipo]?.label || point.tipo;
      const label = `${name}: ${point.descrizione}`;
      const popup = document.createElement("span");
      const title = document.createElement("strong");
      title.textContent = name;
      popup.append(title, document.createElement("br"), point.descrizione ?? "");
      const place = `${point.lat},${point.lng}`;
      const shift = (taken.get(place) || 0) * 30;
      taken.set(place, (taken.get(place) || 0) + 1);
      types.add(point.tipo);
      const marker = L.marker([point.lat, point.lng], { title: label, alt: label, icon: routeMarkerIcon(point.tipo, shift) }).addTo(map).bindPopup(popup);
      if (point.tipo === "sinistro") marker.setZIndexOffset(1000);
      const item = document.createElement("li");
      item.textContent = label;
      pointsList.append(item);
      return [point.lat, point.lng];
    });
    const legend = area.querySelector("[data-map-legend]");
    legend.innerHTML = Object.entries(ROUTE_POINTS).filter(([type]) => types.has(type)).map(([, point]) =>
      `<span><span class="route-marker ${point.css}"><span>${point.symbol}</span></span>${point.label}</span>`
    ).join("");
    legend.classList.remove("d-none");
    const kmBox = area.querySelector("[data-map-km]");
    kmBox.innerHTML = routeKmMarkup(route);
    kmBox.classList.toggle("d-none", !route.geometria || !kmBox.innerHTML);
    let fit;
    if (route.geometria) {
      const line = L.geoJSON(route.geometria, { style: { color: "#6d28d9", weight: 5 } }).addTo(map);
      const bounds = line.getBounds().extend(coordinates);
      fit = () => map.fitBounds(bounds, { padding: [32, 32] });
      status.textContent = "Percorso stimato con rientro al deposito (fonte: OSRM). I km effettivi si confermano alla chiusura.";
    } else {
      fit = () => map.setView(coordinates[0], 15);
      status.textContent = "Scegli un autista per calcolare percorso e chilometri.";
    }
    fit();
    // Il riquadro può cambiare misura dopo il disegno (scrollbar, finestra, layout): si riadatta da solo.
    if (window.ResizeObserver) {
      const observer = new ResizeObserver(() => {
        // Fuori dal ciclo di ridimensionamento, per non generare "ResizeObserver loop" nel browser.
        window.requestAnimationFrame(() => {
          if (!container.isConnected) {
            observer.disconnect();
            return;
          }
          map.invalidateSize();
          fit();
        });
      });
      observer.observe(container);
    }
  }

  async function loadDriverRoute(mission) {
    const route = await showMissionMap(mission.id);
    if (!route) {
      const estimate = document.querySelector(`[data-estimate="${CSS.escape(String(mission.id))}"]`);
      if (estimate && state.driverDetails[mission.id]?.guadagnoStimato == null) estimate.textContent = "Non disponibile";
      return;
    }
    const input = $("#actualKm");
    if (input && !input.value && state.driverMission?.id === mission.id && routeTotalKm(route) != null) {
      input.value = routeTotalKm(route);
    }
    try {
      const detail = await request(`/api/v1/autista/missioni/${mission.id}`);
      state.driverDetails[mission.id] = detail;
      const estimate = document.querySelector(`[data-estimate="${CSS.escape(String(mission.id))}"]`);
      if (estimate) estimate.textContent = estimateText(detail);
    } catch (error) {
      console.error("Stima del guadagno non aggiornata:", error);
    }
  }

  function lastSeenAlertId() {
    try {
      return Number(localStorage.getItem(alertStorageKey(state.user))) || 0;
    } catch (error) {
      return 0;
    }
  }

  function markAlertsRead() {
    const newest = state.alerts.reduce((max, alert) => Math.max(max, alert.id), lastSeenAlertId());
    try {
      localStorage.setItem(alertStorageKey(state.user), String(newest));
    } catch (error) {
      console.error("Avvisi letti non salvati:", error);
    }
    renderAlerts();
  }

  function renderAlerts() {
    const seen = lastSeenAlertId();
    const unread = state.alerts.filter((alert) => alert.id > seen).length;
    const badge = $("#alertBadge");
    badge.textContent = unread > 99 ? "99+" : String(unread);
    badge.classList.toggle("d-none", unread === 0);
    $("#alertButton").setAttribute("aria-label", unread ? `Avvisi, ${unread} non letti` : "Avvisi");
    $("#alertList").innerHTML = state.alerts.length
      ? state.alerts.map((alert) => {
        const driver = alert.autistaNome || "Un autista";
        const text = alert.tipo === "rifiutata"
          ? `${driver} ha rifiutato la missione`
          : `${driver} non ha risposto: proposta scaduta`;
        return `<article class="alert-item${alert.id > seen ? " unread" : ""}"><span>${safe(text)} <strong>${safe(alert.codice)}</strong></span><time>${safe(formatDateTime(alert.quando))}</time><button class="btn btn-sm btn-outline-primary align-self-start" type="button" data-alert-mission="${safe(alert.missioneId)}">Apri missione</button></article>`;
      }).join("")
      : '<p class="small text-secondary mb-0">Nessun avviso.</p>';
  }

  async function loadAlerts() {
    try {
      state.alerts = await request("/api/v1/centrale/avvisi");
      renderAlerts();
    } catch (error) {
      if (error.status === 401) stopAlertPolling();
      else console.error("Avvisi non aggiornati:", error);
    }
  }

  function startAlertPolling() {
    stopAlertPolling();
    void loadAlerts();
    state.alertTimer = window.setInterval(() => void loadAlerts(), ALERT_POLL_MS);
  }

  function stopAlertPolling() {
    if (state.alertTimer) window.clearInterval(state.alertTimer);
    state.alertTimer = null;
    state.alerts = [];
    $("#alertPanel")?.classList.add("d-none");
    $("#alertBadge")?.classList.add("d-none");
  }

  function driverSignature(missions) {
    return missions.map((mission) => `${mission.id}:${mission.stato}:${mission.scadenzaIl}`).sort().join("|");
  }

  /** Controlla solo se è cambiato qualcosa: ridisegnare ricalcolerebbe il percorso e perderebbe il testo scritto. */
  async function pollDriverMissions() {
    if (state.user?.ruolo !== "autista" || state.currentView !== "autista" || state.driverPollPending) return;
    state.driverPollPending = true;
    try {
      const missions = await request("/api/v1/autista/missioni");
      if (driverSignature(missions) !== state.driverSignature) await loadDriverMissions();
    } catch (error) {
      if (error.status === 401) stopDriverPolling();
      else console.error("Proposte non aggiornate:", error);
    } finally {
      state.driverPollPending = false;
    }
  }

  function startDriverPolling() {
    stopDriverPolling();
    state.driverPollTimer = window.setInterval(() => void pollDriverMissions(), DRIVER_POLL_MS);
  }

  function stopDriverPolling() {
    if (state.driverPollTimer) window.clearInterval(state.driverPollTimer);
    state.driverPollTimer = null;
    state.driverSignature = "";
  }

  async function openMissionFromAlert(missionId) {
    $("#alertPanel").classList.add("d-none");
    $("#alertButton").setAttribute("aria-expanded", "false");
    state.currentView = "missioni";
    state.missionFilter = null;
    stopDashboardPolling();
    $("#missionMode").value = "live";
    $("#missionStatus").value = "";
    goToView("missioni");
    await loadMissions(true);
    const card = document.querySelector(`[data-mission-card="${CSS.escape(String(missionId))}"]`);
    if (!card) return;
    card.classList.add("highlight");
    card.scrollIntoView({ block: "center" });
    window.setTimeout(() => card.classList.remove("highlight"), 4000);
  }

  function costCorrectionMarkup(mission) {
    if (mission.stato !== "eseguita" || $("#missionMode").value !== "live") return "";
    const note = mission.rettificaMotivo
      ? `<p class="small text-secondary mb-2">Calcolato ${safe(formatEuro(mission.costoCalcolato))}, corretto: ${safe(mission.rettificaMotivo)}</p>`
      : "";
    return `<section class="mission-cost">${note}<form class="cost-form" data-cost-form="${safe(mission.id)}"><label class="visually-hidden" for="cost-${safe(mission.id)}">Nuovo costo</label><div class="input-group input-group-sm"><input class="form-control" id="cost-${safe(mission.id)}" name="costoFinale" type="number" min="0" step="0.01" value="${safe(mission.costoFinale)}" required><span class="input-group-text">€</span></div><label class="visually-hidden" for="reason-${safe(mission.id)}">Motivo della correzione</label><input class="form-control form-control-sm" id="reason-${safe(mission.id)}" name="motivo" minlength="3" maxlength="1000" placeholder="Motivo della correzione" required><button class="btn btn-sm btn-outline-primary" type="submit">Correggi costo</button></form></section>`;
  }

  function resetRegistrySearch() {
    $("#registrySearch").value = "";
    window.clearTimeout(state.searchTimer);
    state.customerResults = state.data.clienti;
  }

  function goToView(view) {
    const previousView = state.currentView;
    const permitted = state.user?.ruolo === "responsabile"
      ? ["dashboard", "missioni", "costi", ...Object.keys(viewConfig)]
      : ["autista", "storico"];
    state.currentView = permitted.includes(view) ? view : (state.user?.ruolo === "responsabile" ? "dashboard" : "autista");
    if (state.currentView !== previousView) resetRegistrySearch();
    const pages = {
      dashboard: "#dashboardPage",
      missioni: "#missionPage",
      costi: "#costPage",
      autista: "#driverPage",
      storico: "#historyPage"
    };
    document.querySelectorAll(".page-section").forEach((page) => page.classList.add("d-none"));
    if (pages[state.currentView]) {
      $(pages[state.currentView]).classList.remove("d-none");
    } else {
      $("#registryPage").classList.remove("d-none");
      renderRegistry(state.currentView);
    }
    const titles = { dashboard: "Dashboard", missioni: "Missioni", costi: "Costi e analisi", autista: "Le mie missioni", storico: "Storico e guadagni" };
    $("#currentSection").textContent = titles[state.currentView] || viewConfig[state.currentView]?.title || "";
    document.querySelectorAll("[data-view-link]").forEach((button) => {
      button.classList.toggle("active", button.dataset.viewLink === state.currentView);
    });
    if (state.currentView === "dashboard") startDashboardPolling();
    else stopDashboardPolling();
    if (state.currentView === "missioni") void loadMissions(true);
    if (state.currentView === "costi") void loadCosts();
    if (state.currentView === "autista") void loadDriverMissions(true);
    if (state.currentView === "storico") void loadDriverHistory();
    $("#sidebar").classList.remove("sidebar-open");
    $("#menuButton").setAttribute("aria-expanded", "false");
  }

  function renderRegistry(view, search = $("#registrySearch").value.trim()) {
    const config = viewConfig[view];
    if (!config) return;
    $("#registryTitle").textContent = config.title;
    $("#registryDescription").textContent = config.description;
    $("#registrySearch").placeholder = view === "clienti" ? "Cerca per nome o targa…" : `Cerca in ${config.title.toLowerCase()}…`;
    $("#registrySearch").classList.toggle("d-none", !config.searchable);
    $("#addCustomerButton").classList.toggle("d-none", view !== "clienti" || state.data.compagnie.length === 0);
    $("#registryHead").innerHTML = `<tr>${config.columns.map((column) => `<th scope="col">${column}</th>`).join("")}</tr>`;

    const filtered = filterItems(view, search);
    $("#registryCount").textContent = state.errors[view]
      ? "Dati non disponibili"
      : `${filtered.length} ${filtered.length === 1 ? "risultato" : "risultati"}`;
    $("#registryBody").innerHTML = filtered.map((item) => rowFor(view, item)).join("");
    $("#registryEmpty").classList.toggle("d-none", filtered.length > 0 && !state.errors[view]);
    $("#registryBody").classList.toggle("d-none", filtered.length === 0 || Boolean(state.errors[view]));
    $("#registryEmpty h2").textContent = state.errors[view] ? "Dati non disponibili" : "Nessun risultato";
    $("#registryEmpty p").textContent = state.errors[view]
      ? "Aggiorna la pagina o riprova più tardi."
      : "Prova a modificare i termini di ricerca.";
    $("#registryAlert").classList.add("d-none");
    if (state.errors[view]) {
      $("#registryAlert").textContent = state.errors[view].message;
      $("#registryAlert").classList.remove("d-none");
    }
  }

  function filterItems(view, search) {
    const items = view === "clienti" ? state.customerResults : (state.data[view] || []);
    const normalized = search.toLocaleLowerCase("it");
    if (!normalized) return items;
    return items.filter((item) => Object.values(item)
      .some((value) => String(value ?? "").toLocaleLowerCase("it").includes(normalized)));
  }

  function safe(value) {
    return String(value ?? "—")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function rowFor(view, item) {
    if (view === "clienti") {
      return `<tr>
        <td class="primary-cell">${safe(fullName(item))}<div class="secondary-cell">ID ${safe(item.id)}</div></td>
        <td>${safe(item.telefono)}</td>
        <td>${safe(item.targa)}<div class="secondary-cell">${safe(item.modelloVeicolo)}</div></td>
        <td>${safe(item.compagniaNome)}</td>
        <td>${safe(item.numeroPolizza)}<div class="secondary-cell">Scade ${safe(formatDate(item.scadenzaPolizza))}</div></td>
        <td>${safe(item.cittaResidenza)}</td>
      </tr>`;
    }
    if (view === "autisti") {
      const availability = driverAvailability(item);
      return `<tr>
        <td class="primary-cell">${safe(item.nome)}<div class="secondary-cell">ID ${safe(item.id)}</div></td>
        <td>${safe(item.telefono)}</td><td>${safe(item.zona)}</td>
        <td>${safe(vehiclePlate(item.mezzoId))}</td>
        <td><span class="availability${availability.free ? "" : " busy"}">${availability.label}</span></td>
      </tr>`;
    }
    if (view === "mezzi") {
      return `<tr><td class="primary-cell">${safe(item.targa)}</td><td>${safe(item.tipologia)}</td><td>${safe(item.portataT)} t</td><td>${safe(item.baseCitta)}</td><td>${safe(item.baseIndirizzo)}</td></tr>`;
    }
    if (view === "compagnie") {
      return `<tr><td class="primary-cell">${safe(item.nome)}</td><td>${safe(item.telefonoCentrale)}</td><td>${safe(item.emailSinistri)}</td></tr>`;
    }
    return `<tr><td class="primary-cell">${safe(item.nome)}</td><td>${safe(item.indirizzo)}</td><td>${safe(item.citta)}</td></tr>`;
  }

  function fullName(person) {
    return [person?.nome, person?.cognome].filter(Boolean).join(" ");
  }

  function driverAvailability(driver) {
    if (driver.disponibile === false || driver.disponibilita === "non_disponibile") {
      return { free: false, label: "Non disponibile" };
    }
    if (driver.disponibilita === "occupato") return { free: false, label: "Occupato" };
    return { free: true, label: "Disponibile" };
  }

  function vehiclePlate(vehicleId) {
    const vehicle = state.data.mezzi.find((item) => item.id === vehicleId);
    return vehicle ? vehicle.targa : "Non assegnato";
  }

  function formatDate(value) {
    if (!value) return "—";
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.valueOf()) ? value : new Intl.DateTimeFormat("it-IT").format(date);
  }

  function formatDateTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.valueOf()) ? "—" : new Intl.DateTimeFormat("it-IT", {
      dateStyle: "short", timeStyle: "short"
    }).format(date);
  }

  function formatEuro(value) {
    return new Intl.NumberFormat("it-IT", { style: "currency", currency: "EUR" }).format(Number(value || 0));
  }

  function showInlineAlert(selector, message, type = "danger") {
    const alert = $(selector);
    alert.className = `alert alert-${type}`;
    alert.textContent = message;
  }

  function loadingMarkup(text) {
    return `<div class="empty-state" role="status"><span class="spinner-border spinner-border-sm text-secondary" aria-hidden="true"></span><p class="small text-secondary mb-0 mt-2">${safe(text)}</p></div>`;
  }

  async function loadMissions(showLoading = false) {
    const alert = $("#missionAlert");
    alert.classList.add("d-none");
    if (showLoading) {
      removeMaps();
      $("#missionList").innerHTML = loadingMarkup("Caricamento missioni…");
    }
    const mode = $("#missionMode").value;
    const status = $("#missionStatus").value;
    const filter = state.missionFilter;
    const params = new URLSearchParams({ mode });
    // Il parametro stato accetta un solo valore: i filtri della dashboard si applicano qui.
    if (status && !filter) params.set("stato", status);
    renderMissionFilter();
    try {
      const [missions, dashboard, drivers] = await Promise.all([
        request(`/api/v1/missioni?${params}`),
        request(`/api/v1/dashboard?mode=${encodeURIComponent(mode)}`),
        request("/api/v1/autisti")
      ]);
      state.missions = filter ? missions.filter((mission) => matchesFilter(mission, filter)) : missions;
      state.data.autisti = drivers;
      $("#missionPendingCount").textContent = dashboard.pending;
      $("#missionDoneCount").textContent = dashboard.eseguite;
      $("#missionExpiredCount").textContent = dashboard.scadute;
      $("#missionRejectedCount").textContent = dashboard.nonAccettate;
      $("#missionRevenue").textContent = formatEuro(dashboard.totaleEur);
      $("#missionCount").textContent = `${state.missions.length} ${state.missions.length === 1 ? "missione" : "missioni"}`;
      renderMissionList();
    } catch (error) {
      showInlineAlert("#missionAlert", error.message);
      $("#missionList").innerHTML = "";
      $("#missionCount").textContent = "Dati non disponibili";
    }
  }

  function matchesFilter(mission, filter) {
    return filter.stati.includes(mission.stato) || (filter.supporto === true && mission.supportoAperto === true);
  }

  function renderMissionFilter() {
    const filter = state.missionFilter;
    $("#missionFilterChip").classList.toggle("d-none", !filter);
    $("#missionFilterLabel").textContent = filter ? `Filtro: ${filter.label}` : "";
  }

  function showFilteredMissions(filter) {
    state.missionFilter = filter;
    $("#missionMode").value = "live";
    $("#missionStatus").value = "";
    goToView("missioni");
  }

  function renderMissionList() {
    const list = $("#missionList");
    removeMaps();
    if (!state.missions.length) {
      list.innerHTML = '<div class="empty-state"><span class="empty-icon">↗</span><h2 class="h6">Nessuna missione</h2><p class="small text-secondary mb-0">Crea una missione o modifica i filtri.</p></div>';
      return;
    }
    list.innerHTML = state.missions.map(missionCardMarkup).join("");
    updateCountdowns();
  }

  function driverName(driverId) {
    return state.data.autisti.find((driver) => driver.id === driverId)?.nome || (driverId ? `Autista #${driverId}` : "Non assegnato");
  }

  function interventionText(mission) {
    return mission.tipoIntervento === "traino" ? "Traino" : "Riparazione sul posto";
  }

  function missionOutcomeText(mission) {
    if (mission.kmEffettivi != null) {
      return `${mission.kmEffettivi} km · ${mission.costoFinale != null ? formatEuro(mission.costoFinale) : "Importo non disponibile"}`;
    }
    return interventionText(mission);
  }

  function missionCardMarkup(mission) {
    const id = safe(mission.id);
    const live = $("#missionMode").value === "live";
    const canPropose = live && PROPOSABLE_STATES.includes(mission.stato);
    const driverOptions = state.data.autisti.map((driver) => {
      const availability = driverAvailability(driver);
      return `<option value="${safe(driver.id)}"${availability.free ? "" : " disabled"}>${safe(driver.nome)}${availability.free ? "" : ` · ${availability.label.toLowerCase()}`}</option>`;
    }).join("");
    const refusals = Number(mission.esitiNegativi) > 0
      ? `<span class="refusal-count">Esiti negativi: ${safe(mission.esitiNegativi)} su 3</span>`
      : "";
    const problem = PROBLEM_LABELS[mission.tipoProblema];
    return `<article class="mission-card" data-mission-card="${id}">
      <div class="mission-card-header">
        <div><span class="mission-code">${safe(mission.codice)}</span><span class="mission-status status-${safe(mission.stato)}">${safe(statusLabel(mission.stato))}</span></div>
        <span class="small text-secondary">${safe(formatDateTime(mission.chiusaIl || mission.annullataIl || mission.scadenzaIl || mission.creataIl))}</span>
      </div>
      ${missionCustomerRow(mission)}
      <div class="mission-card-grid">
        <div><span class="field-label">Luogo di fermo</span><strong>${safe(mission.origineIndirizzo)}</strong></div>
        <div><span class="field-label">Autista</span><strong>${safe(driverName(mission.autistaId))}</strong>${mission.stato === "proposta" ? `<span class="countdown" data-deadline="${safe(mission.scadenzaIl)}"></span>` : ""}${refusals}</div>
        ${missionWorkshopMarkup(mission, true)}
        <div><span class="field-label">Esito / importo</span><strong>${safe(missionOutcomeText(mission))}</strong></div>
      </div>
      <p class="mission-description">${problem && problem === mission.descrizioneProblema ? "" : safe(mission.descrizioneProblema)}${problem ? `<span class="problem-tag">${safe(problem)}</span>` : ""}</p>
      <div class="mission-toolbar">
        <button class="btn btn-sm btn-outline-secondary" type="button" data-mission-notes="${id}" aria-expanded="false" aria-controls="mission-notes-${id}">Mostra avvisi</button>
        <button class="btn btn-sm btn-outline-secondary" type="button" data-mission-map="${id}" aria-expanded="false">Mostra mappa</button>
        <button class="btn btn-sm btn-outline-secondary" type="button" data-mission-proposals="${id}" aria-expanded="false" aria-controls="mission-proposals-${id}">Mostra storico proposte</button>
      </div>
      <div class="note-list d-none" id="mission-notes-${id}"></div>
      ${missionMapMarkup(mission.id, true)}
      <div class="note-list d-none" id="mission-proposals-${id}"></div>
      ${costCorrectionMarkup(mission)}
      ${canPropose ? interventionFormMarkup(mission) : ""}
      ${canPropose ? `<div class="mission-propose"><label class="visually-hidden" for="driver-${id}">Seleziona autista</label><select class="form-select form-select-sm" id="driver-${id}"><option value="">Seleziona autista</option>${driverOptions}</select><button class="btn btn-sm btn-outline-primary" type="button" data-propose="${id}">Invia proposta</button><button class="btn btn-sm btn-primary" type="button" data-propose-nearest="${id}">Proponi al più vicino</button></div>` : ""}
    </article>`;
  }

  function statusLabel(status) {
    return ({
      nuova: "Nuova", proposta: "Proposta", accettata: "Accettata", eseguita: "Eseguita",
      non_accettata: "Rifiutata", scaduta: "Scaduta", chiusa: "Chiusa",
      verifica_polizza: "Verifica polizza", gestione_manuale: "Gestione manuale", annullata: "Annullata"
    })[status] || status;
  }

  function interventionFormMarkup(mission) {
    const id = safe(mission.id);
    const workshops = state.data.officine.map((item) =>
      `<option value="${safe(item.id)}"${item.id === mission.officinaId ? " selected" : ""}>${safe(item.nome)} · ${safe(item.citta)}</option>`
    ).join("");
    return `<section class="mission-cost"><form class="cost-form" data-intervention-form="${id}"><label class="visually-hidden" for="intervention-${id}">Tipo intervento</label><select class="form-select form-select-sm w-auto" id="intervention-${id}" name="tipoIntervento"><option value="traino"${mission.tipoIntervento === "traino" ? " selected" : ""}>Traino</option><option value="riparazione_sul_posto"${mission.tipoIntervento === "traino" ? "" : " selected"}>Riparazione sul posto</option></select><label class="visually-hidden" for="workshop-${id}">Officina</label><select class="form-select form-select-sm w-auto" id="workshop-${id}" name="officinaId"><option value="">Nessuna officina</option>${workshops}</select><button class="btn btn-sm btn-outline-primary" type="submit">Aggiorna intervento</button></form></section>`;
  }

  function proposalHistoryMarkup(proposals) {
    if (!proposals.length) return '<p class="small text-secondary mb-0">Nessuna proposta inviata.</p>';
    return proposals.map((proposal) =>
      `<article class="note-item"><div><strong>${safe(proposal.autistaNome || `Autista #${proposal.autistaId}`)}</strong><time>${safe(formatDateTime(proposal.propostaIl))}</time></div><p>${safe(statusLabel(proposal.stato))}${proposal.rispostaIl ? ` il ${safe(formatDateTime(proposal.rispostaIl))}` : ""}</p></article>`
    ).join("");
  }

  /** Evita di riscrivere il DOM (e perdere il focus) quando l'aggiornamento periodico non cambia nulla. */
  function setHtml(element, html) {
    if (element.dataset.rendered === html) return;
    element.innerHTML = html;
    element.dataset.rendered = html;
  }

  function startDashboardPolling() {
    stopDashboardPolling();
    if (document.hidden) return;
    void loadDashboard();
    state.dashboardTimer = window.setInterval(() => void loadDashboard(), DASHBOARD_POLL_MS);
  }

  function stopDashboardPolling() {
    if (state.dashboardTimer) window.clearInterval(state.dashboardTimer);
    state.dashboardTimer = null;
  }

  async function loadDashboard() {
    try {
      const dashboard = await request("/api/v1/dashboard?mode=live");
      if (state.dashboardFailed) $("#dashboardAlert").classList.add("d-none");
      state.dashboardFailed = false;
      renderDashboard(dashboard);
    } catch (error) {
      if (error.status === 401) {
        stopDashboardPolling();
        return;
      }
      state.dashboardFailed = true;
      showInlineAlert("#dashboardAlert", error.message);
    }
  }

  function renderDashboard(dashboard) {
    const time = new Intl.DateTimeFormat("it-IT", { timeStyle: "medium" }).format(new Date());
    $("#dashboardUpdated").textContent = `Ultimo aggiornamento: ${time}.`;
    renderCriticalQueue(dashboard);
    renderCategoryTiles(dashboard.categorie);
    const distribution = dashboard.distribuzioneStati;
    $("#statusChartPanel").classList.toggle("d-none", !distribution);
    if (distribution) renderStatusChart(distribution);
  }

  function missionAgeText(value) {
    const minutes = Math.floor((Date.now() - new Date(value).getTime()) / 60000);
    if (!Number.isFinite(minutes)) return "";
    if (minutes < 1) return "adesso";
    if (minutes < 60) return `da ${minutes} min`;
    if (minutes < 1440) return `da ${Math.floor(minutes / 60)} h`;
    return `da ${Math.floor(minutes / 1440)} g`;
  }

  function telLink(number) {
    if (!number) return "";
    return `<a href="tel:${safe(String(number).replace(/[^+0-9]/g, ""))}">${safe(number)}</a>`;
  }

  function renderCriticalQueue(dashboard) {
    const queue = dashboard.codaCritica;
    const categories = dashboard.categorie;
    const panel = $("#criticalPanel");
    const total = categories?.critiche ?? queue?.length ?? 0;
    panel.classList.toggle("is-clear", Array.isArray(queue) && queue.length === 0);
    $("#criticalCount").textContent = total;
    $("#criticalCount").classList.toggle("d-none", !total);
    setHtml($("#criticalChips"), categories
      ? ["verificaPolizza", "rifiutateTreAutisti", "supportoAperto"].map((key) =>
        `<button class="critical-chip" type="button" data-filter-category="${key}">${safe(CATEGORIES[key].label)} <strong>${safe(categories[key] ?? 0)}</strong></button>`
      ).join("")
      : "");
    if (!Array.isArray(queue)) {
      setHtml($("#criticalQueue"), '<p class="small text-secondary mb-0">Coda critica non disponibile: il server non fornisce ancora questi dati.</p>');
      return;
    }
    if (!queue.length) {
      setHtml($("#criticalQueue"), '<p class="critical-clear mb-0">✓ Nessuna missione critica in questo momento.</p>');
      return;
    }
    const more = total > queue.length
      ? `<button class="btn btn-link btn-sm px-0" type="button" data-filter-category="critiche">Mostrate le prime ${queue.length} di ${safe(total)}: vedi tutte</button>`
      : "";
    setHtml($("#criticalQueue"), queue.map(criticalRowMarkup).join("") + more);
  }

  function criticalRowMarkup(item) {
    const contacts = [
      item.clienteNome ? safe(item.clienteNome) : '<span class="data-unavailable">Cliente da identificare</span>',
      item.targa ? `Targa <strong>${safe(item.targa)}</strong>` : "",
      item.telefonoSulPosto ? `Sul posto ${telLink(item.telefonoSulPosto)}` : ""
    ].filter(Boolean).join(" · ");
    return `<article class="critical-row motive-${safe(item.motivo)}">
      <div class="critical-row-main">
        <div class="critical-row-title"><span class="motive-badge">${safe(CRITICAL_MOTIVES[item.motivo] || item.motivo)}</span><span class="mission-code">${safe(item.codice)}</span><span class="critical-age">${safe(missionAgeText(item.creataIl))}</span></div>
        <p class="critical-place"><span class="field-label">Luogo di fermo</span> ${safe(item.origineIndirizzo)}</p>
        <p class="critical-contacts">${contacts}</p>
      </div>
      <div class="critical-row-actions">${criticalActionsMarkup(item)}</div>
    </article>`;
  }

  function criticalActionsMarkup(item) {
    const id = safe(item.id);
    const open = `<button class="btn btn-sm btn-outline-secondary" type="button" data-open-mission="${id}">Apri missione</button>`;
    const propose = item.motivo === "TRE_RIFIUTI"
      ? `<button class="btn btn-sm btn-primary" type="button" data-propose-nearest="${id}">Proponi al più vicino</button>`
      : "";
    return `${propose}${open}`;
  }

  function renderCategoryTiles(categories) {
    if (!categories) {
      setHtml($("#categoryTiles"), '<p class="small text-secondary mb-0">Riepilogo per categoria non disponibile: il server non fornisce ancora questi dati.</p>');
      return;
    }
    setHtml($("#categoryTiles"), CATEGORY_TILES.map((key) => {
      const value = categories[key] ?? 0;
      const alert = key === "critiche" && value > 0 ? " is-alert" : "";
      return `<button class="category-tile tile-${key}${alert}" type="button" data-filter-category="${key}"><span class="category-label">${safe(CATEGORIES[key].label)}</span><span class="category-value">${safe(value)}</span></button>`;
    }).join(""));
  }

  function renderStatusChart(distribution) {
    const known = [...STATUS_ORDER, ...Object.keys(distribution).filter((key) => !STATUS_ORDER.includes(key))];
    const rows = known.map((key) => ({
      key, label: statusLabel(key), value: Number(distribution[key] || 0), critical: CRITICAL_STATES.includes(key)
    }));
    setHtml($("#statusChart"), columnChartMarkup(rows, {
      format: String, valueLabel: "Missioni", nameLabel: "Stato", filterable: true
    }));
  }

  /** Istogramma in HTML/CSS: una sola scala, un solo colore (rosso solo per gli stati critici, già nominati in etichetta). */
  function columnChartMarkup(rows, { format, valueLabel, nameLabel, filterable = false }) {
    const max = Math.max(...rows.map((row) => row.value), 0);
    const columns = rows.map((row) => {
      const share = max > 0 ? (row.value / max) * 100 : 0;
      const text = safe(format(row.value));
      const tag = filterable ? "button" : "div";
      const attributes = filterable ? ` type="button" data-filter-status="${safe(row.key)}"` : "";
      return `<${tag} class="chart-column${row.critical ? " is-critical" : ""}" title="${safe(row.label)}: ${text}"${attributes}><span class="chart-value">${text}</span><span class="chart-track"><span class="chart-fill" style="--share:${share.toFixed(1)}%"></span></span><span class="chart-label">${safe(row.label)}</span></${tag}>`;
    }).join("");
    return `<div class="column-chart" style="--columns:${rows.length}">${columns}</div>${chartTableMarkup(rows, format, nameLabel, valueLabel)}`;
  }

  function barChartMarkup(rows, { format, valueLabel, nameLabel }) {
    const max = Math.max(...rows.map((row) => row.value), 0);
    const bars = rows.map((row) => {
      const share = max > 0 ? (row.value / max) * 100 : 0;
      const text = safe(format(row.value));
      return `<div class="chart-bar" title="${safe(row.label)}: ${text}"><span class="chart-bar-label">${safe(row.label)}${row.note ? ` <small>${safe(row.note)}</small>` : ""}</span><span class="chart-track"><span class="chart-fill" style="--share:${share.toFixed(1)}%"></span></span><span class="chart-value">${text}</span></div>`;
    }).join("");
    return `<div class="bar-chart">${bars}</div>${chartTableMarkup(rows, format, nameLabel, valueLabel)}`;
  }

  function chartTableMarkup(rows, format, nameLabel, valueLabel) {
    const body = rows.map((row) => `<tr><td>${safe(row.label)}${row.note ? ` (${safe(row.note)})` : ""}</td><td class="text-end">${safe(format(row.value))}</td></tr>`).join("");
    return `<details class="chart-table"><summary>Vedi i dati in tabella</summary><table class="table table-sm mb-0"><thead><tr><th scope="col">${safe(nameLabel)}</th><th scope="col" class="text-end">${safe(valueLabel)}</th></tr></thead><tbody>${body}</tbody></table></details>`;
  }

  function monthLabel(value) {
    const match = /^(\d{4})-(\d{2})$/.exec(String(value));
    if (!match) return String(value);
    return new Intl.DateTimeFormat("it-IT", { month: "short", year: "numeric" })
      .format(new Date(Number(match[1]), Number(match[2]) - 1, 1));
  }

  function costTileMarkup(label, value, note = "") {
    return `<article class="cost-tile"><span class="stat-label">${safe(label)}</span><p class="stat-value">${safe(formatEuro(value))}</p>${note ? `<p class="small text-secondary mb-0">${safe(note)}</p>` : ""}</article>`;
  }

  async function loadCosts() {
    $("#costAlert").classList.add("d-none");
    try {
      const dashboard = await request(`/api/v1/dashboard?mode=${encodeURIComponent($("#costMode").value)}`);
      renderCosts(dashboard);
    } catch (error) {
      showInlineAlert("#costAlert", error.message);
    }
  }

  function renderCosts(dashboard) {
    const costs = dashboard.costi;
    $("#costTiles").innerHTML = costs
      ? costTileMarkup("Totale generale", costs.totaleGenerale, "Eseguite e annullate con costo attribuito")
        + costTileMarkup("Missioni eseguite", costs.totaleEseguite)
        + costTileMarkup("Annullate con costo attribuito", costs.totaleAnnullateAddebitate)
      : costTileMarkup("Costo missioni eseguite", dashboard.totaleEur);
    $("#costDriverColumn").classList.toggle("d-none", !costs);
    $("#costMonthColumn").classList.toggle("d-none", !costs);
    if (costs) {
      const drivers = [...(costs.perAutista || [])].sort((a, b) => Number(b.totaleEur) - Number(a.totaleEur));
      $("#costDriverChart").innerHTML = drivers.length
        ? barChartMarkup(drivers.map((driver) => ({
          label: driver.autistaNome || `Autista #${driver.autistaId}`,
          note: `${driver.missioni} ${driver.missioni === 1 ? "missione" : "missioni"}`,
          value: Number(driver.totaleEur || 0)
        })), { format: formatEuro, nameLabel: "Autista", valueLabel: "Totale" })
        : '<p class="small text-secondary mb-0">Nessun costo registrato.</p>';
      const months = [...(costs.perMese || [])].sort((a, b) => String(a.mese).localeCompare(String(b.mese)));
      $("#costMonthChart").innerHTML = months.length
        ? columnChartMarkup(months.map((month) => ({
          key: month.mese, label: monthLabel(month.mese), value: Number(month.totaleEur || 0)
        })), { format: formatEuro, nameLabel: "Mese", valueLabel: "Totale" })
        : '<p class="small text-secondary mb-0">Nessun costo registrato.</p>';
    }
    const totals = dashboard.totalePerAutista || [];
    $("#driverTotals").innerHTML = totals.length
      ? totals.map((driver) =>
        `<div class="driver-total-row"><span>${safe(driver.autistaNome)} <small>${safe(driver.missioniEseguite)} missioni</small></span><strong>${safe(formatEuro(driver.totaleEur))}</strong></div>`
      ).join("")
      : '<span class="small text-secondary">Nessun guadagno registrato.</span>';
  }

  async function loadDriverMissions(showLoading = false) {
    const alert = $("#driverAlert");
    alert.classList.add("d-none");
    if (showLoading) $("#driverMissions").innerHTML = loadingMarkup("Caricamento missioni…");
    try {
      const missions = await request("/api/v1/autista/missioni");
      state.driverSignature = driverSignature(missions);
      const accepted = missions.find((mission) => mission.stato === "accettata");
      state.driverMission = accepted || null;
      state.driverDetails = {};
      await Promise.all(missions.map(async (mission) => {
        try {
          state.driverDetails[mission.id] = await request(`/api/v1/autista/missioni/${mission.id}`);
        } catch (error) {
          console.error("Dettagli missione non disponibili:", error);
        }
      }));
      removeMaps();
      $("#driverMissions").innerHTML = missions.filter((mission) => mission.stato === "proposta").length
        ? missions.filter((mission) => mission.stato === "proposta").map(driverProposalCard).join("")
        : '<div class="empty-state"><span class="empty-icon">✓</span><h2 class="h6">Nessuna proposta in attesa</h2><p class="small text-secondary mb-0">Le nuove missioni appariranno qui.</p></div>';
      const detail = $("#activeMissionDetail");
      if (accepted) {
        detail.classList.remove("d-none");
        detail.innerHTML = driverActiveCard(accepted);
        await loadMissionNotes(accepted.id);
      } else {
        detail.classList.add("d-none");
        detail.innerHTML = "";
      }
      updateCountdowns();
      missions.forEach((mission) => void loadDriverRoute(mission));
    } catch (error) {
      showInlineAlert("#driverAlert", error.message);
      $("#driverMissions").innerHTML = "";
    }
  }

  function driverProposalCard(mission) {
    return `<article class="mission-card proposal-card">
      <div class="mission-card-header"><div><span class="mission-code">${safe(mission.codice)}</span><span class="mission-status status-proposta">Proposta</span></div><strong class="countdown" data-deadline="${safe(mission.scadenzaIl)}"></strong></div>
      <div class="mission-card-grid">
        ${driverCustomerMarkup(mission)}
        <div><span class="field-label">Luogo di fermo</span><strong>${safe(mission.origineIndirizzo)}</strong></div>
        <div><span class="field-label">Intervento</span><strong>${safe(mission.tipoIntervento === "traino" ? "Traino" : "Riparazione sul posto")}</strong></div>
        ${driverWorkshopMarkup(mission)}
        ${driverEstimateMarkup(mission)}
      </div>
      <p class="mission-description">${safe(mission.descrizioneProblema)}</p>
      ${missionMapMarkup(mission.id)}
      <div class="mission-actions"><button class="btn btn-success" type="button" data-driver-action="accept" data-mission-id="${safe(mission.id)}">Accetta</button><button class="btn btn-outline-secondary" type="button" data-driver-action="reject" data-mission-id="${safe(mission.id)}">Rifiuta</button></div>
    </article>`;
  }

  function driverActiveCard(mission) {
    return `<article class="mission-card active-mission-card">
      <div class="mission-card-header"><div><span class="mission-code">${safe(mission.codice)}</span><span class="mission-status status-accettata">In corso</span></div><span class="small text-secondary">Accettata ${safe(formatDateTime(mission.accettataIl))}</span></div>
      <div class="mission-card-grid">
        ${driverCustomerMarkup(mission)}
        <div><span class="field-label">Luogo di fermo</span><strong>${safe(mission.origineIndirizzo)}</strong></div>
        ${driverWorkshopMarkup(mission)}
        <div><span class="field-label">Intervento</span><strong>${safe(mission.tipoIntervento === "traino" ? "Traino" : "Riparazione sul posto")}</strong></div>
        ${driverEstimateMarkup(mission)}
      </div>
      <p class="mission-description">${safe(mission.descrizioneProblema)}</p>
      ${missionMapMarkup(mission.id)}
      <section class="mission-notes"><h2 class="h6">Segnala alla centrale</h2><div class="note-list" id="missionNotes"><p class="small text-secondary">Caricamento segnalazioni…</p></div>
        <form id="missionNoteForm" class="note-form"><label class="visually-hidden" for="missionNoteText">Nota per la centrale</label><textarea id="missionNoteText" class="form-control" rows="2" maxlength="2000" required placeholder="Scrivi un aggiornamento sulla missione…"></textarea><button class="btn btn-outline-primary" type="submit">Invia segnalazione</button></form>
      </section>
      <form id="completeMissionForm" class="complete-form"><label class="form-label" for="actualKm">Chilometri percorsi</label><div class="input-group"><input class="form-control" id="actualKm" type="number" min="0" max="99999999" step="0.01" required><span class="input-group-text">km</span><button class="btn btn-primary" type="submit">Chiudi missione · OK</button></div></form>
    </article>`;
  }

  async function loadMissionNotes(missionId, noteList = $("#missionNotes")) {
    const notes = await request(`/api/v1/missioni/${missionId}/notes`);
    if (!noteList) return;
    noteList.innerHTML = notes.length ? notes.map((note) =>
      `<article class="note-item"><div><strong>${safe(note.autoreNome || `Utente #${note.userId}`)}</strong><time>${safe(formatDateTime(note.createdAt))}</time></div><p>${safe(note.testo)}</p></article>`
    ).join("") : '<p class="small text-secondary mb-0">Nessuna segnalazione inviata.</p>';
  }

  async function loadDriverHistory() {
    $("#historyAlert").classList.add("d-none");
    $("#driverHistory").innerHTML = loadingMarkup("Caricamento storico…");
    try {
      const result = await request("/api/v1/autista/storico");
      $("#driverTotal").textContent = formatEuro(result.totaleEur);
      $("#driverHistory").innerHTML = result.missioni.length ? result.missioni.map((mission) =>
        `<article class="mission-card history-card"><div class="mission-card-header"><div><span class="mission-code">${safe(mission.codice)}</span><span class="mission-status status-eseguita">Eseguita</span></div><span class="small text-secondary">${safe(formatDateTime(mission.chiusaIl))}</span></div><div class="mission-card-grid"><div><span class="field-label">Cliente</span><strong>Cliente #${safe(mission.clienteId)}</strong></div><div><span class="field-label">Luogo di fermo</span><strong>${safe(mission.origineIndirizzo)}</strong></div><div><span class="field-label">Km percorsi</span><strong>${safe(mission.kmEffettivi)} km</strong></div><div><span class="field-label">Guadagno</span><strong>${safe(formatEuro(mission.costoFinale))}</strong></div></div></article>`
      ).join("") : '<div class="empty-state"><h2 class="h6">Nessuna missione completata</h2><p class="small text-secondary mb-0">Lo storico sarà aggiornato al termine delle missioni.</p></div>';
    } catch (error) {
      showInlineAlert("#historyAlert", error.message);
      $("#driverHistory").innerHTML = "";
    }
  }

  function updateCountdowns() {
    let expired = false;
    document.querySelectorAll("[data-deadline]").forEach((element) => {
      const milliseconds = new Date(element.dataset.deadline).getTime() - Date.now();
      if (!Number.isFinite(milliseconds)) { element.textContent = ""; return; }
      if (milliseconds <= 0) {
        element.textContent = "Scaduta";
        expired = true;
        return;
      }
      const seconds = Math.floor(milliseconds / 1000);
      element.textContent = `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")} rimanenti`;
    });
    // Throttle across DOM replacement; fixture proposals never expire server-side.
    const liveManager = state.currentView === "missioni" && $("#missionMode").value === "live";
    if (!expired || !state.user || (!liveManager && state.currentView !== "autista")
        || state.deadlineRefreshPending || Date.now() < state.deadlineRefreshAt) return;
    state.deadlineRefreshAt = Date.now() + 30000;
    state.deadlineRefreshPending = true;
    const refresh = state.user.ruolo === "autista" ? loadDriverMissions() : loadMissions();
    Promise.resolve(refresh).finally(() => { state.deadlineRefreshPending = false; });
  }

  $("#loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const button = $("#loginButton");
    const alert = $("#loginAlert");
    alert.classList.add("d-none");
    setBusy(button, true);
    try {
      await loadCsrf();
      const form = new FormData(formElement);
      const user = await request("/api/v1/auth/login", {
        method: "POST",
        mutating: true,
        body: JSON.stringify({
          email: form.get("email"),
          password: form.get("password")
        })
      });
      state.csrf = null;
      if (!["responsabile", "autista"].includes(user.ruolo)) {
        await request("/api/v1/auth/logout", { method: "POST", mutating: true });
        showLogin("Il profilo non è abilitato all'accesso all'area riservata.");
        return;
      }
      state.csrf = null;
      await loadCsrf();
      formElement.reset();
      showApp(user);
    } catch (error) {
      alert.textContent = error.status === 401 ? "Email o password non corretti." : error.message;
      alert.classList.remove("d-none");
    } finally {
      setBusy(button, false);
    }
  });

  $("#logoutButton").addEventListener("click", async () => {
    try {
      await request("/api/v1/auth/logout", { method: "POST", mutating: true });
    } catch (error) {
      if (error.status !== 401) {
        showPageAlert(error.message);
        return;
      }
    }
    showPublic();
  });

  document.querySelectorAll("[data-show-login]").forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      showLogin();
    });
  });
  $("#backToSite").addEventListener("click", (event) => {
    event.preventDefault();
    showPublic();
  });

  document.querySelectorAll("[data-view-link]").forEach((button) => {
    button.addEventListener("click", () => goToView(button.dataset.viewLink));
  });

  $("#registrySearch").addEventListener("input", () => {
    window.clearTimeout(state.searchTimer);
    state.searchTimer = window.setTimeout(() => {
      const query = $("#registrySearch").value.trim();
      if (state.currentView === "clienti") {
        void searchCustomers(query);
      } else {
        renderRegistry(state.currentView, query);
      }
    }, 250);
  });

  async function searchCustomers(query) {
    try {
      const suffix = query ? `?search=${encodeURIComponent(query)}` : "";
      state.customerResults = await request(`/api/v1/clienti${suffix}`);
      state.errors.clienti = null;
      if (state.currentView === "clienti") renderRegistry("clienti", "");
    } catch (error) {
      state.errors.clienti = error;
      renderCounts();
      if (state.currentView === "clienti") renderRegistry("clienti", "");
      $("#registryAlert").textContent = error.message;
      $("#registryAlert").classList.remove("d-none");
    }
  }

  $("#customerForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const button = $("#saveCustomerButton");
    const alert = $("#customerFormAlert");
    alert.classList.add("d-none");
    setBusy(button, true);
    const form = new FormData(formElement);
    const payload = Object.fromEntries(form.entries());
    payload.targa = payload.targa.trim().toUpperCase();
    payload.compagniaId = Number(payload.compagniaId);
    for (const optional of ["cognome", "modelloVeicolo", "cittaResidenza"]) {
      if (!payload[optional]) delete payload[optional];
    }

    try {
      await request("/api/v1/clienti", {
        method: "POST",
        mutating: true,
        body: JSON.stringify(payload)
      });
      formElement.reset();
      bootstrap.Modal.getOrCreateInstance($("#customerModal")).hide();
      $("#registrySearch").value = "";
      await loadRegistries();
      goToView("clienti");
      showPageAlert("Cliente aggiunto correttamente.", "success");
    } catch (error) {
      alert.textContent = error.message;
      alert.classList.remove("d-none");
    } finally {
      setBusy(button, false);
    }
  });

  $("#missionMode").addEventListener("change", () => {
    state.missionFilter = null;
    void loadMissions(true);
  });
  $("#missionStatus").addEventListener("change", () => {
    state.missionFilter = null;
    void loadMissions(true);
  });
  $("#missionFilterClear").addEventListener("click", () => {
    state.missionFilter = null;
    void loadMissions(true);
  });
  $("#costMode").addEventListener("change", () => void loadCosts());
  $("#costRefreshButton").addEventListener("click", () => {
    void loadRegistries();
    void loadCosts();
  });
  $("#missionList").addEventListener("click", async (event) => {
    const mapButton = event.target.closest("[data-mission-map]");
    if (mapButton) {
      const missionId = mapButton.dataset.missionMap;
      const area = mapAreaFor(missionId);
      if (!area) return;
      if (!area.classList.contains("d-none")) {
        area.classList.add("d-none");
        mapButton.setAttribute("aria-expanded", "false");
        mapButton.textContent = "Mostra mappa";
        return;
      }
      area.classList.remove("d-none");
      mapButton.setAttribute("aria-expanded", "true");
      mapButton.textContent = "Nascondi mappa";
      const mission = state.missions.find((item) => String(item.id) === missionId);
      const selected = document.getElementById(`driver-${missionId}`)?.value;
      mapButton.disabled = true;
      await showMissionMap(missionId, selected || mission?.autistaId);
      mapButton.disabled = false;
      return;
    }
    const notesButton = event.target.closest("[data-mission-notes]");
    if (notesButton) {
      const missionId = notesButton.dataset.missionNotes;
      const noteList = document.getElementById(notesButton.getAttribute("aria-controls"));
      if (!noteList) return;
      if (!noteList.classList.contains("d-none")) {
        noteList.classList.add("d-none");
        notesButton.setAttribute("aria-expanded", "false");
        notesButton.textContent = "Mostra avvisi";
        return;
      }
      noteList.classList.remove("d-none");
      noteList.innerHTML = '<p class="small text-secondary mb-0">Caricamento avvisi…</p>';
      notesButton.setAttribute("aria-expanded", "true");
      notesButton.textContent = "Nascondi avvisi";
      notesButton.disabled = true;
      try {
        await loadMissionNotes(missionId, noteList);
      } catch (error) {
        noteList.textContent = error.message;
        showInlineAlert("#missionAlert", error.message);
      } finally {
        notesButton.disabled = false;
      }
      return;
    }
    const proposalsButton = event.target.closest("[data-mission-proposals]");
    if (proposalsButton) {
      const list = document.getElementById(proposalsButton.getAttribute("aria-controls"));
      if (!list) return;
      if (!list.classList.contains("d-none")) {
        list.classList.add("d-none");
        proposalsButton.setAttribute("aria-expanded", "false");
        proposalsButton.textContent = "Mostra storico proposte";
        return;
      }
      list.classList.remove("d-none");
      list.innerHTML = '<p class="small text-secondary mb-0">Caricamento storico…</p>';
      proposalsButton.setAttribute("aria-expanded", "true");
      proposalsButton.textContent = "Nascondi storico proposte";
      try {
        list.innerHTML = proposalHistoryMarkup(await request(`/api/v1/missioni/${proposalsButton.dataset.missionProposals}/proposte`));
      } catch (error) {
        list.textContent = error.message;
      }
      return;
    }
    const button = event.target.closest("[data-propose]");
    if (!button) return;
    const missionId = button.dataset.propose;
    const driverId = $(`#driver-${CSS.escape(missionId)}`).value;
    if (!driverId) {
      showInlineAlert("#missionAlert", "Seleziona un autista prima di inviare la proposta.");
      return;
    }
    button.disabled = true;
    try {
      await request(`/api/v1/missioni/${missionId}/proposte`, {
        method: "POST",
        mutating: true,
        body: JSON.stringify({ autistaId: Number(driverId) })
      });
      await loadMissions();
      showInlineAlert("#missionAlert", "Proposta inviata.", "success");
    } catch (error) {
      showInlineAlert("#missionAlert", error.message);
    } finally {
      button.disabled = false;
    }
  });

  /** Ricarica la vista aperta dopo un'azione, poi mostra l'esito (il caricamento azzera il messaggio). */
  async function refreshView(message = "", type = "success") {
    const views = {
      dashboard: [loadDashboard, "#dashboardAlert"],
      missioni: [loadMissions, "#missionAlert"],
      autista: [loadDriverMissions, "#driverAlert"]
    };
    const [load, alert] = views[state.currentView] || [async () => {}, "#pageAlert"];
    await load();
    if (message) showInlineAlert(alert, message, type);
    if (state.user?.ruolo === "responsabile") void loadAlerts();
  }

  document.addEventListener("click", async (event) => {
    const category = event.target.closest("[data-filter-category]");
    if (category) {
      showFilteredMissions(CATEGORIES[category.dataset.filterCategory]);
      return;
    }
    const statusBar = event.target.closest("[data-filter-status]");
    if (statusBar) {
      const key = statusBar.dataset.filterStatus;
      showFilteredMissions({ label: statusLabel(key), stati: [key] });
      return;
    }
    const open = event.target.closest("[data-open-mission]");
    if (open) {
      void openMissionFromAlert(open.dataset.openMission);
      return;
    }
    const nearest = event.target.closest("[data-propose-nearest]");
    if (nearest) {
      nearest.disabled = true;
      try {
        const proposal = await request(`/api/v1/missioni/${nearest.dataset.proposeNearest}/proposta-automatica`, {
          method: "POST",
          mutating: true
        });
        await refreshView(`Proposta inviata a ${proposal.autistaNome} (base a ${proposal.distanzaKm} km).`);
      } catch (error) {
        await refreshView(({
          422: "Indirizzo non trovato: scegli l'autista a mano.",
          503: "Servizio mappe non disponibile: scegli l'autista a mano."
        })[error.status] || error.message, "danger");
      }
    }
  });

  $("#missionForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const button = $("#saveMissionButton");
    const alert = $("#missionFormAlert");
    alert.classList.add("d-none");
    const type = $("#missionType").value;
    const workshopId = $("#missionWorkshop").value;
    if (type === "traino" && !workshopId) {
      alert.textContent = "Seleziona l'officina per una missione di traino.";
      alert.classList.remove("d-none");
      return;
    }
    setBusy(button, true);
    try {
      await request("/api/v1/missioni", {
        method: "POST",
        mutating: true,
        body: JSON.stringify({
          clienteId: Number($("#missionCustomer").value),
          tipoIntervento: type,
          descrizioneProblema: $("#missionProblem").value.trim(),
          origineIndirizzo: $("#missionOrigin").value.trim(),
          ...(workshopId ? { officinaId: Number(workshopId) } : {}),
          canaleOrigine: $("#missionChannel").value
        })
      });
      formElement.reset();
      bootstrap.Modal.getOrCreateInstance($("#missionModal")).hide();
      await loadMissions();
      showInlineAlert("#missionAlert", "Missione creata.", "success");
    } catch (error) {
      alert.textContent = error.message;
      alert.classList.remove("d-none");
    } finally {
      setBusy(button, false);
    }
  });

  $("#missionType").addEventListener("change", () => {
    const workshop = $("#missionWorkshop");
    workshop.required = $("#missionType").value === "traino";
  });

  $("#driverMissions").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-driver-action]");
    if (!button) return;
    button.disabled = true;
    try {
      await request(`/api/v1/missioni/${button.dataset.missionId}/${button.dataset.driverAction}`, {
        method: "POST",
        mutating: true
      });
      await loadDriverMissions();
    } catch (error) {
      showInlineAlert("#driverAlert", error.message);
      button.disabled = false;
    }
  });

  document.addEventListener("submit", async (event) => {
    if (event.target.matches("[data-intervention-form]")) {
      event.preventDefault();
      const form = new FormData(event.target);
      const type = form.get("tipoIntervento");
      const workshopId = form.get("officinaId");
      if (type === "traino" && !workshopId) {
        showInlineAlert("#missionAlert", "Seleziona l'officina per una missione di traino.");
        return;
      }
      const button = event.target.querySelector("button");
      button.disabled = true;
      try {
        await request(`/api/v1/missioni/${event.target.dataset.interventionForm}/intervento`, {
          method: "PATCH",
          mutating: true,
          body: JSON.stringify({
            tipoIntervento: type,
            ...(type === "traino" ? { officinaId: Number(workshopId) } : {})
          })
        });
        await loadMissions();
        showInlineAlert("#missionAlert", "Intervento aggiornato.", "success");
      } catch (error) {
        showInlineAlert("#missionAlert", error.message);
        button.disabled = false;
      }
    } else if (event.target.matches("[data-cost-form]")) {
      event.preventDefault();
      const form = new FormData(event.target);
      const button = event.target.querySelector("button");
      button.disabled = true;
      try {
        await request(`/api/v1/missioni/${event.target.dataset.costForm}/cost`, {
          method: "PATCH",
          mutating: true,
          body: JSON.stringify({ costoFinale: Number(form.get("costoFinale")), motivo: form.get("motivo").trim() })
        });
        await loadMissions();
        showInlineAlert("#missionAlert", "Costo corretto.", "success");
      } catch (error) {
        showInlineAlert("#missionAlert", error.message);
        button.disabled = false;
      }
    } else if (event.target.id === "missionNoteForm") {
      event.preventDefault();
      const text = $("#missionNoteText").value.trim();
      if (!text || !state.driverMission) return;
      const button = event.target.querySelector("button");
      button.disabled = true;
      try {
        await request(`/api/v1/missioni/${state.driverMission.id}/notes`, {
          method: "POST",
          mutating: true,
          body: JSON.stringify({ testo: text })
        });
        event.target.reset();
        await loadMissionNotes(state.driverMission.id);
      } catch (error) {
        showInlineAlert("#driverAlert", error.message);
      } finally {
        button.disabled = false;
      }
    } else if (event.target.id === "completeMissionForm") {
      event.preventDefault();
      if (!state.driverMission) return;
      const button = event.target.querySelector("button");
      button.disabled = true;
      try {
        await request(`/api/v1/missioni/${state.driverMission.id}/complete`, {
          method: "POST",
          mutating: true,
          body: JSON.stringify({ kmEffettivi: Number($("#actualKm").value) })
        });
        await loadDriverMissions();
        showInlineAlert("#driverAlert", "Missione chiusa correttamente.", "success");
      } catch (error) {
        showInlineAlert("#driverAlert", error.message);
        button.disabled = false;
      }
    }
  });

  $("#alertButton").addEventListener("click", () => {
    const open = $("#alertPanel").classList.toggle("d-none") === false;
    $("#alertButton").setAttribute("aria-expanded", String(open));
    if (open) void loadAlerts();
  });
  $("#alertReadButton").addEventListener("click", markAlertsRead);
  $("#alertList").addEventListener("click", (event) => {
    const button = event.target.closest("[data-alert-mission]");
    if (button) void openMissionFromAlert(button.dataset.alertMission);
  });
  document.addEventListener("click", (event) => {
    if (!event.target.closest("#alertBell")) {
      $("#alertPanel").classList.add("d-none");
      $("#alertButton").setAttribute("aria-expanded", "false");
    }
  });

  $("#driverRefreshButton").addEventListener("click", () => void loadDriverMissions(true));
  $("#refreshButton").addEventListener("click", () => {
    void loadRegistries();
    void loadDashboard();
  });
  // Niente richieste mentre la scheda del browser non è visibile.
  document.addEventListener("visibilitychange", () => {
    if (state.user?.ruolo !== "responsabile" || state.currentView !== "dashboard") return;
    if (document.hidden) stopDashboardPolling();
    else startDashboardPolling();
  });

  $("#menuButton").addEventListener("click", () => {
    const isOpen = $("#sidebar").classList.toggle("sidebar-open");
    $("#menuButton").setAttribute("aria-expanded", String(isOpen));
  });

  state.deadlineTimer = window.setInterval(updateCountdowns, 1000);

  request("/api/v1/auth/me")
    .then((user) => {
      if (!["responsabile", "autista"].includes(user.ruolo)) {
        showPublic();
        return;
      }
      showApp(user);
    })
    .catch((error) => {
      showPublic();
      if (error.status !== 401) {
        console.error("Impossibile verificare la sessione:", error);
      }
    });
})();
