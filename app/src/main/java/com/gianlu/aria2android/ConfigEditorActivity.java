package com.gianlu.aria2android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gianlu.aria2android.ConfigEditor.OptionsAdapter;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.NameValuePair;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

public class ConfigEditorActivity extends ActivityWithDialog implements OptionsAdapter.Listener {
    private static final int IMPORT_CODE = 3;
    private OptionsAdapter adapter;
    private RecyclerViewLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new RecyclerViewLayout(this);
        setContentView(layout);
        setTitle(R.string.customOptions);

        layout.disableSwipeRefresh();
        layout.setLayoutManager(new SuppressingLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        layout.getList().addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new OptionsAdapter(this, this);
        layout.loadListData(adapter);

        try {
            load();
        } catch (JSONException ex) {
            Toaster.with(this).message(R.string.failedLoadingOptions).ex(ex).show();
            onBackPressed();
            return;
        }

        String importOptions = getIntent().getStringExtra("import");
        if (importOptions != null) {
            try {
                importOptionsFromStream(new FileInputStream(importOptions));
            } catch (FileNotFoundException ex) {
                Toaster.with(this).message(R.string.fileNotFound).ex(ex).show();
            }

            save();

            // noinspection deprecation
            Prefs.remove(PK.DEPRECATED_USE_CONFIG);
            // noinspection deprecation
            Prefs.remove(PK.DEPRECATED_CONFIG_FILE);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.updatedApp_importedConfig)
                    .setMessage(R.string.updatedApp_importedConfig_message)
                    .setNeutralButton(android.R.string.ok, null);

            showDialog(builder);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config_editor, menu);
        return true;
    }

    private void load() throws JSONException {
        adapter.load(Prefs.getJSONObject(PK.CUSTOM_OPTIONS, new JSONObject()));
    }

    private void save() {
        try {
            Prefs.putJSONObject(PK.CUSTOM_OPTIONS, NameValuePair.toJson(adapter.get()));
            adapter.saved();
        } catch (JSONException ex) {
            Toaster.with(this).message(R.string.failedSavingCustomOptions).ex(ex).show();
        }
    }

    private void importOptionsFromStream(@NonNull InputStream in) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        } catch (IOException | OutOfMemoryError ex) {
            System.gc();
            Toaster.with(this).message(R.string.cannotImport).ex(ex).show();
            return;
        }

        adapter.parseAndAdd(builder.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_CODE) {
            if (resultCode == Activity.RESULT_OK && data.getData() != null) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    if (in != null)
                        importOptionsFromStream(in);
                    else
                        Toaster.with(this).message(R.string.cannotImport).ex(new Exception("InputStream null!")).show();
                } catch (FileNotFoundException ex) {
                    Toaster.with(this).message(R.string.fileNotFound).ex(ex).show();
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
                        adapter.add(new NameValuePair(keyStr, value.getText().toString()));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.configEditor_add:
                showAddDialog();
                return true;
            case android.R.id.home:
                if (adapter.hasChanged()) {
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

                    showDialog(builder);
                } else {
                    onBackPressed();
                }
                return true;
            case R.id.configEditor_import:
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(Intent.createChooser(intent, "Import from another configuration file..."), IMPORT_CODE);
                } catch (ActivityNotFoundException ex) {
                    Toaster.with(this).message(R.string.cannotImport).ex(ex).show();
                }
                return true;
            case R.id.configEditor_done:
                save();
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressLint("InflateParams")
    public void onEditOption(@NonNull final NameValuePair option) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.edit_option_dialog, null, false);
        SuperTextView value = layout.findViewById(R.id.editOptionDialog_value);
        value.setHtml(R.string.currentValue, option.value());
        final EditText newValue = layout.findViewById(R.id.editOptionDialog_newValue);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(option.key())
                .setView(layout)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newValueStr = newValue.getText().toString();
                        if (!newValueStr.equals(option.value()))
                            adapter.set(new NameValuePair(option.key(), newValueStr));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    @Override
    public void onItemsCountChanged(int count) {
        if (count <= 0) layout.showInfo(R.string.noCustomOptions);
        else layout.showList();
    }
}
