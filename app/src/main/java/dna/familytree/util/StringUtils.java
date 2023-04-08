package dna.familytree.util;

import androidx.annotation.NonNull;

/**
 * Created by rubansingh.john on 4/24/2021.
 */
public class StringUtils {

    public static final int TAG_ZERO = 0;
    public static final String TAG_EMPTY = "";
    public static final String TAG_SPACE = " ";
    public static final String TAG_NEWLINE = "\n";
    public static final String TAG_COLON = ":";

    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * Set first char in a String to uppercase.
     */
    public static String setFirstCharUpperCase(@NonNull String str) {
        return !str.isEmpty() ? str.substring(0, 1).toUpperCase() + str.substring(1) : str;
    }
}
