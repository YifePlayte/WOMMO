package com.yifeplayte.wommo.utils

/**
 * Object 工具
 */
@Suppress("unused")
object Object {
    init {
        System.loadLibrary("classhelper")
    }

    /**
     * 对当前 Object 直接调用父类方法
     * @param obj Object
     * @param methodName 方法名称
     * @param methodSignature 方法签名
     * @param args 参数
     */
    external fun invokeSuperObjectMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Any?
    external fun invokeSuperBooleanMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Boolean
    external fun invokeSuperByteMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Byte
    external fun invokeSuperCharMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Char
    external fun invokeSuperShortMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Short
    external fun invokeSuperIntMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Int
    external fun invokeSuperLongMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Long
    external fun invokeSuperFloatMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Float
    external fun invokeSuperDoubleMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): Double
    external fun invokeSuperVoidMethod(obj: Any, methodName: String, methodSignature: String, vararg args: Any?)

    @Suppress("UNCHECKED_CAST")
    fun <T> invokeSuperObjectMethodAs(obj: Any, methodName: String, methodSignature: String, vararg args: Any?): T? =
        invokeSuperObjectMethod(obj, methodName, methodSignature, args) as? T?
}