package com.example.joemovies.ui.player;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.joemovies.R;
import com.example.joemovies.utils.VideoUrlResolver;

import java.util.Objects;

public class PlayerActivity extends AppCompatActivity {

    private static final String HIDE_UI_JS =
            "(function(){"
                    + "if(window.__hideUiInstalled)return;window.__hideUiInstalled=true;"
                    + "var css=\".ndfHFb-c4325c-z6088d,.drive-viewer-popout-button,.ndfHFb-c4325c-i5A16b,"
                    + ".ndfHFb-c4325c-pT0Scc-i5A16b,.ytp-chrome-top,.ytp-chrome-bottom,.ytp-gradient-top,"
                    + ".ytp-gradient-bottom,.ytp-pause-overlay,.ytp-show-cards-title,"
                    + "a[aria-label*='pop-out'],a[aria-label*='Pop-out'],a[target='_blank']"
                    + "{display:none!important;opacity:0!important;visibility:hidden!important;pointer-events:none!important;}\";"
                    // inyecta una sola vez por documento (id como guardia, sin duplicar nodos)
                    + "function apply(d){try{if(d.getElementById('__hideUi'))return;"
                    + "var s=d.createElement('style');s.id='__hideUi';s.textContent=css;"
                    + "(d.head||d.documentElement).appendChild(s);}catch(e){}}"
                    + "apply(document);"
                    + "setInterval(function(){apply(document);"
                    + "try{var f=document.querySelectorAll('iframe');"
                    + "for(var i=0;i<f.length;i++){var sd=f[i].contentDocument;if(sd)apply(sd);}}catch(e){}},1000);"
                    + "})();";

    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private WebView webView;
    private ProgressBar pbPlayerLoading;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.playerView);
        webView = findViewById(R.id.webView);
        pbPlayerLoading = findViewById(R.id.pbPlayerLoading);
        btnBack = findViewById(R.id.btnPlayerBack);

        btnBack.setOnClickListener(v -> finish());
        applyOrientationUi(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        String videoUrl = getIntent().getStringExtra("videoUrl");
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Toast.makeText(this, "URL de video no válida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        playVideo(videoUrl.trim());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyOrientationUi(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private void applyOrientationUi(boolean landscape) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !landscape);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (landscape) {
            c.hide(WindowInsetsCompat.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            btnBack.setVisibility(View.GONE);
        } else {
            c.show(WindowInsetsCompat.Type.systemBars());
            btnBack.setVisibility(View.VISIBLE);
        }
    }

    private void playVideo(String url) {
        String direct = VideoUrlResolver.resolveDirect(url);
        if (direct != null) setupNativeVideoView(direct, url);
        else setupWebView(url);
    }

    private void setupNativeVideoView(String url, String fallbackUrl) {
        playerView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        View blockPopout = findViewById(R.id.blockPopout);
        if (blockPopout != null) blockPopout.setVisibility(View.GONE);
        pbPlayerLoading.setVisibility(View.VISIBLE);

        try {
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(exoPlayer);
                exoPlayer.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY) pbPlayerLoading.setVisibility(View.GONE);
                        else if (state == Player.STATE_BUFFERING) pbPlayerLoading.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onPlayerError(@NonNull PlaybackException error) {
                        releasePlayer();
                        setupWebView(fallbackUrl);
                    }
                });
            }
            exoPlayer.setMediaSource(VideoUrlResolver.buildMediaSource(this, url));
            exoPlayer.prepare();
            exoPlayer.play();
        } catch (Exception e) {
            releasePlayer();
            setupWebView(fallbackUrl);
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            playerView.setPlayer(null);
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private static boolean isGoogleHost(String h) {
        return h != null && (h.equals("google.com") || h.endsWith(".google.com"));
    }

    private static boolean isSameSite(String h, String allowed) {
        return h != null && allowed != null && (h.equals(allowed) || h.endsWith("." + allowed));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(String rawUrl) {
        final String fileId = VideoUrlResolver.extractDriveId(rawUrl);
        final String allowedHost = Uri.parse(rawUrl).getHost();

        View blockPopout = findViewById(R.id.blockPopout);
        if (blockPopout != null) blockPopout.setVisibility(fileId != null ? View.VISIBLE : View.GONE);

        playerView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        pbPlayerLoading.setVisibility(View.VISIBLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportMultipleWindows(false);

        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private CustomViewCallback customViewCallback;

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) { onHideCustomView(); return; }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                ((ViewGroup) getWindow().getDecorView()).addView(customView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                ((ViewGroup) getWindow().getDecorView()).removeView(customView);
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 80) pbPlayerLoading.setVisibility(View.GONE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return true;
                String h = request.getUrl().getHost();
                // true = bloquear. Solo permite google.com (y subdominios) o el host original.
                return !(isGoogleHost(h) || isSameSite(h, allowedHost) || Objects.equals(h, allowedHost));
            }

            @Override
            public void onPageFinished(WebView view, String loadedUrl) {
                super.onPageFinished(view, loadedUrl);
                pbPlayerLoading.setVisibility(View.GONE);
                view.evaluateJavascript(HIDE_UI_JS, null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    pbPlayerLoading.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "No se pudo cargar el video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (fileId != null) {
            String embedHtml = "<html><head>"
                    + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover\">"
                    + "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;}"
                    + "iframe{position:absolute;top:0;left:0;width:100vw;height:100vh;border:none;}</style></head><body>"
                    + "<iframe src=\"https://drive.google.com/file/d/" + fileId + "/preview\" allow=\"autoplay; fullscreen\" allowfullscreen=\"true\"></iframe>"
                    + "</body></html>";
            webView.loadDataWithBaseURL("https://drive.google.com", embedHtml, "text/html", "utf-8", null);
        } else {
            webView.loadUrl(rawUrl);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}