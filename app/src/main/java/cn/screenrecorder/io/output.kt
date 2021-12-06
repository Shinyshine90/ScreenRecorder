@file:JvmName("Outputs")
package cn.screenrecorder.io

import android.content.Context
import java.io.File

fun Context.externalRecordDir() = getExternalFilesDir("records")

fun Context.outputFile(name:String, format:String) = File(externalRecordDir(), "$name.$format")
