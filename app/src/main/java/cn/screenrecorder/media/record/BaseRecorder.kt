package cn.screenrecorder.media.record

import android.media.projection.MediaProjection
import cn.screenrecorder.media.IMediaProcessor
import java.io.File

abstract class BaseRecorder(
    protected val outputFileRetriever: () -> File,
    protected val projectionRetriever: () -> MediaProjection
) : IMediaProcessor