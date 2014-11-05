package com.sgt.primoz.alivev1.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Primoz on 5.11.2014.
 */
public class Mode {
    public String name;
    public String[] otherModes;

    public Mode(){
        this.name = "";
        this.otherModes = new String[]{};
    }

    public Mode(String name, String[] otherModes){
        this.name = name;
        this.otherModes = otherModes;
    }


}
