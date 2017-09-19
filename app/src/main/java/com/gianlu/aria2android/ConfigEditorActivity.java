package com.gianlu.aria2android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.gianlu.aria2android.ConfigEditor.OptionsAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Prefs;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigEditorActivity extends AppCompatActivity implements OptionsAdapter.IAdapter {
    private static final int IMPORT_CODE = 4543;
    private final Map<String, String> options = new HashMap<>();
    private boolean hasChanges = false;
    private OptionsAdapter adapter;
    private FrameLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_editor);
        setTitle(R.string.customOptions);

        layout = findViewById(R.id.configEditor);
        RecyclerView list = findViewById(R.id.configEditor_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new OptionsAdapter(this, options, this);
        list.setAdapter(adapter);

        try {
            load();
        } catch (JSONException ex) {
            Toaster.show(this, Utils.Messages.FAILED_LOADING_OPTIONS, ex);
            onBackPressed();
            return;
        }

        String importOptions = getIntent().getStringExtra("import");
        if (importOptions != null) {
            try {
                importOptionsFromStream(new FileInputStream(importOptions));
            } catch (FileNotFoundException ex) {
                Toaster.show(this, Utils.Messages.FILE_NOT_FOUND, ex);
            }

            save();

            Prefs.remove(this, PKeys.DEPRECATED_USE_CONFIG);
            Prefs.remove(this, PKeys.DEPRECATED_CONFIG_FILE);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.updatedApp_importedConfig)
                    .setMessage(R.string.updatedApp_importedConfig_message)
                    .setNeutralButton(android.R.string.ok, null);

            CommonUtils.showDialog(this, builder);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config_editor, menu);
        return true;
    }

    private void load() throws JSONException {
        options.clear();
        JSONObject obj = new JSONObject(Prefs.getBase64String(this, PKeys.CUSTOM_OPTIONS, "{}"));
        Utils.toMap(obj, options);
        adapter.notifyDataSetChanged();
    }

    private void save() {
        try {
            JSONObject obj = new JSONObject();
            Utils.toJSONObject(obj, options);
            Prefs.putBase64String(this, PKeys.CUSTOM_OPTIONS, obj.toString());
            hasChanges = false;
            adapter.saved();
        } catch (JSONException ex) {
            Toaster.show(this, Utils.Messages.FAILED_SAVING_CUSTOM_OPTIONS, ex);
        }
    }

    private void importOptions(String str) {
        Map<String, String> newOptions = Utils.optionsParser(str);
        options.putAll(newOptions);
        hasChanges = true;
        adapter.notifyItemRangeInserted(options.size() - newOptions.size(), newOptions.size());
    }

    private void importOptionsFromStream(@NonNull InputStream in) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        } catch (IOException ex) {
            Toaster.show(this, Utils.Messages.CANNOT_IMPORT, ex);
            return;
        }

        importOptions(builder.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_CODE) {
            if (resultCode == Activity.RESULT_OK && data.getData() != null) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    if (in != null) importOptionsFromStream(in);
                    else
                        Toaster.show(this, Utils.Messages.CANNOT_IMPORT, new Exception("in is null!"));
                } catch (FileNotFoundException ex) {
                    Toaster.show(this, Utils.Messages.FILE_NOT_FOUND, ex);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("InflateParams")
    private void showAddDialog() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.new_option_dialog, null, false);
        final EditText key = layout.findViewById(R.id.editOptionDialog_key);
        final EditText value = layout.findViewById(R.id.editOptionDialog_value);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.newOption)
                .setView(layout)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String keyStr = key.getText().toString();
                        if (keyStr.startsWith("--")) keyStr = keyStr.substring(2);
                        options.put(keyStr, value.getText().toString());
                        int pos = options.size() - 1;
                        adapter.notifyItemInserted(pos);
                        adapter.notifyOptionEdited(pos);
                        hasChanges = true;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        CommonUtils.showDialog(this, builder);
    }

    @SuppressLint("InflateParams")
    private void showEditDialog(final String key) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.edit_option_dialog, null, false);
        SuperTextView value = layout.findViewById(R.id.editOptionDialog_value);
        value.setHtml(R.string.currentValue, options.get(key));
        final EditText newValue = layout.findViewById(R.id.editOptionDialog_newValue);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(key)
                .setView(layout)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        options.put(key, newValue.getText().toString());
                        int pos = CommonUtils.indexOf(options.keySet(), key);
                        if (pos != -1) adapter.notifyOptionEdited(pos);
                        hasChanges = true;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        CommonUtils.showDialog(this, builder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.configEditor_add:
                showAddDialog();
                return true;
            case android.R.id.home:
                if (hasChanges) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.unsavedChanges)
                            .setMessage(R.string.unsavedChanges_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    save();
                                    onBackPressed();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    onBackPressed();
                                }
                            })
                            .setNeutralButton(android.R.string.cancel, null);

                    CommonUtils.showDialog(this, builder);
                } else {
                    onBackPressed();
                }
                return true;
            case R.id.configEditor_import:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "Import from another configuration file..."), IMPORT_CODE);
                return true;
            case R.id.configEditor_done:
                save();
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditOption(String key) {
        showEditDialog(key);
    }

    @Override
    public void onRemoveOption(String key) {
        int pos = CommonUtils.indexOf(options.keySet(), key);
        if (pos != -1) {
            hasChanges = true;
            options.remove(key);
            adapter.notifyItemRemoved(pos);
        }
    }

    @Override
    public void onItemsCountChanged(int count) {
        if (count == 0) {
            MessageLayout.show(layout, R.string.noCustomOptions, R.drawable.ic_info_outline_black_48dp);
        } else {
            MessageLayout.hide(layout);
        }
    }
}
