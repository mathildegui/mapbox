package com.mathildeguillossou.chauffeurprive.view;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mathildeguillossou.chauffeurprive.R;

public class MainActivity extends AppCompatActivity implements MapFragment.OnMenuInteractionListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, MapFragment.newInstance())
                    .commit();
        }
    }

    private void switchFragment(int id, Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(id, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onMenuItemClick(int id, Fragment frag) {
        switchFragment(id, frag);
    }
}
