import com.dolthub.doltlite.Doltlite;
import java.util.List;

/**
 * Runtime smoke for the doltlite-android binding: drives the real compiled
 * Doltlite/CDoltlite classes (extracted from the assembled AAR) over JNA against
 * a host-built libdoltlite, exercising open -> bind -> dolt_commit -> dolt_log.
 * The Android-ABI .so's can't load on the desktop CI runner, but the binding
 * logic and C-API calls are identical, so this guards the binding at runtime.
 */
public class RuntimeSmoke {
  public static void main(String[] args) {
    try (Doltlite db = new Doltlite(":memory:")) {
      Object engine = db.query("SELECT doltlite_engine()").get(0).get(0);
      if (!"prolly".equals(engine)) throw new RuntimeException("engine=" + engine);

      db.execute("CREATE TABLE t(id INTEGER PRIMARY KEY, v TEXT)");
      db.execute("INSERT INTO t(v) VALUES (?)", "a");

      String hash = db.doltCommit("c1", true);
      if (hash == null || hash.isEmpty()) throw new RuntimeException("no commit hash");

      long commits = (Long) db.query("SELECT count(*) FROM dolt_log").get(0).get(0);
      if (commits != 2) throw new RuntimeException("dolt_log count=" + commits + " (expected 2)");

      System.out.println("runtime smoke OK: engine=" + engine
        + " commit=" + hash.substring(0, 8) + " commits=" + commits);
    }
  }
}
