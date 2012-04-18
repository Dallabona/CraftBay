/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright 2012 StarTux
 *
 * This file is part of CraftBay.
 *
 * CraftBay is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * CraftBay is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CraftBay.  If not, see <http://www.gnu.org/licenses/>.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package edu.self.startux.craftBay;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import java.util.Map;

public class PlayerMerchant implements Merchant {
        private CraftBayPlugin plugin;
        private Player player;

        public PlayerMerchant(CraftBayPlugin plugin, Player player) {
                this.plugin = plugin;
                this.player = player;
        }

        @Override
        public String getName() {
                return player.getName();
        }

        @Override
        public boolean hasAmount(int amount) {
                if (plugin.getEco().getBalance(player.getName()) < amount) {
                        return false;
                }
                return true;
        }

        @Override
        public void giveAmount(int amount) {
                if (amount < 0) {
                        throw new IllegalArgumentException("give amount must be positive!");
                }
                plugin.getEco().depositPlayer(player.getName(), amount);
        }

        @Override
        public void takeAmount(int amount) {
                if (amount < 0) {
                        throw new IllegalArgumentException("take amount must be positive!");
                }
                plugin.getEco().withdrawPlayer(player.getName(), amount);
        }

        @Override
        public boolean hasItem(ItemStack stack) {
                Map<Integer, ? extends ItemStack> ret = player.getInventory().all(stack.getType());
                int found = 0;
                for (ItemStack slot : ret.values()) {
                        if (slot.getDurability() == stack.getDurability() && hasSameEnchantments(slot, stack)) {
                                found += slot.getAmount();
                        }
                }
                if (found < stack.getAmount()) {
                        return false;
                }
                return true;
        }

        @Override
        public void takeItem(ItemStack stack) {
                if (stack == null) return;
                Map<Integer, ? extends ItemStack> ret = player.getInventory().all(stack.getType());
                int needed = stack.getAmount();
                for (Map.Entry<Integer, ? extends ItemStack> entry : ret.entrySet()) {
                        Integer ix = entry.getKey();
                        ItemStack slot = entry.getValue();
                        if (slot.getDurability() == stack.getDurability() && hasSameEnchantments(slot, stack)) {
                                if (needed <= 0) break;
                                if (slot.getAmount() <= needed) { // catches amount=1
                                        needed -= slot.getAmount();
                                        player.getInventory().clear(ix);
                                } else {
                                        slot.setAmount(slot.getAmount() - needed);
                                        player.getInventory().setItem(ix, slot);
                                        needed = 0;
                                        break;
                                }
                        }
                }
        }

        @Override
        public void giveItem(ItemStack stack) {
                if (stack == null) return;
                int due = stack.getAmount();
                int stackSize = stack.getMaxStackSize();
                if (stackSize < 1) {
                        stackSize = 64;
                }
                while (due > 0) {
                        ItemStack other = new ItemStack(stack);
                        if (due < stackSize) {
                                other.setAmount(due);
                                due = 0;
                        } else {
                                other.setAmount(stackSize);
                                due -= stackSize;
                        }
                        Map<Integer, ItemStack> ret = player.getInventory().addItem(other);
                        for (ItemStack item : ret.values()) {
                                player.getWorld().dropItem(player.getLocation(), item);
                        }
                }
                if (!player.isOnline()) {
                        player.saveData();
                }
        }

        @Override
        public void msg(String msg) {
                plugin.msg(player, msg);
        }

        @Override
        public void warn(String msg) {
                plugin.warn(player, msg);
        }

        @Override
        public boolean equals(Object o) {
                if (o == null) {
                        return false;
                }
                if (o instanceof PlayerMerchant) {
                        PlayerMerchant other = (PlayerMerchant)o;
                        if (player.equals(other.player)) {
                                return true;
                        }
                } else if (o instanceof CommandSender) {
                        CommandSender sender = (CommandSender)o;
                        if (player.equals(sender)) {
                                return true;
                        }
                }
                return false;
        }

	private static boolean hasSameEnchantments(ItemStack stack, ItemStack value) {
		if ((stack.getEnchantments().isEmpty())&&(value.getEnchantments().isEmpty()))return true;
		if ((!stack.getEnchantments().isEmpty())&&(value.getEnchantments().isEmpty()))return false;
		if ((stack.getEnchantments().isEmpty())&&(!value.getEnchantments().isEmpty()))return false;
		for (Map.Entry<Enchantment, Integer> ench:stack.getEnchantments().entrySet()){
			boolean found = false;
			for(Map.Entry<Enchantment, Integer> compare : value.getEnchantments().entrySet()){
				if(ench.getKey() == compare.getKey()){
					if (ench.getValue() == compare.getValue()) found = true;
				}
			}
			if (found == false) return false;
		}
		for (Map.Entry<Enchantment, Integer> ench:value.getEnchantments().entrySet()){
			boolean found = false;
			for(Map.Entry<Enchantment, Integer> compare :stack.getEnchantments().entrySet()){
				if(ench.getKey() == compare.getKey()){
					if (ench.getValue() == compare.getValue()) found = true;
				}
			}
			if (found == false) return false;
		}
		return true;
	}

}