package com.mathildeguillossou.chauffeurprive;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * @author mathildeguillossou on 24/03/2017
 */

public class MapboxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder()
                .build());
    }
}
