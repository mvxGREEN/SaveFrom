package com.mvxgreen.ytdloader.manager

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.mvxgreen.ytdloader.MainActivity
import com.mvxgreen.ytdloader.R
import com.mvxgreen.ytdloader.databinding.ActivityMainBinding

object AdsManager {
    private val TAG: String = AdsManager::class.java.getCanonicalName()
    private var mInterstitialAd: InterstitialAd? = null

    private const val PKG_SPOTIFLYER = "com.mvxgreen.spotloader"
    private const val MSG_BAD_TOKEN_EXCEPTION = "caught bad token exception!"

    private const val ID_INTER_REAL = "ca-app-pub-7417392682402637/2662302255"
    private const val ID_INTER_TEST = "ca-app-pub-3940256099942544/1033173712"
    private const val ID_BANNER_REAL = "ca-app-pub-7417392682402637/2853873948"
    private const val ID_BANNER_TEST = "ca-app-pub-3940256099942544/9214589741"

    private val ID_INTERSTITIAL = ID_INTER_TEST
    private val ID_BANNER = ID_BANNER_TEST

    /**
     * Decide which ad to display based on runs
     * 
     * @param runs # of runs
     * @param main main activity
     */
    fun showLocalAd(runs: Int, main: MainActivity) {
        val adIndex = (runs % 9)
        when (adIndex) {
            1, 7 -> showRateAd(main)
            else -> {}
        }
    }

