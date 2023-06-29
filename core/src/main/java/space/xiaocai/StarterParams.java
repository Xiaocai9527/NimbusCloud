package space.xiaocai;

public class StarterParams {

    private final String name;
    private final String password;
    private final int port;
    private final int bossThreadCounts;
    private final int workerThreadCounts;
    private final String path;

    private StarterParams(Builder builder) {
        this.name = builder.name;
        this.password = builder.password;
        this.port = builder.port;
        this.bossThreadCounts = builder.bossThreadCounts;
        this.workerThreadCounts = builder.workerThreadCounts;
        this.path = builder.path;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public int getBossThreadCounts() {
        return bossThreadCounts;
    }

    public int getWorkerThreadCounts() {
        return workerThreadCounts;
    }

    public String getPath() {
        return path;
    }

    public static class Builder {
        private String name = "";
        private String password = "";
        private int port = 8080;
        private int bossThreadCounts = 1;
        private int workerThreadCounts = 3;
        private String path;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder bossThreadCounts(int bossThreadCounts) {
            this.bossThreadCounts = bossThreadCounts;
            return this;
        }

        public Builder workerThreadCounts(int workerThreadCounts) {
            this.workerThreadCounts = workerThreadCounts;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public StarterParams build() {
            return new StarterParams(this);
        }
    }
}