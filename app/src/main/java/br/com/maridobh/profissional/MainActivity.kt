package br.com.maridobh.profissional

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.maridobh.profissional.diagnostics.AppDiagnostics
import br.com.maridobh.profissional.location.PreciseLocationManager
import br.com.maridobh.profissional.mobile.MobileApiClient
import br.com.maridobh.profissional.push.PushTokenManager
import br.com.maridobh.profissional.sync.OfflineQueueManager
import br.com.maridobh.profissional.work.WorkSessionManager

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var errorView: LinearLayout
    private lateinit var statusPill: TextView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private var pendingStartPreciseLocation = false

    companion object {
        private const val REQ_FILE = 701
        private const val REQ_LOCATION = 702
        private const val REQ_NOTIFICATION = 703
        private const val REQ_PRECISE_LOCATION = 704
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#071A33")
        window.navigationBarColor = Color.parseColor("#071A33")
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
        loadInitialUrl(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        loadInitialUrl(intent)
    }

    private fun buildLayout() {
        val root = FrameLayout(this)

        webView = WebView(this)
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        root.addView(progress, FrameLayout.LayoutParams(-1, dp(4), Gravity.TOP))

        statusPill = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(7), dp(12), dp(7))
            setBackgroundColor(Color.parseColor("#2563EB"))
            alpha = 0.94f
            setOnClickListener { showMobileDiagnostics() }
        }
        val pillParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(12)
            rightMargin = dp(12)
        }
        root.addView(statusPill, pillParams)

        errorView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            setPadding(dp(24), dp(24), dp(24), dp(24))

            val icon = ImageView(this@MainActivity).apply {
                setImageResource(android.R.drawable.stat_notify_error)
                alpha = 0.65f
            }
            addView(icon, LinearLayout.LayoutParams(dp(52), dp(52)))

            addView(TextView(this@MainActivity).apply {
                text = "Não foi possível carregar o MaridoBH"
                textSize = 20f
                setTextColor(Color.parseColor("#071A33"))
                gravity = Gravity.CENTER
                setPadding(0, dp(18), 0, dp(8))
            })
            addView(TextView(this@MainActivity).apply {
                text = "Verifique sua internet e tente novamente."
                textSize = 15f
                setTextColor(Color.parseColor("#667085"))
                gravity = Gravity.CENTER
            })
            addView(Button(this@MainActivity).apply {
                text = "Tentar novamente"
                setOnClickListener {
                    errorView.visibility = View.GONE
                    webView.reload()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(20)
            })
        }
        root.addView(errorView, FrameLayout.LayoutParams(-1, -1))
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
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) errorView.visibility = View.VISIBLE
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
                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try {
                    startActivityForResult(intent, REQ_FILE)
                    true
                } catch (e: ActivityNotFoundException) {
                    fileCallback = null
                    false
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                if (PreciseLocationManager.hasPermission(this@MainActivity)) {
                    callback?.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
                }
            }
        }
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
        val url = AppConfig.deepLinkToUrl(intent?.data)
        webView.loadUrl(url)
    }

    private fun handleExternalUrl(url: String): Boolean {
        if (url.startsWith(AppConfig.baseUrl) || url.startsWith("about:")) return false
        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.startsWith("https://wa.me")) {
            return try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            } catch (_: Exception) { true }
        }
        return false
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
            (function(){
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            val result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            fileCallback?.onReceiveValue(result)
            fileCallback = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            val granted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
            pendingGeoCallback = null
            pendingGeoOrigin = null
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
        updateStatusPill()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun updateStatusPill() {
        val jornada = WorkSessionManager.isActive(this)
        val precise = PreciseLocationManager.isActive(this)
        val pending = OfflineQueueManager.pendingCount(this)
        val overall = AppDiagnostics.diagnosticsJson(this).optString("overall", "ok")
        statusPill.text = when {
            pending > 0 -> "🟡 Sincronizar $pending"
            overall == "critical" -> "🔴 Diagnóstico"
            jornada && precise -> "🟢 Jornada • GPS preciso"
            precise -> "📍 GPS preciso"
            jornada -> "🟡 Jornada • GPS aproximado"
            else -> "📡 Radar aproximado"
        }
        val color = when {
            overall == "critical" -> "#DC2626"
            pending > 0 || jornada && !precise -> "#F59E0B"
            precise || jornada -> "#16A34A"
            else -> "#2563EB"
        }
        statusPill.setBackgroundColor(Color.parseColor(color))
    }

    private fun showMobileDiagnostics() {
        val message = AppDiagnostics.humanSummary(this) + "\n\n" +
            "Pendências offline: ${OfflineQueueManager.pendingCount(this)}\n" +
            "Versão: ${BuildConfig.VERSION_NAME}"
        AlertDialog.Builder(this)
            .setTitle("Diagnóstico do aplicativo")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Sincronizar agora") { _, _ ->
                MobileApiClient.flushPending(this) { sent ->
                    runOnUiThread {
                        Toast.makeText(this, "Sincronização enviada: $sent item(ns)", Toast.LENGTH_SHORT).show()
                        updateStatusPill()
                        injectMobileBridgeFlag()
                    }
                }
            }
            .setNeutralButton("Configurações") { _, _ -> openAppSettings() }
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
