package com.winning.light_core.LightProtocol;

/**
 * @author yuzhijun TLV解码器
 * */
public class TLVDecoder {

	/**
	 * 用于建立十六进制字符的输出的小写字符数组
	 */
	private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	/**
	 * 用于建立十六进制字符的输出的大写字符数组
	 */
	private static final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * TLV的Tag字节数据长度
	 */
	public static int decoderLength(byte[] lenBytes) {
		int sum = 0;
		int len = 1;
		int index = 0;
		if (0x00 == ((lenBytes[0] & 0xF1) ^ 0x81)
				|| 0x00 == ((lenBytes[0] & 0xF2) ^ 0x82)
				|| 0x00 == ((lenBytes[0] & 0xF3) ^ 0x83)
				|| 0x00 == ((lenBytes[0] & 0xF4) ^ 0x84)) {
			len = lenBytes[0] & 0x0F;
			len += 1;
			index = 1;
		}

		for (int i = index; i < len; i++) {
			int shift = (len - 1 - i) * 8;
			sum += (lenBytes[i] & 0x000000FF) << shift;
		}
		return sum;
	}

	/**
	 * TLV的length和tag字节数据的总长度
	 * */
	public static int decoderLengthAndTagValue(byte[] lenBytes){
		int length = 0;
		int tagLength = decoderLength(lenBytes);

		if (0x00 == ((lenBytes[0] & 0xF1) ^ 0x81)
				|| 0x00 == ((lenBytes[0] & 0xF2) ^ 0x82)
				|| 0x00 == ((lenBytes[0] & 0xF3) ^ 0x83)
				|| 0x00 == ((lenBytes[0] & 0xF4) ^ 0x84)) {
			length = lenBytes[0] & 0x0F;
			length += 1;
		}

		return length + tagLength;
	}

	/**
	 * 将字节数组转换为十六进制字符串
	 *
	 * @param data
	 *            byte[]
	 * @param toLowerCase
	 *            <code>true</code> 传换成小写格式 ， <code>false</code> 传换成大写格式
	 * @return 十六进制String
	 */
	public static String encodeHexStr(byte[] data, boolean toLowerCase) {
		return encodeHexStr(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	/**
	 * 将字节数组转换为十六进制字符数组
	 *
	 * @param data
	 *            byte[]
	 * @param toLowerCase
	 *            <code>true</code> 传换成小写格式 ， <code>false</code> 传换成大写格式
	 * @return 十六进制char[]
	 */
	public static char[] encodeHex(byte[] data, boolean toLowerCase) {
		return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	/**
	 * 将字节数组转换为十六进制字符串
	 *
	 * @param data
	 *            byte[]
	 * @param toDigits
	 *            用于控制输出的char[]
	 * @return 十六进制String
	 */
	private static String encodeHexStr(byte[] data, char[] toDigits) {
		return new String(encodeHex(data, toDigits));
	}

	/**
	 * 将字节数组转换为十六进制字符数组
	 *
	 * @param data
	 *            byte[]
	 * @param toDigits
	 *            用于控制输出的char[]
	 * @return 十六进制char[]
	 */
	private static char[] encodeHex(byte[] data, char[] toDigits) {
		int l = data.length;
		char[] out = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return out;
	}

	// 把byte 转化为两位十六进制数
	public static String toHex(byte b) {
		String result = Integer.toHexString(b & 0xFF);
		if (result.length() == 1) {
			result = '0' + result;
		}
		return result;
	}
}
