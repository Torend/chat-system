package actors;


public class Errors {
    public enum Error {
        SUCCESS(0, ""),
        NO_SERVER(1, "server is offline!"),
        DUPLICATE_USER(2, "is in use!"),
        DUPLICATE_GROUP(3, "already exists!"),
        NO_SUCH_GROUP(4, "group does not exist!"),
        NO_PRIVILEGE (5, "“You are neither an admin nor a co-admin of "),
        ALREADY_MEMBER(6, "already registered in the group"),
        NO_SUCH_MEMBER(7, "member does not exist!"),
        NOT_MUTED(8, "member does not muted!"),
        MUTED(9, "You are muted");



        private final int code;
        private final String description;

       Error(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public int getCode() {
            return code;
        }

        @Override
        public String toString() {
            return code + ": " + description;
        }
    }
}
