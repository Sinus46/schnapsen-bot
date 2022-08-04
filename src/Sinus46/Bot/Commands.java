package Sinus46.Bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Commands extends ListenerAdapter {
    private final ResourceBundle defaultResources = ResourceBundle.getBundle(RESOURCE_BUNDLE, Locale.GERMAN);
    private final HashMap<Guild, ResourceBundle> langSettings = new HashMap<>();
    public static final String RESOURCE_BUNDLE = "messages";

    private Commands(JDA jda){
        jda.addEventListener(this);
        updateCommands(jda);
    }
    
    public static void initCommands(JDA jda){
        if (instance == null) {
            instance = new Commands(jda);
        }
    }
    private static Commands instance;
    private int lobbyID = 1;

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getName().equals("schnapsen")) {
            boolean german_cards;
            try {
                german_cards = event.getOption("german_cards").getAsBoolean();
            } catch (NullPointerException e) {
                german_cards = false;
            }
            ResourceBundle resources = langSettings.getOrDefault(event.getGuild(), defaultResources);
            switch (event.getSubcommandName()){
                case "challenge" ->{
                    event.getJDA().addEventListener(new Challange(lobbyID, event.getChannel(), event.getUser(),
                            event.getOption("gegner").getAsUser(),
                            resources, german_cards));
                    event.reply(resources.getString("lobby_create")
                            .replaceAll("\\$s", String.valueOf(lobbyID))).setEphemeral(true).queue();
                    lobbyID++;
                }
                case "ai" ->{
                    event.reply(resources.getString("game_init")).queue();
                    event.getJDA().addEventListener(new AIGame(event.getUser(), event.getJDA(), lobbyID, event.getChannel(),
                            resources, german_cards));

                }
                case "public" -> {
                    event.getJDA().addEventListener(new Lobby(lobbyID, event.getChannel(), event.getUser(),
                            resources, german_cards));
                    event.reply(resources.getString("lobby_create")
                            .replaceAll("\\$s", String.valueOf(lobbyID))).setEphemeral(true).queue();
                    lobbyID++;
                }
            }
        }
        if (event.getName().equals("language")) {
            langSettings.put(event.getGuild(), ResourceBundle.getBundle(RESOURCE_BUNDLE, new Locale(event.getSubcommandName())));
            event.reply(langSettings.get(event.getGuild()).getString("language_switch")).queue();
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        updateCommands(event.getGuild());
    }

    private void updateCommands(JDA jda){
        for (Guild guild : jda.getGuilds()) {
            updateCommands(guild);
        }
    }

    private void updateCommands(Guild guild) {
        guild.updateCommands()
                .addCommands(new CommandData("schnapsen", "Eine Schnapsenpartie erstellen")
                                .addSubcommands(
                                new SubcommandData("challenge", "Jemanden herausfordern")
                                        .addOption(OptionType.USER, "gegner",
                                                "Der gegner, den du herausfordern willst.", true)
                                        .addOption(OptionType.BOOLEAN, "german_cards",
                                                "Ob Sie mit Deutschblatt spielen wollen.", false),
                                new SubcommandData("public", "Ein öffentliches Spiel erstellen")
                                        .addOption(OptionType.BOOLEAN, "german_cards",
                                                "Ob Sie mit Deutschblatt spielen wollen.", false),
                                new SubcommandData("ai", "Gegen den Komputer spielen.")
                                        .addOption(OptionType.BOOLEAN, "german_cards",
                                                "Ob Sie mit Deutschblatt spielen wollen.", false)
                        ),
                        new CommandData("language", "Sprache verändern")
                        .addSubcommands(new SubcommandData("en", "Change Language to English"),
                                new SubcommandData("tr", "Dili Türkçeye ayarla."),
                                new SubcommandData("de", "Botsprache auf Deutsch verändern")))
                .queue();
    }
}
