package cn.screenrecorder.utils

import android.media.MediaCodecList

fun getSupportMediaCodec() =  MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter {
        it.isEncoder
    }


