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
package com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl;

import com.epam.drill.agent.ttl.threadpool.TtlExecutors;
import com.epam.drill.agent.ttl.threadpool.agent.internal.logging.Logger;
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.ClassInfo;
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.JavassistTransformlet;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.Utils.isClassAtPackageJavaUtil;
import static com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.Utils.signatureOfMethod;

/**
 * TTL {@link JavassistTransformlet} for {@link java.util.concurrent.Executor}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @author wuwen5 (wuwen.55 at aliyun dot com)
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see TtlPriorityBlockingQueueTransformlet
 * @since 2.5.1
 */
public class TtlExecutorTransformlet implements JavassistTransformlet {
    private static final Logger logger = Logger.getLogger(TtlExecutorTransformlet.class);

    private static final Set<String> EXECUTOR_CLASS_NAMES = new HashSet<>();
    private static final Map<String, String> PARAM_TYPE_NAME_TO_DECORATE_METHOD_CLASS = new HashMap<>();

    private static final String THREAD_POOL_EXECUTOR_CLASS_NAME = "java.util.concurrent.ThreadPoolExecutor";
    private static final String RUNNABLE_CLASS_NAME = "java.lang.Runnable";

    static {
        EXECUTOR_CLASS_NAMES.add(THREAD_POOL_EXECUTOR_CLASS_NAME);
        EXECUTOR_CLASS_NAMES.add("java.util.concurrent.ScheduledThreadPoolExecutor");

        PARAM_TYPE_NAME_TO_DECORATE_METHOD_CLASS.put(RUNNABLE_CLASS_NAME, "com.epam.drill.agent.ttl.TtlRunnable");
        PARAM_TYPE_NAME_TO_DECORATE_METHOD_CLASS.put("java.util.concurrent.Callable", "com.epam.drill.agent.ttl.TtlCallable");
    }

    private static final String THREAD_FACTORY_CLASS_NAME = "java.util.concurrent.ThreadFactory";

    private final boolean disableInheritableForThreadPool;

    public TtlExecutorTransformlet(boolean disableInheritableForThreadPool) {
        this.disableInheritableForThreadPool = disableInheritableForThreadPool;
    }

    @Override
    public void doTransform(@NonNull final ClassInfo classInfo) throws IOException, NotFoundException, CannotCompileException {
        // work-around ClassCircularityError:
        //      https://github.com/alibaba/transmittable-thread-local/issues/278
        //      https://github.com/alibaba/transmittable-thread-local/issues/234
        if (isClassAtPackageJavaUtil(classInfo.getClassName())) return;

        final CtClass clazz = classInfo.getCtClass();
        if (EXECUTOR_CLASS_NAMES.contains(classInfo.getClassName())) {
            for (CtMethod method : clazz.getDeclaredMethods()) {
                updateSubmitMethodsOfExecutorClass_decorateToTtlWrapperAndSetAutoWrapperAttachment(method);
            }

            if (disableInheritableForThreadPool) updateConstructorDisableInheritable(clazz);

            classInfo.setModified();
        } else {
            if (clazz.isPrimitive() || clazz.isArray() || clazz.isInterface() || clazz.isAnnotation()) {
                return;
            }
            if (!clazz.subclassOf(clazz.getClassPool().get(THREAD_POOL_EXECUTOR_CLASS_NAME))) return;

            logger.info("Transforming class " + classInfo.getClassName());

            final boolean modified = updateBeforeAndAfterExecuteMethodOfExecutorSubclass(clazz);
            if (modified) classInfo.setModified();
        }
    }

    public static Runnable doAutoWrapForMethodRemove(Runnable runnable) {
        if ("java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask".equals(runnable.getClass().getName())) {
            return runnable;
        }
        return Utils.doAutoWrap(runnable);
    }

    /**
     * @see Utils#doAutoWrap(Runnable)
     * @see Utils#doAutoWrap(Callable)
     * @see #doAutoWrapForMethodRemove(Runnable)
     */
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE") // [ERROR] Format string should use %n rather than \n
    private void updateSubmitMethodsOfExecutorClass_decorateToTtlWrapperAndSetAutoWrapperAttachment(@NonNull final CtMethod method) throws NotFoundException, CannotCompileException {
        final int modifiers = method.getModifiers();
        final CtClass[] parameterTypes = method.getParameterTypes();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || parameterTypes.length == 0) return;

