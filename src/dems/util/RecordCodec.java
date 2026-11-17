package dems.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a record (several text fields) into one line of a text file and back.
 * Fields are separated by TAB; tabs, new lines and backslashes inside a field are escaped.
 */
public final class RecordCodec {

    private RecordCodec() {
    }

    public static String encode(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('\t');
            }
            sb.append(escape(fields[i] == null ? "" : fields[i]));
        }
        return sb.toString();
    }

    public static String[] decode(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                char next = line.charAt(++i);
                switch (next) {
                    case 't':
                        current.append('\t');
                        break;
                    case 'n':
                        current.append('\n');
                        break;
                    case 'r':
                        current.append('\r');
                        break;
                    default:
                        current.append(next);
                }
            } else if (c == '\t') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }
}
