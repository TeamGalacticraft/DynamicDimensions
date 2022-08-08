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

package dev.galacticraft.dyndims.api.config;

/**
 * Basic configuration file for DynamicDimensions.
 */
public interface DynamicDimensionsConfig {
    /**
     * Whether to allow the creation of dynamic worlds.
     *
     * @return Whether to allow the creation of dynamic worlds.
     */
    boolean allowDimensionCreation();

    /**
     * Whether to delete the files of removed dynamic worlds or move them into the 'deleted' folder.
     *
     * @return Whether to delete the files of removed dynamic worlds or move them into the 'deleted' folder.
     */
    boolean deleteRemovedDimensions();

    /**
     * Whether to allow the destruction of worlds which have players online in them.
     *
     * @return Whether to allow the destruction of worlds which have players online in them.
     */
    boolean deleteDimensionsWithPlayers();

    /**
     * Whether the builtin dynamic dimension creation/removal commands should be registered.
     *
     * @return Whether the builtin dynamic dimension creation/removal commands should be registered.
     */
    boolean enableCommands();

    /**
     * The operator permission level required to utilize the dynamic dimension commands.
     *
     * @return The operator permission level required to utilize the dynamic dimension commands.
     */
    int commandPermissionLevel();

    void allowDimensionCreation(boolean value);

    void deleteRemovedDimensions(boolean value);

    void deleteDimensionsWithPlayers(boolean value);

    void enableCommands(boolean value);

    void commandPermissionLevel(int value);
}
