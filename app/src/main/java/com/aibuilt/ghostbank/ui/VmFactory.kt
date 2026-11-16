package com.aibuilt.ghostbank.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Passes dependencies by hand instead of using a DI framework. */
fun <T : ViewModel> ghostFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
