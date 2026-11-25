// Közös segédfüggvények a bejelentkezés utáni oldalakhoz (index.html, stats.html)

// ---------- Pénznemek és kategóriák (a backend Currency / Category enumjainak megfelelője) ----------

const CURRENCIES = {
    HUF: { symbol: "Ft", decimals: 0 },
    EUR: { symbol: "€", decimals: 2 },
    USD: { symbol: "$", decimals: 2 },
    GBP: { symbol: "£", decimals: 2 }
};

const CATEGORIES = {
    ETEL: { label: "Étel", icon: "🍕" },
    UTAZAS: { label: "Utazás", icon: "✈️" },
    LAKAS: { label: "Lakás", icon: "🏠" },
    SZORAKOZAS: { label: "Szórakozás", icon: "🎉" },
    EGYEB: { label: "Egyéb", icon: "📦" }
};

function getCategory(code) {
    return CATEGORIES[code] || CATEGORIES.EGYEB;
}

// ---------- Formázás ----------

// Felhasználói adatok biztonságos beillesztése innerHTML-be (XSS ellen)
function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

// Összeg magyar formátumban a pénznem jelével, pl. "12 000 Ft", "36,5 €".
// A szám és a jel között nem törhető szóköz van, így a "Ft" sosem kerül külön sorba.
function formatMoney(value, currency = "HUF") {
    const info = CURRENCIES[currency] || CURRENCIES.HUF;
    const number = Number(value).toLocaleString("hu-HU", {
        minimumFractionDigits: 0,
        maximumFractionDigits: info.decimals
    });
    return `${number} ${info.symbol}`;
}

// Rövidített összeg diagram-tengelyekhez, pl. "1,6 M Ft", "250 E Ft"
function formatMoneyCompact(value, currency = "HUF") {
    const info = CURRENCIES[currency] || CURRENCIES.HUF;
    const number = Number(value).toLocaleString("hu-HU", { notation: "compact", maximumFractionDigits: 1 });
    return `${number} ${info.symbol}`;
}

// Több pénznemű összeg felsorolva, pl. { HUF: 2500, EUR: 36 } -> "2 500 Ft és 36 €"
function formatMoneyMap(amountsByCurrency) {
    const parts = Object.keys(CURRENCIES)
        .filter(c => amountsByCurrency[c])
        .map(c => formatMoney(amountsByCurrency[c], c));
    if (parts.length <= 1) return parts[0] || formatMoney(0);
    return parts.slice(0, -1).join(", ") + " és " + parts[parts.length - 1];
}

// Dátum rövid, emberi formában: "Ma, 14:30", "Tegnap, 9:05", "nov. 3. 14:30"
function formatDate(isoString) {
    if (!isoString) return "";
    const date = new Date(isoString);
    if (isNaN(date)) return "";

    const time = date.toLocaleTimeString("hu-HU", { hour: "2-digit", minute: "2-digit" });
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    const sameDay = (a, b) => a.toDateString() === b.toDateString();

    if (sameDay(date, today)) return `Ma, ${time}`;
    if (sameDay(date, yesterday)) return `Tegnap, ${time}`;
    const options = date.getFullYear() === today.getFullYear()
        ? { month: "short", day: "numeric" }
        : { year: "numeric", month: "short", day: "numeric" };
    return `${date.toLocaleDateString("hu-HU", options)} ${time}`;
}

// Részeshatározó rag (-nak/-nek) magánhangzó-harmóniával, pl. Dávidnak, Levinek
function toDative(name) {
    if (name.endsWith("a")) return name.slice(0, -1) + "ának";
    if (name.endsWith("e")) return name.slice(0, -1) + "ének";
    const hasBackVowel = /[aáoóuú]/i.test(name);
    return name + (hasBackVowel ? "nak" : "nek");
}

// ---------- Avatar-színek ----------

const AVATAR_COLORS = [
    { bg: "bg-amber-100", text: "text-amber-600", hex: "#f59e0b" },
    { bg: "bg-rose-100", text: "text-rose-600", hex: "#f43f5e" },
    { bg: "bg-emerald-100", text: "text-emerald-600", hex: "#10b981" },
    { bg: "bg-indigo-100", text: "text-indigo-600", hex: "#6366f1" },
    { bg: "bg-sky-100", text: "text-sky-600", hex: "#0ea5e9" },
    { bg: "bg-violet-100", text: "text-violet-600", hex: "#8b5cf6" },
    { bg: "bg-teal-100", text: "text-teal-600", hex: "#14b8a6" },
    { bg: "bg-orange-100", text: "text-orange-600", hex: "#f97316" }
];

