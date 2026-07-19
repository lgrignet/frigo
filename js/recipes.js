// ============================================================
//  recipes.js — Vue Recettes (TheMealDB)
// ============================================================

const RecipesModule = (() => {

  const MEALDB_BASE = 'https://www.themealdb.com/api/json/v1/1';
  let selectedIngredients = new Set();
  let currentRecipes = [];

  // ── Render ─────────────────────────────────────────────────
  function render() {
    renderIngredientSelector();
    renderRecipeNote();
    updateSearchBtn();
    // Don't clear recipes if already showing
  }

  function renderRecipeNote() {
    document.getElementById('recipes-note').textContent = i18n.t('recipes_note');
    document.getElementById('recipes-view-title').textContent = i18n.t('recipes_title');
    document.getElementById('recipes-subtitle').textContent = i18n.t('recipes_subtitle');
    document.getElementById('btn-recipes-search').textContent = i18n.t('recipes_search_btn');
  }

  function renderIngredientSelector() {
    const items = Storage.getItems();
    const container = document.getElementById('ingredient-selector');

    if (items.length === 0) {
      container.innerHTML = `<div class="recipes-empty-state">
        <span class="empty-icon">🧊</span>
        <p>${i18n.lang === 'fr' ? 'Votre frigo est vide. Ajoutez des aliments d\'abord.' : 'Your fridge is empty. Add items first.'}</p>
      </div>`;
      return;
    }

    container.innerHTML = items.map(item => {
      const selected = selectedIngredients.has(item.id);
      const catIcon = i18n.getCategoryIcon(item.category);
      return `
        <button class="ingredient-chip ${selected ? 'selected' : ''}" data-id="${item.id}" data-name="${escapeHtml(item.name)}">
          <span class="chip-icon">${item.photo ? `<img src="${item.photo}" alt="">` : catIcon}</span>
          <span class="chip-name">${escapeHtml(item.name)}</span>
          ${selected ? '<span class="chip-check">✓</span>' : ''}
        </button>`;
    }).join('');

    container.querySelectorAll('.ingredient-chip').forEach(chip => {
      chip.addEventListener('click', () => toggleIngredient(chip.dataset.id, chip.dataset.name));
    });
  }

  function toggleIngredient(id, name) {
    if (selectedIngredients.has(id)) {
      selectedIngredients.delete(id);
    } else {
      selectedIngredients.add(id);
    }
    renderIngredientSelector();
    updateSearchBtn();
  }

  function updateSearchBtn() {
    const btn = document.getElementById('btn-recipes-search');
    const count = selectedIngredients.size;
    if (count === 0) {
      btn.disabled = true;
      btn.textContent = i18n.t('recipes_search_btn');
    } else {
      btn.disabled = false;
      btn.textContent = `${i18n.t('recipes_search_btn')} (${count})`;
    }
  }

  // ── API ────────────────────────────────────────────────────
  async function searchRecipes() {
    if (selectedIngredients.size === 0) return;

    const resultsContainer = document.getElementById('recipes-results');
    resultsContainer.innerHTML = `<div class="recipes-loading">
      <div class="loading-spinner"></div>
      <p>${i18n.t('recipes_loading')}</p>
    </div>`;
    resultsContainer.style.display = 'block';

    const fridgeItems = Storage.getItems();
    const selectedItems = fridgeItems.filter(i => selectedIngredients.has(i.id));
    const ingredientNames = selectedItems.map(i => i.name);

    try {
      // TheMealDB: search by first ingredient, then filter/merge
      const allMeals = new Map();

      for (const ingredient of ingredientNames.slice(0, 3)) { // limit API calls
        const url = `${MEALDB_BASE}/filter.php?i=${encodeURIComponent(ingredient)}`;
        const resp = await fetch(url);
        const data = await resp.json();
        if (data.meals) {
          data.meals.forEach(m => {
            if (!allMeals.has(m.idMeal)) {
              allMeals.set(m.idMeal, { ...m, matchCount: 1 });
            } else {
              allMeals.get(m.idMeal).matchCount++;
            }
          });
        }
      }

      if (allMeals.size === 0) {
        showNoRecipes();
        return;
      }

      // Sort by match count, take top 10
      const sorted = [...allMeals.values()]
        .sort((a, b) => b.matchCount - a.matchCount)
        .slice(0, 10);

      // Fetch details for top 6
      const detailedMeals = await Promise.all(
        sorted.slice(0, 6).map(m => fetchMealDetail(m.idMeal))
      );

      currentRecipes = detailedMeals.filter(Boolean);

      if (currentRecipes.length === 0) {
        showNoRecipes();
        return;
      }

      renderRecipes(currentRecipes, ingredientNames);

    } catch (err) {
      resultsContainer.innerHTML = `<div class="recipes-error">
        <p>⚠️ ${i18n.lang === 'fr' ? 'Erreur de connexion. Vérifiez votre internet.' : 'Connection error. Check your internet.'}</p>
      </div>`;
    }
  }

  async function fetchMealDetail(id) {
    try {
      const resp = await fetch(`${MEALDB_BASE}/lookup.php?i=${id}`);
      const data = await resp.json();
      return data.meals?.[0] || null;
    } catch { return null; }
  }

  function getMealIngredients(meal) {
    const ingredients = [];
    for (let i = 1; i <= 20; i++) {
      const ing = meal[`strIngredient${i}`];
      const measure = meal[`strMeasure${i}`];
      if (ing && ing.trim()) {
        ingredients.push({ name: ing.trim(), measure: measure?.trim() || '' });
      }
    }
    return ingredients;
  }

  function renderRecipes(meals, fridgeIngredientNames) {
    const container = document.getElementById('recipes-results');
    const fridgeNamesLower = fridgeIngredientNames.map(n => n.toLowerCase());

    container.innerHTML = meals.map(meal => {
      const ingredients = getMealIngredients(meal);
      const have = ingredients.filter(ing =>
        fridgeNamesLower.some(fn => ing.name.toLowerCase().includes(fn) || fn.includes(ing.name.toLowerCase()))
      );
      const missing = ingredients.filter(ing =>
        !fridgeNamesLower.some(fn => ing.name.toLowerCase().includes(fn) || fn.includes(ing.name.toLowerCase()))
      );

      const pct = Math.round((have.length / ingredients.length) * 100);

      return `
        <div class="recipe-card" data-id="${meal.idMeal}">
          <div class="recipe-thumb" style="background-image:url('${meal.strMealThumb}/preview')">
            <div class="recipe-match-badge">${pct}% match</div>
            <div class="recipe-category-badge">${meal.strCategory || ''}</div>
          </div>
          <div class="recipe-card-body">
            <h3 class="recipe-name">${escapeHtml(meal.strMeal)}</h3>
            ${meal.strArea ? `<span class="recipe-area">${meal.strArea}</span>` : ''}
            <div class="recipe-ingredient-summary">
              <span class="ing-have">✓ ${have.length} ${i18n.t('recipes_have').toLowerCase()}</span>
              ${missing.length > 0 ? `<span class="ing-missing">✗ ${missing.length} ${i18n.t('recipes_missing').toLowerCase()}</span>` : ''}
            </div>
            <div class="recipe-progress-bar">
              <div class="recipe-progress-fill" style="width:${pct}%"></div>
            </div>
            <div class="recipe-actions">
              ${missing.length > 0 ? `<button class="btn-recipe-shopping btn-sm" data-id="${meal.idMeal}">🛒 ${i18n.t('recipes_add_shopping')}</button>` : ''}
              <button class="btn-recipe-view btn-sm btn-primary" data-id="${meal.idMeal}">📖 ${i18n.t('recipes_view_full')}</button>
            </div>
          </div>
        </div>`;
    }).join('');

    // Bind buttons
    container.querySelectorAll('.btn-recipe-view').forEach(btn => {
      btn.addEventListener('click', () => {
        const meal = currentRecipes.find(m => m.idMeal === btn.dataset.id);
        if (meal) openRecipeModal(meal, fridgeIngredientNames);
      });
    });

    container.querySelectorAll('.btn-recipe-shopping').forEach(btn => {
      btn.addEventListener('click', () => {
        const meal = currentRecipes.find(m => m.idMeal === btn.dataset.id);
        if (meal) addMissingToShopping(meal, fridgeIngredientNames);
      });
    });
  }

  function showNoRecipes() {
    document.getElementById('recipes-results').innerHTML = `
      <div class="recipes-empty-state">
        <span class="empty-icon">🍽️</span>
        <h3>${i18n.t('recipes_empty')}</h3>
        <p>${i18n.t('recipes_empty_sub')}</p>
      </div>`;
  }

  // ── Recipe modal ────────────────────────────────────────────
  function openRecipeModal(meal, fridgeIngredientNames) {
    const fridgeNamesLower = fridgeIngredientNames.map(n => n.toLowerCase());
    const ingredients = getMealIngredients(meal);

    document.getElementById('modal-recipe-thumb').style.backgroundImage = `url('${meal.strMealThumb}')`;
    document.getElementById('modal-recipe-name').textContent = meal.strMeal;
    document.getElementById('modal-recipe-meta').textContent =
      [meal.strCategory, meal.strArea].filter(Boolean).join(' · ');

    // Ingredients list
    document.getElementById('modal-recipe-ingredients-title').textContent = i18n.t('recipes_ingredients_list');
    document.getElementById('modal-recipe-ingredients').innerHTML = ingredients.map(ing => {
      const inFridge = fridgeNamesLower.some(fn =>
        ing.name.toLowerCase().includes(fn) || fn.includes(ing.name.toLowerCase())
      );
      return `<div class="recipe-ing-row ${inFridge ? 'ing-have' : 'ing-missing'}">
        <span class="ing-icon">${inFridge ? '✓' : '✗'}</span>
        <span class="ing-name">${escapeHtml(ing.name)}</span>
        ${ing.measure ? `<span class="ing-measure">${escapeHtml(ing.measure)}</span>` : ''}
      </div>`;
    }).join('');

    // Instructions
    document.getElementById('modal-recipe-instructions-title').textContent = i18n.t('recipes_instructions');
    const instructions = meal.strInstructions || '';
    document.getElementById('modal-recipe-instructions').innerHTML = instructions
      .split('\n')
      .filter(l => l.trim())
      .map((step, i) => `<p class="recipe-step"><span class="step-num">${i + 1}</span>${escapeHtml(step)}</p>`)
      .join('') || `<p>${escapeHtml(instructions)}</p>`;

    // Source link
    const sourceLink = document.getElementById('modal-recipe-source');
    if (meal.strSource) {
      sourceLink.href = meal.strSource;
      sourceLink.style.display = 'inline-flex';
    } else if (meal.strYoutube) {
      sourceLink.href = meal.strYoutube;
      sourceLink.style.display = 'inline-flex';
      sourceLink.textContent = '▶ YouTube';
    } else {
      sourceLink.style.display = 'none';
    }

    const missing = ingredients.filter(ing =>
      !fridgeNamesLower.some(fn => ing.name.toLowerCase().includes(fn) || fn.includes(ing.name.toLowerCase()))
    );
    const btnShopping = document.getElementById('btn-modal-recipe-shopping');
    if (missing.length > 0) {
      btnShopping.style.display = 'flex';
      btnShopping.onclick = () => addMissingToShopping(meal, fridgeIngredientNames);
    } else {
      btnShopping.style.display = 'none';
    }

    document.getElementById('btn-modal-recipe-close').onclick = () => closeModal('modal-recipe');
    openModal('modal-recipe');
  }

  function addMissingToShopping(meal, fridgeIngredientNames) {
    const fridgeNamesLower = fridgeIngredientNames.map(n => n.toLowerCase());
    const ingredients = getMealIngredients(meal);
    const missing = ingredients.filter(ing =>
      !fridgeNamesLower.some(fn => ing.name.toLowerCase().includes(fn) || fn.includes(ing.name.toLowerCase()))
    );
    missing.forEach(ing => {
      const existing = Storage.getShoppingList().find(
        s => s.name.toLowerCase() === ing.name.toLowerCase() && s.source === 'recipe'
      );
      if (!existing) {
        Storage.addShoppingItem({ name: ing.name, quantity: ing.measure, unit: '', source: 'recipe' });
      }
    });
    ShoppingModule.render();
    showToast(i18n.t('recipes_added_shopping'));
  }

  function init() {
    document.getElementById('btn-recipes-search').addEventListener('click', searchRecipes);
    document.getElementById('btn-modal-recipe-close').addEventListener('click', () => closeModal('modal-recipe'));
    render();
  }

  return { init, render };
})();
