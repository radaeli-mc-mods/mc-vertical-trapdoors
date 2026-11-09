package com.radaeli.verticaltrapdoors;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class TrapDoorProperties {
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

    private TrapDoorProperties() {
    }
}
