package br.com.maridobh.profissional

import android.Manifest
import android.app.DownloadManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import br.com.maridobh.profissional.diagnostics.AppDiagnostics
import br.com.maridobh.profissional.location.PreciseLocationManager
import br.com.maridobh.profissional.mobile.MobileApiClient
import br.com.maridobh.profissional.push.PushTokenManager
import br.com.maridobh.profissional.sync.OfflineQueueManager
import br.com.maridobh.profissional.work.WorkSessionManager
import kotlin.math.max
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var errorView: LinearLayout
    private lateinit var statusPill: TextView
    private lateinit var splashOverlay: FrameLayout
    private var splashHidden = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var updatePromptShown = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private var pendingStartPreciseLocation = false
    private var safeTopPx = 0
    private var safeBottomPx = 0

    companion object {
        private const val REQ_FILE = 701
        private const val REQ_LOCATION = 702
        private const val REQ_NOTIFICATION = 703
        private const val REQ_PRECISE_LOCATION = 704
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.parseColor("#071A33")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        buildLayout()
        configureWebView()
        requestNotificationPermissionIfNeeded()
        PushTokenManager.init(this)
        MobileApiClient.syncDevice(this)
        MobileApiClient.flushPending(this)
        if (PreciseLocationManager.isActive(this) && PreciseLocationManager.hasPermission(this)) {
            PreciseLocationManager.start(this)
        }
        updateStatusPill()
        checkForAppUpdate()
        loadInitialUrl(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadInitialUrl(intent)
    }

    private fun buildLayout() {
        val root = FrameLayout(this)
        applySafeArea(root)

        webView = WebView(this).apply {
            alpha = 0f
            setBackgroundColor(Color.parseColor("#0B4EDB"))
        }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.visibility = View.GONE
        root.addView(progress, FrameLayout.LayoutParams(-1, dp(4), Gravity.TOP))

        statusPill = TextView(this).apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(10), dp(16), dp(10))
            alpha = 0.97f
            elevation = dp(10).toFloat()
            maxWidth = dp(330)
            setLineSpacing(dp(2).toFloat(), 1.0f)
            setOnClickListener { updateLocationNow() }
            setOnLongClickListener { showMobileDiagnostics(); true }
        }
        // Card nativo de atualização manual da localização. Fica na parte inferior
        // esquerda para não cobrir o menu hambúrguer nem botões do cabeçalho.
        val pillParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = dp(14)
            bottomMargin = dp(18)
            rightMargin = dp(14)
        }
        root.addView(statusPill, pillParams)

        errorView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F8FAFC"))
            visibility = View.GONE
            setPadding(dp(24), dp(24), dp(24), dp(24))

            val icon = ImageView(this@MainActivity).apply {
                setImageResource(android.R.drawable.stat_notify_error)
                alpha = 0.65f
            }
            addView(icon, LinearLayout.LayoutParams(dp(52), dp(52)))

            addView(TextView(this@MainActivity).apply {
                text = "Você está offline"
                textSize = 20f
                setTextColor(Color.parseColor("#071A33"))
                gravity = Gravity.CENTER
                setPadding(0, dp(18), 0, dp(8))
            })
            addView(TextView(this@MainActivity).apply {
                text = "O aplicativo vai tentar reconectar automaticamente. Você também pode tocar abaixo para atualizar."
                textSize = 15f
                setTextColor(Color.parseColor("#667085"))
                gravity = Gravity.CENTER
            })
            addView(Button(this@MainActivity).apply {
                text = "Tentar novamente"
                setOnClickListener {
                    errorView.visibility = View.GONE
                    performSmartSync(clearWebCache = false, showToast = false)
                    webView.reload()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(20)
            })
        }
        root.addView(errorView, FrameLayout.LayoutParams(-1, -1))

        splashOverlay = buildSplashOverlay()
        root.addView(splashOverlay, FrameLayout.LayoutParams(-1, -1))
        splashOverlay.post {
            splashOverlay.findViewWithTag<ImageView>("splash_logo")?.animate()?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.setDuration(450)?.start()
        }

        setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.MBH_DEBUG_MODE)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = userAgentString + " MaridoBHProfissional/${BuildConfig.VERSION_NAME} AndroidHybrid"
        }
        webView.addJavascriptInterface(MaridoBHBridge(this), "MaridoBHAndroid")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return handleExternalUrl(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progress.visibility = View.GONE
                injectMobileBridgeFlag()
                updateStatusPill()
                hideSplashWhenReady()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    hideSplash(force = true)
                    errorView.visibility = View.VISIBLE
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                progress.progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback

                val acceptTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }.orEmpty()
                val wantsImage = acceptTypes.isEmpty() || acceptTypes.any { it.contains("image", ignoreCase = true) || it == "*/*" }

                val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = when {
                        acceptTypes.size == 1 -> acceptTypes.first()
                        wantsImage -> "image/*"
                        else -> "*/*"
                    }
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE)
                }

                val intents = mutableListOf<Intent>()
                if (wantsImage) {
                    createCameraIntent()?.let { intents.add(it) }
                }

                val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                    putExtra(Intent.EXTRA_INTENT, contentIntent)
                    putExtra(Intent.EXTRA_TITLE, "Selecionar arquivo")
                    if (intents.isNotEmpty()) putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
                }

                return try {
                    startActivityForResult(chooser, REQ_FILE)
                    true
                } catch (e: ActivityNotFoundException) {
                    fileCallback = null
                    cameraImageUri = null
                    false
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                if (PreciseLocationManager.hasPermission(this@MainActivity)) {
                    callback?.invoke(origin, true, false)
                    // Quando o próprio painel usa navigator.geolocation para a localização precisa,
                    // sincronizamos também o estado nativo do app.
                    PreciseLocationManager.start(this@MainActivity)
                    updateStatusPill()
                    injectMobileBridgeFlag()
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    pendingStartPreciseLocation = true
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
                }
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    setDescription("Baixando arquivo do MaridoBH")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                }
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Download iniciado", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                openExternal(url)
            }
        }
    }

    private fun updateLocationNow() {
        if (!PreciseLocationManager.hasPermission(this)) {
            pendingStartPreciseLocation = true
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQ_PRECISE_LOCATION)
            return
        }
        val ok = PreciseLocationManager.start(this)
        performSmartSync(clearWebCache = true, showToast = false)
        Toast.makeText(this, if (ok) "Localização e app sincronizados" else "Não foi possível atualizar a localização", Toast.LENGTH_SHORT).show()
        updateStatusPill()
        injectMobileBridgeFlag()
    }

    fun requestPreciseLocationFromBridge() {
        if (PreciseLocationManager.hasPermission(this)) {
            val ok = PreciseLocationManager.start(this)
            Toast.makeText(this, if (ok) "Localização precisa ativada" else "Não foi possível ativar a localização", Toast.LENGTH_SHORT).show()
            updateStatusPill()
            injectMobileBridgeFlag()
        } else {
            pendingStartPreciseLocation = true
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQ_PRECISE_LOCATION)
        }
    }

    fun stopPreciseLocationFromBridge() {
        PreciseLocationManager.stop(this)
        Toast.makeText(this, "Localização precisa desativada", Toast.LENGTH_SHORT).show()
        updateStatusPill()
        injectMobileBridgeFlag()
    }

    fun startWorkSessionFromBridge() {
        WorkSessionManager.start(this)
        if (!PreciseLocationManager.isActive(this)) requestPreciseLocationFromBridge()
        Toast.makeText(this, "Jornada iniciada", Toast.LENGTH_SHORT).show()
        updateStatusPill()
        injectMobileBridgeFlag()
    }

    fun stopWorkSessionFromBridge() {
        WorkSessionManager.stop(this)
        Toast.makeText(this, "Jornada encerrada", Toast.LENGTH_SHORT).show()
        updateStatusPill()
        injectMobileBridgeFlag()
    }

    private fun loadInitialUrl(intent: Intent?) {
        val url = AppConfig.notificationIntentToUrl(intent)
        webView.loadUrl(url)
    }

    private fun handleExternalUrl(url: String): Boolean {
        if (url.startsWith(AppConfig.baseUrl) || url.startsWith("about:")) return false

        val lower = url.lowercase(Locale.ROOT)
        val isRoute = lower.startsWith("geo:") ||
            lower.startsWith("google.navigation:") ||
            lower.startsWith("waze:") ||
            lower.contains("maps.google.") ||
            lower.contains("google.com/maps") ||
            lower.contains("/maps/dir")

        if (isRoute ||
            lower.startsWith("tel:") ||
            lower.startsWith("mailto:") ||
            lower.startsWith("whatsapp:") ||
            lower.startsWith("https://wa.me") ||
            lower.startsWith("intent:")) {
            openExternal(url)
            return true
        }

        // Links HTTP fora do domínio do MaridoBH devem abrir fora do WebView.
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            openExternal(url)
            return true
        }

        return false
    }

    private fun openExternal(url: String) {
        try {
            val intent = if (url.startsWith("intent:")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                Toast.makeText(this, "Não foi possível abrir este link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun injectMobileBridgeFlag() {
        val payload = org.json.JSONObject().apply {
            put("device", org.json.JSONObject(PushTokenManager.devicePayload(this@MainActivity)))
            put("location", PreciseLocationManager.statusJson(this@MainActivity))
            put("work_session", WorkSessionManager.statusJson(this@MainActivity))
            put("diagnostics", AppDiagnostics.diagnosticsJson(this@MainActivity))
            put("sync", OfflineQueueManager.statusJson(this@MainActivity))
        }.toString().replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
        val endpoint = (AppConfig.baseUrl + BuildConfig.MOBILE_DEVICE_ENDPOINT).replace("'", "\\'")
        val js = """
            window.MBH_APP = window.MBH_APP || {};
            window.MBH_APP.platform = 'android';
            window.MBH_APP.version = '${BuildConfig.VERSION_NAME}';
            window.MBH_APP.channel = '${AppConfig.APP_CHANNEL}';
            document.documentElement.classList.add('mbh-android-app');
            document.body && document.body.classList.add('mbh-android-app');
            (function(){
                try {
                    var safeTop = ${safeTopPx};
                    var safeBottom = ${safeBottomPx};
                    var style = document.getElementById('mbh-android-safe-area-style');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'mbh-android-safe-area-style';
                        document.head.appendChild(style);
                    }
                    style.textContent = ':root{--mbh-safe-top:'+safeTop+'px;--mbh-safe-bottom:'+safeBottom+'px;}' +
                        'html.mbh-android-app,body.mbh-android-app{min-height:100%;}' +
                        'body.mbh-android-app{padding-bottom:calc(var(--mbh-safe-bottom) + 10px)!important;}' +
                        '.mbh-android-app input:focus,.mbh-android-app textarea:focus{scroll-margin-bottom:calc(var(--mbh-safe-bottom) + 150px)!important;}' +
                        '.mbh-chat-composer,.mbh-chat-inputbar,.mbh-chat-input-bar,.mbh-chat-footer,.mbh-chat-form,.mbh-atendimento-composer,.mbh-atendimento-footer,.mbh-central-chat-footer,[data-mbh-chat-composer]{padding-bottom:calc(var(--mbh-safe-bottom) + 8px)!important;bottom:0!important;}' +
                        '.mbh-chat-modal,.mbh-atendimento-modal,.mbh-central-atendimento-modal{max-height:calc(100vh - var(--mbh-safe-top) - var(--mbh-safe-bottom))!important;}' +
                        '.mbh-app-version-native{display:block;text-align:center;color:#667085;font-size:12px;padding:12px 8px 18px;}';
                    if (!window.__mbhAndroidFocusFix) {
                        window.__mbhAndroidFocusFix = true;
                        document.addEventListener('focusin', function(ev){
                            var el = ev.target;
                            if (!el) return;
                            var tag = (el.tagName || '').toLowerCase();
                            if (tag === 'input' || tag === 'textarea' || el.isContentEditable) {
                                setTimeout(function(){
                                    try { el.scrollIntoView({block:'center', inline:'nearest', behavior:'smooth'}); } catch(e) { try { el.scrollIntoView(false); } catch(_){} }
                                }, 320);
                            }
                        }, true);
                    }

                    if (!window.__mbhAndroidVersionFooter) {
                        window.__mbhAndroidVersionFooter = true;
                        setTimeout(function(){
                            try {
                                var existing = document.getElementById('mbh-app-version-native');
                                if (!existing && document.body) {
                                    var footer = document.createElement('div');
                                    footer.id = 'mbh-app-version-native';
                                    footer.className = 'mbh-app-version-native';
                                    footer.textContent = 'MaridoBH Profissional • App v${BuildConfig.VERSION_NAME}';
                                    document.body.appendChild(footer);
                                }
                            } catch(e) {}
                        }, 700);
                    }
                    if (!window.__mbhAndroidSyncHook) {
                        window.__mbhAndroidSyncHook = true;
                        document.addEventListener('click', function(ev){
                            try {
                                var el = ev.target && ev.target.closest ? ev.target.closest('button,a,[role="button"],input[type="button"],input[type="submit"]') : null;
                                if (!el) return;
                                var text = ((el.innerText || el.value || el.getAttribute('aria-label') || '') + '').toLowerCase();
                                if (text.indexOf('sincron') >= 0 || text.indexOf('atualizar') >= 0) {
                                    if (window.MaridoBHAndroid && window.MaridoBHAndroid.smartSync) window.MaridoBHAndroid.smartSync();
                                }
                            } catch(e) {}
                        }, true);
                    }
                } catch(e) { console.log('MBH safe-area css error', e); }

                try {
                    var state = JSON.parse('$payload');
                    window.MBH_APP.device = state.device;
                    window.MBH_APP.location = state.location;
                    window.MBH_APP.work_session = state.work_session;
                    window.MBH_APP.diagnostics = state.diagnostics;
                    window.MBH_APP.sync = state.sync;
                    window.dispatchEvent(new CustomEvent('mbhAndroidState', {detail: state}));
                    if (state.device && state.device.push_token && window.fetch) {
                        if (window.MBHServicos && window.MBHServicos.ajaxUrl && window.MBHServicos.nonce) {
                            var fd = new FormData();
                            fd.append('action', 'mbh_mobile_register_device');
                            fd.append('nonce', window.MBHServicos.nonce);
                            Object.keys(state.device).forEach(function(k){
                                var v = state.device[k];
                                fd.append(k, typeof v === 'object' ? JSON.stringify(v) : v);
                            });
                            fetch(window.MBHServicos.ajaxUrl, { method:'POST', credentials:'include', body: fd })
                                .catch(function(e){ console.log('MBH ajax device sync pending', e); });
                        } else {
                            fetch('$endpoint', {
                                method: 'POST',
                                credentials: 'include',
                                headers: {'Content-Type':'application/json'},
                                body: JSON.stringify(state.device)
                            }).catch(function(e){ console.log('MBH device sync pending', e); });
                        }
                    }
                } catch(e) { console.log('MBH bridge inject error', e); }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }


    fun performSmartSyncFromBridge() {
        runOnUiThread { performSmartSync(clearWebCache = true, showToast = true) }
    }

    private fun performSmartSync(clearWebCache: Boolean, showToast: Boolean) {
        try {
            if (clearWebCache) {
                webView.clearCache(false)
            }
        } catch (_: Exception) {}
        PushTokenManager.markTokenPending(this)
        PushTokenManager.init(this)
        MobileApiClient.syncDevice(this)
        MobileApiClient.flushPending(this) { sent ->
            runOnUiThread {
                updateStatusPill()
                injectMobileBridgeFlag()
                if (showToast) Toast.makeText(this, "Sincronização concluída: $sent item(ns)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildSplashOverlay(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B4EDB"))
            isClickable = true
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val logo = ImageView(this).apply {
            tag = "splash_logo"
            setImageResource(resources.getIdentifier("splash_logo", "drawable", packageName))
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = 0.94f
            scaleY = 0.94f
        }
        box.addView(logo, LinearLayout.LayoutParams(dp(265), dp(265)))
        box.addView(TextView(this).apply {
            text = "MaridoBH\nPROFISSIONAL"
            textSize = 28f
            setTextColor(Color.WHITE)
            alpha = 0.86f
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })
        box.addView(TextView(this).apply {
            text = "App v${BuildConfig.VERSION_NAME}"
            textSize = 12f
            setTextColor(Color.WHITE)
            alpha = 0.65f
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        })
        overlay.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        return overlay
    }

    private fun hideSplashWhenReady() {
        if (splashHidden) return
        webView.evaluateJavascript("document.readyState") { state ->
            if (state.contains("interactive") || state.contains("complete")) {
                mainHandler.postDelayed({ hideSplash() }, 180)
            } else {
                mainHandler.postDelayed({ hideSplashWhenReady() }, 120)
            }
        }
    }

    private fun hideSplash(force: Boolean = false) {
        if (splashHidden && !force) return
        splashHidden = true
        if (::webView.isInitialized) {
            webView.animate().alpha(1f).setDuration(250).start()
        }
        if (::splashOverlay.isInitialized && splashOverlay.visibility == View.VISIBLE) {
            splashOverlay.animate()
                .alpha(0f)
                .setDuration(if (force) 120 else 350)
                .withEndAction {
                    splashOverlay.visibility = View.GONE
                    revealStatusPillAfterSplash()
                }
                .start()
        } else {
            revealStatusPillAfterSplash()
        }
    }

    private fun revealStatusPillAfterSplash() {
        if (!::statusPill.isInitialized) return
        updateStatusPill()
        statusPill.visibility = View.VISIBLE
        statusPill.alpha = 0f
        statusPill.animate().alpha(0.97f).setDuration(220).start()
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) { true }
    }

    private fun checkForAppUpdate() {
        if (updatePromptShown) return
        Thread {
            try {
                if (!isOnline()) return@Thread
                val versionUrl = AppConfig.baseUrl + BuildConfig.APP_VERSION_ENDPOINT
                val conn = (URL(versionUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    requestMethod = "GET"
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(body)
                val remoteCode = json.optInt("versionCode", BuildConfig.VERSION_CODE)
                val remoteName = json.optString("versionName", "")
                val notes = json.optString("notes", "")
                if (remoteCode > BuildConfig.VERSION_CODE) {
                    runOnUiThread { showUpdateAvailable(remoteName, notes) }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun showUpdateAvailable(versionName: String, notes: String) {
        if (updatePromptShown || isFinishing) return
        updatePromptShown = true
        AlertDialog.Builder(this)
            .setTitle("Atualização disponível")
            .setMessage("Existe uma nova versão do MaridoBH Profissional${if (versionName.isNotBlank()) " ($versionName)" else ""}.\n\n$notes")
            .setPositiveButton("Atualizar") { _, _ -> openAppUpdatePage() }
            .setNegativeButton("Agora não", null)
            .show()
    }


    private fun openAppUpdatePage() {
        val updateUrl = AppConfig.baseUrl + "/app-profissional/"
        try {
            openExternal(updateUrl)
        } catch (_: Exception) {
            try {
                openExternal(AppConfig.baseUrl)
            } catch (_: Exception) {
                Toast.makeText(this, "Não foi possível abrir a página de atualização", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            val result = when {
                resultCode != RESULT_OK -> null
                data?.clipData != null -> {
                    val clip = data.clipData!!
                    Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                cameraImageUri != null -> arrayOf(cameraImageUri!!)
                else -> null
            }
            fileCallback?.onReceiveValue(result)
            fileCallback = null
            cameraImageUri = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            val granted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
            if (granted && pendingStartPreciseLocation) {
                PreciseLocationManager.start(this)
            }
            pendingStartPreciseLocation = false
            pendingGeoCallback = null
            pendingGeoOrigin = null
            updateStatusPill()
            injectMobileBridgeFlag()
        }
        if (requestCode == REQ_PRECISE_LOCATION) {
            val granted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (granted && pendingStartPreciseLocation) PreciseLocationManager.start(this)
            pendingStartPreciseLocation = false
            updateStatusPill()
            injectMobileBridgeFlag()
        }
    }

    override fun onResume() {
        super.onResume()
        MobileApiClient.flushPending(this)
        if (isOnline() && ::errorView.isInitialized && errorView.visibility == View.VISIBLE) {
            errorView.visibility = View.GONE
            webView.reload()
        }
        updateStatusPill()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun updateStatusPill() {
        if (!::statusPill.isInitialized) return
        val keepHiddenUntilSplashEnds = ::splashOverlay.isInitialized && splashOverlay.visibility == View.VISIBLE && !splashHidden
        if (keepHiddenUntilSplashEnds) {
            statusPill.visibility = View.GONE
        }
        val precise = PreciseLocationManager.isActive(this)
        val pending = OfflineQueueManager.pendingCount(this)
        val overall = AppDiagnostics.diagnosticsJson(this).optString("overall", "ok")
        statusPill.text = when {
            overall == "critical" -> "⚠️  Verificar aplicativo\nToque para tentar sincronizar"
            pending > 0 -> "🔄  Atualizar localização\n$pending pendência(s) offline"
            precise -> "📍  Atualizar localização\nGPS preciso ativo • toque para sincronizar"
            else -> "🎯  Atualizar localização\nSincronize sua posição para serviços próximos"
        }
        val color = when {
            overall == "critical" -> "#DC2626"
            pending > 0 -> "#F59E0B"
            precise -> "#0B84FF"
            else -> "#0B4EDB"
        }
        applyRoundedPillBackground(statusPill, color)
    }

    private fun applyRoundedPillBackground(view: TextView, color: String) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(22).toFloat()
            setColor(Color.parseColor(color))
        }
        view.background = bg
    }

    private fun showMobileDiagnostics() {
        val message = AppDiagnostics.humanSummary(this) + "\n\n" +
            "Pendências offline: ${OfflineQueueManager.pendingCount(this)}\n" +
            "Versão: ${BuildConfig.VERSION_NAME}"
        AlertDialog.Builder(this)
            .setTitle("Diagnóstico do aplicativo")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("ATUALIZAR LOCALIZAÇÃO") { _, _ ->
                updateLocationNow()
                performSmartSync(clearWebCache = true, showToast = true)
            }
            .setNeutralButton("CONFIGURAÇÕES") { _, _ -> openAppSettings() }
            .show()
    }

    private fun createCameraIntent(): Intent? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            val imageFile = File.createTempFile("MBH_${timeStamp}_", ".jpg", cacheDir)
            cameraImageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        } catch (_: Exception) {
            cameraImageUri = null
            null
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun applySafeArea(root: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                val newBottom = max(systemBars.bottom, ime.bottom)
                safeTopPx = systemBars.top
                safeBottomPx = newBottom
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, newBottom)
                if (::webView.isInitialized) {
                    injectSafeAreaOnly()
                }
                insets
            }
            ViewCompat.requestApplyInsets(root)
        } else {
            safeTopPx = getStatusBarHeight()
            safeBottomPx = getNavigationBarHeight()
            root.setPadding(0, safeTopPx, 0, safeBottomPx)
        }
    }

    private fun injectSafeAreaOnly() {
        if (!::webView.isInitialized) return
        val js = """
            (function(){
                try {
                    var safeTop = ${safeTopPx};
                    var safeBottom = ${safeBottomPx};
                    var style = document.getElementById('mbh-android-safe-area-style');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'mbh-android-safe-area-style';
                        document.head.appendChild(style);
                    }
                    style.textContent = ':root{--mbh-safe-top:'+safeTop+'px;--mbh-safe-bottom:'+safeBottom+'px;}' +
                        'html.mbh-android-app,body.mbh-android-app{min-height:100%;}' +
                        'body.mbh-android-app{padding-bottom:calc(var(--mbh-safe-bottom) + 10px)!important;}' +
                        '.mbh-android-app input:focus,.mbh-android-app textarea:focus{scroll-margin-bottom:calc(var(--mbh-safe-bottom) + 150px)!important;}' +
                        '.mbh-chat-composer,.mbh-chat-inputbar,.mbh-chat-input-bar,.mbh-chat-footer,.mbh-chat-form,.mbh-atendimento-composer,.mbh-atendimento-footer,.mbh-central-chat-footer,[data-mbh-chat-composer]{padding-bottom:calc(var(--mbh-safe-bottom) + 8px)!important;bottom:0!important;}' +
                        '.mbh-chat-modal,.mbh-atendimento-modal,.mbh-central-atendimento-modal{max-height:calc(100vh - var(--mbh-safe-top) - var(--mbh-safe-bottom))!important;}' +
                        '.mbh-app-version-native{display:block;text-align:center;color:#667085;font-size:12px;padding:12px 8px 18px;}';
                    if (!window.__mbhAndroidFocusFix) {
                        window.__mbhAndroidFocusFix = true;
                        document.addEventListener('focusin', function(ev){
                            var el = ev.target;
                            if (!el) return;
                            var tag = (el.tagName || '').toLowerCase();
                            if (tag === 'input' || tag === 'textarea' || el.isContentEditable) {
                                setTimeout(function(){
                                    try { el.scrollIntoView({block:'center', inline:'nearest', behavior:'smooth'}); } catch(e) { try { el.scrollIntoView(false); } catch(_){} }
                                }, 320);
                            }
                        }, true);
                    }

                    if (!window.__mbhAndroidVersionFooter) {
                        window.__mbhAndroidVersionFooter = true;
                        setTimeout(function(){
                            try {
                                var existing = document.getElementById('mbh-app-version-native');
                                if (!existing && document.body) {
                                    var footer = document.createElement('div');
                                    footer.id = 'mbh-app-version-native';
                                    footer.className = 'mbh-app-version-native';
                                    footer.textContent = 'MaridoBH Profissional • App v${BuildConfig.VERSION_NAME}';
                                    document.body.appendChild(footer);
                                }
                            } catch(e) {}
                        }, 700);
                    }
                    if (!window.__mbhAndroidSyncHook) {
                        window.__mbhAndroidSyncHook = true;
                        document.addEventListener('click', function(ev){
                            try {
                                var el = ev.target && ev.target.closest ? ev.target.closest('button,a,[role="button"],input[type="button"],input[type="submit"]') : null;
                                if (!el) return;
                                var text = ((el.innerText || el.value || el.getAttribute('aria-label') || '') + '').toLowerCase();
                                if (text.indexOf('sincron') >= 0 || text.indexOf('atualizar') >= 0) {
                                    if (window.MaridoBHAndroid && window.MaridoBHAndroid.smartSync) window.MaridoBHAndroid.smartSync();
                                }
                            } catch(e) {}
                        }, true);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
