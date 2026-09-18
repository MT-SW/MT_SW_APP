/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    alias(libs.plugins.meshtastic.kmp.library)
    alias(libs.plugins.wire)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
    alias(libs.plugins.meshtastic.kmp.jvm.android)
}

kotlin {
    android {
        withHostTest { isIncludeAndroidResources = true }
        withDeviceTest { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.meshtastic.protobufs)
            // ondemand.proto is compiled locally by the Wire plugin below (see the wire {} block) --
            // it is the OnDemand/Sniffer port-354 protocol, kept out of the official protobufs artifact.
            api(libs.wire.runtime)
            api(projects.core.common)
            api(projects.core.resources)

            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            implementation(libs.kermit)
            api(libs.okio)
            api(libs.compose.multiplatform.resources)

            // TAKPacket-SDK is multiplatform since 0.7.0: proto types come from the
            // protobufs SDK above, zstd compression from its transitive pure-Kotlin
            // kzstd codec, and CoT XML from xmlutil — all KMP, all targets. api()-
            // exported so :core:takserver reaches the org.meshtastic.tak.* pipeline
            // from commonMain on every platform (no JVM-only scoping or iOS stubs).
            //
            // takpacket-sdk still declares a transitive protobufs (protobufs-jvm:2.7.25); exclude it so
            // the app's single protobufs version (api above) is authoritative. Otherwise the older
            // transitive pin out-ranks our snapshot on the host-test classpath and breaks proto ABI
            // (NoSuchMethodError on post-2.7.25 messages). Drop once takpacket stops exporting protobufs.
            // .toString() is load-bearing: catalog deps are immutable ("Minimal dependencies are
            // immutable"), so the exclude{} below only works on the string-notation copy.
            api(libs.takpacket.sdk.kmp.get().toString()) {
                exclude(group = "org.meshtastic", module = "protobufs")
                exclude(group = "org.meshtastic", module = "protobufs-jvm")
                exclude(group = "org.meshtastic", module = "protobufs-android")
            }
        }
        // BouncyCastle only here (not commonMain): core:model also builds for iOS as a compile-only
        // validation target, and bcprov-jdk18on is a JVM-only artifact with no Kotlin/Native variant.
        getByName("jvmAndroidMain") { dependencies { implementation(libs.bouncycastle.bcprov) } }

        androidMain.dependencies {
            api(libs.androidx.annotation)
            api(libs.androidx.core.ktx)
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
            }
        }

        commonTest.dependencies { implementation(projects.core.testing) }
    }
}

// Compiles ondemand.proto (OnDemand diagnostics + Sniffer control, port 354) locally, since it is not part
// of the official meshtastic/protobufs artifact (libs.meshtastic.protobufs above). java_package in the proto
// file matches the official artifact's (org.meshtastic.proto), so generated classes land in the same package
// and every existing OnDemand call site keeps working with no import changes. boxOneOfsMinSize mirrors the
// official meshtastic/protobufs Wire config so oneof fields (e.g. OnDemandResponse.node_stats/.ping/...)
// flatten to nullable properties instead of a sealed class, matching how the rest of the app already uses them.
wire {
    sourcePath { srcDir("src/commonMain/proto") }
    kotlin { boxOneOfsMinSize = 5000 }
}
