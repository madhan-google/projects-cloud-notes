package com.codekiller.cloudnotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListViewAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;
    ArrayList<String> arrayList;

    ListViewAdapter(Context context, ArrayList<String> arrayList){
        this.arrayList = arrayList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if( inflater == null ){
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if( convertView == null ){
            convertView = inflater.inflate(R.layout.row_item,null);
        }
        TextView textView = convertView.findViewById(R.id.text_view);
        textView.setText(arrayList.get(position));
        return convertView;
    }
}
