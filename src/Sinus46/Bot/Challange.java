package Sinus46.Bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.ResourceBundle;

public class Challange extends ListenerAdapter {
    private final int lobbyID;
    private Message message;
    private Message privateMessage;
    private final MessageChannel channel;
    private final User challenger;
    private final User against;
    private final PrivateChannel privateChannel;
    private boolean started = false;
    private final ResourceBundle resources;
    private final boolean german;

    public Challange(int lobbyID, MessageChannel channel, User challenger, User against, ResourceBundle resources, boolean german) {
        this.lobbyID = lobbyID;
        this.channel = channel;
        this.challenger = challenger;
        this.against = against;
        privateChannel = against.openPrivateChannel().complete();
        this.resources = resources;
        this.german = german;
        lobbyStatus();
    }

    private void lobbyStatus(){
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(resources.getString("title").replaceAll("\\$s", String.valueOf(lobbyID)));
        builder.setColor(0x671e73);
        builder.addField(resources.getString("challenger"), challenger.getName(), true);
        builder.addField(resources.getString("opponent"), against.getName(), true);

        Button join = Button.primary(lobbyID + ":join", resources.getString("accept"));

        privateMessage = privateChannel.sendMessageEmbeds(builder.build()).setActionRows(ActionRow.of(join)).complete();
        message = channel.sendMessageEmbeds(builder.build()).complete();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        String[] id = event.getComponentId().split(":");
        if (id[1].equals("join")){
            if (Integer.parseInt(id[0]) != lobbyID) return;
            started = true;
            event.getJDA().addEventListener(new Game(Arrays.asList(challenger, against), event.getJDA(), lobbyID, channel, resources, german));
            event.reply(resources.getString("game_init")).queue();
            message.delete().queue();
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals("sudo")) return;
        if (started) return;
        if (event.getMember().getUser().getIdLong() != 465149183058378762L) {
            event.reply("Bunu yapmaya sadece Sinus46'nın hakkı var!").setEphemeral(true).queue();
            return;
        }
        if (Integer.parseInt(event.getOption("lobby").getAsString()) != lobbyID) return;
        if ("accept".equals(event.getSubcommandName())) {
            if (started) {
                event.reply("Oyun çoktan başladı.").setEphemeral(true).queue();
                return;
            }
            started = true;
            event.getJDA().addEventListener(new Game(Arrays.asList(challenger, against), event.getJDA(), lobbyID, channel, resources, german));
            event.reply("Oyun başlıyor!").queue();
            message.delete().queue();

        }
    }
}

