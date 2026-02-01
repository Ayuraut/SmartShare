CREATE DATABASE smartshare;
USE smartshare;

CREATE TABLE `groups` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    group_id INT,
    FOREIGN KEY (group_id) REFERENCES `groups`(id)
);

CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT,
    payer_id INT,
    amount DOUBLE,
    description VARCHAR(255),
    FOREIGN KEY (group_id) REFERENCES `groups`(id),
    FOREIGN KEY (payer_id) REFERENCES users(id)
);

CREATE TABLE expense_participants (
    expense_id INT,
    user_id INT,
    FOREIGN KEY (expense_id) REFERENCES expenses(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
