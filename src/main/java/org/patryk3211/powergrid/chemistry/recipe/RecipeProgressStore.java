/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.chemistry.recipe;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecipeProgressStore {
    private final Map<Identifier, Progress> progressMap = new HashMap<>();

    @NotNull
    public Progress get(ReactionRecipe recipe) {
        return get(recipe.getId());
    }

    @NotNull
    public Progress get(Identifier id) {
        var progress = progressMap.get(id);
        if(progress != null)
            return progress;

        progress = new Progress();
        progressMap.put(id, progress);
        return progress;
    }

    public float getProgress(ReactionRecipe recipe) {
        var progress = progressMap.get(recipe.getId());
        if(progress != null)
            return progress.value;
        return 0;
    }

    public void setProgress(ReactionRecipe recipe, float progress) {
        if(progress <= 0) {
            progressMap.remove(recipe.getId());
            return;
        }
        get(recipe).value = progress;
    }

    public void filter(List<ReactionRecipe> recipes) {
        var ids = new HashSet<Identifier>();
        recipes.forEach(recipe -> ids.add(recipe.getId()));
        var staleKeys = progressMap.keySet().stream().filter(id -> !ids.contains(id)).toList();
        staleKeys.forEach(progressMap::remove);
    }

    public void write(NbtCompound tag) {
        var map = new NbtCompound();
        progressMap.forEach((id, progress) -> {
            map.putFloat(id.toString(), progress.value);
        });
        tag.put("Progress", map);
    }

    public void read(NbtCompound tag) {
        progressMap.clear();
        var map = tag.getCompound("Progress");
        for(var recipeId : map.getKeys()) {
            var id = new Identifier(recipeId);
            get(id).value = map.getFloat(recipeId);
        }
    }

    public static class Progress {
        private float value = 0.0f;

        public int add(float progress) {
            value += progress;
            var successes = (int) Math.floor(value);
            value -= successes;
            return successes;
        }
    }
}
