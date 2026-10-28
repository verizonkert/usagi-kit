package org.draken.usagikit.tester

import android.app.Application
import org.draken.usagikit.UsagiKit

class TesterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UsagiKit.initialize(this)
    }
}