// Szín a felhasználó listabeli helye alapján, így a csapattagok színe biztosan eltér;
// ismeretlen névnél a név karaktereiből számolt szín
function getAvatarColor(name, users) {
    let index = users.indexOf(name);
    if (index < 0) {
        index = [...String(name)].reduce((sum, ch) => sum + ch.codePointAt(0), 0);
    }
    return AVATAR_COLORS[index % AVATAR_COLORS.length];
}

// ---------- Értesítések (toast) ----------

// durationMs: meddig látszik (alapból sikernél 3, egyébként 6 másodperc)
function showToast(message, type = "success", durationMs = null) {
    let container = document.getElementById("toastContainer");
    if (!container) {
        container = document.createElement("div");
        container.id = "toastContainer";
        container.className = "fixed bottom-4 right-4 left-4 sm:left-auto z-[70] flex flex-col items-end gap-2 pointer-events-none";
        container.setAttribute("aria-live", "polite");
        document.body.appendChild(container);
    }

    const styles = {
        success: { box: "border-green-100", icon: "text-green-700", path: '<polyline points="20 6 9 17 4 12"/>' },
        error: { box: "border-rose-100", icon: "text-rose-600", path: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>' },
        info: { box: "border-indigo-100", icon: "text-indigo-600", path: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>' }
    };
    const style = styles[type] || styles.success;

    const toast = document.createElement("div");
    toast.className = `toast toast-hidden pointer-events-auto bg-white ${style.box} border shadow-lg rounded-xl px-4 py-3 flex items-center gap-3 text-sm font-medium text-slate-700 max-w-sm`;
    toast.setAttribute("role", type === "error" ? "alert" : "status");
    toast.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 flex-shrink-0 ${style.icon}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">${style.path}</svg>
        <span>${escapeHtml(message)}</span>
    `;
    container.appendChild(toast);

    requestAnimationFrame(() => toast.classList.remove("toast-hidden"));
    setTimeout(() => {
        toast.classList.add("toast-hidden");
        setTimeout(() => toast.remove(), 300);
    }, durationMs ?? (type === "success" ? 3000 : 6000));
}

// ---------- Megerősítő ablak ----------

// Saját megerősítő ablak a böngésző confirm() helyett; Promise-t ad vissza:
// jóváhagyáskor true (vagy choices esetén a kiválasztott érték), elvetéskor false.
// requireText: a megadott szót be kell gépelni (pl. TÖRLÉS) a jóváhagyáshoz.
// choices: [{ value, label }] választógombok, az első alapból kijelölve.
let confirmResolver = null;

function ensureConfirmModal() {
    if (document.getElementById("confirmModal")) return;
    document.body.insertAdjacentHTML("beforeend", `
        <div id="confirmModal" class="fixed inset-0 z-[60] hidden bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="confirmTitle">
            <div class="w-full max-w-sm bg-white rounded-2xl shadow-2xl border border-slate-100 p-6 flex flex-col gap-4">
                <h3 id="confirmTitle" class="text-lg font-bold text-slate-800"></h3>
                <p id="confirmMessage" class="text-sm text-slate-600"></p>
                <div id="confirmChoices" class="hidden flex-col gap-2" role="radiogroup" aria-labelledby="confirmTitle"></div>
                <div id="confirmTextField" class="hidden flex-col gap-1.5">
                    <label for="confirmTextInput" id="confirmTextLabel" class="text-xs font-medium text-slate-600"></label>
                    <input type="text" id="confirmTextInput" autocomplete="off" class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
                </div>
                <div class="flex justify-end gap-3 mt-2">
                    <button type="button" id="confirmCancelBtn" class="px-4 py-2 text-sm font-semibold text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors">Mégse</button>
                    <button type="button" id="confirmOkBtn" class="px-4 py-2 text-sm font-semibold text-white rounded-xl shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"></button>
                </div>
            </div>
        </div>
    `);
    document.getElementById("confirmOkBtn").addEventListener("click", () => closeConfirm(true));
    document.getElementById("confirmCancelBtn").addEventListener("click", () => closeConfirm(false));
    document.getElementById("confirmModal").addEventListener("click", (e) => {
        if (e.target.id === "confirmModal") closeConfirm(false);
    });
    document.getElementById("confirmTextInput").addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !document.getElementById("confirmOkBtn").disabled) closeConfirm(true);
    });
    // ESC az ablakot zárja be (és nem jut el az oldal többi ESC-kezelőjéhez)
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && isConfirmOpen()) {
            e.stopImmediatePropagation();
            closeConfirm(false);
        }
    }, true);
}

function showConfirm({ title, message, confirmText, danger = false, requireText = null, choices = null }) {
    ensureConfirmModal();
    document.getElementById("confirmTitle").textContent = title;
    document.getElementById("confirmMessage").textContent = message;

    const choicesBox = document.getElementById("confirmChoices");
    choicesBox.classList.toggle("hidden", !choices);
    choicesBox.classList.toggle("flex", !!choices);
    choicesBox.innerHTML = (choices || []).map((choice, i) => `
        <label class="flex items-center gap-3 border border-slate-200 rounded-xl px-3 py-2 cursor-pointer hover:bg-slate-50 transition-colors">
            <input type="radio" name="confirmChoice" value="${escapeHtml(choice.value)}" ${i === 0 ? "checked" : ""} class="accent-indigo-600">
            <span class="text-sm font-medium text-slate-700">${escapeHtml(choice.label)}</span>
        </label>
    `).join('');

    const okBtn = document.getElementById("confirmOkBtn");
    okBtn.textContent = confirmText;
    okBtn.className = `px-4 py-2 text-sm font-semibold text-white rounded-xl shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${danger ? "bg-rose-600 hover:bg-rose-700" : "bg-primary hover:bg-primaryHover"}`;

    const textField = document.getElementById("confirmTextField");
    const textInput = document.getElementById("confirmTextInput");
    textInput.value = "";
    textField.classList.toggle("hidden", !requireText);
    textField.classList.toggle("flex", !!requireText);
    document.getElementById("confirmTextLabel").textContent = requireText ? `A megerősítéshez írd be: ${requireText}` : "";
    textInput.oninput = () => { okBtn.disabled = textInput.value.trim() !== requireText; };
    okBtn.disabled = !!requireText;

    document.getElementById("confirmModal").classList.remove("hidden");
    (requireText ? textInput : okBtn).focus();
    return new Promise(resolve => { confirmResolver = resolve; });
}

function closeConfirm(result) {
    // Választógomboknál jóváhagyáskor a kiválasztott értéket adjuk vissza
    const checked = document.querySelector('#confirmChoices input[name="confirmChoice"]:checked');
    if (result && checked) result = checked.value;

    document.getElementById("confirmModal").classList.add("hidden");
    if (confirmResolver) {
        confirmResolver(result);
        confirmResolver = null;
    }
}

function isConfirmOpen() {
    const modal = document.getElementById("confirmModal");
    return !!modal && !modal.classList.contains("hidden");
}

// Szöveg vágólapra másolása (régebbi böngészőkben is)
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch (e) {
        const area = document.createElement("textarea");
        area.value = text;
        area.style.position = "fixed";
        area.style.opacity = "0";
        document.body.appendChild(area);
        area.select();
        const ok = document.execCommand("copy");
        area.remove();
        return ok;
    }
}

// ---------- Bejelentkezés és munkamenet ----------

// A bejelentkező oldalra visz; belépés után ugyanide (pl. a meghívó linkre) tér vissza
function redirectToLogin(expired = false) {
    const params = new URLSearchParams();
    if (expired) params.set("lejart", "1");
    const here = window.location.pathname + window.location.search;
    if (here !== "/" && !here.startsWith("/login.html")) params.set("vissza", here);
    const query = params.toString();
    window.location.href = "/login.html" + (query ? `?${query}` : "");
}

// API hívás: 401 (nincs belépve / lejárt a munkamenet) esetén a bejelentkező oldalra visz
async function apiFetch(url, options = {}) {
    const res = await fetch(url, options);
    if (res.status === 401) {
        redirectToLogin(true);
        throw new Error("Nincs bejelentkezve");
    }
    return res;
}

async function apiJson(url, method, body) {
    return apiFetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
}

// ---------- Közös fejléc ----------

const LOGO_SVG = `<svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 flex-shrink-0 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>`;

// A fejléc minden bejelentkezés utáni oldalon ugyanaz. activeTab: "expenses" / "history" / "stats",
// vagy null a csoportválasztó oldalon (ott nincs csoport-link és fülek).
// Mobilon a fülek külön sorba kerülnek, a kijelentkezés csak ikon. Az oldal szkriptjének elején kell
// meghívni (a DOMContentLoaded előtt), hogy a téma-csúszkát a theme.js megtalálja.
function renderHeader(activeTab = null) {
    const tabs = [
        ["expenses", "/", "Kiadások"],
        ["history", "/history.html", "Rendezések"],
        ["stats", "/stats.html", "Statisztika"]
    ];

    const brand = activeTab
        ? `<a href="/groups.html" class="flex items-center gap-2 min-w-0" title="Vissza a csoportokhoz">
               ${LOGO_SVG}
               <span class="min-w-0 leading-tight">
                   <span class="block text-xs font-medium text-slate-500">← Csoportok</span>
                   <span id="currentGroupName" class="block text-sm font-bold text-slate-900 truncate max-w-[11rem] lg:max-w-[14rem]"></span>
               </span>
           </a>`
        : `<a href="/groups.html" class="flex items-center gap-2">
               ${LOGO_SVG}
               <span class="text-xl font-bold text-slate-900 tracking-tight">KözösKassza</span>
           </a>`;

    const nav = activeTab
        ? `<nav class="order-3 w-full md:order-none md:w-auto md:mr-auto flex items-center gap-1 text-sm font-medium overflow-x-auto">
               ${tabs.map(([key, href, label]) => key === activeTab
                   ? `<a data-group-link="${href}" href="${href}" aria-current="page" class="px-3 py-1.5 rounded-lg bg-slate-100 text-slate-800 whitespace-nowrap">${label}</a>`
                   : `<a data-group-link="${href}" href="${href}" class="px-3 py-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors whitespace-nowrap">${label}</a>`
               ).join("")}
           </nav>`
        : "";

    const header = document.getElementById("appHeader");
    header.className = "bg-white shadow-sm border-b border-slate-200 sticky top-0 z-10";
    header.innerHTML = `
        <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-2 md:py-0 md:h-16 flex flex-wrap md:flex-nowrap items-center gap-x-4 lg:gap-x-6 gap-y-2">
            ${brand}
            ${nav}
            <div class="order-2 md:order-none ml-auto flex items-center gap-2 sm:gap-3 flex-shrink-0">
                <span class="hidden lg:flex items-center gap-1 text-xs font-medium text-slate-500" title="Automatikus kijelentkeztetésig hátralévő idő">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    <span id="sessionCountdown" class="tabular-nums"></span>
                </span>
                <button type="button" class="theme-switch" data-theme-toggle role="switch" aria-checked="false" aria-label="Sötét mód" title="Sötét mód">
                    <span class="theme-switch-knob">
                        <svg class="theme-icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
                        <svg class="theme-icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
                    </span>
                </button>
                <div id="currentUserAvatar" class="h-8 w-8 flex-shrink-0 rounded-full bg-indigo-100 flex items-center justify-center text-primary font-bold"></div>
                <span id="currentUserName" class="hidden lg:inline-block max-w-[9rem] truncate font-medium text-sm text-slate-600"></span>
                <button type="button" id="logoutBtn" title="Kijelentkezés" aria-label="Kijelentkezés"
                        class="flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg px-2 sm:px-3 py-1.5 transition-colors">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 sm:hidden" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                    <span class="hidden sm:inline">Kijelentkezés</span>
                </button>
            </div>
        </div>
    `;
}

// ---------- Csoportok ----------

// Az aktuális csoport azonosítója az URL-ből (?csoport=12); ha nincs, a csoportválasztó oldalra visz
function requireGroupId() {
    const id = Number(new URLSearchParams(window.location.search).get("csoport"));
    if (!id) {
        window.location.href = "/groups.html";
        return null;
    }
    return id;
}

// Csoporton belüli API útvonal, pl. groupApi(12, "/expenses") -> "/api/groups/12/expenses"
function groupApi(groupId, path = "") {
    return `/api/groups/${groupId}${path}`;
}

// A csoport adatainak betöltése: a fejlécben megjelenik a neve, a navigációs fülek a csoportra mutatnak.
// Ha a csoport nem létezik, vagy nem vagy tagja, visszavisz a csoportválasztóhoz.
async function loadGroup(groupId) {
    const res = await apiFetch(groupApi(groupId));
    if (!res.ok) {
        window.location.href = "/groups.html";
        return null;
    }
    const group = await res.json();
    const nameLabel = document.getElementById("currentGroupName");
    if (nameLabel) nameLabel.textContent = group.name;
    document.title = `${group.name} – KözösKassza`;
    document.querySelectorAll("a[data-group-link]").forEach(a => {
        a.href = `${a.dataset.groupLink}?csoport=${groupId}`;
    });
    return group;
}

// ---------- Megosztás (a backend ShareCalculator-ának megfelelője) ----------

// A teljes összeg felosztása súlyok arányában a pénznem legkisebb egységére kerekítve
// (legnagyobb maradék módszer), így a részek összege pontosan a teljes összeg
function distributeAmount(total, weights, currency) {
    return distributeRounded(total, weights, (CURRENCIES[currency] || CURRENCIES.HUF).decimals);
}

// Ugyanez tetszőleges tizedesjegyre (pl. százalékokhoz 1 tizedes)
function distributeRounded(total, weights, decimals) {
    const scale = Math.pow(10, decimals);
    const totalUnits = Math.round(total * scale);
    const weightSum = weights.reduce((a, b) => a + b, 0);
    const exact = weights.map(w => (weightSum === 0 ? 0 : totalUnits * w / weightSum));
    const units = exact.map(Math.floor);
    let leftover = totalUnits - units.reduce((a, b) => a + b, 0);
    const order = exact.map((e, i) => i).filter(i => weights[i] > 0)
        .sort((a, b) => (exact[b] - units[b]) - (exact[a] - units[a]));
    for (let k = 0; leftover > 0 && order.length; k++, leftover--) units[order[k % order.length]]++;
    return units.map(u => u / scale);
}

// Résztvevőnkénti összegek a megosztási mód szerint: { név: összeg }
function computeShareAmounts(mode, participants, values, total, currency) {
    if (!participants.length || !(total > 0)) return {};
    const weights = mode === "EQUAL" ? participants.map(() => 1) : participants.map(p => Math.max(0, Number(values[p]) || 0));
    const amounts = distributeAmount(total, weights, currency);
    return Object.fromEntries(participants.map((p, i) => [p, amounts[i]]));
}

// A bejelentkezett felhasználó (vagy átirányítás a bejelentkező oldalra, ha nincs belépve).
// Sikeres ellenőrzés után megjeleníti az oldalt és elindítja a munkamenet-időzítőt.
async function loadCurrentUser() {
    const res = await fetch("/api/auth/me");
    if (!res.ok) {
        redirectToLogin();
        return null;
    }
    const user = await res.json();

    const avatar = document.getElementById("currentUserAvatar");
    const nameLabel = document.getElementById("currentUserName");
    const displayName = user.admin ? `${user.name} (admin)` : user.name;
    if (avatar) {
        avatar.textContent = user.name.charAt(0).toUpperCase();
        avatar.title = displayName; // kisebb képernyőn a név nem látszik, az avatar fölé húzva igen
    }
    if (nameLabel) nameLabel.textContent = displayName;

    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) logoutBtn.addEventListener("click", () => logout());

    startSessionTimer(user.sessionRemainingSeconds);
    document.documentElement.classList.remove("auth-pending");
    return user;
}

async function logout(expired = false) {
    await fetch("/api/auth/logout", { method: "POST" });
    redirectToLogin(expired);
}

// A munkamenet a belépéstől számított 15 percig él: 1 perccel előtte figyelmeztet,
// lejáratkor kijelentkeztet. A fejlécben visszaszámláló mutatja a hátralévő időt.
function startSessionTimer(remainingSeconds) {
    const expiresAt = Date.now() + remainingSeconds * 1000;
    const countdown = document.getElementById("sessionCountdown");
    let warned = false;

    const tick = () => {
        const secondsLeft = Math.max(0, Math.round((expiresAt - Date.now()) / 1000));
        if (countdown) {
            const minutes = Math.floor(secondsLeft / 60);
            const seconds = String(secondsLeft % 60).padStart(2, "0");
            countdown.textContent = `${minutes}:${seconds}`;
            countdown.title = "Ennyi idő múlva automatikusan kijelentkeztetünk";
            countdown.classList.toggle("text-rose-600", secondsLeft <= 60);
        }
        if (!warned && secondsLeft <= 60 && secondsLeft > 0) {
            warned = true;
            showToast("1 perc múlva lejár a munkamenet, utána újra be kell jelentkezned.", "info", 30000);
        }
        if (secondsLeft <= 0) {
            clearInterval(timer);
            logout(true);
        }
    };
    const timer = setInterval(tick, 1000);
    tick();
}
