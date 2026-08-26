package dao;

import adt.ListInterface;
import entity.Member;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import utility.DataFileLocator;

/**
 * MemberDao.java - reads data/members.txt, builds Member objects, and adds
 * them to the given memberList.
 *
 * Notes:
 * - Only reads the file, builds objects, and stores them in the container -
 *   no business logic here (not part of Entity/Boundary/Control).
 * - txt format: memberId,name,phone,tier,currentPoints,totalPointsEarned
 *   (same order as the Member constructor).
 * - The path is resolved via DataFileLocator instead of the runtime working
 *   directory, so the file is found no matter which IDE or working
 *   directory starts the program.
 */
public class MemberDao {

    private static final String FILE_PATH = "data/members.txt";

    /**
     * Reads members.txt, turns each line into a Member, and adds it to
     * memberList.
     * @param memberList the container to add the loaded Member objects into
     */
    public void loadMembers(ListInterface<Member> memberList) {
        File file = DataFileLocator.locate(MemberDao.class, FILE_PATH);
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
                String memberId = fields[0].trim();
                String name = fields[1].trim();
                String phone = fields[2].trim();
                String tier = fields[3].trim();
                int currentPoints = Integer.parseInt(fields[4].trim());
                int totalPointsEarned = Integer.parseInt(fields[5].trim());
                memberList.add(new Member(memberId, name, phone, tier, currentPoints, totalPointsEarned));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }
}
