package Sinus46.Bot;

import Sinus46.Schnapsen.Card;
import Sinus46.Schnapsen.Trick;
import Sinus46.ThreadLocker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class Game extends ListenerAdapter {
    private final List<User> gamers;
    private final JDA jda;
    private final int lobbyID;
    private final MessageChannel channel;
    private final ResourceBundle resources;
    private final boolean german;
    private Trick trick;
    private final int[] scores = {7, 7};
    private final Message[] messages = new Message[2];
    private final String uuid = UUID.randomUUID().toString();
    private int vorhand = 0;
    private boolean ansage = false;

    public Game(List<User> gamers, JDA jda, int lobbyID, MessageChannel channel, ResourceBundle resources, boolean german){
        this.gamers = gamers;
        this.jda = jda;
        this.lobbyID = lobbyID;
        this.channel = channel;
        this.resources = resources;
        this.german = german;
        loop();
    }

    private void loop(){
        new Thread(() -> {
            do {
                trick = new Trick(german);
                while (trick.ergebnis() == 0) {
                    sendMessages();
                    ThreadLocker.pause();
                }
                scores[(trick.player() + vorhand) % 2] -= trick.ergebnis();
                vorhand = (vorhand + 1) % 2;
                if (!(scores[0] < 0 || scores[1] < 0)) {
                    try {
                        zwischenNachrichten();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (!(scores[0] <= 0 || scores[1] <= 0));
            gameEndMessages();
            jda.removeEventListener(this);
        }).start();
    }

    private MessageEmbed generateEmbed(int player){
        int j = (player + vorhand) % 2;
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(resources.getString("schnapsen")).setColor(switch (trick.talonSize()){
            case 0 -> 0xbd1c00;
            case -1 -> 0x871400;
            default -> 0x0286d9;
        });
        builder.addField(resources.getString("trump"),
                        trick.talonGeschlossen() ? Card.parseSuit(trick.getTrump().suit(), german) + "" : trick.getTrump().toString(),
                        true)
                .addField(resources.getString("talon"), switch (trick.talonSize()){
                    case 0 -> resources.getString("closed");
                    case -1 -> resources.getString("locked");
                    default -> resources.getString("remaining").replaceAll("\\$s",
                            String.valueOf(trick.talonSize()));
        }, true);
        builder.addField(resources.getString("points"), trick.scores()[j] + "", true);
        builder.addField(resources.getString("trick"), trick + "",false);
        builder.addField(resources.getString("hand"), trick.getHand(j) + "", false);
        builder.setFooter(gamers.get(0).getName() + " " + scores[0] + " - " + scores[1] + " " + gamers.get(1).getName());
        return builder.build();
    }
    private List<ActionRow> buttons(int player){
        int j = (player + vorhand) % 2;
        List<Button> row1 = new ArrayList<>();
        List<ActionRow> ausgabe = new ArrayList<>();
        List<Card> content = trick.getHand(j).content();
        for (int i = 0; i < content.size(); i++) {
            Card card = content.get(i);
            Button button = Button.secondary(uuid + ":" + i , card + "");
            if (card.suit() == trick.getTrump().suit()) button = button.withStyle(ButtonStyle.PRIMARY);
            if (trick.getHand(j).isPaired(card)) button = button.withStyle(ButtonStyle.SUCCESS);
            if (!trick.getHand(j).isPlayable(card)) button = button.asDisabled();
            row1.add(button);
        }
        ausgabe.add(ActionRow.of(row1));
        if (!trick.isLeading()) return ausgabe;
        List<Button> row2 = new ArrayList<>();
        if (trick.getHand(j).canExchange()) row2.add(Button.primary(uuid + ":austausch", resources.getString("exchange")));
        if (!trick.talonGeschlossen()) row2.add(Button.danger(uuid + ":sperre", resources.getString("lock")));
        if (!row2.isEmpty()) ausgabe.add(ActionRow.of(row2));
        return ausgabe;
    }
    private void sendMessages(){
        for (Message msg:messages) {
            if (msg != null) msg.delete().queue();
        }
        for (int i = 0; i < 2; i++) {
            int j = (i+vorhand)%2;
            PrivateChannel channel = gamers.get(i).openPrivateChannel().complete();
            MessageBuilder msg = new MessageBuilder();
            if (ansage) msg.setContent(resources.getString("meld"));
            msg.setEmbeds(generateEmbed(i));
            if (j == trick.amStich()) msg.setActionRows(buttons(i));
            messages[i] = channel.sendMessage(msg.build()).complete();
        }
    }

    private MessageEmbed gameEndEmbed(int player){
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(resources.getString("title").replaceAll("\\$s", lobbyID + "")).setColor(0x671e73);
        builder.addField(switch (player){
            case 0 -> resources.getString("you");
            case 1 -> resources.getString("your_opponent");
            default -> resources.getString("challenger");
        }, gamers.get(0).getName() + " " + (Math.max(scores[0], 0)), true);
        builder.addField(switch (player){
            case 0 -> resources.getString("you");
            case 1 -> resources.getString("your_opponent");
            default -> resources.getString("challenger");
        }, gamers.get(1).getName() + " " + Math.max(scores[1], 0), true);
        if (player != -1) builder.addField(resources.getString("last_trick"), trick + "", false);
        return builder.build();
    }
    private void zwischenNachrichten() throws InterruptedException {
        for (int i = 0; i < gamers.size(); i++) {
            User gamer = gamers.get(i);
            gamer.openPrivateChannel().complete().sendMessageEmbeds(gameEndEmbed(i)).queue();
        }
        Thread.sleep(1000);
    }
    private void gameEndMessages(){
        for (Message msg:messages) {
            if (msg != null) msg.delete().queue();
        }
        for (int i = 0; i < gamers.size(); i++) {
            User user = gamers.get(i);
            user.openPrivateChannel().complete().sendMessageEmbeds(gameEndEmbed(i)).queue();
        }
        channel.sendMessageEmbeds(gameEndEmbed( -1)).queue();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        String[] id = event.getComponentId().split(":");
        if (!id[0].equals(uuid)) return;
        switch (id[1]){
            case "sperre" -> trick.talonsperre();
            case "austausch" -> trick.exchange();
            default -> {
                trick.play(Integer.parseInt(id[1]));
                ansage = (event.getButton().getStyle() == ButtonStyle.SUCCESS && trick.isLeading());
            }
        }
        ThreadLocker.resume();
    }
}
