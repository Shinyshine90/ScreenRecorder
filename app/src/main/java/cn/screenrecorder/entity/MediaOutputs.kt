package cn.screenrecorder.entity

import android.content.Context
import cn.screenrecorder.io.outputDir
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class MediaOutputs(
    var mp4TempPath:String = "",
    var accTempPath:String = "",
    var assembleOutputPath:String = ""
)

fun MediaOutputs.generate(context: Context) {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    val curDate = Date(System.currentTimeMillis())
    val date = formatter.format(curDate).replace(" ", "")
    //设置路径前需要保证有夫目录
    val parentDir = context.outputDir()!!.apply { mkdirs() }
    mp4TempPath = parentDir.absolutePath + File.separator + date + "-temp.mp4"
    accTempPath = parentDir.absolutePath + File.separator + date + "-temp.acc"
    assembleOutputPath = parentDir.absolutePath + File.separator + date + ".mp4"
}