package com.wh0oo.meteorolocracy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class MessageHelper {
    public static final Formatting RED = Formatting.RED;
    public static final Formatting GREEN = Formatting.GREEN;
    public static final Formatting GOLD = Formatting.GOLD;
    public static final Formatting YELLOW = Formatting.YELLOW;
    public static final Formatting GRAY = Formatting.GRAY;

    public static Text colored(String message, Formatting color) {
        return Text.literal("[Meteorolocracy] " + message).setStyle(Style.EMPTY.withColor(color));
    }

    public static void broadcast(MinecraftServer server, String message, Formatting color) {
        server.getPlayerManager().broadcast(colored(message, color), false);
    }
}
