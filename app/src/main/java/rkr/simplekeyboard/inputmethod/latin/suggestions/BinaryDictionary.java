package rkr.simplekeyboard.inputmethod.latin.suggestions;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BinaryDictionary {
    private static final Map<Character, String> RU_NEAR_KEYS = new HashMap<>();
    private static final Map<Character, String> EN_NEAR_KEYS = new HashMap<>();

    static {
        RU_NEAR_KEYS.put('й', "цф");
        RU_NEAR_KEYS.put('ц', "йуфыв");
        RU_NEAR_KEYS.put('у', "цкыва");
        RU_NEAR_KEYS.put('к', "уевап");
        RU_NEAR_KEYS.put('е', "кнapr");
        RU_NEAR_KEYS.put('н', "егпро");
        RU_NEAR_KEYS.put('г', "ншрол");
        RU_NEAR_KEYS.put('ш', "гщолд");
        RU_NEAR_KEYS.put('щ', "шзлдж");
        RU_NEAR_KEYS.put('з', "щхджэ");
        RU_NEAR_KEYS.put('х', "зъжэ");
        RU_NEAR_KEYS.put('ъ', "хэ");
        RU_NEAR_KEYS.put('ф', "йцыя");
        RU_NEAR_KEYS.put('ы', "цуфвяч");
        RU_NEAR_KEYS.put('в', "укыачс");
        RU_NEAR_KEYS.put('а', "кевпсм");
        RU_NEAR_KEYS.put('п', "енарим");
        RU_NEAR_KEYS.put('р', "нгпоит");
        RU_NEAR_KEYS.put('о', "гшрлтвь");
        RU_NEAR_KEYS.put('л', "шщодьб");
        RU_NEAR_KEYS.put('д', "щзлжбю");
        RU_NEAR_KEYS.put('ж', "здэю");
        RU_NEAR_KEYS.put('э', "хъж");
        RU_NEAR_KEYS.put('я', "фыч");
        RU_NEAR_KEYS.put('ч', "ывяс");
        RU_NEAR_KEYS.put('с', "вачм");
        RU_NEAR_KEYS.put('м', "апси");
        RU_NEAR_KEYS.put('и', "прмт");
        RU_NEAR_KEYS.put('т', "роиь");
        RU_NEAR_KEYS.put('ь', "олтб");
        RU_NEAR_KEYS.put('б', "лдью");
        RU_NEAR_KEYS.put('ю', "джб");

        EN_NEAR_KEYS.put('q', "wa");
        EN_NEAR_KEYS.put('w', "qeasd");
        EN_NEAR_KEYS.put('e', "wsdr");
        EN_NEAR_KEYS.put('r', "edft");
        EN_NEAR_KEYS.put('t', "rfgy");
        EN_NEAR_KEYS.put('y', "tghu");
        EN_NEAR_KEYS.put('u', "yhji");
        EN_NEAR_KEYS.put('i', "ujko");
        EN_NEAR_KEYS.put('o', "iklp");
        EN_NEAR_KEYS.put('p', "ol");
        EN_NEAR_KEYS.put('a', "qwsz");
        EN_NEAR_KEYS.put('s', "qweadzx");
        EN_NEAR_KEYS.put('d', "wersfxc");
        EN_NEAR_KEYS.put('f', "ertdgcv");
        EN_NEAR_KEYS.put('g', "rtyfhvb");
        EN_NEAR_KEYS.put('h', "tyugjbn");
        EN_NEAR_KEYS.put('j', "yuihknm");
        EN_NEAR_KEYS.put('k', "uiojlm");
        EN_NEAR_KEYS.put('l', "iopk");
        EN_NEAR_KEYS.put('z', "asx");
        EN_NEAR_KEYS.put('x', "zsdc");
        EN_NEAR_KEYS.put('c', "xdfv");
        EN_NEAR_KEYS.put('v', "cfgb");
        EN_NEAR_KEYS.put('b', "vghn");
        EN_NEAR_KEYS.put('n', "bhjm");
        EN_NEAR_KEYS.put('m', "njk");
    }

    private final ByteBuffer mBuffer;
    private final int mWordCount;
    private final int mDataStart;
    private final String mLocale;

    public static BinaryDictionary load(final Context context, final String assetPath, final String locale) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
            FileChannel fc = fis.getChannel();
            ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getLength());
            fis.close();
            return new BinaryDictionary(buf, locale);
        } catch (Exception e) {
            try (InputStream is = context.getAssets().open(assetPath)) {
                int size = is.available();
                byte[] bytes = new byte[size];
                int read = 0;
                while (read < size) {
                    int r = is.read(bytes, read, size - read);
                    if (r == -1) break;
                    read += r;
                }
                return new BinaryDictionary(ByteBuffer.wrap(bytes), locale);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private BinaryDictionary(final ByteBuffer buffer, final String locale) {
        mBuffer = buffer;
        mLocale = locale;
        mBuffer.position(4);
        mWordCount = mBuffer.getInt();
        mDataStart = 8 + mWordCount * 4;
    }

    public static class Candidate {
        public final String word;
        public final int score;

        public Candidate(final String word, final int score) {
            this.word = word;
            this.score = score;
        }
    }

    public List<String> getSuggestions(final String rawPrefix, final int maxSuggestions) {
        if (rawPrefix == null || rawPrefix.isEmpty() || mWordCount == 0) {
            return Collections.emptyList();
        }

        final String prefix = rawPrefix.toLowerCase();
        final byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);

        final List<Candidate> candidates = new ArrayList<>();
        final Set<String> seenWords = new HashSet<>();

        collectPrefixCandidates(prefixBytes, 1.0f, candidates, seenWords, maxSuggestions * 4);

        if (candidates.size() < maxSuggestions && prefix.length() >= 2) {
            final Map<Character, String> nearMap = "ru".equals(mLocale) ? RU_NEAR_KEYS : EN_NEAR_KEYS;
            for (int i = prefix.length() - 1; i >= 0 && candidates.size() < maxSuggestions * 2; i--) {
                char ch = prefix.charAt(i);
                String neighbors = nearMap.get(ch);
                if (neighbors != null) {
                    for (int n = 0; n < neighbors.length(); n++) {
                        char alt = neighbors.charAt(n);
                        String altPrefix = prefix.substring(0, i) + alt + prefix.substring(i + 1);
                        byte[] altBytes = altPrefix.getBytes(StandardCharsets.UTF_8);
                        collectPrefixCandidates(altBytes, 0.7f, candidates, seenWords, maxSuggestions);
                    }
                }
            }
        }

        candidates.sort((c1, c2) -> Integer.compare(c2.score, c1.score));

        boolean isAllUpper = isAllUpperCase(rawPrefix);
        boolean isFirstUpper = !isAllUpper && Character.isUpperCase(rawPrefix.charAt(0));

        final List<String> results = new ArrayList<>();
        for (Candidate c : candidates) {
            String word = c.word;
            if (isAllUpper) {
                word = word.toUpperCase();
            } else if (isFirstUpper) {
                word = Character.toUpperCase(word.charAt(0)) + word.substring(1);
            }
            results.add(word);
            if (results.size() >= maxSuggestions) {
                break;
            }
        }
        return results;
    }

    private void collectPrefixCandidates(final byte[] prefixBytes, final float scoreMultiplier,
                                         final List<Candidate> output, final Set<String> seenWords,
                                         final int limit) {
        int firstIndex = findFirstMatchingIndex(prefixBytes);
        if (firstIndex < 0 || firstIndex >= mWordCount) {
            return;
        }

        int collected = 0;
        for (int i = firstIndex; i < mWordCount && collected < limit; i++) {
            int offset = mBuffer.getInt(8 + i * 4);
            int pos = mDataStart + offset;
            int len = mBuffer.get(pos) & 0xFF;
            if (!matchesPrefix(pos + 1, len, prefixBytes)) {
                break;
            }
            int freq = mBuffer.get(pos + 1 + len) & 0xFF;
            byte[] wBytes = new byte[len];
            for (int b = 0; b < len; b++) {
                wBytes[b] = mBuffer.get(pos + 1 + b);
            }
            String word = new String(wBytes, StandardCharsets.UTF_8);
            if (seenWords.add(word)) {
                output.add(new Candidate(word, (int) (freq * scoreMultiplier)));
                collected++;
            }
        }
    }

    private int findFirstMatchingIndex(final byte[] prefixBytes) {
        int low = 0;
        int high = mWordCount - 1;
        int result = mWordCount;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = compareWithPrefix(mid, prefixBytes);
            if (cmp >= 0) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    private int compareWithPrefix(final int wordIndex, final byte[] prefixBytes) {
        int offset = mBuffer.getInt(8 + wordIndex * 4);
        int pos = mDataStart + offset;
        int len = mBuffer.get(pos) & 0xFF;
        int minLen = Math.min(len, prefixBytes.length);

        for (int i = 0; i < minLen; i++) {
            int b1 = mBuffer.get(pos + 1 + i) & 0xFF;
            int b2 = prefixBytes[i] & 0xFF;
            if (b1 != b2) {
                return b1 - b2;
            }
        }
        return len - prefixBytes.length;
    }

    private boolean matchesPrefix(final int wordContentPos, final int wordLen, final byte[] prefixBytes) {
        if (wordLen < prefixBytes.length) {
            return false;
        }
        for (int i = 0; i < prefixBytes.length; i++) {
            if (mBuffer.get(wordContentPos + i) != prefixBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllUpperCase(final String s) {
        if (s == null || s.length() <= 1) return false;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i)) && !Character.isUpperCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
