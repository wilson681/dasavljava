package utility;

/**
 * ValidationUtility.java
 * 通用输入格式校验——同一种输入(会员ID、确认号、电话号码)不管在哪个模块出现,
 * 都靠这里同一套规则判断合不合法,避免各模块各自写一套导致标准不一致。
 *
 * @author 某某
 */
public class ValidationUtility {

    private ValidationUtility() {
    }

    /**
     * 判断字串是不是空的(null 或去掉头尾空白后变成空字串)。
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 判断字串是不是刚好8位数字——确认号(confirmation number)的合法格式。
     */
    public static boolean isEightDigitNumber(String value) {
        return value != null && value.trim().matches("\\d{8}");
    }

    /**
     * 判断字串是不是只由数字组成(至少一位)——电话号码只要求这个程度的合法性,
     * 不做区号/长度这类更严谨的格式检查。
     */
    public static boolean isDigitsOnly(String value) {
        return value != null && value.trim().matches("\\d+");
    }
}
