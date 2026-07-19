// ============================================================
//  recipes.view.js — Vue Recettes (TheMealDB)
// ============================================================

const RecipesView = (() => {
  const MEALDB = 'https://www.themealdb.com/api/json/v1/1';
  let selectedIds = new Set();
  let currentRecipes = [];

  function render() {
    document.getElementById('recipes-modal-title').textContent = i18n.t('recipes_title');
    document.getElementById('recipes-note-text').textContent = i18n.t('recipes_note');
    document.getElementById('btn-recipes-search').textContent = i18n.t('recipes_search');
    renderIngredients();
  }

  async function renderIngredients() {
    const userId = Auth.getCurrentUserId();
    const items = await ItemModel.getAll(userId);
    const storages = await StorageModel.getAll(userId);
    const container = document.getElementById('recipes-chips');

    if (items.length === 0) {
      container.innerHTML = `<div style="color:var(--text-muted);font-size:13px;padding:8px">${i18n.lang === 'fr' ? 'Frigo vide' : 'Fridge empty'}</div>`;
      return;
    }

    container.innerHTML = items.map(item => {
      const sel = selectedIds.has(item.id);
      const storage = storages.find(s => s.id === item.storageId);
      return `
        <button class="chip ${sel ? 'selected' : ''}" data-id="${item.id}" data-name="${escHtml(item.name)}">
          ${item.photo ? `<img class="chip-thumb" src="${item.photo}" alt="">` : (storage?.icon || '📦')}
          ${escHtml(item.name)}
          ${sel ? '<span style="font-size:11px">✓</span>' : ''}
        </button>`;
    }).join('');

    container.querySelectorAll('.chip').forEach(chip => {
      chip.addEventListener('click', () => {
        if (selectedIds.has(chip.dataset.id)) selectedIds.delete(chip.dataset.id);
        else selectedIds.add(chip.dataset.id);
        renderIngredients();
        updateSearchBtn();
      });
    });
    updateSearchBtn();
  }

  function updateSearchBtn() {
    const btn = document.getElementById('btn-recipes-search');
    btn.disabled = selectedIds.size === 0;
  }

  async function searchRecipes() {
    const userId = Auth.getCurrentUserId();
    const items = await ItemModel.getAll(userId);
    const selectedItems = items.filter(i => selectedIds.has(i.id));
    const ingredientNames = selectedItems.map(i => i.name);

    const resultsEl = document.getElementById('recipes-results');
    resultsEl.innerHTML = `<div class="spinner"></div>`;
    resultsEl.style.display = 'block';

    try {
      const allMeals = new Map();
      for (const name of ingredientNames.slice(0, 3)) {
        const resp = await fetch(`${MEALDB}/filter.php?i=${encodeURIComponent(name)}`);
        const data = await resp.json();
        (data.meals || []).forEach(m => {
          if (!allMeals.has(m.idMeal)) allMeals.set(m.idMeal, { ...m, matchCount: 1 });
          else allMeals.get(m.idMeal).matchCount++;
        });
      }

      if (allMeals.size === 0) {
        resultsEl.innerHTML = `<div class="empty-state"><div class="empty-icon">🍽️</div><div class="empty-title">${i18n.t('recipes_empty')}</div><p class="empty-sub">${i18n.t('recipes_empty_sub')}</p></div>`;
        return;
      }

      const sorted = [...allMeals.values()].sort((a,b) => b.matchCount - a.matchCount).slice(0,6);
      const detailed = await Promise.all(sorted.map(m => fetchDetail(m.idMeal)));
      currentRecipes = detailed.filter(Boolean);

      renderRecipeCards(currentRecipes, ingredientNames);
    } catch {
      resultsEl.innerHTML = `<p style="color:var(--warning);padding:20px;text-align:center">⚠️ ${i18n.lang === 'fr' ? 'Erreur réseau' : 'Network error'}</p>`;
    }
  }

  async function fetchDetail(id) {
    try {
      const r = await fetch(`${MEALDB}/lookup.php?i=${id}`);
      const d = await r.json();
      return d.meals?.[0] || null;
    } catch { return null; }
  }

  function getMealIngredients(meal) {
    const ings = [];
    for (let i = 1; i <= 20; i++) {
      const n = meal[`strIngredient${i}`];
      const m = meal[`strMeasure${i}`];
      if (n?.trim()) ings.push({ name: n.trim(), measure: m?.trim() || '' });
    }
    return ings;
  }

  function renderRecipeCards(meals, fridgeNames) {
    const container = document.getElementById('recipes-results');
    const fridgeLow = fridgeNames.map(n => n.toLowerCase());

    container.innerHTML = meals.map(meal => {
      const ings = getMealIngredients(meal);
      const have = ings.filter(i => fridgeLow.some(f => i.name.toLowerCase().includes(f) || f.includes(i.name.toLowerCase())));
      const missing = ings.filter(i => !fridgeLow.some(f => i.name.toLowerCase().includes(f) || f.includes(i.name.toLowerCase())));
      const pct = Math.round((have.length / ings.length) * 100);

      return `
        <div class="recipe-card" data-id="${meal.idMeal}">
          <div class="recipe-thumb" style="background-image:url('${meal.strMealThumb}/preview')">
            <div class="recipe-match-badge">${pct}%</div>
          </div>
          <div class="recipe-body">
            <div class="recipe-name">${escHtml(meal.strMeal)}</div>
            <div class="recipe-meta">${[meal.strCategory, meal.strArea].filter(Boolean).join(' · ')}</div>
            <div class="recipe-ingredient-bar">
              <span class="ing-have">✓ ${have.length} ${i18n.t('recipes_have')}</span>
              ${missing.length > 0 ? `<span class="ing-missing">✗ ${missing.length} ${i18n.t('recipes_missing')}</span>` : ''}
            </div>
            <div class="recipe-progress"><div class="recipe-progress-fill" style="width:${pct}%"></div></div>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <button class="btn btn-secondary btn-sm btn-recipe-detail" data-id="${meal.idMeal}">📖 ${i18n.lang === 'fr' ? 'Voir' : 'View'}</button>
              ${missing.length > 0 ? `<button class="btn btn-primary btn-sm btn-add-missing" data-id="${meal.idMeal}">🛒 ${i18n.t('recipes_add_shopping')}</button>` : ''}
            </div>
          </div>
        </div>`;
    }).join('');

    container.querySelectorAll('.btn-recipe-detail').forEach(btn => {
      btn.addEventListener('click', () => {
        const meal = currentRecipes.find(m => m.idMeal === btn.dataset.id);
        if (meal) openRecipeDetail(meal, fridgeNames);
      });
    });
    container.querySelectorAll('.btn-add-missing').forEach(btn => {
      btn.addEventListener('click', async () => {
        const meal = currentRecipes.find(m => m.idMeal === btn.dataset.id);
        if (!meal) return;
        await addMissingToShopping(meal, fridgeNames);
      });
    });
  }

  async function addMissingToShopping(meal, fridgeNames) {
    const userId = Auth.getCurrentUserId();
    const fridgeLow = fridgeNames.map(n => n.toLowerCase());
    const ings = getMealIngredients(meal);
    const missing = ings.filter(i => !fridgeLow.some(f => i.name.toLowerCase().includes(f) || f.includes(i.name.toLowerCase())));
    const existing = await ShoppingModel.getAll(userId);
    for (const ing of missing) {
      const already = existing.some(s => s.name.toLowerCase() === ing.name.toLowerCase() && s.source === 'recipe');
      if (!already) await ShoppingModel.add(userId, { name: ing.name, quantity: ing.measure, source: 'recipe' });
    }
    ShoppingView.render();
    Toast.success(i18n.t('recipes_added'));
  }

  function openRecipeDetail(meal, fridgeNames) {
    const fridgeLow = fridgeNames.map(n => n.toLowerCase());
    const ings = getMealIngredients(meal);

    document.getElementById('rd-thumb').style.backgroundImage = `url('${meal.strMealThumb}')`;
    document.getElementById('rd-name').textContent = meal.strMeal;
    document.getElementById('rd-meta').textContent = [meal.strCategory, meal.strArea].filter(Boolean).join(' · ');
    document.getElementById('rd-ing-title').textContent = i18n.t('recipes_ingredients');
    document.getElementById('rd-inst-title').textContent = i18n.t('recipes_instructions');

    document.getElementById('rd-ingredients').innerHTML = ings.map(ing => {
      const inFridge = fridgeLow.some(f => ing.name.toLowerCase().includes(f) || f.includes(ing.name.toLowerCase()));
      return `<div style="display:flex;align-items:center;gap:10px;padding:8px 0;border-bottom:1px solid var(--border);font-size:13px">
        <span style="color:${inFridge ? 'var(--success)' : 'var(--danger)'};font-weight:700;width:14px">${inFridge ? '✓' : '✗'}</span>
        <span style="flex:1">${escHtml(ing.name)}</span>
        <span style="color:var(--text-secondary)">${escHtml(ing.measure)}</span>
      </div>`;
    }).join('');

    document.getElementById('rd-instructions').innerHTML = (meal.strInstructions || '')
      .split('\n').filter(l => l.trim())
      .map((step, i) => `<div style="display:flex;gap:12px;margin-bottom:12px;font-size:13px;color:var(--text-secondary)">
        <div style="width:22px;height:22px;border-radius:50%;background:var(--accent-glow);border:1px solid var(--accent);color:var(--accent);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;flex-shrink:0;margin-top:2px">${i+1}</div>
        <p style="flex:1;line-height:1.6">${escHtml(step)}</p>
      </div>`).join('');

    const missing = ings.filter(i => !fridgeLow.some(f => i.name.toLowerCase().includes(f) || f.includes(i.name.toLowerCase())));
    const btnShop = document.getElementById('btn-rd-shopping');
    btnShop.style.display = missing.length > 0 ? 'flex' : 'none';
    btnShop.onclick = async () => {
      await addMissingToShopping(meal, fridgeNames);
      Modal.close('modal-recipe-detail');
    };
    btnShop.textContent = `🛒 ${i18n.t('recipes_add_shopping')}`;

    const src = document.getElementById('rd-source');
    if (meal.strSource || meal.strYoutube) {
      src.href = meal.strSource || meal.strYoutube;
      src.style.display = 'inline-flex';
    } else {
      src.style.display = 'none';
    }

    Modal.open('modal-recipe-detail');
  }

  function open() {
    render();
    Modal.openFullscreen('modal-recipes');
  }

  function init() {
    document.getElementById('btn-recipes-search').addEventListener('click', searchRecipes);
    document.getElementById('btn-recipes-close').addEventListener('click', () => Modal.closeFullscreen('modal-recipes'));
    document.getElementById('btn-rd-close').addEventListener('click', () => Modal.close('modal-recipe-detail'));
  }

  return { init, render, open };
})();
