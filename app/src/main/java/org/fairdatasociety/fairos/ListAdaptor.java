package org.fairdatasociety.fairos;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ListAdaptor extends RecyclerView.Adapter<ListAdaptor.ViewHolder>{
    private List<Item> mData = new ArrayList<Item>();
    private LayoutInflater mInflater;
    private ListAdaptor.ItemClickListener mClickListener;
    int charactersToDisplay = 10;

    // data is passed into the constructor
    ListAdaptor(Context context, List<String> dirs, List<String> files) {
        this.mInflater = LayoutInflater.from(context);
        for (int i=0; i<dirs.size(); i++) {
            Item item = new Item();
            item.name = dirs.get(i);
            item.type = "dir";
            this.mData.add(item);
        }
        for (int i=0; i<files.size(); i++) {
            Item item = new Item();
            item.name = files.get(i);
            item.type = "file";
            this.mData.add(item);
        }
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String file = mData.get(position).name;
        int length = file.length();
        if (length > charactersToDisplay * 2) {
            int start = charactersToDisplay;
            int end = length - charactersToDisplay;
            String truncatedFilename = file.substring(0, start) + "...." + file.substring(end, length);
            holder.myTextView.setText(truncatedFilename);
        } else {
            holder.myTextView.setText(file);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.filename);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    Item getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ListAdaptor.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

class Item {
    String name;
    String type;
}
