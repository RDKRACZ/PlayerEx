package com.github.clevernucleus.playerex.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.clevernucleus.playerex.api.client.page.Page;
import com.github.clevernucleus.playerex.api.client.page.PageRegistry;
import com.github.clevernucleus.playerex.client.NetworkHandlerClient;
import com.github.clevernucleus.playerex.client.PlayerExClient;
import com.github.clevernucleus.playerex.client.gui.widget.ScreenButtonWidget;
import com.github.clevernucleus.playerex.client.gui.widget.TabButtonWidget;
import com.github.clevernucleus.playerex.handler.AttributesScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class AttributesScreen extends AbstractInventoryScreen<AttributesScreenHandler> {
	private List<Page> pages = new ArrayList<Page>();
	private int currentTab = 0;
	
	public AttributesScreen(AttributesScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
		
		this.pages.add(0, PageRegistry.findPage(PlayerExClient.ATTRIBUTES_PAGE));
		this.pages.add(1, PageRegistry.findPage(PlayerExClient.COMBAT_PAGE));
		
		PageRegistry.pages().entrySet().stream().filter(this::filter).forEach(entry -> this.pages.add(entry.getValue()));
		
		this.pages.forEach(page -> page.buildLayers(this, screenHandler, playerInventory));
	}
	
	private boolean filter(Map.Entry<Identifier, Page> entry) {
		return !(entry.getKey().equals(PlayerExClient.ATTRIBUTES_PAGE) || entry.getKey().equals(PlayerExClient.COMBAT_PAGE));
	}
	
	private void forEachButton(Consumer<ButtonWidget> consumer) {
		this.children().stream().filter(e -> e instanceof ButtonWidget).forEach(e -> consumer.accept((ButtonWidget)e));
	}
	
	private void forEachTab(Consumer<TabButtonWidget> consumer) {
		this.children().stream().filter(e -> e instanceof TabButtonWidget).forEach(e -> consumer.accept((TabButtonWidget)e));
	}
	
	private Page currentPage() {
		int current = MathHelper.clamp(this.currentTab, 0, this.pages.size() - 1);
		
		return this.pages.get(current);
	}
	
	private void onTooltip(ButtonWidget button, MatrixStack matrices, int mouseX, int mouseY) {
		this.renderTooltip(matrices, ((TabButtonWidget)button).page().title(), mouseX, mouseY);
	}
	
	@Override
	protected void applyStatusEffectOffset() {
		if(this.client.player.getStatusEffects().isEmpty()) {
			this.x = (this.width - this.backgroundWidth) / 2;
			this.drawStatusEffects = false;
		} else {
			this.drawStatusEffects = true;
		}
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(PlayerExClient.keyBinding.matchesKey(keyCode, scanCode)) {
			this.onClose();
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		this.currentPage().layers().forEach(layer -> layer.render(matrices, mouseX, mouseY, delta));
		this.forEachButton(button -> button.renderTooltip(matrices, mouseX, mouseY));
	}
	
	@Override
	protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
		int u = this.x;
		int v = (this.height - this.backgroundHeight) / 2;
		
		RenderSystem.setShaderTexture(0, PlayerExClient.GUI);
		this.drawTexture(matrices, u, v, 0, 0, this.backgroundWidth, this.backgroundWidth);
		this.currentPage().layers().forEach(layer -> layer.drawBackground(matrices, delta, mouseX, mouseY));
		this.forEachButton(button -> button.render(matrices, mouseX, mouseY, delta));
	}
	
	@Override
	protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
		this.textRenderer.draw(matrices, this.currentPage().title(), (float)this.titleX, (float)(this.titleY + 2), 4210752);
	}
	
	@Override
	protected void init() {
		super.init();
		this.clearChildren();
		this.addDrawableChild(new ScreenButtonWidget(this, 155, 7, 190, 0, 14, 13, NetworkHandlerClient::openInventoryScreen));
		
		for(int i = 0; i < this.pages.size(); i++) {
			Page page = this.pages.get(i);
			int u = ((i % 6) * 29) + (i < 6 ? 0 : 3);
			int v = i < 6 ? -28 : (this.backgroundHeight - 4);
			
			this.addDrawableChild(new TabButtonWidget(this, page, i, u, v, btn -> {
				TabButtonWidget button = (TabButtonWidget)btn;
				this.currentTab = button.index();
				this.forEachTab(tab -> tab.active = true);
				button.active = false;
				this.init();
			}, this::onTooltip));
		}
		
		this.forEachTab(btn -> {
			if(btn.index() == this.currentTab) {
				btn.active = false;
			}
		});
		
		this.currentPage().layers().forEach(layer -> {
			layer.init(this.client, this.width, this.height);
			layer.children().stream().filter(e -> e instanceof ButtonWidget).forEach(e -> this.addDrawableChild((ButtonWidget)e));
		});
	}
}
