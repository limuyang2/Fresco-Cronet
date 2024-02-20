package io.github.limuyang2.fresco.cronet

import android.os.SystemClock
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.BaseNetworkFetcher
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.producers.ProducerContext
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor


/**
 * @author LiMuYang
 * @date 2024/2/20
 * @description
 */
class CronetNetworkFetcher(
    private val cronetEngine: CronetEngine,
    private val callExecutor: Executor = ThreadExecutor.INSTANCE
) : BaseNetworkFetcher<CronetFetchState>() {

    override fun createFetchState(
        consumer: Consumer<EncodedImage>,
        producerContext: ProducerContext
    ): CronetFetchState = CronetFetchState(consumer, producerContext)

    override fun fetch(fetchState: CronetFetchState, callback: NetworkFetcher.Callback) {
        try {
            val urlRequest = cronetEngine.newUrlRequestBuilder(
                fetchState.uri.toString(),
                UrlRequestCallback(callback),
                callExecutor
            ).build()

            fetchState.context.addCallbacks(callbacks = object : BaseProducerContextCallbacks() {
                override fun onCancellationRequested() {
                    urlRequest.cancel()
                }
            })

            urlRequest.start()

        } catch (e: Exception) {
            callback.onFailure(e)
        }
    }

    override fun onFetchCompletion(fetchState: CronetFetchState, byteSize: Int) {
        fetchState.fetchCompleteTime = SystemClock.elapsedRealtime()
    }

    override fun getExtraMap(
        fetchState: CronetFetchState,
        byteSize: Int
    ): Map<String, String> = mapOf(
        QUEUE_TIME to (fetchState.responseTime - fetchState.submitTime).toString(),
        FETCH_TIME to (fetchState.fetchCompleteTime - fetchState.responseTime).toString(),
        TOTAL_TIME to (fetchState.fetchCompleteTime - fetchState.submitTime).toString(),
        IMAGE_SIZE to byteSize.toString()
    )

    private class UrlRequestCallback(private val callback: NetworkFetcher.Callback) :
        UrlRequest.Callback() {
        private var out: PipedOutputStream = PipedOutputStream()

        override fun onRedirectReceived(
            request: UrlRequest, info: UrlResponseInfo?, newLocationUrl: String?
        ) {
            request.followRedirect()
        }

        override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo?) {
            request.read(ByteBuffer.allocateDirect(32 * 1024))
        }

        override fun onReadCompleted(
            request: UrlRequest,
            info: UrlResponseInfo?,
            byteBuffer: ByteBuffer
        ) {
            byteBuffer.flip()

            try {
                if (byteBuffer.hasArray()) {
                    out.write(byteBuffer.array())
                }

            } catch (e: IOException) {
                callback.onFailure(e)
            }
            byteBuffer.clear()
            request.read(byteBuffer)
        }

        override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
            val contentLength: Int? = info.allHeaders["Content-Length"]?.lastOrNull()?.toIntOrNull()

            callback.onResponse(PipedInputStream(out), contentLength ?: -1)
        }

        override fun onFailed(
            request: UrlRequest?, info: UrlResponseInfo?, error: CronetException?
        ) {
            callback.onFailure(error)
        }
    }


    private enum class ThreadExecutor : Executor {
        INSTANCE;

        override fun execute(command: Runnable) {
            Thread(command).start()
        }
    }

    private companion object {
        private const val QUEUE_TIME = "queue_time"
        private const val FETCH_TIME = "fetch_time"
        private const val TOTAL_TIME = "total_time"
        private const val IMAGE_SIZE = "image_size"
    }
}