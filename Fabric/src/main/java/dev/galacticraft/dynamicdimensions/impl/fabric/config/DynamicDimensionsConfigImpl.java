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

package dev.galacticraft.dynamicdimensions.impl.fabric.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.config.DynamicDimensionsConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@ApiStatus.Internal
public final class DynamicDimensionsConfigImpl implements DynamicDimensionsConfig {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    @Expose
    private boolean allowDimensionCreation;
    @Expose
    private boolean deleteRemovedDimensions;
    @Expose
    private boolean deleteDimensionsWithPlayers;
    @Expose
    private boolean enableCommands;
    @Expose
    private int commandPermissionLevel;

    private DynamicDimensionsConfigImpl() {
        this(true, false, true, false, 2);
    }

    private DynamicDimensionsConfigImpl(boolean allowDimensionCreation, boolean deleteRemovedDimensions, boolean deleteDimensionsWithPlayers, boolean enableCommands, int commandPermissionLevel) {
        this.allowDimensionCreation = allowDimensionCreation;
        this.deleteRemovedDimensions = deleteRemovedDimensions;
        this.deleteDimensionsWithPlayers = deleteDimensionsWithPlayers;
        this.enableCommands = enableCommands;
        this.commandPermissionLevel = commandPermissionLevel;
    }

    public static @NotNull DynamicDimensionsConfigImpl create() {
        File file = FabricLoader.getInstance().getConfigDir().resolve("dynamic_dimensions.json").toFile();
        if (file.exists()) {
            try (FileReader json = new FileReader(file, StandardCharsets.UTF_8)) {
                DynamicDimensionsConfigImpl config = GSON.fromJson(json, DynamicDimensionsConfigImpl.class);
                if (config != null) {
                    return config;
                } else {
                    throw new RuntimeException("Dynamic Dimensions: Failed to read configuration file!");
                }
            } catch (IOException e) {
                throw new RuntimeException("Dynamic Dimensions: Failed to read configuration file!", e);
            }
        } else {
            file.getParentFile().mkdirs();
            DynamicDimensionsConfigImpl config = new DynamicDimensionsConfigImpl();
            try {
                Files.createDirectories(FabricLoader.getInstance().getConfigDir());
                try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(config, writer);
                }
            } catch (IOException e) {
                Constants.LOGGER.error("Failed to write config file!", e);
            }
            return config;
        }
    }

    @Override
    public boolean allowDimensionCreation() {
        return this.allowDimensionCreation;
    }

    @Override
    public boolean deleteRemovedDimensions() {
        return this.deleteRemovedDimensions;
    }

    @Override
    public boolean deleteDimensionsWithPlayers() {
        return this.deleteDimensionsWithPlayers;
    }

    @Override
    public boolean enableCommands() {
        return this.enableCommands;
    }

    @Override
    public int commandPermissionLevel() {
        return this.commandPermissionLevel;
    }

    @Override
    public void allowDimensionCreation(boolean value) {
        this.allowDimensionCreation = value;
    }

    @Override
    public void deleteRemovedDimensions(boolean value) {
        this.deleteRemovedDimensions = value;
    }

    @Override
    public void deleteDimensionsWithPlayers(boolean value) {
        this.deleteDimensionsWithPlayers = value;
    }

    @Override
    public void enableCommands(boolean value) {
        this.enableCommands = value;
    }

    @Override
    public void commandPermissionLevel(int value) {
        this.commandPermissionLevel = value;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "DynamicDimensionsConfigImpl{" +
                "allowDimensionCreation=" + allowDimensionCreation +
                ", deleteRemovedDimensions=" + deleteRemovedDimensions +
                ", deleteDimensionsWithPlayers=" + deleteDimensionsWithPlayers +
                ", enableCommands=" + enableCommands +
                ", commandPermissionLevel=" + commandPermissionLevel +
                '}';
    }
}
