// ============================================================
//  modal.js — Système de modales réutilisable
// ============================================================

const Modal = {
  open(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.add('open');
    document.body.classList.add('modal-open');
    // Focus first focusable element
    setTimeout(() => {
      el.querySelector('input, button:not(.modal-close), select, textarea')?.focus();
    }, 350);
  },

  close(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('open');
    if (!document.querySelector('.modal.open, .modal-fullscreen.open')) {
      document.body.classList.remove('modal-open');
    }
  },

  closeAll() {
    document.querySelectorAll('.modal.open, .modal-fullscreen.open').forEach(el => {
      el.classList.remove('open');
    });
    document.body.classList.remove('modal-open');
  },

  openFullscreen(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.style.display = 'flex';
    requestAnimationFrame(() => el.classList.add('open'));
    document.body.classList.add('modal-open');
  },

  closeFullscreen(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('open');
    setTimeout(() => { el.style.display = 'none'; }, 300);
    if (!document.querySelector('.modal.open, .modal-fullscreen.open')) {
      document.body.classList.remove('modal-open');
    }
  },

  init() {
    // Close on backdrop click
    document.querySelectorAll('.modal').forEach(modal => {
      modal.addEventListener('click', e => {
        if (e.target === modal) this.close(modal.id);
      });
    });
    // Escape key
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape') {
        const open = document.querySelector('.modal.open');
        if (open) this.close(open.id);
        const openFs = document.querySelector('.modal-fullscreen.open');
        if (openFs) this.closeFullscreen(openFs.id);
      }
    });
  }
};
