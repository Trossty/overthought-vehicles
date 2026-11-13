package me.trossty.car.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Saved data being read back: a compound tag with defaults for missing keys and
 * codecs that see the world's registries. The block entities and vehicles were
 * written against this shape of reader, so they read their tags through it.
 */
public final class ValueInput {

    private final CompoundTag tag;
    private final @Nullable HolderLookup.Provider registries;

    private ValueInput(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
    }

    public static ValueInput of(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        return new ValueInput(tag, registries);
    }

    /** The registries the stored codecs see; only null in tests that load bare tags. */
    public HolderLookup.Provider registries() {
        return java.util.Objects.requireNonNull(this.registries, "no registries to read items with");
    }

    public CompoundTag tag() {
        return this.tag;
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public int getIntOr(String key, int fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getInt(key) : fallback;
    }

    public long getLongOr(String key, long fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getLong(key) : fallback;
    }

    public float getFloatOr(String key, float fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getFloat(key) : fallback;
    }

    public double getDoubleOr(String key, double fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getDouble(key) : fallback;
    }

    public boolean getBooleanOr(String key, boolean fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getBoolean(key) : fallback;
    }

    public String getStringOr(String key, String fallback) {
        return this.tag.contains(key, Tag.TAG_STRING) ? this.tag.getString(key) : fallback;
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag value = this.tag.get(key);
        if (value == null) {
            return Optional.empty();
        }
        var ops = this.registries == null ? NbtOps.INSTANCE : this.registries.createSerializationContext(NbtOps.INSTANCE);
        return codec.parse(ops, value).resultOrPartial(error -> { }).map(t -> t);
    }
}
