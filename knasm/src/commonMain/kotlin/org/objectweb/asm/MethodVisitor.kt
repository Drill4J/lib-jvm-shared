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
 * A visitor to visit a Java method. The methods of this class must be called in the following
 * order: ( `visitParameter` )* [ `visitAnnotationDefault` ] ( `visitAnnotation` |
 * `visitAnnotableParameterCount` | `visitParameterAnnotation` `visitTypeAnnotation` | `visitAttribute` )* [ `visitCode` ( `visitFrame` |
 * `visit<i>X</i>Insn` | `visitLabel` | `visitInsnAnnotation` | `visitTryCatchBlock` | `visitTryCatchAnnotation` | `visitLocalVariable` | `visitLocalVariableAnnotation` | `visitLineNumber` )* `visitMaxs` ] `visitEnd`.
 * In addition, the `visit<i>X</i>Insn` and `visitLabel` methods must be called in the
 * sequential order of the bytecode instructions of the visited code, `visitInsnAnnotation`
 * must be called *after* the annotated instruction, `visitTryCatchBlock` must be called
 * *before* the labels passed as arguments have been visited, `visitTryCatchBlockAnnotation` must be called *after* the corresponding try catch block has
 * been visited, and the `visitLocalVariable`, `visitLocalVariableAnnotation` and `visitLineNumber` methods must be called *after* the labels passed as arguments have been
 * visited.
 *
 * @author Eric Bruneton
 */
abstract class MethodVisitor @JvmOverloads constructor(api: Int, methodVisitor: MethodVisitor? = null) {
    /**
     * The ASM API version implemented by this visitor. The value of this field must be one of the
     * `ASM`*x* values in [Opcodes].
     */
    protected val api: Int

    /**
     * The method visitor to which this visitor must delegate method calls. May be null.
     */
    var mv: MethodVisitor?
    // -----------------------------------------------------------------------------------------------
    // Parameters, annotations and non standard attributes
    // -----------------------------------------------------------------------------------------------
    /**
     * Visits a parameter of this method.
     *
     * @param name parameter name or null if none is provided.
     * @param access the parameter's access flags, only `ACC_FINAL`, `ACC_SYNTHETIC`
     * or/and `ACC_MANDATED` are allowed (see [Opcodes]).
     */
    open fun visitParameter(name: String?, access: Int) {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        if (mv != null) {
            mv!!.visitParameter(name, access)
        }
    }

    /**
     * Visits the default value of this annotation interface method.
     *
     * @return a visitor to the visit the actual default value of this annotation interface method, or
     * null if this visitor is not interested in visiting this default value. The
     * 'name' parameters passed to the methods of this annotation visitor are ignored. Moreover,
     * exacly one visit method must be called on this annotation visitor, followed by visitEnd.
     */
    open fun visitAnnotationDefault(): AnnotationVisitor? {
        return if (mv != null) {
            mv!!.visitAnnotationDefault()
        } else null
    }

