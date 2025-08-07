package com.gianlu.aria2android;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2android.databinding.ConfigEditorBinding;
import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.aria2lib.R;
import com.gianlu.aria2lib.ui.ImportExportUtils;
import com.gianlu.aria2lib.ui.SimpleOptionsAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.preferences.CommonPK;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ConfigEditorActivity extends BastActivity implements SimpleOptionsAdapter.Listener {
    private static final int IMPORT_CODE = 1;
    private static final String TAG = "ConfigEditorActivity";
    private SimpleOptionsAdapter adapter;
    private RecyclerMessageView rmv;

    private void load() throws JSONException {
        adapter.load(JsonStoring.intoPrefs().getJsonObject(Aria2PK.CUSTOM_OPTIONS));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfigEditorBinding binding = ConfigEditorBinding.inflate(getLayoutInflater());
        rmv = binding.content;
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.customOptions);
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());

        getMenuInflater().inflate(R.menu.aria2lib_config_editor, binding.toolbar.getMenu());
        binding.toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
        setNight();

        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        adapter = new SimpleOptionsAdapter(this, this);
        rmv.loadListData(adapter);

        try {
            load();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed loading JSON.", ex);
            Toaster.with(this).message(R.string.failedLoadingOptions).show();
            onBackPressed();
        }
    }

    private void setNight() {
        boolean b = Prefs.getBoolean(CommonPK.NIGHT_MODE, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (!b) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    private void save() {
        try {
            JsonStoring.intoPrefs().putJsonObject(Aria2PK.CUSTOM_OPTIONS, ImportExportUtils.toJson(adapter.get()));
            adapter.saved();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed saving JSON.", ex);
            Toaster.with(this).message(R.string.failedSavingCustomOptions).show();
        }
    }

    @SuppressLint("InflateParams")
    private void showAddDialog() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.aria2lib_dialog_new_option, null, false);
        TextInputLayout key = layout.findViewById(R.id.editOptionDialog_key);
        TextInputLayout value = layout.findViewById(R.id.editOptionDialog_value);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.newOption).setView(layout)
                .setPositiveButton(R.string.apply, (dialogInterface, i) -> {
                    String keyStr = CommonUtils.getText(key);
                    if (keyStr.startsWith("--")) keyStr = keyStr.substring(2);
                    adapter.add(new Pair<>(keyStr, CommonUtils.getText(value)));
                }).setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_CODE) {
            if (resultCode == Activity.RESULT_OK && data.getData() != null) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    if (in == null || adapter == null) return;

                    try {
                        adapter.add(ImportExportUtils.readConfigFromStream(in));
                    } catch (IOException | OutOfMemoryError ex) {
                        Toaster.with(this).message(R.string.cannotImport).show();
                    }
                } catch (FileNotFoundException ex) {
                    Toaster.with(this).message(R.string.fileNotFound).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.configEditor_add) {
            showAddDialog();
            return true;
        } else if (id == android.R.id.home) {
            if (adapter.hasChanged()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.unsavedChanges)
                        .setMessage(R.string.unsavedChanges_message)
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            save();
                            onBackPressed();
                        })
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> onBackPressed())
                        .setNeutralButton(android.R.string.cancel, null);

                showDialog(builder);
            } else {
                onBackPressed();
            }

            return true;
        } else if (id == R.id.configEditor_import) {
            try {
                startActivityForResult(Intent.createChooser(ImportExportUtils.createConfigImportIntent(), getString(R.string.importConfig)), IMPORT_CODE);
            } catch (ActivityNotFoundException ex) {
                Toaster.with(this).message(R.string.missingFileExplorer).show();
            }
            return true;
        } else if (id == R.id.configEditor_done) {
            save();
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressLint("InflateParams")
    public void onEditOption(@NonNull Pair<String, String> option) {
        FrameLayout layout = (FrameLayout) getLayoutInflater().inflate(R.layout.aria2lib_dialog_edit_option, null, false);
        TextInputLayout newValue = layout.findViewById(R.id.editOptionDialog_value);
        CommonUtils.setText(newValue, option.second);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(option.first)
                .setView(layout)
                .setPositiveButton(R.string.apply, (dialogInterface, i) -> {
                    String newValueStr = CommonUtils.getText(newValue);
                    if (!newValueStr.equals(option.second))
                        adapter.set(new Pair<>(option.first, newValueStr));
                })
                .setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    @Override
    public void onItemsCountChanged(int count) {
        if (count <= 0) rmv.showInfo(R.string.noCustomOptions);
        else rmv.showList();
    }
}