package cn.screenrecorder.io

import android.content.Context
import java.io.File

fun Context.outputDir() = getExternalFilesDir("records")

fun Context.outputFile(name:String, format:String) = File(outputDir(), "$name.$format")