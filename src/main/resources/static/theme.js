// Világos / sötét téma kezelése (index.html és login.html közös kódja)
// A választást a böngésző megjegyzi; ha még nincs mentett választás, a rendszer beállítását követi.
(function () {
    const STORAGE_KEY = "kozoskassza-theme";

    function getSavedTheme() {
        try {
            return localStorage.getItem(STORAGE_KEY);
        } catch (e) {
            return null;
        }
    }

    function saveTheme(theme) {
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        } catch (e) {
            // Privát módban a mentés elmaradhat, a téma ettől még működik
        }
    }

    function preferredTheme() {
        const saved = getSavedTheme();
        if (saved === "dark" || saved === "light") return saved;
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function applyTheme(theme) {
        const isDark = theme === "dark";
        document.documentElement.classList.toggle("dark", isDark);
        document.querySelectorAll("[data-theme-toggle]").forEach(btn =>
            btn.setAttribute("aria-checked", String(isDark)));
    }

    // Azonnal alkalmazzuk, még a tartalom kirajzolása előtt (ne villanjon fel a világos téma)
    applyTheme(preferredTheme());

    document.addEventListener("DOMContentLoaded", () => {
        applyTheme(preferredTheme());
        document.querySelectorAll("[data-theme-toggle]").forEach(btn =>
            btn.addEventListener("click", () => {
                const next = document.documentElement.classList.contains("dark") ? "light" : "dark";
                saveTheme(next);
                applyTheme(next);
            }));
    });
})();
