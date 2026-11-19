package com.unigram.api;

import com.unigram.model.User;

public class SafeUserDto {
    private String id;
    private String userId;
    private String name;
    private String fullName;
    private String email;
    private String role;
    private String studentId;
    private String student_id;

    public SafeUserDto(User user) {
        if (user != null) {
            this.id = user.getUserId();
            this.userId = user.getUserId();
            this.name = user.getFullName();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.studentId = user.getStudentId();
            this.student_id = user.getStudentId();
        }
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStudentId() { return studentId; }
    public String getStudent_id() { return student_id; }
}
