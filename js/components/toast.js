// ============================================================
//  toast.js — Notifications toast
// ============================================================

const Toast = (() => {
  let timer = null;

  function show(message, type = 'default', duration = 2800) {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = message;
    el.className = `toast-${type}`;
    el.classList.add('show');
    clearTimeout(timer);
    timer = setTimeout(() => el.classList.remove('show'), duration);
  }

  function success(msg) { show(msg, 'success'); }
  function error(msg)   { show(msg, 'error', 3500); }
  function info(msg)    { show(msg, 'info'); }

  return { show, success, error, info };
})();
