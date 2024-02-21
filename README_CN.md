# Fresco-Cronet
使用`Cornet`作为`Fresco`的网络请求库

## 引入
```
implementation("io.github.limuyang2:fresco-cronet:1.0.1")
```

## 使用示例:
```kotlin
    // 1.创建 CronetEngine
    val cronetEngine = CronetEngine.Builder(
        NativeCronetEngineBuilderImpl(this)
    )
    // ... other config
    .build()

    // 2.创建 CronetNetworkFetcher。第二个参数是线程池，不传递则使用默认的。
    val networkFetcher = CronetNetworkFetcher(cronetEngine, Dispatchers.IO.asExecutor())

    // Fresco config
    val config = ImagePipelineConfig.Builder(this)
        .setNetworkFetcher(networkFetcher)
    .build()

    // 3.初始化 Fresco
    Fresco.initialize(this, config)
```
