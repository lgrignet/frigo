package com.mystockmanager.app.ui.components

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Charge et affiche une pub récompensée AdMob. Le chargement est lancé à l'avance
 * (appeler [load] dès que le CTA "regarder une pub" peut apparaître) pour que [show]
 * n'ait pas à attendre un aller-retour réseau au moment où l'utilisateur tape dessus.
 */
class RewardedAdManager(private val adUnitId: String) {
    private var rewardedAd: RewardedAd? = null
    private var loading = false

    fun load(context: Context) {
        if (rewardedAd != null || loading) return
        loading = true
        RewardedAd.load(context, adUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                loading = false
                rewardedAd = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false
                rewardedAd = null
            }
        })
    }

    /** [onRewardEarned] n'est appelé que si la pub a été regardée jusqu'au bout. */
    fun show(activity: Activity, onRewardEarned: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            load(activity)
            return
        }
        rewardedAd = null
        ad.show(activity) { onRewardEarned() }
    }
}
