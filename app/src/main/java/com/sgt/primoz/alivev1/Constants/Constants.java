package com.sgt.primoz.alivev1.Constants;

import android.content.res.Resources;
import android.util.ArrayMap;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Primoz on 5.11.2014.
 */
public class Constants {
    public static final String sAPI = "/s/api";

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static class Modes {
        public Map<String,Mode> modes;

        public Modes(){
            this.modes = initModes();
        }

        private Map<String,Mode> initModes(){
            Map<String,Mode> list = new ArrayMap<String,Mode>();
            list.put("absent",new Mode("Odsoten",new String[]{"work","at_home","fieldwork"}));
            list.put("work",new Mode("V službi",new String[]{"absent","pause","snack","fieldwork"}));
            list.put("at_home", new Mode("Delo doma", new String[]{"absent", "pause_h", "snack_h", "fieldwork"}));
            list.put("fieldwork", new Mode("Teren", new String[]{"absent", "pause_f", "snack_f", "work", "at_home"}));
            list.put("pause", new Mode("Pavza",new String[]{"work"}));
            list.put("pause_h", new Mode("Pavza - doma",new String[]{"at_home"}));
            list.put("pause_f", new Mode("Pavza - teren", new String[]{"fieldwork"}));
            list.put("snack", new Mode("Malica",new String[]{"work"}));
            list.put("snack_h",new Mode("Malica - doma", new String[]{"at_home"}));
            list.put("snack_f", new Mode("Malica - teren", new String[]{"fieldwork"}));
            list.put("sick_leave", new Mode("Bolniška",new String[]{"work", "at_home", "fieldwork"}));
            list.put("holiday", new Mode("Dopus", new String[]{"work", "at_home", "fieldwork"}));
            return list;
        }
    }
}
