package com.logankulinski.runner;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public final class ApplicationCommandRunner implements ApplicationRunner {
    private final JDA discordClient;

    private final long guildId;

    @Autowired
    public ApplicationCommandRunner(JDA discordClient, @Value("${discord.guild-id}") long guildId) {
        this.discordClient = Objects.requireNonNull(discordClient);

        this.guildId = guildId;
    }

    private OptionData getNameOption() {
        String name = "name";

        String description = "The name of the item used to search for a recipe";

        boolean required = true;

        return new OptionData(OptionType.STRING, name, description, required);
    }

    private List<Command.Choice> getDataCenterChoices() {
        List<String> dataCenterNames = List.of(
            "Aether",
            "Crystal",
            "Dynamis",
            "Primal",
            "Chaos",
            "Light",
            "Materia",
            "Elemental",
            "Gaia",
            "Mana",
            "Meteor"
        );

        return dataCenterNames.stream()
                              .map(name -> new Command.Choice(name, name))
                              .toList();
    }

    private OptionData getDataCenterOption() {
        String name = "data-center";

        String description = "The data center where ingredients will be searched for";

        boolean required = true;

        OptionData option = new OptionData(OptionType.STRING, name, description, required);

        List<Command.Choice> choices = this.getDataCenterChoices();

        option.addChoices(choices);

        return option;
    }

    private CommandData getItemCostCommand() {
        String name = "item-cost";

        String description = "Gets the cost comparison of buying the item from the market board or crafting it";

        OptionData nameOption = this.getNameOption();

        OptionData dataCenterOption = this.getDataCenterOption();

        return Commands.slash(name, description)
                       .addOptions(nameOption, dataCenterOption);
    }

    private CommandData getRecipeCommand() {
        String name = "recipe";

        String description = "Gets the recipe for the item with the specified name";

        OptionData nameOption = this.getNameOption();

        OptionData dataCenterOption = this.getDataCenterOption();

        return Commands.slash(name, description)
                       .addOptions(nameOption, dataCenterOption);
    }

    @Override
    public void run(ApplicationArguments args) {
        Guild guild = this.discordClient.getGuildById(this.guildId);

        if (guild == null) {
            System.out.println("Guild not found");

            return;
        }

        CommandData itemCostCommand = this.getItemCostCommand();

        CommandData recipeCommand = this.getRecipeCommand();

        guild.updateCommands()
             .addCommands(itemCostCommand, recipeCommand)
             .queue();
    }
}
