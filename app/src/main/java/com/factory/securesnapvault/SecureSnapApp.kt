package com.factory.securesnapvault

import android.app.Application

class SecureSnapApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val vaultDir = filesDir.resolve("vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
    }
}
