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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.concurrent.Executor


/**
 * @author LiMuYang
 * @date 2024/2/20
 * @description
 */
class CronetNetworkFetcher(
    private val cronetEngine: CronetEngine,
    /**
     * Thread management
     */
    private val callExecutor: Executor = ThreadExecutor.INSTANCE,
    /**
     * UrlRequest.Builder config
     */
    private val config: (UrlRequest.Builder.() -> Unit)? = null
) : BaseNetworkFetcher<CronetFetchState>() {

    override fun createFetchState(
        consumer: Consumer<EncodedImage>,
        producerContext: ProducerContext
    ): CronetFetchState = CronetFetchState(consumer, producerContext)

    override fun fetch(fetchState: CronetFetchState, callback: NetworkFetcher.Callback) {
        try {
            val urlRequestBuilder = cronetEngine.newUrlRequestBuilder(
                fetchState.uri.toString(),
                UrlRequestCallback(callback),
                callExecutor
            )

            config?.invoke(urlRequestBuilder)

            val urlRequest = urlRequestBuilder.build()

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
        private val mBytesOs = ByteArrayOutputStream()
        private val mReceiveChannel = Channels.newChannel(mBytesOs)

        private var isFailure = false

        private var canceled = false

        private fun UrlResponseInfo.isSuccess() = httpStatusCode in 200..299

        override fun onRedirectReceived(
            request: UrlRequest, info: UrlResponseInfo?, newLocationUrl: String?
        ) {
            request.followRedirect()
        }

        override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
            if (info.isSuccess()) {
                request.read(ByteBuffer.allocateDirect(32 * 1024))
            } else {
                callback.onFailure(IOException("Http code is: ${info.httpStatusCode}"))
            }
        }

        override fun onReadCompleted(
            request: UrlRequest,
            info: UrlResponseInfo?,
            byteBuffer: ByteBuffer
        ) {
            if (isFailure || canceled) return

            byteBuffer.flip()

            try {
                mReceiveChannel.write(byteBuffer)
            } catch (e: IOException) {
                isFailure = true
                callback.onFailure(e)
                return
            }
            byteBuffer.clear()
            request.read(byteBuffer)
        }

        override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
            val contentLength: Int? = info.allHeaders["Content-Length"]?.lastOrNull()?.toIntOrNull()

            callback.onResponse(ByteArrayInputStream(mBytesOs.toByteArray()), contentLength ?: -1)
        }

        override fun onFailed(
            request: UrlRequest?, info: UrlResponseInfo?, error: CronetException?
        ) {
            isFailure = true
            callback.onFailure(error)
            mBytesOs.reset()
            mBytesOs.close()
        }

        override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
            canceled = true
            callback.onCancellation()
            mBytesOs.reset()
            mBytesOs.close()
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