package com.mystockmanager.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    private static final String APP_ASSETS_HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://appassets.androidplatform.net/index.html";
    private static final String JS_ADS_BRIDGE_NAME = "AndroidAdsBridge";
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private WebView webView;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        webView = findViewById(R.id.webview);
        adView = findViewById(R.id.adView);
        configureWebView(webView);
        configureAds();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(START_URL);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void configureWebView(@NonNull WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        view.addJavascriptInterface(new AndroidAdsBridge(), JS_ADS_BRIDGE_NAME);

        boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        WebView.setWebContentsDebuggingEnabled(isDebuggable);
        
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        view.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (APP_ASSETS_HOST.equals(uri.getHost())) {
                    return false;
                }

                if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }

                return super.shouldOverrideUrlLoading(webView, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideWebPlaceholderAd(view);
            }
        });
    }

    private void configureAds() {
        MobileAds.initialize(this, initializationStatus -> {});
        updateNativeAdVisibility(false);
    }

    private void updateNativeAdVisibility(boolean enabled) {
        runOnUiThread(() -> {
            if (adView == null) return;
            if (enabled) {
                adView.setVisibility(View.VISIBLE);
                adView.resume();
                adView.loadAd(new AdRequest.Builder().build());
            } else {
                adView.pause();
                adView.setVisibility(View.GONE);
            }
        });
    }

    private void hideWebPlaceholderAd(@NonNull WebView view) {
        String script =
                "(() => {" +
                "  const applyHide = () => {" +
                "    const banner = document.getElementById('ad-banner');" +
                "    if (banner) banner.style.setProperty('display', 'none', 'important');" +
                "    const app = document.getElementById('app');" +
                "    if (app) app.classList.remove('ad-active');" +
                "  };" +
                "  applyHide();" +
                "  if (!window.__msmNativeAdObserver) {" +
                "    window.__msmNativeAdObserver = new MutationObserver(() => applyHide());" +
                "    window.__msmNativeAdObserver.observe(document.documentElement || document.body, { attributes: true, childList: true, subtree: true });" +
                "  }" +
                "})();";
        view.evaluateJavascript(script, (ValueCallback<String>) null);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    private class AndroidAdsBridge {
        @JavascriptInterface
        public void setAdsEnabled(boolean enabled) {
            updateNativeAdVisibility(enabled);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }
}
