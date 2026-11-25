package com.chirag.home;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Mac-side harness: runs the real voice pipeline (VoiceCore.understand + VoiceAgent with the Groq router,
 * JustWatch and YouTube) on typed queries and checks the route and the app that would open.
 *
 * Keys come from voice.json (path in $VOICE_JSON, default ../tv-launcher/voice.json next to this worktree,
 * then ./voice.json) or $GROQ_KEY. Nothing here prints a key.
 *
 * Usage: tools/voice-test/run.sh [filter]   (filter = substring of the queries to run; a|b|c for several)
 */
public final class VoiceTest {
    static final String PRIME = "com.amazon.amazonvideo.livingroom";
    static final String YT = "com.google.android.youtube.tv";
    static final long PACE_MS = Long.parseLong(System.getenv().getOrDefault("VOICETEST_PACE_MS", "9000"));

    public static void main(String[] args) throws Exception {
        File dir = new File(System.getProperty("voicetest.dir", "tools/voice-test"));
        if (args.length > 0 && args[0].equals("--selftest")) {
            selfTest();
            return;
        }
        VoiceClient.Config cfg = config();
        // Taste cache and the launcher's played-track log live in out/ (gitignored), never next to voice.json.
        cfg.dataDir = new File(dir, "out");
        cfg.dataDir.mkdirs();
        System.out.println("Spotify taste: " + (cfg.spotifyRefresh.isEmpty() ? "off (no spotify_refresh_token)" : "owner sign-in configured"));
        System.out.println("Spotify Web API: " + (cfg.hasSpotifyApi() ? "configured" : "not configured (music plays from YouTube unless Spotify is named)"));
        String filter = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        long now = System.currentTimeMillis();
        List<VoiceAgent.Play> history = history(now);

        List<String[]> cases = new ArrayList<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dir, "cases.tsv")), "UTF-8"));
        for (String line; (line = in.readLine()) != null; ) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t");
            if (f.length < 3) continue;
            if (!filter.isEmpty() && !matches(f[0].toLowerCase(Locale.ROOT), filter)) continue;
            cases.add(f);
        }
        in.close();

        new File(dir, "out").mkdirs();
        PrintWriter traces = new PrintWriter(new File(dir, "out/traces.txt"), "UTF-8");
        StringBuilder table = new StringBuilder();
        table.append("| # | Query | Expected | Got route | Decision | Top result | App | ms | Pass |\n");
        table.append("|---|---|---|---|---|---|---|---|---|\n");
        int pass = 0, n = 0;
        for (String[] c : cases) {
            // One request at a time, like a person. Groq's free tier allows ~8k tokens a minute (about 5 router
            // calls), so pace the run; VOICETEST_PACE_MS=0 to go fast with a paid key.
            if (n > 0) Thread.sleep(PACE_MS);
            n++;
            String query = c[0];
            List<String> wantRoutes = Arrays.asList(c[1].split("\\|"));
            String wantApp = c[2];
            if (wantApp.equals("spotify*")) {
                wantApp = cfg.hasSpotifyApi() ? "spotify" : "youtube";
            }
            long t0 = System.nanoTime();
            VoiceAgent.Outcome o;
            String err = null;
            try {
                o = new VoiceAgent(cfg, VoiceClient.REAL).run(query, history, now);
                if (o.trace.contains("HTTP 429")) {
                    System.out.println("     (rate limited; waiting 30 s and running it again)");
                    Thread.sleep(30000);
                    t0 = System.nanoTime();
                    o = new VoiceAgent(cfg, VoiceClient.REAL).run(query, history, now);
                }
            } catch (Throwable e) {
                o = null;
                err = e.toString();
            }
            long ms = (System.nanoTime() - t0) / 1000000;
            String route = o == null ? "crash" : o.route;
            String decision = o == null ? "-" : o.decision;
            String top = "";
            String app = "-";
            if (o != null && !o.candidates.isEmpty()) {
                VoiceAgent.Candidate cand = o.candidates.get(0);
                top = cand.line();
                if (!cand.links.isEmpty()) {
                    VoiceCore.App a = VoiceCore.appForPkg(cand.links.get(0).pkg);
                    app = a == null ? cand.links.get(0).pkg : a.key;
                }
            } else if (o != null && o.titles != null && !o.titles.titles.isEmpty()) {
                top = "(title list) " + o.titles.titles.get(0).name;
            } else if (o != null) {
                top = o.message;
            } else {
                top = err;
            }
            boolean ok = wantRoutes.contains(route) && (wantApp.equals("any") || wantApp.equals(app)) && !decision.equals("none");
            if (ok) pass++;
            String spoken = o == null ? "" : VoiceAgent.spoken(o);
            table.append("| ").append(n).append(" | ").append(query).append(" | ").append(c[1]).append(" / ").append(wantApp)
                    .append(" | ").append(route).append(" | ").append(decision).append(" | ").append(cell(top))
                    .append(" | ").append(app).append(" | ").append(ms).append(" | ").append(ok ? "PASS" : "FAIL").append(" |\n");
            System.out.println(String.format(Locale.ROOT, "%-4s %-42s route=%-15s %-6s app=%-8s %5dms  %s  | says: %s",
                    ok ? "PASS" : "FAIL", query, route, decision, app, ms, top, spoken));
            if (o != null && !o.candidates.isEmpty() && !o.candidates.get(0).why.isEmpty()) {
                System.out.println("       seed: " + o.candidates.get(0).detail.replaceFirst("^starting with ", "") + "  <- " + o.candidates.get(0).why);
            }
            traces.println("=== " + query + "  [" + (ok ? "PASS" : "FAIL") + "]");
            traces.println(o == null ? err : o.trace);
            if (o != null) {
                for (VoiceAgent.Candidate cand : o.candidates) {
                    traces.println("  cand: " + cand.line());
                    for (VoiceCore.Link l : cand.links) traces.println("     " + l);
                }
                traces.println("  says: " + spoken);
            }
            traces.println();
            traces.flush();
        }
        traces.close();
        table.append("\n").append(pass).append("/").append(n).append(" passed\n");
        PrintWriter md = new PrintWriter(new File(dir, "out/results.md"), "UTF-8");
        md.print(table);
        md.close();
        System.out.println();
        System.out.println(pass + "/" + n + " passed  (table: " + new File(dir, "out/results.md") + ", traces: out/traces.txt)");
    }

    /** Offline checks of the Spotify lookup with canned API replies (no keys needed). */
    static void selfTest() throws Exception {
        VoiceClient.Config cfg = new VoiceClient.Config();
        cfg.spotifyId = "test-id";
        cfg.spotifySecret = "test-secret";
        cfg.groqKey = "";
        final List<String> calls = new ArrayList<>();
        VoiceClient.Http fake = new VoiceClient.Http() {
            @Override
            public VoiceClient.Resp call(String method, String url, java.util.Map<String, String> h, byte[] body, int timeout) {
                calls.add(method + " " + url);
                if (url.startsWith("https://accounts.spotify.com/api/token")) {
                    boolean basic = h.getOrDefault("Authorization", "").startsWith("Basic ");
                    return new VoiceClient.Resp(basic ? 200 : 401, "{\"access_token\":\"tok\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
                }
                if (url.startsWith("https://api.spotify.com/v1/search")) {
                    return new VoiceClient.Resp(200, "{\"tracks\":{\"items\":["
                            + "{\"id\":\"1111111111111111111111\",\"name\":\"Kesariya (Dance Mix)\",\"popularity\":40,\"artists\":[{\"name\":\"Pritam\"}]},"
                            + "{\"id\":\"2222222222222222222222\",\"name\":\"Kesariya (From Brahmastra)\",\"popularity\":80,\"artists\":[{\"name\":\"Arijit Singh\"}]},"
                            + "{\"id\":\"bad\",\"name\":\"broken\",\"popularity\":99,\"artists\":[]}]}}");
                }
                return new VoiceClient.Resp(404, "");
            }
        };
        int fails = 0;
        VoiceClient.Track t = new VoiceClient(cfg, fake).spotifyTrack("kesariya", "song", "", 3000);
        fails += check("song lookup picks the popular exact match", t != null && t.id.equals("2222222222222222222222"));
        fails += check("token is fetched once and cached", new VoiceClient(cfg, fake).spotifyTrack("kesariya", "song", "", 3000) != null
                && calls.stream().filter(c -> c.contains("accounts.spotify.com")).count() == 1);
        fails += check("search asks for market IN", calls.stream().anyMatch(c -> c.contains("market=IN")));
        VoiceAgent.Outcome o = new VoiceAgent(cfg, fake).run("play Kesariya song", new ArrayList<>(), System.currentTimeMillis());
        VoiceCore.Link first = o.candidates.isEmpty() || o.candidates.get(0).links.isEmpty() ? null : o.candidates.get(0).links.get(0);
        fails += check("agent plays spotify:track:<id> (" + (first == null ? "nothing" : first.uri) + ")", o.decision.equals("play") && first != null
                && "com.spotify.tv.android".equals(first.pkg) && "spotify:track:2222222222222222222222".equals(first.uri));
        fails += check("spoken line: " + VoiceAgent.spoken(o), VoiceAgent.spoken(o).equals("Playing Kesariya by Arijit Singh on Spotify"));
        o = new VoiceAgent(cfg, fake).run("play some lo-fi", new ArrayList<>(), System.currentTimeMillis());
        fails += check("genre plays on Spotify: " + VoiceAgent.spoken(o), o.decision.equals("play") && VoiceAgent.spoken(o).equals("Playing lo-fi on Spotify"));
        cfg.spotifySecret = "";
        o = new VoiceAgent(cfg, fake).run("Arijit Singh songs on Spotify", new ArrayList<>(), System.currentTimeMillis());
        first = o.candidates.isEmpty() || o.candidates.get(0).links.isEmpty() ? null : o.candidates.get(0).links.get(0);
        fails += check("no keys + Spotify named: opens Spotify (" + (first == null ? "nothing" : first.uri) + ")", first != null && "com.spotify.tv.android".equals(first.pkg));
        cfg.spotifySecret = "test-secret";
        cfg.installed = new java.util.HashSet<>(Arrays.asList("com.google.android.youtube.tv"));
        o = new VoiceAgent(cfg, VoiceClient.REAL).run("play some lo-fi", new ArrayList<>(), System.currentTimeMillis());
        first = o.candidates.isEmpty() || o.candidates.get(0).links.isEmpty() ? null : o.candidates.get(0).links.get(0);
        fails += check("Spotify not installed: YouTube (" + (first == null ? "nothing" : first.pkg) + ")", first != null && YT.equals(first.pkg));
        System.out.println(fails == 0 ? "self-test passed" : fails + " self-test check(s) failed");
    }

    /** filter = one substring, or several joined with "|" (any may match). */
    static boolean matches(String query, String filter) {
        for (String f : filter.split("\\|")) {
            if (!f.isEmpty() && query.contains(f)) return true;
        }
        return false;
    }

    static int check(String what, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + what);
        return ok ? 0 : 1;
    }

    static String cell(String s) {
        s = s == null ? "" : s.replace("|", "/").replace("\n", " ");
        return s.length() > 70 ? s.substring(0, 70) + "…" : s;
    }

    static VoiceClient.Config config() {
        String path = System.getenv("VOICE_JSON");
        List<File> tries = new ArrayList<>();
        if (path != null && !path.isEmpty()) tries.add(new File(path));
        tries.add(new File("../tv-launcher/voice.json"));
        tries.add(new File("voice.json"));
        VoiceClient.Config cfg = null;
        for (File f : tries) {
            cfg = VoiceClient.Config.read(f);
            if (cfg != null) {
                System.out.println("config: " + f.getPath());
                break;
            }
        }
        if (cfg == null) cfg = new VoiceClient.Config();
        String key = System.getenv("GROQ_KEY");
        if (key != null && !key.isEmpty()) cfg.groqKey = key;
        if (cfg.groqKey.isEmpty()) System.out.println("warning: no Groq key; the LLM router is off");
        String installed = System.getenv("INSTALLED");
        if (installed != null) {
            cfg.installed = new java.util.HashSet<>(Arrays.asList(installed.split(",")));
            System.out.println("installed apps: " + cfg.installed);
        }
        return cfg;
    }

    /** A small watch history so "next episode" and "continue" have something to work with. */
    static List<VoiceAgent.Play> history(long now) {
        List<VoiceAgent.Play> list = new ArrayList<>();
        VoiceAgent.Play p = new VoiceAgent.Play();
        p.pkg = PRIME;
        p.title = "Panchayat";
        p.subtitle = "S3 E2 · Chakka Jam";
        p.lastTs = now - 3 * 3600000L;
        p.duration = 35 * 60000L;
        p.position = 12 * 60000L;
        list.add(p);
        p = new VoiceAgent.Play();
        p.pkg = YT;
        p.title = "Why India's trains are always late";
        p.subtitle = "Dhruv Rathee";
        p.lastTs = now - 26 * 3600000L;
        p.duration = 20 * 60000L;
        p.position = 20 * 60000L;
        list.add(p);
        return list;
    }
}
