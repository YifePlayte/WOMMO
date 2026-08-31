package com.yifeplayte.wommo

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

class App : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        @Volatile
        var instance: App? = null
            private set

        @Volatile
        var mService: XposedService? = null
            private set
        private val serviceStateListeners = CopyOnWriteArraySet<ServiceStateListener>()

        fun addServiceStateListener(listener: ServiceStateListener, notifyImmediately: Boolean) {
            serviceStateListeners.add(listener)
            if (notifyImmediately) {
                listener.onServiceStateChanged(mService)
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            serviceStateListeners.remove(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        XposedServiceHelper.registerListener(this)
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    override fun onServiceBind(service: XposedService) {
        mService = service
        for (listener in serviceStateListeners) {
            listener.onServiceStateChanged(service)
        }
    }

    override fun onServiceDied(service: XposedService) {
        mService = null
        for (listener in serviceStateListeners) {
            listener.onServiceStateChanged(null)
        }
    }
}
