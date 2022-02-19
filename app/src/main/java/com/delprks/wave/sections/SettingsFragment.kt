package com.delprks.wave.sections

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import com.delprks.wave.App
import com.delprks.wave.dao.RemoteSourceType
import com.delprks.wave.domain.RemoteSettings
import com.delprks.wave.security.SettingsManager
import com.delprks.wave.security.UserAccount
import com.delprks.wave.services.SettingsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wave.R

class SettingsFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onResume() {
        super.onResume()

        val account: UserAccount? = SettingsManager.getAccount(activity?.applicationContext!!)

        val settingsNameTextField = activity?.findViewById<EditText>(R.id.settings_webdav_name_value)
        val hostTextField = activity?.findViewById<EditText>(R.id.settings_webdav_host_value)
        val mediaPathTextField = activity?.findViewById<EditText>(R.id.settings_webdav_media_path_value)
        val usernameTextField = activity?.findViewById<EditText>(R.id.settings_username_value)
        val passwordTextField = activity?.findViewById<EditText>(R.id.settings_password_value)
        val saveButton = activity?.findViewById<Button>(R.id.settings_credentials_save_btn)

        CoroutineScope(Dispatchers.Main).launch {
            val currentSettings = SettingsService.getWebDavRemoteSettings(App.getDB())

            currentSettings?.let {
                settingsNameTextField?.setText(it.name)
                hostTextField?.setText(it.host)
                mediaPathTextField?.setText(it.mediaPath)
            }
        }

        account.let { currentAccount ->
            usernameTextField?.setText(currentAccount?.id)
            passwordTextField?.setText(currentAccount?.password)
        }

        saveButton?.setOnClickListener {
            val name = settingsNameTextField?.text.toString()
            val host = hostTextField?.text.toString()
            val mediaPath = mediaPathTextField?.text.toString()
            val username = usernameTextField?.text.toString()
            val password = passwordTextField?.text.toString()

            CoroutineScope(Dispatchers.Main).launch {
                val updatedSettings = RemoteSettings(
                    SettingsService.WEB_DAV_ID,
                    RemoteSourceType.WEB_DAV,
                    name,
                    host,
                    mediaPath
                )

                SettingsService.addWebDavRemoteSettings(App.getDB(), updatedSettings)
                SettingsManager.addAccount(activity?.applicationContext!!, username, password)

                Toast.makeText(activity, "Saved successfully", Toast.LENGTH_SHORT).show()

                // hide keyboard
                val imm: InputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)

                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }
}
