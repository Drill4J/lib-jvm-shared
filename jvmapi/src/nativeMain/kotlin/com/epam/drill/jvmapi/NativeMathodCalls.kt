/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.jvmapi

import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.gen.jobject
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret

@Suppress("UNUSED_PARAMETER")
fun callNativeVoidMethod(env: JNIEnv, thiz: jobject, method: () -> Unit) = memScoped {
    withJString {
        ex = env.getPointer(this@memScoped).reinterpret()
        method()
    }
}

@Suppress("UNUSED_PARAMETER")
fun callNativeStringMethod(env: JNIEnv, thiz: jobject, method: () -> String?) = memScoped {
    withJString {
        ex = env.getPointer(this@memScoped).reinterpret()
        NewStringUTF(method())
    }
}
