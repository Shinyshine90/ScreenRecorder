package cn.screenrecorder.record

import android.media.projection.MediaProjection
import java.io.File

abstract class BaseRecorder(
    protected val outputFileRetriever: () -> File,
    protected val projectionRetriever: () -> MediaProjection
) : IMediaRecord