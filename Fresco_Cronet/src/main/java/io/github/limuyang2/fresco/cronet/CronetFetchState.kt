package io.github.limuyang2.fresco.cronet

import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.FetchState
import com.facebook.imagepipeline.producers.ProducerContext

/**
 * @author LiMuYang
 * @date 2024/2/20
 * @description
 */
class CronetFetchState(
    consumer: Consumer<EncodedImage>,
    producerContext: ProducerContext
) : FetchState(consumer, producerContext) {
    @JvmField
    var submitTime: Long = 0

    @JvmField
    var responseTime: Long = 0

    @JvmField
    var fetchCompleteTime: Long = 0
}