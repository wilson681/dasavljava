package dao;

import adt.ListInterface;
import entity.Member;
import entity.PointsLedgerEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import utility.DataFileLocator;

/**
 * PointsLedgerDao.java - 负责把 data/points_ledger.txt 读进来,组成
 * PointsLedgerEntry,加进 memberList 里对应会员的积分明细清单。
 *
 * @author 某某
 *
 * 说明:
 * - 只做"读文件、组物件、塞进容器"这件事,不做任何业务判断
 * - 必须在 MemberDao 之后跑,靠线性找 memberList 拿到对应的 Member 物件
 * - txt格式: memberId,pointsAmount,earnedDate,expiryDate
 * - 不碰 Member.currentPoints/totalPointsEarned——那两个已经由 members.txt
 *   直接设定好了,这里只是补充"明细"给到期提醒报表用,不重新加总
 * - ledgerId 前缀 SPL,不会跟真实加分产生的 PL 编号撞号
 */
public class PointsLedgerDao {

    private static final String FILE_PATH = "data/points_ledger.txt";

    private int ledgerCounter = 0;

    public void loadPointsLedger(ListInterface<Member> memberList) {
        File file = DataFileLocator.locate(PointsLedgerDao.class, FILE_PATH);
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
                int pointsAmount = Integer.parseInt(fields[1].trim());
                String earnedDate = fields[2].trim();
                String expiryDate = fields[3].trim();

                Member member = findMemberById(memberList, memberId);
                if (member == null) {
                    System.out.println("Skipped " + FILE_PATH + " row: no member found for id " + memberId);
                    continue;
                }

                ledgerCounter++;
                String ledgerId = "SPL" + String.format("%06d", ledgerCounter);
                member.addPointsEntry(new PointsLedgerEntry(ledgerId, memberId, pointsAmount, earnedDate, expiryDate));
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + FILE_PATH + ": " + e.getMessage());
        }
    }

    private Member findMemberById(ListInterface<Member> memberList, String memberId) {
        Iterator<Member> iterator = memberList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (member.getMemberId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }
}
