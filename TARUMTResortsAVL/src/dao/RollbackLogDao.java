package dao;

import adt.ListInterface;
import entity.RollbackLogEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * RollbackLogDao.java - 负责把 data/rollback_log.txt 读进来,组成
 * RollbackLogEntry,加进 rollbackLog。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断
 * - txt格式: roomNumber,fromStatus,toStatus,date(跟构造函数参数顺序一样)
 */
public class RollbackLogDao {

    private static final String FILE_PATH = "data/rollback_log.txt";

    public void loadRollbackLog(ListInterface<RollbackLogEntry> rollbackLog) {
        File file = DataFileLocator.locate(RollbackLogDao.class, FILE_PATH);
        if (file == null) {
            System.out.println("Failed to load " + FILE_PATH + ": file not found near project root");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",");
                String roomNumber = fields[0].trim();
                String fromStatus = fields[1].trim();
                String toStatus = fields[2].trim();
                String date = fields[3].trim();
                rollbackLog.add(new RollbackLogEntry(roomNumber, fromStatus, toStatus, date));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
