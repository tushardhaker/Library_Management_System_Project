import java.util.Scanner;
import java.sql.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class LMS {
    private static final Scanner scanner = new Scanner(System.in);
    private static String loggedInRole = "";
    private static String loggedInUserId = "";

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n***********************************************");
            System.out.println("  WELCOME TO THE LIBRARY MANAGEMENT SYSTEM  ");
            System.out.println("***********************************************");
            System.out.println("[1] Register");
            System.out.println("[2] Login");
            System.out.println("[3] Exit");
            System.out.print("Enter choice: ");

            int choice = getValidIntInput();

            switch (choice) {
                case 1 -> userauthentication.registerUser();
                case 2 -> {
                    if (loginUser()) runTransactionSystem();
                }
                case 3 -> {
                    System.out.println("Exiting... Thank you for using the system!");
                    return;
                }
                default -> System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private static boolean loginUser() {
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        String query = "SELECT Role FROM User WHERE email = ? AND Password = ?";

        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                loggedInRole = rs.getString("Role").toUpperCase();
                loggedInUserId = email;
                System.out.println("Login successful as " + loggedInRole + "!");
                return true;
            } else {
                System.out.println("Invalid credentials! Try again.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error during login: " + e.getMessage());
            return false;
        }
    }

    private static void runTransactionSystem() {
        while (true) {
            System.out.println("\n******************************************");
            System.out.println(" LIBRARY MANAGEMENT MENU ");
            System.out.println("******************************************");
            System.out.println("[1] View Books");
            System.out.println("[2] Issue Book");
            System.out.println("[3] Return Book");
            System.out.println("[4] Search Book");
            System.out.println("[5] Submit Feedback");
            System.out.println("[6] Change Password");

            if (loggedInRole.equals("ADMIN") || loggedInRole.equals("SUPER_ADMIN")) {
                System.out.println("[7] Add Book");
                System.out.println("[8] Delete Book");
                System.out.println("[9] View Users");
                System.out.println("[10] Delete User");
                System.out.println("[11] View Issued Books");
                System.out.println("[12] View Feedback");
            }

            if (loggedInRole.equals("SUPER_ADMIN")) {
                System.out.println("[13] Delete Admin");
            }

            System.out.println("[0] Logout");
            System.out.print("Enter choice: ");
            int choice = getValidIntInput();

            switch (choice) {
                case 1 -> Transaction.viewBooks();
                case 2 -> Transaction.issueBook();
                case 3 -> Transaction.returnBook();
                case 4 -> Transaction.searchBook();
                case 5 -> submitFeedback();
                case 6 -> changePassword();
                case 7 -> {
                    if (isAdmin()) Transaction.addBook();
                    else invalidAccess();
                }
                case 8 -> {
                    if (isAdmin()) Transaction.deleteBook();
                    else invalidAccess();
                }
                case 9 -> {
                    if (isAdmin()) userauthentication.viewUsers();
                    else invalidAccess();
                }
                case 10 -> {
                    if (isAdmin()) userauthentication.deleteUser();
                    else invalidAccess();
                }
                case 11 -> {
                    if (isAdmin()) Transaction.viewIssuedBooks();
                    else invalidAccess();
                }
                case 12 -> {
                    if (isAdmin()) viewFeedback();
                    else invalidAccess();
                }
                case 13 -> {
                    if (loggedInRole.equals("SUPER_ADMIN")) deleteAdmin();
                    else invalidAccess();
                }
                case 0 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice! Try again.");
            }
        }
    }

    private static boolean isAdmin() {
        return loggedInRole.equals("ADMIN") || loggedInRole.equals("SUPER_ADMIN");
    }

    private static void invalidAccess() {
        System.out.println("Access Denied! You don't have permission for this action.");
    }

    private static void deleteAdmin() {
        System.out.print("Enter Admin email to delete: ");
        String adminEmail = scanner.nextLine();

        String checkQuery = "SELECT Role FROM User WHERE email = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(checkQuery)) {
            stmt.setString(1, adminEmail);
            var rs = stmt.executeQuery();

            if (!rs.next() || !rs.getString("Role").equalsIgnoreCase("ADMIN")) {
                System.out.println("Error: Either user does not exist or is not an Admin!");
                return;
            }
        } catch (Exception e) {
            System.out.println("Error verifying user: " + e.getMessage());
            return;
        }

        String deleteQuery = "DELETE FROM User WHERE email = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setString(1, adminEmail);
            stmt.executeUpdate();
            System.out.println("Admin deleted successfully!");
        } catch (Exception e) {
            System.out.println("Error deleting Admin: " + e.getMessage());
        }
    }

    private static void submitFeedback() {
        System.out.println("\nEnter your feedback:");
        String feedback = scanner.nextLine().trim();

        if (feedback.isEmpty()) {
            System.out.println("Feedback cannot be empty!");
            return;
        }

        String query = "INSERT INTO Feedback (email, Feedback) VALUES (?, ?)";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loggedInUserId);
            stmt.setString(2, feedback);
            stmt.executeUpdate();
            System.out.println("Thank you for your feedback!");
        } catch (Exception e) {
            System.out.println("Error submitting feedback: " + e.getMessage());
        }
    }

    private static void viewFeedback() {
        System.out.println("\n****** User Feedback ******");
        String query = "SELECT email, Feedback, Timestamp FROM Feedback ORDER BY Timestamp DESC";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(query);
             var rs = stmt.executeQuery()) {
            boolean hasFeedback = false;
            while (rs.next()) {
                hasFeedback = true;
                System.out.println("\nEmail: " + rs.getString("email"));
                System.out.println("Feedback: " + rs.getString("Feedback"));
                System.out.println("Date: " + rs.getTimestamp("Timestamp"));
                System.out.println("---------------------------");
            }
            if (!hasFeedback) {
                System.out.println("No feedback found.");
            }
        } catch (Exception e) {
            System.out.println("Error retrieving feedback: " + e.getMessage());
        }
    }

    private static void changePassword() {
        System.out.print("\nEnter your current password: ");
        String currentPassword = scanner.nextLine();

        String query = "SELECT Password FROM User WHERE email = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loggedInUserId);
            var rs = stmt.executeQuery();

            if (!rs.next() || !rs.getString("Password").equals(currentPassword)) {
                System.out.println("Incorrect current password! Try again.");
                return;
            }
        } catch (Exception e) {
            System.out.println("Error verifying password: " + e.getMessage());
            return;
        }

        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine();

        // Validate the new password
        if (!isValidPassword(newPassword)) {
            System.out.println("Password must be at least 8 characters long and include at least one letter, one digit, and one special character.");
            return;
        }

        query = "UPDATE User SET Password = ? WHERE email = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, loggedInUserId);
            stmt.executeUpdate();
            System.out.println("Password changed successfully!");
        } catch (Exception e) {
            System.out.println("Error updating password: " + e.getMessage());
        }
    }

    private static boolean isValidPassword(String password) {
        // Regular expression to check password criteria
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private static int getValidIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input! Enter a number: ");
            }
        }
    }
}
