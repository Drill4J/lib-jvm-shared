// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm

import kotlin.jvm.*

/**
 * A visitor to visit a Java module. The methods of this class must be called in the following
 * order: ( `visitMainClass` | ( `visitPackage` | `visitRequire` | `visitExport` | `visitOpen` | `visitUse` | `visitProvide` )* ) `visitEnd`.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
abstract class ModuleVisitor @JvmOverloads constructor(api: Int, moduleVisitor: ModuleVisitor? = null) {
    /**
     * The ASM API version implemented by this visitor. The value of this field must be one of [ ][Opcodes.ASM6] or [Opcodes.ASM7].
     */
    protected val api: Int

    /**
     * The module visitor to which this visitor must delegate method calls. May be null.
     */
    protected var mv: ModuleVisitor?

    /**
     * Visit the main class of the current module.
     *
     * @param mainClass the internal name of the main class of the current module.
     */
    open fun visitMainClass(mainClass: String?) {
        if (mv != null) {
            mv!!.visitMainClass(mainClass)
        }
    }

    /**
     * Visit a package of the current module.
     *
     * @param packaze the internal name of a package.
     */
    open fun visitPackage(packaze: String?) {
        if (mv != null) {
            mv!!.visitPackage(packaze)
        }
    }

    /**
     * Visits a dependence of the current module.
     *
     * @param module the fully qualified name (using dots) of the dependence.
     * @param access the access flag of the dependence among `ACC_TRANSITIVE`, `ACC_STATIC_PHASE`, `ACC_SYNTHETIC` and `ACC_MANDATED`.
     * @param version the module version at compile time, or null.
     */
    open fun visitRequire(module: String?, access: Int, version: String?) {
        if (mv != null) {
            mv!!.visitRequire(module, access, version)
        }
    }

    /**
     * Visit an exported package of the current module.
     *
     * @param packaze the internal name of the exported package.
     * @param access the access flag of the exported package, valid values are among `ACC_SYNTHETIC` and `ACC_MANDATED`.
     * @param modules the fully qualified names (using dots) of the modules that can access the public
     * classes of the exported package, or null.
     */
    open fun visitExport(packaze: String?, access: Int, vararg modules: String?) {
        if (mv != null) {
            mv!!.visitExport(packaze, access, *modules)
        }
    }

    /**
     * Visit an open package of the current module.
     *
     * @param packaze the internal name of the opened package.
     * @param access the access flag of the opened package, valid values are among `ACC_SYNTHETIC` and `ACC_MANDATED`.
     * @param modules the fully qualified names (using dots) of the modules that can use deep
     * reflection to the classes of the open package, or null.
     */
    open fun visitOpen(packaze: String?, access: Int, vararg modules: String?) {
        if (mv != null) {
            mv!!.visitOpen(packaze, access, *modules)
        }
    }

    /**
     * Visit a service used by the current module. The name must be the internal name of an interface
     * or a class.
     *
     * @param service the internal name of the service.
     */
    open fun visitUse(service: String?) {
        if (mv != null) {
            mv!!.visitUse(service)
        }
    }

    /**
     * Visit an implementation of a service.
     *
     * @param service the internal name of the service.
     * @param providers the internal names of the implementations of the service (there is at least
     * one provider).
     */
    open fun visitProvide(service: String?, vararg providers: String?) {
        if (mv != null) {
            mv!!.visitProvide(service, *providers)
        }
    }

    /**
     * Visits the end of the module. This method, which is the last one to be called, is used to
     * inform the visitor that everything have been visited.
     */
    open fun visitEnd() {
        if (mv != null) {
            mv!!.visitEnd()
        }
    }
    /**
     * Constructs a new [ModuleVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of [Opcodes.ASM6]
     * or [Opcodes.ASM7].
     * @param moduleVisitor the module visitor to which this visitor must delegate method calls. May
     * be null.
     */
    /**
     * Constructs a new [ModuleVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of [Opcodes.ASM6]
     * or [Opcodes.ASM7].
     */
    init {
        if (api != Opcodes.ASM9 && api != Opcodes.ASM8 && api != Opcodes.ASM7 && api != Opcodes.ASM6 && api != Opcodes.ASM5 && api != Opcodes.ASM4 && api != Opcodes.ASM10_EXPERIMENTAL) {
            throw IllegalArgumentException("Unsupported api $api")
        }
        if (api == Opcodes.ASM10_EXPERIMENTAL) {
            Constants.checkAsmExperimental(this)
        }
        this.api = api
        mv = moduleVisitor
    }
}
