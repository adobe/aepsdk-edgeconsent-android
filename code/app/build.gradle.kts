/*
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("com.diffplug.spotless")
}

val mavenCoreVersion: String by project
val mavenEdgeVersion: String by project

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        toggleOffOn("format:off", "format:on")
        target("src/*/java/**/*.java")
        removeUnusedImports()
        prettier(mapOf("prettier" to "2.7.1", "prettier-plugin-java" to "1.6.2"))
                .config(mapOf("parser" to "java", "tabWidth" to 4, "useTabs" to true, "printWidth" to 120))
        endWithNewline()
        licenseHeader(BuildConstants.ADOBE_LICENSE_HEADER)
    }
}

android {
      namespace = "com.adobe.marketing.mobile.edge.consent.testapp"

    defaultConfig {
        applicationId = "com.adobe.marketing.mobile.edge.consent.testapp"
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION
        versionCode = BuildConstants.Versions.VERSION_CODE
        versionName = BuildConstants.Versions.VERSION_NAME
    }

    buildTypes {
        getByName(BuildConstants.BuildTypes.RELEASE)  {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":edgeconsent"))
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    implementation("com.adobe.marketing.mobile:edge:$mavenEdgeVersion")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}