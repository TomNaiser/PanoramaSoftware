package de.tomnaiser.panoramaandroid;

/** Bounded streaming parser; oversized and partial frames resynchronize at the next @. */
final class FrameReader {
    private final StringBuilder line = new StringBuilder();
    private boolean collecting;
    P2Codec.Frame accept(int value) {
        if (value == '@') { line.setLength(0); collecting = true; }
        if (!collecting) return null;
        if (value == '\n') {
            collecting = false;
            try { return P2Codec.decode(line.toString()); }
            catch (IllegalArgumentException ignored) { return null; }
            finally { line.setLength(0); }
        }
        if (line.length() >= P2Codec.MAX_FRAME_LENGTH) { collecting = false; line.setLength(0); }
        else line.append((char)value);
        return null;
    }
}
