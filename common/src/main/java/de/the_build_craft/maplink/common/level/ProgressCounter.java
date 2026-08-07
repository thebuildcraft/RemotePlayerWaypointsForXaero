/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2026  Leander Knüttel and contributors
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.the_build_craft.maplink.common.level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class ProgressCounter {
    public static final AtomicBoolean converting = new AtomicBoolean(false);
    public static final AtomicBoolean readyForRender = new AtomicBoolean(false);

    public static final List<CompletableFuture<Void>> conversionTasks = new ArrayList<>();

    public static AtomicInteger totalTiles = new AtomicInteger(0);
    public static AtomicInteger convertedTiles = new AtomicInteger(0);

    public static AtomicInteger totalChunks = new AtomicInteger(0);
    public static AtomicInteger renderedChunks = new AtomicInteger(0);

    private static final AtomicBoolean success = new AtomicBoolean(false);
    private static ExecutorService executor;

    public static void init() {
        converting.set(true);
        totalTiles.set(0);
        convertedTiles.set(0);
        totalChunks.set(0);
        renderedChunks.set(0);
        success.set(false);
        executor = Executors.newFixedThreadPool(4);
    }

    public static void addTask(RunnableWithException runnable) {
        if (!converting.get()) return;

        totalTiles.incrementAndGet();
        ProgressCounter.conversionTasks.add(CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
                success.set(true);
            } catch (Exception ignored) {}
            convertedTiles.incrementAndGet();
        }, executor));
    }

    public static boolean finishAllTasks() {
        synchronized (TileConverter.conversionLock) {
            if (!converting.get()) return false;
            try {
                CompletableFuture.allOf(conversionTasks.toArray(new CompletableFuture[0])).join();

                converting.set(false);
                if (success.get()) {
                    ChunkCache.countChunks();
                    if (totalChunks.get() == 0) {
                        TileConverter.clear();
                        return false;
                    }
                    readyForRender.set(true);
                    return true;
                } else {
                    TileConverter.clear();
                    return false;
                }
            } catch (Exception ignored) {
                TileConverter.clear();
                return false;
            }
        }
    }

    public static void clear() {
        converting.set(false);
        readyForRender.set(false);
        CompletableFuture.allOf(conversionTasks.toArray(new CompletableFuture[0])).cancel(true);
        conversionTasks.clear();
        if (executor != null) {
            executor.close();
            executor = null;
        }
        success.set(false);
    }

    public static String getProgressString() {
        if (converting.get()) {
            if (totalTiles.get() == 0) return "0.0 %";
            return String.format("Downloading & Converting: %.1f %%", (((float)convertedTiles.get()) / totalTiles.get()) * 100);
        }
        if (readyForRender.get()) {
            return String.format("Xaero is rendering: %.1f %%", (((float)renderedChunks.get()) / totalChunks.get()) * 100);
        }
        return null;
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }
}
