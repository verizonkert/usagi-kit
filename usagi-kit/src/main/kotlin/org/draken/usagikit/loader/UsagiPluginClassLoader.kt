package org.draken.usagikit.loader

import android.os.Build
import dalvik.system.BaseDexClassLoader
import dalvik.system.DelegateLastClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import java.io.File
import java.lang.reflect.Array
import java.net.URL
import java.util.Collections
import java.util.Enumeration

/**
 * ClassLoader specifically for Usagi dynamic plugins (.jar containing dex).
 */
class UsagiPluginClassLoader(
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    parent: ClassLoader,
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    private val bootClassLoader: ClassLoader? = Any::class.java.classLoader

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        findLoadedClass(name)?.let { return it }

        if (bootClassLoader != null) {
            try {
                return bootClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {}
        }

        // Try plugin dex first
        val fromPlugin = try {
            return findClass(name)
        } catch (e: ClassNotFoundException) {
            e
        }

        // Fallback to parent
        return try {
            parent.loadClass(name)
        } catch (_: ClassNotFoundException) {
            throw fromPlugin
        }
    }

    override fun getResource(name: String?): URL? {
        return bootClassLoader?.getResource(name)
            ?: findResource(name)
            ?: parent?.getResource(name)
    }

    override fun getResources(name: String?): Enumeration<URL> {
        val resources = buildList {
            bootClassLoader?.getResources(name)?.let { addAll(it.toList()) }
            findResources(name)?.let { addAll(it.toList()) }
            parent?.getResources(name)?.let { addAll(it.toList()) }
        }
        return Collections.enumeration(resources)
    }
}
