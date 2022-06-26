/*
 * Copyright (c) 2021-2022 Team Galacticraft
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

package dev.galacticraft.dynworlds.impl.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import dev.galacticraft.dynworlds.api.config.DynWorldsConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DynWorldsConfigImpl implements DynWorldsConfig {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    @Expose
    private final boolean allowWorldCreation;
    @Expose
    private final boolean deleteRemovedWorlds;
    @Expose
    private final boolean deleteWorldsWithPlayers;

    private DynWorldsConfigImpl() {
        this(true, false, true);
    }

    private DynWorldsConfigImpl(boolean allowWorldCreation, boolean deleteRemovedWorlds, boolean deleteWorldsWithPlayers) {
        this.allowWorldCreation = allowWorldCreation;
        this.deleteRemovedWorlds = deleteRemovedWorlds;
        this.deleteWorldsWithPlayers = deleteWorldsWithPlayers;
    }

    public static @NotNull DynWorldsConfigImpl create() {
        Path resolve = FabricLoader.getInstance().getConfigDir().resolve("dynworlds.json");
        File file = resolve.toFile();
        if (file.exists()) {
            try (FileReader json = new FileReader(file, StandardCharsets.UTF_8)) {
                DynWorldsConfigImpl config = GSON.fromJson(json, DynWorldsConfigImpl.class);
                if (config != null) {
                    return config;
                } else {
                    throw new RuntimeException("DynWorlds: Failed to read configuration file!");
                }
            } catch (IOException e) {
                throw new RuntimeException("DynWorlds: Failed to read configuration file!", e);
            }
        } else {
            DynWorldsConfigImpl dynWorldsConfig = new DynWorldsConfigImpl();
            try {
                Files.createDirectories(FabricLoader.getInstance().getConfigDir());
                try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(dynWorldsConfig, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return dynWorldsConfig;
        }
    }

    @Override
    public boolean allowWorldCreation() {
        return this.allowWorldCreation;
    }

    @Override
    public boolean deleteRemovedWorlds() {
        return this.deleteRemovedWorlds;
    }

    @Override
    public boolean deleteWorldsWithPlayers() {
        return this.deleteWorldsWithPlayers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DynWorldsConfigImpl) obj;
        return this.allowWorldCreation == that.allowWorldCreation &&
                this.deleteRemovedWorlds == that.deleteRemovedWorlds &&
                this.deleteWorldsWithPlayers == that.deleteWorldsWithPlayers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowWorldCreation, deleteRemovedWorlds, deleteWorldsWithPlayers);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "DynWorldsConfigImpl[" +
                "allowWorldCreation=" + allowWorldCreation + ", " +
                "deleteRemovedWorlds=" + deleteRemovedWorlds + ", " +
                "deleteWorldsWithPlayers=" + deleteWorldsWithPlayers + ']';
    }
}
