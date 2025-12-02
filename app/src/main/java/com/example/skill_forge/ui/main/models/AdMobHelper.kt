package com.example.skill_forge.ui.main.models

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobHelper(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private val TAG = "AdMobHelper"

    // Test Ad Unit ID for Rewarded Ads.
    // IMPORTANT: REPLACE with your real ID from AdMob Console for Release builds.
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    init {
        loadRewardedAd()
    }

    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad

                // Set the FullScreenContentCallback to handle events like closing the ad
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed fullscreen content.")
                        rewardedAd = null
                        // Pre-load the next ad so it's ready when the user wants it again
                        loadRewardedAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Ad failed to show fullscreen content.")
                        rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                    }
                }
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (Int) -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
                // Handle the reward.
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "User earned the reward: $rewardAmount $rewardType")
                onRewardEarned(rewardAmount)
            })
        } ?: run {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            // Try to reload if it wasn't ready
            loadRewardedAd()
        }
    }

    fun isAdReady(): Boolean {
        return rewardedAd != null
    }
}