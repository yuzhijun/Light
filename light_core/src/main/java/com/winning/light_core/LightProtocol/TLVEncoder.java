package com.winning.light_core.LightProtocol;

import java.nio.charset.Charset;

/**
 * @author yuzhijun TLV编码器
 * */
public class TLVEncoder {

	/**
	 * 生成 Tag ByteArray
	 */
	public static byte[] encoderTag(int tagValue) {
		int rawTag = tagValue & 0xFF;
		return intToByteArray(rawTag);
	}

	/**
	 * 生成TLV的Tag字节数据
	 */
	public static byte[] encoderLength(int length) {
		if (length < 0) {
			throw new IllegalArgumentException();
		} else
			// 短形式
			if (length < 128) {
				byte[] actual = new byte[1];
				actual[0] = (byte) length;
				return actual;
			} else
				// 长形式
				if (length < 256) {
					byte[] actual = new byte[2];
					actual[0] = (byte) 0x81;
					actual[1] = (byte) length;
					return actual;
				} else if (length < 65536) {
					byte[] actual = new byte[3];
					actual[0] = (byte) 0x82;
					actual[1] = (byte) (length >> 8);
					actual[2] = (byte) length;
					return actual;
				} else if (length < 16777126) {
					byte[] actual = new byte[4];
					actual[0] = (byte) 0x83;
					actual[1] = (byte) (length >> 16);
					actual[2] = (byte) (length >> 8);
					actual[3] = (byte) length;
					return actual;
				} else {
					byte[] actual = new byte[5];
					actual[0] = (byte) 0x84;
					actual[1] = (byte) (length >> 24);
					actual[2] = (byte) (length >> 16);
					actual[3] = (byte) (length >> 8);
					actual[4] = (byte) length;
					return actual;
				}
	}

	/**
	 * 字符串转化为字节数组
	 * */
	public static byte[] str2ByteArray(String str) {
		byte[] strByte = str.getBytes(Charset.forName("utf-8"));
		return strByte;
	}

	/**
	 * int转化为byteArray
	 * */
	private static byte[] intToByteArray(int i) {
		byte[] result = new byte[1];
		result[0] = (byte) (i & 0xFF);
		return result;
	}
}