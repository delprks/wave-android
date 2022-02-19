package com.delprks.wave.security

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi

object SettingsManager {
    private val WEB_DAV_ACCOUNT = "delprks.com_wave_webdav"

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun addAccount(context: Context, username: String, password: String) {
        val accountManager = AccountManager.get(context)
        val account = Account(username, WEB_DAV_ACCOUNT)

        getSystemAccounts(context).forEach { currentAccount ->
            accountManager.removeAccountExplicitly(currentAccount)
        }

        accountManager.addAccountExplicitly(account, password, null)
    }

    fun getAccount(context: Context): UserAccount? {
        var userAccount: UserAccount? = null

        val systemAccounts = getSystemAccounts(context)

        if (systemAccounts.isNotEmpty()) {
            val account = systemAccounts[0]
            val password = getUserPassword(account, context)

            userAccount = UserAccount(WEB_DAV_ACCOUNT, account.name.toString(), password)
        }

        return userAccount
    }

    fun getAuthMap(context: Context): Map<String, String> {
        val userAccount = getAccount(context)
        val auth = "Basic " + Base64.encodeToString("${userAccount?.id}:${userAccount?.password}".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)

        return mapOf(Pair("Authorization", auth))
    }

    private fun getSystemAccounts(context: Context): Array<Account> {
        val accountManager = AccountManager.get(context)

        return accountManager.getAccountsByType(WEB_DAV_ACCOUNT)
    }

    private fun getUserPassword(account: Account?, context: Context?): String {
        val accountManager = AccountManager.get(context)
        return accountManager.getPassword(account)
    }

}