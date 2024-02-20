import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")

    `maven-publish`
    signing
}

android {
    namespace = "io.github.limuyang2.fresco.cronet"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("com.facebook.fresco:fresco:3.1.3")
    implementation("com.facebook.fresco:middleware:3.1.3")
    compileOnly("org.chromium.net:cronet-api:119.6045.31")
}



//---------- maven upload info -----------------------------------

val versionName = "1.0.0"

var signingKeyId = ""//签名的密钥后8位
var signingPassword = ""//签名设置的密码
var secretKeyRingFile = ""//生成的secring.gpg文件目录


val localProperties: File = project.rootProject.file("local.properties")

if (localProperties.exists()) {
    println("Found secret props file, loading props")
    val properties = Properties()

    InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
        properties.load(reader)
    }
    signingKeyId = properties.getProperty("signing.keyId")
    signingPassword = properties.getProperty("signing.password")
    secretKeyRingFile = properties.getProperty("signing.secretKeyRingFile")

} else {
    println("No props file, loading env vars")
}

afterEvaluate {

    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.findByName("release"))
                groupId = "io.github.limuyang2"
                artifactId = "fresco-cronet"
                version = versionName

                pom {
                    name.value("fresco-cronet")
                    description.value("fresco cronet lib")
                    url.value("https://github.com/limuyang2/Fresco-Cronet")

                    licenses {
                        license {
                            //协议类型
                            name.value("The MIT License")
                            url.value("https://github.com/limuyang2/Fresco-Cronet/blob/main/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.value("limuyang2")
                            name.value("limuyang")
                            email.value("limuyang2@hotmail.com")
                        }
                    }

                    scm {
                        connection.value("scm:git@github.com:limuyang2/Fresco-Cronet.git")
                        developerConnection.value("scm:git@github.com:limuyang2/Fresco-Cronet.git")
                        url.value("https://github.com/limuyang2/Fresco-Cronet")
                    }
                }
            }

        }

        repositories {
            maven {
                setUrl("$rootDir/RepoDir")
            }
        }



    }

}

gradle.taskGraph.whenReady {
    if (allTasks.any { it is Sign }) {

        allprojects {
            extra["signing.keyId"] = signingKeyId
            extra["signing.secretKeyRingFile"] = secretKeyRingFile
            extra["signing.password"] = signingPassword
        }
    }
}

signing {
    sign(publishing.publications)
}