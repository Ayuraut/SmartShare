package backend;

import java.sql.*;
import java.util.*;

public class GroupManager {

    // Create a group if it doesn't exist
    public static int createGroup(String groupName) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check if group exists
            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM groups WHERE name = ?");
            checkStmt.setString(1, groupName);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

            // Insert new group
            PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO groups (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, groupName);
            insertStmt.executeUpdate();

            rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Add a user to a group
    public static void addUserToGroup(String userName, String groupName) {
        try (Connection conn = DBConnection.getConnection()) {
            int groupId = createGroup(groupName);

            // Check if user exists in that group
            PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE name = ? AND group_id = ?");
            check.setString(1, userName);
            check.setInt(2, groupId);
            ResultSet rs = check.executeQuery();
            if (rs.next()) return; // already exists

            PreparedStatement insert = conn.prepareStatement("INSERT INTO users (name, group_id) VALUES (?, ?)");
            insert.setString(1, userName);
            insert.setInt(2, groupId);
            insert.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get all users in a group
    public static List<String> getUsersInGroup(String groupName) {
        List<String> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            int groupId = getGroupIdByName(conn, groupName);

            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE group_id = ?");
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private static int getGroupIdByName(Connection conn, String groupName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM groups WHERE name = ?");
        stmt.setString(1, groupName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        throw new SQLException("Group not found");
    }
}
