package com.mystockmanager.app.ui.recipes

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mystockmanager.app.R
import com.mystockmanager.app.data.remote.RecipeDto
import com.mystockmanager.app.ui.components.RewardedAdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeResultsScreen(
    itemIds: List<String>,
    cuisineTypes: List<String>,
    onRecipeSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val rewardedAdUnitId = stringResource(R.string.admob_rewarded_unit_id)
    val rewardedAdManager = remember(rewardedAdUnitId) { RewardedAdManager(rewardedAdUnitId) }

    LaunchedEffect(itemIds, cuisineTypes) {
        viewModel.search(itemIds, cuisineTypes)
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is RecipeResultsUiState.Success && state.degradedReason == "quota_exceeded") {
            rewardedAdManager.load(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recipes_results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is RecipeResultsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is RecipeResultsUiState.Error -> {
                    Text(
                        text = stringResource(R.string.msg_ai_unavailable),
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is RecipeResultsUiState.Success -> {
                    if (state.recipes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.msg_no_recipes_found),
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (state.degraded) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = stringResource(
                                                if (state.degradedReason == "quota_exceeded") R.string.msg_recipes_quota_exceeded
                                                else R.string.msg_recipes_degraded
                                            ),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp
                                        )
                                        if (state.degradedReason == "quota_exceeded") {
                                            Button(
                                                onClick = {
                                                    (context as? Activity)?.let { activity ->
                                                        rewardedAdManager.show(activity) {
                                                            viewModel.claimAdBonusAndRetry()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Text(stringResource(R.string.btn_watch_ad_for_recipe))
                                            }
                                        }
                                    }
                                }
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.recipes, key = { it.id }) { recipe ->
                                    RecipeCard(recipe, onClick = { onRecipeSelected(recipe.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: RecipeDto, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(recipe.imageEmoji ?: "🍽️", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (recipe.servings != null) {
                    Text(
                        stringResource(R.string.recipes_servings, recipe.servings),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
