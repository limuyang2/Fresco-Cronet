# Fresco-Cronet
### [中文](https://github.com/limuyang2/Fresco-Cronet/blob/main/README_CN.md)

Using Cornet as Fresco network request library

## Get
```
implementation("io.github.limuyang2:fresco-cronet:1.0.1")
```

## Usage example:
```kotlin
    // 1.Create CronetEngine
    val cronetEngine = CronetEngine.Builder(applicationContext)
    // ... other config
    .build()

    // 2.Create CronetNetworkFetcher. 
    // The second parameter is the thread pool, If not parameter, the default is used
    val networkFetcher = CronetNetworkFetcher(cronetEngine, Dispatchers.IO.asExecutor())

    // Fresco config
    val config = ImagePipelineConfig.Builder(this)
        .setNetworkFetcher(networkFetcher)
    .build()

    // 3.init Fresco
    Fresco.initialize(this, config)
```
