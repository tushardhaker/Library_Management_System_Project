import java.sql.*;
import java.util.Scanner;

public class userauthentication {
    private static final Connection connection = DatabaseConnection.getConnection();
    static Scanner scanner = new Scanner(System.in);

    private static final String SUPER_ADMIN_EMAIL = "example@gmail.com";
    private static final String ADMIN_SECURITY_KEY = "admin1234"; // Security key for Admin registration

    private static boolean isValidPassword(String password) {
        if (password.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false, hasSpecialChar = false;

        for (char ch : password.toCharArray()) {
            if (Character.isLetter(ch)) hasLetter = true;
            else if (Character.isDigit(ch)) hasDigit = true;
            else hasSpecialChar = true;

            if (hasLetter && hasDigit && hasSpecialChar) return true;
        }
        return false;
    }

    private static boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    public static void registerUser() {
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();

        String email;
        do {
            System.out.print("Enter Email: ");
            email = scanner.nextLine();
            if (!isValidEmail(email)) {
                System.out.println("Invalid email! Must contain '@' and '.' characters.");
            }
        } while (!isValidEmail(email));

        String password;
        do {
            System.out.print("Enter Password: ");
            password = scanner.nextLine();
            if (!isValidPassword(password)) {
                System.out.println("Invalid password! It must be at least 8 characters long and contain letters, digits, and special characters.");
            }
        } while (!isValidPassword(password));

        System.out.print("Enter Your Role (Super_Admin/Admin/User): ");
        String role = scanner.nextLine().toUpperCase();

        if (role.equals("SUPER_ADMIN") && !email.equals(SUPER_ADMIN_EMAIL)) {
            System.out.println("Only the designated SUPER_ADMIN email can register as SUPER_ADMIN.");
            return;
        }

        if (role.equals("ADMIN")) {
            System.out.print("Enter Security Key to Register as ADMIN: ");
            String securityKey = scanner.nextLine();
            if (!securityKey.equals(ADMIN_SECURITY_KEY)) {
                System.out.println("Invalid Security Key! Registration failed.");
                return;
            }
        }

        String query = "INSERT INTO User (Name, Email, Password, Role) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setString(4, role);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    System.out.println(role + " registered successfully! ");
                    System.out.println("Your User ID: " + userId);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during user registration: " + e.getMessage());
        }
    }

    public static void deleteUser() {
        System.out.print("Enter email to delete: ");
        String userEmail = scanner.nextLine();

        String adminEmail="";
        if (adminEmail.equals(SUPER_ADMIN_EMAIL)) {
            // SuperAdmin can delete anyone
            deleteUserByEmail(userEmail);
        } else {
            // Admin can delete only users, not other admins or super admin
            String query = "SELECT Role FROM User WHERE Email = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, userEmail);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String role = rs.getString("Role");
                    if (role.equals("USER")) {
                        deleteUserByEmail(userEmail);
                    } else {
                        System.out.println("Admins cannot delete other Admins or SuperAdmin.");
                    }
                } else {
                    System.out.println("User not found!");
                }
            } catch (SQLException e) {
                System.out.println("Error retrieving user role: " + e.getMessage());
            }
        }
    }

    private static void deleteUserByEmail(String userEmail) {
        String query = "DELETE FROM User WHERE Email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userEmail);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("User deleted successfully!");
            } else {
                System.out.println("User not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting user: " + e.getMessage());
        }
    }

    // User Login
    public static String[] login() {
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        String sql = "SELECT Name, Role FROM User WHERE Email = ? AND Password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String userRole = rs.getString("Role");
                System.out.println("Login Successful! Welcome " + rs.getString("Name") + " (" + userRole + ")");
                return new String[]{email, userRole};
            } else {
                System.out.println("Invalid Credentials!");
            }
        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
        }
        return null;
    }
    // Change Password method
    public static void changePassword(String email) {
        String newPassword;
        do {
            System.out.print("Enter New Password (at least 8 characters, including letters & digits): ");
            newPassword = scanner.nextLine();
            if (!isValidPassword(newPassword)) {
                System.out.println("Invalid password! Make sure it's at least 8 characters long and contains both letters & digits.");
            }
        } while (!isValidPassword(newPassword));

        String query = "UPDATE User SET Password = ? WHERE Email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, email);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Password changed successfully!");
            } else {
                System.out.println("User not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error changing password: " + e.getMessage());
        }
    }

    // Admin Menu
    public static void adminMenu(String userId) {
        while (true) {
            System.out.println("\nAdmin Menu");
            System.out.println("1. View Users");
            System.out.println("2. Delete User");
            System.out.println("3. Logout");
            System.out.print("Enter choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> viewUsers();
                case 2 ->deleteUser();
                case 3 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice! Try again.");
            }
        }
    }

    // User Menu
    public static void userMenu(String userId) {
        while (true) {
            System.out.println("\nUser Menu");
            System.out.println("1. Change Password");
            System.out.println("2. Logout");
            System.out.print("Enter choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> changePassword(userId);
                case 2 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice! Try again.");
            }
        }
    }

    // View All Users (Admin only)
    public static void viewUsers() {
        String query = "SELECT USER_ID, Name, Role FROM User";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\nUser List:");
            while (rs.next()) {
                System.out.println("USER_ID: " + rs.getString("USER_ID") +
                        " | Name: " + rs.getString("Name") +
                        " | Role: " + rs.getString("Role"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving users: " + e.getMessage());
        }
    }




    // Main Method
    public static void main(String[] args) {
        while (true) {
            System.out.println("\nLibrary Management System");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (choice == 1) {
                registerUser();
            } else if (choice == 2) {
                String[] loginData = login();
                if (loginData != null) {
                    String userId = loginData[0];
                    String role = loginData[1];

                    if (role.equalsIgnoreCase("ADMIN")) {
                        adminMenu(userId);
                    } else {
                        userMenu(userId);
                    }
                }
            } else {
                System.out.println("Exiting System. Goodbye!");
                break;
            }
        }
    }
} 
