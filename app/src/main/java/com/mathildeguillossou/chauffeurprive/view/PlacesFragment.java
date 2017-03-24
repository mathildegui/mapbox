package com.mathildeguillossou.chauffeurprive.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mathildeguillossou.chauffeurprive.R;
import com.mathildeguillossou.chauffeurprive.adapter.PlaceAdapter;
import com.mathildeguillossou.chauffeurprive.model.MyPlaces;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlacesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlacesFragment extends Fragment {


    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;

    public PlacesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlacesFragment.
     */
    public static PlacesFragment newInstance() {
        return new PlacesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_places, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        Realm realm = Realm.getDefaultInstance();
        OrderedRealmCollection<MyPlaces> thePlaces = realm.where(MyPlaces.class).findAllSorted("timestramp");


        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        PlaceAdapter adapter = new PlaceAdapter(thePlaces, true, new PlaceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(double latitude, double longitude) {
                //FIXME : handle this better -- nthing to do here
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(0);
                fm.popBackStack(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fm.executePendingTransactions();
                fm.beginTransaction()
                        .replace(R.id.fragment_container, MapFragment.newInstance(latitude, longitude))
                        .commit();
            }
        });

        mRecyclerView.setAdapter(adapter);
        for(MyPlaces mp : thePlaces) {
            Log.d("The_Place", mp.toString());
        }
    }
}
