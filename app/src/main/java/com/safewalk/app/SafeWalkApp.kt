package com.safewalk.app

import android.app.Application

class SafeWalkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.init(this)
    }
}