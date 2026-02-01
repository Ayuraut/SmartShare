package backend.models;

public class Expense {
    private int id;
    private int groupId;
    private int payerId;
    private double amount;
    private String description;

    public Expense(int id, int groupId, int payerId, double amount, String description) {
        this.id = id;
        this.groupId = groupId;
        this.payerId = payerId;
        this.amount = amount;
        this.description = description;
    }

    public int getId() { return id; }
    public int getGroupId() { return groupId; }
    public int getPayerId() { return payerId; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}
