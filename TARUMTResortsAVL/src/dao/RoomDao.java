package dao;

import adt.ListInterface;
import entity.Room;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * RoomDao.java - reads data/rooms.txt, builds Room objects, and adds them
 * to the given roomList.
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here (not part of Entity/Boundary/Control).
 * - txt format: roomNumber,roomType,nightlyRate,status (same order as the
 *   Room constructor).
 * - The path is resolved via DataFileLocator instead of the runtime working
 *   directory, so the file is found no matter which IDE or working
 *   directory starts the program.
 *
 * @author All
 */
public class RoomDao {

    private static final String FILE_PATH = "data/rooms.txt";

    /**
     * Reads rooms.txt, turns each line into a Room, and adds it to roomList.
     * @param roomList the container to add the loaded Room objects into
     */
    public void loadRooms(ListInterface<Room> roomList) {
        File file = DataFileLocator.locate(RoomDao.class, FILE_PATH);
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
