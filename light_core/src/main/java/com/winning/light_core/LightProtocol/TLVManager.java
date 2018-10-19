package com.winning.light_core.LightProtocol;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class TLVManager {
	/**
	 * 基本数据类型
	 * */
	private final static int STRING_TYPE = 0x0D;
	private final static int INTEGER_TYPE = 0x06;
	private final static int BOOLEN_TYPE = 0x01;
	/**
	 * 标志是哪种类型的性能数据
	 * */
	public static final int ACCOUNT = 0x10;
	public static final int BATTERY = 0x11;
	public static final int CPU = 0x12;
	public static final int CRASH = 0x13;
	public static final int DEADLOCK = 0x14;
	public static final int DEVICE = 0x15;
	public static final int FPS = 0x16;
	public static final int INFLATE = 0x17;
	public static final int LEAK = 0x18;
	public static final int NETWORK = 0x19;
	public static final int SM = 0x1A;
	public static final int STARTUP = 0x1B;
	public static final int TRAFFIC = 0x1C;
	public static final int DEVICE_HANDLER = 0x1D;
	public static final int APP_HANDLER = 0x1E;
	public static final int ACCOUNT_HANDLER = 0x1F;
	public static final int BASE_INFO = 0x20;
	public static final int USER_BEHAVIOR = 0x21;

	private static List<TLVObject> tlvObjects = new ArrayList<TLVObject>();

	/**
	 * 将对象转化为TLV编码的byte数组
	 * @param t
	 *            需要转化的对象
	 * @param type
	 *            存储的数据类别
	 * @throws Exception
	 * */
	public static <T> byte[] convertModel2ByteArray(T t,int type) throws Exception {
		byte[] tag = null;
		int tempTagLength = 0;
		List<TLVObject> list = convertModel2TLV(t);
		tag = TLVEncoder.encoderTag(type);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for (int i = 0; i < list.size(); i++) {
			byte[] tempTagValue = list.get(i).make();
			tempTagLength += tempTagValue.length;
			bos.write(tempTagValue);
		}

		byte[] tagLength = TLVEncoder.encoderLength(tempTagLength);
		byte[] tagValue = bos.toByteArray();

		byte[] tlvByteArray = new byte[tag.length + tagLength.length
				+ tagValue.length];

		System.arraycopy(tag, 0, tlvByteArray, 0, tag.length);
		System.arraycopy(tagLength, 0, tlvByteArray, tag.length,
				tagLength.length);
		System.arraycopy(tagValue, 0, tlvByteArray, tag.length
				+ tagLength.length, tagValue.length);
		return tlvByteArray;

	}

	/**
	 * 将字节数组转化出TLV格式
	 * @param tagvalue
	 *  		字节数组
	 * @param tlvSumObjects
	 * 			用于递归叠加的List
	 * */
	public static List<List<TLVObject>> convertSumTagValue(byte[] tagValue, List<List<TLVObject>> tlvSumObjects){
		byte[] leftByteArray;
		tlvObjects = new ArrayList<TLVObject>();
		if (null != tagValue && tagValue.length >= 1) {
			//获取长度
			byte[] subReceiveByte = subBytes(tagValue, 1, tagValue.length -1);
			int sumLength = TLVDecoder.decoderLengthAndTagValue(subReceiveByte) + 1;

			List<TLVObject> tlvObjects = convertTagValue(Arrays.copyOfRange(tagValue, 0, sumLength));

			tlvSumObjects.add(tlvObjects);

			if (tagValue.length > sumLength) {
				leftByteArray = new byte[tagValue.length - sumLength];
				System.arraycopy(tagValue, sumLength, leftByteArray, 0, tagValue.length - sumLength);
				convertSumTagValue(leftByteArray, tlvSumObjects);
			}
		}


		return tlvSumObjects;
	}

	/**
	 * 解析出byte数组中的tagvalue成List<>
	 * */
	private static List<TLVObject> convertTagValue(byte[] tagValue){
		List<TLVObject> tlvObjects = new ArrayList<TLVObject>();

		TLVObject tlvObject = convertByteArray(tagValue);
		if (tlvObject.getTagLength() == 0) {
			tlvObjects.add(tlvObject);
			return tlvObjects;
		}


		tlvObjects.addAll(convertSubTagValue(tlvObject.getTagValue()));
		return tlvObjects;
	}

	private static List<TLVObject> convertSubTagValue(byte[] subTagValue){
		TLVObject tlvObject = new TLVObject();
		byte[] tag = new byte[1];
		tag[0] = subTagValue[0];
		tlvObject.setTag(tag);

		byte[] subReceiveByte = subBytes(subTagValue, 1, subTagValue.length -1);
		int tagLength = TLVDecoder.decoderLength(subReceiveByte);
		tlvObject.setTagLength(tagLength);

		if (tagLength <= 0) {
			byte[] blankByte = new byte[1];
			blankByte[0] = 0x00;
			tlvObject.setTagValue(blankByte);
			tlvObjects.add(tlvObject);
			return tlvObjects;
		}

		int len = 2;
		if(0x00 == ((subReceiveByte[0] & 0xF1) ^ 0x81))
		{
			len += 1;
		}else if(0x00 == ((subReceiveByte[0] & 0xF2) ^ 0x82)){
			len += 2;
		}else if(0x00 == ((subReceiveByte[0] & 0xF3) ^ 0x83)){
			len += 3;
		}else if(0x00 == ((subReceiveByte[0] & 0xF4) ^ 0x84)){
			len += 4;
		}
		byte[] tagValue = subBytes(subTagValue, len, tagLength);
		tlvObject.setTagValue(tagValue);
		tlvObjects.add(tlvObject);

		int subTagValueLength = subTagValue.length;
		if (tlvObject.make().length < subTagValueLength) {
			byte[] subBytes = subBytes(subTagValue, tagLength + len, subTagValueLength - tagLength -len);
			convertSubTagValue(subBytes);
		}

		return tlvObjects;
	}

	/**
	 * 解析出最外层的TagValue
	 * */
	public static TLVObject convertByteArray(byte[] receiveByte){
		TLVObject tlvObject = new TLVObject();
		//获取tag
		byte[] tag = new byte[1];
		tag[0] = receiveByte[0];
		tlvObject.setTag(tag);

		//获取长度
		byte[] subReceiveByte = subBytes(receiveByte, 1, receiveByte.length -1);
		int tagLength = TLVDecoder.decoderLength(subReceiveByte);
		tlvObject.setTagLength(tagLength);

		//获取tagValue
		int len = 2;
		if (0x00 == ((subReceiveByte[0] & 0xF1) ^ 0x81)) {
			len += 1;
		} else if (0x00 == ((subReceiveByte[0] & 0xF2) ^ 0x82)) {
			len += 2;
		} else if (0x00 == ((subReceiveByte[0] & 0xF3) ^ 0x83)) {
			len += 3;
		} else if (0x00 == ((subReceiveByte[0] & 0xF4) ^ 0x84)) {
			len += 4;
		}
		byte[] tagValue = subBytes(receiveByte, len, receiveByte.length - len);
		tlvObject.setTagValue(tagValue);

		return tlvObject;
	}

	/**
	 * 将对象拼接成符合TLV格式的List列表
	 *
	 * @param t
	 *            需要转化的对象
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * */
	private static <T> List<TLVObject> convertModel2TLV(T t)
			throws IllegalArgumentException, IllegalAccessException {
		String str = null;
		List<TLVObject> list = new ArrayList<TLVObject>();
		Class<? extends Object> clazz = t.getClass();
		Field[] declaredFields = clazz.getDeclaredFields();

		for (Field field : declaredFields) {
			field.setAccessible(true);
			str = (String) field.get(t);

			byte[] strByteArray = TLVEncoder.str2ByteArray(str);
			int tagLength = strByteArray.length;
			byte[] tag = null;
			if (field.getType().isAssignableFrom(String.class)) {
				tag = TLVEncoder.encoderTag(STRING_TYPE);
			} else if (field.getType().isAssignableFrom(Number.class)) {
				tag = TLVEncoder.encoderTag(INTEGER_TYPE);
			} else if (field.getType().isAssignableFrom(Boolean.class)) {
				tag = TLVEncoder.encoderTag(BOOLEN_TYPE);
			}
			TLVObject tlvObject = new TLVObject(tag, tagLength, strByteArray);
			list.add(tlvObject);
		}

		return list;

	}

	public static byte[] subBytes(byte[] src, int begin, int count) {
		byte[] bs = new byte[count];
		for (int i = begin; i < begin + count; i++) {
			bs[i - begin] = src[i];
		}
		return bs;
	}
}
