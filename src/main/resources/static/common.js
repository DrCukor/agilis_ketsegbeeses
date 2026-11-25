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

// Összeg magyar formátumban a pénznem jelével, pl. "12 000 Ft", "36,5 €"
function formatMoney(value, currency = "HUF") {
    const info = CURRENCIES[currency] || CURRENCIES.HUF;
    const number = Number(value).toLocaleString("hu-HU", {
        minimumFractionDigits: 0,
        maximumFractionDigits: info.decimals
    });
    return `${number} ${info.symbol}`;
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
    if (avatar) avatar.textContent = user.name.charAt(0).toUpperCase();
    if (nameLabel) nameLabel.textContent = user.admin ? `${user.name} (admin)` : user.name;

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
