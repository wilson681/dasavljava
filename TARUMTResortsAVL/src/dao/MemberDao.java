package dao;

import adt.ListInterface;
import entity.Member;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * MemberDao.java - 负责把 data/members.txt 读进来,组成 Member 物件,加进传进来的 memberList。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断(不属于Entity/Boundary/Control)
 * - txt格式: memberId,name,phone,tier,currentPoints,totalPointsEarned(跟 Member 构造函数一样)
 */
public class MemberDao {

    private static final String FILE_PATH = "data/members.txt";

    /**
     * 读取 members.txt,把每一行组成一个 Member,加进 memberList。
     * @param memberList 要把读到的Member塞进去的容器
     */
    public void loadMembers(ListInterface<Member> memberList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
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
