package Sinus46.Bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.ResourceBundle;

public class Lobby extends ListenerAdapter {
    private final int lobbyID;
    private final ArrayList<User> gamers = new ArrayList<>();
    private Message message;
    private final MessageChannel channel;
    private final ResourceBundle resources;
    private final boolean german;

    public Lobby(int lobbyID, MessageChannel channel, User user, ResourceBundle resources, boolean german) {
        this.lobbyID = lobbyID;
        this.channel = channel;
        this.resources = resources;
        this.german = german;
        gamers.add(user);
        lobbyStatus();
    }

    private void lobbyStatus(){
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(resources.getString("title").replaceAll("\\$s", String.valueOf(lobbyID)));
        builder.setColor(0x671e73);
        StringBuilder players = new StringBuilder();
        for (User gamer:gamers) {
            players.append(gamer.getName()).append("\n");
        }
        builder.addField(resources.getString("challenger"), players.toString(), true);

        Button join = Button.primary(lobbyID + ":join", resources.getString("accept"));

        message = channel.sendMessageEmbeds(builder.build()).setActionRows(ActionRow.of(join)).complete();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        String[] id = event.getComponentId().split(":");
        if (id[1].equals("join")){
            if (Integer.parseInt(id[0]) != lobbyID) return;

            if (gamers.size() == 2) {
                event.reply(resources.getString("lobby_full")).setEphemeral(true).queue();
                return;
            }

            if (gamers.contains(event.getUser())) {
                event.reply(resources.getString("already_joined")).setEphemeral(true).queue();
                return;
            }

            gamers.add(event.getUser());
            if (gamers.size() == 2) {
                event.getJDA().addEventListener(new Game(gamers, event.getJDA(), lobbyID, channel, resources, german));
                event.reply(resources.getString("game_init")).queue();
                message.delete().queue();
            } else {
                event.reply(resources.getString("accepted")).setEphemeral(true).queue();
                message.delete().queue();
                lobbyStatus();
            }
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals("sudo")) return;
        if (gamers.size() == 3) return;
        if (event.getMember().getUser().getIdLong() != 465149183058378762L) {
            event.reply("Bunu yapmaya sadece Sinus46'nın hakkı var!").setEphemeral(true).queue();
            return;
        }
        if (Integer.parseInt(event.getOption("lobby").getAsString()) != lobbyID) return;
        if ("join".equals(event.getSubcommandName())) {
            if (gamers.size() == 2) {
                event.reply("Lobi doldu.").setEphemeral(true).queue();
                return;
            }

            if (gamers.contains(event.getOption("user").getAsUser())) {
                event.reply("Bu oyuncu zaten oyunda").setEphemeral(true).queue();
                return;
            }

            gamers.add(event.getOption("user").getAsUser());

            if (gamers.size() == 2) {
                event.getJDA().addEventListener(new Game(gamers, event.getJDA(), lobbyID, channel, resources, german));
                event.reply("Oyun başlıyor!").queue();
                message.delete().queue();
            } else {
                event.reply(event.getOption("user").getAsUser().getName() +
                        " oyuncusu oyuna zorla sokuldu.").setEphemeral(true).queue();
                message.delete().queue();
                lobbyStatus();
            }
        }
    }
}
