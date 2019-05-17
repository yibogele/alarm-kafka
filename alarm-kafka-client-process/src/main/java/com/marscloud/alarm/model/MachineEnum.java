package com.fanwill.alarm.model;

public enum MachineEnum {

	AD("爱动手环","ad"),
	EMPTY("无设备","empty"),
	CAR01001G01("汽车盒子","CAR01001G01");
	private MachineEnum(String name,String code){
		this.name=name;
		this.code=code;
	}
	private String name;
	private String code;
	public String getName() {
		return name;
	}
	public String getCode() {
		return code;
	}
	public static final MachineEnum findMachineEnumByCode(String code){
		if(code==null||code.length()<1){
			return EMPTY;
		}
		MachineEnum values[]=MachineEnum.values();
		for(MachineEnum me:values){
			if(me.getCode().equals(code)){
				return me;
			}
		}
		return EMPTY;
	}
	
}
