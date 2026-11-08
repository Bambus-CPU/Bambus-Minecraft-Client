package de.bambus.launcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ein kleiner JSON-Leser/-Schreiber, damit der Launcher ohne weitere Bibliotheken auskommt. */
final class Json {

	private Json() {}

	static Object parse(String text) {
		Leser l = new Leser(text);
		l.leer();
		Object wert = l.wert();
		l.leer();
		if (l.i < text.length()) {
			throw l.fehler("Unerwartete Zeichen am Ende");
		}
		return wert;
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> objekt(Object o) {
		return o instanceof Map ? (Map<String, Object>) o : null;
	}

	@SuppressWarnings("unchecked")
	static List<Object> liste(Object o) {
		return o instanceof List ? (List<Object>) o : null;
	}

	// ------------------------------------------------------------- Lesen

	private static final class Leser {
		final String s;
		int i;

		Leser(String s) {
			this.s = s;
		}

		IllegalArgumentException fehler(String grund) {
			return new IllegalArgumentException("JSON-Fehler bei Zeichen " + i + ": " + grund);
		}

		void leer() {
			while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
				i++;
			}
		}

		Object wert() {
			if (i >= s.length()) {
				throw fehler("Text endet zu früh");
			}
			char c = s.charAt(i);
			if (c == '{') return objekt();
			if (c == '[') return liste();
			if (c == '"') return text();
			if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
			if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
			if (s.startsWith("null", i)) { i += 4; return null; }
			return zahl();
		}

		Map<String, Object> objekt() {
			Map<String, Object> m = new LinkedHashMap<>();
			i++;
			leer();
			if (s.charAt(i) == '}') { i++; return m; }
			while (true) {
				leer();
				if (s.charAt(i) != '"') throw fehler("Schlüssel erwartet");
				String k = text();
				leer();
				if (s.charAt(i) != ':') throw fehler("':' erwartet");
				i++;
				leer();
				m.put(k, wert());
				leer();
				char c = s.charAt(i++);
				if (c == '}') return m;
				if (c != ',') throw fehler("',' oder '}' erwartet");
			}
		}

		List<Object> liste() {
			List<Object> l = new ArrayList<>();
			i++;
			leer();
			if (s.charAt(i) == ']') { i++; return l; }
			while (true) {
				leer();
				l.add(wert());
				leer();
				char c = s.charAt(i++);
				if (c == ']') return l;
				if (c != ',') throw fehler("',' oder ']' erwartet");
			}
		}

		String text() {
			StringBuilder b = new StringBuilder();
			i++;
			while (true) {
				char c = s.charAt(i++);
				if (c == '"') return b.toString();
				if (c != '\\') { b.append(c); continue; }
				char e = s.charAt(i++);
				switch (e) {
					case 'n': b.append('\n'); break;
					case 't': b.append('\t'); break;
					case 'r': b.append('\r'); break;
					case 'b': b.append('\b'); break;
					case 'f': b.append('\f'); break;
					case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
					default: b.append(e);
				}
			}
		}

		Object zahl() {
			int start = i;
			while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
				i++;
			}
			if (start == i) throw fehler("Unbekannter Wert");
			String z = s.substring(start, i);
			if (z.contains(".") || z.contains("e") || z.contains("E")) {
				return Double.parseDouble(z);
			}
			return Long.parseLong(z);
		}
	}

	// ----------------------------------------------------------- Schreiben

	static String write(Object o) {
		StringBuilder b = new StringBuilder();
		schreibe(b, o, 0);
		return b.append('\n').toString();
	}

	private static void einruecken(StringBuilder b, int tiefe) {
		for (int k = 0; k < tiefe; k++) {
			b.append("  ");
		}
	}

	private static void schreibe(StringBuilder b, Object o, int tiefe) {
		if (o == null) {
			b.append("null");
		} else if (o instanceof String) {
			schreibeText(b, (String) o);
		} else if (o instanceof Map) {
			Map<String, Object> m = objekt(o);
			if (m.isEmpty()) { b.append("{}"); return; }
			b.append("{\n");
			int n = 0;
			for (Map.Entry<String, Object> e : m.entrySet()) {
				einruecken(b, tiefe + 1);
				schreibeText(b, e.getKey());
				b.append(" : ");
				schreibe(b, e.getValue(), tiefe + 1);
				b.append(++n < m.size() ? ",\n" : "\n");
			}
			einruecken(b, tiefe);
			b.append('}');
		} else if (o instanceof List) {
			List<Object> l = liste(o);
			if (l.isEmpty()) { b.append("[]"); return; }
			b.append("[\n");
			for (int k = 0; k < l.size(); k++) {
				einruecken(b, tiefe + 1);
				schreibe(b, l.get(k), tiefe + 1);
				b.append(k + 1 < l.size() ? ",\n" : "\n");
			}
			einruecken(b, tiefe);
			b.append(']');
		} else {
			b.append(o.toString());
		}
	}

	private static void schreibeText(StringBuilder b, String s) {
		b.append('"');
		for (int k = 0; k < s.length(); k++) {
			char c = s.charAt(k);
			switch (c) {
				case '"': b.append("\\\""); break;
				case '\\': b.append("\\\\"); break;
				case '\n': b.append("\\n"); break;
				case '\r': b.append("\\r"); break;
				case '\t': b.append("\\t"); break;
				default:
					if (c < 0x20) {
						b.append(String.format("\\u%04x", (int) c));
					} else {
						b.append(c);
					}
			}
		}
		b.append('"');
	}
}
