package com.mathildeguillossou.chauffeurprive.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mathildeguillossou.chauffeurprive.R;
import com.mathildeguillossou.chauffeurprive.model.MyPlaces;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
/**
 * @author mathildeguillossou on 24/03/2017
 */

public class PlaceAdapter extends RealmRecyclerViewAdapter<MyPlaces, PlaceAdapter.MyViewHolder> {

    private OnItemClickListener mListener;

    public PlaceAdapter(@Nullable OrderedRealmCollection<MyPlaces> data, boolean autoUpdate, OnItemClickListener listener) {
        super(data, autoUpdate);
        mListener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_places, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        if(getData() != null) {
            int line = position +1;
            MyPlaces obj = getData().get(position);

            String address = "";
            address += (obj.address != null)?obj.address:"";
            address += (obj.city != null)?", " + obj.city:"";
            address += (obj.country != null)?", " + obj.country:"";
            holder.addressTv.setText(line + ": " + address);
            holder.bind(obj, mListener);
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView addressTv;

        MyViewHolder(View itemView) {
            super(itemView);
            addressTv = (TextView) itemView.findViewById(R.id.address);
        }

        void bind(final MyPlaces thePlace, final OnItemClickListener listener) {
            //name.setText(item.name);
            //Picasso.with(itemView.getContext()).load(item.imageUrl).into(image);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(thePlace.latitude, thePlace.longitude);
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(double latitude, double longitude);
    }
}
