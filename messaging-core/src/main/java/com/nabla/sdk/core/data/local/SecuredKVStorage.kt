package com.nabla.sdk.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecuredKVStorage(context: Context) : SharedPreferences {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "nabla_kv_sec.sp", // This name needs to be in sync with the one of the backup_rules.xml of the app
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getAll(): MutableMap<String, *> {
        return sharedPreferences.all
    }

    override fun getString(key: String, defValue: String?): String? {
        return sharedPreferences.getString(key, defValue)
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String> {
        return sharedPreferences.getStringSet(key, defValues) as MutableSet<String>
    }

    override fun getInt(key: String, defValue: Int): Int {
        return sharedPreferences.getInt(key, defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return sharedPreferences.getFloat(key, defValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return sharedPreferences.edit()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        return sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        return sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
