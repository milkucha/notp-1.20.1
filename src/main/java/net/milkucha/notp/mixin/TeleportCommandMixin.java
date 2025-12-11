package net.milkucha.notp.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.milkucha.notp.Notp;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("notp");

	@Inject(
			method = "teleport(Lnet/minecraft/server/command/ServerCommandSource;Lnet/minecraft/entity/Entity;Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFLnet/minecraft/server/command/TeleportCommand$LookTarget;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void notp$checkTeleport(ServerCommandSource source,
										   Entity entity,
										   ServerWorld world,
										   double x, double y, double z,
										   Set<?> relative,
										   float yaw, float pitch,
										   @Coerce Object lookTarget,
										   CallbackInfo ci) throws CommandSyntaxException {
		LOGGER.info("notp: teleport check coords: src=({}, {}), dst=({}, {})", entity.getX(), entity.getZ(), x, z);
		LOGGER.info("notp: region info: {}", Notp.debugRegionInfo(entity.getX(), entity.getZ(), x, z));

		if (!Notp.canTeleport(entity.getX(), entity.getZ(), x, z)) {
			// Throwing Brigadier exception aborts the command before vanilla feedback is sent
			throw Notp.OUT_OF_BOUNDS.create();
		}
	}
}
