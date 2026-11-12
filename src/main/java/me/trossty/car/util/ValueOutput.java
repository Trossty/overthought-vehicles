package me.trossty.car.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.Nullable;

/**
 * Data being saved: the writing half of {@link ValueInput}, over a compound tag.
 */
public final class ValueOutput {

    private final CompoundTag tag;
    private final @Nullable HolderLookup.Provider registries;

    private ValueOutput(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
    }

    public static ValueOutput of(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        return new ValueOutput(tag, registries);
    }

    /** The registries the stored codecs see; only null in tests that load bare tags. */
    public HolderLookup.Provider registries() {
        return java.util.Objects.requireNonNull(this.registries, "no registries to read items with");
    }

    public CompoundTag tag() {
        return this.tag;
    }

    public void putInt(String key, int value) {
        this.tag.putInt(key, value);
    }

    public void putLong(String key, long value) {
        this.tag.putLong(key, value);
    }

    public void putFloat(String key, float value) {
        this.tag.putFloat(key, value);
    }

    public void putDouble(String key, double value) {
        this.tag.putDouble(key, value);
    }

    public void putBoolean(String key, boolean value) {
        this.tag.putBoolean(key, value);
    }

    public void putString(String key, String value) {
        this.tag.putString(key, value);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        var ops = this.registries == null ? NbtOps.INSTANCE : this.registries.createSerializationContext(NbtOps.INSTANCE);
        codec.encodeStart(ops, value).result().ifPresent(encoded -> this.tag.put(key, encoded));
    }

    public void discard(String key) {
        this.tag.remove(key);
    }
}
