package utils;

public class PinYinUtils {
    public static String toPinYin(String input) {
        return input;
        //暂时不做拼音隐藏
        //return Pinyin.toPinyin(input,"_").toLowerCase();
    }

    public static String[] toPinYin(String[] inputs) {
        if (null == inputs) {
            return null;
        }
        String[] result = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            result[i] = toPinYin(inputs[i]);
        }
        return result;
    }
}
