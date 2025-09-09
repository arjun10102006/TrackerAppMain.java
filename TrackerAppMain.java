package trackers;

import java.time.LocalDateTime;
import java.util.*;

enum Role { QA, DEV, MANAGER }
enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
enum Status { NEW, IN_PROGRESS, RESOLVED, CLOSED }

class User {
    private String id;
    private String name;
    private Role role;
    private String email;
    private String bio;

    public User(String id, String name, Role role, String email) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.email = email;
        this.bio = "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getBio() { return bio; }

    public void setName(String name) { this.name = name; }
    public void setRole(Role role) { this.role = role; }
    public void setEmail(String email) { this.email = email; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean approveIssue(Issue issue) { return false; }

    public String toString() { return name + " (" + role + ")"; }
}

class Manager extends User {
    public Manager(String id, String name, String email) {
        super(id, name, Role.MANAGER, email);
    }

    @Override
    public boolean approveIssue(Issue issue) {
        if (issue.getSeverity() == Severity.CRITICAL) {
            issue.setStatus(Status.IN_PROGRESS);
            return true;
        }
        return false;
    }
}

abstract class Issue {
    private String issueId;
    private String title;
    private String description;
    private Severity severity;
    private Status status;
    private User assignee;
    private List<String> attachments;
    private Set<String> tags;
    private LocalDateTime createdAt;

    public Issue(String issueId, String title, String description, Severity severity) {
        this.issueId = issueId;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = Status.NEW;
        this.attachments = new ArrayList<>();
        this.tags = new HashSet<>();
        this.createdAt = LocalDateTime.now();
    }

    public String getIssueId() { return issueId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public Status getStatus() { return status; }
    public User getAssignee() { return assignee; }
    public List<String> getAttachments() { return Collections.unmodifiableList(attachments); }
    public Set<String> getTags() { return Collections.unmodifiableSet(tags); }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public void setStatus(Status status) { this.status = status; }

    public void assignTo(User user) { this.assignee = user; }
    public void addAttachment(String a) { attachments.add(a); }
    public void addTag(String t) { tags.add(t); }

    public abstract void display();
}

class Bug extends Issue {
    public Bug(String issueId, String title, String description, Severity severity) {
        super(issueId, title, description, severity);
    }

    @Override
    public void display() {
        System.out.println("[BUG] " + getIssueId() + " " + getTitle() + " " + getStatus() + " " + getSeverity());
    }
}

class Task extends Issue {
    public Task(String issueId, String title, String description, Severity severity) {
        super(issueId, title, description, severity);
    }

    @Override
    public void display() {
        System.out.println("[TASK] " + getIssueId() + " " + getTitle() + " " + getStatus() + " " + getSeverity());
    }
}

class Project {
    private String projectId;
    private String name;
    private String repoUrl;
    private List<Issue> backlog;
    private List<User> team;
    private String description;
    private LocalDateTime createdAt;

    public Project(String projectId, String name, String repoUrl) {
        this.projectId = projectId;
        this.name = name;
        this.repoUrl = repoUrl;
        this.backlog = new ArrayList<>();
        this.team = new ArrayList<>();
        this.description = "";
        this.createdAt = LocalDateTime.now();
    }

    public String getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getRepoUrl() { return repoUrl; }
    public List<Issue> getBacklog() { return Collections.unmodifiableList(backlog); }
    public List<User> getTeam() { return Collections.unmodifiableList(team); }

    public void setDescription(String d) { this.description = d; }
    public void addUser(User u) { team.add(u); }
    public void removeUser(User u) { team.remove(u); }
    public void addIssue(Issue i) { backlog.add(i); }
    public void removeIssue(Issue i) { backlog.remove(i); }

    public List<Issue> listBySeverity(Severity s) {
        List<Issue> out = new ArrayList<>();
        for (Issue i : backlog) if (i.getSeverity() == s) out.add(i);
        return out;
    }

    public String toString() { return name + " [" + projectId + "]"; }
}

class TrackerService {
    private Map<String, Project> projects = new HashMap<>();
    private Map<String, User> users = new HashMap<>();
    private Map<String, Issue> issues = new HashMap<>();

