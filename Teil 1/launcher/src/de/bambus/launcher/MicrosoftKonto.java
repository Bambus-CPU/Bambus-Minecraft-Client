package de.bambus.launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anmeldung mit dem Microsoft-Konto – nach dem offiziellen Ablauf (Microsoft → Xbox Live → XSTS → Minecraft).
 *
 * Bambus sieht dabei NIE ein Passwort: Der Spieler meldet sich auf der Microsoft-Seite an
 * (microsoft.com/link, „Gerätecode“-Anmeldung) und Bambus bekommt nur ein Zugangs-Token.
 *
 * Voraussetzung: eine eigene Azure-App (öffentlicher Client, „Allow public client flows“ = Ja),
 * die Mojang für die Minecraft-Dienste freigegeben hat (https://aka.ms/mce-reviewappid).
 * Ohne eingetragene App-ID bleibt die Anmeldung aus und Bambus startet wie bisher über den
 * offiziellen Minecraft Launcher.
 */
final class MicrosoftKonto {

	private static final String MS = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";
	private static final String SCOPE = "XboxLive.signin offline_access";

	/** Gerätecode, den der Spieler auf microsoft.com/link eintippt. */
	static final class Geraetecode {
		final String deviceCode, userCode, adresse;
		final long ablaufMs, intervallMs;

		Geraetecode(String deviceCode, String userCode, String adresse, long ablaufMs, long intervallMs) {
			this.deviceCode = deviceCode;
			this.userCode = userCode;
			this.adresse = adresse;
			this.ablaufMs = ablaufMs;
			this.intervallMs = intervallMs;
		}
	}

	/** Für den Spielstart: Name, UUID, Minecraft-Token. */
	static final class Sitzung {
		final String name, uuid, token, xuid;

		Sitzung(String name, String uuid, String token, String xuid) {
			this.name = name;
			this.uuid = uuid;
			this.token = token;
			this.xuid = xuid;
		}
	}

	/** Gespeicherte Daten liegen NICHT im Spielordner (dort könnten Mods sie lesen), sondern im Benutzerordner. */
	private final Path datei = Paths.get(System.getProperty("user.home"), ".bambus-launcher", "konto.json");
	private Map<String, Object> daten = new LinkedHashMap<>();

	MicrosoftKonto() {
		try {
			if (Files.isRegularFile(datei)) {
				Map<String, Object> m = Json.objekt(Json.parse(new String(Files.readAllBytes(datei), StandardCharsets.UTF_8)));
				if (m != null) daten = m;
			}
		} catch (Exception e) {
			System.err.println("[Bambus] Kontodaten nicht lesbar: " + e);
		}
	}

	// ============================================================ Einstellungen

	/**
	 * Fest eingebaute App-ID (nach der Freigabe durch Mojang hier eintragen) –
	 * dann müssen Spieler nichts selbst eintragen. Ein Eintrag im Launcher-Fenster hat Vorrang.
	 */
	static final String STANDARD_APP_ID = "";

	String appId() {
		Object o = daten.get("appId");
		String eigene = o instanceof String ? ((String) o).trim() : "";
		return eigene.isEmpty() ? STANDARD_APP_ID : eigene;
	}

	void setzeAppId(String id) {
		daten.put("appId", id == null ? "" : id.trim());
		speichern();
	}

	boolean freigeschaltet() {
		return appId().matches("[0-9a-fA-F-]{36}");
	}

	boolean angemeldet() {
		return freigeschaltet() && daten.get("refreshToken") instanceof String && daten.get("name") instanceof String;
	}

	String name() {
		Object o = daten.get("name");
		return o instanceof String ? (String) o : "";
	}

	boolean direktStart() {
		return !Boolean.FALSE.equals(daten.get("direktStart"));
	}

	void setzeDirektStart(boolean an) {
		daten.put("direktStart", an);
		speichern();
	}

	void abmelden() {
		for (String k : new String[]{"refreshToken", "mcToken", "mcAblauf", "name", "uuid", "xuid"}) daten.remove(k);
		speichern();
	}

	private void speichern() {
		try {
			Files.createDirectories(datei.getParent());
			Path tmp = datei.resolveSibling("konto.json.tmp");
			Files.write(tmp, Json.write(daten).getBytes(StandardCharsets.UTF_8));
			nurFuerMich(tmp);
			Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("[Bambus] Kontodaten nicht speicherbar: " + e);
		}
	}

	private static void nurFuerMich(Path p) {
		try {
			Files.setPosixFilePermissions(p, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
		} catch (Exception windows) {
			// unter Windows liegt die Datei ohnehin im eigenen Benutzerprofil
		}
	}

	// ============================================================ Anmeldung

	/** Schritt 1: Gerätecode bei Microsoft anfordern. */
	Geraetecode starteAnmeldung() throws IOException {
		Map<String, Object> r = Json.objekt(Json.parse(post(MS + "devicecode",
				"client_id=" + enc(appId()) + "&scope=" + enc(SCOPE), "application/x-www-form-urlencoded", null)));
		if (r == null || !(r.get("device_code") instanceof String)) {
			throw new IOException("Microsoft hat keinen Code geschickt. Stimmt die App-ID?");
		}
		return new Geraetecode((String) r.get("device_code"), (String) r.get("user_code"),
				r.get("verification_uri") instanceof String ? (String) r.get("verification_uri") : "https://www.microsoft.com/link",
				System.currentTimeMillis() + zahl(r.get("expires_in"), 900) * 1000,
				zahl(r.get("interval"), 5) * 1000);
	}

	interface Abbruch { boolean abgebrochen(); }

	/** Schritt 2: warten, bis der Spieler den Code eingegeben hat – danach Xbox/Minecraft-Anmeldung. */
	Sitzung warteAufAnmeldung(Geraetecode c, Abbruch abbruch) throws IOException, InterruptedException {
		long intervall = c.intervallMs;
		while (System.currentTimeMillis() < c.ablaufMs) {
			Thread.sleep(intervall);
			if (abbruch.abgebrochen()) throw new InterruptedException("abgebrochen");
			Antwort a = postRoh(MS + "token", "grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code")
					+ "&client_id=" + enc(appId()) + "&device_code=" + enc(c.deviceCode), "application/x-www-form-urlencoded", null);
			Map<String, Object> r = Json.objekt(Json.parse(a.text));
			if (r == null) continue;
			Object fehler = r.get("error");
			if ("authorization_pending".equals(fehler)) continue;
			if ("slow_down".equals(fehler)) { intervall += 5000; continue; }
			if (fehler != null) throw new IOException(fehlerText(String.valueOf(fehler)));
			daten.put("refreshToken", r.get("refresh_token"));
			return minecraftAnmeldung((String) r.get("access_token"));
		}
		throw new IOException("Der Code ist abgelaufen. Bitte noch einmal anmelden.");
	}

	/** Für den Spielstart: gültige Sitzung (Token wird bei Bedarf erneuert). */
	Sitzung sitzung() throws IOException {
		if (!angemeldet()) throw new IOException("Nicht angemeldet.");
		long ablauf = zahl(daten.get("mcAblauf"), 0);
		if (daten.get("mcToken") instanceof String && System.currentTimeMillis() < ablauf - 10 * 60 * 1000) {
			return new Sitzung(name(), (String) daten.get("uuid"), (String) daten.get("mcToken"), str(daten.get("xuid")));
		}
		Map<String, Object> r = Json.objekt(Json.parse(post(MS + "token", "grant_type=refresh_token&client_id=" + enc(appId())
				+ "&refresh_token=" + enc((String) daten.get("refreshToken")) + "&scope=" + enc(SCOPE),
				"application/x-www-form-urlencoded", null)));
		if (r == null || !(r.get("access_token") instanceof String)) {
			abmelden();
			throw new IOException("Die Anmeldung ist abgelaufen. Bitte neu anmelden.");
		}
		if (r.get("refresh_token") instanceof String) daten.put("refreshToken", r.get("refresh_token"));
		return minecraftAnmeldung((String) r.get("access_token"));
	}

	private Sitzung minecraftAnmeldung(String msToken) throws IOException {
		// Xbox Live
		Map<String, Object> props = new LinkedHashMap<>();
		props.put("AuthMethod", "RPS");
		props.put("SiteName", "user.auth.xboxlive.com");
		props.put("RpsTicket", "d=" + msToken);
		Map<String, Object> xbl = new LinkedHashMap<>();
		xbl.put("Properties", props);
		xbl.put("RelyingParty", "http://auth.xboxlive.com");
		xbl.put("TokenType", "JWT");
		Map<String, Object> xr = Json.objekt(Json.parse(post("https://user.auth.xboxlive.com/user/authenticate",
				Json.write(xbl), "application/json", null)));
		String xblToken = str(xr.get("Token"));

		// XSTS
		Map<String, Object> props2 = new LinkedHashMap<>();
		props2.put("SandboxId", "RETAIL");
		props2.put("UserTokens", Collections.singletonList(xblToken));
		Map<String, Object> xsts = new LinkedHashMap<>();
		xsts.put("Properties", props2);
		xsts.put("RelyingParty", "rp://api.minecraftservices.com/");
		xsts.put("TokenType", "JWT");
		Antwort xa = postRoh("https://xsts.auth.xboxlive.com/xsts/authorize", Json.write(xsts), "application/json", null);
		Map<String, Object> xs = Json.objekt(Json.parse(xa.text.isEmpty() ? "{}" : xa.text));
		if (xa.code == 401 && xs != null) throw new IOException(xboxFehler(String.valueOf(xs.get("XErr"))));
		if (xa.code != 200 || xs == null) throw new IOException("Xbox-Anmeldung fehlgeschlagen (" + xa.code + ").");
		String xstsToken = str(xs.get("Token"));
		Map<String, Object> claims = Json.objekt(xs.get("DisplayClaims"));
		Map<String, Object> xui = Json.objekt(Json.liste(claims.get("xui")).get(0));
		String uhs = str(xui.get("uhs"));
		String xuid = str(xui.get("xid"));

		// Minecraft
		Map<String, Object> mcBody = new LinkedHashMap<>();
		mcBody.put("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
		Antwort ma = postRoh("https://api.minecraftservices.com/authentication/login_with_xbox", Json.write(mcBody), "application/json", null);
		if (ma.code == 403) {
			throw new IOException("Diese App ist von Mojang noch nicht für Minecraft freigegeben (Formular: aka.ms/mce-reviewappid).");
		}
		Map<String, Object> mc = Json.objekt(Json.parse(ma.text));
		if (ma.code != 200 || mc == null || !(mc.get("access_token") instanceof String)) {
			throw new IOException("Minecraft-Anmeldung fehlgeschlagen (" + ma.code + ").");
		}
		String token = (String) mc.get("access_token");
		long gueltig = zahl(mc.get("expires_in"), 86400);

		// Besitzt das Konto Minecraft: Java Edition?
		Map<String, Object> rechte = Json.objekt(Json.parse(get("https://api.minecraftservices.com/entitlements/mcstore", token)));
		List<Object> items = rechte == null ? null : Json.liste(rechte.get("items"));
		if (items == null || items.isEmpty()) {
			throw new IOException("Dieses Microsoft-Konto besitzt Minecraft: Java Edition nicht.");
		}
		Map<String, Object> profil = Json.objekt(Json.parse(get("https://api.minecraftservices.com/minecraft/profile", token)));
		if (profil == null || !(profil.get("name") instanceof String)) {
			throw new IOException("Noch kein Minecraft-Name. Starte Minecraft einmal im offiziellen Launcher und wähle einen Namen.");
		}
		daten.put("mcToken", token);
		daten.put("mcAblauf", System.currentTimeMillis() + gueltig * 1000);
		daten.put("name", profil.get("name"));
		daten.put("uuid", profil.get("id"));
		daten.put("xuid", xuid);
		speichern();
		return new Sitzung((String) profil.get("name"), (String) profil.get("id"), token, xuid);
	}

	private static String fehlerText(String code) {
		switch (code) {
			case "authorization_declined": return "Die Anmeldung wurde abgelehnt.";
			case "expired_token": return "Der Code ist abgelaufen. Bitte noch einmal anmelden.";
			case "bad_verification_code": return "Der Code war ungültig.";
			case "invalid_client": case "unauthorized_client":
				return "Microsoft kennt diese App-ID nicht oder öffentliche Clients sind in der App nicht erlaubt.";
			default: return "Microsoft-Anmeldung fehlgeschlagen: " + code;
		}
	}

	private static String xboxFehler(String xerr) {
		switch (xerr) {
			case "2148916233": return "Zu diesem Konto gibt es noch kein Xbox-Profil. Einmal auf xbox.com anmelden, dann erneut versuchen.";
			case "2148916235": return "Xbox Live ist in deinem Land nicht verfügbar.";
			case "2148916236": case "2148916237": return "Das Konto muss auf xbox.com noch bestätigt werden (Altersnachweis).";
			case "2148916238": return "Kinderkonto: Ein Erwachsener muss das Konto erst zu einer Microsoft-Familie hinzufügen.";
			default: return "Xbox-Anmeldung fehlgeschlagen (" + xerr + ").";
		}
	}

	// ============================================================ HTTP

	private static final class Antwort {
		final int code;
		final String text;

		Antwort(int code, String text) {
			this.code = code;
			this.text = text;
		}
	}

	private static String post(String url, String body, String typ, String bearer) throws IOException {
		Antwort a = postRoh(url, body, typ, bearer);
		if (a.code / 100 != 2) throw new IOException("Server antwortet mit " + a.code + " (" + url.replaceAll("\\?.*", "") + ")");
		return a.text;
	}

	private static Antwort postRoh(String url, String body, String typ, String bearer) throws IOException {
		HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		c.setRequestMethod("POST");
		c.setDoOutput(true);
		c.setConnectTimeout(15000);
		c.setReadTimeout(30000);
		c.setRequestProperty("Content-Type", typ);
		c.setRequestProperty("Accept", "application/json");
		c.setRequestProperty("User-Agent", "BambusLauncher/" + BambusLauncher.VERSION);
		if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer);
		try (OutputStream out = c.getOutputStream()) {
			out.write(body.getBytes(StandardCharsets.UTF_8));
		}
		int code = c.getResponseCode();
		InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
		return new Antwort(code, in == null ? "" : lies(in));
	}

	private static String get(String url, String bearer) throws IOException {
		HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		c.setConnectTimeout(15000);
		c.setReadTimeout(30000);
		c.setRequestProperty("Accept", "application/json");
		c.setRequestProperty("User-Agent", "BambusLauncher/" + BambusLauncher.VERSION);
		c.setRequestProperty("Authorization", "Bearer " + bearer);
		int code = c.getResponseCode();
		InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
		String text = in == null ? "" : lies(in);
		if (code == 404) return "{}";
		if (code / 100 != 2) throw new IOException("Server antwortet mit " + code);
		return text;
	}

	private static String lies(InputStream in) throws IOException {
		try (InputStream i = in) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int n;
			while ((n = i.read(b)) > 0) out.write(b, 0, n);
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	private static String enc(String s) {
		try {
			return URLEncoder.encode(s == null ? "" : s, "UTF-8");
		} catch (Exception e) {
			return "";
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static long zahl(Object o, long standard) {
		return o instanceof Number ? ((Number) o).longValue() : standard;
	}
}
