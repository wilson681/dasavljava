package entity;

/**
 * Member.java - Entity class representing a loyalty programme member.
 *
 * @author Wilson
 */
public class Member {

    private String memberId;
    private String name;
    private Tier tier;
    private int points;
    private boolean active;

    public Member(String memberId, String name, Tier tier, int points, boolean active) {
        this.memberId = memberId;
        this.name = name;
        this.tier = tier;
        this.points = points;
        this.active = active;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tier getTier() {
        return tier;
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Member other = (Member) obj;
        return this.memberId.equals(other.memberId);
    }

    @Override
    public int hashCode() {
        return memberId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-10s %-25s %-10s %8d", memberId, name, tier, points);
    }
}