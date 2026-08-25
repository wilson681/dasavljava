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
    /**
     * 判断字串是不是合法的人名——只允许字母、空格,以及名字里常见的
     * 撇号(O'Brien)、连字号(Anne-Marie)、句点(Jr.)和斜线(A/L、A/P)。
     * 数字和其他符号一律拒绝。
     */
    public static boolean isValidName(String value) {

        if (isBlank(value)) {
            return false;
        }

        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean allowed = Character.isLetter(c)
                    || c == ' ' || c == '\'' || c == '-' || c == '.' || c == '/';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
    /**
     * 比较两个识别码(比如会员ID)是不是视为同一个,不分大小写——使用者手动输入ID
     * 查找资料时,大小写不该影响查得到查不到。null-safe:任一边是null就视为不相符。
     */
    public static boolean idsMatch(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    /**
     * 校验日期输入,统一只收 "yyyy-MM-dd" 这一种格式——资料档里存的就是这个格式,
     * 报表画面上显示的也是这个格式,输入跟着一致,不另外支援别种写法。
     *
     * 会真的检查这一天存不存在,不只是看长相:2026-02-30 长得像日期,但2月没有30号,
     * 这种一样判定不合法。光用正则 \\d{4}-\\d{2}-\\d{2} 检查是挡不掉的。
     *
     * @param value 使用者输入的字串
     * @return 合法就回传去掉头尾空白的日期字串;格式不对或这一天不存在时回传 null
     */
    public static String normalizeDate(String value) {

        if (isBlank(value)) {
            return null;
        }

        String trimmed = value.trim();

        if (!trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }

        try {
            // parse 成功才代表这一天真的存在(会挡掉 2026-02-30、2026-13-01 这种)
            java.time.LocalDate.parse(trimmed);
            return trimmed;
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 判断使用者打的日期合不合法(格式对、而且这一天真的存在)。
     * 只需要判断真假、不需要拿回日期字串时用这个。
     */
    public static boolean isValidDate(String value) {
        return normalizeDate(value) != null;
    }
}
