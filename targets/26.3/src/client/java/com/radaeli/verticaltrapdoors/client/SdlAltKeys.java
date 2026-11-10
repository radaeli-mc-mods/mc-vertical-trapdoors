package com.radaeli.verticaltrapdoors.client;

import com.mojang.blaze3d.platform.InputConstants;

/** Physical Alt scancodes used by SDL in Minecraft 26.3. */
public final class SdlAltKeys {
    private SdlAltKeys() {}

    public static boolean isAlt(int key) {
        return key == InputConstants.KEY_LALT || key == InputConstants.KEY_RALT;
    }

    public static boolean isAltDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(InputConstants.KEY_RALT);
    }
}
