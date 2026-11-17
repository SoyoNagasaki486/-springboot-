package edu.lab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 不改变数据库结构的业务与安全参数。 */
@ConfigurationProperties(prefix = "labguard")
public class LabGuardProperties {
    private final Security security = new Security();
    private final Reservation reservation = new Reservation();
    private final Exam exam = new Exam();

    public Security getSecurity() {
        return security;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Exam getExam() {
        return exam;
    }

    public static class Security {
        private int sessionTimeoutMinutes = 30;
        private int maxLoginAttempts = 5;
        private int loginBlockMinutes = 10;

        public int getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }

        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }

        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }

        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }

        public int getLoginBlockMinutes() {
            return loginBlockMinutes;
        }

        public void setLoginBlockMinutes(int loginBlockMinutes) {
            this.loginBlockMinutes = loginBlockMinutes;
        }
    }

    public static class Reservation {
        private int maxAdvanceDays = 60;

        public int getMaxAdvanceDays() {
            return maxAdvanceDays;
        }

        public void setMaxAdvanceDays(int maxAdvanceDays) {
            this.maxAdvanceDays = maxAdvanceDays;
        }
    }

    public static class Exam {
        private int questionCount = 10;

        public int getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(int questionCount) {
            this.questionCount = questionCount;
        }
    }
}
