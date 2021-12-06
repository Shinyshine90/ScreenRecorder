package cn.screenrecorder.utils;

public class AccEncodeUtils {

    public static int ADTS_PACKAGE_HEADER_LENGTH = 7;

    /**
     * 由Aac帧创建ADTS Aac帧
     * @param accPacket codec编码后的帧
     * @return ADTS Aac 帧
     */
    public static byte[] createADTSPacket(byte[] accPacket) {
        int adtsFrameLength = accPacket.length + ADTS_PACKAGE_HEADER_LENGTH;
        byte[] adtsAccFrame = new byte[adtsFrameLength];
        addADTStoPacket(adtsAccFrame, adtsFrameLength);
        System.arraycopy(accPacket, 0, adtsAccFrame, ADTS_PACKAGE_HEADER_LENGTH, accPacket.length);
        return adtsAccFrame;
    }

    /**
     * 添加ADTS头
     */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;
        // 44.1KHz
        int freqIdx = 4;
        // CPE
        int chanCfg = 2;
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

}
