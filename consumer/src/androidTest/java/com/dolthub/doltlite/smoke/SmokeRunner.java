package com.dolthub.doltlite.smoke;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;
import com.dolthub.doltlite.Doltlite;
import java.io.File;
import java.util.Objects;

public final class SmokeRunner extends Instrumentation {
    private Bundle arguments;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        this.arguments = arguments;
        start();
    }

    @Override
    public void onStart() {
        String phase = arguments.getString("phase");
        Bundle result = new Bundle();
        try {
            File file = new File(getTargetContext().getFilesDir(), "versioned.db");
            if ("write".equals(phase)) {
                checkMemoryDatabase();
                if (file.exists()) throw new AssertionError("Database already exists before write");
                try (Doltlite db = open(file)) {
                    writeHistory(db);
                }
            } else if ("reopen".equals(phase)) {
                if (!file.isFile()) throw new AssertionError("Database missing after relaunch");
                try (Doltlite db = open(file)) {
                    verifyHistory(db);
                }
            } else {
                throw new AssertionError("Unknown phase " + phase);
            }
            result.putString("stream", "\nDoltLite smoke " + phase + " OK\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "\n" + Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private Doltlite open(File file) {
        Doltlite db = new Doltlite(file.getPath());
        try {
            verifyEngine(db);
            return db;
        } catch (Throwable failure) {
            db.close();
            throw failure;
        }
    }

    private void checkMemoryDatabase() {
        try (Doltlite db = new Doltlite(":memory:")) {
            verifyEngine(db);
            db.execute("CREATE TABLE t(id INTEGER PRIMARY KEY, v TEXT)");
            db.execute("INSERT INTO t(v) VALUES (?)", "a");
            commit(db, "memory commit");
            expect("in-memory commit count", 2L, scalar(db, "SELECT count(*) FROM dolt_log"));
        }
    }

    private void verifyEngine(Doltlite db) {
        expect("engine", "prolly", scalar(db, "SELECT doltlite_engine()"));
        expect("engine version", arguments.getString("version"), db.doltVersion());
    }

    private static void writeHistory(Doltlite db) {
        db.execute("CREATE TABLE notes(id INTEGER PRIMARY KEY, body TEXT, value INTEGER, optional TEXT)");
        db.execute("INSERT INTO notes VALUES (?, ?, ?, ?)", 1, "café", Long.MAX_VALUE, null);
        db.execute("BEGIN");
        db.execute("UPDATE notes SET body = 'rolled back'");
        db.execute("ROLLBACK");
        commit(db, "initial note");
        db.execute("SELECT dolt_branch(?)", "topic");
        db.execute("SELECT dolt_checkout(?)", "topic");
        db.execute("UPDATE notes SET body = ?", "café on topic");
        commit(db, "edit on topic");
        db.execute("SELECT dolt_checkout(?)", "main");
        expect("main before merge", "café", scalar(db, "SELECT body FROM notes"));
        db.execute("SELECT dolt_merge(?)", "topic");
    }

    private static void verifyHistory(Doltlite db) {
        expect("merged row", "café on topic", scalar(db, "SELECT body FROM notes WHERE id = 1"));
        expect("bound integer", Long.MAX_VALUE, scalar(db, "SELECT value FROM notes"));
        expect("bound NULL", null, scalar(db, "SELECT optional FROM notes"));
        expect("commit history", 2L, scalar(db,
            "SELECT count(*) FROM dolt_log WHERE message IN ('initial note', 'edit on topic')"));
    }

    private static void commit(Doltlite db, String message) {
        String hash = db.doltCommit(message, true);
        if (hash == null || hash.isEmpty()) throw new AssertionError("Missing commit hash for " + message);
    }

    private static Object scalar(Doltlite db, String sql) {
        return db.query(sql).get(0).get(0);
    }

    private static void expect(String label, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
    }
}
