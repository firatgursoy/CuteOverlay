package com.gnosis.cuteoverlay;

public class DataToSend {
    private String name;
    private String sensorType;
    private String value;

    public DataToSend() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DataToSend{" +
                "name='" + name + '\'' +
                ", sensorType='" + sensorType + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}