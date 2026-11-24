package com.gulfjobworks.mobile;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Insets;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.net.http.SslError;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String ROOT = "https://gulfjobworks.com/";
    private static final String[] TABS = {
        ROOT,
        ROOT + "job-openings/",
        ROOT + "upload-cv/",
        ROOT + "contact-us/"
    };
    private static final String[] TITLES = {"Home", "Jobs", "Upload CV", "Contact"};
    private static final int[] ICONS = {
        R.drawable.nav_home,
        R.drawable.nav_jobs,
        R.drawable.nav_upload,
        R.drawable.nav_contact
    };
    private static final int FILE_REQUEST = 2301;
    private static final int NAVY = Color.rgb(10, 36, 61);
    private static final int GOLD = Color.rgb(213, 166, 40);
    private static final int MUTED = Color.rgb(103, 119, 134);
    private static final String MOBILE_CSS =
        "html,body{max-width:100vw!important;overflow-x:hidden!important;}" +
        "img,video{max-width:100%!important;height:auto;}" +
        "iframe{max-width:100%!important;}" +
        "input,select,textarea{max-width:100%;box-sizing:border-box;}" +
        "@media(max-width:767px){input,select,textarea{font-size:16px!important;}" +
        "button,input[type=submit]{min-height:44px;}" +
        "a,button{-webkit-tap-highlight-color:rgba(213,166,40,.18);}}";

    private WebView webView;
    private FrameLayout pageFrame;
    private LinearLayout errorPanel;
    private ProgressBar loading;
    private final List<LinearLayout> navItems = new ArrayList<>();
    private final List<ImageView> navIcons = new ArrayList<>();
    private final List<TextView> navLabels = new ArrayList<>();
    private ValueCallback<Uri[]> pendingFiles;
    private String currentUrl = ROOT;
    private boolean mainFrameFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        buildLayout();
        configureWebView();
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            goTo(ROOT);
        }
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 35) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), 0, dp(8), 0);
        header.setBackgroundColor(NAVY);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView brand = new TextView(this);
        brand.setText("GULF JOB WORKS");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(17);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setLetterSpacing(0.03f);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView reload = headerAction("⟳", "Reload current page");
        reload.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });
        header.addView(reload);

        TextView more = headerAction("⋮", "More options");
        more.setTextSize(26);
        more.setOnClickListener(this::showMoreMenu);
        header.addView(more);

        loading = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        loading.setMax(100);
        loading.setProgressTintList(ColorStateList.valueOf(GOLD));
        loading.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(219, 229, 235)));
        root.addView(loading, new LinearLayout.LayoutParams(-1, dp(3)));

        pageFrame = new FrameLayout(this);
        root.addView(pageFrame, new LinearLayout.LayoutParams(-1, 0, 1));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        pageFrame.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        createErrorPanel();

        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(Color.WHITE);
        bottom.setElevation(dp(8));
        root.addView(bottom, new LinearLayout.LayoutParams(-1, dp(68)));
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setContentDescription(TITLES[i]);
            item.setOnClickListener(v -> goTo(TABS[index]));
            ImageView icon = new ImageView(this);
            icon.setImageResource(ICONS[i]);
            item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
            TextView label = new TextView(this);
            label.setText(TITLES[i]);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, dp(20));
            labelParams.topMargin = dp(3);
            item.addView(label, labelParams);
            bottom.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
            navItems.add(item);
            navIcons.add(icon);
            navLabels.add(label);
        }
        selectTab(0);
    }

    private TextView headerAction(String symbol, String description) {
        TextView view = new TextView(this);
        view.setText(symbol);
        view.setTextSize(25);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return view;
    }

    private void createErrorPanel() {
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(20), dp(28), dp(20));
        errorPanel.setBackgroundColor(Color.WHITE);
        TextView title = new TextView(this);
        title.setText("Connection unavailable");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(NAVY);
        title.setGravity(Gravity.CENTER);
        errorPanel.addView(title);
        TextView note = new TextView(this);
        note.setText("Check your internet connection and try again.");
        note.setTextSize(15);
        note.setTextColor(MUTED);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(12);
        errorPanel.addView(note, noteParams);
        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setTextColor(Color.WHITE);
        retry.setBackgroundTintList(ColorStateList.valueOf(NAVY));
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(-1, dp(52));
        retryParams.topMargin = dp(24);
        errorPanel.addView(retry, retryParams);
        retry.setOnClickListener(v -> goTo(currentUrl));
        errorPanel.setVisibility(View.GONE);
        pageFrame.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isInternal(uri)) return false;
                openExternal(uri);
                return true;
            }
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                currentUrl = url;
                mainFrameFailed = false;
                errorPanel.setVisibility(View.GONE);
                loading.setVisibility(View.VISIBLE);
                updateSelection(url);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mainFrameFailed && isInternal(Uri.parse(url))) {
                    String js = "(function(){if(document.getElementById('gjw-mobile-tweaks'))return;" +
                        "var s=document.createElement('style');s.id='gjw-mobile-tweaks';" +
                        "s.textContent=" + JSONObject.quote(MOBILE_CSS) + ";" +
                        "document.head.appendChild(s);})();";
                    view.evaluateJavascript(js, null);
                }
                loading.setVisibility(View.GONE);
                updateSelection(url);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    mainFrameFailed = true;
                    loading.setVisibility(View.GONE);
                    errorPanel.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                mainFrameFailed = true;
                errorPanel.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                loading.setProgress(progress);
                loading.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
            }
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.addCategory(Intent.CATEGORY_OPENABLE);
                picker.setType("*/*");
                picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                String[] types = params.getAcceptTypes();
                if (types != null && types.length > 0 && !types[0].isEmpty()) {
                    picker.putExtra(Intent.EXTRA_MIME_TYPES, types);
                }
                try {
                    startActivityForResult(picker, FILE_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    pendingFiles = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "No file picker found", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Uri uri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                Toast.makeText(this, "This download cannot be opened", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                openExternal(uri);
            }
        });
    }

    private boolean isInternal(Uri uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(scheme) || host == null) return false;
        String name = host.toLowerCase(Locale.US);
        return name.equals("gulfjobworks.com") || name.endsWith(".gulfjobworks.com");
    }

    private void openExternal(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !(
                scheme.equalsIgnoreCase("https") ||
                scheme.equalsIgnoreCase("http") ||
                scheme.equalsIgnoreCase("mailto") ||
                scheme.equalsIgnoreCase("tel") ||
                scheme.equalsIgnoreCase("sms") ||
                scheme.equalsIgnoreCase("whatsapp"))) {
            Toast.makeText(this, "Unsupported link", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show();
        }
    }

    private void goTo(String url) {
        currentUrl = url;
        errorPanel.setVisibility(View.GONE);
        webView.loadUrl(url);
        updateSelection(url);
    }

    private void updateSelection(String url) {
        int active = -1;
        Uri uri = Uri.parse(url);
        String path = uri.getPath() == null ? "/" : uri.getPath();
        if (path.equals("/") || path.isEmpty()) active = 0;
        else if (path.contains("job-openings") || path.contains("latest-jobs") || path.contains("job-listing")) active = 1;
        else if (path.contains("upload-cv")) active = 2;
        else if (path.contains("contact-us")) active = 3;
        selectTab(active);
    }

    private void selectTab(int active) {
        for (int i = 0; i < navItems.size(); i++) {
            int color = i == active ? NAVY : MUTED;
            navIcons.get(i).setColorFilter(color);
            navLabels.get(i).setTextColor(color);
            navLabels.get(i).setTypeface(Typeface.DEFAULT, i == active ? Typeface.BOLD : Typeface.NORMAL);
            navItems.get(i).setBackgroundColor(i == active ? Color.rgb(245, 248, 250) : Color.WHITE);
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        Menu items = menu.getMenu();
        items.add(0, 1, 0, "Services");
        items.add(0, 2, 1, "Countries");
        items.add(0, 3, 2, "Assessment");
        items.add(0, 4, 3, "Download Center");
        items.add(0, 5, 4, "Share app link");
        items.add(0, 6, 5, "Open in browser");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: goTo(ROOT + "services/"); return true;
                case 2: goTo(ROOT + "countries/"); return true;
                case 3: goTo("https://assessment.gulfjobworks.com/"); return true;
                case 4: goTo(ROOT + "download/"); return true;
                case 5:
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, "Gulf Job Works: " + currentUrl);
                    startActivity(Intent.createChooser(share, "Share Gulf Job Works"));
                    return true;
                case 6: openExternal(Uri.parse(currentUrl)); return true;
                default: return false;
            }
        });
        menu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_REQUEST || pendingFiles == null) return;
        Uri[] selected = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                selected = new Uri[count];
                for (int i = 0; i < count; i++) selected[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) {
                selected = new Uri[]{data.getData()};
            }
        }
        pendingFiles.onReceiveValue(selected);
        pendingFiles = null;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (pendingFiles != null) pendingFiles.onReceiveValue(null);
        pageFrame.removeView(webView);
        webView.destroy();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
