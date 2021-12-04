package cn.screenrecorder.record


interface IMediaRecord {

    fun prepare()

    fun start()

    fun stop()

    fun release()
}