package cn.screenrecorder.media


interface IMediaProcessor {

    fun prepare()

    fun start()

    fun stop()

    fun release()
}