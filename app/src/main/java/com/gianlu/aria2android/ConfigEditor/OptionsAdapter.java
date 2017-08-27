package com.gianlu.aria2android.ConfigEditor;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2android.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {
    private final Context context;
    private final Map<String, String> options;
    private final LayoutInflater inflater;
    private final IAdapter listener;
    private final List<Integer> edited;

    public OptionsAdapter(Context context, Map<String, String> options, IAdapter listener) {
        this.context = context;
        this.options = options;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.edited = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    public void notifyOptionEdited(int pos) {
        edited.add(pos);
        notifyItemChanged(pos);
    }

    public void saved() {
        edited.clear();
        notifyDataSetChanged();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Map.Entry<String, String> entry = (Map.Entry<String, String>) options.entrySet().toArray()[position];

        holder.key.setText(entry.getKey());
        holder.value.setText(entry.getValue());
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onEditOption(entry.getKey());
            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onRemoveOption(entry.getKey());
            }
        });

        if (edited.contains(position))
            holder.value.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
        else
            holder.value.setTextColor(ContextCompat.getColor(context, android.R.color.tertiary_text_light));
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public interface IAdapter {
        void onEditOption(String key);

        void onRemoveOption(String key);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView key;
        final TextView value;
        final ImageButton edit;
        final ImageButton delete;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.option_item, parent, false));

            key = itemView.findViewById(R.id.optionItem_key);
            value = itemView.findViewById(R.id.optionItem_value);
            edit = itemView.findViewById(R.id.optionItem_edit);
            delete = itemView.findViewById(R.id.optionItem_delete);
        }
    }
}
