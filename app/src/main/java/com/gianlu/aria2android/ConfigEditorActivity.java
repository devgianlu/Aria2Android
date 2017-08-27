package com.gianlu.aria2android;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Prefs;
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

public class ConfigEditorActivity extends AppCompatActivity {
    private static final int IMPORT_CODE = 4543;
    private boolean hasChanges = false;
    private Map<String, String> options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_editor);

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

    private void notifyOptionsChanged() {
        ((TextView) findViewById(R.id.configEditor_test)).setText(options.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config_editor, menu);
        return true;
    }

    private void load() throws JSONException {
        options = new HashMap<>();
        JSONObject obj = new JSONObject(Prefs.getBase64String(this, PKeys.CUSTOM_OPTIONS, "{}"));
        Utils.toMap(obj, options);
        notifyOptionsChanged();
    }

    private void save() {
        try {
            JSONObject obj = new JSONObject();
            Utils.toJSONObject(obj, options);
            Prefs.putBase64String(this, PKeys.CUSTOM_OPTIONS, obj.toString());
            hasChanges = false;
        } catch (JSONException ex) {
            Toaster.show(this, Utils.Messages.FAILED_SAVING_CUSTOM_OPTIONS, ex);
        }
    }

    private void importOptions(String str) {
        options.putAll(Utils.optionsParser(str));
        hasChanges = true;
        notifyOptionsChanged();
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
            if (resultCode == Activity.RESULT_OK) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
}
