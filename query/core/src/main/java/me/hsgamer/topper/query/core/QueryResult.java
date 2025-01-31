package me.hsgamer.topper.query.core;

public final class QueryResult {
    public final boolean handled;
    public final String result;

    private QueryResult(boolean handled, String result) {
        this.handled = handled;
        this.result = result;
    }

    public static QueryResult handled(String result) {
        return new QueryResult(true, result);
    }

    public static QueryResult notHandled() {
        return new QueryResult(false, null);
    }
}
