
import java.sql.*;
import java.util.Scanner;

public class Transaction {
    private static final Scanner scanner = new Scanner(System.in);

    // Issue Book - For both Users and Admins
    public static void issueBook() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine();
        System.out.print("Enter Book ID: ");
        String bookId = scanner.nextLine();

        String bookQuery = "SELECT Quantity FROM Books WHERE Book_id = ?";
        String issueQuery = "INSERT INTO IssuedBooks (User_ID, Book_id, IssueDate) VALUES (?, ?, NOW())";
        String updateQuery = "UPDATE Books SET Quantity = Quantity - 1 WHERE Book_ID = ?";

        try (PreparedStatement bookStmt = conn.prepareStatement(bookQuery)) {
            bookStmt.setString(1, bookId);
            ResultSet bookRs = bookStmt.executeQuery();

            if (!bookRs.next() || bookRs.getInt("Quantity") <= 0) {
                System.out.println("Book is out of stock!");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error while checking book availability: " + e.getMessage());
            return;
        }

        try (PreparedStatement issueStmt = conn.prepareStatement(issueQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

            issueStmt.setString(1, userId);
            issueStmt.setString(2, bookId);
            issueStmt.executeUpdate();

            updateStmt.setString(1, bookId);
            updateStmt.executeUpdate();

            System.out.println("Book issued successfully!");
        } catch (SQLException e) {
            System.err.println("Error while issuing book: " + e.getMessage());
        }
    }

    public static void deleteBook(String loggedInRole) {
        if (!"ADMIN".equalsIgnoreCase(loggedInRole)) {
            System.out.println("Only admins can delete books!");
            return;
        }

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        System.out.print("Enter Book ID to delete: ");
        String bookId = scanner.nextLine();

        String deleteQuery = "DELETE FROM Books WHERE Book_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setString(1, bookId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Book deleted successfully!");
            } else {
                System.out.println("No book found with the given ID.");
            }
        } catch (SQLException e) {
            System.err.println("Error while deleting book: " + e.getMessage());
        }
    }
    // Return Book - For both Users and Admins
    public static void returnBook() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine();
        System.out.print("Enter Book ID: ");
        String bookId = scanner.nextLine();

        String returnQuery = "UPDATE IssuedBooks SET ReturnDate = NOW() WHERE User_ID = ? AND Book_id = ? AND ReturnDate IS NULL LIMIT 1";
        String updateQuery = "UPDATE Books SET Quantity = Quantity + 1 WHERE Book_id = ?";

        try (PreparedStatement returnStmt = conn.prepareStatement(returnQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

            returnStmt.setString(1, userId);
            returnStmt.setString(2, bookId);
            int rowsUpdated = returnStmt.executeUpdate();

            if (rowsUpdated > 0) {
                updateStmt.setString(1, bookId);
                updateStmt.executeUpdate();
                System.out.println("Book returned successfully!");
            } else {
                System.out.println("No issued record found for this book.");
            }
        } catch (SQLException e) {
            System.err.println("Error while returning book: " + e.getMessage());
        }
    }
    // View Book -  For both Users and Admins
    public static void viewBooks() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        String query = "SELECT Book_id, Title, Author, Quantity FROM Books";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            if (!rs.isBeforeFirst()) {
                System.out.println("No books found.");
                return;
            }

            System.out.println("Book_ID |     Title            | Author            | Quantity");
            System.out.println("-------------------------------------------------------------");

            while (rs.next()) {
                String bookId = rs.getString("Book_ID");
                String title = rs.getString("Title");
                String author = rs.getString("Author");
                int quantity = rs.getInt("Quantity");

                System.out.printf("%-8s | %-20s | %-15s | %d%n", bookId, title, author, quantity);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving books: " + e.getMessage());
        }
    }
    // Add Book - Only For Admins
    public static void addBook() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        System.out.print("Enter Book ID: ");
        String bookId = scanner.nextLine();
        System.out.print("Enter Book Title: ");
        String title = scanner.nextLine();
        System.out.print("Enter Author Name: ");
        String author = scanner.nextLine();
        System.out.print("Enter Quantity: ");
        int quantity = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        // Check if the book already exists
        String checkQuery = "SELECT Quantity FROM Books WHERE Book_ID = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            checkStmt.setString(1, bookId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // If book exists, update its quantity instead of inserting a duplicate entry
                int existingQuantity = rs.getInt("Quantity");
                int newQuantity = existingQuantity + quantity;

                String updateQuery = "UPDATE Books SET Quantity = ? WHERE Book_ID = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, newQuantity);
                    updateStmt.setString(2, bookId);
                    updateStmt.executeUpdate();
                    System.out.println("Book quantity updated successfully!");
                }
            } else {
                // If book does not exist, insert it as a new entry
                String insertQuery = "INSERT INTO Books (Book_ID, Title, Author, Quantity) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, bookId);
                    insertStmt.setString(2, title);
                    insertStmt.setString(3, author);
                    insertStmt.setInt(4, quantity);
                    insertStmt.executeUpdate();
                    System.out.println("Book added successfully!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while adding book: " + e.getMessage());
        }
    }
    // Delete Book - Only For Admins
    public static void deleteBook() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        System.out.print("Enter Book ID to delete: ");
        String bookId = scanner.nextLine();

        String deleteQuery = "DELETE FROM Books WHERE Book_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setString(1, bookId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Book deleted successfully!");
            } else {
                System.out.println("No book found with the given ID.");
            }
        } catch (SQLException e) {
            System.err.println("Error while deleting book: " + e.getMessage());
        }
    }
    // Search Book - For both Users and Admins
    public static void searchBook() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        System.out.print("Enter Book Title or Author to search: ");
        String keyword = scanner.nextLine();

        String query = "SELECT * FROM Books WHERE Title LIKE ? OR Author LIKE ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("No books found matching the keyword.");
                return;
            }

            System.out.println("Book_ID | Title | Author | Quantity");
            while (rs.next()) {
                System.out.printf("%s   | %s    | %s     | %d%n", rs.getString("Book_ID"), rs.getString("Title"),
                        rs.getString("Author"), rs.getInt("Quantity"));
            }
        } catch (SQLException e) {
            System.err.println("Error while searching books: " + e.getMessage());
        }
    }
    // ViewIssued Book - Only Book Admins
    public static void viewIssuedBooks() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            System.out.println("Database connection is not available.");
            return;
        }

        String query = "SELECT * FROM IssuedBooks";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            if (!rs.isBeforeFirst()) {
                System.out.println("No issued books found.");
                return;
            }

            System.out.println("User_ID | Book_ID | IssueDate | ReturnDate");
            while (rs.next()) {
                System.out.printf("%s   | %s      | %s      | %s%n", rs.getString("User_ID"), rs.getString("Book_id"),
                        rs.getString("IssueDate"),
                        rs.getString("ReturnDate") != null ? rs.getString("ReturnDate") : "Not Returned");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving issued books: " + e.getMessage());
        }
    }
}

