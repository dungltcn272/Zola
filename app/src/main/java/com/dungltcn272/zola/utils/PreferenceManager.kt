package com.dungltcn272.zola.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharePreference: SharedPreferences

    init {
        sharePreference =
            context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = sharePreference.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(key: String): Boolean {
        return sharePreference.getBoolean(key, false)
    }

    fun putString(key: String, value: String) {
        val editor = sharePreference.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(key: String): String? {
        return sharePreference.getString(key, null)
    }

    fun clear(){
        val editor = sharePreference.edit()
        editor.clear()
        editor.apply()
    }
}