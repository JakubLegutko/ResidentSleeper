package com.residentsleeper

import android.app.Application
import com.residentsleeper.util.LocaleHelper

class ResidentSleeperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleHelper.initLocale(this)
    }
}
