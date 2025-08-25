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
package com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @since 2.11.0
 */
public class ClassInfo {
    private final String className;
    private final byte[] classFileBuffer;
    private final ClassLoader loader;

    // SuppressFBWarnings for classFileBuffer/loader parameter:
    // SuppressFBWarnings for classFileBuffer parameter:
    //   [ERROR] new com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.ClassInfo(String, byte[], ClassLoader)
    //   may expose internal representation by storing an externally mutable object
    //   into ClassInfo.classFileBuffer/loader
    public ClassInfo(@NonNull String className,
                     @NonNull @SuppressFBWarnings({"EI_EXPOSE_REP2"}) byte[] classFileBuffer,
                     @Nullable @SuppressFBWarnings({"EI_EXPOSE_REP2"}) ClassLoader loader) {
        this.className = className;
        this.classFileBuffer = classFileBuffer;
        this.loader = loader;
    }

    @NonNull
    public String getClassName() {
        return className;
    }

    private CtClass ctClass;

    @NonNull
    @SuppressFBWarnings({"EI_EXPOSE_REP"})
    // [ERROR] Medium: com.epam.drill.agent.ttl.threadpool.agent.transformlet.ClassInfo.getCtClass()
    // may expose internal representation
    // by returning ClassInfo.ctClass [com.epam.drill.agent.ttl.threadpool.agent.transformlet.ClassInfo]
    public CtClass getCtClass() throws IOException {
        if (ctClass != null) return ctClass;

        final ClassPool classPool = new ClassPool(false);
        if (loader == null) {
            classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
        } else {
            classPool.appendClassPath(new LoaderClassPath(loader));
        }

        final CtClass clazz = classPool.makeClass(new ByteArrayInputStream(classFileBuffer), false);
        clazz.defrost();

        this.ctClass = clazz;
        return clazz;
    }

    private boolean modified = false;

    public boolean isModified() {
        return modified;
    }

    public void setModified() {
        this.modified = true;
    }
}
