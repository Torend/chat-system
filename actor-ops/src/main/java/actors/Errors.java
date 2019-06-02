package actors;


public class Errors {
    public enum Error {
        SUCCESS(0, ""),
        NO_SERVER(1, "server is offline!"),
        DUPLICATE_USER(2, "is in use!"),
        DUPLICATE_GROUP(3, "already exists!");




        private final int code;
        private final String description;

        private Error(int code, String description) {
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
