package me.hsgamer.topper.agent.update;

interface UpdateStatus {
    UpdateStatus DEFAULT = new UpdateStatus() {
    };
    UpdateStatus RESET = new UpdateStatus() {
    };

    class Skip implements UpdateStatus {
        private final int skips;

        public Skip(int skips) {
            this.skips = skips;
        }

        public boolean skip() {
            return skips > 0;
        }

        public Skip decrement() {
            return new Skip(skips - 1);
        }
    }

    class Set implements UpdateStatus {
        private final Object value;

        public Set(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }
}
