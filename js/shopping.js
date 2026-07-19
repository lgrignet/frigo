// ============================================================
//  shopping.js — Vue Liste de courses
// ============================================================

const ShoppingModule = (() => {

  function render() {
    renderHeader();
    renderAutoSection();
    renderManualSection();
    renderRecipeSection();
    updateShoppingBadge();
  }

  function renderHeader() {
    document.getElementById('shopping-view-title').textContent = i18n.t('shopping_title');
    document.getElementById('btn-shopping-copy').title = i18n.t('shopping_copy');
    document.getElementById('btn-shopping-clear-done').title = i18n.t('shopping_clear_done');
    document.getElementById('shopping-input').placeholder = i18n.t('shopping_placeholder');
    document.getElementById('btn-shopping-add').textContent = i18n.t('shopping_add_btn');
  }

  function renderAutoSection() {
    const section = document.getElementById('shopping-auto-section');
    const list = Storage.getShoppingList().filter(i => i.source === 'auto');

    document.getElementById('shopping-auto-title').textContent = i18n.t('shopping_section_auto');
    const container = document.getElementById('shopping-auto-list');

    if (list.length === 0) {
      container.innerHTML = `<div class="shopping-empty-section">${
        i18n.lang === 'fr' ? 'Aucun réassort automatique' : 'No auto restock items'
      }</div>`;
    } else {
      container.innerHTML = list.map(item => renderShoppingItem(item)).join('');
      bindShoppingItemEvents(container);
    }
  }

  function renderManualSection() {
    const list = Storage.getShoppingList().filter(i => i.source === 'manual');
    document.getElementById('shopping-manual-title').textContent = i18n.t('shopping_section_manual');
    const container = document.getElementById('shopping-manual-list');

    if (list.length === 0) {
      container.innerHTML = `<div class="shopping-empty-section">${
        i18n.lang === 'fr' ? 'Aucun article manuel' : 'No manual items'
      }</div>`;
    } else {
      container.innerHTML = list.map(item => renderShoppingItem(item)).join('');
      bindShoppingItemEvents(container);
    }
  }

  function renderRecipeSection() {
    const list = Storage.getShoppingList().filter(i => i.source === 'recipe');
    const section = document.getElementById('shopping-recipe-section');
    document.getElementById('shopping-recipe-title').textContent = i18n.t('shopping_section_recipe');
    const container = document.getElementById('shopping-recipe-list');

    if (list.length === 0) {
      section.style.display = 'none';
      return;
    }
    section.style.display = 'block';
    container.innerHTML = list.map(item => renderShoppingItem(item)).join('');
    bindShoppingItemEvents(container);
  }

  function renderShoppingItem(item) {
    return `
      <div class="shopping-item ${item.checked ? 'checked' : ''}" data-id="${item.id}">
        <button class="shopping-check-btn" data-id="${item.id}" aria-label="toggle">
          <div class="shopping-checkbox ${item.checked ? 'checked' : ''}">
            ${item.checked ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>' : ''}
          </div>
        </button>
        <div class="shopping-item-info">
          <span class="shopping-item-name ${item.checked ? 'strikethrough' : ''}">${escapeHtml(item.name)}</span>
          ${item.quantity ? `<span class="shopping-item-qty">${item.quantity} ${item.unit || ''}</span>` : ''}
        </div>
        <button class="shopping-delete-btn" data-id="${item.id}" aria-label="delete">✕</button>
      </div>`;
  }

  function bindShoppingItemEvents(container) {
    container.querySelectorAll('.shopping-check-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        Storage.toggleShoppingItem(btn.dataset.id);
        render();
      });
    });
    container.querySelectorAll('.shopping-delete-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        Storage.removeShoppingItem(btn.dataset.id);
        render();
      });
    });
  }

  function updateShoppingBadge() {
    const unchecked = Storage.getShoppingList().filter(i => !i.checked).length;
    const badge = document.getElementById('nav-shopping-badge');
    badge.textContent = unchecked > 0 ? unchecked : '';
    badge.style.display = unchecked > 0 ? 'flex' : 'none';
  }

  function addManualItem() {
    const input = document.getElementById('shopping-input');
    const name = input.value.trim();
    if (!name) return;
    Storage.addShoppingItem({ name, source: 'manual' });
    input.value = '';
    render();
  }

  function copyList() {
    const allItems = Storage.getShoppingList().filter(i => !i.checked);
    if (allItems.length === 0) {
      showToast(i18n.lang === 'fr' ? 'Liste vide' : 'Empty list');
      return;
    }
    const text = allItems.map(i => {
      let line = `• ${i.name}`;
      if (i.quantity) line += ` (${i.quantity}${i.unit ? ' ' + i.unit : ''})`;
      return line;
    }).join('\n');
    navigator.clipboard.writeText(text).then(() => {
      showToast(i18n.t('shopping_copied'));
    });
  }

  function init() {
    document.getElementById('shopping-input').addEventListener('keydown', (e) => {
      if (e.key === 'Enter') addManualItem();
    });
    document.getElementById('btn-shopping-add').addEventListener('click', addManualItem);
    document.getElementById('btn-shopping-copy').addEventListener('click', copyList);
    document.getElementById('btn-shopping-clear-done').addEventListener('click', () => {
      Storage.clearCheckedShopping();
      render();
    });
    render();
  }

  return { init, render };
})();
