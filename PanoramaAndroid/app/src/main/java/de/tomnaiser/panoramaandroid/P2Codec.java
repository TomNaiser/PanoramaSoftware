package de.tomnaiser.panoramaandroid;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Wire format shared with PanoramaControl and PanoDuinoControl, protocol V2. */
public final class P2Codec {
    public static final int MAX_FRAME_LENGTH = 256;

    private P2Codec() { }

    public static String encode(int sequence, String type, String... fields) {
        if (sequence < 1 || sequence > 65535) throw new IllegalArgumentException("sequence");
        StringBuilder body = new StringBuilder("@P2|").append(sequence).append('|');
        appendToken(body, type);
        for (String field : fields) {
            body.append('|');
            appendToken(body, field);
        }
        String frame = body + "|" + String.format(java.util.Locale.ROOT, "%04X", crc(body.toString())) + "\r\n";
        if (frame.length() > MAX_FRAME_LENGTH) throw new IllegalArgumentException("Frame zu lang");
        return frame;
    }

    public static Frame decode(String raw) {
        if (raw == null) throw new IllegalArgumentException("Leerer Frame");
        String value = raw.replaceAll("[\\r\\n]+$", "");
        if (value.length() > MAX_FRAME_LENGTH || !value.startsWith("@P2|"))
            throw new IllegalArgumentException("Ungültiger Frame");
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 127 || value.charAt(i) < 32)
                throw new IllegalArgumentException("Nicht druckbares ASCII");
        }
        int separator = value.lastIndexOf('|');
        if (separator < 0 || value.length() - separator != 5)
            throw new IllegalArgumentException("CRC fehlt");
        int received;
        try { received = Integer.parseInt(value.substring(separator + 1), 16); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("CRC ungültig", ex); }
        String body = value.substring(0, separator);
        if (crc(body) != received) throw new IllegalArgumentException("CRC falsch");
        String[] tokens = body.split("\\|", -1);
        if (tokens.length < 3 || !tokens[0].equals("@P2") || tokens[2].isEmpty())
            throw new IllegalArgumentException("Felder fehlen");
        int sequence;
        try { sequence = Integer.parseInt(tokens[1]); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Sequenz ungültig", ex); }
        if (sequence < 0 || sequence > 65535) throw new IllegalArgumentException("Sequenz außerhalb des Bereichs");
        return new Frame(sequence, tokens[2], Arrays.copyOfRange(tokens, 3, tokens.length));
    }

    public static int crc(String ascii) {
        int result = 0xFFFF;
        for (byte value : ascii.getBytes(StandardCharsets.US_ASCII)) {
            result ^= (value & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++)
                result = (result & 0x8000) != 0 ? ((result << 1) ^ 0x1021) & 0xFFFF : (result << 1) & 0xFFFF;
        }
        return result;
    }

    private static void appendToken(StringBuilder out, String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("Leeres Feld");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 32 || ch > 126 || ch == '|') throw new IllegalArgumentException("Ungültiges Feld");
        }
        out.append(value);
    }

    public static final class Frame {
        public final int sequence;
        public final String type;
        public final String[] fields;

        Frame(int sequence, String type, String[] fields) {
            this.sequence = sequence;
            this.type = type;
            this.fields = fields;
        }
    }
}
