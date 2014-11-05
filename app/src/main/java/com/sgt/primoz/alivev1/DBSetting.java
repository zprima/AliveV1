package com.sgt.primoz.alivev1;

import java.util.UUID;

/**
 * Created by Primoz on 4.11.2014.
 */
public class DBSetting {
    public String Id;
    public String Username;
    public String Password;
    public String Url;

    public DBSetting(){
        this.Id = UUID.randomUUID().toString();
        this.Username = "";
        this.Password = "";
        this.Url = "https://twebapp.pro4erp.com/pb";
    }
}
