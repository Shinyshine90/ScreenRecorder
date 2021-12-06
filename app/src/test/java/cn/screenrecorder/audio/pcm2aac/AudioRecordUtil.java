package cn.screenrecorder.audio.pcm2aac;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.screenrecorder.App;
import cn.screenrecorder.io.Outputs;

public class AudioRecordUtil {

    //设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private final int sampleRateInHz = 44100;
    //设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private final int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    //音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //录制状态
    private boolean recorderState = true;
    private byte[] buffer;
    private AudioRecord audioRecord;
    private static AudioRecordUtil audioRecordUtil = new AudioRecordUtil();

    public static AudioRecordUtil getInstance() {
        return audioRecordUtil;
    }

    private AudioRecordUtil() {
        init();
    }

    private void init() {
        int recordMinBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        //指定 AudioRecord 缓冲区大小
        buffer = new byte[recordMinBufferSize];
        //根据录音参数构造AudioRecord实体对象
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig,
                audioFormat, recordMinBufferSize);
    }

    /**
     * 开始录制
     */
    public void start() {
        if (audioRecord.getState() == AudioRecord.RECORDSTATE_STOPPED) {
            recorderState = true;
            audioRecord.startRecording();
            new RecordThread().start();
        }
    }

    /**
     * 停止录制
     */
    public void stop() {
        recorderState = false;
        if (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
        }
        audioRecord.release();
    }

    private static String TAG = "RecordThread";

    private class RecordThread extends Thread {

        private FileOutputStream fos;

        private PCMEncoderAAC encoder = new PCMEncoderAAC(sampleRateInHz, data -> {
            try {
                Log.d(TAG, String.valueOf(data.length));
                fos.write(data);
            } catch (Exception e) { }
        });

        @Override
        public void run() {
            try {
                fos = new FileOutputStream(Outputs.outputFile(App.Companion.getApp(),"test", "aac"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (recorderState) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    //获取到的pcm数据就是buffer了
                    //Log.d(TAG, String.valueOf(buffer.length));
                    encoder.encodeData(buffer);
                }
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}