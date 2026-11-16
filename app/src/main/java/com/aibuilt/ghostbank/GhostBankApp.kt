package com.aibuilt.ghostbank

import android.app.Application
import com.aibuilt.ghostbank.data.AppContainer

class GhostBankApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
