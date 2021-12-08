package cn.screenrecorder.delegate

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReadWriteLazyVar<T>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {

    private var value: Any? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value == null) {
            value = initializer() ?: throw IllegalArgumentException("Initializer block property $property return null")
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}