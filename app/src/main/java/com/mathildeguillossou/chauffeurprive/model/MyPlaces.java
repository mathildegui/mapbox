package com.mathildeguillossou.chauffeurprive.model;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * @author mathildeguillossou on 24/03/2017
 */
@RealmClass
public class MyPlaces implements RealmModel {

    @PrimaryKey
    public String timestramp;
    public String address;
    public String city;
    public String country;
    public double latitude;
    public double longitude;

    @Override
    public String toString() {
        return "MyPlaces{" +
                "timestramp='" + timestramp + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
