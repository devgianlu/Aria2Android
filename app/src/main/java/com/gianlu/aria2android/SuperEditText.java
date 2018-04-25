package com.gianlu.aria2android;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.gianlu.commonutils.Logging;

public class SuperEditText extends TextInputLayout implements TextWatcher {
    private TextInputEditText text;
    private Validator validator;

    public SuperEditText(Context context) {
        super(context);
        init();
    }

    public SuperEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SuperEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public String getText() {
        return text.getText().toString();
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public TextInputEditText getEditText() {
        return text;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    private void init() {
        text = new TextInputEditText(getContext());
        text.addTextChangedListener(this);
        addView(text);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (validator != null) {
            try {
                validator.validate(editable.toString());
            } catch (InvalidInputException ex) {
                try {
                    text.setError(getContext().getString(ex.messageId));
                } catch (ClassCastException exx) {
                    Logging.log(exx);
                }

                return;
            }

            setErrorEnabled(false);
        }
    }

    public interface Validator {
        void validate(String text) throws InvalidInputException;
    }

    public static class InvalidInputException extends Throwable {
        private final int messageId;

        public InvalidInputException(@StringRes int messageId) {
            this.messageId = messageId;
        }
    }
}
