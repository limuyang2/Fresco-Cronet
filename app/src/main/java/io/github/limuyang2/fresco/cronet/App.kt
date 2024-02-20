package io.github.limuyang2.fresco.cronet

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.chromium.net.CronetEngine
import org.chromium.net.impl.NativeCronetEngineBuilderImpl
import java.io.File

/**
 * @author 李沐阳
 * @date 2024/2/20
 * @description
 */
class App: Application() {

    val cronetEngine by lazy {
        val httpCacheDir = File(externalCacheDir ?: cacheDir, "demoHttp")

        if (!httpCacheDir.exists()) {
            httpCacheDir.mkdir()
        }

        return@lazy CronetEngine.Builder(
            NativeCronetEngineBuilderImpl(this)
        )
            .setThreadPriority(-2)
            .setStoragePath(httpCacheDir.absolutePath)
            .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 1048576)
            .enableHttp2(true)
            .enableQuic(true)
            .enableBrotli(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        val networkFetcher = CronetNetworkFetcher(cronetEngine, Dispatchers.IO.asExecutor())

        val config = ImagePipelineConfig.Builder(this)
            .setNetworkFetcher(networkFetcher)
        .build()

        Fresco.initialize(this, config)
    }
}