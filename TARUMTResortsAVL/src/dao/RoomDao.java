package dao;

import adt.ListInterface;
import entity.Room;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * RoomDao.java - 负责把 data/rooms.txt 读进来,组成 Room 物件,加进传进来的 roomList。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断(不属于Entity/Boundary/Control)
 * - txt格式: roomNumber,roomType,nightlyRate,status(跟 Room 构造函数的参数顺序一样)
 */
public class RoomDao {

    private static final String FILE_PATH = "data/rooms.txt";

    /**
     * 读取 rooms.txt,把每一行组成一个 Room,加进 roomList。
     * @param roomList 要把读到的Room塞进去的容器
     */
    public void loadRooms(ListInterface<Room> roomList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",");
                String roomNumber = fields[0].trim();
                String roomType = fields[1].trim();
                double nightlyRate = Double.parseDouble(fields[2].trim());
                String status = fields[3].trim();
                roomList.add(new Room(roomNumber, roomType, nightlyRate, status));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
