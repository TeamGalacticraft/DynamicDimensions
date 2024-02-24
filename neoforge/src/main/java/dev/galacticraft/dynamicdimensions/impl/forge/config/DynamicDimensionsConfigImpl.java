/*
 * Copyright (c) 2021-2024 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.dynamicdimensions.impl.forge.config;

import dev.galacticraft.dynamicdimensions.impl.config.DynamicDimensionsConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class DynamicDimensionsConfigImpl implements DynamicDimensionsConfig {
    public static final DynamicDimensionsConfigImpl INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        Pair<DynamicDimensionsConfigImpl, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(DynamicDimensionsConfigImpl::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private final @NotNull ModConfigSpec.BooleanValue enableCommands;
    private final @NotNull ModConfigSpec.IntValue commandPermissionLevel;

    private DynamicDimensionsConfigImpl(@NotNull ModConfigSpec.Builder builder) {
        this.enableCommands = builder
                .comment("Set this to true to enable commands")
                .translation("dynamicdimensions.config.enable_commands")
                .define("enable_commands", false);
        this.commandPermissionLevel = builder
                .comment("Set this to true to enable commands")
                .translation("dynamicdimensions.config.command_permission_level")
                .defineInRange("command_permission_level", 2, 0, 99);
    }

    @Override
    public boolean enableCommands() {
        return this.enableCommands.get();
    }

    @Override
    public int commandPermissionLevel() {
        return this.commandPermissionLevel.get();
    }

    @Override
    public void enableCommands(boolean value) {
        this.enableCommands.set(value);
    }

    @Override
    public void commandPermissionLevel(int value) {
        this.commandPermissionLevel.set(value);
    }
}
