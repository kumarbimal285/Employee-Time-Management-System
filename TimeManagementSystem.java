import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class TimeManagementSystem {

    // ---------- Data classes ----------
    static class TimeLog {
        String employee;
        LocalDate date;
        LocalTime start, end;

        TimeLog(String employee, LocalDate date, LocalTime start, LocalTime end) {
            this.employee = employee;
            this.date = date;
            this.start = start;
            this.end = end;
        }

        double hours() {
            return Duration.between(start, end).toMinutes() / 60.0;
        }

        public String toString() {
            return String.format("%s | %s | %s - %s | %.2f hrs", employee, date, start, end, hours());
        }
    }

   static class TimeOffRequest {
        int id;
        String employee;
        LocalDate from, to;
        String reason;
        String status = "PENDING";

        TimeOffRequest(int id, String employee, LocalDate from, LocalDate to, String reason) {
            this.id = id;
            this.employee = employee;
            this.from = from;
            this.to = to;
            this.reason = reason;
        }

        boolean covers(LocalDate d) {
            return !d.isBefore(from) && !d.isAfter(to);
        }

        public String toString() {
            return String.format("#%d | %s | %s to %s | %s | %s", id, employee, from, to, reason, status);
        }
    }

    // ---------- In-memory storage ----------
    static List<TimeLog> logs = new ArrayList<>();
    static List<TimeOffRequest> requests = new ArrayList<>();
    // employee -> (day of week -> "09:00-17:00" or "OFF")
    static Map<String, Map<DayOfWeek, String>> schedules = new HashMap<>();
    static int nextRequestId = 1;
    static Scanner sc = new Scanner(System.in);

    // ---------- Input helpers ----------
    static String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static LocalDate askDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(ask(prompt + " (yyyy-MM-dd): "));
            } catch (Exception e) {
                System.out.println("Invalid date. Try again.");
            }
        }
    }

    static LocalTime askTime(String prompt) {
        while (true) {
            try {
                return LocalTime.parse(ask(prompt + " (HH:mm): "));
            } catch (Exception e) {
                System.out.println("Invalid time. Try again.");
            }
        }
    }

    static int askInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(ask(prompt));
            } catch (Exception e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    static DayOfWeek askDay() {
        while (true) {
            try {
                return DayOfWeek.valueOf(ask("Day (e.g. MONDAY): ").toUpperCase());
            } catch (Exception e) {
                System.out.println("Invalid day. Try again.");
            }
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        System.out.println("=== Employee Time Management System ===");
        while (true) {
            System.out.println("\n1. HR Manager\n2. Employee\n3. Exit");
            String choice = ask("Choose user type: ");
            switch (choice) {
                case "1":
                    hrMenu();
                    break;
                case "2":
                    String name = ask("Enter your name: ");
                    if (name.isEmpty()) {
                        System.out.println("Name cannot be empty.");
                    } else {
                        employeeMenu(name);
                    }
                    break;
                case "3":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // ---------- Employee ----------
    static void employeeMenu(String name) {
        while (true) {
            System.out.println("\n--- Employee Dashboard (" + name + ") ---");
            System.out.println("1. Log work hours\n2. Request time off\n3. View schedule\n4. My time off requests\n5. Back");
            String c = ask("Choose: ");
            switch (c) {
                case "1":
                    logHours(name);
                    break;
                case "2":
                    requestTimeOff(name);
                    break;
                case "3":
                    viewSchedule(name);
                    break;
                case "4":
                    boolean any = false;
                    for (TimeOffRequest r : requests) {
                        if (r.employee.equalsIgnoreCase(name)) {
                            System.out.println(r);
                            any = true;
                        }
                    }
                    if (!any) System.out.println("No requests found.");
                    break;
                case "5":
                    return;
                default:
                    System.out.println("Invalid choice.");
            } 
        }
    }

    static void logHours(String name) {
        LocalDate date = askDate("Work date");
        LocalTime start = askTime("Start time");
        LocalTime end = askTime("End time");
        if (!end.isAfter(start)) {
            System.out.println("End time must be after start time.");
            return;
        }
        logs.add(new TimeLog(name, date, start, end));
        System.out.println("Work hours logged successfully.");
    }

    static void requestTimeOff(String name) {
        LocalDate from = askDate("From date");
        LocalDate to = askDate("To date");
        if (to.isBefore(from)) {
            System.out.println("'To' date cannot be before 'From' date.");
            return;
        }
        String reason = ask("Reason: ");
        TimeOffRequest r = new TimeOffRequest(nextRequestId++, name, from, to, reason);
        requests.add(r);
        System.out.println("Time off request submitted (ID " + r.id + "). Status: PENDING");
    }

    static void viewSchedule(String name) {
        Map<DayOfWeek, String> sched = schedules.get(name.toLowerCase());
        if (sched == null) {
            System.out.println("No schedule set for " + name + ".");
            return;
        }
        LocalDate from = askDate("From date");
        LocalDate to = askDate("To date");
        System.out.println("\nSchedule for " + name + ":");
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String entry = sched.getOrDefault(d.getDayOfWeek(), "OFF");
            for (TimeOffRequest r : requests) {
                if (r.employee.equalsIgnoreCase(name) && r.status.equals("APPROVED") && r.covers(d)) {
                    entry = "TIME OFF (approved)";
                }
            }
            System.out.println(d + " " + d.getDayOfWeek() + " : " + entry);
        }
    }

    // ---------- HR Manager ----------
    static void hrMenu() {
        while (true) {
            System.out.println("\n--- HR Manager Dashboard ---");
            System.out.println("1. Time off management\n2. Schedule management\n3. Time tracking review\n4. Generate report\n5. Back");
            String c = ask("Choose: ");
            switch (c) {
                case "1":
                    manageTimeOff();
                    break;
                case "2":
                    manageSchedules();
                    break;
                case "3":
                    reviewTimeLogs();
                    break;
                case "4":
                    generateReport();
                    break;
                case "5":
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void manageTimeOff() {
        if (requests.isEmpty()) {
            System.out.println("No time off requests.");
            return;
        }
        System.out.println("\nID | Employee | Dates | Reason | Status");
        for (TimeOffRequest r : requests) System.out.println(r);
        int id = askInt("Enter request ID to review (0 to cancel): ");
        if (id == 0) return;
        for (TimeOffRequest r : requests) {
            if (r.id == id) {
                String d = ask("Approve or Deny (A/D): ").toUpperCase();
                if (d.equals("A")) {
                    r.status = "APPROVED";
                    System.out.println("Request #" + id + " approved.");
                } else if (d.equals("D")) {
                    r.status = "DENIED";
                    System.out.println("Request #" + id + " denied.");
                } else {
                    System.out.println("No change made.");
                }
                return;
            }
        }
        System.out.println("Request ID not found.");
    }

    static void manageSchedules() {
        System.out.println("\nCurrent schedules:");
        if (schedules.isEmpty()) System.out.println("(none)");
        for (Map.Entry<String, Map<DayOfWeek, String>> e : schedules.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
        String emp = ask("Employee name to update (blank to cancel): ");
        if (emp.isEmpty()) return;
        DayOfWeek day = askDay();
        String off = ask("Day off? (y/n): ").toLowerCase();
        String value;
        if (off.equals("y")) {
            value = "OFF";
        } else {
            LocalTime s = askTime("Shift start");
            LocalTime e = askTime("Shift end");
            if (!e.isAfter(s)) {
                System.out.println("Shift end must be after start.");
                return;
            }
            value = s + "-" + e;
        }
        schedules.computeIfAbsent(emp.toLowerCase(), k -> new HashMap<>()).put(day, value);
        System.out.println("Schedule updated for " + emp + ".");
    }

    static void reviewTimeLogs() {
        if (logs.isEmpty()) {
            System.out.println("No time logs.");
            return;
        }
        for (int i = 0; i < logs.size(); i++) {
            System.out.println((i + 1) + ". " + logs.get(i));
        }
        int n = askInt("Enter log number to correct (0 to cancel): ");
        if (n < 1 || n > logs.size()) return;
        TimeLog l = logs.get(n - 1);
        LocalTime s = askTime("New start time");
        LocalTime e = askTime("New end time");
        if (!e.isAfter(s)) {
            System.out.println("End time must be after start time.");
            return;
        }
        l.start = s;
        l.end = e;
        System.out.println("Time log corrected: " + l);
    }

    static void generateReport() {
        String emp = ask("Employee name (blank for all): ");
        LocalDate from = askDate("From date");
        LocalDate to = askDate("To date");

        System.out.println("\n===== REPORT: " + (emp.isEmpty() ? "All employees" : emp) + " | " + from + " to " + to + " =====");

        System.out.println("\nTime logs:");
        double total = 0;
        for (TimeLog l : logs) {
            boolean who = emp.isEmpty() || l.employee.equalsIgnoreCase(emp);
            if (who && !l.date.isBefore(from) && !l.date.isAfter(to)) {
                System.out.println(l);
                total += l.hours();
            }
        }
        System.out.printf("Total hours: %.2f%n", total);

        System.out.println("\nTime off:");
        int approved = 0, denied = 0, pending = 0;
        for (TimeOffRequest r : requests) {
            boolean who = emp.isEmpty() || r.employee.equalsIgnoreCase(emp);
            boolean overlap = !r.to.isBefore(from) && !r.from.isAfter(to);
            if (who && overlap) {
                System.out.println(r);
                if (r.status.equals("APPROVED")) approved++;
                else if (r.status.equals("DENIED")) denied++;
                else pending++;
            }
        }
        System.out.println("Approved: " + approved + " | Denied: " + denied + " | Pending: " + pending);

        // Simple text bar for total hours per employee
        System.out.println("\nHours chart (each # = 1 hour):");
        Map<String, Double> perEmp = new HashMap<>();
        for (TimeLog l : logs) {
            boolean who = emp.isEmpty() || l.employee.equalsIgnoreCase(emp);
            if (who && !l.date.isBefore(from) && !l.date.isAfter(to)) {
                perEmp.merge(l.employee, l.hours(), Double::sum);
            }
        }
        if (perEmp.isEmpty()) System.out.println("(no data)");
        for (Map.Entry<String, Double> e : perEmp.entrySet()) {
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < Math.round(e.getValue()); i++) bar.append('#');
            System.out.printf("%-12s %s %.1f%n", e.getKey(), bar, e.getValue());
        }
    }
}