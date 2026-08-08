package utility;

import java.io.File;
import java.net.URISyntaxException;

/**
 * DataFileLocator.java
 * Utility 类 —— 找出 data/ 底下的资料档实际在磁盘上的位置。
 *
 * @author 某某
 *
 * 说明:
 * - 只含 static 方法,没有任何状态,符合 Utility 类规范
 * - 不能直接把"data/members.txt"这种路径丢给FileReader——那是相对"程序执行时的工作目录",
 *   NetBeans、VSCode、命令行各自默认的工作目录不一样,同一份代码换个方式启动就读不到档案
 * - 改成从anchorClass编译后.class档实际躺在磁盘的位置(一定在专案文件夹里面,不管是
 *   NetBeans的build/classes、VSCode的bin、还是打包后的dist/xxx.jar)往上层一层一层找,
 *   直到某一层底下真的存在这个相对路径,这样就完全不用管程序是从哪个工作目录被启动的
 */
public final class DataFileLocator {

    private DataFileLocator() {
        // 不给外部 new 出来,纯 static 工具类
    }

    /**
     * 从anchorClass实际所在位置往上层目录找,定位relativePath指到的档案。
     * @param anchorClass 用来定位"程序实际跑在磁盘哪里"的class(通常传呼叫方自己的.class)
     * @param relativePath 相对专案根目录的路径,例如 "data/members.txt"
     * @return 找到的File;找不到就回传null,交给呼叫方自行处理
     */
    public static File locate(Class<?> anchorClass, String relativePath) {
        try {
            File location = new File(anchorClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = location.isFile() ? location.getParentFile() : location;
            while (dir != null) {
                File candidate = new File(dir, relativePath);
                if (candidate.exists()) {
                    return candidate;
                }
                dir = dir.getParentFile();
            }
        } catch (URISyntaxException | NullPointerException e) {
            // 反推不出实际磁盘位置,交给呼叫方自己处理(通常是印警告、维持容器空的)
        }
        return null;
    }
}
