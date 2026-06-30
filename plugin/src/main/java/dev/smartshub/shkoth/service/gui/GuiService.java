package dev.smartshub.shkoth.service.gui;


import dev.smartshub.shkoth.gui.AddPhysicalRewardGui;
import dev.smartshub.shkoth.gui.CommandGui;
import dev.smartshub.shkoth.gui.CreateKothGui;
import dev.smartshub.shkoth.gui.CreateSchedulerGui;
import org.bukkit.entity.Player;

public class GuiService {

    private final CreateKothGui createKothGui;
    private final CreateSchedulerGui createSchedulerGui;
    private final AddPhysicalRewardGui addPhysicalRewardGui;
    private final CommandGui commandGui;
    private final dev.smartshub.shkoth.gui.EditKothListGui editKothListGui;

    public GuiService(CreateKothGui createKothGui, CreateSchedulerGui createSchedulerGui, AddPhysicalRewardGui addPhysicalRewardGui, CommandGui commandGui, dev.smartshub.shkoth.gui.EditKothListGui editKothListGui){
        this.createKothGui = createKothGui;
        this.createSchedulerGui = createSchedulerGui;
        this.addPhysicalRewardGui = addPhysicalRewardGui;
        this.commandGui = commandGui;
        this.editKothListGui = editKothListGui;
    }

    public void openCreateKothGui(Player player) {
        createKothGui.open(player);
    }

    public void openCreateSchedulerGui(Player player) {
        createSchedulerGui.open(player);
    }

    public void openAddPhysicalRewardGui(Player player) {
        addPhysicalRewardGui.open(player);
    }

    public void openCommandGui(Player player) {
        commandGui.open(player);
    }

    public void openEditKothListGui(Player player) {
        if (editKothListGui != null) {
            editKothListGui.open(player);
        }
    }

}
