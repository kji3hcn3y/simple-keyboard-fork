package rkr.simplekeyboard.inputmethod.latin.suggestions;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import rkr.simplekeyboard.inputmethod.R;

public final class SuggestionStripView extends LinearLayout {
    public interface Listener {
        void onSuggestionChosen(String word);
    }

    private static final int MAX_SUGGESTIONS = 3;
    private final List<TextView> mWordViews = new ArrayList<>(MAX_SUGGESTIONS);
    private Listener mListener;
    private int mTextColor = Color.WHITE;
    private int mSecondaryTextColor = 0x88FFFFFF;

    public SuggestionStripView(final Context context) {
        this(context, null);
    }

    public SuggestionStripView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SuggestionStripView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        try {
            TypedArray a = context.obtainStyledAttributes(null, new int[]{
                    R.attr.keyTextColor,
                    R.attr.keyTextInactivatedColor,
                    android.R.attr.background
            }, R.attr.keyboardViewStyle, 0);
            mTextColor = a.getColor(0, Color.WHITE);
            mSecondaryTextColor = a.getColor(1, 0x88FFFFFF);
            int bg = a.getColor(2, 0);
            if (bg != 0) {
                setBackgroundColor(bg);
            }
            a.recycle();
        } catch (Exception ignored) {
        }

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        int rippleRes = outValue.resourceId;

        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            if (i > 0) {
                View divider = new View(context);
                LayoutParams divParams = new LayoutParams(1, (int) (18 * getResources().getDisplayMetrics().density));
                divParams.gravity = Gravity.CENTER_VERTICAL;
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(0x22FFFFFF & mTextColor);
                addView(divider);
            }

            final TextView tv = new TextView(context);
            LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setTextColor(mTextColor);
            if (rippleRes != 0) {
                tv.setBackgroundResource(rippleRes);
            }
            tv.setClickable(true);
            tv.setFocusable(false);

            tv.setOnClickListener(v -> {
                CharSequence text = tv.getText();
                if (text != null && text.length() > 0 && mListener != null) {
                    mListener.onSuggestionChosen(text.toString());
                }
            });

            mWordViews.add(tv);
            addView(tv);
        }
    }

    public void setListener(final Listener listener) {
        mListener = listener;
    }

    public void setSuggestions(final List<String> suggestions) {
        int count = suggestions != null ? suggestions.size() : 0;
        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            TextView tv = mWordViews.get(i);
            if (i < count) {
                String word = suggestions.get(i);
                tv.setText(word);
                if (i == 0) {
                    tv.setTypeface(Typeface.DEFAULT_BOLD);
                    tv.setTextColor(mTextColor);
                } else {
                    tv.setTypeface(Typeface.DEFAULT);
                    tv.setTextColor(mSecondaryTextColor);
                }
            } else {
                tv.setText("");
            }
        }
    }

    public void clear() {
        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            mWordViews.get(i).setText("");
        }
    }
}
