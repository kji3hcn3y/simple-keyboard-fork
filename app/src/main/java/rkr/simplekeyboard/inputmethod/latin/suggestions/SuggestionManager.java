package rkr.simplekeyboard.inputmethod.latin.suggestions;

import android.content.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SuggestionManager {
    private static SuggestionManager sInstance;

    private final Context mContext;
    private final Map<String, BinaryDictionary> mDictionaries = new HashMap<>();
    private String mCurrentLocale = "en";

    public static synchronized void init(final Context context) {
        if (sInstance == null) {
            sInstance = new SuggestionManager(context.getApplicationContext());
        }
    }

    public static synchronized SuggestionManager getInstance() {
        return sInstance;
    }

    private SuggestionManager(final Context context) {
        mContext = context;
    }

    public synchronized void setLocale(final Locale locale) {
        if (locale == null) return;
        String lang = locale.getLanguage().toLowerCase();
        if ("ru".equals(lang) || "uk".equals(lang) || "be".equals(lang)) {
            mCurrentLocale = "ru";
        } else {
            mCurrentLocale = "en";
        }
    }

    private synchronized BinaryDictionary getDictionary(final String lang) {
        BinaryDictionary dict = mDictionaries.get(lang);
        if (dict == null) {
            String path = "dict/" + lang + "_dict.bin";
            dict = BinaryDictionary.load(mContext, path, lang);
            if (dict != null) {
                mDictionaries.put(lang, dict);
            }
        }
        return dict;
    }

    public synchronized List<String> getSuggestions(final String prefix, final int maxSuggestions) {
        BinaryDictionary dict = getDictionary(mCurrentLocale);
        if (dict == null) {
            return Collections.emptyList();
        }
        return dict.getSuggestions(prefix, maxSuggestions);
    }

    public synchronized void deallocateMemory() {
        mDictionaries.clear();
    }
}