        // special handle for remove(Runnable) method of ScheduledThreadPoolExecutor(ThreadPoolExecutor)
        //   more info see https://github.com/alibaba/transmittable-thread-local/issues/547
        if ("remove".equals(method.getName()) && parameterTypes.length == 1
                && RUNNABLE_CLASS_NAME.equals(parameterTypes[0].getName())) {
            String code = "$1 = com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.TtlExecutorTransformlet.doAutoWrapForMethodRemove($1);";
            method.insertBefore(code);
            return;
        }

        StringBuilder insertCode = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            final String paramTypeName = parameterTypes[i].getName();
            if (PARAM_TYPE_NAME_TO_DECORATE_METHOD_CLASS.containsKey(paramTypeName)) {
                String code = String.format(
                        // auto decorate to TTL wrapper
                        "$%d = com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.Utils.doAutoWrap($%<d);",
                        i + 1);
                insertCode.append(code);
            }
        }
        if (insertCode.length() > 0) {
            logger.info("insert code before method " + signatureOfMethod(method) + " of class " +
                method.getDeclaringClass().getName() + ":\n" + insertCode);
            method.insertBefore(insertCode.toString());
        }
    }

    /**
     * @see TtlExecutors#getDisableInheritableThreadFactory(java.util.concurrent.ThreadFactory)
     */
    private void updateConstructorDisableInheritable(@NonNull final CtClass clazz) throws NotFoundException, CannotCompileException {
        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            final CtClass[] parameterTypes = constructor.getParameterTypes();
            final StringBuilder insertCode = new StringBuilder();
            for (int i = 0; i < parameterTypes.length; i++) {
                final String paramTypeName = parameterTypes[i].getName();
                if (THREAD_FACTORY_CLASS_NAME.equals(paramTypeName)) {
                    String code = String.format("$%d = com.epam.drill.agent.ttl.threadpool.TtlExecutors.getDisableInheritableThreadFactory($%<d);", i + 1);
                    insertCode.append(code);
                }
            }
            if (insertCode.length() > 0) {
                logger.info("insert code before constructor " + signatureOfMethod(constructor) + " of class " +
                    constructor.getDeclaringClass().getName() + ": " + insertCode);
                constructor.insertBefore(insertCode.toString());
            }
        }
    }

    /**
     * @see Utils#doUnwrapIfIsAutoWrapper(Runnable)
     */
    private boolean updateBeforeAndAfterExecuteMethodOfExecutorSubclass(@NonNull final CtClass clazz) throws NotFoundException, CannotCompileException {
        final CtClass runnableClass = clazz.getClassPool().get(RUNNABLE_CLASS_NAME);
        final CtClass threadClass = clazz.getClassPool().get("java.lang.Thread");
        final CtClass throwableClass = clazz.getClassPool().get("java.lang.Throwable");
        boolean modified = false;

        try {
            final CtMethod beforeExecute = clazz.getDeclaredMethod("beforeExecute", new CtClass[]{threadClass, runnableClass});
            // unwrap runnable if IsAutoWrapper
            String code = "$2 = com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.Utils.doUnwrapIfIsAutoWrapper($2);";
            logger.info("insert code before method " + signatureOfMethod(beforeExecute) + " of class " +
                beforeExecute.getDeclaringClass().getName() + ": " + code);
            beforeExecute.insertBefore(code);
            modified = true;
        } catch (NotFoundException e) {
            // clazz does not override beforeExecute method, do nothing.
        }

        try {
            final CtMethod afterExecute = clazz.getDeclaredMethod("afterExecute", new CtClass[]{runnableClass, throwableClass});
            // unwrap runnable if IsAutoWrapper
            String code = "$1 = com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.Utils.doUnwrapIfIsAutoWrapper($1);";
            logger.info("insert code before method " + signatureOfMethod(afterExecute) + " of class " +
                afterExecute.getDeclaringClass().getName() + ": " + code);
            afterExecute.insertBefore(code);
            modified = true;
        } catch (NotFoundException e) {
            // clazz does not override afterExecute method, do nothing.
        }

        return modified;
    }
}
