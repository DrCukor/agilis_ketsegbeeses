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

// ---------- Bejelentkezés és munkamenet ----------

function redirectToLogin(expired = false) {
    window.location.href = expired ? "/login.html?lejart=1" : "/login.html";
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