    /**
     * Ask the user to rate the app
     * @param main main activity
     */
    fun showRateAd(main: MainActivity) {
        Log.i(TAG, "Showing rate ad")

        val appPackageName = main.getApplicationContext().getPackageName()

        val dialog = Dialog(ContextThemeWrapper(main, R.style.DialogDrip))
        dialog.setTitle(main.getString(R.string.msg_rate_dialog_title))

        val ll = LinearLayout(main)
        ll.setOrientation(LinearLayout.VERTICAL)

        val tv = TextView(main)
        val msg = main.getString(R.string.msg_rate_dialog_body)
        tv.setText(msg)
        tv.setWidth(280)
        tv.setPadding(4, 0, 4, 43)
        tv.setTextAppearance(R.style.TextAppFragBody)
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        ll.addView(tv)

        val l2 = LinearLayout(main)
        l2.setOrientation(LinearLayout.HORIZONTAL)
        l2.setBottom(ll.getBottom())
        l2.setForegroundGravity(Gravity.BOTTOM)

        val b3 = Button(ContextThemeWrapper(main, R.style.ButtonDripBad))
        b3.setText(main.getString(R.string.msg_rate_button2))
        b3.setOnClickListener(View.OnClickListener { v: View? -> dialog.dismiss() })
        l2.addView(b3)

        val b1 = Button(ContextThemeWrapper(main, R.style.ButtonDripGood))
        b1.setText(main.getString(R.string.msg_rate_button1))
        b1.setOnClickListener(View.OnClickListener { v: View? ->
            main.startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                        "market://details?id="
                                + appPackageName
                    )
                )
            )
            dialog.dismiss()
        })
        l2.addView(b1)

        ll.addView(l2)
        dialog.setContentView(ll)
        if (!main.isFinishing()) {
            try {
                dialog.show()
            } catch (e: Exception) {
                Log.w(TAG, MSG_BAD_TOKEN_EXCEPTION)
            }
        }
    }

    /**
     * Ask user to install spotify downloader
     * @param main context
     */
    fun showSpotiflyerAd(main: MainActivity, adIndex: Int) {
        Log.i(TAG, "Showing spotiflyer ad...")
        val dialog = Dialog(ContextThemeWrapper(main, R.style.DialogDrip))
        dialog.setTitle(main.getString(R.string.ad_title_spotiflyer))

        val ll = LinearLayout(main)
        ll.setOrientation(LinearLayout.VERTICAL)

        val cl = RelativeLayout(main)
        cl.setGravity(RelativeLayout.CENTER_HORIZONTAL)

        val msg = main.getString(R.string.ad_body_spotiflyer)

        /*
        int iconBottom;
        ImageView imageView = new ImageView(main);
        imageView.setImageResource(R.drawable.promo_spotiflyer);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(640, 320));
        cl.addView(imageView);
        iconBottom = imageView.getBottom();
         */
        ll.addView(cl)

        val body = TextView(main)
        body.setText(msg)
        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        params.setMargins(0, 16, 0, 20)
        body.setPadding(8, 8, 8, 8)
        body.setLayoutParams(params)
        body.setTextAppearance(R.style.TextAppFragBody)
        body.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        ll.addView(body)

        val l2 = LinearLayout(main)
        l2.setOrientation(LinearLayout.HORIZONTAL)
        l2.setBottom(ll.getBottom())
        l2.setForegroundGravity(Gravity.CENTER)

        val b2 = Button(ContextThemeWrapper(main, R.style.ButtonDripBad))
        b2.setText(main.getString(R.string.ad_btn_negative))
        b2.setWidth(320)
        b2.setOnClickListener(View.OnClickListener { v: View? -> dialog.dismiss() })
        l2.addView(b2)

        val b1 = Button(ContextThemeWrapper(main, R.style.ButtonDripGood))
        b1.setText(main.getString(R.string.ad_btn_positive))
        b1.setWidth(320)
        b1.setOnClickListener(View.OnClickListener { v: View? ->
            main.startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                        "market://details?id="
                                + PKG_SPOTIFLYER
                    )
                )
            )
            dialog.dismiss()
        })
        l2.addView(b1)

        ll.addView(l2)
        dialog.setContentView(ll)
        if (!main.isFinishing()) {
            try {
                dialog.show()
            } catch (e: Exception) {
                Log.w(TAG, MSG_BAD_TOKEN_EXCEPTION)
            }
        }
    }

    // ADMOB
    fun loadAdmobInterstitialAd(main: MainActivity) {
        Log.i(TAG, "loadAdmobInterstitialAd")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            main, ID_INTERSTITIAL, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ia: InterstitialAd) {
                    Log.i(TAG, "interstitial onLoaded")
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    mInterstitialAd = ia
                    mInterstitialAd!!.setFullScreenContentCallback(object :
                        FullScreenContentCallback() {
                        override fun onAdClicked() {
                            // Called when a click is recorded for an ad.
                            Log.d(TAG, "Ad was clicked.")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            // Set the ad reference to null so you don't show the ad a second time.
                            Log.d(TAG, "Ad dismissed fullscreen content.")
                            mInterstitialAd = null
                            // load next interstitial ad
                            loadAdmobInterstitialAd(main)
                        }

                        override fun onAdImpression() {
                            // Called when an impression is recorded for an ad.
                            Log.d(TAG, "Ad recorded an impression.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            Log.d(TAG, "Ad showed fullscreen content.")
                        }
                    })
                    Log.i(TAG, "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    Log.d(TAG, loadAdError.toString())
                    mInterstitialAd = null
                }
            })
    }

    fun showInterstitialAd(main: MainActivity) {
        if (mInterstitialAd != null) {
            mInterstitialAd!!.show(main)
        } else {
            Log.d("TAG", "mInterstitialAd wasn't ready yet.")
        }
    }

    private fun getBannerAdSize(main: MainActivity, binding: ActivityMainBinding): AdSize {
        // Determine the screen width (less decorations) to use for the ad width.
        val display = main.getWindowManager().getDefaultDisplay()
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = binding.bannerContainer.getWidth().toFloat()

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(main, adWidth)
    }

    fun loadBanner(main: MainActivity, binding: ActivityMainBinding) {
        // Create a new ad view.
        val adView = AdView(main)
        adView.setAdSize(getBannerAdSize(main, binding))

        adView.setAdUnitId(ID_BANNER)

        // Replace ad container with new ad view.
        binding.bannerContainer.removeAllViews()
        binding.bannerContainer.addView(adView)

        // Start loading the ad in the background.
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}
