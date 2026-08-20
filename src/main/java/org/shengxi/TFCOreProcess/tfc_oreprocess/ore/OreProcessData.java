package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** 保存加工矿物的实际熔炼结果，避免按名称推测金属属性。 */
public record OreProcessData(
    ResourceLocation sourceItemId,
    ResourceLocation moltenFluidId,
    int metalAmountMb,
    float meltingTemperature
) {
    public static final Codec<OreProcessData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("source_item").forGetter(OreProcessData::sourceItemId),
        ResourceLocation.CODEC.fieldOf("molten_fluid").forGetter(OreProcessData::moltenFluidId),
        Codec.INT.fieldOf("metal_amount_mb").forGetter(OreProcessData::metalAmountMb),
        Codec.FLOAT.fieldOf("melting_temperature").forGetter(OreProcessData::meltingTemperature)
    ).apply(instance, OreProcessData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreProcessData> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, OreProcessData::sourceItemId,
        ResourceLocation.STREAM_CODEC, OreProcessData::moltenFluidId,
        ByteBufCodecs.VAR_INT, OreProcessData::metalAmountMb,
        ByteBufCodecs.FLOAT, OreProcessData::meltingTemperature,
        OreProcessData::new
    );
}
