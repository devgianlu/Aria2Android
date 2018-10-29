package com.gianlu.aria2android.ConfigEditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2android.R;
import com.gianlu.aria2android.Utils;
import com.gianlu.commonutils.NameValuePair;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {
    private final Context context;
    private final List<NameValuePair> options;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final Set<Integer> edited;
    private boolean changed = false;

    public OptionsAdapter(Context context, Listener listener) {
        this.context = context;
        this.options = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.edited = new HashSet<>();
        if (listener != null) listener.onItemsCountChanged(options.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final NameValuePair entry = options.get(position);

        holder.key.setText(entry.key());
        holder.value.setText(entry.value());
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onEditOption(entry);
            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                remove(holder.getAdapterPosition());
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

    public void saved() {
        edited.clear();
        notifyDataSetChanged();
        changed = false;

        if (listener != null) listener.onItemsCountChanged(options.size());
    }

    private void changed() {
        changed = true;
    }

    public boolean hasChanged() {
        return changed;
    }

    private void remove(int pos) {
        if (pos == -1) return;

        edited.remove(pos);
        options.remove(pos);
        changed();
        notifyItemRemoved(pos);

        if (listener != null) listener.onItemsCountChanged(options.size());
    }

    public void set(NameValuePair newOption) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).key().equals(newOption.key())) {
                options.set(i, newOption);
                edited.add(i);
                changed();
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    public List<NameValuePair> get() {
        return options;
    }

    public void load(@NonNull JSONObject obj) {
        options.clear();

        Iterator<String> iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            options.add(new NameValuePair(key, obj.optString(key, null)));
        }

        notifyDataSetChanged();
        if (listener != null) listener.onItemsCountChanged(options.size());
    }

    public void parseAndAdd(@NonNull String str) {
        List<NameValuePair> newOptions = Utils.parseOptions(str);
        options.addAll(newOptions);
        notifyItemRangeInserted(options.size() - newOptions.size(), newOptions.size());
        changed();

        if (listener != null) listener.onItemsCountChanged(options.size());
    }

    public void add(@NonNull NameValuePair option) {
        options.add(option);
        edited.add(options.size() - 1);
        changed();
        notifyItemInserted(options.size() - 1);

        if (listener != null) listener.onItemsCountChanged(options.size());
    }

    public interface Listener {
        void onEditOption(@NonNull NameValuePair option);

        void onItemsCountChanged(int count);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView key;
        final TextView value;
        final ImageButton edit;
        final ImageButton delete;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.option_item, parent, false));

            key = itemView.findViewById(R.id.optionItem_key);
            value = itemView.findViewById(R.id.optionItem_value);
            edit = itemView.findViewById(R.id.optionItem_edit);
            delete = itemView.findViewById(R.id.optionItem_delete);
        }
    }
}
