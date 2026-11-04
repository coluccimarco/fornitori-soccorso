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
    deadlineTimer: null
  };

  const ALERT_POLL_MS = 15000;
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
    if (payload && typeof payload === "object" && payload.message) return payload.message;
    if (status === 400) return "Controlla i dati inseriti e riprova.";
    if (status === 409) return "L'operazione è in conflitto con lo stato attuale dei dati. Aggiorna e riprova.";
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
    state.user = null;
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
      state.currentView = "dashboard";
      void loadRegistries();
      startAlertPolling();
    } else {
      stopAlertPolling();
      state.currentView = "autista";
    }
    goToView(state.currentView);
  }

  function showPublic() {
    stopAlertPolling();
    state.user = null;
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
    else if (state.currentView !== "dashboard") renderRegistry(state.currentView);
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
      option.textContent = `${item.nome} · ${item.targa}`;
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

  function missionCustomerMarkup(mission, useRegistry = false) {
    const customer = useRegistry
      ? state.data.clienti.find((item) => item.id === mission.clienteId)
      : null;
    const customerId = mission.clienteId ?? "non disponibile";
    const name = customer?.nome || `Cliente #${customerId}`;
    const contact = customer?.telefono
      ? `<a href="tel:${safe(customer.telefono)}">${safe(customer.telefono)}</a>`
      : `<span class="data-unavailable">${useRegistry && state.errors.clienti
        ? "Anagrafica clienti non disponibile"
        : customer
          ? "Recapito non disponibile nell'anagrafica"
          : "Nome e recapito non forniti dall'API missioni"}</span>`;
    return `<div><span class="field-label">Cliente</span><strong>${safe(name)}</strong>${contact}</div>`;
  }

  function missionWorkshopMarkup(mission, useRegistry = false) {
    if (!mission.officinaId) {
      return `<div><span class="field-label">Officina</span><strong>${mission.tipoIntervento === "traino"
        ? "Non indicata"
        : "Non prevista"}</strong></div>`;
    }
    const workshop = useRegistry
      ? state.data.officine.find((item) => item.id === mission.officinaId)
      : null;
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
    return `<div class="mission-map-area${hidden ? " d-none" : ""}" data-map-area="${safe(missionId)}"><p class="small text-secondary mb-2" data-map-status>Caricamento mappa…</p><div class="mission-map d-none" data-map></div><ol class="mission-map-points small d-none" data-map-points></ol></div>`;
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
      const route = await request(`/api/v1/maps/route?${params}`);
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
    const labels = { base: "Base operativa", sinistro: "Dove si trova il cliente", officina: "Officina" };
    pointsList.replaceChildren();
    const coordinates = route.punti.map((point) => {
      const label = `${labels[point.tipo] || point.tipo}: ${point.descrizione}`;
      const popup = document.createElement("span");
      popup.textContent = label;
      const marker = L.marker([point.lat, point.lng], { title: label }).addTo(map).bindPopup(popup);
      if (point.tipo === "sinistro") marker.setZIndexOffset(1000);
      const item = document.createElement("li");
      item.textContent = label;
      pointsList.append(item);
      return [point.lat, point.lng];
    });
    if (route.geometria) {
      const line = L.geoJSON(route.geometria, { style: { color: "#6d28d9", weight: 5 } }).addTo(map);
      map.fitBounds(line.getBounds().extend(coordinates), { padding: [24, 24] });
      status.textContent = `Percorso stimato: ${route.kmStimati} km (fonte: OSRM). I km effettivi si confermano alla chiusura.`;
    } else {
      map.setView(coordinates[0], 15);
      status.textContent = "Scegli un autista per calcolare percorso e chilometri.";
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
    if (input && !input.value && state.driverMission?.id === mission.id && route.kmStimati != null) {
      input.value = route.kmStimati;
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

  async function openMissionFromAlert(missionId) {
    $("#alertPanel").classList.add("d-none");
    $("#alertButton").setAttribute("aria-expanded", "false");
    state.currentView = "missioni";
    $("#missionMode").value = "live";
    $("#missionStatus").value = "";
    goToView("missioni");
    await loadMissions();
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

  function goToView(view) {
    const permitted = state.user?.ruolo === "responsabile"
      ? ["dashboard", "missioni", ...Object.keys(viewConfig)]
      : ["autista", "storico"];
    state.currentView = permitted.includes(view) ? view : (state.user?.ruolo === "responsabile" ? "dashboard" : "autista");
    const pages = {
      dashboard: "#dashboardPage",
      missioni: "#missionPage",
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
    const titles = { dashboard: "Panoramica", missioni: "Missioni", autista: "Le mie missioni", storico: "Storico e guadagni" };
    $("#currentSection").textContent = titles[state.currentView] || viewConfig[state.currentView]?.title || "";
    document.querySelectorAll("[data-view-link]").forEach((button) => {
      button.classList.toggle("active", button.dataset.viewLink === state.currentView);
    });
    if (state.currentView === "missioni") void loadMissions();
    if (state.currentView === "autista") void loadDriverMissions();
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
        <td class="primary-cell">${safe(item.nome)}<div class="secondary-cell">ID ${safe(item.id)}</div></td>
        <td>${safe(item.telefono)}</td>
        <td>${safe(item.targa)}<div class="secondary-cell">${safe(item.modelloVeicolo)}</div></td>
        <td>${safe(item.compagniaNome)}</td>
        <td>${safe(item.numeroPolizza)}<div class="secondary-cell">Scade ${safe(formatDate(item.scadenzaPolizza))}</div></td>
        <td>${safe(item.cittaResidenza)}</td>
      </tr>`;
    }
    if (view === "autisti") {
      const busy = item.disponibilita === "occupato";
      return `<tr>
        <td class="primary-cell">${safe(item.nome)}<div class="secondary-cell">ID ${safe(item.id)}</div></td>
        <td>${safe(item.telefono)}</td><td>${safe(item.zona)}</td>
        <td>${safe(vehiclePlate(item.mezzoId))}</td>
        <td><span class="availability${busy ? " busy" : ""}">${busy ? "Occupato" : "Disponibile"}</span></td>
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

  async function loadMissions() {
    const alert = $("#missionAlert");
    alert.classList.add("d-none");
    const mode = $("#missionMode").value;
    const status = $("#missionStatus").value;
    const params = new URLSearchParams({ mode });
    if (status) params.set("stato", status);
    try {
      const [missions, dashboard] = await Promise.all([
        request(`/api/v1/missioni?${params}`),
        request(`/api/v1/dashboard?mode=${encodeURIComponent(mode)}`)
      ]);
      state.missions = missions;
      $("#missionPendingCount").textContent = dashboard.pending;
      $("#missionDoneCount").textContent = dashboard.eseguite;
      $("#missionExpiredCount").textContent = dashboard.scadute;
      $("#missionRevenue").textContent = formatEuro(dashboard.totaleEur);
      $("#driverTotals").innerHTML = dashboard.totalePerAutista.length
        ? dashboard.totalePerAutista.map((driver) =>
          `<div class="driver-total-row"><span>${safe(driver.autistaNome)} <small>${safe(driver.missioniEseguite)} missioni</small></span><strong>${safe(formatEuro(driver.totaleEur))}</strong></div>`
        ).join("")
        : '<span class="small text-secondary">Nessun guadagno registrato.</span>';
      $("#missionCount").textContent = `${missions.length} ${missions.length === 1 ? "missione" : "missioni"}`;
      renderMissionList();
    } catch (error) {
      showInlineAlert("#missionAlert", error.message);
      $("#missionList").innerHTML = "";
      $("#missionCount").textContent = "Dati non disponibili";
    }
  }

  function renderMissionList() {
    const list = $("#missionList");
    if (!state.missions.length) {
      list.innerHTML = '<div class="empty-state"><span class="empty-icon">↗</span><h2 class="h6">Nessuna missione</h2><p class="small text-secondary mb-0">Crea una missione o modifica i filtri.</p></div>';
      return;
    }
    removeMaps();
    list.innerHTML = state.missions.map((mission) => {
      const canPropose = $("#missionMode").value === "live"
        && ["nuova", "non_accettata", "scaduta"].includes(mission.stato);
      const driverOptions = state.data.autisti.map((driver) =>
        `<option value="${safe(driver.id)}"${driver.disponibilita === "occupato" ? " disabled" : ""}>${safe(driver.nome)}${driver.disponibilita === "occupato" ? " · occupato" : ""}</option>`
      ).join("");
      return `<article class="mission-card" data-mission-card="${safe(mission.id)}">
        <div class="mission-card-header">
          <div><span class="mission-code">${safe(mission.codice)}</span><span class="mission-status status-${safe(mission.stato)}">${safe(statusLabel(mission.stato))}</span></div>
          <span class="small text-secondary">${safe(formatDateTime(mission.chiusaIl || mission.scadenzaIl))}</span>
        </div>
        <div class="mission-card-grid">
          ${missionCustomerMarkup(mission, true)}
          <div><span class="field-label">Luogo</span><strong>${safe(mission.origineIndirizzo)}</strong></div>
          <div><span class="field-label">Autista</span><strong>${safe(state.data.autisti.find((driver) => driver.id === mission.autistaId)?.nome || (mission.autistaId ? `Autista #${mission.autistaId}` : "Non assegnato"))}</strong>${mission.stato === "proposta" ? `<span class="countdown" data-deadline="${safe(mission.scadenzaIl)}"></span>` : ""}</div>
          ${missionWorkshopMarkup(mission, true)}
          <div><span class="field-label">Esito / importo</span><strong>${safe(mission.kmEffettivi != null ? `${mission.kmEffettivi} km · ${mission.costoFinale != null ? formatEuro(mission.costoFinale) : "Importo non disponibile"}` : mission.tipoIntervento === "traino" ? "Traino" : "Riparazione sul posto")}</strong></div>
        </div>
        <p class="mission-description">${safe(mission.descrizioneProblema)}</p>
        <section class="mission-notes">
          <button class="btn btn-link btn-sm px-0" type="button" data-mission-notes="${safe(mission.id)}" aria-expanded="false" aria-controls="mission-notes-${safe(mission.id)}">Mostra segnalazioni</button>
          <div class="note-list d-none" id="mission-notes-${safe(mission.id)}"><p class="small text-secondary mb-0">Apri per caricare le segnalazioni.</p></div>
        </section>
        <section class="mission-notes">
          <button class="btn btn-link btn-sm px-0" type="button" data-mission-map="${safe(mission.id)}" aria-expanded="false">Mostra mappa</button>
          ${missionMapMarkup(mission.id, true)}
        </section>
        ${costCorrectionMarkup(mission)}
        ${canPropose ? `<div class="mission-propose"><label class="visually-hidden" for="driver-${safe(mission.id)}">Seleziona autista</label><select class="form-select form-select-sm" id="driver-${safe(mission.id)}"><option value="">Seleziona autista</option>${driverOptions}</select><button class="btn btn-sm btn-outline-primary" type="button" data-propose="${safe(mission.id)}">Invia proposta</button><button class="btn btn-sm btn-primary" type="button" data-propose-nearest="${safe(mission.id)}">Proponi al più vicino</button></div>` : ""}
      </article>`;
    }).join("");
    updateCountdowns();
  }

  function statusLabel(status) {
    return ({
      nuova: "Nuova", proposta: "Proposta", accettata: "Accettata", eseguita: "Eseguita",
      non_accettata: "Rifiutata", scaduta: "Scaduta"
    })[status] || status;
  }

  async function loadDriverMissions() {
    const alert = $("#driverAlert");
    alert.classList.add("d-none");
    try {
      const missions = await request("/api/v1/autista/missioni");
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
        <div><span class="field-label">Luogo</span><strong>${safe(mission.origineIndirizzo)}</strong></div>
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
        <div><span class="field-label">Luogo intervento</span><strong>${safe(mission.origineIndirizzo)}</strong></div>
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
    try {
      const result = await request("/api/v1/autista/storico");
      $("#driverTotal").textContent = formatEuro(result.totaleEur);
      $("#driverHistory").innerHTML = result.missioni.length ? result.missioni.map((mission) =>
        `<article class="mission-card history-card"><div class="mission-card-header"><div><span class="mission-code">${safe(mission.codice)}</span><span class="mission-status status-eseguita">Eseguita</span></div><span class="small text-secondary">${safe(formatDateTime(mission.chiusaIl))}</span></div><div class="mission-card-grid"><div><span class="field-label">Cliente</span><strong>Cliente #${safe(mission.clienteId)}</strong></div><div><span class="field-label">Luogo</span><strong>${safe(mission.origineIndirizzo)}</strong></div><div><span class="field-label">Km percorsi</span><strong>${safe(mission.kmEffettivi)} km</strong></div><div><span class="field-label">Guadagno</span><strong>${safe(formatEuro(mission.costoFinale))}</strong></div></div></article>`
      ).join("") : '<div class="empty-state"><h2 class="h6">Nessuna missione completata</h2><p class="small text-secondary mb-0">Lo storico sarà aggiornato al termine delle missioni.</p></div>';
    } catch (error) {
      showInlineAlert("#historyAlert", error.message);
      $("#driverHistory").innerHTML = "";
    }
  }

  function updateCountdowns() {
    document.querySelectorAll("[data-deadline]").forEach((element) => {
      const milliseconds = new Date(element.dataset.deadline).getTime() - Date.now();
      if (milliseconds <= 0) {
        element.textContent = "Scaduta";
        if (element.dataset.refreshed !== "true") {
          element.dataset.refreshed = "true";
          if (state.user?.ruolo === "autista") void loadDriverMissions();
          else if (state.currentView === "missioni") void loadMissions();
        }
        return;
      }
      const seconds = Math.floor(milliseconds / 1000);
      const minutes = Math.floor(seconds / 60);
      element.textContent = `${String(minutes).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")} rimanenti`;
    });
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
      if (!["responsabile", "autista"].includes(user.ruolo)) {
        await request("/api/v1/auth/logout", { method: "POST", mutating: true });
        showLogin("Il profilo non è abilitato all'accesso all'area riservata.");
        return;
      }
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
    for (const optional of ["modelloVeicolo", "cittaResidenza"]) {
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

  $("#missionMode").addEventListener("change", () => void loadMissions());
  $("#missionStatus").addEventListener("change", () => void loadMissions());
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
        notesButton.textContent = "Mostra segnalazioni";
        return;
      }
      noteList.classList.remove("d-none");
      noteList.innerHTML = '<p class="small text-secondary mb-0">Caricamento segnalazioni…</p>';
      notesButton.setAttribute("aria-expanded", "true");
      notesButton.textContent = "Nascondi segnalazioni";
      notesButton.disabled = true;
      try {
        await loadMissionNotes(missionId, noteList);
        notesButton.setAttribute("aria-expanded", "true");
        notesButton.textContent = "Nascondi segnalazioni";
      } catch (error) {
        noteList.textContent = error.message;
        showInlineAlert("#missionAlert", error.message);
      } finally {
        notesButton.disabled = false;
      }
      return;
    }
    const nearestButton = event.target.closest("[data-propose-nearest]");
    if (nearestButton) {
      nearestButton.disabled = true;
      try {
        const proposal = await request(`/api/v1/missioni/${nearestButton.dataset.proposeNearest}/proposta-automatica`, {
          method: "POST",
          mutating: true
        });
        await loadMissions();
        showInlineAlert("#missionAlert", `Proposta inviata a ${proposal.autistaNome} (base a ${proposal.distanzaKm} km).`, "success");
      } catch (error) {
        showInlineAlert("#missionAlert", ({
          409: "Nessun autista libero che non abbia già risposto a questa missione.",
          422: "Indirizzo non trovato: scegli l'autista a mano.",
          503: "Servizio mappe non disponibile: scegli l'autista a mano."
        })[error.status] || error.message);
        nearestButton.disabled = false;
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
      showInlineAlert("#missionAlert", "Missione creata. Ora puoi inviare una proposta a un autista.", "success");
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
    if (event.target.matches("[data-cost-form]")) {
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

  $("#driverRefreshButton").addEventListener("click", () => void loadDriverMissions());
  $("#refreshButton").addEventListener("click", () => {
    void loadRegistries();
    if (state.currentView === "missioni") void loadMissions();
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
