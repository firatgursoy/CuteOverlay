package com.gnosis.cuteoverlay;

import java.util.ArrayList;
import java.util.List;

public class DataToSendContainer {
    private List<DataToSend> data = new ArrayList<>();

    public DataToSendContainer() {
    }

    public List<DataToSend> getData() {
        return data;
    }

    public void setData(ArrayList<DataToSend> data) {
        this.data = data;
    }
}
