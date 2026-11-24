package com.chirag.home;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class VoiceJson {
    private int i;
    private final String s;

    private VoiceJson(String str) {
        this.s = str;
    }

    static Object parse(String str) {
        VoiceJson voiceJson = new VoiceJson(str);
        voiceJson.ws();
        Object objValue = voiceJson.value();
        voiceJson.ws();
        if (voiceJson.i < voiceJson.s.length()) {
            throw voiceJson.err("trailing characters");
        }
        return objValue;
    }

    static Object tryParse(String str) {
        try {
            return parse(str);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static Object at(Object obj, Object... objArr) {
        for (Object obj2 : objArr) {
            if ((obj2 instanceof String) && (obj instanceof Map)) {
                obj = ((Map) obj).get(obj2);
            } else {
                if (!(obj2 instanceof Integer) || !(obj instanceof List)) {
                    return null;
                }
                List list = (List) obj;
                int iIntValue = ((Integer) obj2).intValue();
                obj = (iIntValue < 0 || iIntValue >= list.size()) ? null : list.get(iIntValue);
            }
            if (obj == null) {
                return null;
            }
        }
        return obj;
    }

    static String str(Object obj, Object... objArr) {
        Object objAt = at(obj, objArr);
        if (objAt == null) {
            return "";
        }
        if (!(objAt instanceof Double)) {
            return ((objAt instanceof String) || (objAt instanceof Boolean)) ? String.valueOf(objAt) : "";
        }
        double dDoubleValue = ((Double) objAt).doubleValue();
        return (dDoubleValue != Math.rint(dDoubleValue) || Math.abs(dDoubleValue) >= 1.0E15d) ? String.valueOf(dDoubleValue) : String.valueOf((long) dDoubleValue);
    }

    static double num(Object obj, Object... objArr) {
        Object objAt = at(obj, objArr);
        if (objAt instanceof Double) {
            return ((Double) objAt).doubleValue();
        }
        if (objAt instanceof String) {
            try {
                return Double.parseDouble((String) objAt);
            } catch (NumberFormatException e) {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    static boolean bool(Object obj, boolean z, Object... objArr) {
        Object objAt = at(obj, objArr);
        return objAt instanceof Boolean ? ((Boolean) objAt).booleanValue() : z;
    }

    static List<Object> list(Object obj, Object... objArr) {
        Object objAt = at(obj, objArr);
        return objAt instanceof List ? (List) objAt : new ArrayList();
    }

    static String quote(String str) {
        StringBuilder sbAppend = new StringBuilder(str.length() + 8).append('\"');
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            switch (cCharAt) {
                case '\t':
                    sbAppend.append("\\t");
                    break;
                case '\n':
                    sbAppend.append("\\n");
                    break;
                case '\r':
                    sbAppend.append("\\r");
                    break;
                case '\"':
                    sbAppend.append("\\\"");
                    break;
                case '\\':
                    sbAppend.append("\\\\");
                    break;
                default:
                    if (cCharAt < ' ') {
                        sbAppend.append(String.format("\\u%04x", Integer.valueOf(cCharAt)));
                    } else {
                        sbAppend.append(cCharAt);
                    }
                    break;
            }
        }
        return sbAppend.append('\"').toString();
    }

    private RuntimeException err(String str) {
        return new IllegalArgumentException("JSON: " + str + " at " + this.i);
    }

    private void ws() {
        while (this.i < this.s.length() && this.s.charAt(this.i) <= ' ') {
            this.i++;
        }
    }

    private Object value() {
        if (this.i >= this.s.length()) {
            throw err("unexpected end");
        }
        char cCharAt = this.s.charAt(this.i);
        if (cCharAt == '{') {
            return object();
        }
        if (cCharAt == '[') {
            return array();
        }
        if (cCharAt == '\"') {
            return string();
        }
        if (this.s.startsWith("true", this.i)) {
            this.i += 4;
            return Boolean.TRUE;
        }
        if (this.s.startsWith("false", this.i)) {
            this.i += 5;
            return Boolean.FALSE;
        }
        if (this.s.startsWith("null", this.i)) {
            this.i += 4;
            return null;
        }
        int i = this.i;
        while (this.i < this.s.length() && "+-0123456789.eE".indexOf(this.s.charAt(this.i)) >= 0) {
            this.i++;
        }
        if (i == this.i) {
            throw err("unexpected '" + cCharAt + "'");
        }
        try {
            return Double.valueOf(this.s.substring(i, this.i));
        } catch (NumberFormatException e) {
            throw err("bad number");
        }
    }

    private Map<String, Object> object() {
        char cCharAt;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        this.i++;
        ws();
        if (this.i < this.s.length() && this.s.charAt(this.i) == '}') {
            this.i++;
            return linkedHashMap;
        }
        do {
            ws();
            if (this.i >= this.s.length() || this.s.charAt(this.i) != '\"') {
                throw err("expected key");
            }
            String strString = string();
            ws();
            if (this.i >= this.s.length() || this.s.charAt(this.i) != ':') {
                throw err("expected ':'");
            }
            this.i++;
            ws();
            linkedHashMap.put(strString, value());
            ws();
            if (this.i >= this.s.length()) {
                throw err("unexpected end");
            }
            String str = this.s;
            int i = this.i;
            this.i = i + 1;
            cCharAt = str.charAt(i);
            if (cCharAt == '}') {
                return linkedHashMap;
            }
        } while (cCharAt == ',');
        throw err("expected ',' or '}'");
    }

    private List<Object> array() {
        char cCharAt;
        ArrayList arrayList = new ArrayList();
        this.i++;
        ws();
        if (this.i < this.s.length() && this.s.charAt(this.i) == ']') {
            this.i++;
            return arrayList;
        }
        do {
            ws();
            arrayList.add(value());
            ws();
            if (this.i >= this.s.length()) {
                throw err("unexpected end");
            }
            String str = this.s;
            int i = this.i;
            this.i = i + 1;
            cCharAt = str.charAt(i);
            if (cCharAt == ']') {
                return arrayList;
            }
        } while (cCharAt == ',');
        throw err("expected ',' or ']'");
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        this.i++;
        while (this.i < this.s.length()) {
            String str = this.s;
            int i = this.i;
            this.i = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"') {
                return sb.toString();
            }
            if (cCharAt == '\\') {
                if (this.i >= this.s.length()) {
                    throw err("bad escape");
                }
                String str2 = this.s;
                int i2 = this.i;
                this.i = i2 + 1;
                char cCharAt2 = str2.charAt(i2);
                switch (cCharAt2) {
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (this.i + 4 > this.s.length()) {
                            throw err("bad \\u");
                        }
                        try {
                            sb.append((char) Integer.parseInt(this.s.substring(this.i, this.i + 4), 16));
                            this.i += 4;
                        } catch (NumberFormatException e) {
                            throw err("bad \\u");
                        }
                        break;
                    default:
                        sb.append(cCharAt2);
                        break;
                }
            } else {
                sb.append(cCharAt);
            }
        }
        throw err("unterminated string");
    }
}
