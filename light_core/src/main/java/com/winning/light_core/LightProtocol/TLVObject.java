package com.winning.light_core.LightProtocol;

import java.io.Serializable;
/**
 * @author yuzhijun
 * 发送的TLV结构体
 * */
public class TLVObject implements Serializable{
	private byte[] tag;
	private int tagLength;
	private byte[] tagValue;
	
	public TLVObject(){
		
	}
	public TLVObject(byte[] tag,int tagLength,byte[] tagValue){
		this.tag = tag;
		this.tagLength = tagLength;
		this.tagValue = tagValue;
	}
	
	public byte[] getTag() {
		return tag;
	}
	public void setTag(byte[] tag) {
		this.tag = tag;
	}
	
	public int getTagLength() {
		return tagLength;
	}
	public void setTagLength(int tagLength) {
		this.tagLength = tagLength;
	}
	public byte[] getTagValue() {
		return tagValue;
	}
	public void setTagValue(byte[] tagValue) {
		this.tagValue = tagValue;
	}
	
	public byte[] make()
	{
		if (tagLength <= 0) {
			byte[] blankByte = new byte[1];
			blankByte[0] = 0x00;
			return blankByte;
		}
		byte[] bLen = TLVEncoder.encoderLength(this.tagLength);
		byte[] bytes = new byte[tagLength+tag.length+bLen.length];
		int len = 0;
		System.arraycopy(this.tag, 0, bytes, len,this.tag.length );
		len += tag.length;
		System.arraycopy(bLen, 0, bytes, len,bLen.length );
		len += bLen.length;
		System.arraycopy(this.tagValue, 0, bytes, len,tagLength );
		
		return bytes;
	}
	
}