    /**
     * Visits an annotation of this method.
     *
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return if (mv != null) {
            mv!!.visitAnnotation(descriptor, visible)
        } else null
    }

    /**
     * Visits an annotation on a type in the method signature.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     * [TypeReference.METHOD_TYPE_PARAMETER], [     ][TypeReference.METHOD_TYPE_PARAMETER_BOUND], [TypeReference.METHOD_RETURN], [     ][TypeReference.METHOD_RECEIVER], [TypeReference.METHOD_FORMAL_PARAMETER] or [     ][TypeReference.THROWS]. See [TypeReference].
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
     * static inner type within 'typeRef'. May be null if the annotation targets
     * 'typeRef' as a whole.
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitTypeAnnotation(
        typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean
    ): AnnotationVisitor? {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        return if (mv != null) {
            mv!!.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
        } else null
    }

    /**
     * Visits the number of method parameters that can have annotations. By default (i.e. when this
     * method is not called), all the method parameters defined by the method descriptor can have
     * annotations.
     *
     * @param parameterCount the number of method parameters than can have annotations. This number
     * must be less or equal than the number of parameter types in the method descriptor. It can
     * be strictly less when a method has synthetic parameters and when these parameters are
     * ignored when computing parameter indices for the purpose of parameter annotations (see
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18).
     * @param visible true to define the number of method parameters that can have
     * annotations visible at runtime, false to define the number of method parameters
     * that can have annotations invisible at runtime.
     */
    open fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        if (mv != null) {
            mv!!.visitAnnotableParameterCount(parameterCount, visible)
        }
    }

    /**
     * Visits an annotation of a parameter this method.
     *
     * @param parameter the parameter index. This index must be strictly smaller than the number of
     * parameters in the method descriptor, and strictly smaller than the parameter count
     * specified in [.visitAnnotableParameterCount]. Important note: *a parameter index i
     * is not required to correspond to the i'th parameter descriptor in the method
     * descriptor*, in particular in case of synthetic parameters (see
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18).
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitParameterAnnotation(
        parameter: Int, descriptor: String?, visible: Boolean
    ): AnnotationVisitor? {
        return if (mv != null) {
            mv!!.visitParameterAnnotation(parameter, descriptor, visible)
        } else null
    }

    /**
     * Visits a non standard attribute of this method.
     *
     * @param attribute an attribute.
     */
    open fun visitAttribute(attribute: Attribute) {
        if (mv != null) {
            mv!!.visitAttribute(attribute)
        }
    }

    /** Starts the visit of the method's code, if any (i.e. non abstract method).  */
    open fun visitCode() {
        if (mv != null) {
            mv!!.visitCode()
        }
    }

    /**
     * Visits the current state of the local variables and operand stack elements. This method must(*)
     * be called *just before* any instruction **i** that follows an unconditional branch
     * instruction such as GOTO or THROW, that is the target of a jump instruction, or that starts an
     * exception handler block. The visited types must describe the values of the local variables and
     * of the operand stack elements *just before* **i** is executed.<br></br>
     * <br></br>
     * (*) this is mandatory only for classes whose version is greater than or equal to [ ][Opcodes.V1_6]. <br></br>
     * <br></br>
     * The frames of a method must be given either in expanded form, or in compressed form (all frames
     * must use the same format, i.e. you must not mix expanded and compressed frames within a single
     * method):
     *
     *
     *  * In expanded form, all frames must have the F_NEW type.
     *  * In compressed form, frames are basically "deltas" from the state of the previous frame:
     *
     *  * [Opcodes.F_SAME] representing frame with exactly the same locals as the
     * previous frame and with the empty stack.
     *  * [Opcodes.F_SAME1] representing frame with exactly the same locals as the
     * previous frame and with single value on the stack ( `numStack` is 1 and
     * `stack[0]` contains value for the type of the stack item).
     *  * [Opcodes.F_APPEND] representing frame with current locals are the same as the
     * locals in the previous frame, except that additional locals are defined (`
     * numLocal` is 1, 2 or 3 and `local` elements contains values
     * representing added types).
     *  * [Opcodes.F_CHOP] representing frame with current locals are the same as the
     * locals in the previous frame, except that the last 1-3 locals are absent and with
     * the empty stack (`numLocal` is 1, 2 or 3).
     *  * [Opcodes.F_FULL] representing complete frame data.
     *
     *
     *
     * <br></br>
     * In both cases the first frame, corresponding to the method's parameters and access flags, is
     * implicit and must not be visited. Also, it is illegal to visit two or more frames for the same
     * code location (i.e., at least one instruction must be visited between two calls to visitFrame).
     *
     * @param type the type of this stack map frame. Must be [Opcodes.F_NEW] for expanded
     * frames, or [Opcodes.F_FULL], [Opcodes.F_APPEND], [Opcodes.F_CHOP], [     ][Opcodes.F_SAME] or [Opcodes.F_APPEND], [Opcodes.F_SAME1] for compressed frames.
     * @param numLocal the number of local variables in the visited frame.
     * @param local the local variable types in this frame. This array must not be modified. Primitive
     * types are represented by [Opcodes.TOP], [Opcodes.INTEGER], [     ][Opcodes.FLOAT], [Opcodes.LONG], [Opcodes.DOUBLE], [Opcodes.NULL] or
     * [Opcodes.UNINITIALIZED_THIS] (long and double are represented by a single element).
     * Reference types are represented by String objects (representing internal names), and
     * uninitialized types by Label objects (this label designates the NEW instruction that
     * created this uninitialized value).
     * @param numStack the number of operand stack elements in the visited frame.
     * @param stack the operand stack types in this frame. This array must not be modified. Its
     * content has the same format as the "local" array.
     * @throws IllegalStateException if a frame is visited just after another one, without any
     * instruction between the two (unless this frame is a Opcodes#F_SAME frame, in which case it
     * is silently ignored).
     */
    open fun visitFrame(
        type: Int,
        numLocal: Int,
        local: Array<Any?>?,
        numStack: Int,
        stack: Array<Any?>?
    ) {
        if (mv != null) {
            mv!!.visitFrame(type, numLocal, local, numStack, stack)
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Normal instructions
    // -----------------------------------------------------------------------------------------------
    /**
     * Visits a zero operand instruction.
     *
     * @param opcode the opcode of the instruction to be visited. This opcode is either NOP,
     * ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
     * LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD,
     * FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE,
     * AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2,
     * SWAP, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV,
     * FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR, IUSHR,
     * LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I,
     * D2L, D2F, I2B, I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN, FRETURN,
     * DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW, MONITORENTER, or MONITOREXIT.
     */
    open fun visitInsn(opcode: Int) {
        if (mv != null) {
            mv!!.visitInsn(opcode)
        }
    }

    /**
     * Visits an instruction with a single int operand.
     *
     * @param opcode the opcode of the instruction to be visited. This opcode is either BIPUSH, SIPUSH
     * or NEWARRAY.
     * @param operand the operand of the instruction to be visited.<br></br>
     * When opcode is BIPUSH, operand value should be between Byte.MIN_VALUE and Byte.MAX_VALUE.
     * <br></br>
     * When opcode is SIPUSH, operand value should be between Short.MIN_VALUE and Short.MAX_VALUE.
     * <br></br>
     * When opcode is NEWARRAY, operand value should be one of [Opcodes.T_BOOLEAN], [     ][Opcodes.T_CHAR], [Opcodes.T_FLOAT], [Opcodes.T_DOUBLE], [Opcodes.T_BYTE],
     * [Opcodes.T_SHORT], [Opcodes.T_INT] or [Opcodes.T_LONG].
     */
    open fun visitIntInsn(opcode: Int, operand: Int) {
        if (mv != null) {
            mv!!.visitIntInsn(opcode, operand)
        }
    }

    /**
     * Visits a local variable instruction. A local variable instruction is an instruction that loads
     * or stores the value of a local variable.
     *
     * @param opcode the opcode of the local variable instruction to be visited. This opcode is either
     * ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET.
     * @param var the operand of the instruction to be visited. This operand is the index of a local
     * variable.
     */
    open fun visitVarInsn(opcode: Int, `var`: Int) {
        if (mv != null) {
            mv!!.visitVarInsn(opcode, `var`)
        }
    }

    /**
     * Visits a type instruction. A type instruction is an instruction that takes the internal name of
     * a class as parameter.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either NEW,
     * ANEWARRAY, CHECKCAST or INSTANCEOF.
     * @param type the operand of the instruction to be visited. This operand must be the internal
     * name of an object or array class (see [Type.getInternalName]).
     */
    open fun visitTypeInsn(opcode: Int, type: String?) {
        if (mv != null) {
            mv!!.visitTypeInsn(opcode, type)
        }
    }

    /**
     * Visits a field instruction. A field instruction is an instruction that loads or stores the
     * value of a field of an object.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either
     * GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
     * @param owner the internal name of the field's owner class (see [Type.getInternalName]).
     * @param name the field's name.
     * @param descriptor the field's descriptor (see [Type]).
     */
    open fun visitFieldInsn(
        opcode: Int, owner: String?, name: String?, descriptor: String?
    ) {
        if (mv != null) {
            mv!!.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

    /**
     * Visits a method instruction. A method instruction is an instruction that invokes a method.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either
     * INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
     * @param owner the internal name of the method's owner class (see [     ][Type.getInternalName]).
     * @param name the method's name.
     * @param descriptor the method's descriptor (see [Type]).
     */
    @Deprecated("use {@link #visitMethodInsn(int, String, String, String, boolean)} instead.")
    open fun visitMethodInsn(
        opcode: Int, owner: String?, name: String?, descriptor: String?
    ) {
        val opcodeAndSource = opcode or if (api < Opcodes.ASM5) Opcodes.SOURCE_DEPRECATED else 0
        visitMethodInsn(opcodeAndSource, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE)
    }

    /**
     * Visits a method instruction. A method instruction is an instruction that invokes a method.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either
     * INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
     * @param owner the internal name of the method's owner class (see [     ][Type.getInternalName]).
     * @param name the method's name.
     * @param descriptor the method's descriptor (see [Type]).
     * @param isInterface if the method's owner class is an interface.
     */
    open fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (api < Opcodes.ASM5 && opcode and Opcodes.SOURCE_DEPRECATED === 0) {
            if (isInterface != (opcode == Opcodes.INVOKEINTERFACE)) {
                throw UnsupportedOperationException("INVOKESPECIAL/STATIC on interfaces requires ASM5")
            }
            visitMethodInsn(opcode, owner, name, descriptor)
            return
        }
        if (mv != null) {
            mv!!.visitMethodInsn(opcode and Opcodes.SOURCE_MASK.inv(), owner, name, descriptor, isInterface)
        }
    }

    /**
     * Visits an invokedynamic instruction.
     *
     * @param name the method's name.
     * @param descriptor the method's descriptor (see [Type]).
     * @param bootstrapMethodHandle the bootstrap method.
     * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
     * an [Integer], [Float], [Long], [Double], [String], [     ], [Handle] or [ConstantDynamic] value. This method is allowed to modify
     * the content of the array so a caller should expect that this array may change.
     */
    open fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        if (mv != null) {
            mv!!.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
        }
    }

    /**
     * Visits a jump instruction. A jump instruction is an instruction that may jump to another
     * instruction.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either IFEQ,
     * IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
     * IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
     * @param label the operand of the instruction to be visited. This operand is a label that
     * designates the instruction to which the jump instruction may jump.
     */
    open fun visitJumpInsn(opcode: Int, label: Label?) {
        if (mv != null) {
            mv!!.visitJumpInsn(opcode, label)
        }
    }

    /**
     * Visits a label. A label designates the instruction that will be visited just after it.
     *
     * @param label a [Label] object.
     */
    open fun visitLabel(label: Label?) {
        if (mv != null) {
            mv!!.visitLabel(label)
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Special instructions
    // -----------------------------------------------------------------------------------------------
    /**
     * Visits a LDC instruction. Note that new constant types may be added in future versions of the
     * Java Virtual Machine. To easily detect new constant types, implementations of this method
     * should check for unexpected constant types, like this:
     *
     * <pre>
     * if (cst instanceof Integer) {
     * // ...
     * } else if (cst instanceof Float) {
     * // ...
     * } else if (cst instanceof Long) {
     * // ...
     * } else if (cst instanceof Double) {
     * // ...
     * } else if (cst instanceof String) {
     * // ...
     * } else if (cst instanceof Type) {
     * int sort = ((Type) cst).getSort();
     * if (sort == Type.OBJECT) {
     * // ...
     * } else if (sort == Type.ARRAY) {
     * // ...
     * } else if (sort == Type.METHOD) {
     * // ...
     * } else {
     * // throw an exception
     * }
     * } else if (cst instanceof Handle) {
     * // ...
     * } else if (cst instanceof ConstantDynamic) {
     * // ...
     * } else {
     * // throw an exception
     * }
    </pre> *
     *
     * @param value the constant to be loaded on the stack. This parameter must be a non null [     ], a [Float], a [Long], a [Double], a [String], a [     ] of OBJECT or ARRAY sort for `.class` constants, for classes whose version is
     * 49, a [Type] of METHOD sort for MethodType, a [Handle] for MethodHandle
     * constants, for classes whose version is 51 or a [ConstantDynamic] for a constant
     * dynamic for classes whose version is 55.
     */
    open fun visitLdcInsn(value: Any?) {
        if (api < Opcodes.ASM5
            && (value is Handle
                    || value is Type && value.getSort() === Type.METHOD)
        ) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        if (api < Opcodes.ASM7 && value is ConstantDynamic) {
            throw UnsupportedOperationException("This feature requires ASM7")
        }
        if (mv != null) {
            mv!!.visitLdcInsn(value)
        }
    }

    /**
     * Visits an IINC instruction.
     *
     * @param var index of the local variable to be incremented.
     * @param increment amount to increment the local variable by.
     */
    open fun visitIincInsn(`var`: Int, increment: Int) {
        if (mv != null) {
            mv!!.visitIincInsn(`var`, increment)
        }
    }

    /**
     * Visits a TABLESWITCH instruction.
     *
     * @param min the minimum key value.
     * @param max the maximum key value.
     * @param dflt beginning of the default handler block.
     * @param labels beginnings of the handler blocks. `labels[i]` is the beginning of the
     * handler block for the `min + i` key.
     */
    open fun visitTableSwitchInsn(
        min: Int, max: Int, dflt: Label?, vararg labels: Label?
    ) {
        if (mv != null) {
            mv!!.visitTableSwitchInsn(min, max, dflt, *labels)
        }
    }

    /**
     * Visits a LOOKUPSWITCH instruction.
     *
     * @param dflt beginning of the default handler block.
     * @param keys the values of the keys.
     * @param labels beginnings of the handler blocks. `labels[i]` is the beginning of the
     * handler block for the `keys[i]` key.
     */
    open fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<Label?>?) {
        if (mv != null) {
            mv!!.visitLookupSwitchInsn(dflt, keys, labels)
        }
    }

    /**
     * Visits a MULTIANEWARRAY instruction.
     *
     * @param descriptor an array type descriptor (see [Type]).
     * @param numDimensions the number of dimensions of the array to allocate.
     */
    open fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        if (mv != null) {
            mv!!.visitMultiANewArrayInsn(descriptor, numDimensions)
        }
    }

    /**
     * Visits an annotation on an instruction. This method must be called just *after* the
     * annotated instruction. It can be called several times for the same instruction.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     * [TypeReference.INSTANCEOF], [TypeReference.NEW], [     ][TypeReference.CONSTRUCTOR_REFERENCE], [TypeReference.METHOD_REFERENCE], [     ][TypeReference.CAST], [TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT], [     ][TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT], [     ][TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT], or [     ][TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT]. See [TypeReference].
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
     * static inner type within 'typeRef'. May be null if the annotation targets
     * 'typeRef' as a whole.
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitInsnAnnotation(
        typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean
    ): AnnotationVisitor? {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        return if (mv != null) {
            mv!!.visitInsnAnnotation(typeRef, typePath, descriptor, visible)
        } else null
    }
    // -----------------------------------------------------------------------------------------------
    // Exceptions table entries, debug information, max stack and max locals
    // -----------------------------------------------------------------------------------------------
    /**
     * Visits a try catch block.
     *
     * @param start the beginning of the exception handler's scope (inclusive).
     * @param end the end of the exception handler's scope (exclusive).
     * @param handler the beginning of the exception handler's code.
     * @param type the internal name of the type of exceptions handled by the handler, or null to catch any exceptions (for "finally" blocks).
     * @throws IllegalArgumentException if one of the labels has already been visited by this visitor
     * (by the [.visitLabel] method).
     */
    open fun visitTryCatchBlock(
        start: Label?, end: Label?, handler: Label?, type: String?
    ) {
        if (mv != null) {
            mv!!.visitTryCatchBlock(start, end, handler, type)
        }
    }

    /**
     * Visits an annotation on an exception handler type. This method must be called *after* the
     * [.visitTryCatchBlock] for the annotated exception handler. It can be called several times
     * for the same exception handler.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     * [TypeReference.EXCEPTION_PARAMETER]. See [TypeReference].
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
     * static inner type within 'typeRef'. May be null if the annotation targets
     * 'typeRef' as a whole.
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitTryCatchAnnotation(
        typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean
    ): AnnotationVisitor? {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        return if (mv != null) {
            mv!!.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible)
        } else null
    }

    /**
     * Visits a local variable declaration.
     *
     * @param name the name of a local variable.
     * @param descriptor the type descriptor of this local variable.
     * @param signature the type signature of this local variable. May be null if the local
     * variable type does not use generic types.
     * @param start the first instruction corresponding to the scope of this local variable
     * (inclusive).
     * @param end the last instruction corresponding to the scope of this local variable (exclusive).
     * @param index the local variable's index.
     * @throws IllegalArgumentException if one of the labels has not already been visited by this
     * visitor (by the [.visitLabel] method).
     */
    open fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        if (mv != null) {
            mv!!.visitLocalVariable(name, descriptor, signature, start, end, index)
        }
    }

    /**
     * Visits an annotation on a local variable type.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     * [TypeReference.LOCAL_VARIABLE] or [TypeReference.RESOURCE_VARIABLE]. See [     ].
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
     * static inner type within 'typeRef'. May be null if the annotation targets
     * 'typeRef' as a whole.
     * @param start the fist instructions corresponding to the continuous ranges that make the scope
     * of this local variable (inclusive).
     * @param end the last instructions corresponding to the continuous ranges that make the scope of
     * this local variable (exclusive). This array must have the same size as the 'start' array.
     * @param index the local variable's index in each range. This array must have the same size as
     * the 'start' array.
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        start: Array<Label?>?,
        end: Array<Label?>?,
        index: IntArray?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor? {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException(REQUIRES_ASM5)
        }
        return if (mv != null) {
            mv!!.visitLocalVariableAnnotation(
                typeRef, typePath, start, end, index, descriptor, visible)
        } else null
    }

    /**
     * Visits a line number declaration.
     *
     * @param line a line number. This number refers to the source file from which the class was
     * compiled.
     * @param start the first instruction corresponding to this line number.
     * @throws IllegalArgumentException if `start` has not already been visited by this visitor
     * (by the [.visitLabel] method).
     */
    open fun visitLineNumber(line: Int, start: Label) {
        if (mv != null) {
            mv!!.visitLineNumber(line, start)
        }
    }

    /**
     * Visits the maximum stack size and the maximum number of local variables of the method.
     *
     * @param maxStack maximum stack size of the method.
     * @param maxLocals maximum number of local variables for the method.
     */
    open fun visitMaxs(maxStack: Int, maxLocals: Int) {
        if (mv != null) {
            mv!!.visitMaxs(maxStack, maxLocals)
        }
    }

    /**
     * Visits the end of the method. This method, which is the last one to be called, is used to
     * inform the visitor that all the annotations and attributes of the method have been visited.
     */
    open fun visitEnd() {
        if (mv != null) {
            mv!!.visitEnd()
        }
    }

    companion object {
        private const val REQUIRES_ASM5 = "This feature requires ASM5"
    }
    /**
     * Constructs a new [MethodVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of the `ASM`*x* values in [Opcodes].
     * @param methodVisitor the method visitor to which this visitor must delegate method calls. May
     * be null.
     */
    /**
     * Constructs a new [MethodVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of the `ASM`*x* values in [Opcodes].
     */
    init {
        if (api != Opcodes.ASM9 && api != Opcodes.ASM8 && api != Opcodes.ASM7 && api != Opcodes.ASM6 && api != Opcodes.ASM5 && api != Opcodes.ASM4 && api != Opcodes.ASM10_EXPERIMENTAL) {
            throw IllegalArgumentException("Unsupported api $api")
        }
        if (api == Opcodes.ASM10_EXPERIMENTAL) {
            Constants.checkAsmExperimental(this)
        }
        this.api = api
        mv = methodVisitor
    }
}
