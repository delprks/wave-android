package com.delprks.wave.services

import androidx.fragment.app.FragmentActivity
import com.delprks.wave.security.SettingsManager
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebDavResourceRetriever {
    companion object {
        fun retrieve(activity: FragmentActivity, path: String): List<DavResource> {
            val server: Sardine = OkHttpSardine(unSafeOkHttpClient().build())
            val userAccount = SettingsManager.getAccount(activity.applicationContext!!)

            server.setCredentials(userAccount?.id, userAccount?.password)
            var resources: List<DavResource> = ArrayList()

            try {
                resources = server.list(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return resources
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

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
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
