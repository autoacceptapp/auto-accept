package com.example

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Manages in-app auto updates by checking a public GitHub repository releases API,
 * downloading new APK releases using Android DownloadManager, and launching
 * the system package installer via FileProvider.
 */
object GitHubUpdateManager {

    private const val TAG = "GitHubUpdateManager"
    const val PREFS_KEY_GITHUB_OWNER = "key_github_owner"
    const val PREFS_KEY_GITHUB_REPO = "key_github_repo"

    // Default repository details
    const val DEFAULT_OWNER = "autoacceptapp"
    const val DEFAULT_REPO = "auto-accept"

    data class UpdateInfo(
        val tagName: String,
        val versionName: String,
        val releaseTitle: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val apkName: String,
        val htmlUrl: String
    )

    sealed class UpdateCheckResult {
        data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult()
        object UpToDate : UpdateCheckResult()
        data class Error(val message: String) : UpdateCheckResult()
    }

    fun getOwner(context: Context): String {
        val prefs = context.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREFS_KEY_GITHUB_OWNER, DEFAULT_OWNER)
        return if (saved.isNullOrBlank() || saved == "OWNER") DEFAULT_OWNER else saved
    }

    fun setOwner(context: Context, owner: String) {
        val prefs = context.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_KEY_GITHUB_OWNER, owner.trim()).apply()
    }

    fun getRepo(context: Context): String {
        val prefs = context.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREFS_KEY_GITHUB_REPO, DEFAULT_REPO)
        return if (saved.isNullOrBlank() || saved == "REPO") DEFAULT_REPO else saved
    }

    fun setRepo(context: Context, repo: String) {
        val prefs = context.getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_KEY_GITHUB_REPO, repo.trim()).apply()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = cm.activeNetworkInfo
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Queries the latest release from the GitHub Releases API.
     * Compares the release tag with the current BuildConfig.VERSION_NAME.
     */
    suspend fun checkForUpdates(
        context: Context,
        owner: String = getOwner(context),
        repo: String = getRepo(context)
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "Network unavailable. Skipping GitHub update check.")
            return@withContext UpdateCheckResult.Error("Network unavailable")
        }

        if (owner.isBlank() || repo.isBlank()) {
            Log.d(TAG, "GitHub repository not configured ($owner/$repo). Skipping update check.")
            return@withContext UpdateCheckResult.Error("GitHub repository is not configured (Current: $owner/$repo).")
        }

        var connection: HttpURLConnection? = null
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val url = URL(apiUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "RapidoAutoAccept-Updater")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.Error("Release not found for $owner/$repo (404)")
            } else if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("GitHub API returned HTTP $responseCode")
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val releaseJson = JSONObject(responseBody)

            val tagName = releaseJson.optString("tag_name", "")
            val releaseTitle = releaseJson.optString("name", tagName)
            val releaseNotes = releaseJson.optString("body", "No release notes provided.")
            val htmlUrl = releaseJson.optString("html_url", "")

            val remoteRunNumber = Regex("build-(\\d+)").find(tagName)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val currentRunNumber = BuildConfig.VERSION_CODE

            if (remoteRunNumber <= currentRunNumber) {
                Log.i(TAG, "App is up to date: currentRunNumber=$currentRunNumber, remoteRunNumber=$remoteRunNumber (tag=$tagName)")
                return@withContext UpdateCheckResult.UpToDate
            }

            val cleanRemoteVersion = tagName.removePrefix("v").removePrefix("V").trim()

            // Find APK asset in assets array
            var apkDownloadUrl: String? = null
            var apkFileName = "update-$cleanRemoteVersion.apk"

            val assetsArray = releaseJson.optJSONArray("assets")
            if (assetsArray != null && assetsArray.length() > 0) {
                for (i in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.getJSONObject(i)
                    val assetName = assetObj.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = assetObj.optString("browser_download_url", "")
                        apkFileName = assetName
                        break
                    }
                }
            }

            if (apkDownloadUrl.isNullOrBlank()) {
                return@withContext UpdateCheckResult.Error(
                    "New release '$tagName' found, but no .apk asset was uploaded to this release."
                )
            }

            val updateInfo = UpdateInfo(
                tagName = tagName,
                versionName = cleanRemoteVersion,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = apkDownloadUrl,
                apkName = apkFileName,
                htmlUrl = htmlUrl
            )

            return@withContext UpdateCheckResult.UpdateAvailable(updateInfo)
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "GitHub update check timed out (${e.message}). Skipping.")
            return@withContext UpdateCheckResult.Error("Connection timed out")
        } catch (e: UnknownHostException) {
            Log.w(TAG, "GitHub host unreachable (${e.message}). Skipping.")
            return@withContext UpdateCheckResult.Error("Host unreachable")
        } catch (e: ConnectException) {
            Log.w(TAG, "Failed to connect to GitHub (${e.message}). Skipping.")
            return@withContext UpdateCheckResult.Error("Connection failed")
        } catch (e: SSLException) {
            Log.w(TAG, "SSL error contacting GitHub (${e.message}). Skipping.")
            return@withContext UpdateCheckResult.Error("SSL error")
        } catch (e: IOException) {
            Log.w(TAG, "I/O error checking GitHub updates: ${e.message}")
            return@withContext UpdateCheckResult.Error("Network error")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check for updates: ${e.message}")
            return@withContext UpdateCheckResult.Error(e.localizedMessage ?: "Failed to check for updates")
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Starts downloading the APK using Android's DownloadManager and registers
     * a completion receiver to automatically prompt installation.
     */
    fun startDownloadAndInstall(
        context: Context,
        updateInfo: UpdateInfo,
        onDownloadStarted: (Long) -> Unit = {}
    ): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            Toast.makeText(context, "DownloadManager not available", Toast.LENGTH_SHORT).show()
            return -1L
        }

        // Clean up previously downloaded file if present
        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val destinationFile = File(destinationDir, updateInfo.apkName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl)).apply {
            setTitle("Updating to ${updateInfo.tagName}")
            setDescription("Downloading APK update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, updateInfo.apkName)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
        onDownloadStarted(downloadId)

        registerDownloadReceiver(context.applicationContext, downloadId, destinationFile)
        return downloadId
    }

    private fun registerDownloadReceiver(
        appContext: Context,
        targetDownloadId: Long,
        apkFile: File
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val receivedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (receivedId == targetDownloadId) {
                    try {
                        context?.unregisterReceiver(this)
                    } catch (e: Exception) {
                        Log.w(TAG, "Receiver unregister note: ${e.message}")
                    }
                    val targetContext = context ?: appContext
                    installApk(targetContext, apkFile)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    /**
     * Prompts the Android Package Installer using the secure FileProvider URI.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file does not exist at ${apkFile.absolutePath}")
            Toast.makeText(context, "Downloaded update file not found", Toast.LENGTH_SHORT).show()
            return
        }

        // On Android 8.0+ (API 26+), check if unknown sources install permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Please allow installation from this app in the next screen",
                    Toast.LENGTH_LONG
                ).show()
                val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(manageIntent)
            }
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
            Toast.makeText(context, "Could not launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
