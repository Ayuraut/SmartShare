package backend;

import java.sql.*;
import java.util.*;

public class ExpenseManager {

    public static void addExpense(String groupName, String payerName, double amount, List<String> participantNames) {
        try (Connection conn = DBConnection.getConnection()) {
            // Ensure group and user exist before adding expense
            int groupId = getGroupId(conn, groupName);
            int payerId = getUserId(conn, payerName, groupId);

            // Insert expense
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO expenses (payer_id, group_id, amount) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, payerId);
            stmt.setInt(2, groupId);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            int expenseId = rs.getInt(1);

            // Divide amount among participants
            double share = amount / participantNames.size();
            for (String name : participantNames) {
                int userId = getUserId(conn, name, groupId);
                stmt = conn.prepareStatement("INSERT INTO expense_users (expense_id, user_id, share) VALUES (?, ?, ?)");
                stmt.setInt(1, expenseId);
                stmt.setInt(2, userId);
                stmt.setDouble(3, share);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getBalances(String groupName) {
        List<String> balances = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            int groupId = getGroupId(conn, groupName);
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.name, " +
                    "SUM(CASE WHEN e.payer_id = u.id THEN e.amount ELSE 0 END) - " +
                    "SUM(COALESCE(eu.share, 0)) AS balance " +
                    "FROM users u " +
                    "LEFT JOIN expenses e ON u.id = e.payer_id " +
                    "LEFT JOIN expense_users eu ON eu.user_id = u.id " +
                    "LEFT JOIN expenses e2 ON eu.expense_id = e2.id " +
                    "WHERE u.group_id = ? " +
                    "GROUP BY u.name");
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                double balance = rs.getDouble("balance");
                balances.add(name + ": " + balance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balances;
    }

    public static List<String> getSettlements(String groupName) {
        List<String> settlements = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            int groupId = getGroupId(conn, groupName);
            Map<String, Double> balances = new HashMap<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.name, " +
                    "SUM(CASE WHEN e.payer_id = u.id THEN e.amount ELSE 0 END) - " +
                    "SUM(COALESCE(eu.share, 0)) AS balance " +
                    "FROM users u " +
                    "LEFT JOIN expenses e ON u.id = e.payer_id " +
                    "LEFT JOIN expense_users eu ON eu.user_id = u.id " +
                    "LEFT JOIN expenses e2 ON eu.expense_id = e2.id " +
                    "WHERE u.group_id = ? " +
                    "GROUP BY u.name");
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                balances.put(rs.getString("name"), rs.getDouble("balance"));
            }

            // Separate debtors and creditors
            PriorityQueue<Map.Entry<String, Double>> debtors = new PriorityQueue<>(Map.Entry.comparingByValue());
            PriorityQueue<Map.Entry<String, Double>> creditors = new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                if (entry.getValue() < 0) debtors.add(entry);
                else if (entry.getValue() > 0) creditors.add(entry);
            }

            while (!debtors.isEmpty() && !creditors.isEmpty()) {
                Map.Entry<String, Double> debtor = debtors.poll();
                Map.Entry<String, Double> creditor = creditors.poll();

                double min = Math.min(-debtor.getValue(), creditor.getValue());
                settlements.add(debtor.getKey() + " pays " + min + " to " + creditor.getKey());

                double debtorNew = debtor.getValue() + min;
                double creditorNew = creditor.getValue() - min;

                if (debtorNew < 0) debtors.add(Map.entry(debtor.getKey(), debtorNew));
                if (creditorNew > 0) creditors.add(Map.entry(creditor.getKey(), creditorNew));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settlements;
    }

    // ✅ NEW METHODS

    private static int getGroupId(Connection conn, String groupName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM groups WHERE name = ?");
        stmt.setString(1, groupName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");

        stmt = conn.prepareStatement("INSERT INTO groups (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, groupName);
        stmt.executeUpdate();
        rs = stmt.getGeneratedKeys();
        rs.next();
        return rs.getInt(1);
    }

    private static int getUserId(Connection conn, String userName, int groupId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE name = ? AND group_id = ?");
        stmt.setString(1, userName);
        stmt.setInt(2, groupId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");

        stmt = conn.prepareStatement("INSERT INTO users (name, group_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, userName);
        stmt.setInt(2, groupId);
        stmt.executeUpdate();
        rs = stmt.getGeneratedKeys();
        rs.next();
        return rs.getInt(1);
    }
}
