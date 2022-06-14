package com.delprks.wave.services

import android.app.Activity
import com.delprks.wave.security.SettingsManager
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebDavDownloader {
    companion object {
        fun download(activity: Activity, source: String, destination: String): Boolean {
            val server: Sardine = OkHttpSardine(unSafeOkHttpClient().build())
            val userAccount = SettingsManager.getAccount(activity.applicationContext!!)

            server.setCredentials(userAccount?.id, userAccount?.password)

            try {
                val fileStream = server.get(source)
                val output: OutputStream = FileOutputStream(destination)
                val data = ByteArray(1024)
                var total: Long = 0

                while (true) {
                    val count = fileStream!!.read(data)
                    if (count == -1) break
                    total += count

                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                fileStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

            return true
        }

        fun unSafeOkHttpClient(): OkHttpClient.Builder {
            val okHttpClient = OkHttpClient.Builder()
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                        try {
                            chain!![0].checkValidity()
                        } catch (e: Exception) {
                            throw CertificateException("Certificate not valid or trusted.")
                        }
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                // Create an ssl socket factory with our all-trusting manager
                val sslSocketFactory = sslContext.socketFactory
                if (trustAllCerts.isNotEmpty() && trustAllCerts.first() is X509TrustManager) {
                    okHttpClient.sslSocketFactory(
                        sslSocketFactory,
                        trustAllCerts.first() as X509TrustManager
                    )
                }

                return okHttpClient
            } catch (e: Exception) {
                return okHttpClient
            }
        }
    }
}
