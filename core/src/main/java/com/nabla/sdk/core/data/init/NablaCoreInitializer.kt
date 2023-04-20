package com.nabla.sdk.core.data.init

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.startup.Initializer
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.reporting.error.ErrorReportingInitializer

public class NablaCoreInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Configuration.defaultAppContext = context
        val metaData: Bundle? = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            ).metaData
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            ).metaData
        }
        val publicApiKey = metaData?.getString("com.nabla.sdk.PUBLIC_API_KEY")
        publicApiKey?.let { Configuration.defaultPublicApiKey = it }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        val dependencies = mutableListOf<Class<out Initializer<*>>>()
        try {
            dependencies.add(ErrorReportingInitializer::class.java)
        } catch (noClassDefFoundError: NoClassDefFoundError) {
            // reporting removed from runtime classpath
        }
        return dependencies.toList()
    }
}