    public User createUser(String id, String name, Role role, String email) {
        User u = role == Role.MANAGER ? new Manager(id, name, email) : new User(id, name, role, email);
        users.put(id, u);
        return u;
    }

    public Project createProject(String pid, String name, String repoUrl) {
        Project p = new Project(pid, name, repoUrl);
        projects.put(pid, p);
        return p;
    }

    public Issue createIssue(String issueId, String title, String desc, Severity sev, String type) {
        Issue i = "task".equalsIgnoreCase(type) ? new Task(issueId, title, desc, sev) : new Bug(issueId, title, desc, sev);
        issues.put(issueId, i);
        return i;
    }

    public Issue createIssue(String issueId, String title, String desc, Severity sev) {
        return createIssue(issueId, title, desc, sev, "bug");
    }

    public void attachToIssue(String issueId, String attachment) {
        Issue i = issues.get(issueId);
        if (i != null) i.addAttachment(attachment);
    }

    public void tagIssue(String issueId, String tag) {
        Issue i = issues.get(issueId);
        if (i != null) i.addTag(tag);
    }

    public boolean assignIssue(String issueId, String userId) {
        Issue i = issues.get(issueId);
        User u = users.get(userId);
        if (i != null && u != null) {
            i.assignTo(u);
            i.setStatus(Status.IN_PROGRESS);
            return true;
        }
        return false;
    }

    public boolean changeStatus(String issueId, Status s) {
        Issue i = issues.get(issueId);
        if (i != null) {
            i.setStatus(s);
            return true;
        }
        return false;
    }

    public List<Issue> listBySeverity(String projectId, Severity s) {
        Project p = projects.get(projectId);
        if (p == null) return Collections.emptyList();
        return p.listBySeverity(s);
    }

    public void addIssueToProject(String projectId, Issue i) {
        Project p = projects.get(projectId);
        if (p != null) p.addIssue(i);
    }

    public void addUserToProject(String projectId, User u) {
        Project p = projects.get(projectId);
        if (p != null) p.addUser(u);
    }

    public void printProjectDashboard(String projectId) {
        Project p = projects.get(projectId);
        if (p == null) return;
        System.out.println("Project: " + p.getName());
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity sv : Severity.values()) counts.put(sv, 0);
        for (Issue i : p.getBacklog()) counts.put(i.getSeverity(), counts.get(i.getSeverity()) + 1);
        for (Severity sv : Severity.values()) System.out.println(sv + ": " + counts.get(sv));
    }

    public void printSeverityReport(String projectId) {
        Project p = projects.get(projectId);
        if (p == null) return;
        for (Issue i : p.getBacklog()) {
            System.out.println(i.getIssueId() + " " + i.getTitle() + " " + i.getSeverity() + " " + i.getStatus());
        }
    }

    public void printAllIssues() {
        for (Issue i : issues.values()) i.display();
    }
}

public class TrackerAppMain {
    public static void main(String[] args) {
        TrackerService ts = new TrackerService();

        User u1 = ts.createUser("U1", "Alice", Role.QA, "alice@example.com");
        User u2 = ts.createUser("U2", "Bob", Role.DEV, "bob@example.com");
        User m1 = ts.createUser("M1", "Carol", Role.MANAGER, "carol@example.com");

        Project p = ts.createProject("P1", "Alpha", "https://repo/alpha");

        ts.addUserToProject("P1", u1);
        ts.addUserToProject("P1", u2);
        ts.addUserToProject("P1", m1);

        Issue i1 = ts.createIssue("I1", "NullPointer in Login", "NPE when user logs in", Severity.CRITICAL, "bug");
        Issue i2 = ts.createIssue("I2", "UI alignment", "Button misaligned on mobile", Severity.LOW, "task");

        ts.addIssueToProject("P1", i1);
        ts.addIssueToProject("P1", i2);

        ts.attachToIssue("I1", "screenshot.png");
        ts.tagIssue("I1", "login");
        ts.assignIssue("I1", "U2");
        ts.changeStatus("I2", Status.IN_PROGRESS);

        ts.printProjectDashboard("P1");
        ts.printSeverityReport("P1");
        ts.printAllIssues();

        Manager mgr = (Manager) m1;
        mgr.approveIssue(i1);

        ts.printAllIssues();
    }
}
