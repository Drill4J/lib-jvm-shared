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

import com.epam.drill.agent.ttl.TtlCallable;
import com.epam.drill.agent.ttl.TtlRunnable;
import com.epam.drill.agent.ttl.spi.TtlAttachments;
import com.epam.drill.agent.ttl.spi.TtlEnhanced;
import com.epam.drill.agent.ttl.threadpool.agent.internal.logging.Logger;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import javassist.*;

import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

import static com.epam.drill.agent.ttl.TransmittableThreadLocal.Transmitter.capture;

/**
 * <b>Internal</b> utils for {@code Transformlet}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @since 2.6.0
 */
public class Utils {
    private static final Logger logger = Logger.getLogger(Utils.class);

    /**
     * String like {@code public ScheduledFuture scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
     * for {@link  java.util.concurrent.ScheduledThreadPoolExecutor#scheduleAtFixedRate}.
     *
     * @param method method object
     * @return method signature string
     */
    @NonNull
    static String signatureOfMethod(@NonNull final CtBehavior method) throws NotFoundException {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(Modifier.toString(method.getModifiers()));
        if (method instanceof CtMethod) {
            final String returnType = ((CtMethod) method).getReturnType().getSimpleName();
            stringBuilder.append(" ").append(returnType);
        }
        stringBuilder.append(" ").append(method.getName()).append("(");

        final CtClass[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            CtClass parameterType = parameterTypes[i];
            if (i != 0) stringBuilder.append(", ");
            stringBuilder.append(parameterType.getSimpleName());
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    @NonNull
    static String renamedMethodNameByTtl(@NonNull CtMethod method) {
        return "original$" + method.getName() + "$method$renamed$by$ttl";
    }

    static void doTryFinallyForMethod(@NonNull CtMethod method, @NonNull String beforeCode, @NonNull String finallyCode) throws CannotCompileException, NotFoundException {
        doTryFinallyForMethod(method, renamedMethodNameByTtl(method), beforeCode, finallyCode);
    }

    static void doTryFinallyForMethod(@NonNull CtMethod method, @NonNull String renamedMethodName, @NonNull String beforeCode, @NonNull String finallyCode) throws CannotCompileException, NotFoundException {
        final CtClass clazz = method.getDeclaringClass();
        final CtMethod newMethod = CtNewMethod.copy(method, clazz, null);

        // rename original method, and set to private method(avoid reflect out renamed method unexpectedly)
        method.setName(renamedMethodName);
        method.setModifiers(method.getModifiers()
                & ~Modifier.PUBLIC /* remove public */
                & ~Modifier.PROTECTED /* remove protected */
                | Modifier.PRIVATE /* add private */);

        final String returnOp;
        if (method.getReturnType() == CtClass.voidType) {
            returnOp = "";
        } else {
            returnOp = "return ";
        }
        // set new method implementation
        final String code = "{\n" +
                beforeCode + "\n" +
                "try {\n" +
                "    " + returnOp + renamedMethodName + "($$);\n" +
                "} finally {\n" +
                "    " + finallyCode + "\n" +
                "} }";
        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + signatureOfMethod(newMethod) + " of class " + clazz.getName() + ": " + code);
    }

    @Nullable
    public static Object doCaptureWhenNotTtlEnhanced(@Nullable Object obj) {
        if (obj instanceof TtlEnhanced) return null;
        else return capture();
    }

    @Nullable
    public static Runnable doAutoWrap(@Nullable final Runnable runnable) {
        if (runnable == null) return null;

        final TtlRunnable ret = TtlRunnable.get(runnable, false, true);

        // have been auto wrapped?
        if (ret != runnable) setAutoWrapperAttachment(ret);

        return ret;
    }

    @Nullable
    public static <T> Callable<T> doAutoWrap(@Nullable final Callable<T> callable) {
        if (callable == null) return null;

        final TtlCallable<T> ret = TtlCallable.get(callable, false, true);

        // have been auto wrapped?
        if (ret != callable) setAutoWrapperAttachment(ret);

        return ret;
    }

    private static void setAutoWrapperAttachment(@Nullable final Object ttlAttachment) {
        if (!(ttlAttachment instanceof TtlAttachments)) return;

        ((TtlAttachments) ttlAttachment).setTtlAttachment(TtlAttachments.KEY_IS_AUTO_WRAPPER, true);
    }

    @Nullable
    public static Runnable doUnwrapIfIsAutoWrapper(@Nullable final Runnable runnable) {
        if (!(runnable instanceof TtlAttachments)) return runnable;

        // is an auto wrapper?
        final Boolean isAutoWrapper = ((TtlAttachments) runnable).getTtlAttachment(TtlAttachments.KEY_IS_AUTO_WRAPPER);
        if (!Boolean.TRUE.equals(isAutoWrapper)) return runnable;

        return TtlRunnable.unwrap(runnable);
    }

    @NonNull
    public static String getPackageName(@NonNull String className) {
        final int idx = className.lastIndexOf('.');
        if (-1 == idx) return "";

        return className.substring(0, idx);
    }

    /**
     * check the class at the package(not include sub-package).
     */
    public static boolean isClassAtPackage(@NonNull String className, @NonNull String packageName) {
        return packageName.equals(getPackageName(className));
    }

    /**
     * check the class under the package or sub-package of the package.
     */
    public static boolean isClassUnderPackage(@NonNull String className, @NonNull String packageName) {
        String packageOfClass = getPackageName(className);
        return packageOfClass.equals(packageName) || packageOfClass.startsWith(packageName + ".");
    }

    /**
     * check the class at the package {@code java.util}(not include sub-package).
     */
    public static boolean isClassAtPackageJavaUtil(@NonNull String className) {
        return isClassAtPackage(className, "java.util");
    }

    /**
     * check the class is the specified class or its inner class.
     */
    public static boolean isClassOrInnerClass(@NonNull String className, @NonNull String enclosingClassName) {
        return className.equals(enclosingClassName) || className.startsWith(enclosingClassName + "$");
    }
}
