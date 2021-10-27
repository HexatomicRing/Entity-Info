package com.hexatomic_ring.entity_info.mixin;

import com.google.common.collect.Lists;
import com.hexatomic_ring.entity_info.utils.ModNumber;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Consumer;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow @Nullable public ClientPlayerEntity player;

	@Shadow public @Nullable abstract Entity getCameraEntity();

	@Shadow @Nullable public ClientWorld world;

	@Inject(at = @At("HEAD"), method = "doItemUse")
	private void showInfo(CallbackInfo info) {
		HitResult blockHitResult = player.raycast(1000.0,1.0F,false);

		try{
			if(blockHitResult.getType() == HitResult.Type.BLOCK){
				BlockState blockState = world.getBlockState(((BlockHitResult)blockHitResult).getBlockPos());
				if(blockState.isOf(Blocks.BEE_NEST) || blockState.isOf(Blocks.BEEHIVE)){
					player.sendMessage(new LiteralText(""),false);
					player.sendMessage(new TranslatableText("entity_info.block.honey_level", new Object[]{blockState.get(BeehiveBlock.HONEY_LEVEL)}),false);
					player.networkHandler.getDataQueryHandler().queryBlockNbt(((BlockHitResult)blockHitResult).getBlockPos(), (nbt) -> {
						player.sendMessage(new TranslatableText("entity_info.block.bee_count", new Object[]{nbt.getList("Bees",10).size()}),false);
					});
				}
			}
		}catch(Exception ignored){}

		double d = 1000.0;
		assert player != null;
		Vec3d vec3d = player.getCameraPosVec(1.0F);
		Vec3d vec3d2 = player.getRotationVec(1.0F);
		Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
		Box box = player.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0D, 1.0D, 1.0D);
		double e1 = d * d;
		EntityHitResult entityHitResult = ProjectileUtil.raycast(Objects.requireNonNull(getCameraEntity()), vec3d, vec3d3, box, (entityx) -> !entityx.isSpectator(), e1);
		if(entityHitResult==null)
			entityHitResult = ProjectileUtil.raycast(Objects.requireNonNull(getCameraEntity()), vec3d, vec3d3, box, (entityx) -> !entityx.isSpectator() && entityx.collides(), e1);

		if (entityHitResult != null){
			Entity e = entityHitResult.getEntity();
			player.sendMessage(new LiteralText(""),false);
				try{
				player.sendMessage(new TranslatableText("entity_info.entity.show_name", new Object[]{e.getDisplayName()}),false);
			}catch (Exception ignored){}

			if(e instanceof LivingEntity){
				try{
					String h1 = String.format("%.1f",((LivingEntity)e).getMaxHealth());
					String h2 = String.format("%.1f",((LivingEntity)e).getHealth());
					player.sendMessage(new TranslatableText("entity_info.living_entity.show_health", new Object[]{h1,h2}),false);
				}catch (Exception ignored){}
			}
			if(e instanceof HorseEntity){
				try{
					double s = ((HorseEntity)e).getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getBaseValue()*42.1578;
					player.sendMessage(new TranslatableText("entity_info.horse_entity.show_speed", new Object[]{String.format("%.2f",s)}),false);
					double j = ((HorseEntity)e).getAttributeInstance(EntityAttributes.HORSE_JUMP_STRENGTH).getBaseValue();
					double h =  -0.1817584952 * j*j*j + 3.689713992 * j*j + 2.128599134 * j - 0.343930367;
					player.sendMessage(new TranslatableText("entity_info.horse_entity.show_jump_strength", new Object[]{String.format("%.2f", h)}),false);
				}catch (Exception ignored){
				}
			}
			if(e instanceof PassiveEntity){
				try{
					player.networkHandler.getDataQueryHandler().queryEntityNbt(e.getId(), (nbt) ->{
						int i = nbt.getInt("Age");
						int s = Math.abs(i) / 20;
						int m = s / 60;
						s = s - m * 60;
						if(i > 0){
							player.sendMessage(new TranslatableText("entity_info.passive_entity.breed", new Object[]{m,s}),false);
						}else if(i < 0){
							player.sendMessage(new TranslatableText("entity_info.passive_entity.grow", new Object[]{m,s}),false);
						}else{
							player.sendMessage(new TranslatableText("entity_info.passive_entity.can_breed"),false);
						}
					});
					}catch (Exception ignored){}
			}
			if(e instanceof PandaEntity){
				try{
					player.sendMessage(new TranslatableText("entity_info.panda_entity.show_main_gene", new Object[]{getPandaGene(((PandaEntity)e).getMainGene())}),false);
					player.sendMessage(new TranslatableText("entity_info.panda_entity.show_hidden_gene", new Object[]{getPandaGene(((PandaEntity)e).getHiddenGene())}),false);
				}catch (Exception ignored){}
			}
			if(e instanceof TameableEntity){
				try{
					player.sendMessage(new TranslatableText("entity_info.tameable_entity.show_owner", new Object[]{((TameableEntity) e).getOwner().getDisplayName()}),false);
				}catch (Exception ignored){}
			}

			if(e instanceof MobEntity){
				try{
					player.networkHandler.getDataQueryHandler().queryEntityNbt(e.getId(), (nbt) ->{
						if(nbt.getBoolean("CanPickUpLoot"))
							player.sendMessage(new TranslatableText("entity_info.mob_entity.can_pick_up_loot"),false);
					});
				}catch (Exception ignored){}
			}

			if(e instanceof ZombieEntity){
				try{
					player.networkHandler.getDataQueryHandler().queryEntityNbt(e.getId(), (nbt) ->{
						if(nbt.getBoolean("CanBreakDoors"))
							player.sendMessage(new TranslatableText("entity_info.zombie_entity.can_break_doors"),false);
						double spawn_reinforcements = 0.0;
						NbtList attributes = nbt.getList("Attributes",10);
						for(int i = 0;i < attributes.size();i++){
							NbtCompound nbt1 = attributes.getCompound(i);
							if(nbt1.getString("Name").equals("minecraft:zombie.spawn_reinforcements")){
								spawn_reinforcements = nbt1.getDouble("Base");
								break;
							}
						}
						player.sendMessage(new TranslatableText("entity_info.zombie_entity.spawn_reinforcements",new Object[]{String.format("%.2f",spawn_reinforcements*100.0)+"%"}),false);

					});
				}catch (Exception ignored){}
			}
			if(e instanceof ItemEntity){
				try{
					ItemEntity item = (ItemEntity)e;
					ItemStack stack = item.getStack();
					player.sendMessage(new TranslatableText("entity_info.item_entity.show_info", new Object[]{new TranslatableText(stack.getItem().getTranslationKey()),stack.getCount()}),false);
					player.networkHandler.getDataQueryHandler().queryEntityNbt(e.getId(), (nbt) -> player.sendMessage(new TranslatableText("entity_info.item_entity.age", new Object[]{String.format("%.2f",(double)(6000-nbt.getInt("Age"))/20.0)}),false));
					if(stack.hasNbt()){
						player.sendMessage(new TranslatableText("entity_info.item_entity.nbt", new Object[]{}),false);
						NbtCompound compound = stack.getNbt();
						List<String> list = Lists.newArrayList((Iterable)compound.getKeys());
						String string = "";
						for(Iterator var3 = list.iterator(); var3.hasNext(); ) {
							string = (String)var3.next();
							switch(string){
								case "display":
									String s = compound.getCompound("display").getCompound("Name").getString("text");
									if(!s.isEmpty())
										player.sendMessage(new TranslatableText("entity_info.item_entity.display_name", new Object[]{}),false);
									break;
								case "Damage":
									int used = compound.getInt("Damage");
									int total = stack.getItem().getMaxDamage();
									player.sendMessage(new TranslatableText("entity_info.item_entity.durability", new Object[]{total-used,total}),false);
									break;
								case "RepairCost":
									player.sendMessage(new TranslatableText("entity_info.item_entity.repair_cost", new Object[]{compound.getInt("RepairCost")}),false);
									break;
								case "Enchantments":
									player.sendMessage(new TranslatableText("entity_info.item_entity.enchantment", new Object[]{}),false);
									NbtList list0 =  compound.getList("Enchantments",10);
									for(int i = 0;i < list0.size();i++){
										player.sendMessage(new TranslatableText("entity_info.item_entity.enchantment_format",new Object[]{
												new TranslatableText(((Enchantment)Registry.ENCHANTMENT.get(new Identifier(((NbtCompound)list0.get(i)).getString("id")))).getTranslationKey())
												, new TranslatableText("enchantment.level."+((NbtCompound)list0.get(i)).getShort("lvl"))}),false);

									}
									break;
								case "BlockEntityTag":
									player.sendMessage(new TranslatableText("entity_info.item_entity.contain_items", new Object[]{}),false);
									NbtList list1 =  compound.getCompound("BlockEntityTag").getList("Items",10);
                                    ArrayList<String> itemNames = new ArrayList<>();
									ArrayList<ModNumber> itemCount = new ArrayList<>();
									items:
									for(int i = 0;i < list1.size();i++){
										NbtCompound n = list1.getCompound(i);
										String name = n.getString("id");
										int count = n.getInt("Slot");
										for(int j = 0;j < itemNames.size();j++){
											if(itemNames.get(j).equals(name)) {
												itemCount.get(j).add(count);
												continue items;
											}
										}
										itemNames.add(name);
										itemCount.add(new ModNumber(count));
									}
									for(int i = 0;i < itemNames.size();i++){
										player.sendMessage(new TranslatableText("entity_info.item_entity.nbt_item", new Object[]{new TranslatableText(((Item)Registry.ITEM.get(new Identifier(itemNames.get(i)))).getTranslationKey()),itemCount.get(i).value()}),false);
									}
									break;
								default:
									player.sendMessage(new LiteralText(new StringBuilder(string).append(':').append((new StringNbtWriter()).apply(compound.get(string))).toString()),false);


							}
						}
					}
				}catch (Exception ignored){}
			}
		}
	}
	private TranslatableText getPandaGene(PandaEntity.Gene gene){
		return switch (gene) {
			case NORMAL -> new TranslatableText("entity_info.panda_entity.gene.normal", new Object[]{});
			case LAZY -> new TranslatableText("entity_info.panda_entity.gene.lazy", new Object[]{});
			case WORRIED -> new TranslatableText("entity_info.panda_entity.gene.worried", new Object[]{});
			case PLAYFUL -> new TranslatableText("entity_info.panda_entity.gene.playful", new Object[]{});
			case BROWN -> new TranslatableText("entity_info.panda_entity.gene.brown", new Object[]{});
			case WEAK -> new TranslatableText("entity_info.panda_entity.gene.weak", new Object[]{});
			case AGGRESSIVE -> new TranslatableText("entity_info.panda_entity.gene.aggressive", new Object[]{});
		};
	}


}
