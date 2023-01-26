/*
 * Copyright (c) 2021-2023 Team Galacticraft
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
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class DynamicDimensionsConfigImpl implements DynamicDimensionsConfig {
    public static final DynamicDimensionsConfigImpl INSTANCE;
    public static final ForgeConfigSpec SPEC;

    static {
        Pair<DynamicDimensionsConfigImpl, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                .configure(DynamicDimensionsConfigImpl::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private final @NotNull ForgeConfigSpec.BooleanValue allowDimensionCreation;
    private final @NotNull ForgeConfigSpec.BooleanValue deleteRemovedDimensions;
    private final @NotNull ForgeConfigSpec.BooleanValue deleteDimensionsWithPlayers;
    private final @NotNull ForgeConfigSpec.BooleanValue enableCommands;
    private final @NotNull ForgeConfigSpec.IntValue commandPermissionLevel;

    private DynamicDimensionsConfigImpl(@NotNull ForgeConfigSpec.Builder builder) {
        this.allowDimensionCreation = builder
                .comment("Set this to false to disable DynamicDimensions")
                .translation("dynamicdimensions.config.allow_dimension_creation")
                .define("allow_dimension_creation", true);
        this.deleteRemovedDimensions = builder
                .comment("Set this to true to permanently delete the files of removed dimensions")
                .translation("dynamicdimensions.config.delete_removed_dimensions")
                .define("delete_removed_dimensions", false);
        this.deleteDimensionsWithPlayers = builder
                .comment("Set this to true to permanently delete the files of removed dimensions")
                .translation("dynamicdimensions.config.delete_dimensions_with_players")
                .define("delete_dimensions_with_players", true);
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
    public boolean allowDimensionCreation() {
        return this.allowDimensionCreation.get();
    }

    @Override
    public boolean deleteRemovedDimensions() {
        return this.deleteRemovedDimensions.get();
    }

    @Override
    public boolean deleteDimensionsWithPlayers() {
        return this.deleteDimensionsWithPlayers.get();
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
    public void allowDimensionCreation(boolean value) {
        this.allowDimensionCreation.set(value);
    }

    @Override
    public void deleteRemovedDimensions(boolean value) {
        this.deleteRemovedDimensions.set(value);
    }

    @Override
    public void deleteDimensionsWithPlayers(boolean value) {
        this.deleteDimensionsWithPlayers.set(value);
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
