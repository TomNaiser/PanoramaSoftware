package de.tomnaiser.panoramaandroid;

/** Losing read-only data must not imply that a physical action has an unknown outcome. */
final class CommandEffects {
    static boolean readOnly(String type) { return type.startsWith("GET_") || type.equals("HELLO"); }
}
